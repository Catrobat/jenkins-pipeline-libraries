#!/usr/bin/env groovy

@NonCPS
boolean call()
{
	for (def buildCause in currentBuild.rawBuild.causes) {
		if (buildCause?.shortDescription?.contains('Started by timer')) {
			return true
		}
	}

	false
}

