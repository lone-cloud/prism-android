# GitHub Copilot Instructions

## Code Style

- Do NOT add obvious comments that just describe what the code does
- Only add comments for complex logic, non-obvious behavior, or important context
- Prefer self-documenting code with clear variable and function names over comments
- Avoid redundant comments like "// Create button" or "// Set text color"
- Do NOT add copyright headers to new files
- After every code change, run `./gradlew ktlintCheck compileDebugKotlin` and fix any errors or warnings before finishing

## Codebase Overview

Prism Android is a **UnifiedPush distributor**. It maintains a persistent WebSocket to a Mozilla Autopush-compatible or self-hosted Prism server, decrypts RFC 8291 WebPush payloads, and routes notifications to registered apps.

### Notification Flow
```
Push server (WebSocket) → ServerConnection.kt
  → Decrypt (RFC 8291 aes128gcm via Tink)
  → Manual app?        → ManualAppNotifications.kt → Android notification + action buttons
  → UnifiedPush app?   → Distributor.sendMessage() → IPC to target app
```

### Key Files

| File | Role |
|------|------|
| `api/ServerConnection.kt` | WebSocket client; decrypts + routes notifications |
| `api/ManualAppRegistrationHandler.kt` | Registers manual apps with Prism server |
| `utils/ManualAppNotifications.kt` | Builds/displays Android notifications; owns action PendingIntent logic |
| `utils/WebPushDecryptor.kt` | RFC 8291 ECDH+HKDF+AES-GCM (Tink) |
| `receivers/NotificationActionReceiver.kt` | Executes HTTP actions (validated to Prism server origin only) |
| `receivers/NotificationDismissReceiver.kt` | Notification dismiss callbacks |
| `PrismPreferences.kt` | All persistent config (server URL, API key, UAID, endpoints) |
| `PrismServerClient.kt` | HTTP client for Prism server (register/delete subscriptions) |
| `EncryptionKeyStore.kt` | Per-channel ECDH keys stored encrypted via AndroidKeyStore |
| `services/FgService.kt` | Foreground service; manages connection lifecycle |
| `services/RestartWorker.kt` | WorkManager reconnection on network changes |

### Notification Actions

`NotificationAction` has `id`, `label`, `endpoint`, `method`, `data`.

`endpoint` is either:
- A relative/absolute HTTP URL (POST/GET to Prism server, same-origin validated)
- A URI scheme: `tel:`, `mailto:`, `sms:`, `geo:`

Intent-scheme actions are handled in `NotificationActionReceiver` — dismiss fires first, then `startActivity(ACTION_VIEW)`. HTTP actions make an authenticated request to the Prism server. Both paths go through `PendingIntent.getBroadcast` → `NotificationActionReceiver`.

The Prism server auto-generates `tel:` and `mailto:` actions from notification body content when no explicit actions are provided.

### Security Notes
- Action HTTP endpoints validated against Prism server origin (`resolveAndValidateActionUrl`)
- API key stored AES-256-GCM in AndroidKeyStore
- WebPush: P-256 ECDH, 16-byte auth secret, stored encrypted per-channel

### Build
- minSdk 31, targetSdk 36, Java 21, Kotlin + Compose
- UI in `:ui` process; foreground service in main process
- `AppScope`: global `SupervisorJob + Dispatchers.IO`
- `HttpClientFactory`: 3 OkHttp instances — `shared`, `action`, `longLived` (WebSocket)
