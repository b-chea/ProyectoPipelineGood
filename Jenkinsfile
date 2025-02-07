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

        stage('Create Jira Test Issue') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        def testIssueResponse = bat(script: """
                            curl -X POST ^
                            -H "Authorization: ${authHeader}" ^
                            -H "Content-Type: application/json" ^
                            -H "Accept: application/json" ^
                            --data "{ \\"fields\\": { \\"project\\": { \\"key\\": \\"${JIRA_ISSUE_KEY}\\" }, \\"summary\\": \\"Prueba de Test desde Jenkins\\", \\"description\\": { \\"type\\": \\"doc\\", \\"version\\": 1, \\"content\\": [{\\"type\\": \\"paragraph\\", \\"content\\": [{\\"type\\": \\"text\\", \\"text\\": \\"Creando un issue de Test desde Jenkins\\"}]}] }, \\"issuetype\\": { \\"name\\": \\"Test\\" } } }" ^
                            "${JIRA_URL}"
                        """, returnStdout: true)

                        def testIssueKey = readJSON(text: testIssueResponse).key
                        env.TEST_ISSUE_KEY = testIssueKey
                        echo "Created Jira Test Issue: ${testIssueKey}"
                    }
                }
            }
        }

        stage('Ejecutar Pruebas') {
            steps {
                script {
                    try {
                        bat 'mvn test'
                        echo 'Tests executed successfully!'
                    } catch (Exception e) {
                        echo 'Tests failed, creating a Bug issue in JIRA...'
                        withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                            def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                            def bugIssueResponse = bat(script: """
                                curl -X POST ^
                                -H "Authorization: ${authHeader}" ^
                                -H "Content-Type: application/json" ^
                                -H "Accept: application/json" ^
                                --data "{ \\"fields\\": { \\"project\\": { \\"key\\": \\"${JIRA_ISSUE_KEY}\\" }, \\"summary\\": \\"Bug encontrado en Test ${env.TEST_ISSUE_KEY}\\", \\"description\\": { \\"type\\": \\"doc\\", \\"version\\": 1, \\"content\\": [{\\"type\\": \\"paragraph\\", \\"content\\": [{\\"type\\": \\"text\\", \\"text\\": \\"Bug encontrado durante la ejecución del Test ${env.TEST_ISSUE_KEY}\\"]}] }, \\"issuetype\\": { \\"name\\": \\"Bug\\" }, \\"customfield_10000\\": \\"${env.TEST_ISSUE_KEY}\\" } }" ^
                                "${JIRA_URL}"
                            """, returnStdout: true)

                            def bugIssueKey = readJSON(text: bugIssueResponse).key
                            echo "Created Jira Bug Issue: ${bugIssueKey} linked to Test Issue: ${env.TEST_ISSUE_KEY}"
                        }
                        error 'Tests failed, Bug issue created in JIRA.'
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