package devsecops

/**
 * Trivy → SonarQube Generic External Issues Mapper
 *
 * Jenkins-safe:
 * - Accepts JSONObject / JSONArray / Map
 * - No hard casting in method signature
 */
class TrivyExternalSonarMapper implements Serializable {

    static Map mapSeverity(String sev) {
        switch (sev?.toUpperCase()) {
            case "CRITICAL": return [severity: "CRITICAL", type: "VULNERABILITY"]
            case "HIGH":     return [severity: "HIGH",    type: "VULNERABILITY"]
            case "MEDIUM":   return [severity: "MEDIUM",    type: "VULNERABILITY"]
            case "LOW":      return [severity: "LOW",     type: "VULNERABILITY"]
            default:         return [severity: "INFO",     type: "VULNERABILITY"]
        }
    }

    /**
     * ENTRY POINT (type-tolerant)
     */
    static Map toSonar(def trivyJson) {

        if (!trivyJson) {
            return [rules: [], issues: []]
        }

        // Normalize Results safely
        def results = trivyJson.Results
        if (!(results instanceof List)) {
            return [rules: [], issues: []]
        }

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        results.each { result ->

            String target = result?.Target ?: "container-image"

            // -------------------------
            // OS-level vulnerabilities
            // -------------------------
            def vulns = result?.Vulnerabilities
            if (vulns instanceof List) {
                vulns.each { v ->
                    processVuln(v, target, rulesIndex, issues, null)
                }
            }

            // -------------------------
            // Package-level vulnerabilities
            // -------------------------
            def packages = result?.Packages
            if (packages instanceof List) {
                packages.each { pkg ->
                    def pkgVulns = pkg?.Vulnerabilities
                    if (pkgVulns instanceof List) {
                        pkgVulns.each { v ->
                            processVuln(v, target, rulesIndex, issues, pkg)
                        }
                    }
                }
            }
        }

        return [
            rules : rulesIndex.values().toList(),
            issues: issues
        ]
    }

    /**
     * Process one vulnerability
     */
    static void processVuln(
        def v,
        String target,
        Map rulesIndex,
        List issues,
        def pkg
    ) {
        if (!v?.VulnerabilityID || !v?.Severity) {
            return
        }

        String sonarRuleId = "trivy:${v.VulnerabilityID}"
        def sev = mapSeverity(v.Severity)

        // -------- RULE --------
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

        // -------- ISSUE --------
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

    static String buildMessage(def v, def pkg) {
        String msg = v.Title ?: v.Description ?: "Trivy vulnerability"
        if (pkg?.InstalledVersion) {
            msg += " | Installed: ${pkg.InstalledVersion}"
        }
        if (v.FixedVersion) {
            msg += " | Fixed in: ${v.FixedVersion}"
        }
        return msg
    }

    static String buildFilePath(def v, String target, def pkg) {
        if (pkg?.Name) {
            return "dependency:${pkg.Name}"
        }
        if (v?.PkgName) {
            return "dependency:${v.PkgName}"
        }
        return "container:${target}"
    }
}
