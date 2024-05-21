package com.eresto.captain.views.printer;

import com.eresto.captain.views.printer.connection.DeviceConnection;

public class AsyncEscPosPrinter extends EscPosPrinterSize {
    private final DeviceConnection printerConnection;
    private String textToPrint = "";

    public AsyncEscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        this.printerConnection = printerConnection;
    }

    public DeviceConnection getPrinterConnection() {
        return this.printerConnection;
    }

    public AsyncEscPosPrinter setTextToPrint(String textToPrint) {
        this.textToPrint = textToPrint;
        return this;
    }

    public String getTextToPrint() {
        return this.textToPrint;
    }
}
