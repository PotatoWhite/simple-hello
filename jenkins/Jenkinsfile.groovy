def DOCKER_IMAGE_NAME = "bravopotato/simple-hello"
def DOCKER_IMAGE_TAGS = "demo"
def VERSION = "${env.BUILD_NUMBER}"

def NAMESPACE = 'ns-demo'
def DATE = new Date();

podTemplate(label: 'builder',
            containers: [
                containerTemplate(name: 'gradle', image: 'gradle:6.8.3-jdk11', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'docker', image: 'docker:20.10.5-dind', command: 'dockerd-entrypoint.sh', ttyEnabled: true, privileged: true),
                containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.20.5', command: 'cat', ttyEnabled: true)
            ],
            volumes: [
                hostPathVolume(mountPath: '/home/gradle/.gradle', hostPath: '/home/admin/k8s/jenkins/.gradle')
//                hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
            ]) {
    node('builder') {
        stage('Checkout') {
             checkout scm
        }
        stage('Build') {
            container('gradle') {
                /* 도커 이미지를 활용하여 gradle 빌드를 수행하여 ./build/libs에 jar파일 생성 */
                sh "gradle -x test build"
            }
        }
        stage('Docker build') {
            container('docker') {
                withCredentials([usernamePassword(
                    credentialsId: 'DOCKERHUB_CREDENTIAL',
                    usernameVariable: 'USERNAME',
                    passwordVariable: 'PASSWORD')]) {
                        /* ./build/libs 생성된 jar파일을 도커파일을 활용하여 도커 빌드를 수행한다 */
                        sh "docker build -t ${DOCKER_IMAGE_NAME}:${VERSION} ."
                        sh "docker login -u ${USERNAME} -p ${PASSWORD}"
                        sh "docker push ${DOCKER_IMAGE_NAME}:${VERSION}"
                }
            }
        }
        stage('Run kubectl') {
            container('kubectl') {
                withCredentials([usernamePassword(
                    credentialsId: 'DOCKERHUB_CREDENTIAL',
                    usernameVariable: 'USERNAME',
                    passwordVariable: 'PASSWORD')]) {
                        /* namespace 존재여부 확인. 미존재시 namespace 생성 */
                        sh "kubectl get ns ${NAMESPACE}|| kubectl create ns ${NAMESPACE}"
                        sh "echo ${DATE}"
                        sh "kubectl apply -f ./k8s/configmap.yml -n ${NAMESPACE}"
                        sh "kubectl apply -f ./k8s/service.yml -n ${NAMESPACE}"


                        sh "sed s%IMAGE_NAME_PLACEHOLDER%${DOCKER_IMAGE_NAME}:${VERSION}% k8s/deploy.yml > k8s-deploy.yml"
                        sh "cat k8s-deploy.yml"

                        sh "kubectl apply -f ./k8s-deploy.yml -n ${NAMESPACE}"
                }
            }
        }
    }
}