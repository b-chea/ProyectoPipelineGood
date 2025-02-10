pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
        XRAY_URL = "${JIRA_SITE}/rest/raven/1.0/api/testexec"
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



        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    // Read CSV file with test steps
                    def testSteps = readFile(file: 'data.csv').readLines()
                    def formattedTestSteps = []

                    // Parse CSV and format test steps for Jira
                    testSteps.each { line ->
                        def (stepNumber, description, expectedResult) = line.split(',')
                        formattedTestSteps << [
                            "stepNumber": stepNumber.trim(),
                            "description": description.trim(),
                            "expectedResult": expectedResult.trim()
                        ]
                    }

                    // Store formatted test steps as environment variable
                    env.FORMATTED_TEST_STEPS = groovy.json.JsonOutput.toJson(formattedTestSteps)
                }
            }
        }

        stage('Create Test Jira Issue with Steps') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        // Create Jira Test Issue
                        def createIssueResponse = bat(
                            script: """
                        curl -X POST ^
                        -H "Authorization: ${authHeader}" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json" ^
                        --data "{ \\"fields\\": { \\"project\\": { \\"key\\": \\"PLPROJECT1\\" }, \\"summary\\": \\"Automated Test Case from Jenkins\\", \\"description\\": { \\"type\\": \\"doc\\", \\"version\\": 1, \\"content\\": [{\\"type\\": \\"paragraph\\", \\"content\\": [{\\"type\\": \\"text\\", \\"text\\": \\"Automated test case generated from Jenkins pipeline\\"}]}] }, \\"issuetype\\": { \\"name\\": \\"Test\\" } } }" ^
                        "${JIRA_URL}"
                      """,
                            returnStdout: true
                        ).trim()

                        // Parse issue key from response
                        def issueKey = (createIssueResponse =~ /"key":"([^"]+)"/)[0][1]

                        // Add test steps to the issue using Xray API
                        bat """
                        curl -X POST ^
                        -H "Authorization: ${authHeader}" ^
                        -H "Content-Type: application/json" ^
                        "${XRAY_URL}/testcase" ^
                        --data "{
                           \\"testCaseKey\\": \\"${issueKey}\\",
                           \\"steps\\": ${env.FORMATTED_TEST_STEPS}
                        }"
                   """
                    }
                }
            }
        }






        stage('Create Test Jira Issue') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        bat """
                        curl -X POST ^
                        -H "Authorization: ${authHeader}" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json" ^
                        --data "{ \\"fields\\": { \\"project\\": { \\"key\\": \\"PLPROJECT1\\" }, \\"summary\\": \\"Prueba desde Jenkins\\", \\"description\\": { \\"type\\": \\"doc\\", \\"version\\": 1, \\"content\\": [{\\"type\\": \\"paragraph\\", \\"content\\": [{\\"type\\": \\"text\\", \\"text\\": \\"Creando un issue test desde Jenkins\\"}]}] }, \\"issuetype\\": { \\"name\\": \\"Test\\" } } }" ^
                        "${JIRA_URL}"
                        """
                    }
                }
            }
        }

        stage('Create Jira Issue') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        bat """
                        curl -X POST ^
                        -H "Authorization: ${authHeader}" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json" ^
                        --data "{ \\"fields\\": { \\"project\\": { \\"key\\": \\"PLPROJECT1\\" }, \\"summary\\": \\"Bug desde Jenkins\\", \\"description\\": { \\"type\\": \\"doc\\", \\"version\\": 1, \\"content\\": [{\\"type\\": \\"paragraph\\", \\"content\\": [{\\"type\\": \\"text\\", \\"text\\": \\"Creando un issue desde Jenkins\\"}]}] }, \\"issuetype\\": { \\"name\\": \\"Bug\\" } } }" ^
                        "${JIRA_URL}"
                        """
                    }
                }
            }
        }



    }

    post {
        success {
            echo 'Successfully created Jira issue!'
        }
        failure {
            echo 'Build failed and Jira issue was not created!'
        }
    }
}