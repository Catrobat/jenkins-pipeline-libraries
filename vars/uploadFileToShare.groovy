#!/usr/bin/env groovy

def call(def localfile, def remotefile)
{
	echo "Upload file '${localfile}' to share as '${remotefile}'"
	sh "mv -f \"${localfile}\" \"${remotefile}\""
	sh "sha256sum \"${remotefile}\" > \"${remotefile}.sha256\""

	sshagent (credentials: ['files-catrob-at-upload']) {
		sh "echo \"put ${remotefile}\"        | sftp -o StrictHostKeychecking=no -b- file-downloads@files.catrob.at:www"
		sh "echo \"put ${remotefile}.sha256\" | sftp -o StrictHostKeychecking=no -b- file-downloads@files.catrob.at:www"
	}
}
