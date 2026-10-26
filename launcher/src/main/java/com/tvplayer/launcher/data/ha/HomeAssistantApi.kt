package com.tvplayer.launcher.data.ha

import com.tvplayer.launcher.data.ha.model.HaState
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/** Home Assistant REST API — https://developers.home-assistant.io/docs/api/rest/ */
interface HomeAssistantApi {

    @GET
    suspend fun getStates(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): List<HaState>

    @POST
    suspend fun callService(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    )
}
