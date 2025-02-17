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
        stage('Authenticate Xray') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'xray-api-token', variable: 'XRAY_AUTH')]) {
                        def xrayResponse = bat(script: '''
                            curl -X POST ^
                            -H "Content-Type: application/json" ^
                            -d "{\\"client_id\\": \\"YOUR_CLIENT_ID\\", \\"client_secret\\": \\"%XRAY_AUTH%\\"}" ^
                            "https://xray.cloud.getxray.app/api/v2/authenticate" > xray_token.json
                        ''', returnStdout: true).trim()

                        def xrayToken = powershell(script: '''
                            $json = Get-Content xray_token.json -Raw | ConvertFrom-Json
                            echo $json
                        ''', returnStdout: true).trim()

                        if (!xrayToken) {
                            error "❌ ERROR: No se pudo autenticar con Xray."
                        }
                        env.XRAY_TOKEN = xrayToken
                    }
                }
            }
        }

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
                            $json = Get-Content issue_response.json -Raw | ConvertFrom-Json
                            echo $json.id
                        ''', returnStdout: true).trim()

                        if (!issueId) {
                            error "❌ ERROR: No se pudo obtener el issue key de Jira"
                        }
                        env.TEST_ID = issueId
                    }
                }
            }
        }

        stage('Get Test Version ID') {
            steps {
                script {
                    bat '''
                        curl -X GET ^
                        -H "Authorization: Bearer %XRAY_TOKEN%" ^
                        -H "Content-Type: application/json" ^
                        "%XRAY_BASE_URL%/%TEST_ID%" > test_info.json
                    '''

                    def testInfoContent = readFile('test_info.json').trim()
                    if (!testInfoContent || testInfoContent == "") {
                        error "❌ ERROR: test_info.json está vacío."
                    }

                    def testVersionId = powershell(script: '''
                        try {
                            $json = Get-Content test_info.json -Raw | ConvertFrom-Json
                            echo $json.testVersionId
                        } catch {
                            Write-Host "❌ ERROR: test_info.json no es un JSON válido."
                            exit 1
                        }
                    ''', returnStdout: true).trim()

                    if (!testVersionId) {
                        error "❌ ERROR: No se pudo obtener el testVersionId"
                    }
                    env.TEST_VERSION_ID = testVersionId
                }
            }
        }

        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    def testStepsContent = readFile('src/main/resources/data.csv').trim()
                    def testSteps = testStepsContent.split("\n")

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
                    if (!env.TEST_ID || !env.TEST_VERSION_ID) {
                        error "❌ ERROR: TEST_ID o TEST_VERSION_ID no están definidos"
                    }

                    writeFile file: 'payload.json', text: """{
                        \"steps\": ${env.FORMATTED_TEST_STEPS},
                        \"callTestDatasets\": [],
                        \"importType\": \"csv\"
                    }"""

                    bat '''
                        curl -X POST ^
                        -H "Authorization: Bearer %XRAY_TOKEN%" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json" ^
                        "%XRAY_BASE_URL%/%TEST_ID%/import?testVersionId=%TEST_VERSION_ID%&resetSteps=false" ^
                        -d @payload.json
                    '''
                }
            }
        }
    }

    post {
        always {
            bat 'del /F /Q payload.json create_issue.json issue_response.json test_info.json xray_token.json'
        }
        success {
            echo '✅ Successfully created Jira test issue and imported steps!'
        }
        failure {
            echo '❌ Build failed!'
        }
    }
}
