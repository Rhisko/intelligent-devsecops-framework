pipeline {
  agent any

  parameters {

    // Repository metadata
    string(name: 'REPOSITORY_NAME')
    string(name: 'REPOSITORY_FULL_NAME')
    string(name: 'LANGUAGE')
    string(name: 'UPDATED_AT')

    // Git references
    string(name: 'REF')
    string(name: 'BASE_REF')
    string(name: 'TAG_NAME')

    // Commit metadata
    string(name: 'COMMIT_HASH_BEFORE')
    string(name: 'COMMIT_HASH_AFTER')
    string(name: 'COMMIT_URL')
    string(name: 'AUTHOR')
    string(name: 'COMMIT_MESSAGE')

    // File changes
    string(name: 'ADDED')
    string(name: 'REMOVED')
    string(name: 'MODIFIED')

    // Derived
    string(name: 'EVENT_TYPE')
  }

  stages {
    stage('Init Context') {
      steps {
        script {
          echo """
========== DOWNSTREAM CONTEXT ==========
Repository : ${params.REPOSITORY_FULL_NAME}
Tag        : ${params.TAG_NAME}
Commit     : ${params.COMMIT_HASH_AFTER}
Event      : ${params.EVENT_TYPE}
=======================================
"""
        }
      }
    }
  }
}
