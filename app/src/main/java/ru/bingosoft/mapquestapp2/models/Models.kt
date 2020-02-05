package ru.bingosoft.mapquestapp2.models

import com.google.gson.annotations.SerializedName
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

    class Order


}