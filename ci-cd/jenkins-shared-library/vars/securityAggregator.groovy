
/**
 * securityAggregator
 *
 * Tujuan:
 * - Menerima hasil scan dari berbagai security tools
 * - Menyimpan raw result
 * - (Opsional) lakukan policy check sederhana
 * - Menjadi single entry point (tool-agnostic)
 *
 * NOTE:
 * - Jenkins hanya ORCHESTRATE
 * - Logic security tidak menyebar di Jenkinsfile
 */
def collect(Map config = [:]) {

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

    echo "[securityAggregator] Tool      : ${tool}"
    echo "[securityAggregator] Metadata  : ${metadata}"

    // ===== STORE RAW RESULT (ARTIFACT / DEBUG) =====
    def outputFile = "${tool}-raw-result.json"
    writeJSON file: outputFile, json: data, pretty: 2
    archiveArtifacts artifacts: outputFile, fingerprint: true

    // ===== SIMPLE POLICY (OPTIONAL, SAFE DEFAULT) =====
    // NOTE: Jangan parsing berat di Jenkins
    if (tool == 'trivy') {
        handleTrivyPolicy(data, metadata)
    }

    echo "[securityAggregator] Collection completed for ${tool}"
}

/**
 * Minimal policy handler khusus Trivy
 * - Aman
 * - Tidak parsing kompleks
 */
def handleTrivyPolicy(def trivyResult, Map metadata) {

    def criticalCount = 0

    // Trivy standard JSON structure
    trivyResult?.Results?.each { result ->
        result?.Vulnerabilities?.each { vuln ->
            if (vuln?.Severity == 'CRITICAL') {
                criticalCount++
            }
        }
    }

    echo "[securityAggregator][Trivy] CRITICAL findings: ${criticalCount}"

    // Example policy:
    // - local scan  -> fail build
    // - registry scan -> warn only
    def scanScope = metadata.scan_scope ?: 'local'

    if (scanScope == 'local' && criticalCount > 0) {
        error "[SECURITY GATE] Trivy found ${criticalCount} CRITICAL vulnerabilities (local scan)"
    }

    if (scanScope == 'registry' && criticalCount > 0) {
        echo "[SECURITY WARNING] CRITICAL vulnerabilities found in registry image"
    }
}
