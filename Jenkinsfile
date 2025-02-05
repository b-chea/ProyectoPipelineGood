pipeline {
    agent any
    environment{
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_CREDENTIALS_ID = 'jenkins-credentials'
        JIRA_ISSUE_KEY = 'PROY-123'
        JIRA_ISSUE_TYPE = 'Bug'
    }
    tools {
        maven 'MAVEN_HOME'
        jdk 'JAVA_HOME'
    }
    stages {
        stage('Actualizar JIRA') {
            steps {
                jiraComment issueKey: "${JIRA_ISSUE_KEY}", comment: "Build iniciada en Jenkins: ${env.BUILD_URL}"
            }
        }

        stage('Compilar proyecto') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Ejectutar Pruebas') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Report to Jira') {
            steps {
                script {
                    // Define the Jira issue payload
                    def issuePayload = """
                    {
                        "fields": {
                            "project": {
                                "key": "PROY-123"
                            },
                            "summary": "Build and Test Report - ${env.BUILD_NUMBER}",
                            "description": "Build and test results for build number ${env.BUILD_NUMBER}",
                            "issuetype": {
                                "name": "${env.JIRA_ISSUE_TYPE}"
                            }
                        }
                    }
                    """

                    // Convert JIRA_CREDENTIALS_ID to a String and then encode it
                    def credentials = env.JIRA_CREDENTIALS_ID.toString()
                    def encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes())

                    // Create the Jira issue using the REST API
                    def response = httpRequest(
                        url: "${env.JIRA_SITE}/rest/api/2/issue",
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        customHeaders: [
                            [name: 'Authorization', value: "Basic ${encodedCredentials}"]
                        ],
                        requestBody: issuePayload
                    )

                    // Parse the response
                    def responseJson = readJSON text: response.content
                    echo "Created Jira issue: ${responseJson.key}"
                }
            }
        }


    }
    post{
        failure{
            jiraComment issueKey: "${JIRA_ISSUE_KEY}", comment: "Build fallida en Jenkins: ${env.BUILD_URL}"
        }
    }
}

