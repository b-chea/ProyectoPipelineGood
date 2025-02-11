pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Test'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
        XRAY_BASE_URL = 'https://us.xray.cloud.getxray.app/api/internal/10000/test'
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
                    withCredentials([
                        usernamePassword(credentialsId: 'xray-credentials',
                            usernameVariable: 'XRAY_CLIENT_ID',
                            passwordVariable: 'XRAY_CLIENT_SECRET')
                    ]) {
                        writeFile file: 'auth.json', text: """{
                            "client_id": "${XRAY_CLIENT_ID}",
                            "client_secret": "${XRAY_CLIENT_SECRET}"
                        }"""

                        bat '''
                            curl -X POST ^
                            -H "Content-Type: application/json" ^
                            -d @auth.json ^
                            https://xray.cloud.getxray.app/api/v2/authenticate > token.txt
                        '''

                        def token = readFile('token.txt').trim()
                        bat 'del auth.json token.txt'
                        env.XRAY_TOKEN = token.replaceAll('"', '')
                    }
                }
            }
        }

        stage('Create Jira Test Issue') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local',
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_AUTH_PSW')]) {

                        writeFile file: 'create_issue.json', text: """{
                    "fields": {
                        "project": {
                            "key": "${JIRA_ISSUE_KEY}"
                        },
                        "summary": "Automated Test Case from Jenkins",
                        "description": {
                            "type": "doc",
                            "version": 1,
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "Automated test case generated from Jenkins pipeline"
                                        }
                                    ]
                                }
                            ]
                        },
                        "issuetype": {
                            "name": "${JIRA_ISSUE_TYPE}"
                        }
                    }
                }"""

                        // Create issue
                        bat '''
                    curl -X POST ^
                    -u "%JIRA_USER%:%JIRA_AUTH_PSW%" ^
                    -H "Content-Type: application/json" ^
                    -d @create_issue.json ^
                    "%JIRA_URL%" > issue_response.json
                '''

                        // For Windows, using PowerShell to extract the key
                        def issueKey = bat(script: '@powershell -Command "Get-Content issue_response.json | ConvertFrom-Json | Select-Object -ExpandProperty key"', returnStdout: true).trim()
                        env.ISSUE_KEY = issueKey

                        // Get Xray test ID using the extracted issue key
                        bat """
                    curl -H "Authorization: Bearer %XRAY_TOKEN%" ^
                    -H "Content-Type: application/json" ^
                    "https://us.xray.cloud.getxray.app/api/internal/10000/graphql" ^
                    -d "{\\"query\\":\\"query { getTests(jql: \\\\\\"key = ${issueKey}\\\\\\") { results { id testType { name } } } }\\"}" > test_info.json
                """

                        // Extract test ID using PowerShell
                        def testId = bat(script: '@powershell -Command "Get-Content test_info.json | ConvertFrom-Json | Select-Object -ExpandProperty data | Select-Object -ExpandProperty getTests | Select-Object -ExpandProperty results | Select-Object -First 1 | Select-Object -ExpandProperty id"', returnStdout: true).trim()
                        env.TEST_ID = testId

                        // Get version ID using the extracted test ID
                        bat """
                    curl -H "Authorization: Bearer %XRAY_TOKEN%" ^
                    -H "Content-Type: application/json" ^
                    "https://us.xray.cloud.getxray.app/api/internal/10000/graphql" ^
                    -d "{\\"query\\":\\"query { getTestById(id: \\\\\\"${testId}\\\\\\") { latestVersion { id } } }\\"}" > version_info.json
                """

                        // Extract version ID using PowerShell
                        def versionId = bat(script: '@powershell -Command "Get-Content version_info.json | ConvertFrom-Json | Select-Object -ExpandProperty data | Select-Object -ExpandProperty getTestById | Select-Object -ExpandProperty latestVersion | Select-Object -ExpandProperty id"', returnStdout: true).trim()
                        env.VERSION_ID = versionId

                        // Debug output
                        echo "Issue Key: ${env.ISSUE_KEY}"
                        echo "Test ID: ${env.TEST_ID}"
                        echo "Version ID: ${env.VERSION_ID}"
                    }
                }
            }
        }

        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    def testSteps = readFile(file: 'src/main/resources/data.csv').readLines()
                    def formattedSteps = new StringBuilder("[")

                    testSteps.drop(1).eachWithIndex { line, index ->
                        def parts = line.split(',')
                        if (parts.length >= 3) {
                            if (index > 0) formattedSteps.append(',')
                            formattedSteps.append("""
                                {
                                    "action": "${parts[0].trim()}",
                                    "data": "${parts[1].trim()}",
                                    "result": "${parts[2].trim()}"
                                }
                            """)
                        }
                    }
                    formattedSteps.append("]")

                    env.FORMATTED_TEST_STEPS = formattedSteps.toString()
                }
            }
        }

        stage('Import Test Steps') {
            steps {
                script {
                    writeFile file: 'payload.json', text: """{
                        "steps": ${env.FORMATTED_TEST_STEPS},
                        "callTestDatasets": [],
                        "importType": "csv"
                    }"""

                    bat '''
                        curl -X POST ^
                        -H "Authorization: Bearer %XRAY_TOKEN%" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json" ^
                        "https://us.xray.cloud.getxray.app/api/internal/10000/test/%TEST_ID%/import?testVersionId=%VERSION_ID%&resetSteps=false" ^
                        -d @payload.json
                    '''
                }
            }
        }
    }

    post {
        always {
            bat '''
                if exist payload.json del payload.json
                if exist create_issue.json del create_issue.json
                if exist issue_response.json del issue_response.json
                if exist test_info.json del test_info.json
                if exist version_info.json del version_info.json
            '''
        }
        success {
            echo 'Successfully created Jira test issue and imported steps!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}