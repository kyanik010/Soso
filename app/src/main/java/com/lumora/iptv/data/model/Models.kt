package com.lumora.iptv.data.model

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String?,
    val plot: String? = null,
    val duration: String? = null,
    val streamUrl: String = "",
    val containerExtension: String = "mp4"
)

data class Series(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val plot: String?,
    val categoryId: String?,
    val rating: String? = null,
    val genre: String? = null
)

data class Season(
    val number: Int,
    val name: String?,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val title: String?,
    val number: Int,
    val seasonNumber: Int,
    val duration: String?,
    val containerExtension: String = "mp4",
    val thumbnailUrl: String?,
    val streamUrl: String = ""
)

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val categoryId: String?,
    val streamType: String?,
    val streamUrl: String = "",
    val epgChannelId: String? = null
)

data class Category(
    val id: String,
    val name: String,
    val type: String = "live" // "live", "movie", "series"
)

data class Credentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val sourceType: String = "xtream", // "xtream" or "m3u"
    val m3uUrl: String? = null,
    val expiryDate: String? = null
)

data class VodInfo(
    val movieData: Movie,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null
)

data class SeriesInfo(
    val series: Series,
    val seasons: List<Season>
)
