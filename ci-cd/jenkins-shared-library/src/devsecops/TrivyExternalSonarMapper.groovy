package devsecops

/**
 * Trivy Image Scan → SonarQube Generic External Issues
 *
 * Supports:
 * - container image scan
 * - os-pkgs / language-pkgs
 */
class TrivyExternalSonarMapper implements Serializable {

    static Map mapSeverity(String sev) {
        switch (sev?.toUpperCase()) {
            case "CRITICAL": return [severity: "CRITICAL", type: "VULNERABILITY"]
            case "HIGH":     return [severity: "MAJOR",    type: "VULNERABILITY"]
            case "MEDIUM":   return [severity: "MINOR",    type: "VULNERABILITY"]
            case "LOW":      return [severity: "INFO",     type: "VULNERABILITY"]
            default:         return [severity: "INFO",     type: "VULNERABILITY"]
        }
    }

    static Map toSonar(Map trivyJson) {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        trivyJson?.Results?.each { result ->

            def target = result.Target ?: "container-image"

            result?.Vulnerabilities?.each { v ->

                if (!v?.VulnerabilityID || !v?.Severity) {
                    return
                }

                String ruleId = "trivy:${v.VulnerabilityID}"
                def sev = mapSeverity(v.Severity)

                // ---- RULE (once) ----
                if (!rulesIndex.containsKey(ruleId)) {
                    rulesIndex[ruleId] = [
                        id          : ruleId,
                        engineId    : "trivy",
                        ruleId      : v.VulnerabilityID,
                        name        : "Trivy ${v.VulnerabilityID}",
                        description : v.Description ?: v.Title ?: "Trivy vulnerability",
                        type        : sev.type,
                        severity    : sev.severity
                    ]
                }

                // ---- ISSUE ----
                issues << [
                    engineId: "trivy",
                    ruleId  : ruleId,
                    primaryLocation: [
                        message  : buildMessage(v),
                        filePath : "dependency:${v.PkgName ?: target}",
                        textRange: [
                            startLine  : 1,
                            endLine    : 1,
                            startColumn: 1,
                            endColumn  : 2
                        ]
                    ]
                ]
            }
        }

        return [
            rules : rulesIndex.values().toList(),
            issues: issues
        ]
    }

    static String buildMessage(v) {
        def msg = v.Title ?: v.Description ?: "Trivy vulnerability"
        if (v.FixedVersion) {
            msg += " | Fixed in: ${v.FixedVersion}"
        }
        return msg
    }
}
