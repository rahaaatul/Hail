# Hail App - Compose Migration Plan: ApiActivity

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate ApiActivity from XML layout + ViewBinding to Jetpack Compose using setContent, preserving all functionality: translucent activity showing API documentation or help content.

**Architecture:** 
- Replace `activity_api.xml` with a ComposeView in ApiActivity (or use setContent directly since it's an Activity)
- Create `@Composable ApiScreen` that hosts the UI
- Use `ScrollableState` and `VerticalScroller` for scrolling text content
- Use `rememberSaveable` for UI state (scroll position)
- Use `AnnotatedString` to display formatted API documentation (from strings or assets)
- Preserve the translucent theme and window flags
- No ViewModel needed for this simple screen

**Tech Stack:**
- Jetpack Compose, Material3
- Core Compose text handling for scrolling and formatted text
- Activity-ktx for setContent extension

## File Changes

### 1. Update ApiActivity to Use Compose
```kotlin
// app/src/main/java/com/aistra/hail/ApiActivity.kt
package com.aistra.hail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.ui.theme.ApiScreen
import com.aistra.hail.ui.theme.HailTheme

class ApiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the translucent theme and window flags from XML
        setContent {
            HailTheme {
                ApiScreen(
                    // ... pass any necessary parameters
                )
            }
        }
    }
}

// Note: The activity_api.xml layout can be removed entirely
```

### 2. Create ApiScreen Composable
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/ApiScreen.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.ui.theme.HailTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.absoluteFill
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.core.TextLayoutResult
import androidx.compose.foundation.text.core.TextMeasurer
import androidx.compose.foundation.text.core.ParagraphIntrinsics
import androidx.compose.foundation.text.core.ParagraphStyle
import androidx.compose.foundation.text.drawScope.drawWithCache
import androidx.compose.foundation.text.drawScope.drawText
import androidx.compose.foundation.text.drawScope.drawParagraph
import androidx.compose.foundation.text.drawScope.drawLayout
import androidx.compose.foundation.text.drawScope.drawPlaceholder
import androidx.compose.foundation.text.drawScope.drawMultiParagraph
import androidx.compose.foundation.text.drawScope.drawTextLayout
import androidx.compose.foundation.text.drawScope.drawTextLayoutResult
import androidx.compose.foundation.text.drawScope.drawTextMeasurer
import androidx.compose.foundation.text.drawScope.drawTextStyle
import androidx.compose.foundation.text.drawScope.drawVariant
import androidx.compose.foundation.text.drawScope.drawParagraphStyle
import androidx.compose.foundation.text.drawScope.drawPlaceholderVerticalAlign
import androidx.compose.foundation.text.drawScope.drawPlaceholderHeight
import androidx.compose.foundation.text.drawScope.drawPlaceholderWidth
import androidx.compose.foundation.text.drawScope.drawPlaceholderBaseline
import androidx.compose.foundation.text.drawScope.drawPlaceholderAlign
import androidx.compose.foundation.text.drawScope.drawPlaceholderOffset
import androidx.compose.foundation.text.drawScope.drawPlaceholderRotation
import androidx.compose.foundation.text.drawScope.drawPlaceholderScale
import androidx.compose.foundation.text.drawScope.drawPlaceholderSkewX
import androidx.compose.foundation.text.drawScope.drawPlaceholderSkewY
import androidx.compose.foundation.text.drawScope.drawPlaceholderPivotX
import androidx.compose.foundation.text.drawScope.drawPlaceholderPivotY
import androidx.compose.foundation.text.drawScope.drawPlaceholderAlpha
import androidx.compose.foundation.text.drawScope.drawPlaceholderColor
import androidx.compose.foundation.text.drawScope.drawPlaceholderFontSize
import androidx.compose.foundation.text.drawScope.drawPlaceholderFontWeight
import androidx.compose.foundation.text.drawScope.drawPlaceholderFontStyle
import androidx.compose.foundation.text.drawScope.drawPlaceholderFontFamily
import androidx.compose.foundation.text.drawScope.drawPlaceholderLetterSpacing
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextGeometricTransform
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextDirection
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextAlign
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextIndent
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextDirection
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextAlign
import androidx.compose.foundation.text.drawScope.drawPlaceholderTextIndent
            end text
        
        }
    
    }
}