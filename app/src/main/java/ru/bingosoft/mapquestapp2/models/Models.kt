package ru.bingosoft.mapquestapp2.models

import com.google.gson.annotations.SerializedName
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.db.Orders.Orders

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

    class OrderList(
        @SerializedName("data") var orders: List<Orders> = listOf()
    )

    class CheckupList(
        @SerializedName("data") var checkups: List<Checkup> = listOf()
    )

    class ControlList(
        @SerializedName("controls") var controls: List<TemplateControl> = listOf()
    )

    class TemplateControl (
        @SerializedName("id") var id: Int = 0,
        @SerializedName("type") var type: String = "",
        @SerializedName("value") var value: Array<String> = arrayOf(),
        @SerializedName("question") var question: String=""
    )


}