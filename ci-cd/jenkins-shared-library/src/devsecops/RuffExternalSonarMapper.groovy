package devsecops

class RuffExternalSonarMapper implements Serializable {

    /**
     * Severity & Type policy (SonarQube)
     */
    static Map mapSeverity(String code) {
        if (code?.startsWith("S")) {
            return [severity: "CRITICAL", type: "VULNERABILITY"]
        }
        if (code?.startsWith("B") || code?.startsWith("F")) {
            return [severity: "MAJOR", type: "BUG"]
        }
        if (code?.startsWith("E") || code?.startsWith("W") || code?.startsWith("I")) {
            return [severity: "MINOR", type: "CODE_SMELL"]
        }
        return [severity: "INFO", type: "CODE_SMELL"]
    }

    /**
     * Convert Ruff findings → Sonar Generic External Issues (NEW FORMAT)
     */
    static Map toSonar(List findings, String stripPrefix = "/ci-workspace/") {

        // RULES (deduplicated by ruleId)
        Map<String, Map> rulesIndex = [:]

        // ISSUES
        List<Map> issues = []

        findings.each { f ->

            if (!f?.code || !f?.filename || !f?.location) {
                return
            }

            def sev = mapSeverity(f.code)
            def ruleId = f.code

            // ----------------------------
            // RULE (defined once per ruleId)
            // ----------------------------
            if (!rulesIndex.containsKey(ruleId)) {
                rulesIndex[ruleId] = [
                    engineId   : "ruff",
                    ruleId     : ruleId,
                    name       : "Ruff rule ${ruleId}",
                    description: f.message ?: "Ruff rule ${ruleId}",
                    type       : sev.type,
                    severity   : sev.severity
                ]
            }

            // ----------------------------
            // LOCATION (Sonar strict fix)
            // ----------------------------
            int startLine   = f.location?.row ?: 1
            int startColumn = f.location?.column ?: 1
            int endLine     = f.end_location?.row ?: startLine
            int endColumn   = f.end_location?.column ?: startColumn

            // CRITICAL FIX: Sonar requires start < end
            if (startLine == endLine && startColumn == endColumn) {
                endColumn = startColumn + 1
            }

            // ----------------------------
            // ISSUE (no severity/type here!)
            // ----------------------------
            issues << [
                engineId: "ruff",
                ruleId  : ruleId,
                primaryLocation: [
                    message  : f.message,
                    filePath : f.filename.replace(stripPrefix, ""),
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
