import groovy.json.JsonSlurperClassic

def call(Map config = [:]) {
    def payload = config.payload
    def filePath = config.file

    if (!payload && !filePath) {
        error("[parseAiAdvisorySummary] Either 'payload' or 'file' is required")
    }

    if (payload && filePath) {
        error("[parseAiAdvisorySummary] Use only one input: 'payload' or 'file'")
    }

    def rawJson
    if (payload) {
        rawJson = payload
    } else {
        rawJson = readFile(filePath)
    }

    def parsed = new JsonSlurperClassic().parseText(rawJson)

    return [
        project               : parsed.project,
        overall_risk          : parsed.overall_risk,
        exploitation_risk     : parsed.exploitation_risk,
        release_recommendation: parsed.release_recommendation,
        executive_summary     : parsed.executive_summary
    ]
}