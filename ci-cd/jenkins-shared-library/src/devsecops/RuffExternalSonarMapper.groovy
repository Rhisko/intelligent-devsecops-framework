package devsecops

class RuffExternalSonarMapper implements Serializable {

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

    static Map toSonar(List findings, String stripPrefix = "/ci-workspace/") {

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        findings.each { f ->

            if (!f?.code || !f?.filename || !f?.location) {
                return
            }

            String ruleKey = f.code
            String sonarRuleId = "ruff:${ruleKey}"
            def sev = mapSeverity(ruleKey)

            // ===============================
            // RULE (id is authoritative)
            // ===============================
            if (!rulesIndex.containsKey(ruleKey)) {
                rulesIndex[ruleKey] = [
                    id          : sonarRuleId,
                    engineId    : "ruff",
                    ruleId      : ruleKey,
                    name        : "Ruff rule ${ruleKey}",
                    description : f.message ?: "Ruff rule ${ruleKey}",
                    type        : sev.type,
                    severity    : sev.severity
                ]
            }

            int startLine   = f.location?.row ?: 1
            int startColumn = f.location?.column ?: 1
            int endLine     = f.end_location?.row ?: startLine
            int endColumn   = f.end_location?.column ?: startColumn

            // Sonar strict requirement
            if (startLine == endLine && startColumn == endColumn) {
                endColumn = startColumn + 1
            }

            // ===============================
            // ISSUE (ruleId MUST reference rules.id)
            // ===============================
            issues << [
                engineId: "ruff",
                ruleId  : sonarRuleId,   // 🔥 FIX UTAMA DI SINI
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
