#!/bin/bash -e

[ $# -ne 1 ] || [ ! -f $1 ] && echo "Usage: $0 {version.aab}" && exit 1

pass ls | grep sunup.key >/dev/null
if [ $? -ne 0 ]; then
  echo "Pass keystore/sunup.key not found. Aborting."
  exit 1
fi

export KS="$HOME/.password-store/keystore/sunup.jks"
export KS_PASS=$(pass keystore/sunup.key)
export KEY_ALIAS="sunup"
export AAB="$1"

echo "[+] Pass copied"

./gradlew bundletoolBuildApks

unzip -o universal.apks
mv universal.apk sunup.apk
mv app.apks sunup.apks
echo "[+] Done"
