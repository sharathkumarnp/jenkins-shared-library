@Library('devops-shared-lib') _


pipeline {

    agent {
        kubernetes {
            cloud "kube-agents"
            defaultContainer 'production-tools'
            yaml k8sService.getAgentProductionToolsK8sYaml(productionToolsImage)
        }
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: getAllEnvs(), description: 'Select environment')
        string(name: 'NAME', description: '[Mandatory] Choose server name')
        choice(name: 'GET_API', choices: getAllApis(), description: '[Mandatory] Choose the relevant get API to run against the server')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        skipDefaultCheckout(true)
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        ansiColor('xterm')
    }
    environment {
        USE_JENKINS = 'True'
    }
    stages {
        stage('Prepare') {
            steps {
                deleteDir()
                script {
                    validateParams()
                    narcissusService.setNarcissusEnv(selectedEnvDomain.domainName)
                    storeService.setStoreEnv(selectedEnvDomain)
                }
            }
        }
        stage ('Run API Call') {
            steps {
                script {
                    if ("${GET_API}" == 'get-application-detail' || "${GET_API}" =='all_projects' || "${GET_API}" =='get_realms_list' || "${GET_API}" =='access_http_sso' || "${GET_API}" =='get_ldap_setting' || "${GET_API}" =='get_group_setting' || "${GET_API}" =='get_permissions' || "${GET_API}" =='get_access_version' || "${GET_API}" =='disable_api_key_creation'){
                        sh "cloudadmin_cli access --api ${GET_API} --customer ${NAME}"
                    }
                    else{
                        sh "aol_cli ${GET_API} --customer ${NAME}"
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                def buildDetails = [
                        jobName: env.JOB_NAME,
                        buildNumber: env.BUILD_NUMBER,
                        parameters: params
                ]
                publish_to_system(buildDetails) // --> passing the job details to the function
            }
        }
        always {
            deleteDir()
        }
    }
}

def getAllEnvs() {
    return environmentService.getAllEnvironments([''])
}

def getAllApis() {
    return ['', 'system_status', 'get_system_info']
}

def validateParams(){
    if (! params.NAME
            || !params.ENVIRONMENT
            || !params.GET_API){
        error 'Mandatory parameters not provided'
    }

    selectedEnvDomain = environmentService.validateSelectedEnv(params.ENVIRONMENT)
}