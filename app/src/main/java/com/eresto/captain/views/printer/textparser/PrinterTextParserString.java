package com.eresto.captain.views.printer.textparser;

import com.eresto.captain.views.printer.EscPosCharsetEncoding;
import com.eresto.captain.views.printer.EscPosPrinter;
import com.eresto.captain.views.printer.EscPosPrinterCommands;
import com.eresto.captain.views.printer.exceptions.EscPosEncodingException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class PrinterTextParserString implements IPrinterTextParserElement {
    private final EscPosPrinter printer;
    private final String text;
    private final byte[] textSize;
    private final byte[] textColor;
    private final byte[] textReverseColor;
    private final byte[] textBold;
    private final byte[] textUnderline;
    private final byte[] textDoubleStrike;

    public PrinterTextParserString(PrinterTextParserColumn printerTextParserColumn, String text, byte[] textSize, byte[] textColor, byte[] textReverseColor, byte[] textBold, byte[] textUnderline, byte[] textDoubleStrike) {
        this.printer = printerTextParserColumn.getLine().getTextParser().getPrinter();
        this.text = text;
        this.textSize = textSize;
        this.textColor = textColor;
        this.textReverseColor = textReverseColor;
        this.textBold = textBold;
        this.textUnderline = textUnderline;
        this.textDoubleStrike = textDoubleStrike;
    }

    @Override
    public int length() throws EscPosEncodingException {
        EscPosCharsetEncoding charsetEncoding = this.printer.getEncoding();

        int coef = Arrays.equals(this.textSize, EscPosPrinterCommands.TEXT_SIZE_DOUBLE_WIDTH) || Arrays.equals(this.textSize, EscPosPrinterCommands.TEXT_SIZE_BIG) ? 2 : 1;

        if (charsetEncoding != null) {
            try {
                return this.text.getBytes(charsetEncoding.getName()).length * coef;
            } catch (UnsupportedEncodingException e) {
                throw new EscPosEncodingException(e.getMessage());
            }
        }

        return this.text.length() * coef;
    }

    /**
     * Print text
     *
     * @param printerSocket Instance of EscPosPrinterCommands
     * @return this Fluent method
     */
    @Override
    public PrinterTextParserString print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException {
        printerSocket.printText(this.text, this.textSize, this.textColor, this.textReverseColor, this.textBold, this.textUnderline, this.textDoubleStrike);
        return this;
    }
}
