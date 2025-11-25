// Jenkinsfile - DevSecOps Pipeline (Corp/Startup Style)
// All secrets via Jenkins Credentials, no hard-code.

pipeline {
    // Use a Docker agent image that already contains:
    // - node / npm
    // - trivy
    // - horusec
    // - conftest
    // - sonar-scanner
    agent {
        docker {
            image 'myorg/devsecops-agent:latest'
            args '-u root:root' // so we can install extra tools if needed
        }
    }

    options {
        timestamps()                 // Add timestamps to logs
        ansiColor('xterm')           // Colorful console output
        disableConcurrentBuilds()    // Avoid overlapping runs for same job
        buildDiscarder(logRotator(numToKeepStr: '30')) // Keep last 30 builds
    }

    parameters {
        // Common in companies: one pipeline, many environments
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Target environment')
        booleanParam(name: 'SKIP_SECURITY_SCANS', defaultValue: false, description: 'Skip heavy security scans (for quick dev feedback)')
    }

    environment {
        APP_NAME        = 'my-secure-app'
        REPORT_DIR      = 'reports'
        TZ              = 'Asia/Jakarta'

        // Docker registry (optional, can be empty if only local)
        DOCKER_REGISTRY = 'registry.example.com'

        // SonarQube integration
        SONARQUBE_ENV      = 'sonarqube-server'   // Name of configured SonarQube server in Jenkins
        SONAR_PROJECT_KEY  = 'my-secure-app'
        SONAR_PROJECT_NAME = 'My Secure App'

        // n8n webhook URL (hosted in your local docker-compose)
        N8N_WEBHOOK_URL = 'http://host.docker.internal:5678/webhook/devsecops-advisory'
    }

    stages {

        stage('Init & Checkout') {
            steps {
                script {
                    echo "[INIT] Checking out source code"
                }
                checkout scm
                script {
                    sh "mkdir -p ${REPORT_DIR}"
                    echo "[INIT] Branch: ${env.GIT_BRANCH}, Commit: ${env.GIT_COMMIT}"
                }
            }
        }

        stage('Set Build Metadata') {
            steps {
                script {
                    // Common practice: short commit SHA for tagging
                    env.SHORT_COMMIT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_TAG    = "${env.SHORT_COMMIT}-${env.BUILD_NUMBER}"
                    echo "[META] IMAGE_TAG = ${env.IMAGE_TAG}"
                    echo "[META] ENVIRONMENT = ${params.ENVIRONMENT}"
                }
            }
        }

        stage('Fast Feedback: Lint & Unit Tests') {
            parallel {
                stage('Lint') {
                    steps {
                        script {
                            echo "[LINT] Running lint checks"
                            // Example: Node.js project with ESLint
                            sh """
                                if [ -f package.json ]; then
                                  npm ci --no-fund --no-audit
                                  if npm run lint; then
                                    echo "[LINT] Lint success"
                                  else
                                    echo "[LINT] Lint failed"
                                    exit 1
                                  fi
                                else
                                  echo "[LINT] No package.json, skipping lint"
                                fi
                            """
                        }
                    }
                }
                stage('Unit Tests') {
                    steps {
                        script {
                            echo "[TEST] Running unit tests"
                            sh """
                                if [ -f package.json ]; then
                                  if npm test; then
                                    echo "[TEST] Unit tests passed"
                                  else
                                    echo "[TEST] Unit tests failed"
                                    exit 1
                                  fi
                                else
                                  echo "[TEST] No test config, skipping"
                                fi
                            """
                        }
                    }
                }
            }
        }

        stage('Security Scans (Horusec / Conftest / Trivy FS)') {
            when {
                expression { !params.SKIP_SECURITY_SCANS }
            }
            parallel {

                stage('Horusec SAST') {
                    steps {
                        script {
                            echo "[SEC] Running Horusec SAST"
                            sh """
                                horusec start -p . \
                                  -o json \
                                  -O ${REPORT_DIR}/horusec-report.json || true
                                # Note: Allow Horusec to finish even with findings; we decide later.
                            """
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: "${REPORT_DIR}/horusec-report.json", onlyIfSuccessful: false
                        }
                    }
                }

                stage('Policy as Code (Conftest)') {
                    steps {
                        script {
                            echo "[SEC] Running Conftest policy checks"
                            sh """
                                if [ -d policy ] && [ -d deploy ]; then
                                  set +e
                                  conftest test deploy/ --policy policy/ --output=json > ${REPORT_DIR}/conftest-report.json
                                  EXIT_CODE=\$?
                                  set -e
                                  if [ "\$EXIT_CODE" -ne 0 ]; then
                                    echo "[SEC] Policy violations found (Conftest). Failing pipeline."
                                    exit 1
                                  else
                                    echo "[SEC] Conftest policy checks passed"
                                  fi
                                else
                                  echo "[SEC] No policy/ or deploy/ directory, skipping policy check"
                                fi
                            """
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: "${REPORT_DIR}/conftest-report.json", onlyIfSuccessful: false
                        }
                    }
                }

                stage('Trivy Filesystem Scan') {
                    steps {
                        script {
                            echo "[SEC] Running Trivy filesystem scan"
                            sh """
                                trivy fs . \
                                  --format json \
                                  --output ${REPORT_DIR}/trivy-fs.json \
                                  --severity HIGH,CRITICAL || true
                                # We may choose to fail on CRITICAL later in a gate stage.
                            """
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: "${REPORT_DIR}/trivy-fs.json", onlyIfSuccessful: false
                        }
                    }
                }

            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "[BUILD] Building Docker image"
                    sh """
                        if [ -f Dockerfile ]; then
                          docker build -t ${APP_NAME}:${IMAGE_TAG} .
                        else
                          echo "[BUILD] No Dockerfile found, cannot build image"
                          exit 1
                        fi
                    """
                }
            }
        }

        stage('Trivy Image Scan') {
            when {
                expression { !params.SKIP_SECURITY_SCANS }
            }
            steps {
                script {
                    echo "[SEC] Running Trivy image scan"
                    sh """
                        trivy image ${APP_NAME}:${IMAGE_TAG} \
                          --format json \
                          --output ${REPORT_DIR}/trivy-image.json \
                          --severity HIGH,CRITICAL || true
                    """
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${REPORT_DIR}/trivy-image.json", onlyIfSuccessful: false
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    echo "[QUALITY] Running SonarQube analysis"
                }
                withSonarQubeEnv("${SONARQUBE_ENV}") {
                    // Use Jenkins SonarQube plugin for token & URL
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        sh """
                            sonar-scanner \
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                              -Dsonar.projectName=${SONAR_PROJECT_NAME} \
                              -Dsonar.sources=. \
                              -Dsonar.host.url=\$SONAR_HOST_URL \
                              -Dsonar.login=\$SONAR_TOKEN
                        """
                    }
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                script {
                    echo "[QUALITY] Waiting for SonarQube Quality Gate"
                    timeout(time: 10, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "[QUALITY] Quality Gate failed: ${qg.status}. Stopping pipeline."
                        } else {
                            echo "[QUALITY] Quality Gate passed"
                        }
                    }
                }
            }
        }

        stage('Push Image (Optional)') {
            when {
                allOf {
                    expression { params.ENVIRONMENT != 'dev' }  // Example: only push for staging/prod
                    expression { env.DOCKER_REGISTRY?.trim() }
                }
            }
            steps {
                script {
                    echo "[DELIVER] Pushing Docker image to registry"
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-creds',
                        usernameVariable: 'REG_USER',
                        passwordVariable: 'REG_PASS'
                    )]) {
                        sh """
                            echo "$REG_PASS" | docker login ${DOCKER_REGISTRY} -u "$REG_USER" --password-stdin
                            docker tag ${APP_NAME}:${IMAGE_TAG} ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG}
                            docker push ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG}
                            docker logout ${DOCKER_REGISTRY}
                        """
                    }
                }
            }
        }

        stage('Notify n8n (AI Advisory & Orchestration)') {
            steps {
                script {
                    echo "[OBS] Sending summary to n8n webhook"
                    // If your n8n webhook needs a token, add withCredentials here
                    withCredentials([string(credentialsId: 'n8n-webhook-token', variable: 'N8N_TOKEN')]) {
                        sh """
                            curl -X POST "${N8N_WEBHOOK_URL}" \
                              -H "Content-Type: application/json" \
                              -H "Authorization: Bearer $N8N_TOKEN" \
                              -d '{
                                    "app": "${APP_NAME}",
                                    "environment": "${params.ENVIRONMENT}",
                                    "build_number": "${BUILD_NUMBER}",
                                    "git_branch": "${env.GIT_BRANCH}",
                                    "commit": "${env.SHORT_COMMIT}",
                                    "reports": {
                                      "horusec": "${REPORT_DIR}/horusec-report.json",
                                      "conftest": "${REPORT_DIR}/conftest-report.json",
                                      "trivy_fs": "${REPORT_DIR}/trivy-fs.json",
                                      "trivy_image": "${REPORT_DIR}/trivy-image.json"
                                    },
                                    "result": "${currentBuild.currentResult}"
                                  }'
                        """
                    }
                }
            }
        }

        stage('Deploy (Controlled)') {
            when {
                allOf {
                    branch 'main'
                    expression { params.ENVIRONMENT == 'staging' || params.ENVIRONMENT == 'prod' }
                }
            }
            steps {
                script {
                    echo "[DEPLOY] Deploying to ${params.ENVIRONMENT} (placeholder)"
                    // Here you would call:
                    // - Helm chart
                    // - kubectl apply
                    // - Ansible playbook
                    // - or trigger another Jenkins job
                }
            }
        }
    }

    post {
        always {
            script {
                echo "[POST] Build finished with status: ${currentBuild.currentResult}"
            }
        }
        failure {
            script {
                echo "[POST] Build failed – you can extend this to trigger n8n incident workflow."
            }
        }
    }
}
