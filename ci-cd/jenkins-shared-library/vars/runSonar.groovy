/**
 * runSonar
 * - Sonar runs as ephemeral container
 * - Source copied into isolated host dir (base_host_path/workDir)
 * - No dependency on Jenkins workspace lifecycle
 * - Result stored server-side (SonarQube)
 */
def call(Map config = [:]) {

    if (!config.projectKey) {
        error "[runSonar] projectKey is required"
    }

    def cfgLoader = new devsecops.ConfigLoader(this)
    def toolMeta  = cfgLoader.load("tool-metadata").sonarqube

    if (!toolMeta?.image || !toolMeta?.command) {
        error "[runSonar] tool-metadata.yaml missing sonarqube config"
    }

    // ===== RUNTIME CONTEXT =====
    // def branch  = config.branch ?: toolMeta.defaults.branch ?: 'main'
    def token   = config.token  ?: toolMeta.defaults.token ?: env.SONAR_TOKEN
    def hostUrl = config.hostUrl ?: toolMeta.defaults.hostUrl
    def sources = config.sources ?: toolMeta.defaults.sources
    def image = toolMeta.image

    if (!token) {
        error "[runSonar] SONAR_TOKEN not provided"
    }

    // ===== ISOLATED WORKDIR (LIKE SEMGREP) =====
    def workDir = "/ci-workspace/sonarqube/${env.JOB_NAME}-${env.BUILD_NUMBER}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    sh """
        mkdir -p "${workDir}" && \
        cp -R . "${workDir}/"
    """

    // ===== BUILD COMMAND =====
    def command = toolMeta.command
        .replace('{projectKey}', config.projectKey)
        .replace('{sources}', sources)
        .replace('{hostUrl}', hostUrl)
        .replace('{token}', token)
        // .replace('{branch}', branch)

    def runner = new devsecops.DockerRunner(this)

    // ===== RUN SCANNER ON SNAPSHOT =====
    runner.run(
        workDir,          // isolated snapshot
        image,
        command,
        [SONAR_TOKEN: token],
        [],
    )

    // ===== CLEANUP SNAPSHOT =====
    sh "rm -rf ${workDir}"

    // Sonar result is server-side
    return [
        tool   : 'sonarqube',
        project: config.projectKey,
        branch : branch,
        host   : hostUrl
    ]
}
