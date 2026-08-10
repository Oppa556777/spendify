package com.myexpense.tracker.ui.screens.categories

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.TypeSelector
import com.myexpense.tracker.ui.screens.accounts.Palette
import com.myexpense.tracker.ui.viewmodel.CategoriesViewModel
import com.myexpense.tracker.utils.IconMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    var editing by remember { mutableStateOf<Category?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TypeSelector(
                selected = state.selectedType,
                onSelect = viewModel::setType,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            state.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (state.categories.isEmpty()) {
                EmptyState(
                    title = "No ${state.selectedType.name.lowercase()} categories",
                    subtitle = "Add categories to organise your ${state.selectedType.name.lowercase()}s.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    editing = category
                                    showDialog = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CategoryIcon(icon = category.icon, color = Color(category.color), size = 40)
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (category.isDefault) {
                                    Text(
                                        "Default",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.delete(category) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            category = editing,
            onDismiss = { showDialog = false },
            onSave = { category ->
                viewModel.save(category)
                showDialog = false
            },
        )
    }
}

@Composable
private fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
) {
    val type = category?.type ?: TransactionType.EXPENSE
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "category") }
    var color by remember { mutableStateOf(category?.color ?: Palette.colors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(240.dp),
                ) {
                    items(IconMap.all.entries.toList()) { (key, vector) ->
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { icon = key },
                            shape = RoundedCornerShape(10.dp),
                            color = if (icon == key) {
                                Color(color).copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = if (icon == key) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.colors.forEach { c ->
                        Surface(
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { color = c },
                            shape = RoundedCornerShape(50),
                            color = Color(c),
                        ) {
                            if (color == c) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(5.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        Category(
                            id = category?.id ?: 0,
                            name = name.trim(),
                            type = type,
                            icon = icon,
                            color = color,
                            parentId = category?.parentId,
                            isDefault = category?.isDefault ?: false,
                            createdAt = category?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
