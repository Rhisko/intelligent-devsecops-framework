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
    stage('Initialize Pipeline Context') {
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
    stage("Checkout Source Code from GitHub & Capture Workspace Baseline") {
      steps {
        script {
          retry(5) {
            sleep(1)

            def tagRef = params.TAG_NAME?.trim()
            if (!tagRef) {
              error "TAG_NAME is required for tag-based checkout"
            }

            checkout([
              $class: 'GitSCM',
              branches: [[name: "refs/tags/${tagRef}"]],
              userRemoteConfigs: [[
                url: "git@github.com:${params.REPOSITORY_FULL_NAME}.git",
                credentialsId: 'creds-github-ssh-access'
              ]]
            ])
          }

          workspaceBaseline = workspaceIntegrity.capture()
        }
      }
    }

    stage('Python Linting [Ruff]') {
      when {
        expression { params.LANGUAGE?.toLowerCase() == 'python' }
      }
      steps {
        script {
          echo "[PIPELINE] Running Ruff Linting ......"

          ruffFindings = runRuff(
            path: '.',            // scan root repo
          )

          echo "[PIPELINE] Ruff findings count: ${ruffFindings.size()}"
          // echo "[PIPELINE] Ruff findings as String : ${ruffFindings}"
          ruffTosonarPayload = publishRuffExternalIssues(
                input: "/ci-workspace/ruff/${JOB_NAME}-${BUILD_NUMBER}/ruff.json"
            )
        }
      }
    }
    stage('Static Application Security Testing [Semgrep]') {
      steps {
        script {
          echo "[PIPELINE] Running semgrep SAST scan ......"

          semgrepFindings = runSemgrep(
            path: '.',            // scan root repo
          )

          echo "[PIPELINE] semgrep findings count: ${semgrepFindings.size()}"
          echo "[PIPELINE] semgrep findings as String : ${semgrepFindings}"
            }
          }
        }
    stage("Workspace Integrity Verification") {
      steps {
        script {
          workspaceIntegrity.assertUnchanged(workspaceBaseline)
        }
      }
    }
    stage('Build and Push Container Image') {
      steps {
        script {
          def shortSha = params.COMMIT_HASH_AFTER.take(7)
          def version  = "${params.TAG_NAME}-${shortSha}"

          buildAndPushImage(
            image: params.REPOSITORY_NAME,
            context: ".",
            dockerfile: "Dockerfile",
            buildArgs: [
              APP_ENV: "production",
              VERSION: version
            ],
            tag: version,
            labels: [
              "org.opencontainers.image.source"  : env.GIT_URL,
              "org.opencontainers.image.revision": env.GIT_COMMIT,
              "org.opencontainers.image.version" : version,
              "org.opencontainers.image.created" : new Date()
                .format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone('UTC'))
            ],
            registry: "dockerhub"  // specify registry defined in docker-build.yaml
          )
        }
      }
    }


    stage('Container Security Scan [Trivy]') {
      steps {
        script {

          def localImage = env.LOCAL_IMAGE

          // echo "[INFO] Published image : ${env.PUBLISHED_IMAGE}"
          // echo "[INFO] Local image     : ${localImage}"
          echo "[PIPELINE] Running Trivy container image security scan ......"

          def trivyResult = runTrivy(
            scanType: 'image',
            target: localImage,
            severity: 'HIGH,CRITICAL'
          )

          // Optional: log summary (do NOT parse deeply here)
          if (trivyResult?.Results) {
            echo "[INFO] Trivy scan completed with ${trivyResult.Results.size()} result blocks"
          } else {
            echo "[INFO] Trivy scan completed (no results or empty)"
          }

          // Send raw result to aggregator
          def trivyAgg = securityAggregator.call(
            tool: 'trivy',
            data: trivyResult,
            metadata: [
              image: localImage,
              scope: 'local'
            ]
          )
          println "[DEBUG] Trivy aggregated findings: ${trivyAgg.findings.size()} items"
          println "[DEBUG] Trivy aggregated summary: ${trivyAgg.summary}"
          println "[DEBUG] Trivy raw result data: ${trivyAgg.findings}"
        }
      }
    }


    stage('Code Quality Analysis [SonarQube]') {
      steps {
        script {
          withCredentials([
            string(credentialsId: 'CREDS-SONAR-TOKEN', variable: 'SONAR_TOKEN')
          ]) {
            writeFile(
              file: "ruff-external.json",
              text: JsonOutput.prettyPrint(JsonOutput.toJson(ruffTosonarPayload))
            )
            echo "[PIPELINE] Performing code quality analysis using SonarQube"

            sonarFindings = runSonar(
              projectKey: "payment-service",
              externalIssuesReportPaths: "ruff-external.json",
            )

            echo "[PIPELINE] Sonar execution result: ${sonarFindings}"
          }
        }
      }
    }


    stage('AI-Driven Security Advisory [n8n]') {
      steps {
        script {
          echo "[PIPELINE] Generating AI-driven security advisory via n8n"
          }
        }
      }

    stage('Policy-as-Code Security Gate [Conftest]') {
      steps {
        script {
          echo "[PIPELINE] Evaluating security policies using Conftest"
          }
        }
      }


    stage('Deploy to Kubernetes Production Cluster [GKE]') {
      steps {
        script {
          echo "[PIPELINE] Deploying application to Kubernetes production cluster"
          }
        }
      }

    stage('Publish Security Report and Notifications [Confluence & Telegram]') {
      steps {
        script {
          echo "[PIPELINE] Publishing security report to Confluence and sending notifications"
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
                cleanWs()
            }        
        }
    }
}
