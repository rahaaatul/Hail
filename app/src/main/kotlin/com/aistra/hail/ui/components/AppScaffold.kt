package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.ui.navigation.HailRoute

/**
 * Material 3 Expressive Scaffold wrapper.
 * Replaces the CoordinatorLayout + AppBarLayout + BottomNav from activity_main.xml.
 *
 * @param title Top bar title
 * @param navController Current navigation controller
 * @param currentRoute Currently selected route (for bottom nav / rail)
 * @param onItemSelected Navigation item selection callback
 * @param fab FAB composable
 * @param useNavRail Whether to show navigation rail instead of bottom bar
 * @param actions Top bar action icons
 * @param content Screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    navController: androidx.navigation.compose.NavHostController,
    currentRoute: HailRoute?,
    onItemSelected: (HailRoute) -> Unit,
    fab: @Composable (() -> Unit)? = null,
    useNavRail: Boolean = false,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = rememberTopAppBarState()
    val topBarScrollBehavior = TopAppBarScrollBehavior.entranceScrollBehavior(scrollBehavior)

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { navController.hailGoBack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(id = android.R.string.ok))
                    }
                },
                actions = actions,
                scrollBehavior = topBarScrollBehavior
            )
        },
        bottomBar = {
            if (!useNavRail) {
                AppBottomNav(
                    currentRoute = currentRoute,
                    onItemSelected = onItemSelected
                )
            }
        },
        floatingActionButton = fab,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (useNavRail) {
                AppNavigationRail(
                    currentRoute = currentRoute,
                    onItemSelected = onItemSelected
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
        }
    }
}