def call(Map args = [:]) {

    def inputFile = args.input ?: error("input (ruff.json) is required")

    if (!fileExists(inputFile)) {
        error "Ruff result not found: ${inputFile}"
    }

    echo "[publishRuffExternalIssues] Reading Ruff output: ${inputFile}"

    // 1. Load Ruff findings (List<Map>)
    def ruffFindings = readJSON file: inputFile

    // 2. Normalize to Sonar format (IN-MEMORY)
    def sonarPayload = devsecops.RuffExternalSonarMapper.toSonar(ruffFindings)

    echo "[publishRuffExternalIssues] Issues count: ${sonarPayload.issues.size()}"
    echo "[publishRuffExternalIssues] Sample All data: ${sonarPayload}"

    // env.RUFF_SONAR_PAYLOAD = groovy.json.JsonOutput.toJson(sonarPayload)

    return sonarPayload
}
