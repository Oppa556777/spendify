package com.myexpense.tracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.myexpense.tracker.data.model.TransactionType

/** Expense / Income / Transfer segmented control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSelector(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    includeTransfer: Boolean = false,
) {
    val options = buildList {
        add(TransactionType.EXPENSE to "Expense")
        add(TransactionType.INCOME to "Income")
        if (includeTransfer) add(TransactionType.TRANSFER to "Transfer")
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = when (type) {
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                },
            )
        }
    }
}
