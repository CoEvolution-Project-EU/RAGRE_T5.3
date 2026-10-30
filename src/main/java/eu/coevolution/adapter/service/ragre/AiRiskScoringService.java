package eu.coevolution.adapter.service.ragre;

import eu.coevolution.adapter.dto.AssessmentReportDTO;
import eu.coevolution.adapter.dto.AssessmentReportEntryDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AiRiskScoringService {

    private static final double SECOND_SCORE_WEIGHT = 0.5;
    private static final double REMAINING_SCORES_WEIGHT = 0.25;
    private static final double NORMALIZATION_DIVISOR = 5.0;
    private static final double MAX_AI_RISK_SCORE = 10.0;

    public double scoreAssessmentEntry(AssessmentReportEntryDTO assessment) {
        if (isIncomplete(assessment)) {
            return 0.0;
        }

        double severityWeight = assessment.getSeverity().getWeight();
        return severityWeight * severityWeight
                * assessment.getConfidence().getWeight()
                * assessment.getOccurrence().getWeight()
                * assessment.getCategory().getWeight();
    }

    public double scoreAssessmentReports(List<AssessmentReportDTO> assessmentReports) {
        if (assessmentReports == null || assessmentReports.isEmpty()) {
            return 0.0;
        }

        List<Double> scores = new ArrayList<>();
        for (AssessmentReportDTO assessmentReport : assessmentReports) {
            if (assessmentReport == null || assessmentReport.getReport() == null) {
                continue;
            }
            for (AssessmentReportEntryDTO assessment : assessmentReport.getReport()) {
                double score = scoreAssessmentEntry(assessment);
                if (score > 0.0) {
                    scores.add(score);
                }
            }
        }

        if (scores.isEmpty()) {
            return 0.0;
        }

        scores.sort(Comparator.reverseOrder());

        double finalRawScore = scores.get(0);
        if (scores.size() > 1) {
            finalRawScore += SECOND_SCORE_WEIGHT * scores.get(1);
        }
        for (int i = 2; i < scores.size(); i++) {
            finalRawScore += REMAINING_SCORES_WEIGHT * scores.get(i);
        }

        double normalizedScore = Math.min(MAX_AI_RISK_SCORE, finalRawScore / NORMALIZATION_DIVISOR);
        return BigDecimal.valueOf(normalizedScore).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean isIncomplete(AssessmentReportEntryDTO assessment) {
        return assessment == null
                || assessment.getSeverity() == null
                || assessment.getConfidence() == null
                || assessment.getOccurrence() == null
                || assessment.getCategory() == null;
    }
}
