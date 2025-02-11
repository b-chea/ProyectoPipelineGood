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
                        // Create auth payload file
                        writeFile file: 'auth.json', text: """{
                            "client_id": "${XRAY_CLIENT_ID}",
                            "client_secret": "${XRAY_CLIENT_SECRET}"
                        }"""

                        // Get token and save to file to avoid command line issues
                        bat '''
                            curl -X POST ^
                            -H "Content-Type: application/json" ^
                            -d @auth.json ^
                            https://xray.cloud.getxray.app/api/v2/authenticate > token.txt
                        '''

                        // Read token from file and clean up
                        def token = readFile('token.txt').trim()
                        bat 'del auth.json token.txt'

                        // Store clean token
                        env.XRAY_TOKEN = token.replaceAll('"', '')

                        // Verify token is not empty
                        if (!env.XRAY_TOKEN?.trim()) {
                            error "Failed to obtain valid Xray token"
                        }
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
                    // Create payload file
                    def payload = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson([
                        steps: new groovy.json.JsonSlurper().parseText(env.FORMATTED_TEST_STEPS),
                        callTestDatasets: [],
                        importType: "csv"
                    ]))

                    writeFile file: 'payload.json', text: payload

                    // Write token to file to avoid command line issues
                    writeFile file: 'token.txt', text: env.XRAY_TOKEN

                    // Use token from file in curl command
                    bat '''
                        set /p XRAY_TOKEN=<token.txt
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

    post {
        always {
            bat '''
                if exist payload.json del payload.json
                if exist token.txt del token.txt
                if exist auth.json del auth.json
            '''
        }
        success {
            echo 'Successfully created Jira issue with test steps!'
        }
        failure {
            echo 'Build failed and Jira issue was not created!'
        }
    }
}