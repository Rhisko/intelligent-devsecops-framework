/**
 * securityAggregator
 *
 * Jenkins Shared Library
 * - SINGLE entry point: call()
 * - Multi-tool (trivy, semgrep, linter)
 * - Variable-based (NO file output)
 * - Sonar-ready
 */
def call(Map config = [:]) {

    // ===== VALIDATION =====
    if (!config.tool) {
        error "[securityAggregator] 'tool' is required"
    }
    if (!config.data) {
        error "[securityAggregator] 'data' is required"
    }

    def tool     = config.tool
    def data     = config.data
    def metadata = config.metadata ?: [:]

    echo "[securityAggregator] Processing tool: ${tool}"

    // ===== DISPATCH =====
    switch (tool) {
        case 'trivy':
            return handleTrivy(data, metadata)

        case 'semgrep':
            return handleSemgrep(data, metadata)

        case 'linter':
            return handleLinter(data, metadata)

        default:
            error "[securityAggregator] Unsupported tool: ${tool}"
    }
}

/* =========================================================
 * INTERNAL HELPERS (NOT Jenkins steps)
 * ========================================================= */

def initSummary() {
    [LOW:0, MEDIUM:0, HIGH:0, CRITICAL:0]
}

def inc(Map summary, String sev) {
    if (summary.containsKey(sev)) {
        summary[sev]++
    }
}

def logSummary(String tool, Map s) {
    echo """
[securityAggregator][${tool}] Summary
------------------------------------
LOW      : ${s.LOW}
MEDIUM   : ${s.MEDIUM}
HIGH     : ${s.HIGH}
CRITICAL : ${s.CRITICAL}
------------------------------------
"""
}

/* =========================================================
 * TRIVY HANDLER (Image / Dependency)
 * ========================================================= */
def handleTrivy(def trivyResult, Map metadata) {

    def summary  = initSummary()
    def findings = []

    trivyResult?.Results?.each { r ->
        r?.Vulnerabilities?.each { v ->

            def sev = v?.Severity?.toUpperCase()
            inc(summary, sev)

            findings << [
            tool               : 'trivy',
            severity           : sev,
            rule_id            : v?.VulnerabilityID,
            message            : v?.Title ?: v?.Description,

            // === WHAT to fix ===
            component           : v?.PkgName,
            installed_version   : v?.InstalledVersion,
            fixed_version       : v?.FixedVersion ?: 'N/A',

            // === WHERE it comes from ===
            artifact            : metadata.image,
            source              : r?.Target,   // e.g. debian:12, alpine:3.19
            scan_scope          : metadata.scan_scope ?: 'local',

            // === HOW to fix (RECOMMENDATION) ===
            recommendation      : v?.FixedVersion
                ? "Upgrade ${v.PkgName} to version ${v.FixedVersion} or later"
                : "No fixed version available. Consider upgrading base image, replacing dependency, or applying mitigation",

            // === EVIDENCE ===
            reference           : v?.PrimaryURL
            ]

            // findings << [
            //     tool       : 'trivy',
            //     severity   : sev,
            //     rule_id    : v?.VulnerabilityID,
            //     message    : v?.Title ?: v?.Description,
            //     component  : v?.PkgName,
            //     artifact   : metadata.image,
            //     scan_scope : metadata.scan_scope ?: 'local',
            //     reference  : v?.PrimaryURL
            // ]
        }
    }

    logSummary('Trivy', summary)
    println("[DEBUG] Trivy findings collected: ${findings}")
    return [summary: summary, findings: findings]
}

/* =========================================================
 * SEMGREP HANDLER (SAST)
 * ========================================================= */
def handleSemgrep(def semgrepResult, Map metadata) {

    def summary  = initSummary()
    def findings = []

    semgrepResult?.results?.each { r ->

        def sev = r?.extra?.severity?.toUpperCase()
        inc(summary, sev)

        findings << [
            tool       : 'semgrep',
            severity   : sev,
            rule_id    : r?.check_id,
            message    : r?.extra?.message,
            component  : r?.path,
            artifact   : metadata.repo ?: 'source-code',
            scan_scope : 'repo',
            reference  : r?.extra?.metadata?.source
        ]
    }

    logSummary('Semgrep', summary)
    return [summary: summary, findings: findings]
}

/* =========================================================
 * LINTER HANDLER (Ruff / ESLint / Pylint)
 * ========================================================= */
def handleLinter(def linterResult, Map metadata) {

    def summary  = initSummary()
    def findings = []

    linterResult?.each { issue ->

        def sev = mapLinterSeverity(issue.level)
        inc(summary, sev)

        findings << [
            tool       : 'linter',
            severity   : sev,
            rule_id    : issue.code,
            message    : issue.message,
            component  : issue.file,
            artifact   : metadata.repo ?: 'source-code',
            scan_scope : 'repo'
        ]
    }

    logSummary('Linter', summary)
    return [summary: summary, findings: findings]
}

def mapLinterSeverity(String level) {
    switch (level?.toLowerCase()) {
        case 'error':   return 'HIGH'
        case 'warning': return 'MEDIUM'
        default:        return 'LOW'
    }
}
