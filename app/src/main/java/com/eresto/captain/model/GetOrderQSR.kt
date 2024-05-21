package com.eresto.captain.model

data class GetOrderQSR(
    val `data`: QSRData,
    val message: String,
    val status: Int
)

data class QSRData(
    val order_details: List<OrderDetailQSR>,
    val total_orders: Int
)

data class OrderDetailQSR(
    val order: OrderQSR
)

data class OrderQSR(
    var created_at: String?,
    var cust_mobile: String?,
    var cust_name: String?,
    var cust_add: String?,
    var disc_percentage: Float,
    var disc_type: Int,
    var disc_code_id: Int,
    var cust_gst_no: String?,
    var no_of_person: Int,
    var cust_cat_id: Int,
    var inv_ncv: Int,
    var table_name: String,
    var kot_instance: ArrayList<KotInstance>?,
    var id: Int,
    var inv_id: String,
    var is_billed: Int,
    var last_kot_time: String,
    var order_closed: Int,
    var order_closed_by: Int,
    var inv_date: String,
    var order_instance: Int,
    var order_ref_no: String,
    var order_time: String,
    var order_type: Int,
    var resto_id: Int,
    var seating: Int,
    var session_id: Int,
    var short_name: String?,
    var tab_status: Int,
    var table_id: Int,
    var table_type: String?,
    var updated_at: String,
    var price: Float,
    var user_id: Int,
    var newItem: List<CartItemRow>?,
    var all_item_list : ArrayList<ItemQSR>
){
    constructor() : this(
    "",
    "",
    "",
    "",
    0f,0,0,
    "",
    0,0,0,
    "",
    arrayListOf(),
    0,
    "",
    0,
    "",
    0,0,
    "",0,
    "",
    "",0,0,0,0,
    "",0,0,
    "",
    "",0f,0, emptyList(),ArrayList<ItemQSR>()
    )
}

data class KotInstance(
    var item: List<ItemQSR>,
    val kot_instance_id: String,
    val instance: String,
    val orderId: Int,
    val kot_order_date: String,
    val is_delivered: Int,
    var isExpanded: Boolean = true,
    val short_name: String?,
    var soft_delete: Int,
    var kitchenName: String?
)


data class ItemQSR(
    val id: Int,
    val item_id: Int,
    val item_name: String,
    val order_date: String,
    var price: Int,
    var qty: Int,
    val short_name: String?,
    val is_delivered: Int,
    var sp_inst: String?,
    var soft_delete: Int,
    var is_edited: Int,
    var kitchen_cat_id: Int,
    var kot_ncv: Int,
    var item_tax: String?,
    var item_tax_amt: String,
    var item_amt: String
)

data class ItemQSRs(
    val id: Int,
    val item_id: Int,
    val item_name: String,
    val order_date: String,
    var price: Int,
    var qty: Int,
    val short_name: String,
    val is_delivered: Int,
    var sp_inst: String?,
    var notes: String?,
    var ncv: Int,
    var item_tax: String?,
    var item_tax_amt: String,
    var item_amt: String
)

data class OrderQSRs(
    val created_at: String,
    var cust_gst_no: String?,
    var cust_mobile: String?,
    var cust_name: String?,
    var cust_add: String?,
    var disc_percentage: Float,
    var disc_type: Int,
    var disc_code_id: Int,
    val id: Int,
    val inv_id: String,
    val is_billed: Int,
    val kot_instance: List<CartItemRow>,
    val last_kot_time: String?,
    var no_of_persons: Int,
    val cust_cat_id: Int,
    val order_ncv: Int,
    val order_closed: Int,
    val order_closed_by: Int,
    val order_date: String,
    val order_instance: Int,
    val order_ref_no: String,
    val order_time: String,
    val order_type: Int,
    val resto_id: Int,
    val seating: Int,
    val session_id: Int,
    val short_name: String,
    val tab_status: Int,
    val table_id: Int,
    var table_type: String,
    var table_name: String,
    val updated_at: String,
    var price: Float,
    val user_id: Int)

data class Orders(
    val order: OrderInstanceData,
    val kot: List<Kot>
)

data class Kot(
    val instance_unique_id: Int,
    val order_id: Int,
    val table_id: Int,
    val order_instance: Int,
    val order_instance_username: String,
    val kot_instance_id: String,
    val order_instance_data: List<OrderInstanceData>
)

data class OrderInstanceData(
    val id: Int,
    val order_id: Int,
    val kot_id: Int,
    val order_date: String,
    val item_id: Int,
    val item_name: String,
    val order_closed: Int,
    val tab_label: String,
    val qty: Int,
    val price: Int,
    val tab_id: Int,
    val sp_inst: String,
    val is_delivered: Int,
    val short_name: String,
    var cust_name:String,
    var cust_mobile:String,
    var no_of_persons:Int,
    var kitchen_cat_id:Int,
    var ncv:Int
)

data class SubmitNewOrderKOT(
    val `data`: DataInvoice,
    val message: String,
    val kot_instance_id: String,
    val status: Int,
)