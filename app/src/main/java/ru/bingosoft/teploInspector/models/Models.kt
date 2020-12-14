package ru.bingosoft.teploInspector.models

import android.widget.TextView
import com.google.gson.JsonArray
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.yandex.mapkit.geometry.Point
import ru.bingosoft.teploInspector.db.Orders.Orders

class Models {

    data class LP(
        @SerializedName("login") var login: String = "",
        @SerializedName("password") var password: String = ""
    )

    data class Uuid(
        @SerializedName("uuid") var uuid: String = ""
    )

    data class Token(
        @SerializedName("token") var token: String = "",
        @SerializedName("name") var name: String = ""
    )

    data class User(
        @SerializedName("photo_url") var photoUrl: String = "",
        @SerializedName("fullname") var fullname: String = "",
        @SerializedName("surname") var surname: String = "",
        @SerializedName("name") var nameUser: String = "",
        @SerializedName("fname") var fname: String = ""
    )

    data class Result (
        @SerializedName("id_order") var id_order: Int=0,
        @SerializedName("history_order_state") var history_order_state: String?="",
        @SerializedName("controls") var controls: String=""
    )


    data class Result2 (
        @SerializedName("id_order") var id_order: Int = 0,
        @SerializedName("history_order_state") var history_order_state: JsonArray = JsonArray(),
        @SerializedName("controls") var controls: JsonArray = JsonArray()
    )

    data class ResultOrder (
        @SerializedName("data") var data: JsonArray = JsonArray()
    )

    data class HistoryOrderOnServer (
        @SerializedName("unique_id") var unique_id: Long = 0,
        @SerializedName("idOrder") var idOrder: Long? =null,
        @SerializedName("stateOrder") var stateOrder: String="",
        @SerializedName("dateChange") var dateChange: Long=0L
    )


    data class FileRoute(
        @SerializedName("fileRoute") var fileRoute: String=""
    )

    data class ControlList(
        @Expose @SerializedName("controls") var list: MutableList<TemplateControl> = mutableListOf()
    )

    data class CommonControlList(
        @Expose @SerializedName("common") var list: MutableList<ControlList> = mutableListOf()
    )

    data class TemplateControl (
        @Expose @SerializedName("id_question") var id: Int = 0,
        @Expose @SerializedName("results_id") var results_id: Int = 0,
        @Expose @SerializedName("node") val node: Int? = null,
        @Expose @SerializedName("question_guid") var guid: String = "",
        @Expose @SerializedName("type") var type: String = "",
        @Expose @SerializedName("value") var value: Array<String> = arrayOf(),
        @Expose @SerializedName("question") var question: String="",
        @Expose @SerializedName("hint") var hint: String="",
        @Expose @SerializedName("resvalue") var resvalue: String?=null,
        @Expose @SerializedName("maxrange") var maxRange: Double? = null,
        @Expose @SerializedName("minrange") var minRange: Double? = null,
        @Expose @SerializedName("datetime_question_answered") var datetime: Long=0L,

        @Expose @SerializedName("replication_nodes") var replication_nodes: Boolean?=null,
        @Expose @SerializedName("replicating_archival_records") var replicating_archival_records: Boolean?=null,
        @Expose @SerializedName("group_checklist") val group_checklist: String?=null,
        @Expose @SerializedName("replicated_on") val replicated_on: Int?=null,
        @Expose @SerializedName("node_itp") val node_itp: String?=null,
        @Expose @SerializedName("archival_records") val archival_records: Int?=null,


        @Expose @SerializedName("answered") var answered: Boolean = false,

        var parent: TemplateControl?=null
        //var view: View?=null
    )

    data class CustomMarker(
        val order: Orders,
        val markerView: TextView
    )

    data class StopTransferMarker(
        val name: String,
        val position: Point
    )

    data class DataFile(
        @SerializedName("idOrder") var idOrder: Int=0,
        @SerializedName("id") var id: Int=0,
        @SerializedName("resValue") var dir: String=""
    )

    data class FilesFromServer(
        @SerializedName("guid") var guid: String="",
        @SerializedName("fileName") var fileName: String=""
    )

    data class ReverseData(
        @SerializedName("data") var data: List<Result2> = listOf()
    )

    data class MessageData(
        @SerializedName("text") var text: String="",
        @SerializedName("date") var date: Long=0L,
        @SerializedName("event_type") var event_type: Int=0,
        @SerializedName("lat") var lat: Double?=0.0,
        @SerializedName("lon") var lon: Double?=0.0
    )

    data class FCMToken(
        @SerializedName("fcmToken") var token: String=""
    )

    data class Notification(
        @SerializedName("id") val id: Int=0,
        @SerializedName("notification_id") val notification_id: Int=0,
        @SerializedName("author_id") val author_id: Int=0,
        @SerializedName("recipient_id") val recipient_id: Int=0,
        @SerializedName("guid") val guid: String="",
        @SerializedName("event_guid") val event_guid: String="",
        @SerializedName("record_id") val record_id: Int=0,
        @SerializedName("title") val title: String="",
        @SerializedName("content") val content: String="",
        @SerializedName("shows_popup_message") val shows_popup_message: Boolean=false,
        @SerializedName("email") val email: String?=null,
        @SerializedName("create_date") val create_date: String="",
        @SerializedName("submit_date") val submit_date: String="",
        @SerializedName("read_date") val read_date: String?=""
        //@SerializedName("author") val author: List<Author>?=null,
        //@SerializedName("notification") val notification: List<NoticationData>?=null

    )

    /*data class Author(
        @SerializedName("id") val id: Int=0,
        @SerializedName("name") val name: String="",
        @SerializedName("email") val email: String="",
        @SerializedName("login") val login: String="",
        @SerializedName("midname") val midname: String="",
        @SerializedName("surname") val surname: String="",
        @SerializedName("avatar_id") val avatar_id: String?=null
    )

    data class NoticationData(
        @SerializedName("id") val id: Int=0,
        @SerializedName("guid") val guid: String="",
        @SerializedName("icon") val icon: String="",
        @SerializedName("name") val name: String="",
        @SerializedName("group_id") val group_id: Int?=null,
        @SerializedName("author_id") val author_id: Int=0,
        @SerializedName("object_id") val object_id: Int?=null,
        @SerializedName("row_order") val row_order: Int=0,
        @SerializedName("email_type") val email_type: String?=null,
        @SerializedName("author_type") val author_type: String="",
        @SerializedName("email_value") val email_value: String?=null,
        @SerializedName("condition_type") val condition_type: Int?=null,
        @SerializedName("email_field_id") val email_field_id: Int?=null,
        @SerializedName("period_type_id") val period_type_id: Int?=null,
        @SerializedName("recipient_type") val recipient_type: String?=null,
        @SerializedName("author_field_id") val author_field_id: Int?=null,
        @SerializedName("author_state_id") val author_state_id: Int?=null,
        @SerializedName("interaction_type") val interaction_type: String?="",
        @SerializedName("title_formula_id") val title_formula_id: Int=0,
        @SerializedName("content_formula_id") val content_formula_id: Int=0,
        @SerializedName("shows_popup_message") val shows_popup_message: Boolean=false,
        @SerializedName("condition_match_type") val condition_match_type: String="",
        @SerializedName("recipient_match_type") val recipient_match_type: String?=null

    )*/

    data class MessageId(
        @SerializedName("message_id") val id: Int=0
    )

}