#!/usr/bin/env groovy

def call(def localfile, def upload)
{
	echo "Uploading file '${localfile}' to WEB"
	// +x, otherwise we would spoil the upload token
	sh "set +x; curl -X POST -k -F upload=@${localfile} '${upload}'"
}
