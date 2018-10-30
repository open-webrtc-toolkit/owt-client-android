pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo "Building..:$GIT_COMMIT"
            }
        }
        stage('startMCU') {
            agent{
                node {
                    label 'webrtc151'
                }
            }
      
            steps {
                script{
                    withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                        sh '/home/webrtc/workspace/script/killserver.sh'
                        sh '/home/webrtc/workspace/script/startmcu.sh'
                        sh '/home/webrtc/workspace/script/checkmcustatus.sh'
                    }
                }
            }
        }
    }
}