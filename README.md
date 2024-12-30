# Sunup - Android

UnifiedPush provider using [Autopush](https://github.com/mozilla-services/autopush-rs)

## Usage

1. Install this application
2. Open it, and grant background usage without restrictions permission
3. Register [your application compatible with UnifiedPush](https://unifiedpush.org/users/apps/) (may be transparently done)

## Self-host

It is possible to your own Autopush server. Autopush is designed to work with Google BigTable but it is also possible to use it with redis.

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

## Developpers

It is possible to configure a few things with build config:

| Name | Description | Default |
|------|-------------|---------|
| DEFAULT_API_URL | Define the API Url used by default | `"https://push.services.mozilla.com"` |
| URGENCY | To add support for urgency requirement depending on the battery level | `false` until this is supported by the main server |
