/**
 * runRuff
 * - Ruff runs as ephemeral container
 * - Output written to private temp directory
 * - Parsed immediately to memory
 * - Temp artifact removed after use
 * - No dependency on workspace lifecycle
 */



def call(Map config = [:]) {

    def target = config.path ?: '.'

    def cfgLoader = new devsecops.ConfigLoader(this)

    // Load tool metadata dynamically
    def toolMetaAll = cfgLoader.load("tool-metadata")
    def toolMeta = toolMetaAll.ruff

    if (!toolMeta?.image || !toolMeta?.command) {
        error("tool-metadata.yaml missing ruff.image or ruff.command")
    }

    // Load tool metadata from resources
    // def toolMeta = readYaml(
    //     text: libraryResource('tool-metadata.yaml')
    // ).ruff

    def image   = toolMeta.image
    def command = toolMeta.command.replace('{target}', target)
    

    // println "[DEBUG] Ruff toolMeta.command = ${toolMeta.command}"
    // println "[DEBUG] Ruff target resolved = ${target}"
    // println "[DEBUG] Ruff final command = ${command}"


    def workDir = "/ci-workspace/ruff/${env.JOB_NAME}-${env.BUILD_NUMBER}".replaceAll('[^a-zA-Z0-9_./-]', '_')
    // def command = toolMeta.command.replace('{target}', "ruff/${env.JOB_NAME}-${env.BUILD_NUMBER}".replaceAll('[^a-zA-Z0-9_./-]', '_'))
    // Prepare isolated source snapshot
    sh """
    mkdir -p "${workDir}" && \
    cp -r . "${workDir}/"
    """

    def runner = new devsecops.DockerRunner(this)
    

    runner.run(
        workDir,
        image,
        "${command} > ${workDir}/ruff.json || true"
    )

    def findings = []
    if (fileExists("${workDir}/ruff.json")) {
        findings = readJSON file: "${workDir}/ruff.json"
    }

    // Cleanup
    sh "rm -rf ${workDir}"


    return findings
}
