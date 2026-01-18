def call(Map args = [:]) {

    // --------------------------------------------------
    // 1. Validate arguments
    // --------------------------------------------------
    String tool      = args.tool ?: "ruff"   // default: ruff (BACKWARD COMPATIBLE)
    String inputFile = args.input ?: error("input (<tool>.json) is required")

    if (!fileExists(inputFile)) {
        error "[ExternalIssuesPublisher] Result not found: ${inputFile}"
    }

    echo "[ExternalIssuesPublisher] Tool  : ${tool}"
    echo "[ExternalIssuesPublisher] Input : ${inputFile}"

    // --------------------------------------------------
    // 2. Load raw JSON
    // --------------------------------------------------
    def raw = readJSON file: inputFile
    echo "[ExternalIssuesPublisher] Raw data loaded ${raw.results}"

    // --------------------------------------------------
    // 3. Normalize findings (tool-specific)
    // --------------------------------------------------
    List findings
    switch (tool) {

        case "ruff":
            findings = raw instanceof List ? raw : []
            break

        case "semgrep":
            findings = raw?.results ?: []
            break

        case "trivy":
            findings = raw?.results ?: []
            break

        default:
            error "[ExternalIssuesPublisher] Unsupported tool: ${tool}"
    }

    echo "[ExternalIssuesPublisher] Findings count: ${findings.size()}"

    // --------------------------------------------------
    // 4. Dispatch to mapper (STRICT SEPARATION)
    // --------------------------------------------------
    Map sonarPayload

    switch (tool) {

        case "ruff":
            sonarPayload = devsecops.RuffExternalSonarMapper
                .toSonar(findings)
            break

        case "semgrep":
            sonarPayload = devsecops.SemgrepExternalSonarMapper
                .toSonar(findings)
            break

        case "trivy":
            sonarPayload = devsecops.TrivyExternalSonarMapper
                .toSonar(findings)
            break
    }

    echo "[ExternalIssuesPublisher] Sonar rules : ${sonarPayload.rules.size()}"
    echo "[ExternalIssuesPublisher] Sonar issues: ${sonarPayload.issues.size()}"

    return sonarPayload
}
