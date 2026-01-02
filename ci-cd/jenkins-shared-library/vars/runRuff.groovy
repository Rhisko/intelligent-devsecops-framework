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
    pringtln("[runRuff] Running Ruff scan on target: ${target}")

    // Load tool metadata from resources
    def toolMeta = readYaml(
        text: libraryResource('tool-metadata.yaml')
    ).ruff
    // sh"ls -la && pwd"
    def image   = toolMeta.image
    def command = toolMeta.command.replace('{target}', target)

    def workDir = "../devsecops/ruff/${env.JOB_NAME}-${env.BUILD_NUMBER}".replaceAll('[^a-zA-Z0-9_./-]', '_')

    sh "mkdir -p ${workDir}"

    def runner = new devsecops.DockerRunner(this)
    

    runner.run(
        image,
        "${command} > ${workDir}/ruff.json || true"
    )

    def findings = []
    if (fileExists("${workDir}/ruff.json")) {
        findings = readJSON file: "${workDir}/ruff.json"
        echo "[runRuff] Reading Ruff findings from ${workDir}/ruff.json"
        echo sh(script: "cat ${workDir}/ruff.json", returnStdout: true)
    }

    sh "rm -rf ${workDir}"

    return findings
}
