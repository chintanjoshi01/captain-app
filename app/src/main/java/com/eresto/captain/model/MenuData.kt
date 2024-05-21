package com.eresto.captain.model

data class MenuData(
    var category_display_order: Int,
    val category_name: String,
    val item_cat_id: Int,
    var pt: Int?,
    var en: Int?,
    var menu_type: Int?,
    var isExpanded: Boolean,
    var isSelected: Boolean,
    var items: ArrayList<Item>
)

data class Item(
    val delivery_time: Int,
    val id: Int,
    val item_id: Int,
    val is_all_time: Int,
    val is_breakfast: Int,
    val is_dinner: Int,
    val is_lunch: Int,
    var item_cat_id: Int,
    val ingredients: String?,
    var is_na: Int,
    var is_nonveg: Int,
    val is_room:Int,
    val in_room_price:Int,
    var item_name: String,
    val item_name_alias: String?,
    var item_price: Double,
    val short_desc: String?,
    val item_short_name: String?,
    val session_id: String?,
    val item_image: String?,
    val sp_inst: String?,
    val qty: String?,
    val cal: String?,
    val aik: String?,
    val kitchen_cat_id: Int,
    val item_sales_cat_id: Int,
    val item_att: String?,
    var isHighLight: Boolean=false,
    var is_fsi: Int,
    var local_sp_inst: String?,
    var isChecked: Boolean,
    var count: Int,
    var localId: Int,
    var menu_type: Int,
    var tax_tmpl_mast_id: Int,
    var menu_cat_id: String,
    var kot_ncv: Int,
    var item_tax: String?
)


data class PriceTemplateData(
    val pt_id: Int,
    val price_template: String,
    val prices: String?,
    val menu_type: Int,
    var isChecked: Boolean
)
