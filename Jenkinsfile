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
                        // Create auth payload file
                        writeFile file: 'auth.json', text: """{
                            "client_id": "${XRAY_CLIENT_ID}",
                            "client_secret": "${XRAY_CLIENT_SECRET}"
                        }"""

                        // Get token and save to file
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
                        // Create Jira test issue
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

                        // Create issue and get response
                        bat '''
                            curl -X POST ^
                            -u "%JIRA_USER%:%JIRA_AUTH_PSW%" ^
                            -H "Content-Type: application/json" ^
                            -d @create_issue.json ^
                            "%JIRA_URL%" > issue_response.json
                        '''

                        // Read and parse the response
                        def issueResponse = readFile('issue_response.json')
                        def issueJson = new groovy.json.JsonSlurper().parseText(issueResponse)
                        env.ISSUE_KEY = issueJson.key

                        // Get Xray test ID
                        bat '''
                            curl -H "Authorization: Bearer %XRAY_TOKEN%" ^
                            -H "Content-Type: application/json" ^
                            "https://us.xray.cloud.getxray.app/api/internal/10000/graphql" ^
                            -d "{\\"query\\":\\"query { getTests(jql: \\\\\\"key = %ISSUE_KEY%\\\\\\") { results { id testType { name } } } }\\"}" > test_info.json
                        '''

                        def testInfo = readFile('test_info.json')
                        def testJson = new groovy.json.JsonSlurper().parseText(testInfo)
                        env.TEST_ID = testJson.data.getTests.results[0].id

                        // Get latest test version
                        bat '''
                            curl -H "Authorization: Bearer %XRAY_TOKEN%" ^
                            -H "Content-Type: application/json" ^
                            "https://us.xray.cloud.getxray.app/api/internal/10000/graphql" ^
                            -d "{\\"query\\":\\"query { getTestById(id: \\\\\\"${TEST_ID}\\\\\\") { latestVersion { id } } }\\"}" > version_info.json
                        '''

                        def versionInfo = readFile('version_info.json')
                        def versionJson = new groovy.json.JsonSlurper().parseText(versionInfo)
                        env.VERSION_ID = versionJson.data.getTestById.latestVersion.id
                    }
                }
            }
        }

        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    def testSteps = readFile(file: 'src/main/resources/data.csv').readLines()
                    def formattedTestSteps = []

                    testSteps.drop(1).each { line ->
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

        stage('Import Test Steps') {
            steps {
                script {
                    def payload = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson([
                        steps: new groovy.json.JsonSlurper().parseText(env.FORMATTED_TEST_STEPS),
                        callTestDatasets: [],
                        importType: "csv"
                    ]))

                    writeFile file: 'payload.json', text: payload

                    // Use the dynamic TEST_ID and VERSION_ID
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