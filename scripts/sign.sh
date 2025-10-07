#!/bin/bash -e

[ $# -ne 1 ] && echo "Usage: $0 {version}" && exit 1

VERSION="$1"

[ -f "$VERSION.aab" ] || curl -o "$VERSION.aab" --follow "https://codeberg.org/Sunup/android/releases/download/$VERSION/sunup.aab"

pass ls | grep sunup.key >/dev/null
if [ $? -ne 0 ]; then
  echo "Pass keystore/sunup.key not found. Aborting."
  exit 1
fi

export KS="$HOME/.password-store/keystore/sunup.jks"
export KS_PASS=$(pass keystore/sunup.key)
export KEY_ALIAS="sunup"
export AAB="$VERSION.aab"

echo "[+] Pass copied"

./gradlew bundletoolBuildApks

unzip -o universal.apks
mv universal.apk sunup.apk
echo "[+] Done"
