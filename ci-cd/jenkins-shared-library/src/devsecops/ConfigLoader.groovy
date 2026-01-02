package devsecops

class ConfigLoader implements Serializable {

    def steps
    private Map<String, Object> cache = [:]

    ConfigLoader(steps) {
        this.steps = steps
    }

    /**
     * Load YAML config dynamically from resources/
     *
     * @param name config name without .yaml
     *             example: "docker-config", "tool-metadata"
     */
    Map load(String name) {

        if (cache.containsKey(name)) {
            return cache[name]
        }

        String resourcePath = "${name}.yaml"

        try {
            def content = steps.libraryResource(resourcePath)
            def data = steps.readYaml(text: content)

            if (!data) {
                steps.error("Config '${resourcePath}' is empty or invalid")
            }

            cache[name] = data
            return data

        } catch (Exception e) {
            steps.error("Failed to load config '${resourcePath}': ${e.message}")
        }
    }
}
