#!/usr/bin/env bash

echo "Setting uid/gid..."
[ -n "${MYUID}" ] && usermod -u "${MYUID}" "${USER}"
[ -n "${MYGID}" ] && groupmod -g "${MYGID}" "${USER}"

chown "${USER}:${USER}" "/home/${USER}/.gradle"
chmod 777 /usr/local/cargo/registry

exec gosu "${USER}" ./gradlew :android:buildAndroidLib
