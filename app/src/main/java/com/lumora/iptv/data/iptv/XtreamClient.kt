package com.lumora.iptv.data.iptv

import com.lumora.iptv.data.model.Category
import com.lumora.iptv.data.model.Channel
import com.lumora.iptv.data.model.Movie
import com.lumora.iptv.data.model.Series
import com.lumora.iptv.data.model.XtreamAuthResponse
import com.lumora.iptv.data.model.XtreamCategory
import com.lumora.iptv.data.model.XtreamLiveStream
import com.lumora.iptv.data.model.XtreamSeriesItem
import com.lumora.iptv.data.model.XtreamVodStream
import com.lumora.iptv.player.PlaybackUrlBuilder
import com.lumora.iptv.util.AppLogger
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class XtreamClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun authenticate(baseUrl: String, user: String, pass: String): XtreamAuthResponse = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val url = "$host/player_api.php?username=$user&password=$pass"
        AppLogger.d("XtreamClient", "Authenticating against server")

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: فشل الاتصال بالسيرفر")
            val body = response.body?.string() ?: throw Exception("استجابة فارغة من السيرفر")
            val adapter = moshi.adapter(XtreamAuthResponse::class.java)
            adapter.fromJson(body) ?: throw Exception("فشل في قراءة بيانات الاشتراك")
        }
    }

    suspend fun getLiveCategories(baseUrl: String, user: String, pass: String): List<Category> = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val url = "$host/player_api.php?username=$user&password=$pass&action=get_live_categories"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamCategory::class.java)
            val adapter = moshi.adapter<List<XtreamCategory>>(listType)
            val list = adapter.fromJson(body) ?: emptyList()
            list.map { Category(id = it.categoryId, name = it.categoryName, type = "live") }
        }
    }

    suspend fun getLiveStreams(baseUrl: String, user: String, pass: String, categoryId: String?): List<Channel> = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val catParam = if (!categoryId.isNullOrBlank() && categoryId != "all") "&category_id=$categoryId" else ""
        val url = "$host/player_api.php?username=$user&password=$pass&action=get_live_streams$catParam"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamLiveStream::class.java)
            val adapter = moshi.adapter<List<XtreamLiveStream>>(listType)
            val list = adapter.fromJson(body) ?: emptyList()

            list.mapNotNull { item ->
                val streamId = item.streamId ?: return@mapNotNull null
                val streamUrl = "$host/live/$user/$pass/$streamId.ts"
                Channel(
                    id = streamId.toString(),
                    name = item.name ?: "قناة $streamId",
                    logoUrl = item.streamIcon,
                    categoryId = item.categoryId ?: "all",
                    streamType = item.streamType ?: "live",
                    streamUrl = streamUrl,
                    epgChannelId = item.epgChannelId
                )
            }
        }
    }

    suspend fun getVodCategories(baseUrl: String, user: String, pass: String): List<Category> = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val url = "$host/player_api.php?username=$user&password=$pass&action=get_vod_categories"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamCategory::class.java)
            val adapter = moshi.adapter<List<XtreamCategory>>(listType)
            val list = adapter.fromJson(body) ?: emptyList()
            list.map { Category(id = it.categoryId, name = it.categoryName, type = "movie") }
        }
    }

    suspend fun getVodStreams(baseUrl: String, user: String, pass: String, categoryId: String?): List<Movie> = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val catParam = if (!categoryId.isNullOrBlank() && categoryId != "all") "&category_id=$categoryId" else ""
        val url = "$host/player_api.php?username=$user&password=$pass&action=get_vod_streams$catParam"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamVodStream::class.java)
            val adapter = moshi.adapter<List<XtreamVodStream>>(listType)
            val list = adapter.fromJson(body) ?: emptyList()

            list.mapNotNull { item ->
                val streamId = item.streamId ?: return@mapNotNull null
                val ext = item.containerExtension ?: "mp4"
                val streamUrl = "$host/movie/$user/$pass/$streamId.$ext"
                Movie(
                    id = streamId.toString(),
                    title = item.name ?: "فيلم $streamId",
                    posterUrl = item.streamIcon,
                    categoryId = item.categoryId ?: "all",
                    streamUrl = streamUrl,
                    containerExtension = ext
                )
            }
        }
    }

    suspend fun getSeriesCategories(baseUrl: String, user: String, pass: String): List<Category> = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val url = "$host/player_api.php?username=$user&password=$pass&action=get_series_categories"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamCategory::class.java)
            val adapter = moshi.adapter<List<XtreamCategory>>(listType)
            val list = adapter.fromJson(body) ?: emptyList()
            list.map { Category(id = it.categoryId, name = it.categoryName, type = "series") }
        }
    }

    suspend fun getSeries(baseUrl: String, user: String, pass: String, categoryId: String?): List<Series> = withContext(Dispatchers.IO) {
        val host = PlaybackUrlBuilder.normalizeBaseUrl(baseUrl)
        val catParam = if (!categoryId.isNullOrBlank() && categoryId != "all") "&category_id=$categoryId" else ""
        val url = "$host/player_api.php?username=$user&password=$pass&action=get_series$catParam"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamSeriesItem::class.java)
            val adapter = moshi.adapter<List<XtreamSeriesItem>>(listType)
            val list = adapter.fromJson(body) ?: emptyList()

            list.mapNotNull { item ->
                val seriesId = item.seriesId ?: return@mapNotNull null
                Series(
                    id = seriesId.toString(),
                    title = item.name ?: "مسلسل $seriesId",
                    posterUrl = item.cover,
                    plot = item.plot,
                    categoryId = item.categoryId ?: "all",
                    rating = item.rating?.toString() ?: "0.0",
                    genre = item.genre
                )
            }
        }
    }
}
