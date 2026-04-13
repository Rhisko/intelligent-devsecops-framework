// package devsecops

// class SemgrepExternalSonarMapper implements Serializable {
//     static Map mapSemgrepRule(Map result) {
//         String normalizedSeverity = normalizeSemgrepSeverity(result?.extra?.severity as String)
//         String type = inferSemgrepType(result)

//         return buildSemgrepMapping(type, normalizedSeverity)
//     }

//     static String normalizeSemgrepSeverity(String semgrepSeverity) {
//         switch (semgrepSeverity?.toUpperCase()) {
//             case "ERROR":
//                 return "CRITICAL"
//             case "WARNING":
//                 return "MAJOR"
//             case "INFO":
//                 return "INFO"
//             default:
//                 return "INFO"
//         }
//     }

//     static String inferSemgrepType(Map result) {
//         String checkId = (result?.check_id ?: "").toLowerCase()
//         String category = (result?.extra?.metadata?.category ?: "").toLowerCase()

//         if (category == "security" || checkId.contains(".security.") || checkId.contains("secrets")) {
//             return "VULNERABILITY"
//         }

//         if (checkId.contains(".correctness.") || checkId.contains(".bug.")) {
//             return "BUG"
//         }

//         return "CODE_SMELL"
//     }

//     static Map buildSemgrepMapping(String type, String severity) {
//         switch (type) {
//             case "VULNERABILITY":
//                 return [
//                     type: "VULNERABILITY",
//                     severity: severity,
//                     cleanCodeAttribute: "TRUSTWORTHY",
//                     impacts: [[softwareQuality: "SECURITY", severity: mapImpactSeverity(severity)]]
//                 ]

//             case "BUG":
//                 return [
//                     type: "BUG",
//                     severity: severity == "CRITICAL" ? "MAJOR" : severity,
//                     cleanCodeAttribute: "LOGICAL",
//                     impacts: [[softwareQuality: "RELIABILITY", severity: mapImpactSeverity(severity)]]
//                 ]

//             default:
//                 return [
//                     type: "CODE_SMELL",
//                     severity: severity == "CRITICAL" ? "MAJOR" : severity,
//                     cleanCodeAttribute: "CONVENTIONAL",
//                     impacts: [[softwareQuality: "MAINTAINABILITY", severity: mapImpactSeverity(severity)]]
//                 ]
//         }
//     }

//     static String mapImpactSeverity(String sonarSeverity) {
//         switch (sonarSeverity) {
//             case "CRITICAL":
//                 return "HIGH"
//             case "MAJOR":
//                 return "MEDIUM"
//             case "MINOR":
//                 return "LOW"
//             case "INFO":
//                 return "INFO"
//             default:
//                 return "INFO"
//         }
//     }

//     // static Map mapSeverity(String semgrepSeverity) {
//     //     switch (semgrepSeverity?.toUpperCase()) {
//     //         case "ERROR":
//     //             return [severity: "CRITICAL", type: "VULNERABILITY"]
//     //         case "WARNING":
//     //             return [severity: "HIGH", type: "VULNERABILITY"]
//     //         case "INFO":
//     //             return [severity: "INFO", type: "VULNERABILITY"]
//     //         default:
//     //             return [severity: "INFO", type: "VULNERABILITY"]
//     //     }
//     // }

//     /**
//      * Overload: accept full Semgrep JSON root object
//      */
//     static Map toSonar(Map semgrepJson, String stripPath = "/ci-workspace/") {
//         List findings = (semgrepJson?.results ?: []) as List
//         return toSonar(findings, stripPath)
//     }

//     /**
//      * Convert Semgrep findings list → Sonar Generic External Issues
//      */
//     static Map toSonar(List findings, String stripPath = "/ci-workspace/") {

//         Map<String, Map> rulesIndex = [:]
//         List<Map> issues = []

//         findings.each { f ->
//             if (!f?.check_id || !f?.path || !f?.start) {
//                 return
//             }

//             String semgrepRuleId = f.check_id
//             String sonarRuleId   = "semgrep:${semgrepRuleId}"

//             def sev = mapSeverity(f.severity)

//             if (!rulesIndex.containsKey(sonarRuleId)) {
//                 String description = f.extra?.message ?: "Semgrep security rule ${semgrepRuleId}"

//                 rulesIndex[sonarRuleId] = [
//                     id         : sonarRuleId,
//                     engineId   : "semgrep",
//                     ruleId     : semgrepRuleId,
//                     name       : "Semgrep rule ${semgrepRuleId}",
//                     description: description,
//                     type       : sev.type,
//                     severity   : sev.severity
//                 ]
//             }

//             int startLine   = f.start?.line ?: 1
//             int startColumn = f.start?.col  ?: 1
//             int endLine     = f.end?.line   ?: startLine
//             int endColumn   = f.end?.col    ?: startColumn

//             if (startLine == endLine && endColumn <= startColumn) {
//                 endColumn = startColumn + 1
//             }

//             if (endColumn > startColumn + 1) {
//                 endColumn = startColumn + 1
//             }

//             issues << [
//                 engineId: "semgrep",
//                 ruleId  : sonarRuleId,
//                 primaryLocation: [
//                     message : f.extra?.message ?: "Semgrep issue ${semgrepRuleId}",
//                     filePath: f.path.replace(stripPath, ""),
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

class SemgrepExternalSonarMapper implements Serializable {

    static Map mapSemgrepRule(Map result) {
        String normalizedSeverity = normalizeSemgrepSeverity(result?.extra?.severity as String)
        String type = inferSemgrepType(result)

        return buildSemgrepMapping(type, normalizedSeverity)
    }

    static String normalizeSemgrepSeverity(String semgrepSeverity) {
        switch (semgrepSeverity?.toUpperCase()) {
            case "ERROR":
                return "CRITICAL"
            case "WARNING":
                return "MAJOR"
            case "INFO":
                return "INFO"
            default:
                return "INFO"
        }
    }

    static String inferSemgrepType(Map result) {
        String checkId = (result?.check_id ?: "").toLowerCase()
        String category = (result?.extra?.metadata?.category ?: "").toLowerCase()

        if (category == "security" || checkId.contains(".security.") || checkId.contains("secrets")) {
            return "VULNERABILITY"
        }

        if (checkId.contains(".correctness.") || checkId.contains(".bug.")) {
            return "BUG"
        }

        return "CODE_SMELL"
    }

    static Map buildSemgrepMapping(String type, String severity) {
        switch (type) {
            case "VULNERABILITY":
                return [
                    type: "VULNERABILITY",
                    severity: severity,
                    cleanCodeAttribute: "TRUSTWORTHY",
                    impacts: [[softwareQuality: "SECURITY", severity: mapImpactSeverity(severity)]]
                ]

            case "BUG":
                return [
                    type: "BUG",
                    severity: severity == "CRITICAL" ? "MAJOR" : severity,
                    cleanCodeAttribute: "LOGICAL",
                    impacts: [[softwareQuality: "RELIABILITY", severity: mapImpactSeverity(severity)]]
                ]

            default:
                return [
                    type: "CODE_SMELL",
                    severity: severity == "CRITICAL" ? "MAJOR" : severity,
                    cleanCodeAttribute: "CONVENTIONAL",
                    impacts: [[softwareQuality: "MAINTAINABILITY", severity: mapImpactSeverity(severity)]]
                ]
        }
    }

    static String mapImpactSeverity(String sonarSeverity) {
        switch (sonarSeverity) {
            case "CRITICAL":
                return "HIGH"
            case "MAJOR":
                return "MEDIUM"
            case "MINOR":
                return "LOW"
            case "INFO":
                return "INFO"
            default:
                return "INFO"
        }
    }

    static Map toSonar(Map semgrepJson, String stripPath = "/ci-workspace/") {
        List findings = (semgrepJson?.results ?: []) as List
        return toSonar(findings, stripPath)
    }

    static Map toSonar(List findings, String stripPath = "/ci-workspace/") {
        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []

        findings.each { f ->
            if (!f?.check_id || !f?.path || !f?.start) {
                return
            }

            String semgrepRuleId = f.check_id as String
            String sonarRuleId = "semgrep:${semgrepRuleId}"

            Map mapped = mapSemgrepRule(f)

            if (!rulesIndex.containsKey(sonarRuleId)) {
                String description = f?.extra?.message ?: "Semgrep rule ${semgrepRuleId}"

                rulesIndex[sonarRuleId] = [
                    id                : sonarRuleId,
                    engineId          : "semgrep",
                    name              : "Semgrep rule ${semgrepRuleId}",
                    description       : description,
                    cleanCodeAttribute: mapped.cleanCodeAttribute,
                    type              : mapped.type,
                    severity          : mapped.severity,
                    impacts           : mapped.impacts
                ]
            }

            // int startLine   = (f?.start?.line ?: 1) as int
            // int startColumn = (f?.start?.col ?: 1) as int
            // int endLine     = (f?.end?.line ?: startLine) as int
            // int endColumn   = (f?.end?.col ?: startColumn) as int
            int startLine = (f?.start?.line ?: 1) as int
            int startColumn = Math.max(1, (f?.start?.col ?: 1) as int)

            int endLine = startLine
            int endColumn = startColumn + 1

            if (endLine == startLine && endColumn <= startColumn) {
                endColumn = startColumn + 1
            }

            issues << [
                ruleId: sonarRuleId,
                primaryLocation: [
                    message : f?.extra?.message ?: "Semgrep issue ${semgrepRuleId}",
                    filePath: normalizePath(f.path as String, stripPath),
                    textRange: [
                        startLine  : startLine,
                        startColumn: startColumn,
                        endLine    : endLine,
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

    static String normalizePath(String rawPath, String stripPath = "/ci-workspace/") {
        if (!rawPath) {
            return ""
        }
        return rawPath.startsWith(stripPath) ? rawPath.substring(stripPath.length()) : rawPath
    }
}