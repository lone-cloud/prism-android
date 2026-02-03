#!/bin/bash -e

[ $# -ne 1 ] || [ ! -f $1 ] && echo "Usage: $0 {version.aab}" && exit 1

passage ls | grep prism.key >/dev/null
if [ $? -ne 0 ]; then
  echo "Pass keystore/prism.key not found. Aborting."
  exit 1
fi

export KS="$HOME/.passage/store/keystore/prism.jks"
export KS_PASS=$(passage keystore/prism.key)
export KEY_ALIAS="prism"
export AAB="$1"

echo "[+] Pass copied"

./gradlew bundletoolBuildApks

unzip -o universal.apks
mv universal.apk prism.apk
mv app.apks prism.apks
echo "[+] Done"
