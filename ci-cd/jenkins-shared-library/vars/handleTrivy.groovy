/**
 * Handle Trivy result
 * - Count severity (LOW → CRITICAL)
 * - Extract actionable vulnerability detail
 * - Return as Map (Sonar-ready)
 */
def handleTrivy(def trivyResult, Map metadata) {

    def summary = [
        LOW      : 0,
        MEDIUM   : 0,
        HIGH     : 0,
        CRITICAL : 0
    ]

    def findings = []

    trivyResult?.Results?.each { result ->
        def target = result?.Target

        result?.Vulnerabilities?.each { vuln ->

            def sev = vuln?.Severity?.toUpperCase()
            if (summary.containsKey(sev)) {
                summary[sev]++
            }

            findings << [
                tool              : 'trivy',
                severity          : sev,
                vulnerability_id  : vuln?.VulnerabilityID,
                package_name      : vuln?.PkgName,
                installed_version : vuln?.InstalledVersion,
                fixed_version     : vuln?.FixedVersion ?: 'N/A',
                title             : vuln?.Title,
                description       : vuln?.Description,
                reference         : vuln?.PrimaryURL,
                target            : target,
                artifact          : metadata.image,
                scan_scope        : metadata.scan_scope ?: 'local'
            ]
        }
    }

    echo """
[securityAggregator][Trivy] Summary
----------------------------------
LOW      : ${summary.LOW}
MEDIUM   : ${summary.MEDIUM}
HIGH     : ${summary.HIGH}
CRITICAL : ${summary.CRITICAL}
----------------------------------
"""

    return [
        summary  : summary,
        findings : findings
    ]
}
