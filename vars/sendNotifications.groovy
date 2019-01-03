#!/usr/bin/env groovy

/**
 * Send a slack notifications if the build status has changed
 */

def call()
{
	call(false)
}

def call(def buildStandalone) {
	List channels = ['#ci-status']
	if (buildStandalone) {
		channels += '#ci-status-standalone'
	}

	call(channels)
}

def call(Map args) {
	if (args.size() == 0) {
		call()
	} else if (args.size() == 1 && args.containsKey('buildStandalone')) {
		call(args['buildStandalone'] as boolean)
	} else {
		throw new IllegalArgumentException("Called with unsupported parameters ${args}.The only supported named paramter is buildStandalone.")
	}
}

def call(List channels)
{
	String buildStatus = currentBuild.currentResult ?: 'SUCCESS'
	String generalizedStatus = generalizeBuildStatus(currentBuild)
	String generalizedPrevStatus = generalizeBuildStatus(currentBuild.previousBuild)

	// From here on we only need to handle: SUCCESS, UNSTABLE, FAILURE
	boolean successful = (generalizedStatus == 'SUCCESS')
	boolean failed = (generalizedStatus == 'FAILURE')
	boolean backToNormal = (generalizedStatus == 'SUCCESS' && generalizedPrevStatus != 'SUCCESS')
	boolean unchanged = (generalizedStatus == generalizedPrevStatus)

	// 'non-standalone' builds:
	// do not report subsequent same results
	// only report success if back to normal (covered by above requirement)
	//
	// 'standalone' builds:
	// only report failures, unstable is used for 'expected' failure states
	// (eg: web triggers a build even if the program upload failed, so there
	// is no program to download and also not shown on WEB)
	// report subsequent failures to get aware of longstanding broken builds
	if ((!buildStandalone && unchanged) || (buildStandalone && !failed)) {
		return
	}

	// Set text
	def buildStatusText = buildStatus
	if (backToNormal) {
		buildStatusText = "${buildStatus} (Back to Normal)"
	}
	def message = "${buildStatusText}: '${env.JOB_NAME} [${env.BUILD_DISPLAY_NAME}]' (<${env.BUILD_URL}|Open>)"

	// Set color
	def color = 'good'
	if (generalizedStatus == 'SUCCESS') {
		color = 'good'
	} else if (generalizedStatus == 'UNSTABLE') {
		color = 'warning'
	} else {
		color = 'danger'
	}

	// Send notifications
	echo "Send to Slack: $channels ${color}: ${message}"
	slackSend(color: color, message: message, channel: channels.join(','))
}

/**
 * Maps the build status to fewer statuses (SUCCESS, UNSTABLE, FAILURE) for simplicity.
 */
String generalizeBuildStatus(def build)
{
	def statusMapping = ['ABORTED': 'FAILURE', 'NOT_BUILT': 'SUCCESS']
	return statusMapping.get(build?.currentResult, build?.currentResult ?: 'SUCCESS')
}
