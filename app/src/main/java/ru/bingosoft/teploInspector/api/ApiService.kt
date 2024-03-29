package ru.bingosoft.teploInspector.api

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.models.Models

interface ApiService {
    @POST
    @Headers("Content-Type: application/json")
    fun getAuthentication(
        @Url url: String,
        @Body lp: RequestBody
    ): Single<Models.Uuid>

    @POST("/accesseditor/login/authorize")
    @Headers("Content-Type: application/json")
    fun getAuthorization(
        @Body uuid: RequestBody
    ): Single<Models.Token>

    @GET("registryservice/plugins/execute/GetRequestDataQuery")
    fun getListOrder():Single<List<Orders>>

    @GET("datawarehouseservice/query/23")
    fun getTechParams(
        @Query("user_id") user: Int
    ):Single<List<TechParams>>

    @GET("datawarehouseservice/query/30")
    fun getAddLoad(
        @Query("user_id") user: Int
    ):Single<List<AddLoad>>

    @GET("registryservice/plugins/execute/ReceiveCheckListQuery")
    fun getCheckups():Single<List<Checkup>>

    @POST("registryservice/attachments")
    @Multipart
    fun sendFiles(
        @Part("record_id") record_id: Int,
        @Part("registry_id") registry_id: Int,
        @Part files: List<MultipartBody.Part>
    ): Single<List<Models.FilesFromServer>>


    @POST("registryservice/plugins/execute/SaveApplicationHistoryStatusCommand")
    @Headers("Content-Type: application/json")
    fun sendStatusOrder(
        @Body json: RequestBody
    ): Single<Unit>


    @POST("registryservice/plugins/execute/SaveAdministratorMessageCommand")
    @Headers("Content-Type: application/json")
    fun sendMessageToAdmin(
        @Body json: RequestBody
    ): Single<Unit>

    @POST("registryservice/plugins/execute/SaveApplicationBaseInformationCommand")
    @Headers("Content-Type: application/json")
    fun sendGiOrder(
        @Body json: RequestBody
    ): Single<Unit>

    @POST("registryservice/plugins/execute/SaveSurveyResultCommand")
    @Headers("Content-Type: application/json")
    fun doReverseSync(
        @Body json: RequestBody
    ): Single<List<Models.DataFile>>?

    @POST("registryservice/plugins/execute/SaveSurveyResultCommand")
    @Headers("Content-Type: application/json")
    fun doReverseSyncOrder(
        @Body json: RequestBody
    ): Single<List<Models.DataFile>>?


    @POST("registryservice/plugins/execute/SaveInspectorRouteCommand")
    @Headers("Content-Type: application/json")
    fun sendTrackingUserLocation(
        @Body json: RequestBody
    ): Single<Unit>

    @POST("registryservice/plugins/execute/SaveFCMTokenForUserCommand")
    @Headers("Content-Type: application/json")
    fun saveGCMToken(
        @Body json: RequestBody
    ): Single<Unit>

    @PUT("notificationservice/messages/read?")
    @Headers("Content-Type: application/json")
    fun markMessageAsRead(
        @Body json: RequestBody
    ): Single<Boolean>

    @GET("notificationservice/messages")
    fun getAllMessages():Single<List<Models.Notification>>

    @PUT("notificationservice/messages/read-all")
    fun markAllMessageAsRead(): Single<Boolean>

}