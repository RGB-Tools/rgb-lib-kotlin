#!/usr/bin/env bash

echo "Setting uid/gid..."
[ -n "${MYUID}" ] && usermod -u "${MYUID}" "${USER}"
[ -n "${MYGID}" ] && groupmod -g "${MYGID}" "${USER}"

chown "${USER}:${USER}" "/home/${USER}/.gradle"
chmod 777 /usr/local/cargo/registry

# run the gradle tasks given as arguments, defaulting to building the library
exec gosu "${USER}" ./gradlew "${@:-:android:buildAndroidLib}"
