package ru.bingosoft.teploInspector.models

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
        @SerializedName("controls") var list: List<TemplateControl> = listOf()
    )

    class TemplateControl (
        @SerializedName("id") var id: Int = 0,
        @SerializedName("guid") var guid: String = "",
        @SerializedName("type") var type: String = "",
        @SerializedName("value") var value: Array<String> = arrayOf(),
        @SerializedName("question") var question: String="",
        @SerializedName("hint") var hint: String="",
        @SerializedName("resvalue") var resvalue: String="",

        var checked: Boolean=false
    )


}