#!/usr/bin/env groovy

/**
 * Run on all Android SDK slaves
 */
def call(Closure body) {
	parallel (
	slave2: {
		node('Slave2_emulator') {
			body()
		}
	},
	slave3: {
		node('Slave3_emulator') {
			body()
		}
	}
	)
}
