/**
 * securityAggregator
 *
 * - Collect security scan result (tool-agnostic)
 * - Extract severity summary + detailed findings
 * - Return as variable (NO FILE OUTPUT)
 */
def collect(Map config = [:]) {

    if (!config.tool) {
        error "[securityAggregator] 'tool' is required"
    }
    if (!config.data) {
        error "[securityAggregator] 'data' is required"
    }

    def tool     = config.tool
    def data     = config.data
    def metadata = config.metadata ?: [:]

    echo "[securityAggregator] Tool     : ${tool}"
    echo "[securityAggregator] Metadata : ${metadata}"

    if (tool == 'trivy') {
        return handleTrivy(data, metadata)
    }

    // default return for other tools
    return [
        summary  : [:],
        findings : []
    ]
}
