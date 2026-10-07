package com.lumora.iptv.data.iptv

import com.lumora.iptv.data.model.Category
import com.lumora.iptv.data.model.Channel
import com.lumora.iptv.data.model.Movie
import java.io.BufferedReader
import java.io.StringReader

data class M3uParseResult(
    val categories: List<Category>,
    val channels: List<Channel>,
    val movies: List<Movie>
)

object M3uParser {

    private val ATTR_REGEX = Regex("""([a-zA-Z0-9_-]+)=["']?([^"']*)["']?""")

    fun parse(content: String): M3uParseResult {
        val reader = BufferedReader(StringReader(content))
        val channels = mutableListOf<Channel>()
        val movies = mutableListOf<Movie>()
        val categoriesMap = mutableMapOf<String, Category>()

        var currentExtInf: String? = null
        var lineIndex = 0

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachLine

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = line
            } else if (!line.startsWith("#") && currentExtInf != null) {
                lineIndex++
                val streamUrl = line
                val extInf = currentExtInf!!
                currentExtInf = null

                val parsed = parseExtInfLine(extInf, lineIndex)
                val groupTitle = parsed.groupTitle.ifBlank { "عام" }

                // Collect category
                val catId = "cat_${groupTitle.hashCode()}"
                if (!categoriesMap.containsKey(catId)) {
                    val isMovieGroup = isVodContent(groupTitle, streamUrl)
                    categoriesMap[catId] = Category(
                        id = catId,
                        name = groupTitle,
                        type = if (isMovieGroup) "movie" else "live"
                    )
                }

                if (isVodContent(groupTitle, streamUrl)) {
                    val ext = if (streamUrl.contains(".mkv", ignoreCase = true)) "mkv" else "mp4"
                    movies.add(
                        Movie(
                            id = parsed.tvgId.ifBlank { "m3u_movie_$lineIndex" },
                            title = parsed.name,
                            posterUrl = parsed.tvgLogo,
                            categoryId = catId,
                            streamUrl = streamUrl,
                            containerExtension = ext
                        )
                    )
                } else {
                    channels.add(
                        Channel(
                            id = parsed.tvgId.ifBlank { "m3u_ch_$lineIndex" },
                            name = parsed.name,
                            logoUrl = parsed.tvgLogo,
                            categoryId = catId,
                            streamType = "live",
                            streamUrl = streamUrl,
                            epgChannelId = parsed.tvgId.ifBlank { null }
                        )
                    )
                }
            }
        }

        return M3uParseResult(
            categories = categoriesMap.values.toList(),
            channels = channels,
            movies = movies
        )
    }

    private data class ExtInfData(
        val tvgId: String,
        val tvgName: String,
        val tvgLogo: String?,
        val groupTitle: String,
        val name: String
    )

    private fun parseExtInfLine(line: String, index: Int): ExtInfData {
        var tvgId = ""
        var tvgName = ""
        var tvgLogo: String? = null
        var groupTitle = ""

        // Extract metadata before the comma
        val commaIndex = line.lastIndexOf(',')
        val metaPart = if (commaIndex != -1) line.substring(0, commaIndex) else line
        val namePart = if (commaIndex != -1) line.substring(commaIndex + 1).trim() else "قناة $index"

        ATTR_REGEX.findAll(metaPart).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            when (key) {
                "tvg-id" -> tvgId = value
                "tvg-name" -> tvgName = value
                "tvg-logo" -> tvgLogo = value.ifBlank { null }
                "group-title" -> groupTitle = value
            }
        }

        val finalName = if (namePart.isNotBlank()) namePart else if (tvgName.isNotBlank()) tvgName else "قناة $index"
        return ExtInfData(tvgId, tvgName, tvgLogo, groupTitle, finalName)
    }

    private fun isVodContent(groupTitle: String, url: String): Boolean {
        val lower = groupTitle.lowercase()
        return lower.contains("movie") || lower.contains("vod") || lower.contains("أفلام") ||
                lower.contains("cinema") || url.endsWith(".mp4", ignoreCase = true) ||
                url.endsWith(".mkv", ignoreCase = true)
    }
}
