# Hail App Compose Migration Plan: HomeFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate HomeFragment from XML layout + ViewBinding + ViewPager2 + TabLayout to Jetpack Compose using TabRow and androidx.compose.foundation:pager, preserving all functionality: tab navigation, FAB, and hosting PagerFragment pages.

**Architecture:** 
- Replace `fragment_home.xml` with a `ComposeView` in `HomeFragment.onCreateView`
- Create `@Composable HomeScreen` that hosts the UI
- Use `TabRow` for tab labels at the top
- Use `Pager` (from androidx.compose.foundation) for swiping between tabs
- Each tab page is a `PagerScreen` composable (could inline the PagerFragment logic here, but we'll keep separation by calling the same PagerScreen used in PagerFragment)
- Use `rememberSaveable` for UI state (selected tab index, scroll state)
- Use `StateFlow` from ViewModel collected with `collectAsState()` for tab titles and visibility
- Preserve `HomeViewModel` (no changes needed) but expose `StateFlow` for UI
- Keep the FAB (floating action button) for adding apps to a custom tab
- Navigation to other destinations (Apps, Actions, etc.) remains via Navigation Component (kept in XML nav graph)

**Tech Stack:**
- Jetpack Compose, Material3 (includes foundation pager)
- ViewModel with StateFlow (unchanged)

**Spec:** This plan is based on comprehensive codebase analysis of the HomeFragment and related components.

## Global Constraints
- Jetpack Compose BOM 2026.08.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- Navigation Component 2.10.0 (Fragment-based)
- HomeViewModel exposed as StateFlow for Compose integration

---

### Task 1: Update HomeFragment to Use ComposeView
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/home/HomeFragment.kt`

**Steps:**
- [ ] Replace ViewBinding inflation with `ComposeView` in `onCreateView`
- [ ] Set `ViewCompositionStrategy` to `DisposeOnViewTreeLifecycleDestroyed`
- [ ] Set content to `HailTheme { HomeScreen(viewModel = viewModel, ...) }`

### Task 2: Create HomeScreen Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/HomeScreen.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier)`
- [ ] Collect tab titles and visibility from `viewModel` StateFlow properties
- [ ] Implement layout with `TabRow` and `Pager`
- [ ] Map each tab/page to a `PagerScreen` composable for the corresponding tab type
- [ ] Add `FAB` for adding apps to custom tab
- [ ] Handle navigation to other destinations via `Navigation Component`

### Task 3: Create TabRow Implementation
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/HomeTabRow.kt` (or inline in HomeScreen)

**Steps:**
- [ ] Implement `TabRow` with `Tab` components for each tab
- [ ] Use selected tab index from `viewModel` StateFlow (e.g., `selectedTabIndex`)
- [ ] Handle tab selection callbacks (e.g., `onClick = { tabIndex -> viewModel.selectTab(tabIndex) }`)
- [ ] Apply proper styling, indicators, and dimensions for selected/unselected tabs
- [ ] Use `scrollableTabRow()` for long tab lists if needed

### Task 4: Create Pager Implementation
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/HomePager.kt` (or inline in HomeScreen)

**Steps:**
- [ ] Import `Pager` from `androidx.compose.foundation.lazy`
- [ ] Set `pageCount` to match the number of tabs from `viewModel`
- [ ] Set `currentPage` to match `selectedTabIndex` from `viewModel` with bidirectional binding
- [ ] Each `page/tab` contains `PagerScreen(tabType = tabTypes[page])` or inline equivalent logic
- [ ] Apply proper paging behavior, page transitions, and indicators
- [ ] Consider using `pageSize = PageSize.FillWidth` for full-width pages

### Task 5: Create FAB for Custom Tab Creation
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/HomeFab.kt` (or inline in HomeScreen)

**Steps:**
- [ ] Implement `FloatingActionButton` with:
    - `onClick = { viewModel.showCreateTabDialog(true) }`
    - `modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)`
- [ ] Use `Icons.Default.Add` for the icon
- [ ] Set `contentDescription = "Add tab"`
- [ ] Only show FAB when appropriate based on `viewModel` state (e.g., `!viewModel.isEditingTab`)

### Task 6: Update HomeViewModel to Expose StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/home/HomeViewModel.kt`

**Steps:**
- [ ] Replace `MutableLiveData` with `MutableStateFlow` for relevant UI state
- [ ] Expose `StateFlow` properties for:
    - Tab titles (`tabTitles: StateFlow<List<String>>`)
    - Tab visibility (`tabVisibility: StateFlow<List<Boolean>>`)
    - Selected tab index (`selectedTabIndex: StateFlow<Int>`)
    - Any other relevant state (editing mode, dialog states, etc.)
- [ ] Implement appropriate `UiState` data class if beneficial (e.g., `HomeUiState`)
- [ ] Update all methods to update state flows instead of individual `LiveData`
- [ ] Ensure proper initialization and state update logic for tab operations (create, rename, delete, select)

### Task 7: Validate HomeFragment Migration
**Files:**
- No new files to create (validation uses existing files)

**Steps:**
- [ ] Run `./gradlew assembleDebug` to ensure successful build
- [ ] Test tab navigation works correctly via `TabRow` taps
- [ ] Verify horizontal paging works via swipe gestures
- [ ] Check `TabRow` and `Pager` stay in sync (when one changes, the other updates)
- [ ] Confirm each tab displays correct content via `PagerScreen` (all, frequent, recent, custom tabs)
- [ ] Test FAB opens custom tab creation dialog properly
- [ ] Verify navigation to other destinations (Apps, Actions, Settings, About) works via `Navigation Component`
- [ ] Ensure state survives configuration changes (rotation, multi-window)
- [ ] Confirm TalkBack accessibility works for all tabs, tab labels, FAB, and page content
- [ ] Test performance with multiple tabs containing many apps (should be smooth)
- [ ] Verify that tab creation, renaming, and deletion work correctly through `ViewModel`

## References
- [Jetpack Compose TabRow](https://developer.android.com/jetpack/compose/components#tab-row)
- [Jetpack Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Navigation Component with Compose](https://developer.android.com/guide/navigation/navigation-compose)