package devsecops

/**
 * Semgrep → SonarQube Generic External Issues Mapper
 *
 * DESIGN GOALS:
 * - Stable ruleId (deterministic)
 * - Security-first (Semgrep = VULNERABILITY)
 * - Defensive against malformed offsets
 * - No overlap responsibility with Ruff
 */
class SemgrepExternalSonarMapper implements Serializable {

    /**
     * Map Semgrep severity → Sonar severity
     *
     * Semgrep philosophy:
     *   ERROR   = must-fix
     *   WARNING = real risk, triage allowed
     *   INFO    = contextual
     */
    static Map mapSeverity(String semgrepSeverity) {

        switch (semgrepSeverity?.toUpperCase()) {
            case "ERROR":
                return [severity: "CRITICAL", type: "VULNERABILITY"]
            case "WARNING":
                return [severity: "MAJOR", type: "VULNERABILITY"]
            case "INFO":
                return [severity: "MINOR", type: "VULNERABILITY"]
            default:
                return [severity: "INFO", type: "VULNERABILITY"]
        }
    }

    /**
     * Convert Semgrep JSON results → SonarQube Generic External Issues
     *
     * @param findings   List<Map> parsed from semgrep.json.results
     * @param stripPath  Prefix path to strip (default: /ci-workspace/)
     */
    static Map toSonar(List findings, String stripPath = "/ci-workspace/") {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        findings.each { f ->

            // ---------------------------------------------
            // DEFENSIVE GUARDS
            // ---------------------------------------------
            if (!f?.check_id || !f?.path || !f?.start) {
                return
            }

            String semgrepRuleId = f.check_id
            String sonarRuleId   = "semgrep:${semgrepRuleId}"

            def sev = mapSeverity(f.severity)

            // ---------------------------------------------
            // RULE DEFINITION (ONCE PER RULE)
            // ---------------------------------------------
            if (!rulesIndex.containsKey(sonarRuleId)) {

                String description =
                        f.extra?.message ?:
                        "Semgrep security rule ${semgrepRuleId}"

                rulesIndex[sonarRuleId] = [
                    id          : sonarRuleId,          // REQUIRED
                    engineId    : "semgrep",
                    ruleId      : semgrepRuleId,
                    name        : "Semgrep rule ${semgrepRuleId}",
                    description : description,
                    type        : sev.type,
                    severity    : sev.severity
                ]
            }

            // ---------------------------------------------
            // LOCATION HANDLING (SONAR STRICT)
            // ---------------------------------------------
            int startLine   = f.start?.line ?: 1
            int startColumn = f.start?.col  ?: 1

            int endLine     = f.end?.line   ?: startLine
            int endColumn   = f.end?.col    ?: startColumn

            // Sonar rule: end must be > start
            if (startLine == endLine && endColumn <= startColumn) {
                endColumn = startColumn + 1
            }

            // Keep minimal safe span (avoid invalid offsets)
            if (endColumn > startColumn + 1) {
                endColumn = startColumn + 1
            }

            // ---------------------------------------------
            // ISSUE
            // ---------------------------------------------
            issues << [
                engineId: "semgrep",
                ruleId  : sonarRuleId,
                primaryLocation: [
                    message  : f.extra?.message ?: "Semgrep issue ${semgrepRuleId}",
                    filePath : f.path.replace(stripPath, ""),
                    textRange: [
                        startLine  : startLine,
                        endLine    : endLine,
                        startColumn: startColumn,
                        endColumn  : endColumn
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
