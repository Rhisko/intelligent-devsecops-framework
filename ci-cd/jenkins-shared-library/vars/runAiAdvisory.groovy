def call(Map config = [:]) {

    def cfgLoader = new devsecops.ConfigLoader(this)
    def meta      = cfgLoader.load("tool-metadata").ai_advisory

    def projectKey    = config.project_key ?: error("[runAiAdvisory] project_key is required")
    def analysisMode  = config.analysis_mode ?: meta.defaults.analysis_mode
    def networkName   = config.network ?: "infrastructure_default"

    def timestamp = new Date().format("yyyy-MM-dd_HH-mm-ss-SSS", TimeZone.getTimeZone("Asia/Jakarta"))
    def safeProjectKey = projectKey.replaceAll('[^a-zA-Z0-9_.-]', '-')

    def reportDir = "${env.JOB_NAME}-${env.BUILD_NUMBER}-${safeProjectKey}-${analysisMode}-${timestamp}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    def command = meta.command
        .replace('{project_key}', projectKey)
        .replace('{analysis_mode}', analysisMode)
        .replace('{sonar_url}', sonarUrl)
        .replace('{report_dir}', reportDir)

    def runner = new devsecops.DockerRunner(this)

    def envs = [
        OPENAI_MODEL: openAiModel,
        LOG_LEVEL   : logLevel
    ]

    def advisoryPayload = null

    withCredentials([
        string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY'),
        string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')
    ]) {
        envs.OPENAI_API_KEY = OPENAI_API_KEY
        envs.SONAR_TOKEN    = SONAR_TOKEN

        advisoryPayload = runner.runAndCapture(
            "",
            meta.image,
            command,
            envs,
            ["${reportBaseDir}:/report"],
            false,
            null,
            networkName
        )
    }

    if (!advisoryPayload?.trim()) {
        error("[runAiAdvisory] Empty stdout received from AI advisory container")
    }

    return [
        payload    : advisoryPayload.trim(),
        report_dir : reportDir,
        report_path: "${reportBaseDir}/${reportDir}"
    ]
}