/**
 * runSemgrep
 * - Semgrep runs as ephemeral container
 * - Source copied into isolated host dir (base_host_path/workDir)
 * - Output parsed immediately to memory
 * - Temp artifact removed after use
 * - No dependency on Jenkins workspace lifecycle
 */
def call(Map config = [:]) {

    // Target path inside the snapshot (relative to /ci-workspace)
    def target = config.path ?: '.'

    // def language = config.language ?: 'unknown'
    // if (language == 'unknown') {
    //     error "[WARNING] Language not specified, defaulting to 'unknown'"
    // }

    // Optional: Semgrep ruleset selector (auto / specific packs / custom)
    // Examples:
    //   "auto"
    //   "p/security-audit"
    //   "p/owasp-top-ten"
    //   "p/security-audit,p/owasp-top-ten"
    def ruleset = config.ruleset ?: 'auto'

    // Optional extra args (e.g., "--severity ERROR", "--exclude ...")
    def extraArgs = config.extraArgs ?: ''

    // === LOAD TOOL METADATA VIA CONFIGLOADER ===
    def cfgLoader = new devsecops.ConfigLoader(this)
    def toolMetaAll = cfgLoader.load("tool-metadata")
    def toolMeta = toolMetaAll.semgrep

    if (!toolMeta?.image || !toolMeta?.command) {
        error("tool-metadata.yaml missing semgrep.image or semgrep.command")
    }

    def image = toolMeta.image

    // Build command from template (keep it simple & consistent)
    // toolMeta.command should contain placeholders:
    //   {target}, {ruleset}, {extraArgs}
    def command = toolMeta.command
        .replace('{target}', target)
        .replace('{ruleset}', ruleset)
        .replace('{extraArgs}', extraArgs)


    // === RELATIVE WORKDIR UNDER base_host_path ===
    def workDir = "/ci-workspace/semgrep/${env.JOB_NAME}-${env.BUILD_NUMBER}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    // Prepare isolated source snapshot
    sh """
        mkdir -p "${workDir}" && \
        cp -R . "${workDir}/"
    """

    def runner = new devsecops.DockerRunner(this)

    // Output file decided by caller/step (NOT in config)
    def outputFile = "${workDir}/semgrep.json"

    // Run semgrep. We redirect output at the Jenkins side for simplicity.
    runner.run(
        workDir,
        image,
        command,
        [:],  // env
        []    // extra volumes
    )
    echo "[DEBUG][runSemgrep] Semgrep scan completed : ${outputFile}"
    if (fileExists(outputFile)) {
        semgrepToSonarPayload = externalIssuesPublisher(
            tool: "semgrep",
            input: outputFile
        )   
    }

    // Cleanup
    sh "rm -rf ${workDir}"
    return semgrepToSonarPayload
}

