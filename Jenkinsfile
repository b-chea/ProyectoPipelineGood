pipeline {
    agent any

    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_PROJECT_KEY = 'PROY-123'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_CREDENTIALS_ID = 'jenkins-credentials'
    }

    stages {
        stage('Build') {
            steps {
                echo 'Building...'
                // Aquí irían los pasos de construcción de tu proyecto
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
                // Aquí irían los pasos de pruebas de tu proyecto
            }
        }
        stage('Report to Jira') {
            steps {
                script {
                    def jiraIssue = [
                        fields: [
                            project: [
                                key: env.JIRA_PROJECT_KEY
                            ],
                            summary: "Build and Test Report - ${env.BUILD_NUMBER}",
                            description: "Build and test results for build number ${env.BUILD_NUMBER}",
                            issuetype: [
                                name: env.JIRA_ISSUE_TYPE
                            ]
                        ]
                    ]

                    def response = jiraNewIssue issue: jiraIssue, site: env.JIRA_SITE
                    echo "Created Jira issue: ${response.data.key}"
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up...'
            // Aquí irían los pasos de limpieza, si es necesario
        }
    }
}
