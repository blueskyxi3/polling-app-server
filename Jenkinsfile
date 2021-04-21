def label = "slave-${UUID.randomUUID().toString()}"

podTemplate(label: label, containers: [
  containerTemplate(name: 'maven', image: 'maven:3.6-alpine', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'kubectl', image: 'cnych/kubectl', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'helm', image: 'cnych/helm', command: 'cat', ttyEnabled: true)
], volumes: [
  persistentVolumeClaim(mountPath: '/root/.m2', claimName: 'jenkins-m2'),
  hostPathVolume(mountPath: '/home/jenkins/.kube', hostPath: '/root/.kube'),
  hostPathVolume(mountPath: '/root/.kube', hostPath: '/root/.kube'),
  hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
]) {
  node(label) {
    def myRepo = checkout scm
    def gitCommit = myRepo.GIT_COMMIT
    def gitBranch = myRepo.GIT_BRANCH
    def imageTag = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    def dockerRegistryUrl = "registry.citictel.com"
    def imageEndpoint = "demo/polling-app-server"
    def image = "${dockerRegistryUrl}/${imageEndpoint}"
    def branch = gitBranch.substring(gitBranch.indexOf("/")+1)
    
    parameters {
        gitParameter name: 'BranchOrTag', type: 'PT_BRANCH_TAG', defaultValue: 'master', listSize: '1', sortMode: 'DESCENDING_SMART', description: 'Select branch to build'
    }
    stage('版本檢查') {
      echo "版本檢查"
      sh """
         echo " ----------------------- "
         echo "gitBranch-->${gitBranch}"
         echo "set BranchOrTag is ${BranchOrTag}"
         echo "branch_name:${env.BRANCH_NAME}"
         echo "BUILD_NUMBER:${env.BUILD_NUMBER}"
         echo "BUILD_ID:${env.BUILD_ID}"
         echo "JOB_NAME:${env.JOB_NAME}"
         echo "BUILD_TAG:${env.BUILD_TAG}"
         echo "branch:${branch}"
         echo "imageTag--->${imageTag}"
         echo " ----------------------- "
         """   
      if(gitBranch.indexOf("/")>0){
        imageTag = branch + imageTag
      }else{
        echo "----else---"
        imageTag = BranchOrTag
      }
      echo "imageTag--->${imageTag}"
    }
    
    stage('代码编译打包') {
      try {
      container('maven') {
        echo "2. 代码编译打包阶段"
        sh "mvn clean package -Dmaven.test.skip=true"
      }
    } catch (exc) {
      println "构建失败 - ${currentBuild.fullDisplayName}"
      throw(exc)
    }
   }
    stage('构建 Docker 镜像') {
      withCredentials([[$class: 'UsernamePasswordMultiBinding',credentialsId: 'dockerhub',usernameVariable: 'DOCKER_HUB_USER',passwordVariable: 'DOCKER_HUB_PASSWORD']]) {
        container('docker') {
          echo "3. 构建 Docker 镜像阶段"
          sh """
            docker login ${dockerRegistryUrl} -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}
            docker build -t ${image}:${imageTag} .
            docker push ${image}:${imageTag}
            """
        }
      }
    }
    
    stage('运行 Kubectl') {
      
      container('kubectl') {          
       echo "查看 K8S 集群 Pod 列表"
        sh "kubectl get pods"   
        
        sh """
          sed -i "s/<BUILD_TAG>/${branch}/" manifests/k8s.yaml
          sed -i "s/<CI_ENV>/${branch}/" manifests/k8s.yaml
          kubectl apply -f manifests/k8s.yaml
          """
      }
    }
    stage('运行 Helm') {
      container('helm') {
        echo "查看 Helm Release 列表"
        sh "helm list"
      }
    }
  }
}
