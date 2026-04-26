# GitHub Copilot Instructions

## Code Style

- Do NOT add obvious comments that just describe what the code does
- Only add comments for complex logic, non-obvious behavior, or important context
- Prefer self-documenting code with clear variable and function names over comments
- Avoid redundant comments like "// Create button" or "// Set text color"
- Do NOT add copyright headers to new files

## Validation

After every code change, run:

```sh
./gradlew ktlintFormat compileDebugKotlin
```

Fix all errors before finishing. `ktlintFormat` auto-fixes formatting; if it reformats files, include those changes. For a full lint pass: `./gradlew ktlintCheck detekt`.

## What Prism Android Is

Prism Android is a **UnifiedPush distributor**. It maintains a persistent WebSocket to a Mozilla Autopush-compatible or self-hosted Prism server, decrypts RFC 8291 WebPush payloads, and routes push notifications to registered apps.

Two app types are supported:
- **Standard UP apps**: receive encrypted payloads forwarded via UnifiedPush IPC (`Distributor.sendMessage`)
- **Manual apps**: registered directly with the Prism server; Prism Android decrypts + displays Android notifications with rich actions

## Notification Flow

```
Push server (WebSocket)
  └─ ServerConnection.onMessage()
       └─ deserialize ServerMessage
            ├─ Notification + manual app? → decrypt (WebPushDecryptor) → ManualAppNotifications.showNotification()
            └─ Notification + UP app?     → Distributor.sendMessage() → IPC to target app
```

## Architecture

### Two-Process Split

- `:ui` process: `MainActivity` + all Compose UI
- main process: `FgService`, `ServerConnection`, `PrismServerClient`, all receivers

IPC between processes uses the UP library: `InternalMessenger` (UI side) ↔ `PrismInternalService` (main side). UI sends config changes via `PrismConfigReceiver` broadcasts; main process sends UI updates via `sendUiAction` / `subscribeUiActions`.

### WebSocket Lifecycle

```
FgService.sync()
  → ServerConnection.start()           # builds OkHttp WS request
  → MessageSender.newWs(ws)            # registers WS for outbound sends
  → SourceManager.setConnected()       # tracks state
  → RestartWorker (WorkManager)        # periodic reconnect + network-change reconnect
```

`ServerConnection.onHello()` persists the tested push URL via `ApiUrlCandidate.finish()`.

### Manual App Registration

1. UI calls `MainViewModel.addManualApp(name, targetApp?)`
2. Generates VAPID keypair (`VapidKeyGenerator`) + WebPush encryption keys (`WebPushEncryptionKeys`)
3. Stores in UP library DB with pipe-delimited description: `target:<pkg>|vp:<vapidPrivKey>|sid:<subId>`
4. Sends `ClientMessage.Register(channelID, vapidPublicKey)` to push server
5. Server responds → `ServerConnection.onRegister()` → `ManualAppRegistrationHandler.handleRegister()`
6. Handler calls `PrismServerClient.registerApp()` to register with Prism server HTTP API

### Description Field Encoding (`DescriptionParser`)

The UP library `Store.description` field encodes manual app metadata as pipe-delimited key:value pairs:

```
target:<packageName>|vp:<base64UrlVapidPrivKey>|sid:<subscriptionId>
```

- `isManualApp(description)` checks `startsWith("target:")`
- `extractValue(description, "vp:")` → VAPID private key
- `extractValue(description, "sid:")` → Prism server subscription ID

### Security

| Layer | Mechanism |
|---|---|
| API key storage | AES-256-GCM, AndroidKeyStore-backed (`SecureStringPreferences`) |
| Per-channel WebPush keys | Same — `EncryptionKeyStore` with key alias `"prism_webpush_encryption_keys"` |
| WebPush decryption | RFC 8291 `aes128gcm` — Tink ECDH + HKDF + JCE AES-GCM (`WebPushDecryptor`) |
| Notification action URLs | Same-origin validated against Prism server URL (`resolveAndValidateActionUrl`) |

Never relax the same-origin check. Never store sensitive values in plain SharedPreferences.

## Source Layout

All Kotlin under `app/src/main/java/app/lonecloud/prism/`:

```
PrismApplication.kt         # Application; manual WorkManager init
AppScope.kt                 # Global SupervisorJob + Dispatchers.IO coroutine scope
PrismPreferences.kt         # All persistent config + per-channel token storage
SecureStringPreferences.kt  # AES-256-GCM encrypted SharedPreferences (AndroidKeyStore)
PrismServerClient.kt        # HTTP client: register/delete/test subscriptions
EncryptionKeyStore.kt       # Per-channel ECDH key storage (via SecureStringPreferences)
DatabaseFactory.kt          # UP library DB singleton
PrismConfig.kt              # UP library AppConfig (restartable, no login, migration)
Distributor.kt              # UnifiedPushDistributor: register/unregister channels
MigrationManager.kt         # UP migration support

api/
  ServerConnection.kt           # WebSocket client + message dispatch
  ApiUrlCandidate.kt            # State machine for testing a new push server URL
  ManualAppRegistrationHandler.kt  # Handles Register response → PrismServerClient.registerApp
  MessageSender.kt              # Thread-safe WS send + ping rate limiter
  data/
    ClientMessage.kt            # Outbound WS messages (sealed class, @Serializable)
    ServerMessage.kt            # Inbound WS messages (sealed class, @Serializable)
    NotificationPayload.kt      # Parsed notification body + NotificationAction list

services/
  FgService.kt                  # ForegroundService: owns WS lifecycle
  RestartWorker.kt              # WorkManager Worker: reconnect on network changes
  SourceManager.kt              # SManager<WebSocket>: tracks connection state
  MainRegistrationCounter.kt    # Tracks registration count; refreshes foreground notification
  PrismInternalService.kt       # UP InternalService: IPC from UI process

receivers/
  RegisterBroadcastReceiver.kt  # UP DistributorReceiver: handles REGISTER/UNREGISTER
  StartReceiver.kt              # BOOT_COMPLETED + MY_PACKAGE_REPLACED → start periodic worker
  PrismConfigReceiver.kt        # Internal IPC: SET_PRISM_SERVER_URL, SET_PRISM_API_KEY, SET_PUSH_SERVICE_URL
  NotificationActionReceiver.kt # Executes notification actions (HTTP or URI scheme)
  NotificationDismissReceiver.kt  # Notification dismiss callback

utils/
  HttpClientFactory.kt          # 4 OkHttpClient instances: shared, image, action, longLived
  WebPushDecryptor.kt           # RFC 8291 aes128gcm decryption
  WebPushEncryptionKeys.kt      # P-256 key generation for WebPush subscriptions
  VapidKeyGenerator.kt          # P-256 VAPID keypair generation
  ManualAppNotifications.kt     # Build + show Android notifications; PendingIntent wiring
  DescriptionParser.kt          # Pipe-delimited description field encode/decode
  LogRedaction.kt               # redactIdentifier / redactUrl for safe logging
  ServerUtils.kt                # normalizeUrl / isValidUrl (OkHttp-backed)
  UiActions.kt                  # IPC action string constants
  Notifications.kt              # ForegroundNotification + DisconnectedNotification
  Tag.kt                        # Any.TAG extension (23-char Android log tag)

callback/
  NetworkCallbackFactory.kt     # ConnectivityManager callbacks → reconnect trigger
  BatteryCallbackFactory.kt     # Battery callbacks (no-ops; URGENCY unsupported)

activities/
  MainActivity.kt               # ComponentActivity; creates InternalMessenger
  ViewModelFactory.kt           # Creates all ViewModels; handles battery opt check
  MainViewModel.kt              # Registration list state; add/delete manual apps
  SettingsViewModel.kt          # Settings state; Prism server config + test connection
  ThemeViewModel.kt             # Dynamic color toggle
  ui/
    AppScreen.kt                # NavHost; AppScreen enum; slide animations
    MainScreen.kt               # Registration list; battery opt card
    AddAppScreen.kt             # Manual app name + target picker
    AppPickerScreen.kt          # Installed apps + Prism server recommended apps
    SettingsScreen.kt           # Settings links + toggles
    ServerConfigScreen.kt       # Prism server URL + API key
    PushServiceConfigScreen.kt  # Push server URL
    RegistrationDetailsScreen.kt  # Details + delete for single registration
    IntroScreen.kt              # First-launch intro
    RegistrationList.kt         # LazyColumn items
    MainUiState.kt              # Data classes: MainUiState, InstalledApp
    SettingsState.kt            # SettingsState data class
    components/
      PasswordTextField.kt      # Password input with show/hide toggle
      UrlTextField.kt           # URL input with inline validation (isValidUrl)
    theme/
      Color.kt, Theme.kt, Type.kt  # AppTheme composable + dynamic color support
```

## Key Files Reference

| File | Role |
|------|------|
| `api/ServerConnection.kt` | WebSocket client; decrypts + routes all incoming notifications |
| `api/ManualAppRegistrationHandler.kt` | Maps Register response → PrismServerClient.registerApp |
| `utils/ManualAppNotifications.kt` | Builds + shows Android notifications; owns all PendingIntent wiring |
| `utils/WebPushDecryptor.kt` | RFC 8291 ECDH+HKDF+AES-GCM via Tink |
| `receivers/NotificationActionReceiver.kt` | Executes notification actions; enforces same-origin on HTTP |
| `PrismPreferences.kt` | All persistent config (server URL, API key, UAID, per-channel tokens) |
| `PrismServerClient.kt` | HTTP client for Prism server REST API |
| `EncryptionKeyStore.kt` | Per-channel ECDH keys encrypted via AndroidKeyStore |
| `services/FgService.kt` | Foreground service; manages WebSocket lifecycle |
| `services/RestartWorker.kt` | WorkManager reconnection on network changes |

## Notification Actions

`NotificationAction` has `id`, `label`, `endpoint`, `method`, `data: Map<String, Any>`.

`endpoint` is either:
- An HTTP/HTTPS URL (relative or absolute) → validated same-origin vs Prism server, then `HttpClientFactory.action` POST/GET
- A URI scheme (`tel:`, `mailto:`, `sms:`, `geo:`) → `startActivity(ACTION_VIEW)`

Both paths go through `PendingIntent.getBroadcast` → `NotificationActionReceiver`. The dismiss fires before the action executes. `data` entries are stored as `data_<key>` extras.

## Coroutines and Threading

- `AppScope` (`SupervisorJob + Dispatchers.IO`): all background work in `PrismServerClient`, `ManualAppNotifications`, and fire-and-forget service operations. App-lifetime scope; not tied to any ViewModel.
- `viewModelScope`: used in ViewModels for UI-tied coroutines
- `BroadcastReceiver.goAsync()` + `AppScope.launch`: used in `NotificationActionReceiver` and `NotificationDismissReceiver` to do async work from receivers
- `debugLog { }` pattern: inline lambda — string construction deferred until debug check passes

## OkHttp Instances (`HttpClientFactory`)

| Instance | Timeouts | Use |
|---|---|---|
| `shared` | 10s connect/read | General API calls (PrismServerClient) |
| `image` | 10s, max 2/host | Notification image fetch |
| `action` | 8s call timeout | Notification action HTTP execution |
| `longLived` | 0ms read, 1-min ping | WebSocket |

`Request.Builder().url(url)` throws `IllegalArgumentException` for malformed URLs (not `IOException`). Always catch both when building requests.

## Build

- minSdk 31, targetSdk/compileSdk 36, Java 21, Kotlin 2.3.21, AGP 9.2.0
- Compose + Material3 + Navigation Compose
- `BuildConfig.DEFAULT_API_URL` = `"https://push.services.mozilla.com"`
- Version catalog at `gradle/libs.versions.toml`
- `kotlin-gradle-plugin` in `[libraries]` is a **build classpath dep** used in root `build.gradle.kts` — not an app runtime dep
- Release signing via env vars; minify + shrinkResources enabled
