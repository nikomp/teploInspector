package ru.bingosoft.mapquestapp2.api

import io.reactivex.Single
import retrofit2.http.*
import ru.bingosoft.mapquestapp2.models.Models

interface ApiService {
    @POST("redis-session-php/login.php")
    @FormUrlEncoded
    fun getAuthorization(
        @Field("fingerprint") fingerprint: String?,
        @Field("login") login: String?,
        @Field("password") password: String?
    ): Single<Models.Auth>

    @GET("procs/androidAPI.php")
    fun getListOrder(
        @Query("action") action: String
    ): Single<Models.OrderList>
}