package devsecops

/**
 * Trivy → SonarQube Generic External Issues Mapper
 *
 * Supports:
 * - OS packages (debian, alpine, etc.)
 * - Language packages (pip, npm, maven, etc.)
 * - CVE / GHSA
 *
 * Trivy severity is authoritative (CVSS-based)
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

    /**
     * Entry point
     */
    static Map toSonar(Map trivyJson) {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        trivyJson?.Results?.each { result ->

            String target = result.Target ?: "container-image"

            // ------------------------------
            // 1. Direct vulnerabilities (OS)
            // ------------------------------
            result?.Vulnerabilities?.each { v ->
                processVuln(v, target, rulesIndex, issues)
            }

            // ------------------------------
            // 2. Package-level vulnerabilities (Lang)
            // ------------------------------
            result?.Packages?.each { pkg ->
                pkg?.Vulnerabilities?.each { v ->
                    processVuln(v, "${pkg.Name}", rulesIndex, issues, pkg)
                }
            }
        }

        return [
            rules : rulesIndex.values().toList(),
            issues: issues
        ]
    }

    /**
     * Normalize one Trivy vulnerability → Sonar rule + issue
     */
    static void processVuln(
        Map v,
        String target,
        Map rulesIndex,
        List issues,
        Map pkg = null
    ) {
        if (!v?.VulnerabilityID || !v?.Severity) {
            return
        }

        String sonarRuleId = "trivy:${v.VulnerabilityID}"
        def sev = mapSeverity(v.Severity)

        // ---------- RULE ----------
        if (!rulesIndex.containsKey(sonarRuleId)) {
            rulesIndex[sonarRuleId] = [
                id          : sonarRuleId,
                engineId    : "trivy",
                ruleId      : v.VulnerabilityID,
                name        : "Trivy ${v.VulnerabilityID}",
                description : v.Title ?: v.Description ?: "Trivy vulnerability",
                type        : sev.type,
                severity    : sev.severity
            ]
        }

        // ---------- ISSUE ----------
        issues << [
            engineId: "trivy",
            ruleId  : sonarRuleId,
            primaryLocation: [
                message  : buildMessage(v, pkg),
                filePath : buildFilePath(v, target, pkg),
                textRange: [
                    startLine  : 1,
                    endLine    : 1,
                    startColumn: 1,
                    endColumn  : 2
                ]
            ]
        ]
    }

    static String buildMessage(Map v, Map pkg) {
        String msg = v.Title ?: v.Description ?: "Trivy vulnerability"
        if (pkg?.InstalledVersion) {
            msg += " | Installed: ${pkg.InstalledVersion}"
        }
        if (v.FixedVersion) {
            msg += " | Fixed in: ${v.FixedVersion}"
        }
        return msg
    }

    static String buildFilePath(Map v, String target, Map pkg) {
        if (pkg?.Name) {
            return "dependency:${pkg.Name}"
        }
        if (v.PkgName) {
            return "dependency:${v.PkgName}"
        }
        return "container:${target}"
    }
}
