# Sunup - Android

UnifiedPush provider using [Autopush](https://github.com/mozilla-services/autopush-rs)

[<img alt="Get it on F-Droid" src="https://unifiedpush.org/img/f-droid-badge.png" height=100>](https://f-droid.org/en/packages/org.unifiedpush.distributor.sunup/)
[<img alt="Get it on IzzyOnDroid" src="https://unifiedpush.org/img/IzzyOnDroid-badge.png" height=100 >](https://apt.izzysoft.de/fdroid/index/apk/org.unifiedpush.distributor.sunup)
[<img alt="Get it on Codeberg" src="https://unifiedpush.org/img/codeberg-badge.png" height=100>](https://codeberg.org/Sunup/android/releases)

## Usage

1. Install this application
2. Open it, and grant background usage without restrictions permission
3. Register [your application compatible with UnifiedPush](https://unifiedpush.org/users/apps/) (may be transparently done)

## Self-host

It is possible to host your own Autopush server. Autopush is designed to work with Google BigTable but it is also possible to use it with redis.

For this:
1. Clone Autopush ([this fork](https://github.com/p1gp1g/autopush-rs) until redis support has been merged).
2. Generate Fernet keys:
    1. Set a python virtual env, for instance: `venv .venv && . .venv/bin/activate`
    2. Install cryptography `python -m pip install cryptography`
    3. Run the generation script: `python scripts/fernet_key.py`
3. Replace the following values in `redis-docker-compose.yml`:
    - `AUTOCONNECT__CRYPTO_KEY`
    - `AUTOCONNECT__ENDPOINT_SCHEME`
    - `AUTOCONNECT__ENDPOINT_HOSTNAME`
    - `AUTOCONNECT__ENDPOINT_PORT`
    - `AUTOEND__CRYPTO_KEYS`
    - `AUTOEND__ENDPOINT_URL`
4. Setup a reverse proxy to add TLS support, for instance with caddy:
```
# Autoconnect endpoint
push.domain.tld {
  reverse_proxy 127.0.0.1:8080
}
# Autoend endpoint
updates.push.domain.tld {
  reverse_proxy 127.0.0.1:8000
}
```
5. In the android app, change server to the autoconnect endpoint (here `push.domain.tld`).

[Autopush]: https://github.com/mozilla-services/autopush-rs

## Signing certificate hash

The package name along with the SHA-256 hash can be found below.

To verify the APK use the [AppVerifier](https://github.com/soupslurpr/AppVerifier) Android application or the [`apksigner`](https://developer.android.com/studio/command-line/apksigner#usage-verify) tool. 


```
org.unifiedpush.distributor.sunup
3B:33:D7:8A:5B:CA:C1:B9:52:75:6B:08:FE:88:30:CE:D3:87:AB:B6:B9:56:B0:2A:47:EF:80:32:1D:4A:2B:88
```

## Developers

It is possible to configure a few things with build config:

| Name | Description | Default |
|------|-------------|---------|
| DEFAULT_API_URL | Define the API Url used by default | `"https://push.services.mozilla.com"` |
| URGENCY | To add support for urgency requirement depending on the battery level | `false` until this is supported by the main server |
