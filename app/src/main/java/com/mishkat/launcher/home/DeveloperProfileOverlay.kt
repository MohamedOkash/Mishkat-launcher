package com.mishkat.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.AppSettings
import com.mishkat.launcher.remote.QrCodeEncoder
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass

@Composable
fun DeveloperProfileOverlay(
    tokens: ThemeTokens,
    appSettings: AppSettings,
    onDismiss: () -> Unit,
) {
    val isArabic = appSettings.isArabic
    val glass = appSettings.glassIntensity
    val closeFocus = remember { FocusRequester() }
    val websiteUrl = "https://mohamed-okash.web.app"
    val qrMatrix = remember(websiteUrl) { QrCodeEncoder.encodeVersion3L(websiteUrl) }

    BackHandler { onDismiss() }

    LaunchedEffect(Unit) {
        closeFocus.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(tokens.cardRadius + 8.dp))
                .liquidGlass(
                    tokens = tokens,
                    baseColor = tokens.surface,
                    radius = tokens.cardRadius + 8.dp,
                    glassIntensity = glass,
                    borderColor = tokens.primary,
                )
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header Row: Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.primary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "👨‍💻", fontSize = 28.sp)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Eng. Mohamed Okash",
                            color = tokens.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = if (isArabic)
                                "هندسة النظم والبنية التحتية (IT Infrastructure) والسلامة المهنية (HSE)"
                            else
                                "IT Infrastructure & HSE Safety Engineer • Systems Architecture",
                            color = tokens.onSurfaceMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .focusRequester(closeFocus)
                        .mishkatFocus(id = "dev_profile:close", radius = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .liquidGlass(tokens, tokens.cardFocused, 12.dp, glass)
                        .clickable { onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.radio_close),
                        color = tokens.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Main Content: Left side projects & info, Right side QR Code & Portfolio link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // Column 1: Bio and Featured Projects
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Bio Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .liquidGlass(tokens, tokens.card, 14.dp, glass)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = if (isArabic)
                                "أدمج بين خبرة 7 سنوات في البنية التحتية لتكنولوجيا المعلومات والشبكات المؤسسية، مع هندسة السلامة المهنية وتحليل المخاطر الميدانية، لتصميم وتطوير نظم برمجية وتطبيقات عملية متقدمة تجمع بين الدقة، الأمان، وسرعة الأداء الفائقة."
                            else
                                "Bridging 7 years of enterprise IT infrastructure and networks with modern Health & Safety engineering to build high-performance systems and practical smart applications.",
                            color = tokens.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        )
                    }

                    Text(
                        text = if (isArabic) "✦ أبرز المنظومات والمشاريع الميدانية:" else "✦ Featured Enterprise Projects:",
                        color = tokens.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    // 4 Flagship Projects in a Grid/Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProjectCard(
                            modifier = Modifier.weight(1f),
                            tokens = tokens,
                            glass = glass,
                            glyph = "🛠",
                            title = "Maintix",
                            desc = if (isArabic) "نظام ذكي لإدارة الصيانة والتشغيل" else "Smart Maintenance & Ops Platform",
                        )
                        ProjectCard(
                            modifier = Modifier.weight(1f),
                            tokens = tokens,
                            glass = glass,
                            glyph = "🏗",
                            title = "ConstructFlow",
                            desc = if (isArabic) "منصة تتبع المشاريع الإنشائية" else "Construction Management Engine",
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProjectCard(
                            modifier = Modifier.weight(1f),
                            tokens = tokens,
                            glass = glass,
                            glyph = "🛡",
                            title = "HSE Pro",
                            desc = if (isArabic) "منظومة الامتثال الرقمي للسلامة" else "Workplace Safety & OSHA Compliance",
                        )
                        ProjectCard(
                            modifier = Modifier.weight(1f),
                            tokens = tokens,
                            glass = glass,
                            glyph = "🔗",
                            title = "Wasla Link",
                            desc = if (isArabic) "شبكة الربط والتكامل المؤسسي" else "Enterprise Infrastructure Hub",
                        )
                    }
                }

                // Column 2: Scannable QR Code to visit portfolio
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(BorderStroke(3.dp, tokens.primary), RoundedCornerShape(18.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val n = qrMatrix.size
                            val cell = size.width / n
                            for (r in 0 until n) {
                                for (c in 0 until n) {
                                    if (qrMatrix[r][c]) {
                                        drawRect(
                                            color = Color(0xFF0F172A),
                                            topLeft = Offset(c * cell, r * cell),
                                            size = Size(cell, cell),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (isArabic) "امسح الباركود بهاتفك" else "Scan QR with Phone",
                            color = tokens.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "mohamed-okash.web.app",
                            color = tokens.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "mohamed.okash1998@gmail.com",
                            color = tokens.onSurfaceMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    modifier: Modifier,
    tokens: ThemeTokens,
    glass: Float,
    glyph: String,
    title: String,
    desc: String,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .liquidGlass(tokens, tokens.card, 12.dp, glass)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = glyph, fontSize = 22.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = tokens.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = desc,
                    color = tokens.onSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
