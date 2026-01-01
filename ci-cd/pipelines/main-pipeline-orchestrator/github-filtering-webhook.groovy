@Library('devsecops-pipeline-library') _

pipeline {
    agent none
    triggers {
        GenericTrigger(
            genericVariables: [
                // Json path for basic parameters
                [key:'REF',value:'$.ref',expressionType:'JSONPath'],
                [key:'COMMIT_HASH_BEFORE',value:'$.before',expressionType:'JSONPath'],
                [key:'COMMIT_HASH_AFTER',value:'$.after',expressionType:'JSONPath'],
                [key:'REPOSITORY_NAME',value:'$.repository.name',expressionType:'JSONPath'],
                [key:'REPOSITORY_FULL_NAME',value:'$.repository.full_name',expressionType:'JSONPath'],
                [key:'UPDATED_AT',value:'$.repository.updated_at',expressionType:'JSONPath'],
                [key:'LANGUAGE',value:'$.repository.language',expressionType:'JSONPath'],
                [key:'BASE_REF',value:'$.base_ref',expressionType:'JSONPath'],
                [key:'COMMIT_URL',value:'$.head_commit.url',expressionType:'JSONPath'],
                [key:'AUTHOR',value:'$.head_commit.author.name',expressionType:'JSONPath'],
                [key:'COMMIT_MESSAGE',value:'$.head_commit.message',expressionType:'JSONPath'],
                [key:'ADDED',value:'$.head_commit.added',expressionType:'JSONPath'],
                [key:'REMOVED',value:'$.head_commit.removed',expressionType:'JSONPath'],
                [key:'MODIFIED',value:'$.head_commit.modified',expressionType:'JSONPath']

                ],
            causeString:'Triggered by github webhook',
            printContributedVariables: false,
            printPostContent: true,
            silentResponse: false,
            token:'GITHUB-WEBHOOK-TOKEN-DEVSECOPS',
            regexpFilterText: '$REF',
            regexpFilterExpression: '^refs/tags/.*$',
            // tokenCredentialId: 'creds-github-webhook'
        )
    }

    stages {
        stage("Setup Properties for Parameters") {
            steps {
                script {
                    properties([
                        parameters([
                            string(defaultValue: '', name: 'REPOSITORY_NAME', description: 'Repository name', trim: true),
                            string(defaultValue: '', name: 'REPOSITORY_FULL_NAME', description: 'Repository full name', trim: true),
                            string(defaultValue: '', name: 'BASE_REF', description: 'Base reference', trim: true),
                            string(defaultValue: '', name: 'REF', description: 'Git reference tag', trim: true),
                            string(defaultValue: '', name: 'COMMIT_URL', description: 'Commit URL', trim: true),
                            string(defaultValue: '', name: 'COMMIT_HASH_BEFORE', description: 'Commit hash before', trim: true),
                            string(defaultValue: '', name: 'COMMIT_HASH_AFTER', description: 'Commit hash after', trim: true),
                            string(defaultValue: '', name: 'LANGUAGE', description: 'Repository language', trim: true),
                            string(defaultValue: '', name: 'AUTHOR', description: 'Commit author name', trim: true),
                            string(defaultValue: '', name: 'COMMIT_MESSAGE', description: 'Commit message', trim: true),
                            string(defaultValue: '', name: 'ADDED', description: 'Added files', trim: true),
                            string(defaultValue: '', name: 'REMOVED', description: 'Removed files', trim: true),
                            string(defaultValue: '', name: 'MODIFIED', description: 'Modified files', trim: true),
                            string(defaultValue: '', name: 'UPDATED_AT', description: 'Repository updated at', trim: true),
                        ])
                    ])
                }
            }
        }
        stage('Normalize Webhook Context') {
            steps {
                script {
                    def envMap = env.getEnvironment()
                    webhookCtx = normalizeWebhook(envMap)
                    echo webhookCtx.toString()
    
                }
            }
        }

        // stage('Route to Downstream Pipeline') {
        //     steps {
        //         script {

        //         // === hasil parsing webhook sebelumnya ===
        //         def repo   = ctx.repo        // contoh: Rhisko/payment-service
        //         def branch = ctx.branch      // contoh: main
        //         def commit = ctx.after       // contoh: abc123
        //         def event  = ctx.eventType   // contoh: push

        //         // === routing table (sementara hardcoded, nanti bisa YAML) ===
        //         def routing = [
        //             'Rhisko/payment-service'     : 'DevSecOps/services/payment-service',
        //             'Rhisko/product-service'     : 'DevSecOps/services/product-service',
        //             'Rhisko/transaction-service' : 'DevSecOps/services/transaction-service',
        //             'Rhisko/portal-dashboard'    : 'DevSecOps/services/portal-dashboard'
        //         ]

        //         def downstreamJob = routing[repo]

        //         if (!downstreamJob) {
        //             echo "No downstream pipeline defined for ${repo}. Event ignored."
        //             return
        //         }

        //         echo "Triggering downstream pipeline: ${downstreamJob}"

        //         build job: downstreamJob,
        //                 wait: false,        // async (recommended)
        //                 parameters: [
        //                 string(name: 'REPO_FULL_NAME', value: repo),
        //                 string(name: 'BRANCH', value: branch),
        //                 string(name: 'COMMIT_SHA', value: commit),
        //                 string(name: 'EVENT_TYPE', value: event)
        //                 ]
        //         }
        //     }
        // }
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
                currentBuild.displayName = "${status} - ${REPOSITORY_NAME.toUpperCase()}-${BASE_REF}-${COMMIT_HASH_AFTER}-${BUILD_NUMBER}"
 
            }        
        }
    }
}

