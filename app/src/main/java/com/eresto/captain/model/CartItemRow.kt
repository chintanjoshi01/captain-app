package com.eresto.captain.model

data class CartItemRow(
    val id: Int,
    val kot_id: Int,
    val item_name: String,
    val item_short_name: String,
    var item_price: Double,
    val sp_inst: String?,
    var item_cat_id: Int,
    val table_id: Int,
    val pre_order_id: String,
    var qty: Int,
    var kot_ncv: Int,
    var notes: String?,
    val kitchen_cat_id: Int,
    var item_tax: String?,
    var item_tax_amt: String,
    var item_amt: String,
    var sorting: Int,
    var softDelete: Int,
    var isEdit: Int = 0
)