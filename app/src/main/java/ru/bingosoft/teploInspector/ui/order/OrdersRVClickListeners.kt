package ru.bingosoft.teploInspector.ui.order

import android.view.View

interface OrdersRVClickListeners {
    fun recyclerViewListClicked(v: View?, position: Int)
}