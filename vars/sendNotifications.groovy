#!/usr/bin/env groovy

/**
 * Send a slack notifications if the build status has changed
 */
def call(def notifyStandalone = false)
{
	String buildStatus = currentBuild.currentResult ?: 'SUCCESS'
	String generalizedStatus = generalizeBuildStatus(currentBuild)
	String generalizedPrevStatus = generalizeBuildStatus(currentBuild.previousBuild)

	boolean successful = (generalizedStatus == 'SUCCESS')
	boolean backToNormal = (generalizedStatus == 'SUCCESS' && generalizedPrevStatus != 'SUCCESS')
	boolean unchanged = (generalizedStatus == generalizedPrevStatus)

	// do not report subsequent same results
	// only report success if back to normal
	if (unchanged || (successful && !backToNormal)) {
		return
	}

	// Set text
	def buildStatusText = buildStatus
	if (backToNormal) {
		buildStatusText = "${buildStatus} (Back to Normal)"
	}
	def message = "${buildStatusText}: '${env.JOB_NAME} [#${env.BUILD_NUMBER}]' (<${env.BUILD_URL}|Open>)"

	// Set color
	def color = 'good'
	if (generalizedStatus == 'SUCCESS') {
		color = 'good'
	} else if (generalizedStatus == 'UNSTABLE') {
		color = 'warning'
	} else {
		color = 'danger'
	}

	// Set channel
	def channel = "#ci-status"
	if (notifyStandalone) {
		channel = "${channel},#ci-status-standalone"
	}

	// Send notifications
	echo "Send to Slack: [${channel}] ${color}: ${message}"
	slackSend(color: color, message: message, channel: channel)
}

/**
 * Maps the build status to fewer statuses (SUCCESS, UNSTABLE, FAILURE) for simplicity.
 */
String generalizeBuildStatus(def build)
{
	def statusMapping = ['ABORTED': 'FAILURE', 'NOT_BUILT': 'SUCCESS']
	return statusMapping.get(build?.currentResult, build?.currentResult ?: 'SUCCESS')
}
