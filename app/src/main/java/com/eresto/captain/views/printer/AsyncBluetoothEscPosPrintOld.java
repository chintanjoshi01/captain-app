package com.eresto.captain.views.printer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

import com.eresto.captain.R;
import com.eresto.captain.views.printer.connection.DeviceConnection;
import com.eresto.captain.views.printer.connection.bluetooth.BluetoothPrintersConnections;
import com.eresto.captain.views.printer.exceptions.EscPosConnectionException;

import java.lang.ref.WeakReference;
import java.util.Objects;


public class AsyncBluetoothEscPosPrintOld extends AsyncEscPosPrint {
    protected WeakReference<Context> weakContext;
    private final Context context;
    private final int type;
    private int copies;

    private final OnSuccess onSuccess;
    public AsyncBluetoothEscPosPrintOld(Context context, OnSuccess onSuccess, int type, int copies) {
        super(context, onSuccess, type,copies);
        this.weakContext = new WeakReference<>(context);
        this.context = context;
        this.onSuccess = onSuccess;
        this.type = type;
        this.copies = copies;
    }
    protected void onPreExecute() {
        if (this.dialog == null) {
            Context context = weakContext.get();

            if (context == null) {
                return;
            }

            this.dialog = new ProgressDialog(context);
            this.dialog.setTitle("Printing in progress...");
            this.dialog.setMessage("...");
            this.dialog.setProgressNumberFormat("%1d / %2d");
            this.dialog.setCancelable(false);
            this.dialog.setIndeterminate(false);
            this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.show();
        }
    }

    protected Integer doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return AsyncEscPosPrint.FINISH_NO_PRINTER;
        }
        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);
        AsyncEscPosPrinter printerData = printersData[0];
        DeviceConnection deviceConnection = printerData.getPrinterConnection();

        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

        if (deviceConnection == null) {
            printersData[0] = new AsyncEscPosPrinter(
                    BluetoothPrintersConnections.selectFirstPaired(),
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine()
            );
            printersData[0].setTextToPrint(printerData.getTextToPrint());
        } else {
            try {
                deviceConnection.connect();
            } catch (EscPosConnectionException e) {
                e.printStackTrace();
            }
        }

        return super.doInBackground(printersData);
    }

    protected void onProgressUpdate(Integer... progress) {
        switch (progress[0]) {
            case AsyncEscPosPrint.PROGRESS_CONNECTING:
                this.dialog.setMessage("Connecting printer...");
                break;
            case AsyncEscPosPrint.PROGRESS_CONNECTED:
                this.dialog.setMessage("Printer is connected...");
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTING:
                this.dialog.setMessage("Printer is printing...");
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTED:
                this.dialog.setMessage("Printer has finished...");
                break;
        }
        this.dialog.setProgress(progress[0]);
        this.dialog.setMax(4);
    }
    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        if(this.dialog!=null)
            this.dialog.dismiss();
        this.dialog = null;

        Context context = weakContext.get();
        if (printer != null)
            printer.disconnectPrinter();
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
                }else {
                    onSuccess.onSuccess(true);
                }

                break;
            case AsyncEscPosPrint.FINISH_NO_PRINTER:
                showDialog("Failed", "The application can't find any printer connected", "Retry", false);
                break;
            case AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED:
                if (type != 3) {
                    showDialog("Failed", "Unable to connect the printer.", "Retry", false);
                } else {
                    onSuccess.onSuccess(false);
                }

                break;
            case AsyncEscPosPrint.FINISH_PARSER_ERROR:
                showDialog("Failed", "It seems to be an invalid syntax problem.", "Retry", false);
                break;
            case AsyncEscPosPrint.FINISH_ENCODING_ERROR:
                showDialog("Failed", "The selected encoding character returning an error.", "Retry", false);
                break;
            case AsyncEscPosPrint.FINISH_BARCODE_ERROR:
                showDialog("Failed", "Data send to be converted to barcode or QR code seems to be invalid.", "Retry", false);
                break;
        }
    }

    private void showDialog(String type, String message, String okButtonText, Boolean success) {
        Dialog dialog = new Dialog(context);
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
        btnCancel.setText("Back");
        btnAddSession.setOnClickListener(v -> {
            dialog.cancel();
            /* if (success) {
             */

            /**Cut Paper Code*/

            /*AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerData.getPrinterConnection(),
                        printerData.getPrinterDpi(),
                        printerData.getPrinterWidthMM(),
                        printerData.getPrinterNbrCharactersPerLine());
                printer.setTextToPrint("cut_paper");
                new AsyncTcpEscPosPrint(context, onSuccess, 2, copies).execute(printer);

            } else {
                AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerData.getPrinterConnection(),
                        printerData.getPrinterDpi(),
                        printerData.getPrinterWidthMM(),
                        printerData.getPrinterNbrCharactersPerLine());
                printer.setTextToPrint(printerData.getTextToPrint());
                new AsyncTcpEscPosPrint(context, onSuccess, 2, copies).execute(printer);
            }*/
        });
        btnCancel.setOnClickListener(v -> {
            dialog.cancel();
        });
        dialog.setOnCancelListener(dialog1 -> onSuccess.onSuccess(success));
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }
}
