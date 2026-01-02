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

    // Load tool metadata from resources
    def toolMeta = readYaml(
        text: libraryResource('tool-metadata.yaml')
    ).ruff
    // sh"ls -la && pwd"
    def image   = toolMeta.image
    def command = toolMeta.command.replace('{target}', target)

    // println "[DEBUG] Ruff toolMeta.command = ${toolMeta.command}"
    // println "[DEBUG] Ruff target resolved = ${target}"
    // println "[DEBUG] Ruff final command = ${command}"


    def workDir = "/ci-workspace/ruff/${env.JOB_NAME}-${env.BUILD_NUMBER}".replaceAll('[^a-zA-Z0-9_./-]', '_')

    sh "mkdir -p ${workDir} && cp -r * ${workDir}/"

    def runner = new devsecops.DockerRunner(this)
    

    runner.run(
        workDir,
        image,
        "${command} > ${workDir}/ruff.json || true"
    )

    def findings = []
    if (fileExists("${workDir}/ruff.json")) {
        findings = readJSON file: "${workDir}/ruff.json"
        // echo "[runRuff] Reading Ruff findings from ${workDir}/ruff.json"
        // echo sh(script: "cat ${workDir}/ruff.json", returnStdout: true)
    }

    sh "rm -rf ${workDir}"

    return findings
}
