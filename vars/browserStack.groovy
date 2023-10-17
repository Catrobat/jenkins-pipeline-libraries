#!/usr/bin/env groovy

/**
 * Sending Catty app to BrowserStack for automated mobile app testing and fetch build result.
 *
 * @param path_to_app path of the apk/ipa app.
 */

void call(final String path_to_app) {
    final String credentials = '${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}'
    final def message = browserStackCatty(credentials, path_to_app)

    browserStackPolling(credentials, message)
}

/**
 * Sending Catroid/Paintroid app to BrowserStack for automated mobile app testing and fetch build result.
 *
 * @param path_to_app path of the apk/ipa app.
 * @param path_to_test_suits path of the apk ui tests suits file.
 * @param indicate catroid or paintroid project
 */
void call(final String path_to_app, final String path_to_test_suits, final String project) {
    final String credentials = '${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}'
    final def message = browserStackCatOrPaintroid(credentials, path_to_app, path_to_test_suits, project)

    browserStackPolling(credentials, message)
}

final String getShardsCatty() {
    if (params.BROWSERSTACK_IOS_DEVICES.equals('IosDevices')) {
        return ""
    } else {
        def test_classes_shards = []
        String shards = ""
        def dir = "${workspace}/src/CattyUITests"
        def files = sh(script: "find ${dir} -name \"*.swift\"", returnStdout: true).split()
        def test_classes = files.findAll { it.endsWith("Tests.swift") || it.endsWith("Test.swift") }.collect { it.substring(it.lastIndexOf('/') + 1) }.collect {
            it.replaceAll(/\.swift$/, "")
        } + 'FormulaEditorSectionViewController'


        int n = test_classes.size() / params.BROWSERSTACK_SHARDS.toInteger()
        if (test_classes.size() % params.BROWSERSTACK_SHARDS.toInteger() > 0) {
            n += 1
        }

        for (int i = 0; i < test_classes.size(); i += n) {
            test_classes_shards << test_classes.subList(i, Math.min(i + n, test_classes.size())).collect { "\"$it\"" }
        }

        if (params.BROWSERSTACK_SHARDS.toInteger() == 2) {
            shards = """ \
            "shards": {"numberOfShards": 2, "mapping": [{"name": "Shard 1", "strategy": "only-testing", \
            "values": ${test_classes_shards[0]}}, \
            {"name": "Shard 2", "strategy": "only-testing", \
            "values": ${test_classes_shards[1]}}]},  \
        """
        } else if (params.BROWSERSTACK_SHARDS.toInteger() == 3) {
            shards = """ \
            "shards": {"numberOfShards": 3, "mapping": [{"name": "Shard 1", "strategy": "only-testing", \
            "values": ${test_classes_shards[0]}}, \
            {"name": "Shard 2", "strategy": "only-testing", \
            "values": ${test_classes_shards[1]}},\
            {"name": "Shard 3", "strategy": "only-testing", \
            "values": ${test_classes_shards[2]}}]},  \
        """
        } else if (params.BROWSERSTACK_SHARDS.toInteger() == 4) {
            shards = """ \
            "shards": {"numberOfShards": 4, "mapping": [{"name": "Shard 1", "strategy": "only-testing", \
            "values": ${test_classes_shards[0]}}, \
            {"name": "Shard 2", "strategy": "only-testing", \
            "values": ${test_classes_shards[1]}},\
            {"name": "Shard 3", "strategy": "only-testing", \
            "values": ${test_classes_shards[2]}}, \
            {"name": "Shard 4", "strategy": "only-testing", \
            "values": ${test_classes_shards[3]}}]},  \
        """
        } else if (params.BROWSERSTACK_SHARDS.toInteger() == 5) {
            shards = """ \
            "shards": {"numberOfShards": 5, "mapping": [{"name": "Shard 1", "strategy": "only-testing", \
            "values": ${test_classes_shards[0]}}, \
            {"name": "Shard 2", "strategy": "only-testing", \
            "values": ${test_classes_shards[1]}},\
            {"name": "Shard 3", "strategy": "only-testing", \
            "values": ${test_classes_shards[2]}}, \
            {"name": "Shard 4", "strategy": "only-testing", \
            "values": ${test_classes_shards[3]}}, \
            {"name": "Shard 5", "strategy": "only-testing", \
            "values": ${test_classes_shards[4]}}]},  \
        """
        }

        return shards
    }
}

void checkIfBrowserStackIsReady(final String credentials, final boolean isAnyDevice) {
    int status = 0
    int queued_sessions_max_allowed = 5

    String response, code
    (response, code) = sh(script: """ \
                        curl -w' \\n%{response_code}' \
                        -u "$credentials" \
                        https://api.browserstack.com/app-automate/plan.json \
                        """, returnStdout: true).trim().tokenize("\n")
    queued_sessions_max_allowed = sh(script: "echo \'$response\' | jq '.queued_sessions_max_allowed'", returnStdout: true).trim().toInteger()
    while (code.toInteger() == 200) {
        final int queued_sessions = sh(script: "echo \'$response\' | jq '.queued_sessions'", returnStdout: true).trim().toInteger()
        status = queued_sessions + params.BROWSERSTACK_SHARDS.toInteger()
        if (status <= queued_sessions_max_allowed && !isAnyDevice) {
            return
        } else if (queued_sessions == 0 && isAnyDevice) {
            return
        }
        (response, code) = sh(script: """ \
                        curl -w' \\n%{response_code}' \
                        -u "$credentials" \
                        https://api.browserstack.com/app-automate/plan.json \
                        """, returnStdout: true).trim().tokenize("\n")
    }
}

final String getDevice(final String devices) {
    if (devices.equals('IosDevices')) {
        return '["iPhone 14 Pro-16", "iPhone 13-15", "iPhone XR-15", "iPhone 8-15", "iPhone SE 2022-15", "iPhone 12 Pro Max-14", "iPhone 11-14", "iPhone 12 Mini-14"]'
    } else if (devices.equals('AndroidDevices')) {
        return ''' \
            ["Google Pixel 7 Pro-13.0", "Samsung Galaxy S22 Ultra-12.0", "Google Pixel 5-11.0", \
            "Google Pixel 4-10.0", "Samsung Galaxy S10-9.0", "Samsung Galaxy S9-8.0", \
            "Google Pixel 2-8.0", "Samsung Galaxy S8-7.0", "Google Pixel-7.1", "Samsung Galaxy S7-6.0"] \
        '''
    } else {
        return "[\"${devices}\"]"
    }
}

final def browserStackCatty(final String credentials, final String path_to_app) {
    final String brow_app_url = '"https://api-cloud.browserstack.com/app-automate/xcuitest/v2/app"'
    final String brow_test_url = '"https://api-cloud.browserstack.com/app-automate/xcuitest/v2/test-suite"'
    final String build_url = '"https://api-cloud.browserstack.com/app-automate/xcuitest/v2/build"'
    final String device = getDevice(params.BROWSERSTACK_IOS_DEVICES)
    final String app_name = sh(script: "find ${path_to_app} -name \"*.ipa\"", returnStdout: true)
    final String test_suits_name = sh(script: "find ${path_to_app} -name \"*.zip\"", returnStdout: true)
    final String framework = 'xcuitest'
    final shards = getShardsCatty()

    final def response_app = sh(script: """ \
                                curl -u "$credentials" \
                                -X POST $brow_app_url -F "file=@${app_name}" \
                                """, returnStdout: true).trim()

    echo("Repsonse from upload of ${app_name} is: $response_app")

    final String app_url = sh(script: "echo '$response_app' | jq -r '.app_url'", returnStdout: true).trim()
    final String app_id = sh(script: "echo '$response_app' | jq -r '.app_id'", returnStdout: true).trim()

    final def response_test = sh(script: """ \
                                curl -u "$credentials" \
                                -X POST $brow_test_url -F "file=@${test_suits_name}" \
                                """, returnStdout: true).trim()

    echo("Repsonse from upload of ${test_suits_name} is: ${response_test}")

    final String test_suite_url = sh(script: "echo '$response_test' | jq -r '.test_suite_url'", returnStdout: true).trim()
    final String test_suite_id = sh(script: "echo '$response_test' | jq -r '.test_suite_id'", returnStdout: true).trim()
    final String config = """ \
                             '{${shards}"singleRunnerInvocation": "false", "app": "${app_url}", "testSuite": "${test_suite_url}", \
                             "enableResultBundle": true, "devices": $device, "project": "Catrobat/Catty"}' \
                             """

    checkIfBrowserStackIsReady(credentials, params.BROWSERSTACK_IOS_DEVICES.equals('IosDevices'))

    final def response = sh(script: """ \
                                        curl -u "$credentials" \
                                        -X POST $build_url -d $config \
                                        -H "Content-Type: application/json" | jq -r '.message, .build_id' \
                                        """, returnStdout: true).trim().split("\n")

    final def message = [build_id: response[1], app_id: app_id, test_suite_id: test_suite_id, framework: framework]

    return message

}

final def browserStackCatOrPaintroid(final String credentials, final String path_to_app, final String path_to_test_suits, final String project) {
    final String brow_app_url = '"https://api-cloud.browserstack.com/app-automate/espresso/v2/app"'
    final String brow_test_url = '"https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suite"'
    final String build_url = '"https://api-cloud.browserstack.com/app-automate/espresso/v2/build"'
    final String device = getDevice(params.BROWSERSTACK_ANDROID_DEVICES)
    final String app_name = sh(script: "find ${path_to_app} -name \"*.apk\"", returnStdout: true)
    final String test_suits_name = sh(script: "find ${path_to_test_suits} -name \"*.apk\"", returnStdout: true)
    final String shards = params.BROWSERSTACK_ANDROID_DEVICES.equals('AndroidDevices') ? "1" : params.BROWSERSTACK_SHARDS
    final String framework = 'espresso'

    final def response_app = sh(script: """ \
                                curl -u "$credentials" \
                                -X POST $brow_app_url -F "file=@${app_name}" \
                                """, returnStdout: true).trim()

    echo("Repsonse from upload of ${app_name} is: $response_app")

    final String app_url = sh(script: "echo '$response_app' | jq -r '.app_url'", returnStdout: true).trim()
    final String app_id = sh(script: "echo '$response_app' | jq -r '.app_id'", returnStdout: true).trim()

    final def response_test = sh(script: """ \
                                curl -u "$credentials" \
                                -X POST $brow_test_url -F "file=@${test_suits_name}" \
                                """, returnStdout: true).trim()

    echo("Repsonse from upload of ${test_suits_name} is: ${response_test}")

    final String test_suite_url = sh(script: "echo '$response_test' | jq -r '.test_suite_url'", returnStdout: true).trim()
    final String test_suite_id = sh(script: "echo '$response_test' | jq -r '.test_suite_id'", returnStdout: true).trim()

    checkIfBrowserStackIsReady(credentials, params.BROWSERSTACK_ANDROID_DEVICES.equals('AndroidDevices'))

    final String config = """ \
                             '{"shards": {"numberOfShards": ${shards}, \
                             "deviceSelection": "all"}, "singleRunnerInvocation": "true", \
                             "app": "${app_url}", "testSuite": \
                             "${test_suite_url}", "devices":$device, "project": "Catrobat/${project}"}' \
                          """

    final def response = sh(script: """ \
                            curl -u "$credentials" \
                            -X POST $build_url -d $config \
                            -H "Content-Type: application/json" | jq -r '.message, .build_id' \
                            """, returnStdout: true).trim().split("\n")

    final def message = [build_id: response[1], app_id: app_id, test_suite_id: test_suite_id, framework: framework]

    return message
}

void browserStackPolling(final String credentials, final def message) {
    try {
        String response, code
        (response, code) = sh(script: """ \
                        curl -w' \\n%{response_code}' \
                        -u "$credentials" \
                        -X GET "https://api-cloud.browserstack.com/app-automate/${message.framework}/v2/builds/${message.build_id}" \
                        """, returnStdout: true).trim().tokenize("\n")
        while (code.toInteger() == 200) {
            status = sh(script: "echo '$response' | jq -r '.status'", returnStdout: true).trim()
            echo "Current status is: $status"
            if (status.equals('queued') || status.equals('running')) {
                sleep 60
                (response, code) = sh(script: """ \
                                curl -w' \\n%{response_code}' \
                                -u "$credentials" \
                                -X GET "https://api-cloud.browserstack.com/app-automate/${message.framework}/v2/builds/${message.build_id}" \
                                """, returnStdout: true).trim().tokenize("\n")
                continue
            } else if (status.equals('passed') || status.equals('failed') || status.equals('error') || status.equals('time out') || status.equals('skipped')) {
                getBrowserstackEndMessage(credentials, status, message)
                return
            } else {
                getBrowserstackEndMessage(credentials, status, message)
                return
            }
        }
        getBrowserstackEndMessage(credentials, status, message)
    } catch (exception) {
        echo "BrowserStack testing failed: ${exception}"
        deleteUpload(credentials, message)
    }
}

void getXmlFile(final String credentials, final def message) {
    if (!fileExists('BrowserstackReports')) {
        sh('mkdir BrowserstackReports')
    }
    sh('rm -rf BrowserstackReports/*')

    final def sessions = sh(script: """ \
                            curl -u "$credentials" \
                            -X GET "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/{$message.build_id}" \
                            | jq -r ".devices[].sessions[].id"\
                            """, returnStdout: true).trim().split("\n")
    for (session in sessions) {
        sh """ \
            curl -u "$credentials" \
            -X GET "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/$message.build_id/sessions/$session/report" \
            -o BrowserstackReports/${session}.xml
        """
    }
    deleteUpload(credentials, message)
    sh('junit-merge -o browserstack_reports.xml -d BrowserstackReports')
    junitAndCoverage "$reports/jacoco/jacocoTestDebugUnitTestReport/jacoco.xml", 'unit', javaSrc
}

void getTestBundle(final String credentials, final def message) {
    if (!fileExists('src/fastlane/app_automate/BrowserstackReports')) {
        sh('cd src/fastlane/app_automate && mkdir BrowserstackReports')
    }

    final def sessions = sh(script: """ \
                            curl -u "$credentials" \
                            -X GET "https://api-cloud.browserstack.com/app-automate/xcuitest/v2/builds/{$message.build_id}" \
                            | jq -r ".devices[].sessions[].id"\
                            """, returnStdout: true).trim().split("\n")
    String reports = ""
    for (session in sessions) {
        sh """ \
            curl -u "$credentials" \
            -X GET "https://api-cloud.browserstack.com/app-automate/xcuitest/v2/builds/$message.build_id/sessions/$session/resultbundle" \
            -o src/fastlane/app_automate/BrowserstackReports/${session}.zip
        """
        sh "cd src/fastlane/app_automate/BrowserstackReports && unzip ${session}.zip && mv result-bundle.xcresult ${session}.xcresult"
        reports += "${session}.xcresult "
    }

    sh "cd src/fastlane/app_automate/BrowserstackReports && xcrun xcresulttool merge ${reports} --output-path browserstack_reports.xcresult"
    deleteUpload(credentials, message)
    // Todo convert browserstack_reports.xcresult to xml for jenkins junit
    //sh('junit-merge -o browserstack_reports.xml -d BrowserstackReports')
    // junitAndCoverage "$reports/jacoco/jacocoTestDebugUnitTestReport/jacoco.xml", 'unit', javaSrc
}

void getReportFile(final String credentials, final def message) {

    if (message.framework.equals('espresso')) {
        getXmlFile(credentials, message)
    } else if (message.framework.equals('xcuitest')) {
        getTestBundle(credentials, message)
    }
}

void deleteUpload(final String credentials, final def message) {
    if (message?.app_id && !message?.app_id.equals('null')) {
        sh """ \
            curl -u "$credentials" \
            -X DELETE "https://api-cloud.browserstack.com/app-automate/${message.framework}/v2/apps/${message.app_id}"
        """
    }
    if (message?.test_suite_id && !message?.test_suite_id.equals('null')) {
        sh """ \
            curl -u "$credentials" \
            -X DELETE "https://api-cloud.browserstack.com/app-automate/${message.framework}/v2/test-suites/${message.test_suite_id}"
        """
    }
}

void getBrowserstackEndMessage(final String credentials, final String status, final def message) {
    echo "Status of browserstack exectuion is $status"
    println "https://app-automate.browserstack.com/dashboard/v2/builds/$message.build_id"
    if (status.equals('passed') || status.equals('failed')) {
        getReportFile(credentials, message)
    }
    if (!status.equals('passed')) {
        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE', message: 'Some tests failed on Browserstack') {
            sh "exit 1"
        }
    }
}
