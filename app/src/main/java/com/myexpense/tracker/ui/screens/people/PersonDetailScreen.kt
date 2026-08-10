package com.myexpense.tracker.ui.screens.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.PersonDetailViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: PersonDetailViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    val person = state.person
    val categoryMap = state.categories.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(person?.name ?: "Person", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
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
            // Header card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(person?.avatarColor ?: 0xFF3B82F6)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (person?.name ?: "?").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                        ) {
                            Text(
                                text = person?.name ?: "Unknown",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = NunitoFamily,
                                fontWeight = FontWeight.Bold,
                            )
                            person?.phone?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = "${if (state.net >= 0) "+" else "-"}$symbol${MoneyFormatter.format(kotlin.math.abs(state.net))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = AmountFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (state.net >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Transactions (${state.transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No transactions",
                        subtitle = "Transactions involving this person will appear here.",
                    )
                }
            } else {
                items(state.transactions, key = { it.id }) { t ->
                    val category = t.categoryId?.let { categoryMap[it] }
                    TransactionRow(
                        title = t.title.ifBlank { category?.name ?: if (t.isExpense) "Expense" else "Income" },
                        subtitle = DateUtils.shortDate(t.date),
                        icon = category?.icon,
                        iconColor = Color(category?.color ?: if (t.isExpense) 0xFFE53935 else 0xFF43A047),
                        amount = t.amount,
                        symbol = symbol,
                        isExpense = t.isExpense,
                        onClick = { onEditTransaction(t.id) },
                    )
                }
            }
        }
    }
}
