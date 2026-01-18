package devsecops

/**
 * Trivy → SonarQube Generic External Issues Mapper
 *
 * DESIGN PRINCIPLES:
 * - Trivy severity is authoritative (CVSS-based)
 * - Deterministic ruleId (CVE / GHSA)
 * - SonarQube Generic External Issues compliant
 * - Defensive offsets (Sonar strict)
 */
class TrivyExternalSonarMapper implements Serializable {

    /**
     * Map Trivy severity → Sonar severity
     */
    static Map mapSeverity(String trivySeverity) {
        switch (trivySeverity?.toUpperCase()) {
            case "CRITICAL":
                return [severity: "CRITICAL", type: "VULNERABILITY"]
            case "HIGH":
                return [severity: "MAJOR", type: "VULNERABILITY"]
            case "MEDIUM":
                return [severity: "MINOR", type: "VULNERABILITY"]
            case "LOW":
                return [severity: "INFO", type: "VULNERABILITY"]
            default:
                return [severity: "INFO", type: "VULNERABILITY"]
        }
    }

    /**
     * Convert Trivy findings → SonarQube Generic External Issues
     *
     * @param findings  List<Map> (normalized Trivy findings)
     * @param defaultPath fallback file path (Dockerfile / image)
     */
    static Map toSonar(List findings, String defaultPath = "Dockerfile") {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        findings.each { f ->

            // -------------------------------
            // DEFENSIVE CHECKS
            // -------------------------------
            if (!f?.rule_id || !f?.severity) {
                return
            }

            String vulnId      = f.rule_id
            String sonarRuleId = "trivy:${vulnId}"

            def sev = mapSeverity(f.severity)

            // -------------------------------
            // RULE DEFINITION
            // -------------------------------
            if (!rulesIndex.containsKey(sonarRuleId)) {
                rulesIndex[sonarRuleId] = [
                    id          : sonarRuleId,          // REQUIRED
                    engineId    : "trivy",
                    ruleId      : vulnId,
                    name        : "Trivy vulnerability ${vulnId}",
                    description : f.message ?: "Trivy vulnerability ${vulnId}",
                    type        : sev.type,
                    severity    : sev.severity
                ]
            }

            // -------------------------------
            // LOCATION (SONAR SAFE)
            // -------------------------------
            String filePath =
                    f.component ? "dependency:${f.component}" :
                    f.artifact  ? "image:${f.artifact}" :
                    defaultPath

            issues << [
                engineId: "trivy",
                ruleId  : sonarRuleId,
                primaryLocation: [
                    message  : f.recommendation ?: f.message ?: "Trivy finding ${vulnId}",
                    filePath : filePath,
                    textRange: [
                        startLine  : 1,
                        endLine    : 1,
                        startColumn: 1,
                        endColumn  : 2
                    ]
                ]
            ]
        }

        return [
            rules : rulesIndex.values().toList(),
            issues: issues
        ]
    }
}
