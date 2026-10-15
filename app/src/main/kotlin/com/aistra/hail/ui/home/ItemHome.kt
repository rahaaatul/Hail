package com.aistra.hail.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.aistra.hail.R
import com.aistra.hail.ui.theme.AppTheme

data class ItemHomeState(
    var icon: Bitmap?,
    var appName: String,
    var textColor: Color = Color.Unspecified,
    var textSize: TextUnit = TextUnit.Unspecified,
    var enabled: Boolean = true
)

@Composable
fun ItemHome(
    state: ItemHomeState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    onClick = { if (state.enabled) onClick() },
                    enabled = state.enabled
                )
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                // TODO: Display bitmap icon correctly
                val painter = if (state.icon != null) {
                    // TODO: Replace with actual bitmap painter
                    painterResource(R.drawable.ic_launcher_foreground)
                } else {
                    painterResource(R.drawable.ic_launcher_foreground)
                }
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(8.dp)
                    , contentScale = ContentScale.Fit
                )

                Text(
                    text = state.appName,
                    color = state.textColor,
                    fontSize = state.textSize,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewItemHome() {
    AppTheme {
        ItemHome(
            state = ItemHomeState(
                icon = null,
                appName = "Sample App",
                textColor = MaterialTheme.colorScheme.primary,
                textSize = 16.sp,
                enabled = true
            ),
            onClick = { },
            onLongClick = { }
        )
    }
}