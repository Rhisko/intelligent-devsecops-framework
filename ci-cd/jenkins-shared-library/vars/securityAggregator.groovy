/**
 * securityAggregator
 *
 * Entry point (DSL step)
 */
def call(Map config = [:]) {

    if (!config.tool) {
        error "[securityAggregator] 'tool' is required"
    }
    if (!config.data) {
        error "[securityAggregator] 'data' is required"
    }

    def tool     = config.tool
    def data     = config.data      // net.sf.json.JSONObject OK
    def metadata = config.metadata ?: [:]

    echo "[securityAggregator] Tool     : ${tool}"
    echo "[securityAggregator] Metadata : ${metadata}"

    if (tool == 'trivy') {
        // IMPORTANT: helper dipanggil via this.
        return this.handleTrivy(data, metadata)
    }

    return [
        summary  : [:],
        findings : []
    ]
}
