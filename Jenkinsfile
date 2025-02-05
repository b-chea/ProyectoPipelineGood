pipeline {
    agent any
    environment{
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_CREDENTIALS_ID = 'jenkins-credentials'
        JIRA_ISSUE_KEY = 'PROY-123'
    }
    tools{
        maven 'apache-maven-3.9.9'

    }
    stages {
        stage('Actualiza JIRA') {
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

        stage('Actualiza Jira con Éxito') {
            steps {
                script{
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS'){
                        jiraTrsnsitionIssue issueKey: "${JIRA_ISSUE_KEY}", comment: "Build exitosa: ${env.BUILD_URL}"
                    }
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

