package com.myexpense.tracker.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myexpense.tracker.ui.theme.NunitoFamily

private data class MoreItem(
    val emoji: String,
    val title: String,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun MoreScreen(
    onAchievements: () -> Unit,
    onSplitBill: () -> Unit,
    onAssets: () -> Unit,
    onPeople: () -> Unit,
    onTags: () -> Unit,
    onRecurring: () -> Unit,
    onReports: () -> Unit,
    onCalendar: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onCurrencies: () -> Unit,
    onCategories: () -> Unit,
    onAccounts: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearAll: () -> Unit,
    onAbout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }

        item { MoreSectionTitle("TOOLS") }
        item {
            MoreItems(
                listOf(
                    MoreItem("🏆", "Achievements", onAchievements),
                    MoreItem("🤝", "Bill Splitter", onSplitBill),
                    MoreItem("📈", "Asset Tracker", onAssets),
                    MoreItem("👥", "People", onPeople),
                    MoreItem("🏷️", "Tags Manager", onTags),
                    MoreItem("🔄", "Recurring Transactions", onRecurring),
                )
            )
        }

        item { MoreSectionTitle("REPORTS") }
        item {
            MoreItems(
                listOf(
                    MoreItem("📊", "Detailed Reports", onReports),
                    MoreItem("📅", "Calendar View", onCalendar),
                    MoreItem("📤", "Export Data (PDF/CSV)", onExport),
                )
            )
        }

        item { MoreSectionTitle("SETTINGS") }
        item {
            MoreItems(
                listOf(
                    MoreItem("⚙️", "App Settings", onSettings),
                    MoreItem("💰", "Currencies", onCurrencies),
                    MoreItem("📂", "Categories", onCategories),
                    MoreItem("👛", "Accounts", onAccounts),
                )
            )
        }

        item { MoreSectionTitle("DATA") }
        item {
            MoreItems(
                listOf(
                    MoreItem("💾", "Backup Data (local ZIP file)", onBackup),
                    MoreItem("📥", "Restore Data (from ZIP file)", onRestore),
                    MoreItem("🗑️", "Clear All Data", onClearAll),
                )
            )
        }

        item { MoreSectionTitle("ABOUT") }
        item {
            MoreItems(
                listOf(
                    MoreItem("ℹ️", "About App", onAbout),
                )
            )
        }

        item {
            Text(
                text = "Version: 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp),
    )
}

@Composable
private fun MoreItems(items: List<MoreItem>) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        items.forEach { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = item.onClick != null) { item.onClick?.invoke() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(item.emoji, fontSize = 20.sp)
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NunitoFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.onClick != null) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
