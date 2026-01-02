package devsecops

class DockerRunner implements Serializable {

    def steps

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
        List<String> volumes = [],
   
    ) {

        def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
        def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')
        def workingdir = "/Users/risko/Data/tools/jenkins_data/${workDir}"
        return steps.sh(
            script: """
              docker run --rm \
                -v "${workingdir}:/ci-workspace:ro" \
                ${volArgs} \
                ${envArgs} \
                -w /ci-workspace \
                ${image} ${command}
            """.stripIndent(),
            returnStatus: true
        )
    }
}
