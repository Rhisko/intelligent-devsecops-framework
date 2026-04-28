def call(Map config = [:]) {

    def cfgLoader = new devsecops.ConfigLoader(this)
    def meta      = cfgLoader.load("tool-metadata").ai_advisory

    def projectKey    = config.project_key ?: error("[runAiAdvisory] project_key is required")
    def analysisMode  = config.analysis_mode ?: meta.defaults.analysis_mode
    def sonarUrl      = config.sonar_url ?: meta.defaults.sonar_url
    def networkName   = config.network ?: meta.defaults.network
    def outputFile    = config.output_file ?: meta.defaults.output_file
    def reportBaseDir  = config.report_base_dir ?: meta.defaults.report_base_dir

    def timestamp = new Date().format("yyyy-MM-dd_HH-mm-ss-SSS", TimeZone.getTimeZone("Asia/Jakarta"))
    def safeProjectKey = projectKey.replaceAll('[^a-zA-Z0-9_.-]', '-')

    def reportDir = "/report/${env.BUILD_NUMBER}-${safeProjectKey}-${analysisMode}-${timestamp}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    def command = meta.command
        .replace('{project_key}', projectKey)
        .replace('{analysis_mode}', analysisMode)
        .replace('{sonar_url}', sonarUrl)
        .replace('{report_dir}', reportDir)
        .replace('{output_file}', outputFile)

    def runner = new devsecops.DockerRunner(this)

    def envs = [:]

    def advisoryPayload = runner.run(
        "",
        meta.image,
        command,
        envs,
        ["${reportBaseDir}:/report"],
        false,
        null,
        networkName
    )

    if (!advisoryPayload?.trim()) {
        error("[runAiAdvisory] Empty stdout received from AI advisory container")
    }

    return [
        payload    : advisoryPayload.trim(),
        report_dir : reportDir,
        report_path: "${reportBaseDir}/${reportDir}",
        output_file: "${reportBaseDir}/${reportDir}/${outputFile}"
    ]
}