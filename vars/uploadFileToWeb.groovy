#!/usr/bin/env groovy

def call(def localfile, def upload)
{
	echo "Uploading file '${localfile}' to WEB"
	// +x, otherwise we would spoil the upload token
	def httpStatus = sh script: "set +x; curl --write-out %{http_code} --silent --output /dev/stderr -X POST -k -F upload=@${localfile} '${upload}'", returnStdout: true
	// HTTP status code for HTTP OK is 200
	if (httpStatus == "200") {
		echo "Upload successful: curl reported HTTP status code: ${httpStatus}"
	} else {
		echo "Upload failed: curl reported HTTP status code: ${httpStatus}"
		currentBuild.result = 'FAILURE'
	}
}
