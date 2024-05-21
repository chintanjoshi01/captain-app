package com.eresto.captain.model

data class GetTakeawayOrderQSR(val `data`: List<TakeawayOrder>,
    val message: String,
    val status: Int)

data class TakeawayOrder(
    val cust_mobile: String,
    val cust_name: String,
    val cust_gst_no: String?,
    val cust_add: String?,
    val no_of_persons: Int,
    val cust_cat_id: Int,
    val disc_percentage: Int,
    val id: Int,
    val inv_id: Int,
    val inv_date: String,
    val order_ref_no: String?,
    var resto_order_kot: List<RestoOrderKot>,
    val tab_label: String,
    val table_id: Int,
    val user_id: Int,
    var is_delivered: Int,
    var isExpanded: Boolean = true,
    var kot_instance_id: String,
    var soft_delete: Int,
    var kitchenCat: String)

data class RestoOrderKot(val kot_id: Int,
    val order_id: Int,
    val item_id: Int,
    val item_name: String,
    val qty: Int,
    val price: Double,
    val sp_inst: String,
    val is_delivered: Int,
    val kot_instance_id: String,
    var soft_delete: Int,
    var is_edited: Int,
    var kitchen_cat_id: Int,
    var kot_ncv: Int)