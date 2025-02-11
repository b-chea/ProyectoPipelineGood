pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
        XRAY_URL = 'https://us.xray.cloud.getxray.app/api/internal/10000/test/10042/import'
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
                withCredentials([
                    usernamePassword(credentialsId: 'xray-credentials',
                        usernameVariable: 'XRAY_CLIENT_ID',
                        passwordVariable: 'XRAY_CLIENT_SECRET')
                ]) {
                    script {
                        // Write credentials to a temporary file to avoid command line exposure
                        writeFile file: 'auth.json', text: """{
                            "client_id": "${XRAY_CLIENT_ID}",
                            "client_secret": "${XRAY_CLIENT_SECRET}"
                        }"""

                        // Get authentication token using file
                        def response = bat(
                            script: '''
                                curl -X POST ^
                                -H "Content-Type: application/json" ^
                                -d @auth.json ^
                                https://xray.cloud.getxray.app/api/v2/authenticate
                            ''',
                            returnStdout: true
                        ).trim()

                        // Clean up auth file immediately
                        bat 'del auth.json'

                        // Store token, removing quotes and any whitespace
                        env.XRAY_TOKEN = response.replaceAll('["\\s]', '')
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
                withCredentials([
                    usernamePassword(credentialsId: 'jenkins-credentials-local',
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_AUTH_PSW')
                ]) {
                    script {
                        // Create payload file with proper JSON formatting
                        def payload = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson([
                            steps: new groovy.json.JsonSlurper().parseText(env.FORMATTED_TEST_STEPS),
                            callTestDatasets: [],
                            importType: "csv"
                        ]))

                        writeFile file: 'payload.json', text: payload

                        // Use bat with a heredoc-style command to avoid escaping issues
                        bat '''
                            curl -X POST ^
                            -H "Authorization: Bearer %XRAY_TOKEN%" ^
                            -H "Content-Type: application/json" ^
                            -H "Accept: application/json" ^
                            "%XRAY_URL%?testVersionId=67a9f5d200cff4d61001bdd2&resetSteps=false" ^
                            -d @payload.json
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            // Clean up any temporary files
            bat 'del payload.json'
        }
        success {
            echo 'Successfully created Jira issue with test steps!'
        }
        failure {
            echo 'Build failed and Jira issue was not created!'
        }
    }
}