#!/usr/bin/env groovy

/**
 * Renames all found APKs by adding the suffix.
 *
 * @param suffix The suffix to use for renaming.
 *        Is sanitzized automatically by replacing unsupported characters with '_'
 */
def call(def suffix) {
    suffix = suffix.replaceAll(/[\\:#\/]/, '_')
    def apkFiles = sh script: 'find -name "*.apk"', returnStdout: true
    apkFiles.trim().split('\n').each { oldPath ->
        def newPath = oldPath.replaceAll(/\.apk$/, "-${suffix}.apk")
        sh "mv '$oldPath' '$newPath'"
    }
}
