package com.eresto.captain.views.printer;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

import com.eresto.captain.R;
import com.eresto.captain.utils.Utils;
import com.eresto.captain.views.printer.connection.DeviceConnection;
import com.eresto.captain.views.printer.exceptions.EscPosBarcodeException;
import com.eresto.captain.views.printer.exceptions.EscPosConnectionException;
import com.eresto.captain.views.printer.exceptions.EscPosEncodingException;
import com.eresto.captain.views.printer.exceptions.EscPosParserException;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.lang.ref.WeakReference;
import java.util.Objects;

public abstract class AsyncEscPosPrint extends AsyncTask<AsyncEscPosPrinter, Integer, Integer> {
    protected final static int FINISH_SUCCESS = 1;
    protected final static int FINISH_NO_PRINTER = 2;
    protected final static int FINISH_PRINTER_DISCONNECTED = 3;
    protected final static int FINISH_PARSER_ERROR = 4;
    protected final static int FINISH_ENCODING_ERROR = 5;
    protected final static int FINISH_BARCODE_ERROR = 6;
    protected final static int PROGRESS_CONNECTING = 1;
    protected final static int PROGRESS_CONNECTED = 2;
    protected final static int PROGRESS_PRINTING = 3;
    protected final static int PROGRESS_PRINTED = 4;
    protected ProgressDialog dialog;
    protected WeakReference<Context> weakContext;
    private final Context context;
    private final int type;
    private int copies;
    private final int connectTry = 2;
    private static int copiesNumber = 0;
    AsyncEscPosPrinter printerData = null;
    EscPosPrinter printer = null;
    private final OnSuccess onSuccess;

    public interface OnSuccess {
        void onSuccess(Boolean isSuccess);
    }

    public AsyncEscPosPrint(Context context, OnSuccess onSuccess, int type, int copies) {
        this.weakContext = new WeakReference<>(context);
        this.context = context;
        this.onSuccess = onSuccess;
        this.type = type;
        this.copies = copies;
    }

    protected Integer doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return AsyncEscPosPrint.FINISH_NO_PRINTER;
        }

        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

        printerData = printersData[0];

        try {
            DeviceConnection deviceConnection = printerData.getPrinterConnection();

            if (deviceConnection == null) {
                return AsyncEscPosPrint.FINISH_NO_PRINTER;
            }


            printer = new EscPosPrinter(
                    deviceConnection,
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine(),
                    new EscPosCharsetEncoding(Utils.printCommand, 16)
            );
            if (!printerData.getTextToPrint().equals("cut_paper")) {
                this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTING);
                printer.printFormattedTextAndCut(printerData.getTextToPrint());

                byte[] cmd = printer.getEncoding().getCommand();
                String tet = printerData.getTextToPrint();
            } else {
                printer.printFormattedTextAndCut("");
            }

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTED);

        } catch (EscPosConnectionException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED;
        } catch (EscPosParserException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_PARSER_ERROR;
        } catch (EscPosEncodingException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_ENCODING_ERROR;
        } catch (EscPosBarcodeException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_BARCODE_ERROR;
        }

        return AsyncEscPosPrint.FINISH_SUCCESS;
    }

    protected void onPreExecute() {
        if (this.dialog == null) {
            Context context = weakContext.get();

            if (context == null) {
                return;
            }

            this.dialog = new ProgressDialog(context);
            this.dialog.setTitle(context.getResources().getString(R.string.printing_in_progress));
            this.dialog.setMessage("...");
            this.dialog.setProgressNumberFormat("%1d / %2d");
            this.dialog.setCancelable(false);
            this.dialog.setIndeterminate(false);
            this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.show();
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        switch (progress[0]) {
            case AsyncEscPosPrint.PROGRESS_CONNECTING:
                this.dialog.setMessage(context.getResources().getString(R.string.connecting_printer));
                break;
            case AsyncEscPosPrint.PROGRESS_CONNECTED:
                this.dialog.setMessage(context.getResources().getString(R.string.printer_is_connected));
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTING:
                this.dialog.setMessage(context.getResources().getString(R.string.printer_is_printing));
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTED:
                this.dialog.setMessage(context.getResources().getString(R.string.printer_has_finished));
                break;
        }
        this.dialog.setProgress(progress[0]);
        this.dialog.setMax(4);
    }

    protected void onPostExecute(Integer result) {
        this.dialog.dismiss();
        this.dialog = null;
        if (printer != null)
            printer.disconnectPrinter();
        Context context = weakContext.get();

        if (context == null) {
            return;
        }

        switch (result) {
            case AsyncEscPosPrint.FINISH_SUCCESS:
                if (type != 4 && type != 3) {
                    copies--;
                    if (copies > 0) {
                        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerData.getPrinterConnection(),
                                printerData.getPrinterDpi(),
                                printerData.getPrinterWidthMM(),
                                printerData.getPrinterNbrCharactersPerLine());
                        printer.setTextToPrint(printerData.getTextToPrint());
                        new AsyncTcpEscPosPrint(context, onSuccess, type, copies).execute(printer);
                    } else {
                        onSuccess.onSuccess(true);
                    }
                } else {
                    onSuccess.onSuccess(true);
                }
                break;
            case AsyncEscPosPrint.FINISH_NO_PRINTER:
                showDialog(context.getResources().getString(R.string.failed),
                        context.getResources().getString(R.string.the_application_cant_find_any_printer_connected),
                        context.getResources().getString(R.string.retry_small_caps), false);
                break;
            case AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED:
                if (type != 3) {
                    showDialog(context.getResources().getString(R.string.failed),
                            context.getResources().getString(R.string.unable_to_connect_the_printer),
                            context.getResources().getString(R.string.retry_small_caps), false);
                } else {
                    onSuccess.onSuccess(false);
                }
                break;
            case AsyncEscPosPrint.FINISH_PARSER_ERROR:
                showDialog(context.getResources().getString(R.string.failed),
                        context.getResources().getString(R.string.it_seems_to_be_an_invalid_syntax_problem),
                        context.getResources().getString(R.string.retry_small_caps), false);
                break;
            case AsyncEscPosPrint.FINISH_ENCODING_ERROR:
                showDialog(context.getResources().getString(R.string.failed),
                        context.getResources().getString(R.string.the_selected_encoding_character_returning_an_error),
                        context.getResources().getString(R.string.retry_small_caps), false);
                break;
            case AsyncEscPosPrint.FINISH_BARCODE_ERROR:
                showDialog(context.getResources().getString(R.string.failed),
                        context.getResources().getString(R.string.data_send_to_be_converted_to_barcode_or_qr_code_seems_to_be_invalid),
                        context.getResources().getString(R.string.retry_small_caps), false);
                break;
        }
    }

    private void showDialog(String type, String message, String okButtonText, Boolean success) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_snack_bottom);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        ImageView img = dialog.findViewById(R.id.img);
        if (type.equals("Success")) {
            img.setImageResource(R.drawable.ic_check_circle_fill_green);
        } else {
            img.setImageResource(R.drawable.ic_cancel_red_fill);
        }
        TextView txtStandard = dialog.findViewById(R.id.txt_standard);
        txtStandard.setText(message);

        AppCompatButton btnAddSession = dialog.findViewById(R.id.btn_add_session);
        AppCompatButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        btnAddSession.setText(okButtonText);
        btnCancel.setText(context.getResources().getString(R.string.back));
        btnAddSession.setOnClickListener(v -> {
            onSuccess.onSuccess(false);
            dialog.cancel();
        });
        btnCancel.setOnClickListener(v -> {
            onSuccess.onSuccess(false);
            dialog.cancel();
        });
//        dialog.setOnCancelListener(dialog1 -> onSuccess.onSuccess(success));
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }
}