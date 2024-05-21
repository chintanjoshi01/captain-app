package com.eresto.captain.views.printer;

import android.content.Context;

public class AsyncUsbEscPosPrint extends AsyncEscPosPrint {

    public AsyncUsbEscPosPrint(Context context, OnSuccess onSuccess, int type, int copies) {
        super(context,onSuccess,type,copies);
    }
}
