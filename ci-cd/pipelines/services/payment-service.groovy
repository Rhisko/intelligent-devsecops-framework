@Library('devsecops-pipeline-library') _

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
    stage("Checkout from github repository") {
      steps {
        script {
          retry(5) {
            sleep(1)
            def branchRef = params.BASE_REF ? params.BASE_REF : "main"
            checkout([
              $class: 'GitSCM',
              branches: [[name: branchRef]],
              userRemoteConfigs: [[
                url: 'git@github.com:Rhisko/payment-service.git',
                credentialsId: 'creds-github-ssh-access'
                ]]
              ])
            }
          }
        }
      }
    stage('Lint - Ruff') {
      when {
        expression { params.LANGUAGE?.toLowerCase() == 'python' }
      }
      steps {
        script {
          echo "[PIPELINE] Running Ruff lint via shared library"

          ruffFindings = runRuff(
            path: '.',            // scan entire repo
          )

          echo "[PIPELINE] Ruff findings count: ${ruffFindings.size()}"
        }
      }
    }
    }
    post {
        success {
            echo "[PIPELINE] SUCCESS for issue ${REPOSITORY_NAME.toUpperCase()} (${COMMIT_HASH_AFTER})"
        }
        failure {
            echo "[PIPELINE] FAILED for issue ${REPOSITORY_NAME.toUpperCase()} (${COMMIT_HASH_AFTER})"
        }
        always {
            script {
                def status = currentBuild.currentResult ?: 'UNKNOWN'
                currentBuild.displayName = "${status} - ${REPOSITORY_NAME.toUpperCase()}-${TAG_NAME}-BUILD_NUMBER-${BUILD_NUMBER}"
 
            }        
        }
    }
}
