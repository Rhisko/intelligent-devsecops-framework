package devsecops

class DockerRunner implements Serializable {

    def steps
 // === KONSTANTA BASE PATH (ABSOLUTE) ===
    static final String BASE_HOST_PATH =
        "/Users/risko/Data/tools/jenkins_data"
    DockerRunner(steps) {
        this.steps = steps
    }

    /**
     * Generic container runner
     *
     * @param image Docker image
     * @param command Command executed inside container
     * @param env Environment variables (optional)
     * @param volumes Extra volume mounts (optional)
     */
    int run(
        String workDir,
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = []
   
    ) {
        def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
        def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')
        def hostDir = "${BASE_HOST_PATH}/${workDir}"

        return steps.sh(
            script: """
              docker run --rm \
                -v "${hostDir}:/ci-workspace:ro" \
                ${volArgs} \
                ${envArgs} \
                -w /ci-workspace \
                ${image} ${command}
            """.stripIndent(),
            returnStatus: true
        )
    }
}
