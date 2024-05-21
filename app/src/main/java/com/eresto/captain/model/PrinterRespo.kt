package com.eresto.captain.model


data class PrinterRespo(
    var id: Int,
    var printer_name: String,
    var printer_connection_type_id: Int,
    var printer_type: Int,
    var ip_add: String,
    var port_add: String,
    var printer_port: String,
    var index: String,
)

data class printers(
    var message: String = "",
    var status: Int = 0,
    var data: String
)
data class prnSetting(
    var message: String = "",
    var status: Int = 0,
    var data: List<PrinterSettingData>
)

data class PrinterSettingData(
    val action: Int,
    val copies: Int,
    val printer: Int,
    val submit: Int,
    val type: String,
    val kitchen_print: Int,
    val kit_cat_print: String
)

data class PrinterTypes(
    val id: Int,
    val printer_connection_type: String
)
