package com.lumora.iptv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {

    // Categories
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY categoryName ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // Channels
    @Query("SELECT * FROM channels ORDER BY num ASC, name ASC")
    fun getAllLiveChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE categoryId = :categoryId ORDER BY num ASC, name ASC")
    fun getChannelsByCategory(categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFav WHERE streamId = :streamId")
    suspend fun setChannelFavorite(streamId: Int, isFav: Boolean)

    @Query("SELECT * FROM channels WHERE streamId = :streamId LIMIT 1")
    suspend fun getChannelById(streamId: Int): ChannelEntity?

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    // Movies
    @Query("SELECT * FROM movies ORDER BY streamId DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE categoryId = :categoryId ORDER BY streamId DESC")
    fun getMoviesByCategory(categoryId: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies LIMIT :limit OFFSET :offset")
    suspend fun getMoviesPaged(limit: Int, offset: Int): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE streamId = :streamId LIMIT 1")
    suspend fun getMovieById(streamId: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("UPDATE movies SET isFavorite = :isFav WHERE streamId = :streamId")
    suspend fun setMovieFavorite(streamId: Int, isFav: Boolean)

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    // Series
    @Query("SELECT * FROM series ORDER BY seriesId DESC")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE categoryId = :categoryId ORDER BY seriesId DESC")
    fun getSeriesByCategory(categoryId: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE seriesId = :seriesId LIMIT 1")
    suspend fun getSeriesById(seriesId: Int): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    // Episodes
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC")
    fun getEpisodes(seriesId: Int, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getEpisodeById(episodeId: String): EpisodeEntity?

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun clearEpisodesForSeries(seriesId: Int)

    @Transaction
    suspend fun replaceXtreamContent(
        categories: List<CategoryEntity>,
        channels: List<ChannelEntity>,
        movies: List<MovieEntity>,
        series: List<SeriesEntity>
    ) {
        clearChannels()
        clearMovies()
        clearSeries()
        clearCategories()
        insertCategories(categories)
        insertChannels(channels)
        insertMovies(movies)
        insertSeries(series)
    }

    // Accounts
    @Query("SELECT * FROM accounts WHERE id = 'primary_account' LIMIT 1")
    fun getAccountFlow(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = 'primary_account' LIMIT 1")
    suspend fun getAccount(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    // Watch Progress
    @Query("SELECT * FROM watch_progress ORDER BY updatedAt DESC LIMIT 20")
    fun getContinueWatching(): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWatchProgress(progress: WatchProgressEntity)

    // Search
    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' LIMIT 50")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM movies WHERE name LIKE '%' || :query || '%' LIMIT 50")
    fun searchMovies(query: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM series WHERE name LIKE '%' || :query || '%' LIMIT 50")
    fun searchSeries(query: String): Flow<List<SeriesEntity>>
}
