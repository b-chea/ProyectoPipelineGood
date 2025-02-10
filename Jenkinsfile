pipeline {
    agent any
    environment {
        JIRA_SITE = 'https://bethsaidach-1738694022756.atlassian.net'
        JIRA_ISSUE_KEY = 'PLPROJECT1'
        JIRA_ISSUE_TYPE = 'Bug'
        JIRA_URL = "${JIRA_SITE}/rest/api/3/issue"
        XRAY_URL = 'https://us.xray.cloud.getxray.app/api/internal/10000/test/10042/import' // URL de Xray
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
                        -H "Authorization: ${authHeader}" ^
                        -H "Content-Type: application/json" ^
                        -H "Accept: application/json, text/plain, */*" ^
                        -H "Accept-Language: es-419,es;q=0.9,es-ES;q=0.8,en;q=0.7,en-GB;q=0.6,en-US;q=0.5" ^
                        -H "Connection: keep-alive" ^
                        -H "Origin: https://us.xray.cloud.getxray.app" ^
                        -H "Referer: https://us.xray.cloud.getxray.app/view/dialog/test/manual-steps-import?xdm_e=https%3A%2F%2Fbethsaidach-1738694022756.atlassian.net&xdm_c=channel-com.xpandit.plugins.xray__manual-steps-import&cp=&xdm_deprecated_addon_key_do_not_use=com.xpandit.plugins.xray&lic=active&cv=1001.0.0-SNAPSHOT&jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3MTIwMjA6MzllOWU0YjctNGEwZS00ODU4LWFhMzUtNDcxMzIyNjA0ZTFkIiwicXNoIjoiNjk4ODQ2ZWEyMTIzN2QxYzNiZjM0OTFjZGY2YzM4ZTcxNWVlZGMyMWMzNTA1YzEyN2RlNWVlMjdjOTE1NThjNyIsImlzcyI6IjU1MjhhMDM3LTBkMTUtM2U1My1hMDMzLTY2ZDVjMWU4NDEzYyIsImNvbnRleHQiOnt9LCJleHAiOjE3MzkyMDgwNTMsImlhdCI6MTczOTIwNzE1M30.4jOOu0xaVGMOP_px2C1M8l9QanhCQp1L_HP7tLiWe6Y" ^
                        -H "Sec-Fetch-Dest: empty" ^
                        -H "Sec-Fetch-Mode: cors" ^
                        -H "Sec-Fetch-Site: same-origin" ^
                        -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0" ^
                        -H "X-acpt: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3MTIwMjA6MzllOWU0YjctNGEwZS00ODU4LWFhMzUtNDcxMzIyNjA0ZTFkIiwicXNoIjoiY29udGV4dC1xc2giLCJpc3MiOiI1NTI4YTAzNy0wZDE1LTNlNTMtYTAzMy02NmQ1YzFlODQxM2MiLCJjb250ZXh0Ijp7ImxpY2Vuc2UiOnsiYWN0aXZlIjp0cnVlfSwidXJsIjp7ImRpc3BsYXlVcmwiOiJodHRwczpcL1wvYmV0aHNhaWRhY2gtMTczODY5NDAyMjc1Ni5hdGxhc3NpYW4ubmV0IiwiZGlzcGxheVVybFNlcnZpY2VkZXNrSGVscENlbnRlciI6Imh0dHBzOlwvXC9iZXRoc2FpZGFjaC0xNzM4Njk0MDIyNzU2LmF0bGFzc2lhbi5uZXQifSwiamlyYSI6eyJpc3N1ZSI6eyJpc3N1ZXR5cGUiOnsiaWQiOiIxMDAxNiJ9LCJrZXkiOiJQTFBST0pFQ1QxLTM4IiwiaWQiOiIxMDA0MiJ9LCJwcm9qZWN0Ijp7ImtleSI6IlBMUFJPSkVDVDEiLCJpZCI6IjEwMDAwIn19fSwiZXhwIjoxNzM5MjA4MTE1LCJpYXQiOjE3MzkyMDcyMTV9.Z82vU8qthgKkYu4RitWMwpfDLjtAIKtFqfnxvVOSbxg" ^
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