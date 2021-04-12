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
    
    stage('版本選擇'){
      def userInput1 = input id: 'BranchID', message: 'please select version to deploy', parameters: [gitParameter(branch: '', branchFilter: '.*', defaultValue: '', description: '', listSize: '1', name: 'branchTag', quickFilterEnabled: false, selectedValue: 'TOP', sortMode: 'DESCENDING_SMART', tagFilter: '*', type: 'PT_BRANCH_TAG')]
      def version = ${userInput1.branchTag}
      echo "release version is ${version}."
    }
    stage('单元测试') {
      echo "测试阶段"
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
      def userInput = input id: 'inputId001', message: ' Ready to go?', parameters: [choice(choices: ['Dev', 'Stg', 'Prd'], description: 'production information', name: 'Env'), booleanParam(defaultValue: true, description: '', name: 'flag')]
      echo "This is a deploy step to ${userInput.Env}" 
      for(e in env){
        echo e + " is " + ${e}
      }
      container('kubectl') {          
       echo "查看 K8S 集群 Pod 列表"
       if (userInput.Env == "Dev") {
       // deploy dev stuff
        } else if (userInput.Env == "QA"){
       // deploy qa stuff
        } else {
       // deploy prod stuff
        }
        sh "kubectl get pods"    
        sh """
          sed -i "s/<BUILD_TAG>/${imageTag}/" manifests/k8s.yaml
          sed -i "s/<CI_ENV>/${version}/" manifests/k8s.yaml
          sed -i "s/<CI_ENV>/${env.BRANCH_NAME}/" manifests/k8s.yaml
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
