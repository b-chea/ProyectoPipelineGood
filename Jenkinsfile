pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
        XRAY_URL = 'https://us.xray.cloud.getxray.app/api/internal/10000/test/10042/import' // URL de Xray

        XRAY_CLIENT_ID = credentials('xray-client-id')
        XRAY_CLIENT_SECRET = credentials('xray-client-secret')
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

        stage('Generate Xray Token') {
            steps {
                script {
                    // Generar un nuevo token JWT
                    def tokenResponse = sh(script: """
                        curl -X POST \
                        -H "Content-Type: application/json" \
                        -d '{"client_id": "${XRAY_CLIENT_ID}", "client_secret": "${XRAY_CLIENT_SECRET}"}' \
                        "https://xray.cloud.getxray.app/api/v2/authenticate"
                    """, returnStdout: true).trim()

                    // Extraer el token de la respuesta
                    env.XRAY_TOKEN = tokenResponse.replaceAll('"', '')
                }
            }
        }

        stage('Create Jira Issue') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        // Crear un issue en Jira
                        def createIssueResponse = bat(script: """
                            curl -X POST ^
                            -H "Authorization: ${authHeader}" ^
                            -H "Content-Type: application/json" ^
                            -H "Accept: application/json" ^
                            --data "{ \\"fields\\": { \\"project\\": { \\"key\\": \\"${JIRA_ISSUE_KEY}\\" }, \\"summary\\": \\"Automated Test Case from Jenkins\\", \\"description\\": { \\"type\\": \\"doc\\", \\"version\\": 1, \\"content\\": [{\\"type\\": \\"paragraph\\", \\"content\\": [{\\"type\\": \\"text\\", \\"text\\": \\"Automated test case generated from Jenkins pipeline\\"}]}] }, \\"issuetype\\": { \\"name\\": \\"${JIRA_ISSUE_TYPE}\\" } } }" ^
                            "${JIRA_URL}"
                        """, returnStdout: true).trim()

                        // Extraer la clave del issue creado
                        def issueKey = (createIssueResponse =~ /"key":"([^"]+)"/)[0][1]
                        echo "Created Jira issue: ${issueKey}"
                    }
                }
            }
        }

        stage('Prepare CSV Test Steps') {
            steps {
                script {
                    // Leer el archivo CSV y formatear los pasos de prueba
                    def testSteps = readFile(file: 'src/main/resources/data.csv').readLines()
                    def formattedTestSteps = []

                    testSteps.each { line ->
                        def parts = line.split(',')
                        if (parts.length >= 3) {
                            formattedTestSteps << """
                            {
                                "action": "${parts[0].trim()}",
                                "data": "${parts[1].trim()}",
                                "result": "${parts[2].trim()}"
                            }
                            """
                        }
                    }

                    // Convertir la lista a un JSON válido
                    env.FORMATTED_TEST_STEPS = '[' + formattedTestSteps.join(',') + ']'
                }
            }
        }

        stage('Create Test Jira Issue with Steps') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'jenkins-credentials-local', usernameVariable: 'JIRA_USER', passwordVariable: 'JIRA_AUTH_PSW')]) {
                        def authHeader = "Basic " + "${JIRA_USER}:${JIRA_AUTH_PSW}".bytes.encodeBase64().toString()

                        // Crear un archivo temporal con el JSON
                        def jsonPayload = """
                        {
                            "steps": ${env.FORMATTED_TEST_STEPS},
                            "callTestDatasets": [],
                            "importType": "csv"
                        }
                        """
                        writeFile(file: 'temp_payload.json', text: jsonPayload)

                        // Enviar la solicitud a la API de Xray usando el archivo temporal
                        bat """
                        curl -X POST ^
                        -H "Authorization: Bearer ${env.XRAY_TOKEN}" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json, text/plain, */*" ^
                        -H "Accept-Language: es-419,es;q=0.9,es-ES;q=0.8,en;q=0.7,en-GB;q=0.6,en-US;q=0.5" ^
                        -H "Connection: keep-alive" ^
                        -H "Origin: https://us.xray.cloud.getxray.app" ^
                        -H "Referer: https://us.xray.cloud.getxray.app/view/dialog/test/manual-steps-import?xdm_e=https%3A%2F%2Fbethsaidach-1738694022756.atlassian.net&xdm_c=channel-com.xpandit.plugins.xray__manual-steps-import&cp=&xdm_deprecated_addon_key_do_not_use=com.xpandit.plugins.xray&lic=active&cv=1001.0.0-SNAPSHOT" ^
                        -H "Sec-Fetch-Dest: empty" ^
                        -H "Sec-Fetch-Mode: cors" ^
                        -H "Sec-Fetch-Site: same-origin" ^
                        -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0" ^
                        -H "X-acpt: ${env.XRAY_TOKEN}" ^
                        -H "sec-ch-ua: \\"Not A(Brand\\";v=\\"8\\", \\"Chromium\\";v=\\"132\\", \\"Microsoft Edge\\";v=\\"132\\"" ^
                        -H "sec-ch-ua-mobile: ?0" ^
                        -H "sec-ch-ua-platform: \\"Windows\\"" ^
                        "${XRAY_URL}?testVersionId=67a9f5d200cff4d61001bdd2&resetSteps=false" ^
                        --data @temp_payload.json
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Successfully created Jira issue with test steps!'
        }
        failure {
            echo 'Build failed and Jira issue was not created!'
        }
    }
}