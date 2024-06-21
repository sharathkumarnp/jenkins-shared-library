// ~/jenkins-shared-library/vars/publish_to_system.groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def call(Map buildDetails) {
    def apiUrl = 'https://cts.jfrog.io/jenkins'
    def params = extractParameters(buildDetails)
    def redactedParams = redactPasswords(params)
    def payload = createPayload(buildDetails.jobName, buildDetails.buildNumber, redactedParams, buildDetails.parameters)

    println "Payload to be sent: ${JsonOutput.prettyPrint(JsonOutput.toJson(payload))}"

    sendUpdateToAPI(apiUrl, payload)
}

def extractParameters(Map buildDetails) {
    // Extract parameters and other relevant job details
    return buildDetails.getOrDefault('parameters', []).collect { param ->
        [key: param.name, value: param.value]
    }
}

def redactPasswords(List params) {
    // Redact sensitive data such as passwords
    params.each { param ->
        if (param.key.toLowerCase().contains('password') || param.key.toLowerCase().contains('secret')) {
            param.value = 'REDACTED'
        }
    }
    return params
}

def createPayload(String jobName, String buildNumber, List params, List parameters) {
    // Construct the payload including job parameters and potentially other metadata
    return [
        change: [
            after: params
        ],
        metadata: [
            timestamp: new Date(),
            user: 'sharathappu09@gmail.com',  // You might want to dynamically fetch the user
            jobName: jobName,
            buildNumber: buildNumber,
            entity: parameters
        ]
    ]
}

def sendUpdateToAPI(String apiUrl, Map payload) {
    // Convert payload to JSON
    def jsonPayload = JsonOutput.toJson(payload)
    println "Sending payload to API: ${jsonPayload}"
    def response = httpRequest(
        httpMode: 'POST',
        url: apiUrl,
        contentType: 'APPLICATION_JSON',
        requestBody: jsonPayload
    )

    // Check response status and log
    if (response.status == 200) {
        println "API call was successful: ${response.content}"
    } else {
        println "API call failed: ${response.status} - ${response.content}"
    }
}

