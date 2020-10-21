package ru.bingosoft.teploInspector.models

import android.widget.TextView
import com.google.gson.JsonArray
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.db.Orders.Orders

class Models {
    class SimpleMsg (
        @SerializedName("success") var success: Boolean = true,
        @SerializedName("newToken") var newToken: Boolean = true,
        @SerializedName("msg") var msg: String = ""
    )

    class LP(
        @SerializedName("login") var login: String = "",
        @SerializedName("password") var password: String = ""
    )

    class Uuid(
        @SerializedName("uuid") var uuid: String = ""
    )

    class Token(
        @SerializedName("token") var token: String = "",
        @SerializedName("name") var name: String = ""
    )

    class User(
        @SerializedName("photo_url") var photoUrl: String = "",
        @SerializedName("fullname") var fullname: String = "",
        @SerializedName("surname") var surname: String = "",
        @SerializedName("name") var nameUser: String = "",
        @SerializedName("fname") var fname: String = ""
    )

    class Result (
        @SerializedName("id_order") var id_order: Int=0,
        @SerializedName("history_order_state") var history_order_state: String?="",
        @SerializedName("controls") var controls: String=""
    )


    class Result2 (
        @SerializedName("id_order") var id_order: Int = 0,
        @SerializedName("history_order_state") var history_order_state: JsonArray = JsonArray(),
        @SerializedName("controls") var controls: JsonArray = JsonArray()
    )

    class HistoryOrderOnServer (
        @SerializedName("unique_id") var unique_id: Long = 0,
        @SerializedName("idOrder") var idOrder: Long? =null,
        @SerializedName("stateOrder") var stateOrder: String="",
        @SerializedName("dateChange") var dateChange: Long=0L
    )


    class FileRoute(
        @SerializedName("fileRoute") var fileRoute: String=""
    )

    class ControlList(
        @Expose @SerializedName("controls") var list: MutableList<TemplateControl> = mutableListOf()
    )

    class CommonControlList(
        @Expose @SerializedName("common") var list: MutableList<ControlList> = mutableListOf()
    )

    class TemplateControl (
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


        @Expose @SerializedName("answered") var answered: Boolean = false
    ) {
        override fun toString(): String {
            return "TemplateControl(id=$id, results_id=$results_id, node=$node, guid='$guid', type='$type', value=${value.contentToString()}, question='$question', hint='$hint', resvalue=$resvalue, maxRange=$maxRange, minRange=$minRange, datetime=$datetime, replication_nodes=$replication_nodes, replicating_archival_records=$replicating_archival_records, group_checklist=$group_checklist, answered=$answered)"
        }
    }


    class CustomMarker(
        val order: Orders,
        val markerView: TextView
    )

    class DataFile(
        @SerializedName("idOrder") var idOrder: Int=0,
        @SerializedName("id") var id: Int=0,
        @SerializedName("resValue") var dir: String=""
    )

    class FilesFromServer(
        @SerializedName("guid") var guid: String="",
        @SerializedName("fileName") var fileName: String=""
    )

    class ReverseData(
        @SerializedName("data") var data: List<Result2> = listOf()
    )

    class MessageData(
        @SerializedName("text") var text: String="",
        @SerializedName("date") var date: Long=0L,
        @SerializedName("event_type") var event_type: Int=0,
        @SerializedName("lat") var lat: Double?=0.0,
        @SerializedName("lon") var lon: Double?=0.0
    )

}