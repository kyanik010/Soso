package com.lumora.iptv.data.iptv

import android.content.Context
import com.lumora.iptv.data.local.AccountEntity
import com.lumora.iptv.data.local.AppDatabase
import com.lumora.iptv.data.local.CategoryEntity
import com.lumora.iptv.data.local.ChannelEntity
import com.lumora.iptv.data.local.EpisodeEntity
import com.lumora.iptv.data.local.MovieEntity
import com.lumora.iptv.data.local.SeriesEntity
import com.lumora.iptv.data.local.WatchProgressEntity
import com.lumora.iptv.data.model.Category
import com.lumora.iptv.data.model.Channel
import com.lumora.iptv.data.model.Credentials
import com.lumora.iptv.data.model.Movie
import com.lumora.iptv.data.model.Series
import com.lumora.iptv.data.secure.KeystoreCredentialsStore
import com.lumora.iptv.data.secure.SecureCredentialsStore
import com.lumora.iptv.player.PlaybackSource
import com.lumora.iptv.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SyncState(
    val isSyncing: Boolean = false,
    val message: String = "",
    val progressPercent: Int = 0,
    val error: String? = null
)

class IptvRepository(
    context: Context,
    val secureStore: SecureCredentialsStore = KeystoreCredentialsStore(context),
    private val db: AppDatabase = AppDatabase.getInstance(context),
    private val xtreamClient: XtreamClient = XtreamClient()
) {
    private val dao = db.iptvDao()

    // AppNavHost and PlayerScreen receive the same repository instance from MainActivity.
    private val playbackSources = mutableMapOf<String, PlaybackSource>()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState = _syncState.asStateFlow()

    val accountFlow: Flow<AccountEntity?> = dao.getAccountFlow()
    val continueWatchingFlow: Flow<List<WatchProgressEntity>> = dao.getContinueWatching()

    suspend fun getAccount(): AccountEntity? = withContext(Dispatchers.IO) {
        dao.getAccount()
    }

    suspend fun getCredentials(): Credentials? = secureStore.loadCredentials()

    suspend fun savePlaybackSource(
        sourceId: String,
        source: PlaybackSource,
        m3uUrl: String
    ) {
        val credentials = getCredentials()
        playbackSources[sourceId] = if (
            credentials?.sourceType == "m3u" && m3uUrl.isNotBlank()
        ) {
            PlaybackSource.M3uUrl(m3uUrl)
        } else {
            source
        }
    }

    suspend fun getPlaybackSource(sourceId: String): PlaybackSource? {
        playbackSources[sourceId]?.let { return it }

        val separator = sourceId.indexOf('_')
        if (separator <= 0 || separator == sourceId.lastIndex) return null

        val mediaType = sourceId.substring(0, separator)
        val id = sourceId.substring(separator + 1)
        val credentials = getCredentials()

        val restored = withContext(Dispatchers.IO) {
            when (mediaType) {
                "live" -> id.toIntOrNull()?.let { streamId ->
                    dao.getChannelById(streamId)?.let {
                        if (credentials?.sourceType == "m3u") {
                            PlaybackSource.M3uUrl(it.streamUrl)
                        } else {
                            PlaybackSource.Live(streamId = it.streamId, container = "ts")
                        }
                    }
                }
                "vod" -> id.toIntOrNull()?.let { streamId ->
                    dao.getMovieById(streamId)?.let {
                        if (credentials?.sourceType == "m3u") {
                            PlaybackSource.M3uUrl(it.streamUrl)
                        } else {
                            PlaybackSource.Vod(
                                streamId = it.streamId,
                                container = it.containerExtension
                            )
                        }
                    }
                }
                "episode" -> dao.getEpisodeById(id)?.let {
                    PlaybackSource.Episode(
                        episodeId = it.episodeId,
                        container = it.containerExtension
                    )
                }
                else -> null
            }
        }

        if (restored != null) {
            playbackSources[sourceId] = restored
        }
        return restored
    }

    fun getLiveCategories(): Flow<List<Category>> =
        dao.getCategoriesByType("live").map { list -> list.map { Category(it.categoryId, it.categoryName, "live") } }

    fun getMovieCategories(): Flow<List<Category>> =
        dao.getCategoriesByType("movie").map { list -> list.map { Category(it.categoryId, it.categoryName, "movie") } }

    fun getSeriesCategories(): Flow<List<Category>> =
        dao.getCategoriesByType("series").map { list -> list.map { Category(it.categoryId, it.categoryName, "series") } }

    fun getAllLiveChannels(): Flow<List<Channel>> =
        dao.getAllLiveChannels().map { list -> list.map { it.toChannel() } }

    fun getChannelsByCategory(catId: String): Flow<List<Channel>> =
        dao.getChannelsByCategory(catId).map { list -> list.map { it.toChannel() } }

    fun getAllMovies(): Flow<List<Movie>> =
        dao.getAllMovies().map { list -> list.map { it.toMovie() } }

    fun getMoviesByCategory(catId: String): Flow<List<Movie>> =
        dao.getMoviesByCategory(catId).map { list -> list.map { it.toMovie() } }

    suspend fun getMoviesPaged(limit: Int, offset: Int): List<Movie> = withContext(Dispatchers.IO) {
        dao.getMoviesPaged(limit, offset).map { it.toMovie() }
    }

    suspend fun getMovieById(streamId: Int): Movie? = withContext(Dispatchers.IO) {
        dao.getMovieById(streamId)?.toMovie()
    }

    fun getAllSeries(): Flow<List<Series>> =
        dao.getAllSeries().map { list -> list.map { it.toSeries() } }

    fun getSeriesByCategory(catId: String): Flow<List<Series>> =
        dao.getSeriesByCategory(catId).map { list -> list.map { it.toSeries() } }

    suspend fun getSeriesById(seriesId: Int): Series? = withContext(Dispatchers.IO) {
        dao.getSeriesById(seriesId)?.toSeries()
    }

    fun getEpisodes(seriesId: Int, seasonNumber: Int): Flow<List<com.lumora.iptv.data.model.Episode>> =
        dao.getEpisodes(seriesId, seasonNumber).map { list ->
            list.map {
                com.lumora.iptv.data.model.Episode(
                    id = it.episodeId,
                    title = it.title,
                    number = it.episodeNumber,
                    seasonNumber = it.seasonNumber,
                    duration = it.duration,
                    containerExtension = it.containerExtension,
                    thumbnailUrl = it.thumbnail,
                    streamUrl = it.streamUrl
                )
            }
        }

    suspend fun saveWatchProgress(contentId: String, mediaType: String, positionMs: Long, durationMs: Long) =
        withContext(Dispatchers.IO) {
            dao.saveWatchProgress(
                WatchProgressEntity(
                    contentId = contentId,
                    mediaType = mediaType,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    suspend fun searchChannels(query: String): Flow<List<Channel>> =
        dao.searchChannels(query).map { list -> list.map { it.toChannel() } }

    suspend fun searchMovies(query: String): Flow<List<Movie>> =
        dao.searchMovies(query).map { list -> list.map { it.toMovie() } }

    suspend fun syncXtream(serverUrl: String, user: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState(isSyncing = true, message = "جاري التحقق من بيانات الاشتراك...", progressPercent = 10)
            val auth = xtreamClient.authenticate(serverUrl, user, pass)

            // Save encrypted credentials
            val expiry = auth.userInfo?.expDate ?: "غير محدد"
            secureStore.saveCredentials(
                Credentials(
                    serverUrl = serverUrl,
                    username = user,
                    password = pass,
                    sourceType = "xtream",
                    expiryDate = expiry
                )
            )

            // Save Account
            dao.insertAccount(
                AccountEntity(
                    host = serverUrl,
                    username = user,
                    serverName = auth.serverInfo?.url ?: "Xtream Server",
                    expiryDate = expiry,
                    status = auth.userInfo?.status ?: "Active",
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            )

            // 1. Categories
            _syncState.value = SyncState(isSyncing = true, message = "جاري تحميل التصنيفات...", progressPercent = 30)
            val liveCats = xtreamClient.getLiveCategories(serverUrl, user, pass)
            val vodCats = xtreamClient.getVodCategories(serverUrl, user, pass)
            val seriesCats = xtreamClient.getSeriesCategories(serverUrl, user, pass)

            val catEntities = (liveCats + vodCats + seriesCats).map {
                CategoryEntity(id = "${it.type}_${it.id}", categoryId = it.id, categoryName = it.name, type = it.type)
            }
            dao.insertCategories(catEntities)

            // 2. Channels
            _syncState.value = SyncState(isSyncing = true, message = "جاري مزامنة القنوات...", progressPercent = 50)
            val channels = xtreamClient.getLiveStreams(serverUrl, user, pass, null)
            val channelEntities = channels.mapIndexed { index, ch ->
                ChannelEntity(
                    streamId = ch.id.toIntOrNull() ?: (index + 1),
                    num = index + 1,
                    name = ch.name,
                    streamIcon = ch.logoUrl,
                    categoryId = ch.categoryId ?: "all",
                    streamUrl = ch.streamUrl,
                    epgChannelId = ch.epgChannelId
                )
            }
            dao.insertChannels(channelEntities)

            // 3. Movies
            _syncState.value = SyncState(isSyncing = true, message = "جاري مزامنة الأفلام...", progressPercent = 75)
            val movies = xtreamClient.getVodStreams(serverUrl, user, pass, null)
            val movieEntities = movies.map { m ->
                MovieEntity(
                    streamId = m.id.toIntOrNull() ?: 0,
                    name = m.title,
                    streamIcon = m.posterUrl,
                    categoryId = m.categoryId ?: "all",
                    streamUrl = m.streamUrl,
                    containerExtension = m.containerExtension
                )
            }
            dao.insertMovies(movieEntities)

            // 4. Series
            _syncState.value = SyncState(isSyncing = true, message = "جاري مزامنة المسلسلات...", progressPercent = 90)
            val seriesList = xtreamClient.getSeries(serverUrl, user, pass, null)
            val seriesEntities = seriesList.map { s ->
                SeriesEntity(
                    seriesId = s.id.toIntOrNull() ?: 0,
                    name = s.title,
                    cover = s.posterUrl,
                    plot = s.plot,
                    genre = s.genre,
                    rating = s.rating ?: "0.0",
                    categoryId = s.categoryId ?: "all"
                )
            }
            dao.insertSeries(seriesEntities)

            _syncState.value = SyncState(isSyncing = false, message = "اكتملت المزامنة بنجاح!", progressPercent = 100)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("IptvRepo", "Failed syncXtream", e)
            _syncState.value = SyncState(isSyncing = false, message = "فشل: ${e.localizedMessage}", error = e.localizedMessage)
            Result.failure(e)
        }
    }

    suspend fun syncM3u(m3uUrlOrContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState(isSyncing = true, message = "جاري قراءة ملف M3U...", progressPercent = 20)
            val content = if (m3uUrlOrContent.startsWith("http://") || m3uUrlOrContent.startsWith("https://")) {
                val client = OkHttpClient()
                val req = Request.Builder().url(m3uUrlOrContent).build()
                client.newCall(req).execute().use { resp ->
                    resp.body?.string() ?: throw Exception("ملف M3U فارغ")
                }
            } else {
                m3uUrlOrContent
            }

            _syncState.value = SyncState(isSyncing = true, message = "جاري تحليل القنوات...", progressPercent = 50)
            val result = M3uParser.parse(content)

            // Save Account
            dao.insertAccount(
                AccountEntity(
                    host = m3uUrlOrContent.take(50),
                    username = "M3U User",
                    serverName = "M3U Playlist",
                    expiryDate = "غير محدد",
                    status = "Active",
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            )

            dao.insertCategories(result.categories.map {
                CategoryEntity(id = it.id, categoryId = it.id, categoryName = it.name, type = it.type)
            })

            dao.insertChannels(result.channels.mapIndexed { idx, ch ->
                ChannelEntity(
                    streamId = ch.id.hashCode(),
                    num = idx + 1,
                    name = ch.name,
                    streamIcon = ch.logoUrl,
                    categoryId = ch.categoryId ?: "all",
                    streamUrl = ch.streamUrl,
                    epgChannelId = ch.epgChannelId
                )
            })

            dao.insertMovies(result.movies.map { m ->
                MovieEntity(
                    streamId = m.id.hashCode(),
                    name = m.title,
                    streamIcon = m.posterUrl,
                    categoryId = m.categoryId ?: "all",
                    streamUrl = m.streamUrl,
                    containerExtension = m.containerExtension
                )
            })

            _syncState.value = SyncState(isSyncing = false, message = "تم تحميل M3U بنجاح!", progressPercent = 100)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("IptvRepo", "Failed syncM3u", e)
            _syncState.value = SyncState(isSyncing = false, message = "فشل: ${e.localizedMessage}", error = e.localizedMessage)
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        secureStore.clearCredentials()
        dao.clearAccounts()
        dao.clearCategories()
        dao.clearChannels()
        dao.clearMovies()
        dao.clearSeries()
        AppLogger.i("IptvRepo", "Logged out and cleaned session data completely")
    }

    private fun ChannelEntity.toChannel() = Channel(
        id = streamId.toString(),
        name = name,
        logoUrl = streamIcon,
        categoryId = categoryId,
        streamType = "live",
        streamUrl = streamUrl,
        epgChannelId = epgChannelId
    )

    private fun MovieEntity.toMovie() = Movie(
        id = streamId.toString(),
        title = name,
        posterUrl = streamIcon,
        categoryId = categoryId,
        plot = plot,
        duration = duration,
        streamUrl = streamUrl,
        containerExtension = containerExtension
    )

    private fun SeriesEntity.toSeries() = Series(
        id = seriesId.toString(),
        title = name,
        posterUrl = cover,
        plot = plot,
        categoryId = categoryId,
        rating = rating,
        genre = genre
    )
}
