/**
 * runTrivy
 * - Trivy runs as ephemeral container
 * - Output parsed immediately to memory
 * - Temp artifact removed after use
 * - No dependency on Jenkins workspace lifecycle
 */
def call(Map config = [:]) {

    // === SCAN MODE ===
    // Supported:
    //   fs       → filesystem scan
    //   image    → container image scan
    //   config   → IaC scan (Dockerfile, K8s, Terraform)
    def scanType = config.scanType ?: 'fs'

    // Target:
    //   fs/config → path (default '.')
    //   image     → image:tag
    def target = config.target ?: '.'

    // Severity filter
    def severity = config.severity ?: 'HIGH,CRITICAL'

    // Optional extra args
    // Example:
    //   "--scanners vuln,secret,misconfig"
    //   "--ignore-unfixed"
    def extraArgs = config.extraArgs ?: ''

    // === LOAD TOOL METADATA VIA CONFIGLOADER ===
    def cfgLoader = new devsecops.ConfigLoader(this)
    def toolMetaAll = cfgLoader.load("tool-metadata")
    def toolMeta = toolMetaAll.trivy

    if (!toolMeta?.image || !toolMeta?.command) {
        error("tool-metadata.yaml missing trivy.image or trivy.command")
    }

    def image = toolMeta.image

    // Build command from template
    // toolMeta.command placeholders:
    //   {scanType}, {target}, {severity}, {extraArgs}, {output}
    def command = toolMeta.command
        .replace('{scanType}', scanType)
        .replace('{imageName}', scanType)
        .replace('{target}', target)
        .replace('{severity}', severity)
        .replace('{extraArgs}', extraArgs)
        .replace('{output}', 'trivy.json')

    // === RELATIVE WORKDIR UNDER base_host_path ===
    def workDir = "/ci-workspace/trivy/${env.JOB_NAME}-${env.BUILD_NUMBER}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    // Prepare isolated snapshot
    // NOTE:
    // - For fs/config scan → copy source
    // - For image scan     → no copy needed, but dir still created
    sh """
        mkdir -p "${workDir}"
    """

    if (scanType != 'image') {
        sh """
            cp -R . "${workDir}/"
        """
    }

    def runner = new devsecops.DockerRunner(this)

    // Run Trivy
    runner.run(
        workDir,
        image,
        command,
        [:],  // env
        [
            // Required for image scan
            "/var/run/docker.sock:/var/run/docker.sock"
        ]
    )

    def findings = []
    def outputFile = "${workDir}/trivy.json"

    if (fileExists(outputFile)) {
        findings = readJSON file: outputFile
        echo "Trivy findings data loaded"
    } else {
        echo "Trivy output not found (no findings or execution issue)"
    }

    // Cleanup 
    sh "rm -rf ${workDir} && docker rmi ${target} || true"
    println "[DEBUG] Removed workDir ${workDir} and image ${target} findings data loaded ${findings}"

    return findings
}
