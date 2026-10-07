package com.lumora.iptv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username") val username: String? = null,
    @Json(name = "password") val password: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "exp_date") val expDate: String? = null,
    @Json(name = "is_trial") val isTrial: String? = null,
    @Json(name = "active_cons") val activeConnections: String? = null,
    @Json(name = "max_connections") val maxConnections: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "port") val port: String? = null,
    @Json(name = "https_port") val httpsPort: String? = null,
    @Json(name = "server_protocol") val serverProtocol: String? = null,
    @Json(name = "timezone") val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "parent_id") val parentId: Int? = 0
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStream(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_type") val streamType: String? = null,
    @Json(name = "stream_id") val streamId: Int? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @Json(name = "category_id") val categoryId: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamVodStream(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_type") val streamType: String? = null,
    @Json(name = "stream_id") val streamId: Int? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "rating") val rating: Any? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "container_extension") val containerExtension: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesItem(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "series_id") val seriesId: Int? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "cast") val cast: String? = null,
    @Json(name = "director") val director: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "rating") val rating: Any? = null,
    @Json(name = "category_id") val categoryId: String? = null
)
