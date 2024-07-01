import groovy.json.JsonOutput
import groovy.json.JsonSlurper


def call(Map buildDetails) {
    def apiUrl = '<api-endpoint>'
    def params = buildDetails.parameters
    def redactedParams = redactPasswords(params)
    def payload = createPayload(buildDetails.jobName, currUserId, buildDetails.buildNumber, redactedParams)

    sendUpdateToAPI(apiUrl, payload)
}

def redactPasswords(Map params) {
    params.each { key, value ->
        if (key.toLowerCase().contains('password') || key.toLowerCase().contains('secret')) {
            params[key] = 'REDACTED'
        }
    }
    return params
}

def createPayload(String jobName, String buildNumber, currUserId, Map params) {
    // Construct the payload
    def PNameKey = params.keySet().find { key -> key.toLowerCase().startsWith('param-name') } // If you want to fetch any particular param to pass to entity
    def PName = PNameKey ? params[PNameKey] : 'UNKNOWN'

    return [
            change: params,
            metadata: [
                    timestamp: new Date(),
                    user: currUserId,
                    jobName: jobName,
                    buildNumber: buildNumber,
                    entity: PName
            ]
    ]
}

def sendUpdateToAPI(String apiUrl, Map payload) {

    // Converting payload to JSON
    def jsonPayload = JsonOutput.toJson(payload)
    def response = httpRequest(
            httpMode: 'POST',
            url: apiUrl,
            contentType: 'APPLICATION_JSON',
            requestBody: jsonPayload
    )

    // response status

    if (response.status == 200) {
        println "API call was successful: ${response.content}"
    } else {
        println "API call failed: ${response.status} - ${response.content}"
    }
}