def call(Map config = [:]) {

    def cfgLoader = new devsecops.ConfigLoader(this)
    def meta      = cfgLoader.load("tool-metadata").ruff

    def target = config.target ?: meta.defaults.target
    def select = config.select ?: meta.defaults.select
    def format = config.output_format ?: meta.defaults.output_format

    // println "[DEBUG][runRuff] target=${target}, select=${select}, format=${format}"

    def command = meta.command
        .replace('{target}', target)
        .replace('{select}', select)
        .replace('{output_format}', format)

    def workDir = "/ci-workspace/ruff/${env.JOB_NAME}-${env.BUILD_NUMBER}"
        .replaceAll('[^a-zA-Z0-9_./-]', '_')

    sh """
      mkdir -p "${workDir}" && \
      cp -R . "${workDir}/"
    """

    def runner = new devsecops.DockerRunner(this)

    def resultsScan = runner.run(
        workDir,
        meta.image,
        command,
        [:],
        [] 
    )

    println "[runRuff] Scan Results:\n${resultsScan}"
    // def findings = []
    // def outputFile = "${workDir}/ruff.json"
    // if (fileExists(outputFile)) {
    //     findings = readJSON file: outputFile
    // }

    // // sh "rm -rf ${workDir}"
    // return findings
}
