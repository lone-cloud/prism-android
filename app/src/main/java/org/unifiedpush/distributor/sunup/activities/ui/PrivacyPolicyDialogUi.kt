package org.unifiedpush.distributor.sunup.activities.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.distributor.sunup.R

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        title = {
            Text(stringResource(LibR.string.privacy_policy))
        },
        text = {
            PrivacyPolicy()
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }

    )
}

@Preview
@Composable
private fun PrivacyPolicy() {
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.sunup_privacy_policy).format(stringResource(R.string.app_name)))

            withLink(
                LinkAnnotation.Url(
                    "https://www.mozilla.org/en-US/privacy/firefox/#types-of-data-defined",
                    TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                )
            ) {
                append("https://www.mozilla.org/en-US/privacy/firefox/")
            }
        }
    )
}
