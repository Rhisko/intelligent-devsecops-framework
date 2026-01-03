/**
 * runHorusec
 * - Horusec runs as ephemeral container
 * - Source code copied into isolated workspace
 * - Output parsed immediately
 * - No dependency on Jenkins workspace lifecycle
 */

def call(Map config = [:]) {

    def target = config.path ?: '.'

    // === LOAD CONFIG VIA CONFIGLOADER ===
    def cfgLoader = new devsecops.ConfigLoader(this)

    // Load Horusec metadata
    def toolMetaAll = cfgLoader.load("tool-metadata")
    def toolMeta = toolMetaAll.horusec

    if (!toolMeta?.image || !toolMeta?.command) {
        error("tool-metadata.yaml missing horusec.image or horusec.command")
    }

    def image   = toolMeta.image
    def command = toolMeta.command.replace('{target}', target)

    // === RELATIVE WORKDIR (UNDER base_host_path) ===
    def workDir = "ci-workspace/horusec/${env.JOB_NAME}-${env.BUILD_NUMBER}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    // Prepare isolated snapshot
    sh """
      mkdir -p "${workDir}" && \
      cp -r . "${workDir}/"
    """

    def runner = new devsecops.DockerRunner(this)

    // === OUTPUT FILE DECIDED HERE ===
    def outputFile = "horusec.json"

    runner.run(
        workDir,
        image,
        command,
        [:],   // env
        []     // extra volumes
    )

    def findings = []
    def outputPath = "${workDir}/${outputFile}"

    if (fileExists(outputPath)) {
        findings = readJSON file: outputPath
    }

    // Cleanup isolated workspace
    sh "rm -rf ${workDir}"

    return findings
}
