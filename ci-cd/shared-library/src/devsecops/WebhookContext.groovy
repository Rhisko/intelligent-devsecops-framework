package devsecops

class WebhookContext implements Serializable {

    // Repository metadata
    String repositoryName
    String repositoryFullName
    String language
    String updatedAt

    // Git references
    String ref
    String baseRef
    String tagName

    // Commit metadata
    String commitHashBefore
    String commitHashAfter
    String commitUrl
    String author
    String commitMessage

    // File changes
    String added
    String removed
    String modified

    // Derived / normalized
    String eventType   // tag | branch | unknown

    String toString() {
        return """
========== WEBHOOK CONTEXT ==========
Repository Name      : ${repositoryName}
Repository Full Name : ${repositoryFullName}
Event Type           : ${eventType}
Ref                  : ${ref}
Base Ref             : ${baseRef}
Tag Name             : ${tagName}
Commit Before        : ${commitHashBefore}
Commit After         : ${commitHashAfter}
Commit URL           : ${commitUrl}
Author               : ${author}
Commit Message       : ${commitMessage}
Language             : ${language}
Added Files          : ${added}
Removed Files        : ${removed}
Modified Files       : ${modified}
Repository Updated   : ${updatedAt}
=====================================
"""
    }
}
