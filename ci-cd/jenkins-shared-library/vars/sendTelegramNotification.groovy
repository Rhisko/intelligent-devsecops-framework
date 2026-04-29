import java.net.URLEncoder

def call(Map config = [:]) {
    def chatId = config.chat_id ?: error("[sendToTelegram] 'chat_id' is required")
    def parseMode = config.parse_mode ?: "HTML"
    def disablePreview = config.get("disable_web_page_preview", true)
    def disableNotification = config.get("disable_notification", false)
    def credentialsId = config.credentials_id ?: "telegram-bot-token"

    // Mode 1: raw message
    // Mode 2: build formatted advisory message from summary fields
    def message = config.message ?: buildAiAdvisoryMessage(config)

    withCredentials([
        string(credentialsId: credentialsId, variable: 'TELEGRAM_BOT_TOKEN')
    ]) {
        def encodedMessage = URLEncoder.encode(message, "UTF-8")
        def encodedParseMode = URLEncoder.encode(parseMode, "UTF-8")
        def url = "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage"

        def response = sh(
            script: """
                set -euo pipefail

                curl -sS -X POST "${url}" \\
                  -H "Content-Type: application/x-www-form-urlencoded" \\
                  --data "chat_id=${chatId}" \\
                  --data "text=${encodedMessage}" \\
                  --data "parse_mode=${encodedParseMode}" \\
                  --data "disable_web_page_preview=${disablePreview}" \\
                  --data "disable_notification=${disableNotification}"
            """,
            returnStdout: true
        ).trim()

        return response
    }
}

private String buildAiAdvisoryMessage(Map config) {
    def project = escapeHtml(config.project ?: "-")
    def overallRisk = escapeHtml(config.overall_risk ?: "-")
    def exploitationRisk = escapeHtml(config.exploitation_risk ?: "-")
    def releaseRecommendation = escapeHtml(config.release_recommendation ?: "-")
    def executiveSummary = escapeHtml(config.executive_summary ?: "-")
    def reportUrl = config.report_url ? config.report_url.toString().trim() : ""
    def jobName = escapeHtml(config.job_name ?: "")
    def buildNumber = escapeHtml(config.build_number ?: "")
    def environment = escapeHtml(config.environment ?: "")
    def timestamp = escapeHtml(config.timestamp ?: "")

    def reportSection = reportUrl
        ? """
━━━━━━━━━━━━━━━━━━
<b>REPORT ACCESS</b>
━━━━━━━━━━━━━━━━━━
<a href="${escapeHtmlAttribute(reportUrl)}">Open Full Advisory Report</a>
""".trim()
        : ""

    def buildSection = (jobName || buildNumber || environment || timestamp)
        ? """
━━━━━━━━━━━━━━━━━━
<b>BUILD INFORMATION</b>
━━━━━━━━━━━━━━━━━━
${jobName ? "<b>Job</b>         : <code>${jobName}</code>\n" : ""}${buildNumber ? "<b>Build Number</b>: <code>${buildNumber}</code>\n" : ""}${environment ? "<b>Environment</b> : <code>${environment}</code>\n" : ""}${timestamp ? "<b>Timestamp</b>   : <code>${timestamp}</code>" : ""}
""".trim()
        : ""

    return """
<b>INTELLIGENT DEVSECOPS FRAMEWORK</b>
<i>AI-Driven Security Advisory</i>

━━━━━━━━━━━━━━━━━━
<b>PROJECT</b>
━━━━━━━━━━━━━━━━━━
<code>${project}</code>

━━━━━━━━━━━━━━━━━━
<b>RISK OVERVIEW</b>
━━━━━━━━━━━━━━━━━━
<b>Overall Risk</b>         : <code>${overallRisk}</code>
<b>Exploitation Risk</b>    : <code>${exploitationRisk}</code>
<b>Release Recommendation</b>: <code>${releaseRecommendation}</code>

━━━━━━━━━━━━━━━━━━
<b>EXECUTIVE SUMMARY</b>
━━━━━━━━━━━━━━━━━━
${executiveSummary}

${buildSection ? buildSection + "\n\n" : ""}${reportSection ? reportSection + "\n\n" : ""}<i>Generated automatically by CI/CD AI Advisory Pipeline.</i>
""".stripIndent().trim()
}

private String escapeHtml(Object value) {
    return String.valueOf(value ?: "")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

private String escapeHtmlAttribute(Object value) {
    return escapeHtml(value).replace("\"", "&quot;")
}