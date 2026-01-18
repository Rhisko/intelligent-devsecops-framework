package devsecops

/**
 * Trivy → SonarQube Generic External Issues Mapper
 *
 * SUPPORTED:
 * - Container image scan
 * - OS packages (os-pkgs)
 * - Language packages (lang-pkgs)
 *
 * DESIGN PRINCIPLES:
 * - Follow Trivy JSON v2 schema (SchemaVersion 2)
 * - Defensive parsing (never assume fields exist)
 * - Stable ruleId (CVE / GHSA)
 * - Trivy severity is SOURCE OF TRUTH
 */
class TrivyExternalSonarMapper implements Serializable {

    /**
     * Trivy severity → Sonar severity
     */
    static Map mapSeverity(String sev) {
        switch (sev?.toUpperCase()) {
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
     * Convert Trivy JSON → SonarQube External Issues format
     *
     * @param trivyJson Parsed JSON (Map) from trivy --format json
     */
    static Map toSonar(Map trivyJson) {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        // -------------------------------
        // DEFENSIVE ROOT CHECK
        // -------------------------------
        if (!trivyJson?.Results || !(trivyJson.Results instanceof List)) {
            return [rules: [], issues: []]
        }

        trivyJson.Results.each { result ->

            String target = result.Target ?: "container-image"

            // ======================================================
            // OS & LANGUAGE PACKAGE VULNERABILITIES
            // ======================================================
            result?.Packages?.each { pkg ->

                String componentName =
                        pkg.Name ?:
                        pkg.SrcName ?:
                        target

                pkg?.Vulnerabilities?.each { v ->

                    if (!v?.VulnerabilityID || !v?.Severity) {
                        return
                    }

                    String ruleId = "trivy:${v.VulnerabilityID}"
                    def sev = mapSeverity(v.Severity)

                    // -------------------------------
                    // RULE (ONCE PER CVE / GHSA)
                    // -------------------------------
                    if (!rulesIndex.containsKey(ruleId)) {
                        rulesIndex[ruleId] = [
                            id          : ruleId,                     // REQUIRED
                            engineId    : "trivy",
                            ruleId      : v.VulnerabilityID,
                            name        : "Trivy ${v.VulnerabilityID}",
                            description : v.Description
                                    ?: v.Title
                                    ?: "Trivy detected vulnerability ${v.VulnerabilityID}",
                            type        : sev.type,
                            severity    : sev.severity
                        ]
                    }

                    // -------------------------------
                    // ISSUE INSTANCE
                    // -------------------------------
                    issues << [
                        engineId: "trivy",
                        ruleId  : ruleId,
                        primaryLocation: [
                            message  : buildMessage(v, pkg),
                            filePath : "dependency:${componentName}",
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
        }

        return [
            rules : rulesIndex.values().toList(),
            issues: issues
        ]
    }

    /**
     * Human-readable issue message
     */
    static String buildMessage(v, pkg) {
        String msg =
                v.Title ?:
                v.Description ?:
                "Trivy vulnerability ${v.VulnerabilityID}"

        if (pkg?.InstalledVersion) {
            msg += " | Installed: ${pkg.InstalledVersion}"
        }

        if (v.FixedVersion && v.FixedVersion != "N/A") {
            msg += " | Fixed in: ${v.FixedVersion}"
        }

        if (v.PrimaryURL) {
            msg += " | Ref: ${v.PrimaryURL}"
        }

        return msg
    }
}
