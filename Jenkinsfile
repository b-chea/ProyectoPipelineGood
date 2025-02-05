pipeline {
    agent any
    environment{
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_CREDENTIALS_ID = 'jenkins-credentials'
        JIRA_ISSUE_KEY = 'PROY-123'
    }
    stages {
        stage('Actualiza JIRA') {
           steps {
               jiraAddComment issueKey: "{JIRA_ISSUE_KEY}", comment: "Build iniciada en Jenkins: ${env.BUILD_URL}"
            }
        }

        stage('Compilar proyecto') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Ejectutar Pruebas') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Actualiza Jira con Éxito') {
            steps {
                if (currentBuild.result == null || currentBuild.result == 'SUCCESS'){
                    jiraTrsnsitionIssue issueKey: "${JIRA_ISSUE_KEY}", comment: "Build exitosa: ${env.BUILD_URL}"
                }
            }
        }

        post{
            fauilure{
                jiraAddComment issueKey: "${JIRA_ISSUE_KEY}", comment: "Build fallida en Jenkins: ${env.BUILD_URL}"
            }
        }

    }
}