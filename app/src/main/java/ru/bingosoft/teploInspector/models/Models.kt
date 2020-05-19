package ru.bingosoft.teploInspector.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.CheckupGuide.CheckupGuide
import ru.bingosoft.teploInspector.db.Orders.Orders
import java.util.*

class Models {
    class SimpleMsg (
        @SerializedName("success") var success: Boolean = true,
        @SerializedName("newToken") var newToken: Boolean = true,
        @SerializedName("msg") var msg: String = ""
    )

    class Auth(
        @SerializedName("success") var success: Boolean = true,
        @SerializedName("newToken") var newToken: String = "",
        @SerializedName("session_id") var session_id: String = ""
    )

    class UserResult(
        @SerializedName("data") var userInfo: User? = null
    )

    class User(
        @SerializedName("photo_url") var photoUrl: String = "",
        @SerializedName("fullname") var fullname: String = "",
        @SerializedName("surname") var surname: String = "",
        @SerializedName("name") var nameUser: String = "",
        @SerializedName("fname") var fname: String = ""
    )

    class UserLocation(
        var date: Date,
        var lat: Double,
        var lon: Double
    )

    class OrderList(
        @SerializedName("success") var success: Boolean = false,
        @SerializedName("data") var orders: List<Orders> = listOf()
    )

    class CheckupList(
        @SerializedName("success") var success: Boolean = false,
        @SerializedName("data") var checkups: List<Checkup> = listOf()
    )

    class CheckupGuideList(
        @SerializedName("data") var guides: List<CheckupGuide> = listOf()
    )

    class ControlList(
        @Expose @SerializedName("controls") var list: MutableList<TemplateControl> = mutableListOf()
    )

    class CommonControlList(
        @Expose @SerializedName("common") var list: MutableList<ControlList> = mutableListOf()
    )

    class TemplateControl (
        @Expose @SerializedName("id") var id: Int = 0,
        @Expose @SerializedName("guid") var guid: String = "",
        @Expose @SerializedName("type") var type: String = "",
        @Expose @SerializedName("value") var value: Array<String> = arrayOf(),
        @Expose @SerializedName("idvalue") var idvalue: Array<Int> = arrayOf(),
        @Expose @SerializedName("subvalue") var subvalue: Array<Subvalue> = arrayOf(),
        @Expose @SerializedName("question") var question: String="",
        @Expose @SerializedName("hint") var hint: String="",
        @Expose @SerializedName("resvalue") var resvalue: String="",
        @Expose @SerializedName("subcheckup") var subcheckup: List<Checkup> = listOf(),
        @Expose @SerializedName("multiplicity") var multiplicity: Int=0,

        @Expose var checked: Boolean=false,
        //var controlList: ControlList?=null
        var groupControlList: CommonControlList?=null,
        var parent: TemplateControl?=null
    )

    class Subvalue(
        @SerializedName("idlink") var id: Int = 0,
        @SerializedName("value") var value: Array<String> = arrayOf()
    )

    /*class PhotoResult(
        @SerializedName("dir") var dir: String = "",
        @SerializedName("lat") var lat: Double? = 0.0,
        @SerializedName("lon") var lon: Double? = 0.0
    )*/

    class MapPoint(
        @SerializedName("lat") var lat: Double? = 0.0,
        @SerializedName("lon") var lon: Double? = 0.0
    )


}