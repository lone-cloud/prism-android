#!/bin/bash -e

[ $# -ne 1 ] && echo "Usage: $0 {version}" && exit 1

VERSION="$1"

[ -f "$VERSION.aab" ] || curl -o "$VERSION.aab" --follow "https://codeberg.org/Sunup/android/releases/download/$VERSION/sunup.aab"

pass ls | grep keystore/sunup.key >/dev/null
if [ $? -ne 0 ]; then
  echo "Pass keystore/sunup.key not found. Aborting."
  exit 1
fi

PASS=$(pass keystore/sunup.key)
echo "[+] pass copied"

rm -f sunup.apks universal.apks

bundletool build-apks \
  --bundle="$VERSION.aab" \
  --ks=$HOME/.password-store/keystore/sunup.jks \
  --ks-pass="pass:$PASS" \
  --ks-key-alias=sunup \
  --output=sunup.apks

bundletool build-apks \
  --bundle="$VERSION.aab" \
  --ks=$HOME/.password-store/keystore/sunup.jks \
  --ks-pass="pass:$PASS" \
  --ks-key-alias=sunup \
  --mode=universal \
  --output=universal.apks

unzip -o universal.apks
mv universal.apk sunup.apk
rm -f universal.apks "$VERSION.aab"
echo "[+] Done"
