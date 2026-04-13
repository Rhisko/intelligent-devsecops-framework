// package devsecops

// /**
//  * Trivy → SonarQube Generic External Issues Mapper
//  *
//  * Jenkins-safe:
//  * - Accepts JSONObject / JSONArray / Map
//  * - No hard casting in method signature
//  */
// class TrivyExternalSonarMapper implements Serializable {

//     static Map mapSeverity(String sev) {
//         switch (sev?.toUpperCase()) {
//             case "CRITICAL": return [severity: "CRITICAL", type: "VULNERABILITY"]
//             case "HIGH":     return [severity: "HIGH",    type: "VULNERABILITY"]
//             case "MEDIUM":   return [severity: "MEDIUM",    type: "VULNERABILITY"]
//             case "LOW":      return [severity: "LOW",     type: "VULNERABILITY"]
//             default:         return [severity: "INFO",     type: "VULNERABILITY"]
//         }
//     }

//     /**
//      * ENTRY POINT (type-tolerant)
//      */
//     static Map toSonar(def trivyJson) {

//         if (!trivyJson) {
//             return [rules: [], issues: []]
//         }

//         // Normalize Results safely
//         def results = trivyJson.Results
//         if (!(results instanceof List)) {
//             return [rules: [], issues: []]
//         }

//         Map<String, Map> rulesIndex = [:]
//         List<Map> issues = []

//         results.each { result ->

//             String target = result?.Target ?: "container-image"

//             // -------------------------
//             // OS-level vulnerabilities
//             // -------------------------
//             def vulns = result?.Vulnerabilities
//             if (vulns instanceof List) {
//                 vulns.each { v ->
//                     processVuln(v, target, rulesIndex, issues, null)
//                 }
//             }

//             // -------------------------
//             // Package-level vulnerabilities
//             // -------------------------
//             def packages = result?.Packages
//             if (packages instanceof List) {
//                 packages.each { pkg ->
//                     def pkgVulns = pkg?.Vulnerabilities
//                     if (pkgVulns instanceof List) {
//                         pkgVulns.each { v ->
//                             processVuln(v, target, rulesIndex, issues, pkg)
//                         }
//                     }
//                 }
//             }
//         }

//         return [
//             rules : rulesIndex.values().toList(),
//             issues: issues
//         ]
//     }

//     /**
//      * Process one vulnerability
//      */
//     static void processVuln(
//         def v,
//         String target,
//         Map rulesIndex,
//         List issues,
//         def pkg
//     ) {
//         if (!v?.VulnerabilityID || !v?.Severity) {
//             return
//         }

//         String sonarRuleId = "trivy:${v.VulnerabilityID}"
//         def sev = mapSeverity(v.Severity)

//         // -------- RULE --------
//         if (!rulesIndex.containsKey(sonarRuleId)) {
//             rulesIndex[sonarRuleId] = [
//                 id          : sonarRuleId,
//                 engineId    : "trivy",
//                 ruleId      : v.VulnerabilityID,
//                 name        : "Trivy ${v.VulnerabilityID}",
//                 description : v.Title ?: v.Description ?: "Trivy vulnerability",
//                 type        : sev.type,
//                 severity    : sev.severity
//             ]
//         }

//         // -------- ISSUE --------
//         issues << [
//             engineId: "trivy",
//             ruleId  : sonarRuleId,
//             primaryLocation: [
//                 message  : buildMessage(v, pkg),
//                 filePath : buildFilePath(v, target, pkg),
//                 textRange: [
//                     startLine  : 1,
//                     endLine    : 1,
//                     startColumn: 1,
//                     endColumn  : 2
//                 ]
//             ]
//         ]
//     }

//     static String buildMessage(def v, def pkg) {
//         String msg = v.Title ?: v.Description ?: "Trivy vulnerability"
//         if (pkg?.InstalledVersion) {
//             msg += " | Installed: ${pkg.InstalledVersion}"
//         }
//         if (v.FixedVersion) {
//             msg += " | Fixed in: ${v.FixedVersion}"
//         }
//         return msg
//     }

//     static String buildFilePath(def v, String target, def pkg) {
//         if (pkg?.Name) {
//             return "dependency:${pkg.Name}"
//         }
//         if (v?.PkgName) {
//             return "dependency:${v.PkgName}"
//         }
//         return "container:${target}"
//     }
// }

package devsecops

/**
 * Trivy -> SonarQube Generic External Issues Mapper
 *
 * Goal:
 * - Accept Map / JSONObject-like payload from Jenkins shared library runtime
 * - Normalize Trivy severities to Sonar-compatible severity
 * - Map OS package CVEs to Dockerfile
 * - Map Python dependency CVEs to requirements.txt
 * - Deduplicate repeated CVEs on the same logical file
 *
 * Notes:
 * - Sonar generic external issues expects rule severity values like:
 *   CRITICAL, MAJOR, MINOR, INFO
 * - Trivy severities HIGH/MEDIUM/LOW are preserved in impacts
 */
class TrivyExternalSonarMapper implements Serializable {

    /**
     * Entry point.
     */
    static Map toSonar(def trivyJson) {
        if (!trivyJson) {
            return [rules: [], issues: []]
        }

        def results = trivyJson?.Results
        if (!(results instanceof List)) {
            return [rules: [], issues: []]
        }

        Map<String, Map> rulesIndex = [:]
        List<Map> issues = []
        Set<String> issueKeys = new LinkedHashSet<>()

        results.each { result ->
            String target = (result?.Target ?: "container-image") as String

            // Top-level vulnerabilities
            def vulns = result?.Vulnerabilities
            if (vulns instanceof List) {
                vulns.each { v ->
                    processVulnerability(v, result, target, null, rulesIndex, issues, issueKeys)
                }
            }

            // Package-level vulnerabilities
            def packages = result?.Packages
            if (packages instanceof List) {
                packages.each { pkg ->
                    def pkgVulns = pkg?.Vulnerabilities
                    if (pkgVulns instanceof List) {
                        pkgVulns.each { v ->
                            processVulnerability(v, result, target, pkg, rulesIndex, issues, issueKeys)
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
     * Process a single vulnerability record.
     */
    static void processVulnerability(
        def vuln,
        def result,
        String target,
        def pkg,
        Map<String, Map> rulesIndex,
        List<Map> issues,
        Set<String> issueKeys
    ) {
        if (!vuln?.VulnerabilityID || !vuln?.Severity) {
            return
        }

        String vulnerabilityId = (vuln.VulnerabilityID ?: "") as String
        String sonarRuleId = "trivy:${vulnerabilityId}"
        Map mapped = mapSeverity(vuln.Severity as String)

        if (!rulesIndex.containsKey(sonarRuleId)) {
            rulesIndex[sonarRuleId] = [
                id                : sonarRuleId,
                engineId          : "trivy",
                name              : "Trivy ${vulnerabilityId}",
                description       : normalizeDescription(vuln),
                cleanCodeAttribute: mapped.cleanCodeAttribute,
                type              : mapped.type,
                severity          : mapped.severity,
                impacts           : mapped.impacts
            ]
        }

        String filePath = buildFilePath(result, target, pkg, vuln)
        String issueKey = "${sonarRuleId}::${filePath}"

        if (issueKeys.contains(issueKey)) {
            return
        }
        issueKeys << issueKey

        issues << [
            ruleId       : sonarRuleId,
            effortMinutes: estimateEffortMinutes(vuln),
            primaryLocation: [
                message : buildMessage(vuln, pkg),
                filePath: filePath,
                textRange: [
                    startLine  : 1,
                    endLine    : 1,
                    startColumn: 1,
                    endColumn  : 2
                ]
            ]
        ]
    }

    /**
     * Severity mapping:
     * Trivy -> Sonar rule severity + impact severity
     */
    static Map mapSeverity(String sev) {
        switch ((sev ?: "").toUpperCase()) {
            case "CRITICAL":
                return ruleMapping("VULNERABILITY", "CRITICAL", "TRUSTWORTHY", "SECURITY", "HIGH")
            case "HIGH":
                return ruleMapping("VULNERABILITY", "MAJOR", "TRUSTWORTHY", "SECURITY", "HIGH")
            case "MEDIUM":
                return ruleMapping("VULNERABILITY", "MINOR", "TRUSTWORTHY", "SECURITY", "MEDIUM")
            case "LOW":
                return ruleMapping("VULNERABILITY", "INFO", "TRUSTWORTHY", "SECURITY", "LOW")
            default:
                return ruleMapping("VULNERABILITY", "INFO", "TRUSTWORTHY", "SECURITY", "INFO")
        }
    }

    static Map ruleMapping(
        String type,
        String severity,
        String cleanCodeAttribute,
        String softwareQuality,
        String impactSeverity
    ) {
        return [
            type              : type,
            severity          : severity,
            cleanCodeAttribute: cleanCodeAttribute,
            impacts           : [[
                softwareQuality: softwareQuality,
                severity       : impactSeverity
            ]]
        ]
    }

    /**
     * Build human-readable description for Sonar rule.
     */
    static String normalizeDescription(def vuln) {
        return (vuln?.Title ?: vuln?.Description ?: "Trivy vulnerability") as String
    }

    /**
     * Build message for issue location.
     */
    static String buildMessage(def vuln, def pkg) {
        List<String> parts = []

        parts << ((vuln?.Title ?: vuln?.Description ?: "Trivy vulnerability") as String)

        String packageName = (pkg?.Name ?: vuln?.PkgName ?: "") as String
        if (packageName) {
            parts << "Package: ${packageName}"
        }

        String installedVersion = (pkg?.InstalledVersion ?: vuln?.InstalledVersion ?: "") as String
        if (installedVersion) {
            parts << "Installed: ${installedVersion}"
        }

        String fixedVersion = (vuln?.FixedVersion ?: "") as String
        if (fixedVersion) {
            parts << "Fixed in: ${fixedVersion}"
        }

        return parts.findAll { it?.toString()?.trim() }.join(" | ")
    }

    /**
     * Map Trivy vulnerability to a logical repository file path.
     *
     * Strategy:
     * - Python package vulnerabilities -> requirements.txt
     * - JavaScript package vulnerabilities -> package-lock.json
     * - Java/Maven -> pom.xml
     * - Gradle -> build.gradle
     * - Go -> go.mod
     * - OS/base image/package issues -> Dockerfile
     *
     * This is a pragmatic mapping so Sonar can attach external issues
     * to a real file inside the repo.
     */
    static String buildFilePath(def result, String target, def pkg, def vuln) {
        String packageName = ((pkg?.Name ?: vuln?.PkgName ?: "") as String).toLowerCase()
        String lowerTarget = (target ?: "").toLowerCase()
        String resultClass = ((result?.Class ?: "") as String).toLowerCase()
        String resultType = ((result?.Type ?: "") as String).toLowerCase()

        // Direct target hints first
        if (lowerTarget == "dockerfile" || lowerTarget.endsWith("/dockerfile")) {
            return "Dockerfile"
        }
        if (lowerTarget.endsWith("requirements.txt")) {
            return "requirements.txt"
        }
        if (lowerTarget.endsWith("poetry.lock")) {
            return "poetry.lock"
        }
        if (lowerTarget.endsWith("pyproject.toml")) {
            return "pyproject.toml"
        }
        if (lowerTarget.endsWith("package-lock.json")) {
            return "package-lock.json"
        }
        if (lowerTarget.endsWith("yarn.lock")) {
            return "yarn.lock"
        }
        if (lowerTarget.endsWith("pnpm-lock.yaml")) {
            return "pnpm-lock.yaml"
        }
        if (lowerTarget.endsWith("pom.xml")) {
            return "pom.xml"
        }
        if (lowerTarget.endsWith("build.gradle")) {
            return "build.gradle"
        }
        if (lowerTarget.endsWith("build.gradle.kts")) {
            return "build.gradle.kts"
        }
        if (lowerTarget.endsWith("go.mod")) {
            return "go.mod"
        }

        // Known Python packages from your sample/project context
        if (isPythonPackage(packageName)) {
            return "requirements.txt"
        }

        // Generic language ecosystem hints
        if (resultClass.contains("lang-pkgs") || resultType.contains("library")) {
            return guessManifestFromPackage(packageName)
        }

        // OS/base image packages
        if (resultClass.contains("os-pkgs") || resultType.contains("os")) {
            return "Dockerfile"
        }

        // Fallback
        return "Dockerfile"
    }

    /**
     * Known Python dependency detection.
     */
    static boolean isPythonPackage(String packageName) {
        if (!packageName) {
            return false
        }

        Set<String> knownPythonPackages = [
            "pyyaml",
            "pip",
            "starlette",
            "wheel",
            "jaraco.context"
        ] as Set<String>

        return knownPythonPackages.contains(packageName)
    }

    /**
     * Best-effort manifest guess.
     */
    static String guessManifestFromPackage(String packageName) {
        if (!packageName) {
            return "Dockerfile"
        }

        String name = packageName.toLowerCase()

        // Python-oriented guess
        if (isPythonPackage(name)) {
            return "requirements.txt"
        }

        // Node ecosystem heuristic
        if (name.startsWith("@") || name.contains("node") || name.contains("npm")) {
            return "package-lock.json"
        }

        // Java ecosystem heuristic
        if (name.contains("spring") || name.contains("jackson") || name.contains("log4j")) {
            return "pom.xml"
        }

        // Default for image/base packages
        return "Dockerfile"
    }

    /**
     * Rough remediation effort estimate.
     */
    static int estimateEffortMinutes(def vuln) {
        switch (((vuln?.Severity ?: "") as String).toUpperCase()) {
            case "CRITICAL":
                return 120
            case "HIGH":
                return 90
            case "MEDIUM":
                return 45
            case "LOW":
                return 20
            default:
                return 15
        }
    }
}