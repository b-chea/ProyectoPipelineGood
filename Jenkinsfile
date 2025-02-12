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
                    withCredentials([usernamePassword(credentialsId: 'xray-credentials',
                        usernameVariable: 'XRAY_CLIENT_ID',
                        passwordVariable: 'XRAY_CLIENT_SECRET')]) {

                        writeFile file: 'auth.json', text: """{
                            \"client_id\": \"${XRAY_CLIENT_ID}\",
                            \"client_secret\": \"${XRAY_CLIENT_SECRET}\"
                        }"""

                        bat '''
                            curl -X POST ^
                            -H "Content-Type: application/json" ^
                            -d @auth.json ^
                            https://xray.cloud.getxray.app/api/v2/authenticate > token.txt
                        '''

                        def token = readFile('token.txt').trim().replaceAll('"', '')
                        bat 'del auth.json token.txt'
                        env.XRAY_TOKEN = token
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
                            \"fields\": {
                                \"project\": { \"key\": \"${JIRA_ISSUE_KEY}\" },
                                \"summary\": \"Automated Test Case from Jenkins\",
                                \"description\": { \"type\": \"doc\", \"version\": 1, \"content\": [{ \"type\": \"paragraph\", \"content\": [{ \"type\": \"text\", \"text\": \"Automated test case generated from Jenkins pipeline\" }] }] },
                                \"issuetype\": { \"name\": \"${JIRA_ISSUE_TYPE}\" }
                            }
                        }"""

                        bat '''
                            curl -X POST ^
                            -u "%JIRA_USER%:%JIRA_AUTH_PSW%" ^
                            -H "Content-Type: application/json" ^
                            -d @create_issue.json ^
                            "%JIRA_URL%" > issue_response.json
                        '''

                        def issueId = powershell(script: '''
                            $json = Get-Content issue_response.json -Raw | ConvertFrom-Json;
                            echo $json.key
                        ''', returnStdout: true).trim()

                        if (!issueId) {
                            error "No se pudo obtener el issue key de Jira"
                        }
                        env.TEST_ID = issueId
                    }
                }
            }
        }

        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    def testSteps = readFile(file: 'src/main/resources/data.csv').readLines()
                    def formattedSteps = testSteps.drop(1).collect { line ->
                        def parts = line.split(',')
                        if (parts.length >= 3) {
                            return """{
                                \"action\": \"${parts[0].trim()}\",
                                \"data\": \"${parts[1].trim()}\",
                                \"result\": \"${parts[2].trim()}\"
                            }"""
                        }
                    }.join(',')
                    env.FORMATTED_TEST_STEPS = "[${formattedSteps}]"
                }
            }
        }

        stage('Import Test Steps') {
            steps {
                script {
                    if (!env.TEST_ID) {
                        error "TEST_ID no está definido"
                    }

                    writeFile file: 'payload.json', text: """{
                        \"steps\": ${env.FORMATTED_TEST_STEPS},
                        \"callTestDatasets\": [],
                        \"importType\": \"csv\"
                    }"""

                    echo "Test ID: ${env.TEST_ID}"
                    echo "Xray Token: ${env.XRAY_TOKEN}"
                    bat 'type payload.json'

                    bat '''
                        curl -X POST ^
                        -H "Authorization: Bearer %XRAY_TOKEN%" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json" ^
                        "https://us.xray.cloud.getxray.app/api/internal/10000/test/%TEST_ID%/import?resetSteps=false" ^
                        -d @payload.json
                    '''
                }
            }
        }
    }

    post {
        always {
            bat '''
                del /F /Q payload.json create_issue.json issue_response.json
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
