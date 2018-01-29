#!/usr/bin/env groovy

/**
 * Send a slack notifications if the build status has changed
 */
def call(def notifyStandalone = 'false')
{
	// default to success
	def buildStatus = 'SUCCESS'
	def prevBuildStatus = 'SUCCESS'
	if (currentBuild.currentResult) {
		buildStatus = currentBuild.currentResult
	}
	if (currentBuild.previousBuild && currentBuild.previousBuild.currentResult) {
		prevBuildStatus = currentBuild.previousBuild.currentResult
	}

	// Treat Aborted as Failure and NotBuild as Success
	String generalizedStatus = buildStatus
	if (generalizedStatus == 'ABORTED') {
		generalizedStatus = 'FAILURE'
	}
	if (generalizedStatus == 'NOT_BUILT') {
		generalizedStatus = 'SUCCESS'
	}

	String generalizedPrevStatus = prevBuildStatus
	if (generalizedPrevStatus == 'ABORTED') {
		generalizedPrevStatus = 'FAILURE'
	}
	if (generalizedPrevStatus == 'NOT_BUILT') {
		generalizedPrevStatus = 'SUCCESS'
	}

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
