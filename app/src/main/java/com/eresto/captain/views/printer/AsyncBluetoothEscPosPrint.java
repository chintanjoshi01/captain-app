package com.eresto.captain.views.printer;

import android.content.Context;


public class AsyncBluetoothEscPosPrint extends AsyncEscPosPrint {

    public AsyncBluetoothEscPosPrint(Context context, AsyncEscPosPrint.OnSuccess onSuccess, int type, int copies) {
        super(context,onSuccess,type,copies);
    }
}
