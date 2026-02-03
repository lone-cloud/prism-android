<div align="center">

<img src="app/src/main/prism.webp" alt="Prism Icon" width="120" height="120" />

# Prism

</div>

UnifiedPush provider using [Autopush](https://github.com/mozilla-services/autopush-rs)

Fork of [Sunup](https://codeberg.org/Sunup/android) with manual app creation and notification silencing.

## Features

- **Manual Apps**: Create custom UnifiedPush endpoints for apps that don't natively support it
- **Notification Silencing**: Silence notifications from specific apps while still delivering messages

## Usage

1. Install this application
2. Open it, and grant background usage without restrictions permission
3. Register [your application compatible with UnifiedPush](https://unifiedpush.org/users/apps/) (may be transparently done)
4. Tap the + button to create manual apps for apps that need them
5. Long-press any app to enter selection mode with Info (manual apps only), Silence, and Delete options

## Manual Apps

For apps that don't support UnifiedPush natively (like FMD commands from Home Assistant), you can create manual apps:

1. Tap the + button on the main screen
2. Select the target app that should receive the messages
3. Long-press the created app and tap the Info icon
4. Tap the endpoint card to copy it to clipboard
5. Configure your server to send push messages to the endpoint

## Notification Silencing

Long-press any registered app and tap the notification icon to toggle silent delivery. Silenced apps show a 🔇 indicator and won't display notifications, but messages are still delivered to the app.
