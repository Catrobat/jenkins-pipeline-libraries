#!/usr/bin/env groovy

def call(def password)
{
    echo "Unlocking MAC Keychain"
    sh '''#!/bin/bash
        if [[ "$OSTYPE" == "darwin"* ]]; then
            security list-keychains
            security show-keychain-info ${HOME}/Library/Keychains/login.keychain-db
            echo "Try unlocking keychain..."
            set +x
            security unlock-keychain -p $PASSWORD ${HOME}/Library/Keychains/login.keychain-db
            security set-keychain-settings -l ${HOME}/Library/Keychains/login.keychain-db  
            set -x
            security show-keychain-info ${HOME}/Library/Keychains/login.keychain-db
        else
            echo "Not running on MAC (OSTYPE is not darwin but $OSTYPE)"  
        fi                              
    '''
}
