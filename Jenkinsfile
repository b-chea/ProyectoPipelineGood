pipeline {
    agent any
    environment{
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_CREDENTIALS_ID = 'jenkins-credentials'
        JIRA_ISSUE_KEY = 'PROY-123'
        JIRA_ISSUE_TYPE = 'ERROR'
    }
    tools {
        maven 'MAVEN_HOME'
        jdk 'JAVA_HOME'
    }
    stages {
        stage('Build') {
            steps {
                echo "Building..."
                //jiraAddComment comment: 'Build iniciada en Jenkins', idOrKey: 'PROY-123', site: 'bethsaidach-1738694022756.atlassian.net'
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


        stage('Create Jira Issue') {
            steps {
                script {
                    def jiraIssue = [
                        fields: [
                            project: [
                                key: env.JIRA_ISSUE_KEY
                            ],
                            summary: "Build failed for ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            description: "The build failed. Please check the Jenkins logs for more details.",
                            issuetype: [
                                name: env.JIRA_ISSUE_TYPE
                            ]
                        ]
                    ]

                    def response = httpRequest(
                        acceptType: 'APPLICATION_JSON',
                        contentType: 'APPLICATION_JSON',
                        httpMode: 'POST',
                        requestBody: groovy.json.JsonOutput.toJson(jiraIssue),
                        url: "${env.JIRA_SITE}/rest/api/4/issue",
                        authentication: env.JIRA_CREDENTIALS_ID
                    )

                    echo "Jira issue created: ${response}"
                    echo "Jira response: ${response.content}"
                }
            }
        }


    }
    post {

        success {
            echo 'Successfully!'
        }
        failure {
            echo 'Failed!'
        }

    }

}

