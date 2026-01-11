package devsecops

/**
 * Ruff → SonarQube Generic External Issues Mapper
 *
 * FINAL, STABLE, SONARQUBE-COMPLIANT
 */
class RuffExternalSonarMapper implements Serializable {

    /**
     * Map Ruff rule code → Sonar severity & type
     */
    static Map mapSeverity(String code) {
        if (code?.startsWith("S")) {
            return [severity: "CRITICAL", type: "VULNERABILITY"]
        }
        if (code?.startsWith("B") || code?.startsWith("F")) {
            return [severity: "MAJOR", type: "BUG"]
        }
        if (code?.startsWith("E") ||
            code?.startsWith("W") ||
            code?.startsWith("I")) {
            return [severity: "MINOR", type: "CODE_SMELL"]
        }
        return [severity: "INFO", type: "CODE_SMELL"]
    }

    /**
     * Convert Ruff findings → SonarQube Generic External Issues (NEW FORMAT)
     *
     * @param findings   List<Map> parsed from ruff.json
     * @param stripPath  Prefix path to strip (default: /ci-workspace/)
     */
    static Map toSonar(List findings, String stripPath = "/ci-workspace/") {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        findings.each { f ->

            // Skip malformed entries defensively
            if (!f?.code || !f?.filename || !f?.location) {
                return
            }

            String ruffRule = f.code
            String sonarRuleId = "ruff:${ruffRule}"
            def sev = mapSeverity(ruffRule)

            // --------------------------------------------------
            // RULE (defined once, referenced by issues.ruleId)
            // --------------------------------------------------
            if (!rulesIndex.containsKey(ruffRule)) {
                rulesIndex[ruffRule] = [
                    id          : sonarRuleId,          // MANDATORY
                    engineId    : "ruff",
                    ruleId      : ruffRule,
                    name        : "Ruff rule ${ruffRule}",
                    description : f.message ?: "Ruff rule ${ruffRule}",
                    type        : sev.type,
                    severity    : sev.severity
                ]
            }

            // --------------------------------------------------
            // LOCATION (SONAR-STRICT & DEFENSIVE)
            // --------------------------------------------------
            int startLine   = f.location?.row ?: 1
            int startColumn = f.location?.column ?: 1

            int endLine     = f.end_location?.row ?: startLine
            int endColumn   = f.end_location?.column ?: startColumn

            // RULE 1: start < end (mandatory)
            if (startLine == endLine && endColumn <= startColumn) {
                endColumn = startColumn + 1
            }

            // RULE 2: clamp column span defensively
            // Sonar validates against actual line length
            // We do NOT know file content → keep minimal safe span
            if (endColumn > startColumn + 1) {
                endColumn = startColumn + 1
            }

            // --------------------------------------------------
            // ISSUE (ruleId MUST reference rules.id)
            // --------------------------------------------------
            issues << [
                engineId: "ruff",
                ruleId  : sonarRuleId,
                primaryLocation: [
                    message  : f.message ?: "Ruff issue ${ruffRule}",
                    filePath : f.filename.replace(stripPath, ""),
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
