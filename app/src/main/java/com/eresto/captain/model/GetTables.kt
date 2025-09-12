package com.eresto.captain.model


data class GetTables(
    val id: Int,
    var tab_status: Int,
    var tab_label: String,
    val tab_type: Int,
    val order_type : Int
)

data class kitCat(
    val id: Int,
    var item_kitchen_cat: String
)