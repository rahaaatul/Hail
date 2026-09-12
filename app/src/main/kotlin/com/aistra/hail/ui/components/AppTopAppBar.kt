package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R

/**
 * Material 3 Expressive TopAppBar.
 * Replaces the MaterialToolbar + AppBarLayout from app_bar_main.xml.
 *
 * Supports both title-only and search modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(text = title)
                subtitle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {},
        scrollBehavior = scrollBehavior,
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
}

/**
 * TopAppBar with integrated search bar.
 * Replaces the SearchView from menu_home.xml / menu_apps.xml.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    onSearchDismiss: () -> Unit = {},
    actions: @Composable (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    if (isSearchActive) {
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { /* handled by onSearchQueryChange */ },
            active = isSearchActive,
            onActiveChange = onSearchActiveChange,
            onDismiss = onSearchDismiss,
            placeholder = { Text(text = stringResource(R.string.action_search_apps)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            scrollBehavior = scrollBehavior
        ) {
            /* Search results would go here */
        }
    } else {
        AppTopAppBar(
            title = title,
            modifier = modifier,
            actions = {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.action_search))
                }
                actions?.invoke()
            },
            scrollBehavior = scrollBehavior
        )
    }
}