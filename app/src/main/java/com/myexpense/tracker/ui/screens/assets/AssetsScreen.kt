package com.myexpense.tracker.ui.screens.assets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.AssetType
import com.myexpense.tracker.data.repository.AssetView
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.AssetsViewModel
import com.myexpense.tracker.utils.MoneyFormatter

private val assetTypeEmoji = mapOf(
    AssetType.STOCK to "📈",
    AssetType.MUTUAL_FUND to "💼",
    AssetType.CRYPTO to "🪙",
    AssetType.REAL_ESTATE to "🏠",
    AssetType.GOLD to "🥇",
    AssetType.FD to "🏦",
    AssetType.OTHER to "💎",
)

private val assetTypeColors = listOf(
    0xFF6C63FF, 0xFF3B82F6, 0xFFF59E0B, 0xFF10B981, 0xFFF59E0B,
    0xFF8B5CF6, 0xFF64748B,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    onBack: () -> Unit,
    viewModel: AssetsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AssetView?>(null) }
    var pendingDelete by remember { mutableStateOf<AssetView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assets", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showSheet = true
                },
                containerColor = Color(0xFF6C63FF),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add asset", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Portfolio summary ───────────────────────────────────────────
            if (state.assets.isNotEmpty()) {
                item {
                    PortfolioSummaryCard(
                        invested = state.summary.invested,
                        value = state.summary.value,
                        gain = state.summary.gain,
                        gainPercent = state.summary.gainPercent,
                        symbol = symbol,
                    )
                }
                item {
                    PortfolioDonut(
                        assets = state.assets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                }
            }

            if (state.assets.isEmpty()) {
                item {
                    EmptyState(
                        title = "No assets yet",
                        subtitle = "Track stocks, crypto, gold, property and more.",
                    )
                }
            } else {
                items(state.assets, key = { it.id }) { asset ->
                    AssetRow(
                        asset = asset,
                        symbol = symbol,
                        onClick = {
                            editing = asset
                            showSheet = true
                        },
                        onDelete = { pendingDelete = asset },
                    )
                }
            }
        }
    }

    pendingDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete asset?") },
            text = { Text("Delete \"${asset.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(asset.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showSheet) {
        AddEditAssetSheet(
            asset = editing,
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { id, name, type, qty, buy, cur, note ->
                viewModel.save(id, name, type, qty, buy, cur, note)
                showSheet = false
            },
        )
    }
}

@Composable
private fun PortfolioSummaryCard(invested: Long, value: Long, gain: Long, gainPercent: Double, symbol: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Total Invested", "$symbol${MoneyFormatter.format(invested)}", MaterialTheme.colorScheme.onSurface)
                SummaryStat("Current Value", "$symbol${MoneyFormatter.format(value)}", Color(0xFF3B82F6))
            }
            Spacer(Modifier.height(10.dp))
            val gainColor = if (gain >= 0) Color(0xFF43A047) else Color(0xFFE53935)
            Text(
                text = "Gain/Loss: $symbol${MoneyFormatter.format(gain)}  (${String.format("%.1f", gainPercent)}%)",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
                color = gainColor,
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun PortfolioDonut(assets: List<AssetView>, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(assets) { anim.snapTo(0f); anim.animateTo(1f, tween(800)) }
    val total = assets.sumOf { it.value }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (total <= 0) return@Canvas
            val stroke = size.minDimension * 0.16f
            val inset = stroke / 2
            var start = -90f
            assets.groupBy { it.type }.forEachIndexed { index, (_, list) ->
                val value = list.sumOf { it.value }
                val sweep = value.toFloat() / total * 360f * anim.value
                drawArc(
                    color = Color(assetTypeColors[index % assetTypeColors.size]),
                    startAngle = start,
                    sweepAngle = sweep.coerceAtLeast(0.4f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += value.toFloat() / total * 360f
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$symbol${MoneyFormatter.format(total)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Portfolio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssetRow(
    asset: AssetView,
    symbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val gainColor = if (asset.gain >= 0) Color(0xFF43A047) else Color(0xFFE53935)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(assetTypeEmoji[asset.type] ?: "💎", fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    asset.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${asset.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${asset.quantity} @ $symbol${MoneyFormatter.format(asset.buyPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Invested $symbol${MoneyFormatter.format(asset.invested)} → Value $symbol${MoneyFormatter.format(asset.value)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (asset.gain >= 0) "+" else ""}$symbol${MoneyFormatter.format(asset.gain)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = gainColor,
                )
                Text(
                    text = "${if (asset.gain >= 0) "+" else ""}${String.format("%.1f", asset.gainPercent)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = gainColor,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADD / EDIT ASSET SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditAssetSheet(
    asset: AssetView?,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, type: AssetType, qty: Double, buy: Long, cur: Long, note: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var type by remember { mutableStateOf(asset?.type ?: AssetType.STOCK) }
    var qty by remember { mutableStateOf(asset?.quantity?.toString() ?: "") }
    var buy by remember { mutableStateOf(asset?.let { MoneyFormatter.format(it.buyPrice) } ?: "") }
    var cur by remember { mutableStateOf(asset?.let { MoneyFormatter.format(it.currentPrice) } ?: "") }
    var note by remember { mutableStateOf(asset?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (asset == null) "Add Asset" else "Edit Asset",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Asset name") },
                placeholder = { Text("e.g. AAPL, Bitcoin, Gold") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssetType.entries.forEach { t ->
                    val selected = type == t
                    Surface(
                        modifier = Modifier.clickable { type = t },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            "${assetTypeEmoji[t]} ${t.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Quantity") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = buy,
                onValueChange = { buy = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Buy price (per unit)") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cur,
                onValueChange = { cur = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Current price (per unit)") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    if (name.isBlank()) { error = "Enter a name"; return@Button }
                    val qtyValue = qty.toDoubleOrNull() ?: 0.0
                    if (qtyValue <= 0) { error = "Enter a quantity"; return@Button }
                    val buyMinor = ((buy.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val curMinor = ((cur.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onSave(asset?.id ?: 0L, name.trim(), type, qtyValue, buyMinor, curMinor, note.trim().ifBlank { null })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Asset", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}
