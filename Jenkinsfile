pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
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

        stage('Create Jira Issue') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        def jsonPayload = "{\\\"fields\\\": {\\\"project\\\": {\\\"key\\\": \\\"PLPROJECT1\\\"}, \\\"summary\\\": \\\"Prueba desde Jenkins\\\", \\\"description\\\": {\\\"type\\\": \\\"doc\\\", \\\"version\\\": 1, \\\"content\\\": [{\\\"type\\\": \\\"paragraph\\\", \\\"content\\\": [{\\\"type\\\": \\\"text\\\", \\\"text\\\": \\\"Creando un issue desde Jenkins\\\"}]}]}, \\\"issuetype\\\": {\\\"name\\\": \\\"Bug\\\"}}}"

                        bat """
                curl -X POST ^
                -H "Authorization: ${authHeader}" ^
                -H "Content-Type: application/json" ^
                -H "Accept: application/json" ^
                --data "${jsonPayload}" ^
                "https://bethsaidach-1738694022756.atlassian.net/rest/api/3/issue"
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