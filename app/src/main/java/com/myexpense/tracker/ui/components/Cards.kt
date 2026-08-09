package com.myexpense.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.utils.IconMap
import com.myexpense.tracker.ui.theme.AmountFontFamily

/** Round icon bubble with the category's icon + color. */
@Composable
fun CategoryIcon(
    icon: String?,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.18f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(
                imageVector = IconMap.get(icon),
                contentDescription = null,
                modifier = Modifier.size((size * 0.52f).dp),
                tint = color,
            )
        }
    }
}

/** Category pill used in filters and pickers. */
@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = Color(category.color)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.material3.Icon(
                imageVector = IconMap.get(category.icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One transaction row: icon, title/subtitle, amount. */
@Composable
fun TransactionRow(
    title: String,
    subtitle: String,
    icon: String?,
    iconColor: Color,
    amount: Long,
    symbol: String,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(icon = icon, color = iconColor, size = 42)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
        val amountColor = if (isExpense) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.primary
        }
        Text(
            text = (if (isExpense) "-" else "+") + symbol + com.myexpense.tracker.utils.MoneyFormatter.format(amount),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = amountColor,
            ),
        )
    }
}

/** Summary card with a label + big number + optional icon. */
@Composable
fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    background: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = background,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = iconTint,
                    )
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = valueColor,
                ),
                maxLines = 1,
            )
        }
    }
}

/** Simple full-width gradient hero used on the home screen. */
@Composable
fun HeroBalanceCard(
    title: String,
    balanceText: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    gradient: List<Color>,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(gradient),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = balanceText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = Color.White,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

/** Small stat chip: label above value. */
@Composable
fun StatChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = valueColor,
            ),
        )
    }
}
