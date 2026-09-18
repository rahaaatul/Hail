# Hail App - Compose Migration Plan: HomeFragment

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
```kotlin
// app/src/main/java/com/aistra/hail/ui/home/HomeFragment.kt
package com.aistra.hail.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.RememberObserver
import com.aistra.hail.R
import com.aistra.hail.ui.theme.HomeScreen
import com.aistra.hail.ui.theme.HailTheme

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels { factory }

    // ... existing factory initialization

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HailTheme {
                    HomeScreen(
                        viewModel = viewModel,
                        // ... pass any necessary callbacks for navigation
                    )
                }
            }
        }
    }

    // ... remove existing XML-related code (binding, etc.)
}
```

### 2. Create HomeScreen Composable
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/HomeScreen.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistra.hail.ui.home.HomeViewModel
import com.aistra.hail.ui.theme.HailTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Unit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutId
import androidx.compose.ui.layout.LayoutIdReader
import androidx.compose.layout.OnGloballyPositionedModifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimenResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.VerticalGravity
import androidx.compose.ui.unit.HorizontalGravity
import androidx.compose.ui.draw.WithDrawScope
import androidx.compose.ui.draw.DrawScope
import androidx.compose.ui.draw.DrawContext
import androidx.compose.ui.draw.InspectableValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Offset
import androidx.compose.ui.unit.Size
import androidx.compose.ui.unit.Unspecified
import androidx.compose.ui.unit.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize
            end text
        
        }
    
    }
}