def call(Map args = [:]) {

    def inputFile = args.input ?: error("input (ruff.json) is required")
    def outputDir = args.outputDir ?: ".sonar"
    def outputFile = args.output ?: "${outputDir}/ruff-external.json"

    if (!fileExists(inputFile)) {
        error "Ruff result not found: ${inputFile}"
    }

    echo "[publishRuffExternalIssues] Reading Ruff output: ${inputFile}"
    def ruffFindings = readJSON file: inputFile

    def sonarPayload = devsecops.RuffExternalSonarMapper.toSonar(ruffFindings)

    sh "mkdir -p ${outputDir}"
    writeJSON file: outputFile, json: sonarPayload, pretty: 2

    echo "[publishRuffExternalIssues] Generated ${outputFile}"
    echo "[publishRuffExternalIssues] Issues count: ${sonarPayload.issues.size()}"

    // expose for sonar-scanner
    env.SONAR_EXTERNAL_ISSUES = outputFile
    println "[publishRuffExternalIssues] Set SONAR_EXTERNAL_ISSUES=${env.SONAR_EXTERNAL_ISSUES}"
}
