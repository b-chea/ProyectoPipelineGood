pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
        XRAY_URL = 'https://us.xray.cloud.getxray.app/api/internal/10000/test/10042/import'
        // Using credentials() binding for sensitive data
        XRAY_CREDENTIALS = credentials('xray-credentials')
    }

    tools {
        maven 'MAVEN_HOME'
        jdk 'JAVA_HOME'
    }

    stages {
        stage('Build') {
            steps {
                echo "Building..."
            }
        }

        stage('Compilar proyecto') {
            steps {
                bat 'mvn compile'
            }
        }

        stage('Ejecutar Pruebas') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Generate Xray Token') {
            steps {
                script {
                    // Using a more secure way to handle credentials
                    withCredentials([usernamePassword(credentialsId: 'xray-credentials',
                        usernameVariable: 'XRAY_CLIENT_ID',
                        passwordVariable: 'XRAY_CLIENT_SECRET')]) {
                        // Create authentication payload
                        def authPayload = """{"client_id": "${XRAY_CLIENT_ID}", "client_secret": "${XRAY_CLIENT_SECRET}"}"""

                        // Get authentication token
                        def response = bat(script: """
                            curl -X POST ^
                            -H "Content-Type: application/json" ^
                            -d "${authPayload}" ^
                            "https://xray.cloud.getxray.app/api/v2/authenticate"
                        """, returnStdout: true).trim()

                        // Store token securely
                        env.XRAY_TOKEN = response.replaceAll('"', '')
                    }
                }
            }
        }

        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    def testSteps = readFile(file: 'src/main/resources/data.csv').readLines()
                    def formattedTestSteps = []

                    testSteps.drop(1).each { line -> // Skip header if present
                        def parts = line.split(',')
                        if (parts.length >= 3) {
                            def step = [
                                action: parts[0].trim(),
                                data: parts[1].trim(),
                                result: parts[2].trim()
                            ]
                            formattedTestSteps << groovy.json.JsonOutput.toJson(step)
                        }
                    }

                    env.FORMATTED_TEST_STEPS = "[${formattedTestSteps.join(',')}]"
                }
            }
        }

        stage('Create Test Jira Issue with Steps') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local',
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_AUTH_PSW')]) {
                        // Create temporary file with proper JSON escaping
                        def jsonPayload = groovy.json.JsonOutput.toJson([
                            steps: new groovy.json.JsonSlurper().parseText(env.FORMATTED_TEST_STEPS),
                            callTestDatasets: [],
                            importType: "csv"
                        ])

                        writeFile(file: 'temp_payload.json', text: jsonPayload)

                        // Use powershell instead of bat for better handling of special characters
                        powershell """
                            \$headers = @{
                                'Authorization' = 'Bearer ${env.XRAY_TOKEN}'
                                'Content-Type' = 'application/json'
                                'Accept' = 'application/json, text/plain, */*'
                            }

                            \$uri = '${XRAY_URL}?testVersionId=67a9f5d200cff4d61001bdd2&resetSteps=false'
                            \$payload = Get-Content 'temp_payload.json' -Raw

                            Invoke-RestMethod -Uri \$uri -Method Post -Headers \$headers -Body \$payload
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            // Clean up temporary files
            cleanWs(patterns: [[pattern: 'temp_payload.json', type: 'INCLUDE']])
        }
        success {
            echo 'Successfully created Jira issue with test steps!'
        }
        failure {
            echo 'Build failed and Jira issue was not created!'
        }
    }
}