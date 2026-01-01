import devsecops.WebhookContext

def call(Map envVars) {

    def ctx = new WebhookContext()

    // Repository
    ctx.repositoryName     = envVars.REPOSITORY_NAME
    ctx.repositoryFullName = envVars.REPOSITORY_FULL_NAME
    ctx.language           = envVars.LANGUAGE
    ctx.updatedAt          = envVars.UPDATED_AT

    // Git refs
    ctx.ref     = envVars.REF
    ctx.baseRef = envVars.BASE_REF.replace('refs/heads/', '')

    // Commit
    ctx.commitHashBefore = envVars.COMMIT_HASH_BEFORE
    ctx.commitHashAfter  = envVars.COMMIT_HASH_AFTER
    ctx.commitUrl        = envVars.COMMIT_URL
    ctx.author           = envVars.AUTHOR
    ctx.commitMessage    = envVars.COMMIT_MESSAGE

    // File changes (array dari GitHub → string)
    ctx.added    = envVars.ADDED
    ctx.removed  = envVars.REMOVED
    ctx.modified = envVars.MODIFIED

    // Normalization logic
    if (ctx.ref?.startsWith('refs/tags/')) {
        ctx.eventType = 'tag'
        ctx.tagName   = ctx.ref.replace('refs/tags/', '')
    } else if (ctx.ref?.startsWith('refs/heads/')) {
        ctx.eventType = 'branch'
        ctx.tagName   = 'N/A'
    } else {
        ctx.eventType = 'unknown'
        ctx.tagName   = 'N/A'
    }

    return ctx
}
