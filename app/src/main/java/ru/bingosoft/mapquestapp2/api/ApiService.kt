package ru.bingosoft.mapquestapp2.api

import io.reactivex.Single
import okhttp3.RequestBody
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

    @GET("procs/androidAPI.php")
    fun getCheckups(
        @Query("action") action: String
    ): Single<Models.CheckupList>

    /*@POST("/dashboards/app/backend/native.php")
      @FormUrlEncoded
      Call<Object> doReverseSync(@Header("Cookie") String userCookie, @Field("action") String action,
                                 @Field("jsonData") String jsonData);*/

    @POST("procs/androidAPI.php")
    @Multipart
    fun doReverseSync(
        @Part("action") action: RequestBody?,
        @Part("jsonData") jsonData: RequestBody?
        //@Part file: MultipartBody.Part?,
        //@Part("filemap") filemap: RequestBody?
    ): Single<Models.SimpleMsg>
}