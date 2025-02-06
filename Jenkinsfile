pipeline {
    agent any
    environment{
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_CREDENTIALS_ID = 'jenkins-credentials'
        JIRA_ISSUE_KEY = 'PROY-123'
        JIRA_ISSUE_TYPE = 'TASK'
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

        stage('Create Jira Issue') {
            steps {
                script {
                    jiraIssueSelector(issueSelector: [$class: 'ExplicitIssueSelector', issueKeys: 'PROY-123'])


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

