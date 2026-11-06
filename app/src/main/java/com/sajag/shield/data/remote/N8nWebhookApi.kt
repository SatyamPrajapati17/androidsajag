package com.sajag.shield.data.remote

import com.sajag.shield.domain.model.N8nEventPayload
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface N8nWebhookApi {

    @POST
    suspend fun sendEvent(
        @Url url: String,
        @Header("X-Sajag-Secret") secret: String,
        @Body payload: N8nEventPayload
    ): Response<ResponseBody>
}
