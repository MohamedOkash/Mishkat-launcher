package com.mishkat.launcher.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal Section Header Capsule:
 * High-legibility dark glass pill with an Apple-style Accent Indicator bar
 * and deep text shadow, ensuring 100% readability over any photographic wallpaper.
 */
@Composable
fun SectionTitlePill(
    title: String,
    tokens: ThemeTokens,
    icon: String? = null,
    badgeText: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE6090E1A))
            .border(1.dp, tokens.glassRim.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Colored Accent Pill Indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tokens.primary),
        )
        if (!icon.isNullOrBlank()) {
            Text(text = icon, fontSize = 14.sp)
        }
        Text(
            text = title,
            color = tokens.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.95f),
                    offset = Offset(0f, 2f),
                    blurRadius = 14f,
                ),
            ),
        )
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.primary.copy(alpha = 0.22f))
                    .border(0.5.dp, tokens.primary.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badgeText,
                    color = tokens.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
