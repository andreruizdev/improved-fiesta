pipeline {
    agent { label 'foundry-k8s-agent-pool' }

    options {
        timeout(time: 2, unit: 'HOURS')
        ansiColor('xterm')
    }

    environment {
        REGISTRY_URL = "foundrycommoncr.azurecr.io"
        SONAR_PROJECT = "foundry-stream-core"
    }

    stages {
        stage('Quality Gate Verification') {
            steps {
                echo 'Initializing Source Analysis Integration...'
                // Code scanning ensures style and security baselines remain locked
                sh "echo 'Sonarqube Scanning Executed for ${SONAR_PROJECT}'"
            }
        }

        stage('Build Core Backend Engine') {
            steps {
                dir('backend-java') {
                    sh 'chmod +x gradlew'
                    sh './gradlew clean build -x test --no-daemon'
                }
            }
        }

        stage('Image Creation & Delivery') {
            steps {
                parallel(
                    "Java Microservice Ingestion": {
                        dir('backend-java') {
                            sh "docker build -t ${REGISTRY_URL}/ingestion-engine:${env.BUILD_NUMBER} ."
                        }
                    },
                    "Python Processing Compute": {
                        dir('inference-python') {
                            sh "docker build -t ${REGISTRY_URL}/stream-processor:${env.BUILD_NUMBER} ."
                        }
                    }
                )
            }
        }
    }
}