package com.eresto.captain.views.printer.barcode;

import com.eresto.captain.views.printer.EscPosPrinterCommands;
import com.eresto.captain.views.printer.EscPosPrinterSize;
import com.eresto.captain.views.printer.exceptions.EscPosBarcodeException;

public class BarcodeUPCA extends BarcodeNumber {

    public BarcodeUPCA(EscPosPrinterSize printerSize, String code, float widthMM, float heightMM, int textPosition) throws EscPosBarcodeException {
        super(printerSize, EscPosPrinterCommands.BARCODE_TYPE_UPCA, code, widthMM, heightMM, textPosition);
    }

    @Override
    public int getCodeLength() {
        return 12;
    }
}
