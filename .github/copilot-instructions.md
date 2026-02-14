# GitHub Copilot Instructions

## Code Style

- Do NOT add obvious comments that just describe what the code does
- Only add comments for complex logic, non-obvious behavior, or important context
- Prefer self-documenting code with clear variable and function names over comments
- Avoid redundant comments like "// Create button" or "// Set text color"

## Examples of BAD comments to avoid:
```kotlin
// Description with clickable link
val description = stringResource(R.string.prism_server_description)

// Get the URI handler
val uriHandler = LocalUriHandler.current
```

## Examples of GOOD comments:
```kotlin
// Retry with exponential backoff, max 5 attempts
for (attempt in 1..5) { ... }

// WorkAround: Android 12+ requires explicit mutation flag
val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE)
```
