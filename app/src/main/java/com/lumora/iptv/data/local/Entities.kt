package com.lumora.iptv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val type: String // "live", "movie", "series"
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val streamId: Int,
    val num: Int = 0,
    val name: String,
    val streamIcon: String? = null,
    val categoryId: String = "all",
    val streamUrl: String,
    val epgChannelId: String? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val streamId: Int,
    val name: String,
    val streamIcon: String? = null,
    val rating: String = "0.0",
    val categoryId: String = "all",
    val streamUrl: String,
    val containerExtension: String = "mp4",
    val plot: String? = null,
    val duration: String? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val seriesId: Int,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String = "0.0",
    val categoryId: String = "all",
    val isFavorite: Boolean = false
)

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val episodeId: String,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val containerExtension: String = "mp4",
    val duration: String? = null,
    val thumbnail: String? = null
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = "primary_account",
    val host: String,
    val username: String,
    val serverName: String? = null,
    val expiryDate: String? = null,
    val status: String = "Active",
    val lastSyncTimestamp: Long = 0L
)

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val contentId: String,
    val mediaType: String, // "movie", "series_episode"
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)
