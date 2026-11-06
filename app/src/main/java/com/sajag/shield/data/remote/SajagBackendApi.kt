package com.sajag.shield.data.remote

import com.sajag.shield.domain.model.TurnRequest
import com.sajag.shield.domain.model.TurnResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface SajagBackendApi {

    @POST("api/mobile/turn")
    suspend fun sendTurn(
        @Body request: TurnRequest
    ): TurnResponse

    @Streaming
    @GET("reports/{call_id}/pdf")
    suspend fun downloadReportPdf(
        @Path("call_id") callId: String
    ): Response<ResponseBody>

    @GET("audio/ramesh_voice.mp3")
    @Streaming
    suspend fun downloadDecoyVoice(): Response<ResponseBody>
}
