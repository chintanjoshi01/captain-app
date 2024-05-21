package com.eresto.captain.model

data class GetInvoiceById(val `data`: DataInvoice, val message: String, val status: Int)

data class DataInvoice(
    val inv_details: InvDetails,
    val kot: List<InvoiceKot>,
    var newKot: List<CartItemRow>?
)

data class InvDetails(
    val amt_bf_tax: String,
    val inv_tmpl_id: Int,
    var cust_gst_no: String?,
    var cust_mobile: String,
    var cust_name: String,
    var cust_add: String?,
    var discount_percentage: Float,
    var disc_type: Int,
    var disc_code_id: Int,
    val id: Int,
    val inv_amt: String,
    val inv_date: String,
    var inv_disc: String,
    val inv_no: String,
    val invoicing: Int,
    var no_of_ppl: Int,
    var cust_cat_id: Int,
    var inv_ncv: Int,
    val order_type: Int,
    val pymt_status: Int,
    val rcpt_amt: String,
    val rcpt_details: String,
    val round_off: String,
    val session_id: Int,
    val tab_id: Int,
    val tab_label: String,
    val total_amt: String,
    val top_note: String?,
    val footer_note: String,
    var tab_type: String,
    val tot: Int,
    val apc: Float,
    var short_name: String?,
    var inv_tax: String?,
    var inv_tax_amt: String?,
    var inv_by: String?,
    var pymt_rcvd_by: String?,
    var inv_status: Int
)

data class InvoiceKot(
    val id: Int,
    val is_delivered: Int,
    val item_id: Int,
    val item_name: String,
    val order_instance: Int,
    var price: Int,
    var qty: Int,
    val sp_inst: String,
    val kitchen_cat_id: Int,
    var kot_ncv: Int,
    var item_tax: String,
    var item_tax_amt: String,
    var item_amt: String
)

data class TaxInfo(
    val item: String,
    val tax_info: ArrayList<TaxDetails>
)
data class TaxDetails(
    val tax: String,
    val tax_id: Int,
    val tax_rate: Double,
    val tax_amt: Double
)