package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.unifiedpush.android.distributor.ui.R
import org.unifiedpush.android.distributor.ui.state.RegistrationState
import org.unifiedpush.android.distributor.ui.vm.RegistrationsViewModel

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.registrationList(viewModel: RegistrationsViewModel, onOpenDetails: (token: String) -> Unit) {
    val items = viewModel.state.list.sortedBy { it.app.title.lowercase() }

    itemsIndexed(items) { index, item ->
        val haptics = LocalHapticFeedback.current
        val itemShape = listShape(items.size, index)

        Surface(
            shape = itemShape,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(itemShape)
                .combinedClickable(
                    onClick = {
                        if (viewModel.state.selectionCount > 0) {
                            viewModel.toggleSelection(item.token)
                        } else {
                            onOpenDetails(item.token)
                        }
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSelection(item.token)
                    },
                    onLongClickLabel = stringResource(
                        R.string.list_registrations_alt_long_click_label
                    )
                ),
            color = if (item.selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ) {
            RegistrationRow(item)
        }
    }
}

@Composable
private fun RegistrationRow(item: RegistrationState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            item.app.icon?.let { icon ->
                androidx.compose.foundation.Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } ?: androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_android_24dp),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.app.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (item.selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        NotificationCount(item.msgCount)
    }
}

@Composable
private fun NotificationCount(count: Int) {
    Row {
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Filled.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

private fun listShape(size: Int, index: Int): RoundedCornerShape {
    val small = 4.dp
    val big = 16.dp
    if (size <= 1) return RoundedCornerShape(big)

    return when (index) {
        0 -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
        size - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
        else -> RoundedCornerShape(small)
    }
}
