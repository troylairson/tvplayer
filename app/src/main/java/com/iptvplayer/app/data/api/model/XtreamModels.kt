package com.iptvplayer.app.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo,
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    val username: String,
    val password: String,
    val status: String,
    @Json(name = "exp_date") val expDate: String?,
    @Json(name = "max_connections") val maxConnections: String,
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    val url: String,
    val port: String,
    val timezone: String,
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "parent_id") val parentId: Int,
)

@JsonClass(generateAdapter = true)
data class XtreamStream(
    @Json(name = "stream_id") val streamId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "stream_icon") val streamIcon: String?,
    @Json(name = "epg_channel_id") val epgChannelId: String?,
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "num") val num: Int?,
    @Json(name = "custom_sid") val customSid: String?,
)

@JsonClass(generateAdapter = true)
data class XtreamShortEpg(
    @Json(name = "epg_listings") val listings: List<XtreamEpgListing>,
)

@JsonClass(generateAdapter = true)
data class XtreamEpgListing(
    @Json(name = "id") val id: String,
    @Json(name = "epg_id") val epgId: String,
    @Json(name = "title") val title: String,
    @Json(name = "lang") val lang: String,
    @Json(name = "start") val start: String,
    @Json(name = "end") val end: String,
    @Json(name = "description") val description: String,
    @Json(name = "channel_id") val channelId: String,
    @Json(name = "start_timestamp") val startTimestamp: Long,
    @Json(name = "stop_timestamp") val stopTimestamp: Long,
)
