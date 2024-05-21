package com.eresto.captain.views.printer;

import android.annotation.SuppressLint;
import android.content.Context;

import com.eresto.captain.views.printer.connection.DeviceConnection;

public class AsyncTcpEscPosPrint extends AsyncEscPosPrint {
    public AsyncTcpEscPosPrint(Context context, OnSuccess onSuccess, int type, int copies) {
        super(context,onSuccess,type,copies);
    }

    public AsyncTcpEscPosPrint(Context context, String str, DeviceConnection printerConnection, OnSuccess onSuccess, int type, int copies) {
        super(context,onSuccess,type,copies);
        getAsyncEscPosPrinter(context, str, printerConnection);
    }

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(Context context, String str, DeviceConnection printerConnection) {
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);
        return printer.setTextToPrint(str);
    }

}
