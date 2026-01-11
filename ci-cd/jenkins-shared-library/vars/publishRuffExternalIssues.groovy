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

    echo "[publishRuffExternalIssues] Issues normalized (in-memory)"
    echo "[publishRuffExternalIssues] Issues count: ${sonarPayload.issues.size()}"

    // 3. Simpan di memory (env) jika mau dipakai step lain
    //    (stringify karena env hanya String)
    env.RUFF_SONAR_PAYLOAD = groovy.json.JsonOutput.toJson(sonarPayload)
    println("[publishRuffExternalIssues] Payload data: ${env.RUFF_SONAR_PAYLOAD}")
    echo "[publishRuffExternalIssues] Stored payload in env.RUFF_SONAR_PAYLOAD"

    // 4. RETURN payload agar bisa dipakai langsung
    return sonarPayload
}
