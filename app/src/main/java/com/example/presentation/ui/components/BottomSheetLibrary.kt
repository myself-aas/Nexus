package com.example.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBottomSheet

/**
 * Generic reusable bottom sheet for selection dialogs.
 * Supports search, categorization, and loading states.
 */
@Composable
fun <T> SelectionBottomSheet(
    items: List<T>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    itemIcon: ((T) -> ImageVector)? = null,
    isItemSelected: (T) -> Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    searchEnabled: Boolean = false,
    searchFilter: (String, List<T>) -> List<T> = { _, items -> items },
    isLoading: Boolean = false,
    emptyStateMessage: String = "No items available"
) {
    var searchQuery by remember { mutableStateOf("") }

    if (!isVisible) return

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            testTag = "selection_bottom_sheet"
        ) {
            // Header
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Search bar
            if (searchEnabled) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Items list
            val filteredItems = if (searchEnabled) {
                searchFilter(searchQuery, items)
            } else {
                items
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(emptyStateMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredItems) { item ->
                        SelectionBottomSheetItem(
                            label = itemLabel(item),
                            icon = itemIcon?.invoke(item),
                            isSelected = isItemSelected(item),
                            onClick = {
                                onItemSelected(item)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual item in selection bottom sheet
 */
@Composable
private fun SelectionBottomSheetItem(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Confirmation/Action bottom sheet with primary and secondary actions
 */
@Composable
fun ActionBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String? = null,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
    onConfirm: () -> Unit,
    isDangerous: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Message
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dismissButtonText)
                }

                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(confirmButtonText)
                }
            }
        }
    }
}

/**
 * Information/Details bottom sheet with close button
 */
@Composable
fun InfoBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!isVisible) return

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { content() }
            }
        }
    }
}

/**
 * Filter/Options bottom sheet for multi-select filtering
 */
@Composable
fun <T> FilterBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    filterOptions: List<Pair<T, String>>,
    selectedFilters: Set<T>,
    onFiltersChanged: (Set<T>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var localFilters by remember { mutableStateOf(selectedFilters) }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Filter options as checkboxes
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filterOptions) { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                localFilters = if (localFilters.contains(value)) {
                                    localFilters - value
                                } else {
                                    localFilters + value
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = localFilters.contains(value),
                            onCheckedChange = {
                                localFilters = if (it) {
                                    localFilters + value
                                } else {
                                    localFilters - value
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Apply button
            Button(
                onClick = {
                    onFiltersChanged(localFilters)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
        }
    }
}
