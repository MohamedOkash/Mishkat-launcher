package com.mishkat.launcher.remote

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.theme.LocalThemeTokens
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass

@Composable
fun CompanionRemoteOverlay(
    tokens: ThemeTokens,
    serverState: CompanionServerState,
    onRefreshIp: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val appSettings = LocalAppSettings.current
    val isArabic = appSettings.isArabic
    val glass = appSettings.glassIntensity
    val closeFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        onRefreshIp()
        repeat(4) {
            withFrameNanos { }
            if (runCatching { closeFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    val qrMatrix = remember(serverState.url) {
        QrCodeEncoder.encodeVersion3L(serverState.url)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.surface.copy(alpha = 0.96f))
            .padding(horizontal = 56.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val cardShape = RoundedCornerShape(tokens.cardRadius)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .liquidGlass(tokens, tokens.card, tokens.cardRadius, glass)
                .padding(horizontal = 32.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.remote_qr_title),
                        color = tokens.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.remote_qr_subtitle),
                        color = tokens.onSurfaceMuted,
                        fontSize = 13.sp,
                    )
                }

                val pillShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .focusRequester(closeFocus)
                        .mishkatFocus(id = "remote_qr:close", radius = 14.dp)
                        .clip(pillShape)
                        .liquidGlass(tokens, tokens.cardFocused, 14.dp, glass)
                        .clickable { onDismiss() }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.radio_close),
                        color = tokens.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Scannable QR Code Box
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(BorderStroke(3.dp, tokens.primary), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val n = qrMatrix.size
                        val cell = size.width / n
                        for (r in 0 until n) {
                            for (c in 0 until n) {
                                if (qrMatrix[r][c]) {
                                    drawRect(
                                        color = Color(0xFF08101E),
                                        topLeft = Offset(c * cell, r * cell),
                                        size = Size(cell + 0.6f, cell + 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }

                // Instructions & URL Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val urlShape = RoundedCornerShape(14.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(urlShape)
                            .background(tokens.surface.copy(alpha = 0.65f))
                            .border(BorderStroke(1.dp, tokens.primary), urlShape)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (isArabic) {
                                "امسح الكود بكاميرا الموبايل أو افتح الرابط:"
                            } else {
                                "Scan QR with your phone camera or open URL:"
                            },
                            color = tokens.onSurfaceMuted,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = serverState.url,
                            color = tokens.primary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    FeatureBullet(
                        "📱",
                        if (isArabic) {
                            "يعمل فوراً على أي موبايل (أندرويد أو آيفون) متصل بنفس الواي فاي بدون تعقيد"
                        } else {
                            "Works instantly on any Android or iPhone on the same Wi-Fi"
                        },
                    )
                    FeatureBullet(
                        "📻",
                        if (isArabic) {
                            "تغيير إذاعة القرآن الكريم والقراء، وتبديل الثيمات والخلفيات الحية بلمسة واحدة"
                        } else {
                            "Switch Quran Radio stations, themes, and 4-seasons live wallpapers with one tap"
                        },
                    )
                    FeatureBullet(
                        "📡",
                        if (isArabic) {
                            "فتح الريسيفر الداخلي، مركز المباريات، أو قفل الشاشة الأبوي عن بُعد"
                        } else {
                            "Launch Satellite Receiver, Match Center, or trigger Parental Lock remotely"
                        },
                    )

                    Text(
                        text = if (isArabic) {
                            "حالة الخادم المحلي: ${if (serverState.running) "نشط ومتصل ✓" else "جارٍ التشغيل..."} • الأوامر المستقبلة: ${serverState.connectedClientsCount}"
                        } else {
                            "Local Server: ${if (serverState.running) "Active ✓" else "Starting..."} • Commands received: ${serverState.connectedClientsCount}"
                        },
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBullet(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = text,
            color = LocalThemeTokens.current.onSurface,
            fontSize = 13.sp,
        )
    }
}

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code Version 3-L (29x29 modules) encoder.
 * Encodes up to 53 ASCII/UTF-8 bytes in Byte Mode with 15 Reed-Solomon ECC codewords.
 */
object QrCodeEncoder {
    private const val SIZE = 29
    private const val DATA_CODEWORDS = 55
    private const val ECC_CODEWORDS = 15

    fun encodeVersion3L(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8).take(53).toByteArray()
        val dataCw = buildDataCodewords(bytes)
        val eccCw = computeReedSolomon(dataCw, ECC_CODEWORDS)
        val allCodewords = IntArray(DATA_CODEWORDS + ECC_CODEWORDS)
        System.arraycopy(dataCw, 0, allCodewords, 0, dataCw.size)
        System.arraycopy(eccCw, 0, allCodewords, dataCw.size, eccCw.size)

        val matrix = Array(SIZE) { BooleanArray(SIZE) }
        val reserved = Array(SIZE) { BooleanArray(SIZE) }

        placeFinderPattern(matrix, reserved, 0, 0)
        placeFinderPattern(matrix, reserved, 0, SIZE - 7)
        placeFinderPattern(matrix, reserved, SIZE - 7, 0)
        placeAlignmentPattern(matrix, reserved, 22, 22)
        placeTimingPatterns(matrix, reserved)

        // Reserve format info areas + dark module
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][SIZE - 1 - i] = true
            reserved[SIZE - 1 - i][8] = true
        }
        matrix[SIZE - 8][8] = true
        reserved[SIZE - 8][8] = true

        // Place data bits upward/downward in 2-column zig-zags
        var bitIdx = 0
        val totalBits = allCodewords.size * 8
        var upward = true
        var col = SIZE - 1
        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing line
            for (step in 0 until SIZE) {
                val row = if (upward) (SIZE - 1 - step) else step
                for (cOffset in 0..1) {
                    val c = col - cOffset
                    if (!reserved[row][c]) {
                        val bit = if (bitIdx < totalBits) {
                            ((allCodewords[bitIdx ushr 3] ushr (7 - (bitIdx and 7))) and 1) == 1
                        } else {
                            false
                        }
                        // Apply Mask 0: (row + c) % 2 == 0
                        val masked = if ((row + c) % 2 == 0) !bit else bit
                        matrix[row][c] = masked
                        bitIdx++
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Write Format Information for Level L (01) + Mask 0 (000) -> 0x77C4 (15 bits)
        writeFormatBits(matrix, 0x77C4)
        return matrix
    }

    private fun buildDataCodewords(bytes: ByteArray): IntArray {
        val bits = ArrayList<Int>(DATA_CODEWORDS * 8)
        fun pushBits(value: Int, count: Int) {
            for (i in (count - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        // Byte mode indicator (0100) + 8-bit length
        pushBits(0b0100, 4)
        pushBits(bytes.size, 8)
        for (b in bytes) {
            pushBits(b.toInt() and 0xFF, 8)
        }
        // Terminator up to 4 zero bits
        repeat(minOf(4, DATA_CODEWORDS * 8 - bits.size)) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        val result = IntArray(DATA_CODEWORDS)
        val byteLen = bits.size / 8
        for (i in 0 until byteLen) {
            var v = 0
            for (b in 0..7) v = (v shl 1) or bits[i * 8 + b]
            result[i] = v
        }
        val padBytes = intArrayOf(0xEC, 0x11)
        for (i in byteLen until DATA_CODEWORDS) {
            result[i] = padBytes[(i - byteLen) % 2]
        }
        return result
    }

    private fun computeReedSolomon(data: IntArray, eccCount: Int): IntArray {
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) exp[i] = exp[i - 255]

        fun gfMul(a: Int, b: Int): Int = if (a == 0 || b == 0) 0 else exp[log[a] + log[b]]

        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val next = IntArray(gen.size + 1)
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], exp[i])
            }
            gen = next
        }

        val rem = IntArray(eccCount)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, eccCount - 1)
            rem[eccCount - 1] = 0
            for (i in 0 until eccCount) {
                rem[i] = rem[i] xor gfMul(gen[i + 1], factor)
            }
        }
        return rem
    }

    private fun placeFinderPattern(m: Array<BooleanArray>, r: Array<BooleanArray>, row: Int, col: Int) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val rr = row + dr
                val cc = col + dc
                if (rr in 0 until SIZE && cc in 0 until SIZE) {
                    val dark = (dr in 0..6 && (dc == 0 || dc == 6)) ||
                        (dc in 0..6 && (dr == 0 || dr == 6)) ||
                        (dr in 2..4 && dc in 2..4)
                    m[rr][cc] = dark
                    r[rr][cc] = true
                }
            }
        }
    }

    private fun placeAlignmentPattern(m: Array<BooleanArray>, r: Array<BooleanArray>, centerR: Int, centerC: Int) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val rr = centerR + dr
                val cc = centerC + dc
                val dark = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
                m[rr][cc] = dark
                r[rr][cc] = true
            }
        }
    }

    private fun placeTimingPatterns(m: Array<BooleanArray>, r: Array<BooleanArray>) {
        for (i in 8 until (SIZE - 8)) {
            val dark = (i % 2 == 0)
            if (!r[6][i]) {
                m[6][i] = dark
                r[6][i] = true
            }
            if (!r[i][6]) {
                m[i][6] = dark
                r[i][6] = true
            }
        }
    }

    private fun writeFormatBits(m: Array<BooleanArray>, formatBits: Int) {
        for (i in 0..5) m[8][i] = ((formatBits ushr (14 - i)) and 1) == 1
        m[8][7] = ((formatBits ushr 8) and 1) == 1
        m[8][8] = ((formatBits ushr 7) and 1) == 1
        m[7][8] = ((formatBits ushr 6) and 1) == 1
        for (i in 0..5) m[5 - i][8] = ((formatBits ushr (5 - i)) and 1) == 1

        for (i in 0..6) m[SIZE - 1 - i][8] = ((formatBits ushr (14 - i)) and 1) == 1
        for (i in 0..7) m[8][SIZE - 8 + i] = ((formatBits ushr (7 - i)) and 1) == 1
    }
}
