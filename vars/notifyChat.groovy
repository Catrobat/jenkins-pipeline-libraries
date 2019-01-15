#!/usr/bin/env groovy

/**
 * Send a slack notifications to the specified channels.
 *
 * @note In case the name of the channels changes you can remain
 *       backward compatible by adding a mapping here.
 */

def call(List channels = ['#ci-status']) {
    def buildStatusText = currentBuild.currentResult

    def channel = channels.join(',')
    def color = buildStatusToColor(currentBuild)
    def message = "${buildStatusText}: '${env.JOB_NAME} [${env.BUILD_DISPLAY_NAME}]' (<${env.BUILD_URL}|Open>)"

    echo "Send to Slack: [${channel}] ${color}: ${message}"
    slackSend(color: color, message: message, channel: channel)
}

def buildStatusToColor(def build) {
    def gray = '#E8E8E8'
    def mapping = ['ABORTED': gray,  'FAILURE': 'danger', 'NOT_BUILT': gray, 'SUCCESS': 'good', 'UNSTABLE': 'warning']
    return mapping.get(build.currentResult, gray)
}

