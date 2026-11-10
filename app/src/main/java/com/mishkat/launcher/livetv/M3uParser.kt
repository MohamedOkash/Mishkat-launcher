package com.mishkat.launcher.livetv

import java.io.BufferedReader
import java.io.StringReader

object M3uParser {

    /**
     * Parses standard M3U / M3U8 IPTV playlists into [LiveChannel] list.
     */
    fun parse(content: String, defaultCategory: String = "all"): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()
        val reader = BufferedReader(StringReader(content))
        var currentTitle: String? = null
        var currentCategory: String = defaultCategory
        var channelIndex = 100

        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Extract group-title
                val groupMatch = Regex("""group-title="([^"]+)"""", RegexOption.IGNORE_CASE).find(trimmed)
                val groupTitle = groupMatch?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
                currentCategory = when {
                    "quran" in groupTitle || "islam" in groupTitle || "قرآن" in groupTitle || "دين" in groupTitle -> "islamic_quran"
                    "kid" in groupTitle || "طفل" in groupTitle || "اطفال" in groupTitle || "cartoon" in groupTitle -> "kids_education"
                    "movie" in groupTitle || "cinema" in groupTitle || "افلام" in groupTitle || "سينما" in groupTitle -> "cinema_movies"
                    "drama" in groupTitle || "series" in groupTitle || "مسلسلات" in groupTitle || "دراما" in groupTitle -> "drama_series"
                    "sport" in groupTitle || "رياضة" in groupTitle -> "sports_cooking"
                    "news" in groupTitle || "أخبار" in groupTitle || "اخبار" in groupTitle -> "news_doc"
                    else -> defaultCategory
                }

                // Extract channel title after last comma
                val titlePart = trimmed.substringAfterLast(',', "").trim()
                currentTitle = if (titlePart.isNotBlank()) titlePart else "IPTV Channel $channelIndex"
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("rtmp://")) {
                if (!currentTitle.isNullOrBlank()) {
                    val id = "m3u_${channelIndex++}"
                    channels.add(
                        LiveChannel(
                            id = id,
                            nameAr = currentTitle,
                            nameEn = currentTitle,
                            category = currentCategory,
                            subtitleAr = "قناة IPTV خارجية",
                            streamUrl = trimmed,
                            glyph = "📺",
                            badge = "IPTV",
                            satellite = "all",
                            sourceType = "HLS_STREAM",
                        )
                    )
                }
                currentTitle = null
            }
            line = reader.readLine()
        }
        return channels
    }
}
