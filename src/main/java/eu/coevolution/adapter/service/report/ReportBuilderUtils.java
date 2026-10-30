package eu.coevolution.adapter.service.report;

import eu.coevolution.adapter.dto.AssessmentReportDTO;

import java.util.List;

public final class ReportBuilderUtils {

    public static final String REPORT_TOOL_VENDOR = "SLC";
    public static final String REPORT_TOOL_NAME = "RAGRE";
    public static final String REPORT_TOOL_VERSION = "1.1.1";
    public static final String REPORT_TOOL_TYPE = "application";

    private ReportBuilderUtils() {
    }

    public static boolean hasReports(List<AssessmentReportDTO> reports) {
        return reports != null && !reports.isEmpty();
    }
}
