package com.eresto.captain.views.printer.textparser;

import com.eresto.captain.views.printer.EscPosPrinterCommands;
import com.eresto.captain.views.printer.exceptions.EscPosEncodingException;

public interface IPrinterTextParserElement {
    int length() throws EscPosEncodingException;
    IPrinterTextParserElement print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException;
}
