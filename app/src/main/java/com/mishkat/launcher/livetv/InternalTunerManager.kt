package com.mishkat.launcher.livetv

import android.content.ContentUris
import android.content.Context
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import com.mishkat.launcher.core.Logger

/**
 * Level 3: Internal Tuner Manager for Smart TVs with built-in Satellite / Terrestrial Tuners
 * (DVB-S2 / DVB-T / DTV on MediaTek, MStar, Realtek, Amlogic, TCL, Toshiba, Tornado, Skyworth).
 * Safe querying with fallback to passthrough tuning if READ_EPG_DATA is gated by system restrictions.
 */
object InternalTunerManager {

    private val PROJECTION = arrayOf(
        TvContract.Channels._ID,
        TvContract.Channels.COLUMN_DISPLAY_NAME,
        TvContract.Channels.COLUMN_DISPLAY_NUMBER,
        TvContract.Channels.COLUMN_INPUT_ID,
        TvContract.Channels.COLUMN_TYPE,
    )

    fun queryTunerChannels(context: Context): List<LiveChannel> {
        val list = mutableListOf<LiveChannel>()
        try {
            val cursor = context.contentResolver.query(
                TvContract.Channels.CONTENT_URI,
                PROJECTION,
                null,
                null,
                "${TvContract.Channels.COLUMN_DISPLAY_NUMBER} ASC",
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(TvContract.Channels._ID)
                val nameIdx = c.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)
                val numIdx = c.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)
                val inputIdx = c.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID)

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getLong(idIdx) else -1L
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "قناة فضائية" else "قناة فضائية"
                    val number = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
                    val inputId = if (inputIdx >= 0) c.getString(inputIdx) ?: "" else ""

                    if (id >= 0) {
                        val channelUri = ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, id).toString()
                        list.add(
                            LiveChannel(
                                id = "tuner_$id",
                                nameAr = if (number.isNotBlank()) "$number. $name" else name,
                                nameEn = if (number.isNotBlank()) "$number. $name" else name,
                                category = "egypt_general",
                                subtitleAr = "الريسيفر الداخلي للشاشة (DVB-S2)",
                                streamUrl = "",
                                glyph = "📡",
                                badge = "DVB",
                                satellite = "internal_tuner",
                                sourceType = "INTERNAL_TUNER",
                                tunerChannelUri = channelUri,
                                inputId = inputId,
                            )
                        )
                    }
                }
            }
            Logger.system { "Discovered ${list.size} internal tuner channels via TvContract" }
        } catch (e: SecurityException) {
            Logger.w(Logger.TAG_SYSTEM, e) { "Tuner channels query restricted by system security, activating passthrough mode" }
        } catch (e: Exception) {
            Logger.w(Logger.TAG_SYSTEM, e) { "Failed to query TvContract channels" }
        }

        // If no channels returned (or query was restricted), generate fallback passthrough tuner entries
        if (list.isEmpty()) {
            val tvInputManager = runCatching { context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager }.getOrNull()
            val tunerInputs = tvInputManager?.tvInputList?.filter {
                it.type == TvInputInfo.TYPE_TUNER || it.id.contains("tun", ignoreCase = true)
            }.orEmpty()

            if (tunerInputs.isNotEmpty()) {
                for (input in tunerInputs) {
                    val passthroughUri = runCatching {
                        TvContract.buildChannelUriForPassthroughInput(input.id).toString()
                    }.getOrNull()

                    list.add(
                        LiveChannel(
                            id = "tuner_pass_${input.id.hashCode()}",
                            nameAr = "بث الريسيفر الفضائي المباشر (DVB-S2)",
                            nameEn = "Live Satellite Receiver Tuner",
                            category = "egypt_general",
                            subtitleAr = "تشغيل مدخل الهوائي والريسيفر المدمج",
                            streamUrl = "",
                            glyph = "📡",
                            badge = "TUNER",
                            satellite = "internal_tuner",
                            sourceType = "INTERNAL_TUNER",
                            tunerChannelUri = passthroughUri,
                            inputId = input.id,
                        )
                    )
                }
            } else {
                // Universal default tuner fallback
                list.add(
                    LiveChannel(
                        id = "tuner_default",
                        nameAr = "الريسيفر الداخلي (نايل سات وعرب سات)",
                        nameEn = "Internal TV Tuner (Live)",
                        category = "egypt_general",
                        subtitleAr = "مدخل الهوائي/الستلايت الداخلي",
                        streamUrl = "",
                        glyph = "📡",
                        badge = "DVB",
                        satellite = "internal_tuner",
                        sourceType = "INTERNAL_TUNER",
                        tunerChannelUri = null,
                        inputId = null,
                    )
                )
            }
        }

        return list
    }
}
