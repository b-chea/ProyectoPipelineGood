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
                    def jiraIssue = [
                        fields: [
                            project: [
                                key: '${JIRA_ISSUE_KEY}'
                            ],
                            summary: "Build and Test Report - ${env.BUILD_NUMBER}",
                            description: "Build and test results for build number ${env.BUILD_NUMBER}",
                            issuetype: [
                                name: env.JIRA_ISSUE_TYPE
                            ]
                        ]
                    ]

                    def response = jiraIssue issue: jiraIssue, site: env.JIRA_SITE, credentialsId: env.JIRA_CREDENTIALS_ID
                    echo "Created Jira issue: ${response.data.key}"
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

