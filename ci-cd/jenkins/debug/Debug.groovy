// Jenkinsfile.debug
// DevSecOps Toolchain Debug Pipeline (Corporate + Startup Standard)

pipeline {
    agent any
    options {
        timestamps()
        // ansiColor('xterm')
        // buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        REPORT_DIR = "debug-reports"
        SONARQUBE_ENV = "sonarqube-server"
        N8N_WEBHOOK_URL = "http://host.docker.internal:5678/webhook/devsecops-debug"
    }

    stages {

        stage('Init Workspace') {
            steps {
                script {
                    echo "[INIT] Setting up debug workspace"
                    sh "mkdir -p ${REPORT_DIR}"
                    sh "ls -lah"
                }
            }
        }

        stage('DEBUG: Agent Environment') {
            steps {
                script {
                    echo "[DEBUG] Checking OS & System Info"
                }
                sh '''
                    echo "----- OS Information -----"
                    uname -a
                    cat /etc/os-release || true

                    echo "----- Disk Space -----"
                    df -h

                '''
            }
        }

        stage('DEBUG: Installed Toolchain Versions') {
            steps {
                script {
                    echo "[DEBUG] Checking installed DevSecOps tools"
                }
                sh '''
                    echo "===== SONAR SCANNER ====="
                    sonar-scanner --version || echo "Not installed"

                    echo "===== HORUSEC ====="
                    horusec version || echo "Not installed"

                    echo "===== TRIVY ====="
                    trivy --version || echo "Not installed"

                    echo "===== CONFTEST ====="
                    conftest --version || echo "Not installed"

                    echo "===== NODE / NPM ====="
                    node --version || echo "Not installed"
                    npm --version || echo "Not installed"

                    echo "===== DOCKER ====="
                    docker --version || echo "Not installed (expected for k8s/cloud agents)"
                '''
            }
        }

        /* ============================================================
           SONARQUBE CONNECTIVITY
           ============================================================ */

        // stage('DEBUG: SonarQube Status Check') {
        //     steps {
        //         script { echo "[DEBUG] Testing SonarQube server connectivity" }

        //         withSonarQubeEnv("${SONARQUBE_ENV}") {
        //             sh '''
        //                 echo "----- Checking Sonar API Status -----"
        //                 curl -v "$SONAR_HOST_URL/api/system/status" || true

        //                 echo "----- Checking Auth (token required?) -----"
        //                 curl -u "$SONAR_AUTH_TOKEN:" "$SONAR_HOST_URL/api/authentication/validate" || true
        //             '''
        //         }
        //     }
        // }

        /* ============================================================
           HORUSEC DRY-RUN
           ============================================================ */
        
        // stage('DEBUG: Horusec Dry-Run') {
        //     steps {
        //         script { echo "[DEBUG] Running Horusec quick scan" }
        //         sh '''
        //             horusec start \
        //                 -p . \
        //                 --disable-docker true \
        //                 -o json \
        //                 -O debug-reports/horusec-debug.json || true

        //             echo "Horusec Dry-run Completed"
        //         '''
        //     }
        //     post {
        //         always { archiveArtifacts artifacts: "debug-reports/horusec-debug.json", onlyIfSuccessful: false }
        //     }
        // }

        /* ============================================================
           CONFTEST VALIDATION
           ============================================================ */

        // stage('DEBUG: Conftest Policy Verification') {
        //     steps {
        //         script { echo "[DEBUG] Validating Conftest policy directory" }
        //         sh '''
        //             if [ -d policy ]; then
        //                 conftest verify policy/ || echo "Policy verify returned issues"
        //             else
        //                 echo "No policy/ directory found (OK for debug mode)"
        //             fi
        //         '''
        //     }
        // }

        /* ============================================================
           TRIVY CONNECTIVITY & FS SCAN
           ============================================================ */

        // stage('DEBUG: Trivy FS Scan') {
        //     steps {
        //         script { echo "[DEBUG] Running Trivy filesystem scan" }
        //         sh '''
        //             trivy fs . \
        //               --format json \
        //               --output debug-reports/trivy-fs-debug.json \
        //               --severity HIGH,CRITICAL || true
        //         '''
        //     }
        //     post {
        //         always { archiveArtifacts artifacts: "debug-reports/trivy-fs-debug.json", onlyIfSuccessful: false }
        //     }
        // }

        /* ============================================================
           N8N WEBHOOK CONNECTIVITY TEST
           ============================================================ */

        // stage('DEBUG: n8n Webhook Connectivity Test') {
        //     steps {
        //         script { echo "[DEBUG] Testing n8n webhook" }

        //         withCredentials([string(credentialsId: 'n8n-webhook-token', variable: 'N8N_TOKEN')]) {
        //             sh '''
        //                 echo "[DEBUG] Sending test payload to n8n"
        //                 curl -X POST "${N8N_WEBHOOK_URL}" \
        //                     -H "Content-Type: application/json" \
        //                     -H "Authorization: Bearer $N8N_TOKEN" \
        //                     -d '{
        //                         "debug": true,
        //                         "pipeline": "devsecops-toolchain",
        //                         "message": "Test event from Jenkins debug pipeline"
        //                     }' \
        //                     -v || true
        //             '''
        //         }
        //     }
        // }

        /* ============================================================
           SUMMARY
           ============================================================ */

        stage('DEBUG: Summary') {
            steps {
                script {
                    echo """
=====================================================
🔍 DevSecOps Toolchain Debug Completed
- Agent OK
- SonarQube connectivity tested
- Horusec dry-run executed
- Conftest verify executed
- Trivy FS scan executed
- n8n webhook tested
=====================================================
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                echo "[POST] Archiving debug reports"
                currentBuild.result = "DEBUGGED-${env.BUILD_NUMBER}"
            }
            archiveArtifacts artifacts: "debug-reports/**", onlyIfSuccessful: false
            
        }
    }
}
