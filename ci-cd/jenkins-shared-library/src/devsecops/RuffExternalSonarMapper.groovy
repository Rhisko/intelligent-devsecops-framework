package devsecops

class RuffExternalSonarMapper implements Serializable {

    /**
     * Severity & Type policy (SonarQube)
     */
    static Map mapSeverity(String code) {
        if (code.startsWith("S")) {
            return [severity: "CRITICAL", type: "VULNERABILITY"]
        }
        if (code.startsWith("B") || code.startsWith("F")) {
            return [severity: "MAJOR", type: "BUG"]
        }
        if (code.startsWith("E") || code.startsWith("W") || code.startsWith("I")) {
            return [severity: "MINOR", type: "CODE_SMELL"]
        }
        return [severity: "INFO", type: "CODE_SMELL"]
    }

    /**
     * Convert Ruff findings (sample format) → Sonar Generic External Issues
     *
     * @param findings List of Ruff issues (parsed JSON)
     * @param stripPrefix Prefix path to remove (default: /ci-workspace/)
     */
    static Map toSonar(List findings, String stripPrefix = "/ci-workspace/") {

        def issues = []

        findings.each { f ->
            def sev = mapSeverity(f.code)

            issues << [
                engineId: "ruff",
                ruleId: f.code,
                severity: sev.severity,
                type: sev.type,
                primaryLocation: [
                    message: f.message,
                    filePath: f.filename.replace(stripPrefix, ""),
                    textRange: [
                        startLine: f.location?.row ?: 1,
                        endLine: f.end_location?.row ?: (f.location?.row ?: 1),
                        startColumn: f.location?.column ?: 1,
                        endColumn: f.end_location?.column ?: (f.location?.column ?: 1)
                    ]
                ]
            ]
        }

        return [issues: issues]
    }
}
