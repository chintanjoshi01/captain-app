package com.eresto.captain.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.eresto.captain.R
import com.eresto.captain.model.DataInvoice
import com.eresto.captain.model.InvoiceKot
import com.eresto.captain.model.ItemQSR
import com.eresto.captain.model.KotInstance
import com.eresto.captain.model.Orders
import com.eresto.captain.model.PrinterRespo
import com.eresto.captain.model.RestoOrderKot
import com.eresto.captain.model.TakeawayOrder
import com.eresto.captain.views.printer.AsyncBluetoothEscPosPrint
import com.eresto.captain.views.printer.AsyncEscPosPrint
import com.eresto.captain.views.printer.AsyncEscPosPrinter
import com.eresto.captain.views.printer.AsyncTcpEscPosPrint
import com.eresto.captain.views.printer.AsyncUsbEscPosPrint
import com.eresto.captain.views.printer.connection.tcp.TcpConnection
import com.eresto.captain.views.printer.connection.usb.UsbConnection
import com.eresto.captain.views.printer.connection.usb.UsbPrintersConnections
import org.json.JSONArray
import org.json.JSONException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import kotlin.math.roundToInt


class PrintMaster {

    companion object {
        var dialog: Dialog? = null
        var printer: AsyncEscPosPrinter? = null
        var pref: Preferences? = null
        var formatedAddress = ""
        val SEPARATE = 1
        val SINGLE = 2
        val BOTH = 3
        val KITCHENWISEPRINT = 4
        val PERMISSION_BLUETOOTH = 1

        interface FindPrinter {
            fun onPrinterFond(usbConnection: UsbConnection, usbManager: UsbManager)
            fun onPrinterNotFond()
            fun onPrinterPermission(
                usbConnection: UsbConnection,
                usbManager: UsbManager,
                permissionIntent: PendingIntent
            )
        }

        fun decidePrintFunctionality(
            activity: Activity,
            kot: KotInstance?,
            kots: List<InvoiceKot>?,
            kotTake: TakeawayOrder?,
            data: DataInvoice?,
            table: String,
            person: Int,
            custAddress: String,
            print: PrinterRespo,
            type: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            pref = Preferences()
            val db = DBHelper(activity)
            when (print.printer_connection_type_id) {
                -2 -> {
                    if (kot != null) {

                        val findKitchenCat = ArrayList<Int>()
                        val findKitchenCatStr = ArrayList<String>()
                        for (item in kot.item) {
                            var flag = 0
                            for (kit in findKitchenCat) {
                                if (item.kitchen_cat_id == kit) {
                                    flag = 1
                                }
                            }
                            if (flag == 0) {
                                val cat = db.GetKitCatSingle(item.kitchen_cat_id)
                                var catName = "unknown"
                                if (cat != null) {
                                    catName = cat.item_kitchen_cat
                                }
                                findKitchenCat.add(item.kitchen_cat_id)
                                findKitchenCatStr.add(catName)
                            }
                        }

                        printKitchenCatGroupWise(
                            activity,
                            findKitchenCat,
                            findKitchenCatStr,
                            kot,
                            table,
                            3,
                            print.ip_add,
                            print.port_add,
                            person,
                            onSuccess
                        )

                    } else if (kotTake != null) {
                        val findKitchenCat = ArrayList<Int>()
                        val findKitchenCatStr = ArrayList<String>()
                        for (item in kotTake.resto_order_kot) {
                            var flag = 0
                            for (kit in findKitchenCat) {
                                if (item.kitchen_cat_id == kit) {
                                    flag = 1
                                }
                            }
                            if (flag == 0) {
                                val cat = db.GetKitCatSingle(item.kitchen_cat_id)
                                var catName = "unknown"
                                if (cat != null) {
                                    catName = cat.item_kitchen_cat
                                }
                                findKitchenCat.add(item.kitchen_cat_id)
                                findKitchenCatStr.add(catName)
                            }
                        }
                        printKitchenCatGroupWise(
                            activity,
                            findKitchenCat,
                            findKitchenCatStr,
                            kotTake,
                            table,
                            3,
                            print.ip_add,
                            print.port_add,
                            person,
                            onSuccess
                        )
                    }
                }

                -1 -> {
                    if (kot != null) {
                        val findKitchenCat = ArrayList<Int>()
                        val findKitchenCatStr = ArrayList<String>()
                        for (item in kot.item) {
                            var flag = 0
                            for (kit in findKitchenCat) {
                                if (item.kitchen_cat_id == kit) {
                                    flag = 1
                                }
                            }
                            if (flag == 0) {
                                val cat = db.GetKitCatSingle(item.kitchen_cat_id)
                                var catName = "unknown"
                                if (cat != null) {
                                    catName = cat.item_kitchen_cat
                                }
                                findKitchenCat.add(item.kitchen_cat_id)
                                findKitchenCatStr.add(catName)
                            }
                        }

                        printKitchenCatWise(
                            activity,
                            findKitchenCat,
                            findKitchenCatStr,
                            -1,
                            kot,
                            table,
                            3,
                            print.ip_add,
                            print.port_add,
                            person,
                            onSuccess
                        )

                    } else if (kotTake != null) {
                        val findKitchenCat = ArrayList<Int>()
                        val findKitchenCatStr = ArrayList<String>()
                        for (item in kotTake.resto_order_kot) {
                            var flag = 0
                            for (kit in findKitchenCat) {
                                if (item.kitchen_cat_id == kit) {
                                    flag = 1
                                }
                            }
                            if (flag == 0) {
                                val cat = db.GetKitCatSingle(item.kitchen_cat_id)
                                var catName = "unknown"
                                if (cat != null) {
                                    catName = cat.item_kitchen_cat
                                }
                                findKitchenCat.add(item.kitchen_cat_id)
                                findKitchenCatStr.add(catName)
                            }
                        }
                        printKitchenCatWise(
                            activity,
                            findKitchenCat,
                            findKitchenCatStr,
                            -1,
                            kotTake,
                            table,
                            3,
                            print.ip_add,
                            print.port_add,
                            person,
                            onSuccess
                        )
                    }
                }

                1 -> {
                    when (print.printer_type) {
                        2 -> {
                            if (kot != null) {
                                if (pref!!.getInt(activity, "kitchen_print") > 0) {
                                    val findKitchenCat = ArrayList<Int>()
                                    val findKitchenCatStr = ArrayList<String>()
                                    for (item in kot.item) {
                                        var flag = 0
                                        for (kit in findKitchenCat) {
                                            if (item.kitchen_cat_id == kit) {
                                                flag = 1
                                            }
                                        }
                                        if (flag == 0) {
                                            val cat = db.GetKitCatSingle(item.kitchen_cat_id)
                                            var catName = "unknown"
                                            if (cat != null) {
                                                catName = cat.item_kitchen_cat
                                            }
                                            findKitchenCat.add(item.kitchen_cat_id)
                                            findKitchenCatStr.add(catName)
                                        }
                                    }

                                    printKitchenCatWise(
                                        activity,
                                        findKitchenCat,
                                        findKitchenCatStr,
                                        -1,
                                        kot,
                                        table,
                                        3,
                                        print.ip_add,
                                        print.port_add,
                                        person,
                                        onSuccess
                                    )
                                } else {
                                    printKOTFunction(
                                        activity,
                                        kot,
                                        table,
                                        type,
                                        print.ip_add,
                                        print.port_add,
                                        person,
                                        onSuccess
                                    )
                                }
                            } else if (kotTake != null) {
                                if (pref!!.getInt(activity, "kitchen_print") > 0) {
                                    val findKitchenCat = ArrayList<Int>()
                                    val findKitchenCatStr = ArrayList<String>()
                                    for (item in kotTake.resto_order_kot) {
                                        var flag = 0
                                        for (kit in findKitchenCat) {
                                            if (item.kitchen_cat_id == kit) {
                                                flag = 1
                                            }
                                        }
                                        if (flag == 0) {
                                            val cat = db.GetKitCatSingle(item.kitchen_cat_id)
                                            var catName = "unknown"
                                            if (cat != null) {
                                                catName = cat.item_kitchen_cat
                                            }
                                            findKitchenCat.add(item.kitchen_cat_id)
                                            findKitchenCatStr.add(catName)
                                        }
                                    }
                                    printKitchenCatWise(
                                        activity,
                                        findKitchenCat,
                                        findKitchenCatStr,
                                        -1,
                                        kotTake,
                                        table,
                                        3,
                                        print.ip_add,
                                        print.port_add,
                                        person,
                                        onSuccess
                                    )
                                } else {
                                    printKOTFunction(
                                        activity,
                                        kotTake,
                                        table,
                                        type,
                                        print.ip_add,
                                        print.port_add,
                                        person,
                                        onSuccess
                                    )
                                }

                            } else if (data != null) {
                                printInvoiceFunction(
                                    activity,
                                    data,
                                    print.ip_add,
                                    print.port_add,
                                    onSuccess
                                )
                            }
                        }

                        /* 1 -> {
                             if (kot != null) { //                                printKOTFunction(activity, kot, table, type, print.ip, print.port, onSuccess)
                             } else if (kotTake != null) { //                                printKOTFunction(activity, kotTake, table, type, print.ip, print.port, onSuccess)
                             } else if (data != null) {
                                 A5Printer.a5Print(activity, data, print, onSuccess)
                             }
                         }*/
                    }
                }

                2 -> {//Bluetooth
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.BLUETOOTH
                        ) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(
                                activity,
                                arrayOf(
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                ),
                                PERMISSION_BLUETOOTH
                            )
                        } else {
                            ActivityCompat.requestPermissions(
                                activity,
                                arrayOf(
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_SCAN
                                ),
                                PERMISSION_BLUETOOTH
                            )
                        }
                    } else {
                        when (print.printer_type) {


                            2 -> {
                                if (kot != null) {
//                                    if (pref!!.getInt(activity, "kitchen_print") > 0) {
//
//                                        val findKitchenCat = ArrayList<Int>()
//                                        val findKitchenCatStr = ArrayList<String>()
//                                        for (item in kot.item) {
//                                            var flag = 0
//                                            for (kit in findKitchenCat) {
//                                                if (item.kitchen_cat_id == kit) {
//                                                    flag = 1
//                                                }
//                                            }
//                                            if (flag == 0) {
//                                                val cat = db.GetKitCatSingle(item.kitchen_cat_id)
//                                                var catName = "unknown"
//                                                if (cat != null) {
//                                                    catName = cat.item_kitchen_cat
//                                                }
//                                                findKitchenCat.add(item.kitchen_cat_id)
//                                                findKitchenCatStr.add(catName)
//                                            }
//                                        }
//
//                                        printKitchenCatWise(
//                                            activity,
//                                            findKitchenCat,
//                                            findKitchenCatStr,
//                                            -1,
//                                            kot,
//                                            table,
//                                            3,
//                                            print.ip_add,
//                                            print.port_add,
//                                            person,
//                                            onSuccess
//                                        )
//                                    } else {
                                    bluetoothKOTPrint(
                                        activity,
                                        kot,
                                        table,
                                        type,
                                        person,
                                        onSuccess
                                    )
//                                    }
                                } else if (kotTake != null) {
                                    bluetoothKOTPrint(
                                        activity,
                                        kotTake,
                                        table,
                                        type,
                                        person,
                                        onSuccess
                                    )
                                } else if (data != null) {
                                    printBluetoothInvoiceFunction(
                                        activity,
                                        data,
                                        onSuccess
                                    )
                                }
                            }

//                            1 -> {
//                                if (kot != null) { //                                printKOTFunction(activity, kot, table, type, print.ip, print.port, onSuccess)
//                                } else if (kotTake != null) { //                                printKOTFunction(activity, kotTake, table, type, print.ip, print.port, onSuccess)
//                                } else if (data != null) {
//                                    A5Printer.a5Print(activity, data, print, onSuccess)
//                                }
//                            }
                        }
                    }
                }

                3 -> {
                    var docType = ""
                    var docId = ""
                    if (kot != null) {
                        docType = "1"
                        docId = kot.kot_instance_id
                    } else if (kots != null) {
                        docType = "1"
                    } else {
                        if (kotTake != null) {
                            docType = "1"
                            docId = kotTake.kot_instance_id
                        } else {
                            if (data != null) {
                                docType = "2"
                                docId = data.inv_details.id.toString()
                            } else {
                                docType = "1"
                                docId = ""
                            }
                        }
                    }
//                    cloudPrint(
//                        activity,
//                        "${print.printer_name}|${print.printer_connection_type_id}|${print.printer_type}|${print.ip_add}|${print.port_add}",
//                        docType,
//                        docId,
//                        onSuccess
//                    )
                }

                5 -> {
                    when (print.printer_type) {
                        2 -> {
                            if (kot != null) {
                                printUsb(activity, object : FindPrinter {
                                    override fun onPrinterFond(
                                        usbConnection: UsbConnection,
                                        usbManager: UsbManager
                                    ) {
                                        if (isPrinterConnected(print, usbConnection)) {
                                            printKOTFunctionUSB(
                                                activity,
                                                kot,
                                                table,
                                                type,
                                                usbConnection, usbManager,
                                                person,
                                                onSuccess
                                            )
                                        } else {
                                            Utils.displayActionSnackbar(
                                                activity,
                                                "${print.printer_name} " + activity.resources.getString(
                                                    R.string.printer_not_connected
                                                ),
                                                2
                                            )
                                        }
                                    }

                                    override fun onPrinterNotFond() {
                                        Utils.displayActionSnackbar(
                                            activity,
                                            activity.resources.getString(R.string.printer_not_found),
                                            2
                                        )
                                    }

                                    override fun onPrinterPermission(
                                        usbConnection: UsbConnection,
                                        usbManager: UsbManager,
                                        permissionIntent: PendingIntent
                                    ) {
                                        val filter = IntentFilter(Utils.ACTION_USB_PERMISSION)
                                        val usbReceiver = usbReceiver
                                        activity.registerReceiver(usbReceiver, filter)
                                        usbManager.requestPermission(
                                            usbConnection.device,
                                            permissionIntent
                                        )
                                    }
                                })
                            } else if (kotTake != null) {
                                printUsb(activity, object : FindPrinter {
                                    override fun onPrinterFond(
                                        usbConnection: UsbConnection,
                                        usbManager: UsbManager
                                    ) {
                                        if (isPrinterConnected(print, usbConnection)) {
                                            printKOTFunctionUSB(
                                                activity,
                                                kotTake,
                                                table,
                                                type,
                                                usbConnection, usbManager,
                                                person,
                                                onSuccess
                                            )
                                        } else {
                                            Utils.displayActionSnackbar(
                                                activity,
                                                "${print.printer_name} " + activity.resources.getString(
                                                    R.string.printer_not_connected
                                                ),
                                                2
                                            )
                                        }
                                    }

                                    override fun onPrinterNotFond() {
                                        Utils.displayActionSnackbar(
                                            activity,
                                            activity.resources.getString(R.string.printer_not_found),
                                            2
                                        )
                                    }

                                    override fun onPrinterPermission(
                                        usbConnection: UsbConnection,
                                        usbManager: UsbManager,
                                        permissionIntent: PendingIntent
                                    ) {
                                        val filter = IntentFilter(Utils.ACTION_USB_PERMISSION)
                                        val usbReceiver = usbReceiver
                                        activity.registerReceiver(usbReceiver, filter)
                                        usbManager.requestPermission(
                                            usbConnection.device,
                                            permissionIntent
                                        )
                                    }
                                })
                            } else if (data != null) {
                                printUsb(activity, object : FindPrinter {
                                    override fun onPrinterFond(
                                        usbConnection: UsbConnection,
                                        usbManager: UsbManager
                                    ) {
                                        if (isPrinterConnected(print, usbConnection)) {
                                            printInvoiceFunctionUSB(
                                                activity,
                                                data,
                                                usbConnection, usbManager,
                                                onSuccess
                                            )
                                        } else {
                                            Utils.displayActionSnackbar(
                                                activity,
                                                "${print.printer_name} " + activity.resources.getString(
                                                    R.string.printer_not_connected
                                                ),
                                                2
                                            )
                                        }
                                    }

                                    override fun onPrinterNotFond() {
                                        Utils.displayActionSnackbar(
                                            activity,
                                            activity.resources.getString(R.string.printer_not_found),
                                            2
                                        )
                                    }

                                    override fun onPrinterPermission(
                                        usbConnection: UsbConnection,
                                        usbManager: UsbManager,
                                        permissionIntent: PendingIntent
                                    ) {
                                        val filter = IntentFilter(Utils.ACTION_USB_PERMISSION)
                                        val usbReceiver = usbReceiver
                                        activity.registerReceiver(usbReceiver, filter)
                                        usbManager.requestPermission(
                                            usbConnection.device,
                                            permissionIntent
                                        )

                                    }
                                })
                            }
                        }

//                        1 -> {
//                            if (kot != null) { //                                printKOTFunction(activity, kot, table, type, print.ip, print.port, onSuccess)
//                            } else if (kotTake != null) { //                                printKOTFunction(activity, kotTake, table, type, print.ip, print.port, onSuccess)
//                            } else if (data != null) {
//                                A5Printer.a5Print(activity, data, print, onSuccess)
//                            }
//                        }
                    }
                }
            }
        }

        fun findPrinterAsPerCat(
            db: DBHelper,
            kitCatPrint: List<String>,
            catId: Int
        ): PrinterRespo? {
            for (kitPrinter in kitCatPrint) {
                val catIds = kitPrinter.split("|")[0].toInt()
                if (catId == catIds) {
                    return db.GetPrinterById(kitPrinter.split("|")[1].toInt())
                }
            }
            return null
        }

        fun bluetoothKOTPrint(
            activity: Activity,
            kot: KotInstance,
            table: String,
            type: Int,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {


            var str: String? = null
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            try {
                date = format.parse(kot.kot_order_date)
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var top =
                "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}/${kot.short_name}"
            val max = 20
            val different = if (top.length > max) 0 else max - top.length
            var isEdited = false
            var isDeletedKOT = true

            for (it in kot.item) {
                if (it.soft_delete == 0) isDeletedKOT = false
                if (kot.soft_delete != 0) {
                    it.soft_delete = 2
                } else if (it.is_edited != 0) {
                    isEdited = true
                }
            }
            if (isDeletedKOT) {
                kot.soft_delete = 2
                isEdited = false
            }
            if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                top += "..."
            }
            var print =
                if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
            print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
            print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                str!!.split(
                    "/"
                )[0]
            } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]Pax:$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"
            var total = 0
            for (it in kot.item.indices) {
                if (kot.item[it].soft_delete != 0) kot.item[it].is_edited = 0
                val name = getFontStrikeText(kot.item[it], "name", "[L]")
                val qty = getFontStrikeText(kot.item[it], "qty", "[R]")
                val sp = getFontStrikeText(kot.item[it], "sp", "[L]")
                var remain = ""
                val itemName = if (kot.item[it].item_name.length > 19) {
                    remain = kot.item[it].item_name.substring(19, kot.item[it].item_name.length)
                    kot.item[it].item_name.substring(0, 19)
                } else {
                    kot.item[it].item_name
                }
                print += name + "${it + 1}. $itemName" + getEndPoint(name) + qty + "<b>${
                    if (kot.item[it].soft_delete == 0) {
                        kot.item[it].qty
                    } else {
                        "XXX"
                    }
                }</b>" + getEndPoint(qty) + "\n"
                if (remain.isNotEmpty()) print += "$name    ${remain.trim() + getEndPoint(name)}\n"
                if (kot.item[it].sp_inst != null && kot.item[it].sp_inst != "null") {
                    try {
                        if (!kot.item[it].sp_inst.isNullOrEmpty()) {
                            val items = JSONArray(kot.item[it].sp_inst)
                            for (i in 0 until items.length()) {
                                print += "[L]    $sp<b>-${
                                    items.getString(i).trim().replace("\\/", "/")
                                }</b>" + getEndPoint(sp) + "\n"
                            }
                            print += "\n"
                        } else {
                            if (kot.item[it].sp_inst != "null") {
                                if (kot.item[it].sp_inst != null && kot.item[it].sp_inst!!.isNotEmpty()) {
                                    print += "[L]    $sp<b>-${kot.item[it].sp_inst}</b>" + getEndPoint(
                                        sp
                                    ) + "\n"
                                }
                            }
                        }

                    } catch (e: JSONException) {
                        if (!kot.item[it].sp_inst.isNullOrEmpty()) {
                            val text = kot.item[it].sp_inst!!.split(",")
                            for (te in text) {
                                print += "[L]    $sp<b>-${te.trim()}</b>" + getEndPoint(sp) + "\n"
                            }
                        }
                        e.printStackTrace()
                    }
                }
                if (kot.item[it].kot_ncv != 0) {
                    var ss = ""
                    ss += when (kot.item[it].kot_ncv) {
                        1 -> " (No Charge)\n"
                        2 -> " (Complementary)\n"
                        3 -> " (Void)\n"
                        else -> ""
                    }

                    if (ss.isNotEmpty()) {
                        print += "[L]"
                        for (i in 0 until (it + 1).toString().length + 1) print += " "
                        print += "$ss"
                    }
                }
                total += kot.item[it].qty
            }
            print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>\n"
            print += byteArrayOf(0x1B, 0x0A)
//            print += "\n[C]" + "\n[C]"

            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncBluetoothEscPosPrint(
                activity,
                onSuccess,
                type,
                if (copies == 0) 1 else copies
            ).execute(getAsyncEscBluetoothPosPrinter(print))
        }


        fun bluetoothKOTPrint(
            activity: Activity,
            kot: TakeawayOrder,
            table: String,
            type: Int,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {

            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            var str: String? = null
            try {
                date = format.parse(kot.inv_date)
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var top = "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}"
            val max = 20
            val different = if (top.length > max) 0 else max - top.length
            val rest = (different / 2f).roundToInt()

            var isEdited = false
            var isDeletedKOT = true

            for (it in kot.resto_order_kot) {
                if (it.soft_delete == 0) isDeletedKOT = false
                if (kot.soft_delete != 0) {
                    it.soft_delete = 2
                } else if (it.is_edited != 0) {
                    isEdited = true
                }
            }
            if (isDeletedKOT) {
                kot.soft_delete = 2
                isEdited = false
            }

            if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                top += "..."
            }
            var print =
                if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
            print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
            print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                str!!.split(
                    "/"
                )[0]
            } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"

            var total = 0
            for (it in kot.resto_order_kot.indices) {
                val item =
                    ItemQSR(
                        kot.resto_order_kot[it].kot_id,
                        kot.resto_order_kot[it].item_id,
                        kot.resto_order_kot[it].item_name,
                        "",
                        kot.resto_order_kot[it].price,
                        kot.resto_order_kot[it].qty,
                        "",
                        kot.resto_order_kot[it].is_delivered,
                        kot.resto_order_kot[it].sp_inst,
                        kot.resto_order_kot[it].soft_delete,
                        kot.resto_order_kot[it].is_edited,
                        kot.resto_order_kot[it].kitchen_cat_id,
                        kot.resto_order_kot[it].kot_ncv,
                        "",
                        "",
                        ""
                    )
                val fontSize = "a";
                val name = getFontStrikeText(item, "name", "[L]<font size='$fontSize'>")
                val qty = getFontStrikeText(item, "qty", "[R]<font size='$fontSize'>")
                val sp = getFontStrikeText(item, "sp", "[L]<font size='$fontSize'>")
                print += name + "${it + 1}. ${kot.resto_order_kot[it].item_name}" + getEndPoint(name) + qty + "<b>${
                    if (kot.resto_order_kot[it].soft_delete == 0) {
                        kot.resto_order_kot[it].qty
                    } else {
                        "XXX"
                    }
                }</b>" + getEndPoint(qty) + "\n"
                val itemSp = kot.resto_order_kot[it]
                var spDash = "-";
                if (itemSp.is_edited != 0) {
                    when (itemSp.is_edited) {
                        2, 3, 9 -> { //sp
                            spDash = "--- "
                        }
                    }
                }
                if (itemSp.sp_inst != null && itemSp.sp_inst != "null") {
                    try {
                        if (!itemSp.sp_inst.isNullOrEmpty()) {
                            val items = JSONArray(itemSp.sp_inst)
                            for (i in 0 until items.length()) {
                                print += "[L]    <font size='tall'>$spDash${
                                    items.getString(i).trim().replace("\\/", "/")
                                }</font>\n"
                            }
                            print += "\n"
                        } else {
                            if (itemSp.sp_inst != "null") {
                                if (itemSp.sp_inst != null && itemSp.sp_inst!!.isNotEmpty()) {
                                    print += "[L]    <font size='tall'>$spDash${itemSp.sp_inst}</font>\n"
                                    //+ getEndPoint(sp)
//                                    + "\n"
                                }
                            }
                        }

                    } catch (e: JSONException) {
                        if (!itemSp.sp_inst.isNullOrEmpty()) {
                            val text = itemSp.sp_inst!!.split(",")
                            for (te in text) {
                                print += "[L]     <font size='tall'>$spDash${te.trim()}</font>\n"
                                //getEndPoint(sp)
//                                + "\n"
                            }
                        }
                        e.printStackTrace()
                    }
                }
                if (kot.resto_order_kot[it].kot_ncv != 0) {
                    var ss = ""
                    ss += when (kot.resto_order_kot[it].kot_ncv) {
                        1 -> " (No Charge)\n"
                        2 -> " (Complementary)\n"
                        3 -> " (Void)\n"
                        else -> ""
                    }

                    if (ss.isNotEmpty()) {
                        print += "[L]"
                        for (i in 0 until (it + 1).toString().length + 1) print += " "
                        print += "$ss"
                    }
                }
                total += kot.resto_order_kot[it].qty
            }
            print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>"

            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncBluetoothEscPosPrint(
                activity,
                onSuccess,
                type,
                if (copies == 0) 1 else copies
            ).execute(getAsyncEscBluetoothPosPrinter(print))

        }

        fun printBluetoothInvoiceFunction(
            activity: Activity,
            order: DataInvoice?,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            pref = Preferences()
            val print = getPrintStrInvoice(activity, order)
            val copies = pref!!.getInt(activity, "invoice_copies")
            AsyncBluetoothEscPosPrint(
                activity,
                onSuccess,
                2,
                if (copies == 0) 1 else copies
            ).execute(getAsyncEscBluetoothPosPrinter(print))
        }

        fun printKitchenCatGroupWise(
            activity: Activity,
            findKitCat: ArrayList<Int>,
            findKitCatStr: ArrayList<String>,
            kot: KotInstance,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val instance = ArrayList<KotInstance>()
            for (i in findKitCat.indices) {
                val item = ArrayList<ItemQSR>()
                for (it in kot.item) {
                    if (it.kitchen_cat_id == findKitCat[i]) {
                        item.add(it)
                    }
                }
                instance.add(
                    KotInstance(
                        item,
                        kot.kot_instance_id,
                        kot.instance,
                        kot.orderId,
                        kot.kot_order_date,
                        kot.is_delivered,
                        kot.isExpanded,
                        kot.short_name,
                        kot.soft_delete,
                        findKitCatStr[i]
                    )
                )
            }
            if (!ip.isNullOrBlank() && !port.isNullOrBlank()) {
                printKOTGroupFunction(activity, instance, table, type, ip, port, person) {
                    onSuccess.onSuccess(it)
                }
            } else {
                onSuccess.onSuccess(false)
            }
        }

        fun printKitchenCatGroupWise(
            activity: Activity,
            findKitCat: ArrayList<Int>,
            findKitCatStr: ArrayList<String>,
            kot: TakeawayOrder,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val instance = ArrayList<TakeawayOrder>()
            for (i in findKitCat.indices) {
                val item = ArrayList<RestoOrderKot>()
                for (kots in kot.resto_order_kot) {
                    if (kots.kitchen_cat_id == findKitCat[i]) {
                        item.add(kots)
                    }
                }
                instance.add(
                    TakeawayOrder(
                        kot.cust_mobile,
                        kot.cust_name,
                        kot.cust_gst_no,
                        kot.cust_add,
                        kot.no_of_persons,
                        kot.cust_cat_id,
                        kot.disc_percentage,
                        kot.id,
                        kot.inv_id,
                        kot.inv_date,
                        kot.order_ref_no,
                        item,
                        kot.tab_label,
                        kot.table_id,
                        kot.user_id,
                        kot.is_delivered,
                        kot.isExpanded,
                        kot.kot_instance_id,
                        kot.soft_delete,
                        findKitCatStr[i]
                    )
                )
            }
            if (!ip.isNullOrBlank() && !port.isNullOrBlank()) {
                printKOTGroupTakeawayFunction(activity, instance, table, type, ip, port, person) {
                    onSuccess.onSuccess(it)
                }
            } else {
                onSuccess.onSuccess(false)
            }
        }

        fun printKitchenCatWise(
            activity: Activity,
            findKitCat: ArrayList<Int>,
            findKitCatStr: ArrayList<String>,
            index: Int,
            kot: KotInstance,
            table: String,
            type: Int,
            ips: String,
            ports: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            var ip = ips
            var port = ports
            val mIndex = index + 1
            val item = ArrayList<ItemQSR>()
            var kitchenCatName = ""
            val db = DBHelper(activity)
            val kitCatPrint = pref!!.getStr(activity, "kit_cat_print").split(",")
            val kitchenPrint = pref!!.getInt(activity, "kitchen_print")
            for (i in kot.item) {
                if (i.kitchen_cat_id == findKitCat[mIndex]) {
                    item.add(i)
                    kitchenCatName = findKitCatStr[mIndex]
                }
            }
            if (kitchenPrint == KITCHENWISEPRINT) {
                val printer = findPrinterAsPerCat(db, kitCatPrint, findKitCat[mIndex])
                if (printer != null) {
                    ip = printer.ip_add
                    port = printer.port_add
                }
            }
            if (!ip.isNullOrBlank() && !port.isNullOrBlank()) {
                val kots =
                    KotInstance(
                        item,
                        kot.kot_instance_id,
                        kot.instance,
                        kot.orderId,
                        kot.kot_order_date,
                        kot.is_delivered,
                        kot.isExpanded,
                        kot.short_name,
                        kot.soft_delete,
                        kitchenCatName
                    )
                printKOTFunction(activity, kots, table, type, ip, port, person, kitchenCatName) {
                    if (index < findKitCat.size - 2) {
                        printKitchenCatWise(
                            activity,
                            findKitCat,
                            findKitCatStr,
                            mIndex,
                            kot,
                            table,
                            3,
                            ip,
                            port,
                            person,
                            onSuccess
                        )
                    } else {
                        onSuccess.onSuccess(it)
                    }
                }
            } else {
                onSuccess.onSuccess(false)
            }
        }

        fun printKitchenCatWise(
            activity: Activity,
            findKitCat: ArrayList<Int>,
            findKitCatStr: ArrayList<String>,
            index: Int,
            kot: TakeawayOrder,
            table: String,
            type: Int,
            ips: String,
            ports: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            var ip = ips
            var port = ports
            val mIndex = index + 1
            val item = ArrayList<RestoOrderKot>()
            var kitchenCatName = ""
            val db = DBHelper(activity)
            val kitCatPrint = pref!!.getStr(activity, "kit_cat_print").split(",")
            for (i in kot.resto_order_kot) {
                if (i.kitchen_cat_id == findKitCat[mIndex]) {
                    item.add(i)
                    kitchenCatName = findKitCatStr[mIndex]
                }
            }
            if (pref!!.getInt(activity, "kitchen_print") == KITCHENWISEPRINT) {
                val printer = findPrinterAsPerCat(db, kitCatPrint, findKitCat[mIndex])
                if (printer != null) {
                    ip = printer.ip_add
                    port = printer.port_add
                }
            }
            if (!ip.isNullOrBlank() && !port.isNullOrBlank()) {
                val kots =
                    TakeawayOrder(
                        kot.cust_mobile,
                        kot.cust_name,
                        kot.cust_gst_no,
                        kot.cust_add,
                        kot.no_of_persons,
                        kot.cust_cat_id,
                        kot.disc_percentage,
                        kot.id,
                        kot.inv_id,
                        kot.inv_date,
                        kot.order_ref_no,
                        item,
                        kot.tab_label,
                        kot.table_id,
                        kot.user_id,
                        kot.is_delivered,
                        kot.isExpanded,
                        kot.kot_instance_id,
                        kot.soft_delete,
                        kitchenCatName
                    )
                printKOTFunction(activity, kots, table, type, ip, port, person, kitchenCatName) {
                    if (index < findKitCat.size - 2) {
                        printKitchenCatWise(
                            activity,
                            findKitCat,
                            findKitCatStr,
                            mIndex,
                            kot,
                            table,
                            3,
                            ip,
                            port,
                            person,
                            onSuccess
                        )
                    } else {
                        onSuccess.onSuccess(it)
                    }
                }
            } else {
                onSuccess.onSuccess(false)
            }
        }

        fun getPrintStrInvoice(activity: Activity, order: DataInvoice?): String {
            return ""
        }

        fun getFontStrikeText(item: ItemQSR, tag: String, align: String): String {
            var fontTag = "$align<font size='wide'>"
            if (item.is_edited != 0) {
                when (item.is_edited) {
                    1 -> { //qty
                        when (tag) {
                            "qty" -> {
                                fontTag = "$align<font color='bg-black' size='wide'>"
                            }
                        }
                    }

                    2 -> { //sp
                        when (tag) {
                            "sp" -> {
                                fontTag = "<font color='bg-black' size='wide'>"
                            }
                        }
                    }

                    3 -> { //both
                        when (tag) {
                            "qty" -> {
                                fontTag = "$align<font color='bg-black' size='wide'>"
                            }

                            "sp" -> {
                                fontTag = "<font color='bg-black' size='wide'>"
                            }
                        }
                    }

                    9 -> { //new item
                        when (tag) {
                            "name" -> {
                                fontTag = "$align<font size='wide'>++"
                            }

                            "qty" -> {
                                fontTag = "$align<font color='bg-black' size='wide'>"
                            }

                            "sp" -> {
                                fontTag = "<font color='bg-black' size='wide'>"
                            }
                        }
                    }
                }
            }
            if (item.soft_delete != 0) {
                when (tag) {
                    "qty" -> {
                        if (!fontTag.contains(align)) fontTag += align
                        if (!fontTag.contains("<font color='bg-black'> size='wide'")) {
                            fontTag += "<font color='bg-black' size='wide'>"
                        }
                    }
                }
            }
            return if (fontTag.isEmpty() && tag != "sp") align else fontTag
        }

        fun getFontStrikeTextType2(item: ItemQSR, tag: String, align: String): String {
            var fontTag = if (tag == "qty") align else "$align<font size='wide'>"
            if (item.is_edited != 0) {
                when (item.is_edited) {
                    1 -> { //qty
                        when (tag) {
                            "qty" -> {
                                fontTag = "$align<font color='bg-black' size='wide'>"
                            }
                        }
                    }

                    2 -> { //sp
                        when (tag) {
                            "sp" -> {
                                fontTag = "<font color='bg-black' size='wide'>"
                            }
                        }
                    }

                    3 -> { //both
                        when (tag) {
                            "qty" -> {
                                fontTag = "$align<font color='bg-black' size='wide'>"
                            }

                            "sp" -> {
                                fontTag = "<font color='bg-black' size='wide'>"
                            }
                        }
                    }

                    9 -> { //new item
                        when (tag) {
                            "name" -> {
                                fontTag = "$align<font size='wide'>++"
                            }

                            "qty" -> {
                                fontTag = "$align<font color='bg-black' size='wide'>"
                            }

                            "sp" -> {
                                fontTag = "<font color='bg-black' size='wide'>"
                            }
                        }
                    }
                }
            }
            if (item.soft_delete != 0) {
                when (tag) {
                    "qty" -> {
                        if (!fontTag.contains(align)) fontTag += align
                        if (!fontTag.contains("<font")) {
                            fontTag += "</font>"
                        }
                    }
                }
            }
            return if (fontTag.isEmpty() && tag != "sp") align else fontTag
        }


        fun getEndPoint(fontTag: String): String {
            var endPoint = ""
//                    ||fontTag.contains("<font size='tall'")||fontTag.contains("<font size='2'>")||fontTag.contains("<font size='3'>"))
            if (fontTag.contains("<font")) {
                endPoint += "</font>"
            }
            if (fontTag.contains("<u")) {
                endPoint += "</u>"
            }
//            if (fontTag.contains("<font color='bg-black'>")) {
//                endPoint += "</font>"
//            }
            return endPoint
        }

        fun printAllKOTFunction(
            activity: Activity,
            order: Orders,
            table: String,
            ip: String,
            port: String,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            var print = ""
            var str: String? = null
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            try {
                date = format.parse(order.order.order_date)
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            var i = 0
            for (kot in order.kot) {
                val top = "${order.order.order_id}/<b>$table</b>"
                val max = 20
                val different = if (top.length > max) 0 else max - top.length
                val rest = (different / 2f).roundToInt()
                i++
                print += "[C]<font size='wide'><b>KOT#$i</font></b>\n\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[C]${
                    str!!.split(
                        "/"
                    )[0]
                } <b>${str.split("/")[1]}</b>\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[C]${
                    str!!.split(
                        "/"
                    )[0]
                } <b>${str.split("/")[1]}</b>\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"
                var total = 0
                kot.order_instance_data
                for (it in kot.order_instance_data.indices) {
                    print += "[L]${it + 1}. <b> ${kot.order_instance_data[it].item_name}</b>[R]<b>${kot.order_instance_data[it].qty}</b> \n"
                    if (kot.order_instance_data[it].sp_inst != null) {
                        try {
                            if (!kot.order_instance_data[it].sp_inst.isNullOrEmpty()) {
                                val items = JSONArray(kot.order_instance_data[it].sp_inst)
                                var inst = ""
                                for (i in 0 until items.length()) {
                                    print += "[L]    <b>-${
                                        items.getString(i).trim().replace("\\/", "/")
                                    }</b>\n"
                                }
                                print += "\n"
                            } else {
                                if (kot.order_instance_data[it].sp_inst != "null") if (kot.order_instance_data[it].sp_inst != null && kot.order_instance_data[it].sp_inst.isNotEmpty()) print += "[L]    <b>-${kot.order_instance_data[it].sp_inst}</b>\n"
                                print += "\n"
                            }

                        } catch (e: JSONException) {
                            if (kot.order_instance_data[it].sp_inst != "null") {
                                if (!kot.order_instance_data[it].sp_inst.isNullOrEmpty()) {
                                    val text = kot.order_instance_data[it].sp_inst.split(",")
                                    for (te in text) {
                                        print += "[L]    <b>-${te.trim()}</b>\n"
                                    }
                                    print += "\n"
                                }
                            }
                            e.printStackTrace()
                        }
                    }
                    if (kot.order_instance_data[it].price == 0) {
                        val ss = " (Complementary)\n"
                        if (ss.isNotEmpty()) {
                            print += "[L]"
                            for (i in 0 until (it + 1).toString().length + 1) print += " "
                            print += "$ss"
                        }
                    }
                    total += kot.order_instance_data[it].qty
                }
                print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>"
            }
            val copies = pref!!.getInt(activity, "kot_copies")

            AsyncTcpEscPosPrint(activity, onSuccess, 2, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )

        }


        fun printKOTFunctionUSB(
            activity: Activity,
            kot: KotInstance,
            table: String,
            type: Int,
            usbConnection: UsbConnection,
            usbManager: UsbManager,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kot, "", table, person)
                2 -> getPrintKOTStringType2(activity, kot, "", table, person)
                else -> getPrintKOTString(activity, kot, "", table, person)
            }
            printer =
                AsyncEscPosPrinter(UsbConnection(usbManager, usbConnection.device!!), 203, 80f, 47)
            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncUsbEscPosPrint(activity, onSuccess, type, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printKOTFunctionUSB(
            activity: Activity,
            kot: TakeawayOrder,
            table: String,
            type: Int,
            usbConnection: UsbConnection,
            usbManager: UsbManager,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {

            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kot, "", table, person)
                2 -> getPrintKOTStringType2(activity, kot, "", table, person)
                else -> getPrintKOTString(activity, kot, "", table, person)
            }
            printer =
                AsyncEscPosPrinter(UsbConnection(usbManager, usbConnection.device!!), 203, 80f, 47)

            /* val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
             val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
             var date: Date? = null
             var str: String? = null
             try {
                 date = format.parse(kot.inv_date)
                 str = formatDisplay.format(date)
             } catch (e: ParseException) {
                 e.printStackTrace()
             }

             var top = "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}"
             val max = 20
             val different = if (top.length > max) 0 else max - top.length
             val rest = (different / 2f).roundToInt()

             var isEdited = false
             var isDeletedKOT = true

             for (it in kot.resto_order_kot) {
                 if (it.soft_delete == 0) isDeletedKOT = false
                 if (kot.soft_delete != 0) {
                     it.soft_delete = 2
                 } else if (it.is_edited != 0) {
                     isEdited = true
                 }
             }
             if (isDeletedKOT) {
                 kot.soft_delete = 2
                 isEdited = false
             }

             if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                 top += "..."
             }
             var print =
                 if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                     pref!!.getStr(
                         activity,
                         KeyUtils.shortName
                     )
                 }</font>\n" else ""
             print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
             print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                 str!!.split(
                     "/"
                 )[0]
             } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"

             var total = 0
             for (it in kot.resto_order_kot.indices) {
                 val item =
                     ItemQSR(
                         kot.resto_order_kot[it].kot_id,
                         kot.resto_order_kot[it].item_id,
                         kot.resto_order_kot[it].item_name,
                         "",
                         kot.resto_order_kot[it].price,
                         kot.resto_order_kot[it].qty,
                         "",
                         kot.resto_order_kot[it].is_delivered,
                         kot.resto_order_kot[it].sp_inst,
                         kot.resto_order_kot[it].soft_delete,
                         kot.resto_order_kot[it].is_edited,
                         kot.resto_order_kot[it].kitchen_cat_id,
                         kot.resto_order_kot[it].kot_ncv,
                         "",
                         "",
                         ""
                     )
                 val name = getFontStrikeText(item, "name", "[L]")
                 val qty = getFontStrikeText(item, "qty", "[R]")
                 val sp = getFontStrikeText(item, "sp", "[L]")
                 print += name + "${it + 1}. ${kot.resto_order_kot[it].item_name}" + getEndPoint(name) + qty + "<b>${
                     if (kot.resto_order_kot[it].soft_delete == 0) {
                         kot.resto_order_kot[it].qty
                     } else {
                         "XXX"
                     }
                 }</b>" + getEndPoint(qty) + "\n"
                 if (kot.resto_order_kot[it].sp_inst != null) {
                     try {
                         if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                             val items = JSONArray(kot.resto_order_kot[it].sp_inst)
                             for (i in 0 until items.length()) {
                                 print += "[L]    $sp<b>-${
                                     items.getString(i).trim().replace("\\/", "/")
                                 }</b>" + getEndPoint(sp) + "\n"
                             }
                         } else {
                             if (kot.resto_order_kot[it].sp_inst != "null") {
                                 if (kot.resto_order_kot[it].sp_inst != null && kot.resto_order_kot[it].sp_inst!!.isNotEmpty()) {
                                     print += "[L]    $sp<b>-${kot.resto_order_kot[it].sp_inst}</b>" + getEndPoint(
                                         sp
                                     ) + "\n"
                                 }
                             }
                         }

                     } catch (e: JSONException) {
                         if (kot.resto_order_kot[it].sp_inst != "null") {
                             if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                 val text = kot.resto_order_kot[it].sp_inst.split(",")
                                 for (te in text) {
                                     print += "[L]    $sp<b>-${te.trim()}</b>" + getEndPoint(sp) + "\n"
                                 }
                             }
                         }
                         e.printStackTrace()
                     }
                 }
                 if (kot.resto_order_kot[it].kot_ncv != 0) {
                     var ss = ""
                     ss += when (kot.resto_order_kot[it].kot_ncv) {
                         2 -> " (Complementary)\n"
                         3 -> " (Void)\n"
                         else -> ""
                     }

                     if (ss.isNotEmpty()) {
                         print += "[L]"
                         for (i in 0 until (it + 1).toString().length + 1) print += " "
                         print += "$ss"
                     }
                 }
                 total += kot.resto_order_kot[it].qty
             }
             print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>"
 */
            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncUsbEscPosPrint(activity, onSuccess, type, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printKOTFunction(
            activity: Activity,
            kot: KotInstance,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kot, "", table, person)
                2 -> getPrintKOTStringType2(activity, kot, "", table, person)
                else -> getPrintKOTString(activity, kot, "", table, person)
            }
            Log.e("daljdajdjsljs", "PRINT STRING 1650 printKOTFunction : $print")
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)
            val copies = pref!!.getInt(activity, "kot_copies")
            Log.e("jlfsjfs", "Print String :: $print")
            AsyncTcpEscPosPrint(activity, onSuccess, type, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printKOTGroupFunction(
            activity: Activity,
            kots: List<KotInstance>,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {

            var str: String? = null
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)
            var print = ""
            for (kot in kots) {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
                var date: Date? = null
                try {
                    date = format.parse(kot.kot_order_date)
                    str = formatDisplay.format(date)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

                var top =
                    "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}/${kot.short_name}"
                val max = 20
                val different = if (top.length > max) 0 else max - top.length
                var isEdited = false
                var isDeletedKOT = true

                for (it in kot.item) {
                    if (it.soft_delete == 0) isDeletedKOT = false
                    if (kot.soft_delete != 0) {
                        it.soft_delete = 2
                    } else if (it.is_edited != 0) {
                        isEdited = true
                    }
                }
                if (isDeletedKOT) {
                    kot.soft_delete = 2
                    isEdited = false
                }
                if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                    top += "..."
                }
                print += if (kot.soft_delete != 0) "\n[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
                print += if (isEdited) "\n[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
                print += "\n[C]<font size='wide'><b>$top</font></b>\n\n"
                print += "[L]${str!!.split("/")[0]} <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]Pax:$person\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[L]<b><font size='wide'>${kot.kitchenName}</font></b>\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[L]Item[R]Qty\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                var total = 0
                for (it in kot.item.indices) {
                    if (kot.item[it].soft_delete != 0) kot.item[it].is_edited = 0
                    val name = getFontStrikeText(kot.item[it], "name", "[L]")
                    val qty = getFontStrikeText(kot.item[it], "qty", "[R]")
                    val sp = getFontStrikeText(kot.item[it], "sp", "[L]")
                    var remain = ""
                    val itemName = if (kot.item[it].item_name.length > 19) {
                        remain = kot.item[it].item_name.substring(19, kot.item[it].item_name.length)
                        kot.item[it].item_name.substring(0, 19)
                    } else {
                        kot.item[it].item_name
                    }
                    print += name + itemName + getEndPoint(name) + qty + "<b>${
                        if (kot.item[it].soft_delete == 0) {
                            kot.item[it].qty
                        } else {
                            "XXX"
                        }
                    }</b>" + getEndPoint(qty) + "\n"
                    if (remain.isNotEmpty()) print += "$name    ${remain.trim() + getEndPoint(name)}\n"
                    val itemSp = kot.item[it]
                    var spDash = "-";
                    if (itemSp.is_edited != 0) {
                        when (itemSp.is_edited) {
                            2, 3, 9 -> { //sp
                                spDash = "--- "
                            }
                        }
                    }
                    if (itemSp.sp_inst != null && itemSp.sp_inst != "null") {
                        try {
                            if (!itemSp.sp_inst.isNullOrEmpty()) {
                                val items = JSONArray(itemSp.sp_inst)
                                for (i in 0 until items.length()) {
                                    print += "[L]    <font size='tall'>$spDash${
                                        items.getString(i).trim().replace("\\/", "/")
                                    }</font>\n"
                                }
                                print += "\n"
                            } else {
                                if (itemSp.sp_inst != "null") {
                                    if (itemSp.sp_inst != null && itemSp.sp_inst!!.isNotEmpty()) {
                                        print += "[L]    <font size='tall'>$spDash${itemSp.sp_inst}</font>\n"
                                        //+ getEndPoint(sp)
//                                    + "\n"
                                    }
                                }
                            }

                        } catch (e: JSONException) {
                            if (!itemSp.sp_inst.isNullOrEmpty()) {
                                val text = itemSp.sp_inst!!.split(",")
                                for (te in text) {
                                    print += "[L]     <font size='tall'>$spDash${te.trim()}</font>\n"
                                    //getEndPoint(sp)
//                                + "\n"
                                }
                            }
                            e.printStackTrace()
                        }
                    }
                    if (kot.item[it].kot_ncv != 0) {
                        var ss = ""
                        ss += when (kot.item[it].kot_ncv) {
                            2 -> " (Complementary)\n"
                            3 -> " (Void)\n"
                            else -> ""
                        }

                        if (ss.isNotEmpty()) {
                            print += "[L]"
                            for (i in 0 until (it + 1).toString().length + 1) print += " "
                            print += "$ss"
                        }
                    }
                    total += kot.item[it].qty
                }
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "\n[C]" + "\n[C]"
            }
            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncTcpEscPosPrint(activity, onSuccess, type, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun getPrintKOTString(
            activity: Activity, kots: List<TakeawayOrder>,
            table: String, person: Int
        ): String {
            var print = ""
            for (kot in kots) {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
                var date: Date? = null
                var str: String? = null
                try {
                    date = format.parse(kot.inv_date)
                    str = formatDisplay.format(date)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

                var top =
                    "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}"
                val max = 20
                val different = if (top.length > max) 0 else max - top.length
                val rest = (different / 2f).roundToInt()

                var isEdited = false
                var isDeletedKOT = true

                for (it in kot.resto_order_kot) {
                    if (it.soft_delete == 0) isDeletedKOT = false
                    if (kot.soft_delete != 0) {
                        it.soft_delete = 2
                    } else if (it.is_edited != 0) {
                        isEdited = true
                    }
                }
                if (isDeletedKOT) {
                    kot.soft_delete = 2
                    isEdited = false
                }

                if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                    top += "..."
                }
                print += if (kot.soft_delete != 0) "\n[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
                print += if (isEdited) "\n[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""

                print += "\n[C]<font size='wide'><b>$top</font></b>\n\n"
                print += "[L]${str!!.split("/")[0]} <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]$person\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[L]<b><font size='wide'>${kot.kitchenCat}</font></b>\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[L]Item[R]Qty\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"

                var total = 0
                for (it in kot.resto_order_kot.indices) {
                    val item =
                        ItemQSR(
                            kot.resto_order_kot[it].kot_id,
                            kot.resto_order_kot[it].item_id,
                            kot.resto_order_kot[it].item_name,
                            "",
                            kot.resto_order_kot[it].price,
                            kot.resto_order_kot[it].qty,
                            "",
                            kot.resto_order_kot[it].is_delivered,
                            kot.resto_order_kot[it].sp_inst,
                            kot.resto_order_kot[it].soft_delete,
                            kot.resto_order_kot[it].is_edited,
                            kot.resto_order_kot[it].kitchen_cat_id,
                            kot.resto_order_kot[it].kot_ncv,
                            "",
                            "",
                            ""
                        )
                    if (item.soft_delete == 1) {
                        break
                    }
                    val name = getFontStrikeText(item, "name", "[L]")
                    val qty = getFontStrikeText(item, "qty", "[R]")
                    // val sp = getFontStrikeText(item, "sp", "[L]")
                    val itemSp = kot.resto_order_kot[it]
                    var sp = "-";
                    if (itemSp.is_edited != 0) {
                        when (itemSp.is_edited) {
                            2, 3, 9 -> { //sp
                                sp = "--- "
                            }
                        }
                    }
                    print += name + "${it + 1}. ${kot.resto_order_kot[it].item_name}" + getEndPoint(
                        name
                    ) + qty + "<b>${
                        if (kot.resto_order_kot[it].soft_delete == 0) {
                            kot.resto_order_kot[it].qty
                        } else {
                            "XXX"
                        }
                    }</b>" + getEndPoint(qty) + "\n"
                    if (kot.resto_order_kot[it].sp_inst != null) {
                        try {
                            if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                val items = JSONArray(kot.resto_order_kot[it].sp_inst)
                                for (i in 0 until items.length()) {
                                    print += "[L]    $sp<b>${
                                        items.getString(i).trim().replace("\\/", "/")
                                    }</b>" + getEndPoint(sp) + "\n"
                                }
                                print += "\n"
                            } else {
                                if (kot.resto_order_kot[it].sp_inst != "null") {
                                    if (kot.resto_order_kot[it].sp_inst != null && kot.resto_order_kot[it].sp_inst!!.isNotEmpty()) {
                                        print += "[L]    $sp<b>${kot.resto_order_kot[it].sp_inst}</b>" + getEndPoint(
                                            sp
                                        ) + "\n"
                                    }
                                    print += "\n"
                                }
                            }

                        } catch (e: JSONException) {
                            if (kot.resto_order_kot[it].sp_inst != "null") {
                                if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                    val text = kot.resto_order_kot[it].sp_inst.split(",")
                                    for (te in text) {
                                        print += "[L]    $sp<b>${te.trim()}</b>" + getEndPoint(sp) + "\n"
                                    }
                                    print += "\n"
                                }
                            }
                            e.printStackTrace()
                        }
                    }
                    if (kot.resto_order_kot[it].kot_ncv != 0) {
                        var ss = ""
                        ss += when (kot.resto_order_kot[it].kot_ncv) {
                            2 -> " (Complementary)\n"
                            3 -> " (Void)\n"
                            else -> ""
                        }

                        if (ss.isNotEmpty()) {
                            print += "[L]"
                            for (i in 0 until (it + 1).toString().length + 1) print += " "
                            print += "$ss"
                        }
                    }
                    total += kot.resto_order_kot[it].qty
                }
                print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>\n\n"
            }
            return print
        }

        fun getPrintKOTStringType2(
            activity: Activity, kots: List<TakeawayOrder>,
            table: String, person: Int
        ): String {
            var print = ""
            for (kot in kots) {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
                var date: Date? = null
                var str: String? = null
                try {
                    date = format.parse(kot.inv_date)
                    str = formatDisplay.format(date)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

                var top =
                    "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}"
                val max = 20
                val different = if (top.length > max) 0 else max - top.length
                val rest = (different / 2f).roundToInt()

                var isEdited = false
                var isDeletedKOT = true

                for (it in kot.resto_order_kot) {
                    if (it.soft_delete == 0) isDeletedKOT = false
                    if (kot.soft_delete != 0) {
                        it.soft_delete = 2
                    } else if (it.is_edited != 0) {
                        isEdited = true
                    }
                }
                if (isDeletedKOT) {
                    kot.soft_delete = 2
                    isEdited = false
                }

                if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                    top += "..."
                }
                print += if (kot.soft_delete != 0) "\n[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
                print += if (isEdited) "\n[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""

                print += "\n[C]<font size='wide'><b>$top</font></b>\n\n"
                print += "[L]${str!!.split("/")[0]} <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]$person\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[L]<b><font size='wide'>${kot.kitchenCat}</font></b>\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"
                print += "[L]Item[R]Qty\n"
                print += "[C]<b><font size='wide'>------------------------</font></b>\n"

                var total = 0
                for (it in kot.resto_order_kot.indices) {
                    val item =
                        ItemQSR(
                            kot.resto_order_kot[it].kot_id,
                            kot.resto_order_kot[it].item_id,
                            kot.resto_order_kot[it].item_name,
                            "",
                            kot.resto_order_kot[it].price,
                            kot.resto_order_kot[it].qty,
                            "",
                            kot.resto_order_kot[it].is_delivered,
                            kot.resto_order_kot[it].sp_inst,
                            kot.resto_order_kot[it].soft_delete,
                            kot.resto_order_kot[it].is_edited,
                            kot.resto_order_kot[it].kitchen_cat_id,
                            kot.resto_order_kot[it].kot_ncv,
                            "",
                            "",
                            ""
                        )
                    if (item.soft_delete == 1) {
                        break
                    }
                    var space = ""
                    for (i in 0 until 7 - (kot.resto_order_kot[it].qty).toString().length) space += " "

                    val name = getFontStrikeText(item, "name", "")
                    val qty = getFontStrikeText(item, "qty", "[R]")
                    val sp = getFontStrikeText(item, "sp", "[L]")
                    var remain = ""
                    val itemName = if (kot.resto_order_kot[it].item_name.length > 19) {
                        remain = kot.resto_order_kot[it].item_name.substring(
                            19,
                            kot.resto_order_kot[it].item_name.length
                        )
                        kot.resto_order_kot[it].item_name.substring(0, 19).uppercase()
                    } else {
                        kot.resto_order_kot[it].item_name.uppercase()
                    }
                    print += qty + "<b>${
                        if (kot.resto_order_kot[it].soft_delete == 0) {
                            kot.resto_order_kot[it].qty
                        } else {
                            "XXX"
                        }
                    }</b>" + getEndPoint(qty) + space + name + itemName + getEndPoint(name) + "\n"
                    if (remain.isNotEmpty()) print += "$name    ${remain.trim() + getEndPoint(name)}\n"
                    val itemSp = kot.resto_order_kot[it]
                    var spDash = "- "
                    if (itemSp.is_edited != 0) {
                        when (itemSp.is_edited) {
                            2, 3, 9 -> { //sp
                                spDash = "--- "
                            }
                        }
                    }
                    if (kot.resto_order_kot[it].sp_inst != null) {
                        try {
                            if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                val items = JSONArray(kot.resto_order_kot[it].sp_inst)
                                for (i in 0 until items.length()) {
                                    print += "[L]$space   $spDash<b>${
                                        items.getString(i).trim().replace("\\/", "/")
                                    }</b>\n"
                                }
                                print += "\n"
                            } else {
                                if (kot.resto_order_kot[it].sp_inst != "null") {
                                    if (kot.resto_order_kot[it].sp_inst != null && kot.resto_order_kot[it].sp_inst.isNotEmpty()) {
                                        print += "[L]$space   $spDash<b>${kot.resto_order_kot[it].sp_inst}</b>\n"
                                    }
                                    print += "\n"
                                }
                            }

                        } catch (e: JSONException) {
                            if (kot.resto_order_kot[it].sp_inst != "null") {
                                if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                    val text = kot.resto_order_kot[it].sp_inst.split(",")
                                    for (te in text) {
                                        print += "[L]$space   $spDash<b>${te.trim()}</b>\n"
                                    }
                                    print += "\n"
                                }
                            }
                            e.printStackTrace()
                        }
                    }
                    if (kot.resto_order_kot[it].kot_ncv != 0) {
                        var ss = ""
                        ss += when (kot.resto_order_kot[it].kot_ncv) {
                            2 -> " (Complementary)\n"
                            3 -> " (Void)\n"
                            else -> ""
                        }

                        if (ss.isNotEmpty()) {
                            print += "[L]"
                            for (i in 0 until (it + 1).toString().length + 1) print += " "
                            print += "$space $ss"
                        }
                    }
                    total += kot.resto_order_kot[it].qty
                }
                print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>\n\n"
            }
            return print
        }

        fun getPrintKOTString(
            activity: Activity, kot: TakeawayOrder, kitchenName: String,
            table: String, person: Int
        ): String {
            var print = ""
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            var str: String? = null
            try {
                date = format.parse(kot.inv_date)
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var top = "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}"
            val max = 20
            val different = if (top.length > max) 0 else max - top.length
            val rest = (different / 2f).roundToInt()

            var isEdited = false
            var isDeletedKOT = true

            for (it in kot.resto_order_kot) {
                if (it.soft_delete == 0) isDeletedKOT = false
                if (kot.soft_delete != 0) {
                    it.soft_delete = 2
                } else if (it.is_edited != 0) {
                    isEdited = true
                }
            }
            if (isDeletedKOT) {
                kot.soft_delete = 2
                isEdited = false
            }

            if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                top += "..."
            }
            print =
                if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
            print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
            print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                str!!.split(
                    "/"
                )[0]
            } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"

            var total = 0
            for (it in kot.resto_order_kot.indices) {
                val item =
                    ItemQSR(
                        kot.resto_order_kot[it].kot_id,
                        kot.resto_order_kot[it].item_id,
                        kot.resto_order_kot[it].item_name,
                        "",
                        kot.resto_order_kot[it].price,
                        kot.resto_order_kot[it].qty,
                        "",
                        kot.resto_order_kot[it].is_delivered,
                        kot.resto_order_kot[it].sp_inst,
                        kot.resto_order_kot[it].soft_delete,
                        kot.resto_order_kot[it].is_edited,
                        kot.resto_order_kot[it].kitchen_cat_id,
                        kot.resto_order_kot[it].kot_ncv,
                        "",
                        "",
                        ""
                    )
                if (item.soft_delete == 1) {
                    break
                }
                val name = getFontStrikeText(item, "name", "[L]")
                val qty = getFontStrikeText(item, "qty", "[R]")
                //val sp = getFontStrikeText(item, "sp", "[L]")
                val itemSp = kot.resto_order_kot[it]
                var sp = "-";
                if (itemSp.is_edited != 0) {
                    when (itemSp.is_edited) {
                        2, 3, 9 -> { //sp
                            sp = "--- "
                        }
                    }
                }
                print += name + "${it + 1}. ${kot.resto_order_kot[it].item_name}" + getEndPoint(name) + qty + "<b>${
                    if (kot.resto_order_kot[it].soft_delete == 0) {
                        kot.resto_order_kot[it].qty
                    } else {
                        "XXX"
                    }
                }</b>" + getEndPoint(qty) + "\n"
                if (kot.resto_order_kot[it].sp_inst != null) {
                    try {
                        if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                            val items = JSONArray(kot.resto_order_kot[it].sp_inst)
                            for (i in 0 until items.length()) {
                                print += "[L]    $sp<b>${
                                    items.getString(i).trim().replace("\\/", "/")
                                }</b>" + getEndPoint(sp) + "\n"
                            }
                        } else {
                            if (kot.resto_order_kot[it].sp_inst != "null") {
                                if (kot.resto_order_kot[it].sp_inst != null && kot.resto_order_kot[it].sp_inst.isNotEmpty()) {
                                    print += "[L]    $sp<b>${kot.resto_order_kot[it].sp_inst}</b>" + getEndPoint(
                                        sp
                                    ) + "\n"
                                }
                            }
                        }

                    } catch (e: JSONException) {
                        if (kot.resto_order_kot[it].sp_inst != "null") {
                            if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                val text = kot.resto_order_kot[it].sp_inst.split(",")
                                for (te in text) {
                                    print += "[L]    $sp<b>${te.trim()}</b>" + getEndPoint(sp) + "\n"
                                }
                            }
                        }
                        e.printStackTrace()
                    }
                }
                if (kot.resto_order_kot[it].kot_ncv != 0) {
                    var ss = ""
                    ss += when (kot.resto_order_kot[it].kot_ncv) {
                        1 -> " (No Charge)\n"
                        2 -> " (Complementary)\n"
                        3 -> " (Void)\n"
                        else -> ""
                    }

                    if (ss.isNotEmpty()) {
                        print += "[L]"
                        for (i in 0 until (it + 1).toString().length + 1) print += " "
                        print += "$ss"
                    }
                }
                total += kot.resto_order_kot[it].qty
            }
            print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>"
            if (kitchenName.isNotEmpty())
                print += "[R]<b>$kitchenName</b>"
            print += "\n[C]" + "\n[C]"
            return print
        }

        fun getPrintKOTStringType2(
            activity: Activity, kot: TakeawayOrder, kitchenName: String,
            table: String, person: Int
        ): String {
            var print = ""
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            var str: String? = null
            try {
                date = format.parse(kot.inv_date)
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var top = "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}"
            val max = 20
            val different = if (top.length > max) 0 else max - top.length
            val rest = (different / 2f).roundToInt()

            var isEdited = false
            var isDeletedKOT = true

            for (it in kot.resto_order_kot) {
                if (it.soft_delete == 0) isDeletedKOT = false
                if (kot.soft_delete != 0) {
                    it.soft_delete = 2
                } else if (it.is_edited != 0) {
                    isEdited = true
                }
            }
            if (isDeletedKOT) {
                kot.soft_delete = 2
                isEdited = false
            }

            if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                top += "..."
            }
            print =
                if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
            print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
            print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                str!!.split(
                    "/"
                )[0]
            } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"

            var total = 0
            for (it in kot.resto_order_kot.indices) {
                val item =
                    ItemQSR(
                        kot.resto_order_kot[it].kot_id,
                        kot.resto_order_kot[it].item_id,
                        kot.resto_order_kot[it].item_name,
                        "",
                        kot.resto_order_kot[it].price,
                        kot.resto_order_kot[it].qty,
                        "",
                        kot.resto_order_kot[it].is_delivered,
                        kot.resto_order_kot[it].sp_inst,
                        kot.resto_order_kot[it].soft_delete,
                        kot.resto_order_kot[it].is_edited,
                        kot.resto_order_kot[it].kitchen_cat_id,
                        kot.resto_order_kot[it].kot_ncv,
                        "",
                        "",
                        ""
                    )
                if (item.soft_delete == 1) {
                    break
                }
                var space = ""

                for (i in 0 until 7 - (kot.resto_order_kot[it].qty).toString().length) space += " "

                val name = getFontStrikeTextType2(
                    item,
                    "name",
                    ""
                )
                val qty = getFontStrikeText(item, "qty", "[R]")
                val sp = getFontStrikeText(item, "sp", "[L]")
                var remain = ""
                val itemName = if (kot.resto_order_kot[it].item_name.length > 40) {
                    remain = kot.resto_order_kot[it].item_name.substring(
                        40,
                        kot.resto_order_kot[it].item_name.length
                    )
                    kot.resto_order_kot[it].item_name.substring(0, 40).uppercase()
                } else {
                    kot.resto_order_kot[it].item_name.uppercase()
                }
                print += qty + "<b>${
                    if (kot.resto_order_kot[it].soft_delete == 0) {
                        kot.resto_order_kot[it].qty
                    } else {
                        "XXX"
                    }
                }</b>" + getEndPoint(qty) + space + name + itemName + getEndPoint(name) + "\n"
                val itemSp = kot.resto_order_kot[it]
                var spDash = "- "
                if (itemSp.is_edited != 0) {
                    when (itemSp.is_edited) {
                        2, 3, 9 -> { //sp
                            spDash = "--- "
                        }
                    }
                }
                if (kot.resto_order_kot[it].sp_inst != null) {
                    try {
                        if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                            val items = JSONArray(kot.resto_order_kot[it].sp_inst)
                            for (i in 0 until items.length()) {
                                print += "[L]$space   $spDash<b>${
                                    items.getString(i).trim().replace("\\/", "/")
                                }</b>\n"
                            }
                        } else {
                            if (kot.resto_order_kot[it].sp_inst != "null") {
                                if (kot.resto_order_kot[it].sp_inst != null && kot.resto_order_kot[it].sp_inst.isNotEmpty()) {
                                    print += "[L]$space   $spDash<b>${kot.resto_order_kot[it].sp_inst}</b>\n"
                                }
                            }
                        }

                    } catch (e: JSONException) {
                        if (kot.resto_order_kot[it].sp_inst != "null") {
                            if (!kot.resto_order_kot[it].sp_inst.isNullOrEmpty()) {
                                val text = kot.resto_order_kot[it].sp_inst.split(",")
                                for (te in text) {
                                    print += "[L]$space   $spDash<b>${te.trim()}</b>\n"
                                }
                            }
                        }
                        e.printStackTrace()
                    }
                }
                if (kot.resto_order_kot[it].kot_ncv != 0) {
                    var ss = ""
                    ss += when (kot.resto_order_kot[it].kot_ncv) {
                        1 -> " (No Charge)\n"
                        2 -> " (Complementary)\n"
                        3 -> " (Void)\n"
                        else -> ""
                    }

                    if (ss.isNotEmpty()) {
                        print += "[L]"
                        for (i in 0 until (it + 1).toString().length + 1) print += " "
                        print += "$space $ss"
                    }
                }
                total += kot.resto_order_kot[it].qty
            }
            print += "[L]\n[C]<b><font size='wide'>------------------------</font></b>"
            if (kitchenName.isNotEmpty())
                print += "[R]<b>$kitchenName</b>"
            print += "\n[C]" + "\n[C]"
            return print
        }

        fun getPrintKOTString(
            activity: Activity, kot: KotInstance, kitchenCatName: String,
            table: String, person: Int
        ): String {
            var print = ""
            var str: String? = null
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            try {
                date = format.parse(kot.kot_order_date)
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var top =
                "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}/${kot.short_name}"
            val max = 20
            val different = if (top.length > max) 0 else max - top.length
            var isEdited = false
            var isDeletedKOT = true

            for (it in kot.item) {
                if (it.soft_delete == 0) isDeletedKOT = false
                if (kot.soft_delete != 0) {
                    it.soft_delete = 2
                } else if (it.is_edited != 0) {
                    isEdited = true
                }
            }
            if (isDeletedKOT) {
                kot.soft_delete = 2
                isEdited = false
            }
            if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                top += "..."
            }
            print =
                if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
            print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
            print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                str!!.split(
                    "/"
                )[0]
            } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]Pax:$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Item[R]Qty\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"
            var total = 0
            for (it in kot.item.indices) {
                if (kot.item[it].soft_delete == 1) {
                    break
                }
                if (kot.item[it].soft_delete != 0) kot.item[it].is_edited = 0
                val name = getFontStrikeText(
                    kot.item[it],
                    "name",
                    "[L]"
                )
                val qty = getFontStrikeText(
                    kot.item[it],
                    "qty",
                    "[R]"
                )
                var remain = ""
                val itemName = if (kot.item[it].item_name.length > 19) {
                    remain = kot.item[it].item_name.substring(19, kot.item[it].item_name.length)
                    kot.item[it].item_name.substring(0, 19).uppercase()
                } else {
                    kot.item[it].item_name.uppercase()
                }
                print += name + itemName + getEndPoint(name) + qty + "<b>${
                    if (kot.item[it].soft_delete == 0) {
                        kot.item[it].qty
                    } else {
                        "XX"
                    }
                }</b>" + getEndPoint(qty) + "\n"
                if (remain.isNotEmpty()) print += "$name${remain.trim() + getEndPoint(name)}\n"

                val itemSp = kot.item[it]
                var spDash = "-";
                if (itemSp.is_edited != 0) {
                    when (itemSp.is_edited) {
                        2, 3, 9 -> { //sp
                            spDash = "-- "
                        }
                    }
                }
                if (itemSp.sp_inst != null && itemSp.sp_inst != "null") {
                    try {
                        if (!itemSp.sp_inst.isNullOrEmpty()) {
                            val items = JSONArray(itemSp.sp_inst)
                            for (i in 0 until items.length()) {
                                print += "[L]   $spDash<b>${
                                    items.getString(i).trim().replace("\\/", "/")
                                }</b>\n"
                            }
                            print += "\n"
                        } else {
                            if (itemSp.sp_inst != "null") {
                                if (itemSp.sp_inst != null && itemSp.sp_inst!!.isNotEmpty()) {
                                    print += "[L]   $spDash<b>${itemSp.sp_inst}</b>\n"
                                    //+ getEndPoint(sp)
//                                    + "\n"
                                }
                            }
                        }

                    } catch (e: JSONException) {
                        if (!itemSp.sp_inst.isNullOrEmpty()) {
                            val text = itemSp.sp_inst!!.split(",")
                            for (te in text) {
                                print += "[L]   $spDash<b>${te.trim()}</b>\n"
                                //getEndPoint(sp)
//                                + "\n"
                            }
                        }
                        e.printStackTrace()
                    }
                }
                if (kot.item[it].kot_ncv != 0) {
                    var ss = ""
                    ss += when (kot.item[it].kot_ncv) {
                        1 -> " (No Charge)\n"
                        2 -> " (Complementary)\n"
                        3 -> " (Void)\n"
                        else -> ""
                    }

                    if (ss.isNotEmpty()) {
                        print += "[L]"
                        for (i in 0 until (it + 1).toString().length + 1) print += " "
                        print += "$ss"
                    }
                }
                total += kot.item[it].qty
                print += "[L]\n"
            }
            print += "[C]<b><font size='wide'>------------------------</font></b>\n"
            if (kitchenCatName.isNotEmpty())
                print += "[R]<b>$kitchenCatName</b>"
            print += "\n[C]" + "\n[C]"
            return print
        }

        fun getPrintKOTStringType2(
            activity: Activity, kot: KotInstance, kitchenCatName: String,
            table: String, person: Int
        ): String {
            var print = ""
            var str: String? = null
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val format2 = SimpleDateFormat("yyyy-MM-dd")
            val formatDisplay = SimpleDateFormat("dd-MMM/HH:mm")
            var date: Date? = null
            try {
                date = format.parse(kot.kot_order_date)
                Log.e("jlfsjfs", "kot_order_date :: $date :: ${kot.kot_order_date}")
                str = formatDisplay.format(date)
            } catch (e: ParseException) {
                date = format2.parse(kot.kot_order_date)
                Log.e("jlfsjfs", "kot_order_date catch :: $date :: ${kot.kot_order_date}")
                str = formatDisplay.format(date)
                e.printStackTrace()
            }

            var top =
                "${kot.kot_instance_id.split("_")[0]}/${kot.kot_instance_id.split("_")[2]}/${kot.short_name}"
            val max = 20
            val different = if (top.length > max) 0 else max - top.length
            var isEdited = false
            var isDeletedKOT = true

            for (it in kot.item) {
                if (it.soft_delete == 0) isDeletedKOT = false
                if (kot.soft_delete != 0) {
                    it.soft_delete = 2
                } else if (it.is_edited != 0) {
                    isEdited = true
                }
            }
            if (isDeletedKOT) {
                kot.soft_delete = 2
                isEdited = false
            }
            if (kot.kot_instance_id.split("_")[2].toInt() > 1) {
                top += "..."
            }
            print =
                if (kot.soft_delete != 0) "[C]<font size='wide'>Deleted By - ${
                    pref!!.getStr(
                        activity,
                        KeyUtils.shortName
                    )
                }</font>\n" else ""
            print += if (isEdited) "[C]<font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>   <font color='bg-black' size='wide'> </font>\n" else ""
            print += "[C]<b><font size='wide'>------------------------</font></b>\n" + "[C]<font size='wide'><b>$top</font></b>\n\n" + "[L]${
                str!!.split(
                    "/"
                )[0]
            } <b>${str.split("/")[1]}</b>[C]<b>$table</b>[R]Pax:$person\n" + "[C]<b><font size='wide'>------------------------</font></b>\n" + "[L]Qty   Item\n" + "[C]<b><font size='wide'>------------------------</font></b>\n"
            var total = 0
            for (it in kot.item.indices) {
                if (kot.item[it].soft_delete == 1) {
                    break
                }
                if (kot.item[it].soft_delete != 0) kot.item[it].is_edited = 0
                var space = ""
                for (i in 0 until 7 - (kot.item[it].qty).toString().length) space += " "

                val name = getFontStrikeTextType2(
                    kot.item[it],
                    "name",
                    ""
                )
                val qty = getFontStrikeTextType2(
                    kot.item[it],
                    "qty",
                    "[L]"
                )
                var remain = ""
                val itemName = if (kot.item[it].item_name.length > 19) {
                    remain = kot.item[it].item_name.substring(19, kot.item[it].item_name.length)
                    kot.item[it].item_name.substring(0, 19).uppercase()
                } else {
                    kot.item[it].item_name.uppercase()
                }
                print += qty + "<b>${
                    if (kot.item[it].soft_delete == 0) {
                        kot.item[it].qty
                    } else {
                        "XX"
                    }
                }</b>" + getEndPoint(qty) + space + name + itemName + getEndPoint(name) + "\n"
                if (remain.isNotEmpty()) print += "$name    ${remain.trim() + getEndPoint(name)}\n"
                val itemSp = kot.item[it]
                var spDash = "- "
                if (itemSp.is_edited != 0) {
                    when (itemSp.is_edited) {
                        2, 3, 9 -> { //sp
                            spDash = "-- "
                        }
                    }
                }
                if (itemSp.sp_inst != null && itemSp.sp_inst != "null") {
                    try {
                        if (!itemSp.sp_inst.isNullOrEmpty()) {
                            val items = JSONArray(itemSp.sp_inst)
                            for (i in 0 until items.length()) {
                                print += "[L]$space   $spDash<b>${
                                    items.getString(i).trim().replace("\\/", "/")
                                }</b>\n"
                            }
                            print += "\n"
                        } else {
                            if (itemSp.sp_inst != "null") {
                                if (itemSp.sp_inst != null && itemSp.sp_inst!!.isNotEmpty()) {
                                    print += "[L]$space   $spDash<b>${itemSp.sp_inst}</b>\n"
                                }
                            }
                        }

                    } catch (e: JSONException) {
                        if (!itemSp.sp_inst.isNullOrEmpty()) {
                            val text = itemSp.sp_inst!!.split(",")
                            for (te in text) {
                                print += "[L]$space   $spDash<b>${te.trim()}</b>\n"

                            }
                        }
                        e.printStackTrace()
                    }
                }
                if (kot.item[it].kot_ncv != 0) {
                    var ss = ""
                    ss += when (kot.item[it].kot_ncv) {
                        1 -> " (No Charge)\n"
                        2 -> " (Complementary)\n"
                        3 -> " (Void)\n"
                        else -> ""
                    }

                    if (ss.isNotEmpty()) {
                        print += "[L]"
                        for (i in 0 until (it + 1).toString().length + 1) print += " "
                        print += "$space $ss"
                    }
                }
                total += kot.item[it].qty
                print += "[L]\n"
            }
            print += "[C]<b><font size='wide'>------------------------</font></b>\n"
            if (kitchenCatName.isNotEmpty())
                print += "[R]<b>$kitchenCatName</b>"
            print += "\n[C]" + "\n[C]"
            return print
        }

        fun printKOTGroupTakeawayFunction(
            activity: Activity,
            kots: List<TakeawayOrder>,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kots, table, person)
                2 -> getPrintKOTStringType2(activity, kots, table, person)
                else -> getPrintKOTString(activity, kots, table, person)
            }
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)

            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncTcpEscPosPrint(activity, onSuccess, 3, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printKOTFunction(
            activity: Activity,
            kot: KotInstance,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            kitchenCatName: String,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kot, kitchenCatName, table, person)
                2 -> getPrintKOTStringType2(activity, kot, kitchenCatName, table, person)
                else -> getPrintKOTString(activity, kot, kitchenCatName, table, person)
            }
            Log.e("daljdajdjsljs", "PRINT STRING 2773 printKOTFunction : $print")
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)
            val copies = pref!!.getInt(activity, "kot_copies")

            AsyncTcpEscPosPrint(activity, onSuccess, type, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }


        fun printKOTFunction(
            activity: Activity,
            kot: TakeawayOrder,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kot, "", table, person)
                2 -> getPrintKOTStringType2(activity, kot, "", table, person)
                else -> getPrintKOTString(activity, kot, "", table, person)
            }
            Log.e("daljdajdjsljs", "PRINT STRING 2800 printKOTFunction : $print")
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)
            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncTcpEscPosPrint(activity, onSuccess, 3, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printKOTFunction(
            activity: Activity,
            kot: TakeawayOrder,
            table: String,
            type: Int,
            ip: String,
            port: String,
            person: Int,
            kitchenCatName: String,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            val print = when (pref!!.getInt(activity, "kot_design_type")) {
                1 -> getPrintKOTString(activity, kot, kitchenCatName, table, person)
                2 -> getPrintKOTStringType2(activity, kot, kitchenCatName, table, person)
                else -> getPrintKOTString(activity, kot, kitchenCatName, table, person)
            }
            Log.e("daljdajdjsljs", "PRINT STRING 2823 printKOTFunction : $print")
            printer =
                AsyncEscPosPrinter(
                    TcpConnection(ip, port.toInt()),
                    203,
                    80f,
                    47
                )
            val copies = pref!!.getInt(activity, "kot_copies")
            AsyncTcpEscPosPrint(activity, onSuccess, 3, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printInvoiceFunctionUSB(
            activity: Activity,
            order: DataInvoice?,
            usbConnection: UsbConnection,
            usbManager: UsbManager,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            printer =
                AsyncEscPosPrinter(UsbConnection(usbManager, usbConnection.device!!), 203, 80f, 47)
            val print = getPrintStrInvoice(activity, order)
            val copies = pref!!.getInt(activity, "invoice_copies")
            AsyncTcpEscPosPrint(activity, onSuccess, 2, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun printInvoiceFunction(
            activity: Activity,
            order: DataInvoice?,
            ip: String,
            port: String,
            onSuccess: AsyncEscPosPrint.OnSuccess
        ) {
            printer = AsyncEscPosPrinter(TcpConnection(ip, port.toInt()), 203, 80f, 47)
            val print = getPrintStrInvoice(activity, order)

            val copies = pref!!.getInt(activity, "invoice_copies")
            AsyncTcpEscPosPrint(activity, onSuccess, 2, if (copies == 0) 1 else copies).execute(
                getAsyncEscPosPrinter(print)
            )
        }

        fun getName(name: String): String {
            var print = ""
            if (name.length > 24) {
                val word = name.split(" ")
                var count = 0
                var words = ""
                var index = 0
                while (index < word.size) {
                    val w = word[index]
                    if ((count + w.length) < 24) {
                        words += "$w "
                        count = words.length
                        index++
                    } else {
                        print += "[C]<b><font size='wide'>${words.trim()}</font></b>\n"
                        count = 0
                        words = ""
                    }
                }
                if (words.isNotEmpty()) {
                    print += "[C]<b><font size='wide'>${words.trim()}</font></b>\n"
                }
            } else {
                print += "[C]<b><font size='wide'>${name}</font></b>\n"
            }
            return print
        }

        fun getFormattedAddress(address: String): String {
            val _48Ch = address.substring(0, 48)
            val lastWord = _48Ch.lastIndexOf(" ")
            val breakAddress = address.substring(0, lastWord)
            val secAddress = address.substring(lastWord, address.length)

            if (secAddress.length > 48) {
                formatedAddress += "[C]${breakAddress}\n"
                formatedAddress = getFormattedAddress(secAddress)
            } else {
                formatedAddress += "[C]${breakAddress}\n[C]${secAddress}\n"
            }
            return formatedAddress
        }

        private fun checkStr(name: String, qty: String, rate: String, price: String): String {
            val nameMax = 27
            val qtyMax = 3
            val rateMax = 8
            val priceMax = 8
            var itemName = name
            var itemNameRemain = ""
            if (itemName.length > nameMax) {
                itemName = name.substring(0, 25)
                itemNameRemain = name.substring(25, name.length)
            }
            val countName = if (itemName.length > nameMax) 0 else nameMax - itemName.length
            val countRate = if (rate.length > rateMax) 0 else rateMax - rate.length
            val countQty = if (qty.length > qtyMax) 0 else qtyMax - qty.length
            val countPrice = if (price.length > priceMax) 0 else priceMax - price.length

            var returnString = itemName
            for (i in 0..countName) {
                returnString += " "
            }
            returnString += checkCenter(countQty, qty, "center")
            returnString += checkCenter(countRate, rate, "center")
            returnString += checkCenter(countPrice, price, "right")
            if (itemNameRemain.isNotEmpty()) {
                returnString += "\n[L]   ${itemNameRemain.trim()}"
            }
            return returnString
        }

        private fun checkCenter(max: Int, text: String, align: String): String {
            var returnString = ""
            if (max != 0) {
                if (align == "center") {
                    val div = max / 2

                    for (i in 0 until div) {
                        returnString += " "
                    }
                    returnString += text
                    for (i in 0 until (max - div)) {
                        returnString += " "
                    }
                } else {
                    for (i in 0 until max) {
                        returnString += " "
                    }
                    returnString += text
                }
            } else {
                returnString += text
            }
            return returnString
        }

        /**
         * Asynchronous printing
         */
        @SuppressLint("SimpleDateFormat")
        fun getAsyncEscPosPrinter(print: String): AsyncEscPosPrinter {
            return printer!!.setTextToPrint(print)
        }

        @SuppressLint("SimpleDateFormat")
        fun getAsyncEscBluetoothPosPrinter(print: String): AsyncEscPosPrinter {
            val printer = AsyncEscPosPrinter(null, 203, 80f, 47)
            return printer.setTextToPrint(print)
        }

        fun showProgressDialog(context: Activity) {
            try {
                if (dialog != null && dialog!!.isShowing) try {
                    dialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    dialog = Dialog(context)
                    dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog!!.setContentView(R.layout.dialog_progress)

                    val img = dialog!!.findViewById<ImageView>(R.id.img)
                    Glide.with(context).asGif().load(R.raw.loading).into(img)

                    Objects.requireNonNull(dialog!!.window)
                        ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog!!.setCancelable(false)
                    dialog!!.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun dismissProgressDialog() {
            try {
                if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                dialog = null
            }
        }

        fun printUsb(
            mContext: Context, findPrinter: FindPrinter
        ) {
            val usbConnection = UsbPrintersConnections.selectFirstConnected(mContext)
            val usbManager = mContext.getSystemService(Context.USB_SERVICE) as UsbManager?
            if (usbConnection == null) {
                findPrinter.onPrinterNotFond()
            } else {
                if (usbConnection.device == null) {
                    findPrinter.onPrinterNotFond()
                }
            }
            if (usbManager == null) {
                findPrinter.onPrinterNotFond()
            } else {
                if (usbManager.deviceList == null) {
                    findPrinter.onPrinterNotFond()
                }
            }
            if (usbConnection == null || usbManager == null) {
                return
            }
            val permissionIntent = PendingIntent.getBroadcast(
                mContext,
                0,
                Intent(Utils.ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_MUTABLE
            )
            if (!usbManager.hasPermission(usbConnection.device)) {
                findPrinter.onPrinterPermission(usbConnection, usbManager, permissionIntent)
            } else {
                findPrinter.onPrinterFond(usbConnection, usbManager)
            }
        }

        val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (Utils.ACTION_USB_PERMISSION == action) {
                    synchronized(this) {
                        val usbManager =
                            context.getSystemService(Context.USB_SERVICE) as UsbManager?
                        val usbDevice =
                            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbManager != null && usbDevice != null) {
                                Toast.makeText(
                                    context,
                                    context.resources.getString(R.string.permission_granted_try_print_now),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.permission_not_granted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        fun isPrinterConnected(print: PrinterRespo, usbConnection: UsbConnection): Boolean {
            return print.printer_port == usbConnection.device.serialNumber
        }
    }
}