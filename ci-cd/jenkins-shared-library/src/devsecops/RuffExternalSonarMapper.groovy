// package devsecops

// /**
//  * Ruff → SonarQube Generic External Issues Mapper
//  *
//  * FINAL, STABLE, SONARQUBE-COMPLIANT
//  */
// class RuffExternalSonarMapper implements Serializable {

//     /**
//      * Map Ruff rule code → Sonar severity & type
//      */
//     static Map mapSeverity(String code) {
//         if (code?.startsWith("S")) {
//             return [severity: "CRITICAL", type: "VULNERABILITY"]
//         }
//         if (code?.startsWith("B") || code?.startsWith("F")) {
//             return [severity: "MAJOR", type: "BUG"]
//         }
//         if (code?.startsWith("E") ||
//             code?.startsWith("W") ||
//             code?.startsWith("I")) {
//             return [severity: "MINOR", type: "CODE_SMELL"]
//         }
//         return [severity: "INFO", type: "CODE_SMELL"]
//     }

//     /**
//      * Convert Ruff findings → SonarQube Generic External Issues (NEW FORMAT)
//      *
//      * @param findings   List<Map> parsed from ruff.json
//      * @param stripPath  Prefix path to strip (default: /ci-workspace/)
//      */
//     static Map toSonar(List findings, String stripPath = "/ci-workspace/") {

//         Map<String, Map> rulesIndex = [:]
//         List<Map> issues = []

//         findings.each { f ->

//             // Skip malformed entries defensively
//             if (!f?.code || !f?.filename || !f?.location) {
//                 return
//             }

//             String ruffRule = f.code
//             String sonarRuleId = "ruff:${ruffRule}"
//             def sev = mapSeverity(ruffRule)

//             // --------------------------------------------------
//             // RULE (defined once, referenced by issues.ruleId)
//             // --------------------------------------------------
//             if (!rulesIndex.containsKey(ruffRule)) {
//                 rulesIndex[ruffRule] = [
//                     id          : sonarRuleId,          // MANDATORY
//                     engineId    : "ruff",
//                     ruleId      : ruffRule,
//                     name        : "Ruff rule ${ruffRule}",
//                     description : f.message ?: "Ruff rule ${ruffRule}",
//                     type        : sev.type,
//                     severity    : sev.severity
//                 ]
//             }

//             // --------------------------------------------------
//             // LOCATION (SONAR-STRICT & DEFENSIVE)
//             // --------------------------------------------------
//             int startLine   = f.location?.row ?: 1
//             int startColumn = f.location?.column ?: 1

//             int endLine     = f.end_location?.row ?: startLine
//             int endColumn   = f.end_location?.column ?: startColumn

//             // RULE 1: start < end (mandatory)
//             if (startLine == endLine && endColumn <= startColumn) {
//                 endColumn = startColumn + 1
//             }

//             // RULE 2: clamp column span defensively
//             // Sonar validates against actual line length
//             // We do NOT know file content → keep minimal safe span
//             if (endColumn > startColumn + 1) {
//                 endColumn = startColumn + 1
//             }

//             // --------------------------------------------------
//             // ISSUE (ruleId MUST reference rules.id)
//             // --------------------------------------------------
//             issues << [
//                 engineId: "ruff",
//                 ruleId  : sonarRuleId,
//                 primaryLocation: [
//                     message  : f.message ?: "Ruff issue ${ruffRule}",
//                     filePath : f.filename.replace(stripPath, ""),
//                     textRange: [
//                         startLine  : startLine,
//                         endLine    : endLine,
//                         startColumn: startColumn,
//                         endColumn  : endColumn
//                     ]
//                 ]
//             ]
//         }

//         return [
//             rules : rulesIndex.values().toList(),
//             issues: issues
//         ]
//     }
// }

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

    static Map toSonar(List findings, String stripPath = "/ci-workspace/") {
        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        findings.each { f ->
            if (!f?.code || !f?.filename || !f?.location?.row) {
                return
            }

            String ruffRule = f.code
            String sonarRuleId = "ruff:${ruffRule}"
            def sev = mapSeverity(ruffRule)

            if (!rulesIndex.containsKey(sonarRuleId)) {
                rulesIndex[sonarRuleId] = [
                    id          : sonarRuleId,
                    engineId    : "ruff",
                    ruleId      : ruffRule,
                    name        : "Ruff rule ${ruffRule}",
                    description : f.message ?: "Ruff rule ${ruffRule}",
                    type        : sev.type,
                    severity    : sev.severity
                ]
            }

            String originalPath = f.filename as String
            String relativePath = originalPath.replace(stripPath, "")

            Map safeRange = buildSafeTextRange(
                originalPath,
                f.location,
                f.end_location
            )

            Map primaryLocation = [
                message : f.message ?: "Ruff issue ${ruffRule}",
                filePath: relativePath
            ]

            if (safeRange) {
                primaryLocation.textRange = safeRange
            }

            issues << [
                engineId: "ruff",
                ruleId  : sonarRuleId,
                primaryLocation: primaryLocation
            ]
        }

        return [
            rules : rulesIndex.values().toList(),
            issues: issues
        ]
    }

    static Map buildSafeTextRange(String filePath, def startLoc, def endLoc) {
        File file = new File(filePath)
        if (!file.exists()) {
            int startLine = Math.max((startLoc?.row ?: 1) as int, 1)
            int endLine   = Math.max((endLoc?.row ?: startLine) as int, startLine)

            return [
                startLine  : startLine,
                endLine    : endLine,
                startColumn: 0,
                endColumn  : 1
            ]
        }

        List<String> lines = file.readLines("UTF-8")

        int startLine = Math.max((startLoc?.row ?: 1) as int, 1)
        int endLine   = Math.max((endLoc?.row ?: startLine) as int, startLine)

        if (startLine > lines.size()) {
            startLine = lines.size() > 0 ? lines.size() : 1
        }
        if (endLine > lines.size()) {
            endLine = startLine
        }

        int startLineLen = getLineLength(lines, startLine)
        int endLineLen   = getLineLength(lines, endLine)

        int startCol1 = (startLoc?.column ?: 1) as int
        int endCol1   = (endLoc?.column ?: (startCol1 + 1)) as int

        int startColumn = Math.max(startCol1 - 1, 0)
        int endColumnRaw = endCol1 - 1
        int endColumn = Math.max(endColumnRaw, startColumn + 1)

        if (startLineLen == 0) {
            return [
                startLine: startLine,
                endLine  : startLine
            ]
        }

        startColumn = Math.min(startColumn, Math.max(startLineLen - 1, 0))

        if (startLine == endLine) {
            endColumn = Math.min(endColumn, startLineLen)

            if (endColumn <= startColumn) {
                endColumn = Math.min(startColumn + 1, startLineLen)
            }

            return [
                startLine  : startLine,
                endLine    : endLine,
                startColumn: startColumn,
                endColumn  : endColumn
            ]
        }

        if (endLineLen == 0) {
            return [
                startLine  : startLine,
                endLine    : startLine,
                startColumn: startColumn,
                endColumn  : Math.min(startColumn + 1, startLineLen)
            ]
        }

        endColumn = Math.min(Math.max(endColumn, 1), endLineLen)

        return [
            startLine  : startLine,
            endLine    : endLine,
            startColumn: startColumn,
            endColumn  : endColumn
        ]
    }

    static int getLineLength(List<String> lines, int lineNumber) {
        int idx = lineNumber - 1
        if (idx < 0 || idx >= lines.size()) {
            return 0
        }
        return lines[idx]?.size() ?: 0
    }
}
