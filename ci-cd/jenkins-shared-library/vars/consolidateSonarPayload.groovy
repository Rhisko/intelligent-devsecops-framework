def call(List<Map> payloads) {

    def rules = payloads.collectMany { it.rules }
        .unique { it.id }

    def issues = payloads.collectMany { it.issues }

    return [
        rules : rules,
        issues: issues
    ]
}
