package eu.coevolution.adapter.service.catalogue;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SecurityCatalogueService {

    private static final Map<String, String> ATTACK_NAMES_BY_ID = Map.ofEntries(
            Map.entry("ATT-001", "Data Exfiltration"),
            Map.entry("ATT-002", "Data Poisoning"),
            Map.entry("ATT-003", "Model Manipulation"),
            Map.entry("ATT-004", "Adversarial Examples"),
            Map.entry("ATT-005", "Model Inversion"),
            Map.entry("ATT-006", "Model Extraction"),
            Map.entry("ATT-007", "Exploitation for Client Execution"),
            Map.entry("ATT-008", "Denial of Service"),
            Map.entry("ATT-009", "LiDAR Adversarial Perturbation"),
            Map.entry("ATT-010", "LiDAR Adversarial Injection"),
            Map.entry("ATT-011", "LiDAR Adversarial Removal"),
            Map.entry("ATT-012", "Gradient Poisoning Attack"),
            Map.entry("ATT-013", "Gradient Inversion Attack"),
            Map.entry("ATT-014", "Feature-Based Attack (SLAM)"),
            Map.entry("ATT-015", "GPS Spoofing Attack"),
            Map.entry("ATT-016", "Model Inference Attack"),
            Map.entry("ATT-017", "Spoofing attack"),
            Map.entry("ATT-018", "Supply Chain Compromised"),
            Map.entry("ATT-019", "Unauthorised Access"),
            Map.entry("ATT-020", "Status reporting"),
            Map.entry("ATT-021", "Model Tampering Attack")
    );

    private static final Map<String, DefenceCatalogueEntry> DEFENCE_BY_ID = Map.ofEntries(
            Map.entry("DEF-001", new DefenceCatalogueEntry(
                    "Adversarial Training and Robust Optimization",
                    "Training-time robustness techniques that incorporate adversarial examples and stability-promoting objectives. Includes PGD-based adversarial training (integrated in secml-torch), S2AP for stable adversarial pruning of compressed models, and uncertainty estimation methods that yield reliable confidence scores under adversarial conditions."
            )),
            Map.entry("DEF-002", new DefenceCatalogueEntry(
                    "Input Purification and Reconstruction",
                    "Online or preprocessing transformations that project suspicious inputs back toward the training data manifold before inference. Covers DU-AP (deep unrolled, perception-aware purification for LiDAR range images) as well as autoencoder-based reconstruction, Defense-GAN-style approaches, and diffusion-based purification."
            )),
            Map.entry("DEF-003", new DefenceCatalogueEntry(
                    "Poisoning and Backdoor Defences",
                    "Methods for detecting and mitigating poisoning and backdoor attacks affecting training data or model weights. Includes PGRL (prototype-guided robust learning with Label Consistency Verification), k-NN Label Consistency Filtering, statistical detectors for forgetting attacks, and PEFTGuard for weight-based backdoor detection in fine-tuned models from untrusted sources."
            )),
            Map.entry("DEF-004", new DefenceCatalogueEntry(
                    "Continual Learning Defences",
                    "Continual and incremental learning techniques that preserve robustness and security across model updates. Combines PROACT (projection and activation constrained training), OCAR (curvature-aware replay), self-supervised continual learning methods (CMP, CLA, SOLAR), buffer-free class-incremental learning with OOD detection, and PCT-based regression-aware training to prevent security regression."
            )),
            Map.entry("DEF-005", new DefenceCatalogueEntry(
                    "Robust Federated Learning Aggregation",
                    "Server-side aggregation strategies that mitigate poisoning attacks in federated learning. Groups SecureFL's Loss-Based Clustering and FedGreed mechanisms, CoEvoNEAT's coevolutionary robustness against label-flipping, and centralized context-aware FL with norm-based outlier detection, reputation scoring, and automatic switching to robust aggregators such as Trimmed Mean."
            )),
            Map.entry("DEF-006", new DefenceCatalogueEntry(
                    "Privacy-Preserving Federated Learning",
                    "Cryptographic protection of model updates exchanged during federated learning. Implements a TenSEAL/CKKS homomorphic encryption pipeline allowing the server to aggregate encrypted parameters without accessing plaintext values, mitigating gradient inversion and membership inference attacks."
            )),
            Map.entry("DEF-007", new DefenceCatalogueEntry(
                    "Adversarial Data Generation for Robustness",
                    "Frameworks producing controlled adversarial datasets for robust training and benchmarking. Includes GuidedRangeDiff (adversarially guided diffusion model for LiDAR range images supporting targeted, untargeted, and suppress modes) and the AVISENSE Data Generator (modular simulation framework for clean and adversarial LiDAR/GNSS scenarios)."
            )),
            Map.entry("DEF-008", new DefenceCatalogueEntry(
                    "Inference-Time Anomaly and Attack Detection",
                    "Stateless and stateful detection gadgets embedded in the Protected Inference Pipeline. Combines inline anomaly detection on inputs/outputs/representations with a session and behaviour modelling layer supporting stateful detection of adversarial probing, model extraction, and model inversion attacks."
            )),
            Map.entry("DEF-009", new DefenceCatalogueEntry(
                    "Output-Level Defences",
                    "Active runtime modifications of model responses to degrade the effectiveness of extraction, inversion, and information leakage attacks. Covers confidence reduction, top-k restriction, MemGuard-style perturbation, prediction poisoning, and adaptive misinformation strategies."
            )),
            Map.entry("DEF-010", new DefenceCatalogueEntry(
                    "Resilient Multi-Agent Perception",
                    "ARLOT (Adversarial Resilient Least-squares Object Tracking): a graph-based least-squares fusion method combined with a two-stage association and lifetime management procedure that mitigates adversarially corrupted detections across cooperative V2X agents to produce stable resilient tracks."
            )),
            Map.entry("DEF-011", new DefenceCatalogueEntry(
                    "System and Model Integrity Monitoring",
                    "Continuous validation of deployed artefacts and execution conditions. Includes cryptographic hash verification of model weights and configuration files, runtime monitoring of GPU/memory/latency metrics, configuration integrity tracking, runtime model performance monitoring for drift and confidence anomalies, and statistical output monitoring for parameter tampering detection."
            )),
            Map.entry("DEF-012", new DefenceCatalogueEntry(
                    "Protected Inference Runtime and Telemetry",
                    "Unified runtime infrastructure wrapping deployed models. Covers the Protected Inference Pipeline SDK (modular execution layer integrating gadgets, mitigations, and policy control), the SRME client communication layer for structured event reporting, and the local context-awareness and decision support layer translating telemetry into operational presets."
            )),
            Map.entry("DEF-013", new DefenceCatalogueEntry(
                    "Context-Aware Adaptive Coordination",
                    "Mechanisms enabling distributed learning systems to dynamically adapt to runtime conditions and threats. Combines the decentralized P2P federated learning framework (libp2p-based with local trust and reputation) and the CDS agent (rule-based and reinforcement learning) that maps context vectors into adaptive actions such as client participation, aggregation strategy, and trust weighting."
            )),
            Map.entry("DEF-014", new DefenceCatalogueEntry(
                    "Data Exfiltration Defences",
                    "Defensive techniques against covert extraction of training data hidden inside model weights. Includes weight rounding and LSB stripping methods that neutralize steganography-based exfiltration without degrading model performance."
            ))
    );

    private static final Map<String, String> TOOL_NAMES_BY_ID = Map.ofEntries(
            Map.entry("TOOL-001", "Sec-ml"),
            Map.entry("TOOL-002", "Lidar-shield"),
            Map.entry("TOOL-003", "FL-guard"),
            Map.entry("TOOL-004", "Evolutionary-Defense"),
            Map.entry("TOOL-005", "SecureFL")
    );

    public Optional<String> findAttackName(String attackId) {
        return Optional.ofNullable(ATTACK_NAMES_BY_ID.get(attackId));
    }

    public Optional<DefenceCatalogueEntry> findDefence(String defenceId) {
        return Optional.ofNullable(DEFENCE_BY_ID.get(defenceId));
    }

    public List<DefenceCatalogueEntry> findDefences(List<String> defenceIds) {
        if (defenceIds == null || defenceIds.isEmpty()) {
            return List.of();
        }
        return defenceIds.stream()
                .map(this::findDefence)
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<String> findToolName(String toolId) {
        return Optional.ofNullable(TOOL_NAMES_BY_ID.get(toolId));
    }
}
