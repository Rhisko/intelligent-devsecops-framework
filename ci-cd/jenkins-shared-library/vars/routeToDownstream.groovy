import devsecops.WebhookContext
import devsecops.ConfigLoader

def call(WebhookContext ctx) {

    // === LOAD ROUTING CONFIG VIA CONFIGLOADER ===
    def cfgLoader = new ConfigLoader(this)
    def routingConfig = cfgLoader.load("routing")

    def route = routingConfig?.routes?.get(ctx.repositoryFullName)

    if (!route) {
        echo "[ROUTER] No routing defined for ${ctx.repositoryFullName}. Event ignored."
        return
    }
    println "[ROUTER] Routing to job '${route.job}' for repository '${ctx.repositoryFullName}'"
    build job: route.job,
          wait: false,
          parameters: buildParams(ctx)

}




def buildParams(WebhookContext ctx) {
    return [
        // === Repository metadata ===
        string(name: 'REPOSITORY_NAME',      value: ctx.repositoryName),
        string(name: 'REPOSITORY_FULL_NAME', value: ctx.repositoryFullName),
        string(name: 'LANGUAGE',             value: ctx.language),
        string(name: 'UPDATED_AT',           value: ctx.updatedAt),

        // === Git references ===
        string(name: 'REF',       value: ctx.ref),
        string(name: 'BASE_REF',  value: ctx.baseRef),
        string(name: 'TAG_NAME',  value: ctx.tagName),

        // === Commit metadata ===
        string(name: 'COMMIT_HASH_BEFORE', value: ctx.commitHashBefore),
        string(name: 'COMMIT_HASH_AFTER',  value: ctx.commitHashAfter),
        string(name: 'COMMIT_URL',          value: ctx.commitUrl),
        string(name: 'AUTHOR',              value: ctx.author),
        string(name: 'COMMIT_MESSAGE',      value: ctx.commitMessage),

        // === File changes ===
        string(name: 'ADDED',    value: ctx.added),
        string(name: 'REMOVED',  value: ctx.removed),
        string(name: 'MODIFIED', value: ctx.modified),

        // === Derived ===
        string(name: 'EVENT_TYPE', value: ctx.eventType)
    ]
}


// def call(WebhookContext ctx) {

//     // Load routing config from resources
//     def routingConfig = loadRoutingConfig()

//     def route = routingConfig.routes[ctx.repositoryFullName]

//     if (!route) {
//         echo "[ROUTER] No routing defined for ${ctx.repositoryFullName}. Event ignored."
//         return
//     }



//     build job: route.job,
//           wait: false,
//           parameters: buildParams(ctx)
// }

// def loadRoutingConfig() {
//     def yamlText = libraryResource('routing.yaml')
//     return new Yaml().load(yamlText)
// }