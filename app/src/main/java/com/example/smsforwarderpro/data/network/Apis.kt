package com.example.smsforwarderpro.data.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface TelegramApi {
    @FormUrlEncoded
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Field("chat_id") chatId: String,
        @Field("text") text: String,
        @Field("parse_mode") parseMode: String = "HTML"
    ): Response<ResponseBody>
}

interface WhatsAppApi {
    @POST("v19.0/{phone_number_id}/messages")
    suspend fun sendMessage(
        @Path("phone_number_id") phoneNumberId: String,
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}

interface GmailApi {
    @POST("gmail/v1/users/me/messages/send")
    @Headers("Content-Type: application/json")
    suspend fun sendEmail(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("oauth2/v4/token")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    suspend fun refreshAccessToken(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("refresh_token") refreshToken: String,
        @Query("grant_type") grantType: String = "refresh_token"
    ): Response<ResponseBody>
}

interface WebhookApi {
    @POST
    suspend fun executePost(
        @Url url: String,
        @Header("Authorization") authHeader: String?,
        @Body body: RequestBody
    ): Response<ResponseBody>
}
