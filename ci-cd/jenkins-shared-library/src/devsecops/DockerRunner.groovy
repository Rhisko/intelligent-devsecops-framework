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
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = [],
        String workingDir
    ) {

        def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
        def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')
        

        return steps.sh(
            script: """
              docker run --rm \
                -v "/Users/risko/Data/tools/jenkins_data/${workingDir}:/workspace:ro" \
                ${volArgs} \
                ${envArgs} \
                -w /workspace \
                ${image} ${command}
            """.stripIndent(),
            returnStatus: true
        )
    }
}
