package com.mishkat.remote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mishkat.remote.client.TvApiClient
import com.mishkat.remote.discovery.DiscoveredTv
import kotlinx.coroutines.launch
import org.json.JSONObject

// Color Palette - Liquid Glass & Mishkat Night Theme
private val BgNight = Color(0xFF070B14)
private val GlassSurface = Color(0x331C2844)
private val GlassBorder = Color(0x33FFFFFF)
private val GoldAccent = Color(0xFFD4AF37)
private val GoldGlow = Color(0x40D4AF37)
private val TextWhite = Color(0xFFF5F1E6)
private val TextMuted = Color(0xFF8E9AAF)
private val ColorGreen = Color(0xFF2EC47E)
private val ColorRed = Color(0xFFE63946)
private val ColorYellow = Color(0xFFF4A226)
private val ColorBlue = Color(0xFF2A9D8F)

enum class RemoteTab(val label: String) {
    DPAD("الريموت"),
    TOUCHPAD("الماوس"),
    CHANNELS("القنوات"),
    APPS("التطبيقات"),
    KEYBOARD("الكتابة")
}

@Composable
fun RemoteScreen(
    currentTv: DiscoveredTv?,
    isSearching: Boolean,
    onSaveManualIp: (String) -> Unit,
    onVibrate: () -> Unit,
    onLaunchVoiceInput: () -> Unit,
    recognizedVoiceText: String?,
    onClearVoiceText: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(RemoteTab.DPAD) }
    var showIpDialog by remember { mutableStateOf(false) }
    var manualIpText by remember { mutableStateOf(currentTv?.ip ?: "192.168.1.") }
    var boostStatusMessage by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    var showM3uDialog by remember { mutableStateOf(false) }
    var tvChannels by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var tvApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // Update inputText when voice returns
    if (!recognizedVoiceText.isNullOrBlank() && recognizedVoiceText != inputText) {
        inputText = recognizedVoiceText
        onClearVoiceText()
    }

    val ip = currentTv?.ip ?: "192.168.1.6"
    val port = currentTv?.port ?: 8787

    // Fetch live channels & apps on TV connection
    androidx.compose.runtime.LaunchedEffect(ip) {
        val info = TvApiClient.fetchInfo(ip, port)
        if (info != null) {
            val json = runCatching { JSONObject(info) }.getOrNull()
            val chArr = json?.optJSONArray("channels")
            if (chArr != null) {
                val list = mutableListOf<Pair<String, String>>()
                for (i in 0 until chArr.length()) {
                    val o = chArr.getJSONObject(i)
                    list.add(o.optString("id") to o.optString("name"))
                }
                tvChannels = list
            }
            val appArr = json?.optJSONArray("apps")
            if (appArr != null) {
                val list = mutableListOf<Pair<String, String>>()
                for (i in 0 until appArr.length()) {
                    val o = appArr.getJSONObject(i)
                    list.add(o.optString("pkg") to o.optString("name"))
                }
                tvApps = list
            }
        }
    }

    fun sendKey(key: String) {
        onVibrate()
        scope.launch { TvApiClient.sendKey(ip, port, key) }
    }

    fun sendBoost() {
        onVibrate()
        scope.launch {
            boostStatusMessage = "جارٍ فحص وتحرير الذاكرة..."
            val response = TvApiClient.sendBoost(ip, port)
            if (response != null) {
                val json = runCatching { JSONObject(response) }.getOrNull()
                val freed = json?.optDouble("freedMb", 0.0) ?: 0.0
                val killed = json?.optInt("killedCount", 0) ?: 0
                boostStatusMessage = if (freed > 0) {
                    "تم بنجاح! تحرير ${String.format(java.util.Locale.US, "%.1f", freed)} MB (إيقاف $killed تطبيق)"
                } else {
                    "الذاكرة نظيفة ومثالية بالفعل!"
                }
            } else {
                boostStatusMessage = "تعذر الاتصال بالتلفزيون. تأكد من اتصال الجهازين بنفس الشبكة."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgNight, Color(0xFF0F1528), BgNight)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Connection & TV Info + Boost Button
            TopConnectionHeader(
                currentTv = currentTv,
                isSearching = isSearching,
                onEditIpClick = { showIpDialog = true },
                onBoostClick = { sendBoost() },
                onStorageClick = {
                    onVibrate()
                    scope.launch { TvApiClient.openStorage(ip, port) }
                }
            )

            // Boost message banner
            AnimatedVisibility(visible = boostStatusMessage != null) {
                boostStatusMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE614261B))
                            .border(1.dp, ColorGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { boostStatusMessage = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            text = msg,
                            style = TextStyle(
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Segmented Tabs
            TabSelector(
                selectedTab = selectedTab,
                onSelectTab = {
                    onVibrate()
                    selectedTab = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTab) {
                    RemoteTab.DPAD -> DpadTabContent(
                        onSendKey = { sendKey(it) }
                    )
                    RemoteTab.TOUCHPAD -> TouchpadTabContent(
                        ip = ip,
                        port = port,
                        onVibrate = onVibrate
                    )
                    RemoteTab.CHANNELS -> ChannelsTabContent(
                        channels = tvChannels,
                        onSelectChannel = { chId ->
                            onVibrate()
                            scope.launch { TvApiClient.sendKey(ip, port, "tv") }
                        },
                        onOpenM3uDialog = { showM3uDialog = true }
                    )
                    RemoteTab.APPS -> AppsTabContent(
                        installedApps = tvApps,
                        onLaunch = { pkg ->
                            onVibrate()
                            scope.launch { TvApiClient.launchPackage(ip, port, pkg) }
                        },
                        onSendKey = { sendKey(it) },
                        onOpenStorage = {
                            onVibrate()
                            scope.launch { TvApiClient.openStorage(ip, port) }
                        }
                    )
                    RemoteTab.KEYBOARD -> KeyboardTabContent(
                        inputText = inputText,
                        onInputTextChanged = { inputText = it },
                        onSendText = { text ->
                            onVibrate()
                            scope.launch { TvApiClient.sendText(ip, port, text) }
                        },
                        onVoiceClick = onLaunchVoiceInput
                    )
                }
            }
        }

        // IP Configuration Dialog
        if (showIpDialog) {
            ManualIpDialog(
                currentIp = manualIpText,
                onDismiss = { showIpDialog = false },
                onSave = { newIp ->
                    manualIpText = newIp
                    onSaveManualIp(newIp)
                    showIpDialog = false
                }
            )
        }

        // M3U Playlist Import Dialog
        if (showM3uDialog) {
            M3uImportDialog(
                onDismiss = { showM3uDialog = false },
                onSendM3u = { m3uData ->
                    onVibrate()
                    scope.launch {
                        val ok = TvApiClient.sendM3u(ip, port, m3uData)
                        boostStatusMessage = if (ok) "تم إرسال قائمة M3U للتلفزيون بنجاح!" else "تعذر إرسال القائمة. تأكد من اتصال الشاشة."
                    }
                    showM3uDialog = false
                }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Header & Navigation Tabs
// -------------------------------------------------------------------------------------------------

@Composable
private fun TopConnectionHeader(
    currentTv: DiscoveredTv?,
    isSearching: Boolean,
    onEditIpClick: () -> Unit,
    onBoostClick: () -> Unit,
    onStorageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onEditIpClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (currentTv != null) ColorGreen else ColorYellow)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                BasicText(
                    text = currentTv?.name ?: if (isSearching) "جارٍ البحث عن التلفزيون..." else "غير متصل",
                    style = TextStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                BasicText(
                    text = currentTv?.ip?.let { "$it (تغيير ⚙️)" } ?: "اضغط لإدخال الـ IP",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Storage Explorer Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x331C2844))
                    .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onStorageClick() }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                BasicText(
                    text = "🖴 التخزين",
                    style = TextStyle(color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }

            // Quick TV Boost Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8B2500), Color(0xFFD4380D))))
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onBoostClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "🚀 تسريع TV",
                        style = TextStyle(color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: RemoteTab,
    onSelectTab: (RemoteTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RemoteTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) GoldGlow else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) GoldAccent else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectTab(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = tab.label,
                    style = TextStyle(
                        color = if (isSelected) GoldAccent else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Tab 1: D-Pad & TV Controls
// -------------------------------------------------------------------------------------------------

@Composable
private fun DpadTabContent(
    onSendKey: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Power & Mute & TV inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GlassIconButton(label = "إيقاف ⏻", color = ColorRed) { onSendKey("back") }
            GlassIconButton(label = "كتم 🔇", color = TextWhite) { onSendKey("mute") }
            GlassIconButton(label = "مدخل 🔌", color = TextWhite) { onSendKey("input") }
            GlassIconButton(label = "بث 📺", color = GoldAccent) { onSendKey("tv") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Circular D-Pad
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(GlassSurface)
                .border(2.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // UP
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(60.dp, 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSendKey("up") },
                contentAlignment = Alignment.Center
            ) {
                BasicText(text = "▲", style = TextStyle(color = GoldAccent, fontSize = 22.sp))
            }

            // DOWN
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(60.dp, 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSendKey("down") },
                contentAlignment = Alignment.Center
            ) {
                BasicText(text = "▼", style = TextStyle(color = GoldAccent, fontSize = 22.sp))
            }

            // LEFT
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(48.dp, 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSendKey("left") },
                contentAlignment = Alignment.Center
            ) {
                BasicText(text = "◀", style = TextStyle(color = GoldAccent, fontSize = 22.sp))
            }

            // RIGHT
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(48.dp, 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSendKey("right") },
                contentAlignment = Alignment.Center
            ) {
                BasicText(text = "▶", style = TextStyle(color = GoldAccent, fontSize = 22.sp))
            }

            // CENTER OK
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GoldGlow, GlassSurface)))
                    .border(2.dp, GoldAccent, CircleShape)
                    .clickable { onSendKey("ok") },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "OK",
                    style = TextStyle(color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Nav: Back, Home, Play/Pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GlassIconButton(label = "رجوع ↩", color = TextWhite) { onSendKey("back") }
            GlassIconButton(label = "الرئيسية ⌂", color = GoldAccent) { onSendKey("home") }
            GlassIconButton(label = "تشغيل ⏯", color = TextWhite) { onSendKey("play_pause") }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Volume & Channel Rockers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Volume Rocker
            RockerControl(
                topLabel = "VOL +",
                bottomLabel = "VOL -",
                onTopClick = { onSendKey("vol_up") },
                onBottomClick = { onSendKey("vol_down") }
            )

            // Channel Rocker
            RockerControl(
                topLabel = "CH +",
                bottomLabel = "CH -",
                onTopClick = { onSendKey("ch_up") },
                onBottomClick = { onSendKey("ch_down") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Color Buttons (Red, Green, Yellow, Blue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorButton(ColorRed) { onSendKey("red") }
            ColorButton(ColorGreen) { onSendKey("green") }
            ColorButton(ColorYellow) { onSendKey("yellow") }
            ColorButton(ColorBlue) { onSendKey("blue") }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Tab 2: Touchpad & Air Mouse
// -------------------------------------------------------------------------------------------------

@Composable
private fun TouchpadTabContent(
    ip: String,
    port: Int,
    onVibrate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var lastTapTime by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            text = "حرك إصبعك للتحكم في الماوس • انقر للنقر المباشر",
            style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Touchpad Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurface)
                .border(2.dp, GlassBorder, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 150) {
                                lastTapTime = now
                                onVibrate()
                                scope.launch { TvApiClient.sendTouch(ip, port, 0f, 0f, "click") }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Send relative movement
                        scope.launch {
                            TvApiClient.sendTouch(ip, port, dragAmount.x * 1.5f, dragAmount.y * 1.5f, "move")
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    text = "⊹",
                    style = TextStyle(color = GoldGlow, fontSize = 48.sp)
                )
                BasicText(
                    text = "لوحة لمس تفاعلية (Touchpad)",
                    style = TextStyle(color = TextMuted.copy(alpha = 0.6f), fontSize = 14.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mouse Click Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        onVibrate()
                        scope.launch { TvApiClient.sendTouch(ip, port, 0f, 0f, "click") }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "نقر أيسر (OK)",
                    style = TextStyle(color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        onVibrate()
                        scope.launch { TvApiClient.sendKey(ip, port, "back") }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "نقر أيمن (رجوع)",
                    style = TextStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Tab 3: Keyboard & Voice Input
// -------------------------------------------------------------------------------------------------

@Composable
private fun KeyboardTabContent(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendText: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = "اكتب النص أو تحدث بالصوت لإرساله إلى التلفزيون مباشرة",
            style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            if (inputText.isEmpty()) {
                BasicText(
                    text = "اكتب هنا للبحث أو إدخال الروابط...",
                    style = TextStyle(color = TextMuted.copy(alpha = 0.5f), fontSize = 14.sp)
                )
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                textStyle = TextStyle(color = TextWhite, fontSize = 15.sp),
                cursorBrush = SolidColor(GoldAccent),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: Send & Mic
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Send Button
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFB8860B), GoldAccent)))
                    .clickable {
                        if (inputText.isNotBlank()) {
                            onSendText(inputText)
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "إرسال إلى التلفزيون ⏎",
                    style = TextStyle(color = BgNight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }

            // Voice Mic Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, ColorBlue, RoundedCornerShape(12.dp))
                    .clickable { onVoiceClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "🎙️ إملاء صوتي",
                    style = TextStyle(color = ColorBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        BasicText(
            text = "عبارات بحث سريعة بنقرة واحدة:",
            style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Start),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val quickSearches = listOf(
            "إذاعة القرآن الكريم",
            "سورة البقرة بصوت نقي",
            "أهداف مباريات اليوم",
            "ناشيونال جيوغرافيك وثائقي",
            "يوتيوب أطفال",
            "أخبار الساعة مباشر"
        )

        quickSearches.forEach { phrase ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        onInputTextChanged(phrase)
                        onSendText(phrase)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = phrase,
                        style = TextStyle(color = TextWhite, fontSize = 13.sp)
                    )
                    BasicText(
                        text = "إرسال ↗",
                        style = TextStyle(color = GoldAccent, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Tab 3: TV Channels & M3U Import
// -------------------------------------------------------------------------------------------------

@Composable
private fun ChannelsTabContent(
    channels: List<Pair<String, String>>,
    onSelectChannel: (String) -> Unit,
    onOpenM3uDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // M3U Import Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF0F3A40), Color(0xFF1E5B66))))
                .border(1.dp, Color(0xFF2EC4B6), RoundedCornerShape(12.dp))
                .clickable { onOpenM3uDialog() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "➕ إضافة قائمة قنوات M3U للشاشة",
                style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (channels.isEmpty()) {
            BasicText(
                text = "اضغط على زر (بث 📺) لفتح قنوات التلفزيون أو أضف قائمة M3U",
                style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            )
        } else {
            BasicText(
                text = "قنوات التلفزيون الحية (${channels.size} قناة) — اضغط للتشغيل:",
                style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Start),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            channels.forEach { (id, name) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectChannel(id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "📺 $name",
                            style = TextStyle(color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        )
                        BasicText(
                            text = "تشغيل ⏵",
                            style = TextStyle(color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Tab 4: Apps & Storage
// -------------------------------------------------------------------------------------------------

@Composable
private fun AppsTabContent(
    installedApps: List<Pair<String, String>>,
    onLaunch: (String) -> Unit,
    onSendKey: (String) -> Unit,
    onOpenStorage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Storage Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onOpenStorage() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "🖴 فتح مستعرض فلاشة الراوتر والـ USB على الشاشة",
                style = TextStyle(color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val defaultShortcuts = listOf(
            Triple("📺 القنوات الفضائية والريسيفر", "tv_receiver", "تلفزيون مباشر وساتلايت"),
            Triple("🔴 يوتيوب التلفزيون", "com.google.android.youtube.tv", "YouTube TV"),
            Triple("🎬 نتفليكس", "com.netflix.ninja", "Netflix"),
            Triple("⭐ شاهد VIP", "net.shahid.android", "Shahid"),
            Triple("📻 محطات الراديو الإسلامية", "radio_hub", "بث القرآن الكريم المباشر"),
            Triple("⚙️ إعدادات النظام", "com.android.tv.settings", "إعدادات شاشة التلفزيون")
        )

        defaultShortcuts.forEach { (title, target, desc) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        when (target) {
                            "tv_receiver" -> onSendKey("tv")
                            "radio_hub" -> onSendKey("yellow")
                            else -> onLaunch(target)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    BasicText(
                        text = title,
                        style = TextStyle(color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                    BasicText(
                        text = desc,
                        style = TextStyle(color = TextMuted, fontSize = 11.sp)
                    )
                }
            }
        }

        if (installedApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            BasicText(
                text = "تطبيقات التلفزيون المثبتة (${installedApps.size}):",
                style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Start),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            installedApps.forEach { (pkg, name) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { onLaunch(pkg) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "📱 $name",
                            style = TextStyle(color = TextWhite, fontSize = 13.sp)
                        )
                        BasicText(
                            text = "فتح ↗",
                            style = TextStyle(color = GoldAccent, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Common Reusable Components
// -------------------------------------------------------------------------------------------------

@Composable
private fun GlassIconButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp, 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun RockerControl(
    topLabel: String,
    bottomLabel: String,
    onTopClick: () -> Unit,
    onBottomClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onTopClick() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = topLabel,
                style = TextStyle(color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorder)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onBottomClick() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = bottomLabel,
                style = TextStyle(color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun ColorButton(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp, 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    )
}

@Composable
private fun ManualIpDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var ipInput by remember { mutableStateOf(currentIp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF131A2E))
                .border(1.dp, GoldAccent, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = "إعداد IP التلفزيون يدوياً",
                style = TextStyle(color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicText(
                text = "أدخل عنوان الـ IP الظاهر على شاشة التلفزيون في شريط الحالة أو إعدادات مشكاة:",
                style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090D18))
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    textStyle = TextStyle(color = TextWhite, fontSize = 16.sp, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(GoldAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurface)
                        .clickable { onDismiss() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "إلغاء", style = TextStyle(color = TextWhite, fontSize = 13.sp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldAccent)
                        .clickable { onSave(ipInput.trim()) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "حفظ والاتصال",
                        style = TextStyle(color = BgNight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun M3uImportDialog(
    onDismiss: () -> Unit,
    onSendM3u: (String) -> Unit
) {
    var m3uInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(330.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF131A2E))
                .border(1.dp, Color(0xFF2EC4B6), RoundedCornerShape(20.dp))
                .padding(20.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = "إضافة قائمة قنوات M3U للشاشة",
                style = TextStyle(color = Color(0xFF2EC4B6), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicText(
                text = "الصق رابط قائمة القنوات (URL) أو نص M3U لإرسالها مباشرة لمشغل القنوات في الشاشة:",
                style = TextStyle(color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090D18))
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (m3uInput.isEmpty()) {
                    BasicText(
                        text = "http://example.com/playlist.m3u",
                        style = TextStyle(color = TextMuted.copy(alpha = 0.4f), fontSize = 13.sp)
                    )
                }
                BasicTextField(
                    value = m3uInput,
                    onValueChange = { m3uInput = it },
                    textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                    cursorBrush = SolidColor(GoldAccent),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurface)
                        .clickable { onDismiss() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "إلغاء", style = TextStyle(color = TextWhite, fontSize = 13.sp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2EC4B6))
                        .clickable {
                            if (m3uInput.isNotBlank()) {
                                onSendM3u(m3uInput.trim())
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "إرسال للشاشة ⏎",
                        style = TextStyle(color = BgNight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
