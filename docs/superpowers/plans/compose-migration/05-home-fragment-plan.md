# Hail App Compose Migration Plan: HomeFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate HomeFragment from XML layout + ViewBinding + ViewPager2 + TabLayout to Jetpack Compose using TabRow and Accompanist HorizontalPager (or androidx.compose.foundation:pager when stable), preserving all functionality: tab navigation, FAB, and hosting PagerFragment pages.

**Architecture:** 
- Replace `fragment_home.xml` with a `ComposeView` in `HomeFragment.onCreateView`
- Create `@Composable HomeScreen` that hosts the UI
- Use `TabRow` for tab labels at the top
- Use `HorizontalPager` (from Accompanist Material3 0.37.2) for swiping between tabs
- Each tab page is a `PagerScreen` composable (could inline the PagerFragment logic here, but we'll keep separation by calling the same PagerScreen used in PagerFragment)
- Use `rememberSaveable` for UI state (selected tab index, scroll state)
- Use `StateFlow` from ViewModel collected with `collectAsState()` for tab titles and visibility
- Preserve `HomeViewModel` (no changes needed) but expose `StateFlow` for UI
- Keep the FAB (floating action button) for adding apps to a custom tab
- Navigation to other destinations (Apps, Actions, etc.) remains via Navigation Component (kept in XML nav graph)

**Tech Stack:**
- Jetpack Compose, Material3
- Accompanist Material3 HorizontalPager 0.37.2 (implementation detail: may migrate to androidx.compose.foundation:paper when stable)
- ViewModel with StateFlow (unchanged)

## File Changes

### 1. Update HomeFragment to Use ComposeView
#### Task 1: Update HomeFragment.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/HomeFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView in onCreateView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { HomeScreen(viewModel = viewModel, ...) }

### 2. Create HomeScreen Composable
#### Task 2: Create HomeScreen.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/HomeScreen.kt
- [ ] Implement @Composable fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier)
- [ ] Collect tab titles and visibility from viewModel StateFlow properties
- [ ] Implement Column layout or direct content
- [ ] Add TabRow for tab labels
- [ ] Add HorizontalPager for swiping between tabs
- [ ] Each tab/page contains a PagerScreen for the corresponding tab type
- [ ] Add FAB for adding apps to custom tab
- [ ] Handle navigation to other destinations via Navigation Component

### 3. Create Supporting Composables
#### Task 3: Create TabRow Implementation
- [ ] Implement TabRow with Tab components for each tab
- [ ] Use selected tab index from viewModel StateFlow (e.g., selectedTabIndex)
- [ ] Handle tab selection callbacks (onClick/{ tabIndex -> viewModel.selectTab(tabIndex) })
- [ ] Apply proper styling, indicators, and dimensions for selected/unselected tabs

#### Task 4: Create HorizontalPager Implementation
- [ ] Import HorizontalPager from androidx.accompanist.material3.horizontalpager (remember this is temporary)
- [ ] Set page count to match the number of tabs from viewModel
- [ ] Set current page to match selectedTabIndex from viewModel with bidirectional binding
- [ ] Each page/tab contains PagerScreen(tabType = tabTypes[page]) or inline equivalent logic
- [ ] Apply proper paging behavior, page transitions, and indicators

#### Task 5: Create FAB for Custom Tab Creation
- [ ] Implement FloatingActionButton with onClick = { viewModel.showCreateTabDialog(true) }
- [ ] Position at bottom end with appropriate padding (e.g., modifier.align(Alignment.BottomEnd).padding(16.dp))
- [ ] Use Icons.Default.Add for the icon with contentDescription = "Add tab"
- [ ] Only show FAB when appropriate based on viewModel state (e.g., !viewModel.isEditingTab)

#### Task 6: Update HomeViewModel to Expose StateFlow
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/HomeViewModel.kt
- [ ] Replace MutableLiveData with MutableStateFlow for relevant UI state
- [ ] Expose StateFlow properties for tab titles, visibility, selected tab index, etc.
- [ ] Implement appropriate UiState data class if beneficial
- [ ] Update all methods to update state flows instead of individual LiveData
- [ ] Ensure proper initialization and state update logic for tab operations

### 4. Validate HomeFragment Migration
#### Task 7: Validate HomeFragment Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Test tab navigation works correctly via TabRow taps
- [ ] Verify horizontal paging works via swipe gestures
- [ ] Check TabRow and HorizontalPager stay in sync (when one changes, the other updates)
- [ ] Confirm each tab displays correct content via PagerScreen (all, frequent, recent, custom tabs)
- [ ] Test FAB opens custom tab creation dialog properly
- [ ] Verify navigation to other destinations (Apps, Actions, Settings, About) works via Navigation Component
- [ ] Ensure state survives configuration changes (rotation, multi-window)
- [ ] Confirm TalkBack accessibility works for all tabs, tabs labels, FAB, and page content
- [ ] Test performance with multiple tabs containing many apps (should be smooth)
- [ ] Verify that tab creation, renaming, and deletion work correctly through ViewModel

## References

- [Accompanist Material3 HorizontalPager](https://github.com/google/accompanist/tree/main/horizontal-pager)
- [Jetpack Compose TabRow](https://developer.android.com/jetpack/compose/components#tab-row)
- [Jetpack Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Navigation Component with Compose](https://developer.android.com/guide/navigation/navigation-compose)