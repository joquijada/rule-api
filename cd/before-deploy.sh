#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_7092547da8b3_key -iv $encrypted_7092547da8b3_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
    gpg --import --batch cd/codesigning.asc
fi
