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

            String ruleId = f.code
            def sev = mapSeverity(ruleId)

            // ===============================
            // RULE (FIX: add mandatory `id`)
            // ===============================
            if (!rulesIndex.containsKey(ruleId)) {
                rulesIndex[ruleId] = [
                    id          : "ruff:${ruleId}",        // 🔥 FIX
                    engineId    : "ruff",
                    ruleId      : ruleId,
                    name        : "Ruff rule ${ruleId}",
                    description : f.message ?: "Ruff rule ${ruleId}",
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
