package com.eresto.captain.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.eresto.captain.databinding.DialogConfirmationBinding

// In DialogManager.kt

// Add this import statement at the top

// ... (keep the existing code for showReconnectDialog)

interface OnConfirmationClick {
    fun onPositive()
    fun onNegative()
}

/**
 * Data class to encapsulate parameters for the generic confirmation dialog.
 */
data class ConfirmationDialogParams(
    val title: String,
    val subtitle: String? = null, // Subtitle is optional
    val positiveButtonText: String,
    val negativeButtonText: String,
    @DrawableRes val logo: Int? = null, // Logo is optional
    val isCancelable: Boolean = true,
    val clickListener: OnConfirmationClick
)


/**
 * Manages the creation and display of app-specific dialogs.
 * This class ensures a consistent look and feel and centralizes dialog logic.
 */
object DialogManager {

    // Keep a reference to the currently showing dialog to prevent duplicates
    private var currentDialog: Dialog? = null

    /**
     * Displays a reconnect dialog that adapts to the device type (phone or tablet).
     *
     * @param context The context, preferably an Activity, to show the dialog in.
     * @param params The configuration for the dialog.
     */
    /*  fun showReconnectDialog(context: Context, params: ReconnectDialogParams) {
          // Dismiss any existing dialog to avoid stacking them
          dismissCurrentDialog()

          // Don't show a dialog if the activity is finishing or destroyed
          if (context is Activity && (context.isFinishing || context.isDestroyed)) {
              return
          }

          val isTablet = context.resources.getBoolean(R.bool.is_tablet)

          currentDialog = if (isTablet) {
              createModalDialog(context, params)
          } else {
              createBottomSheetDialog(context, params)
          }

          currentDialog?.show()
      }

      private fun createBottomSheetDialog(context: Context, params: ReconnectDialogParams): Dialog {
          val dialog = BottomSheetDialog(context)
          val binding = DialogReconnectBottomSheetBinding.inflate(LayoutInflater.from(context))

          dialog.setContentView(binding.root)
          dialog.setCancelable(params.isCancelable)
          dialog.setCanceledOnTouchOutside(params.isCancelable)

          setupCommonDialogViews(
              messageView = binding.txtMessage,
              okButton = binding.btnOk,
              cancelButton = binding.btnCancel,
              dialog = dialog,
              params = params
          )

          dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
          dialog.window?.setLayout(
              WindowManager.LayoutParams.MATCH_PARENT,
              WindowManager.LayoutParams.WRAP_CONTENT
          )

          return dialog
      }

      private fun createModalDialog(context: Context, params: ReconnectDialogParams): Dialog {
          val binding = DialogReconnectModalBinding.inflate(LayoutInflater.from(context))
          val dialog = AlertDialog.Builder(context)
              .setView(binding.root)
              .setCancelable(params.isCancelable)
              .create()

          setupCommonDialogViews(
              messageView = binding.txtMessage,
              okButton = binding.btnOk,
              cancelButton = binding.btnCancel,
              dialog = dialog,
              params = params
          )

          dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

          return dialog
      }

      private fun setupCommonDialogViews(
          messageView: android.widget.TextView,
          okButton: android.widget.Button,
          cancelButton: android.widget.Button,
          dialog: Dialog,
          params: ReconnectDialogParams
      ) {
          messageView.text = params.message
          okButton.text = params.okButtonText
          cancelButton.visibility = if (params.showCancel) View.VISIBLE else View.GONE

          okButton.setOnClickListener {
              params.clickListener.onOk()
              dialog.dismiss()
          }

          cancelButton.setOnClickListener {
              params.clickListener.onCancel()
              dialog.dismiss()
          }

          dialog.setOnDismissListener {
              currentDialog = null
          }
      }*/

    /**
     * Dismisses the currently shown dialog, if any. Useful in lifecycle methods like onDestroy.
     */
    fun dismissCurrentDialog() {
        if (currentDialog?.isShowing == true) {
            currentDialog?.dismiss()
        }
        currentDialog = null
    }

    /**
     * Displays a branded, custom confirmation dialog.
     * This dialog is modal and works the same on phones and tablets.
     *
     * @param context The context, preferably an Activity, to show the dialog in.
     * @param params The configuration for the dialog.
     */
    fun showConfirmationDialog(context: Context, params: ConfirmationDialogParams) {
        dismissCurrentDialog()

        if (context is Activity && (context.isFinishing || context.isDestroyed)) {
            return
        }

        val binding = DialogConfirmationBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(params.isCancelable)
            .create()

        // --- Configure Views ---
        binding.txtTitle.text = params.title
        binding.btnPositive.text = params.positiveButtonText
        binding.btnNegative.text = params.negativeButtonText

        // Handle optional subtitle
        if (params.subtitle.isNullOrBlank()) {
            binding.txtSubtitle.visibility = View.GONE
        } else {
            binding.txtSubtitle.visibility = View.VISIBLE
            binding.txtSubtitle.text = params.subtitle
        }

        // Handle optional logo
        if (params.logo != null) {
            binding.imgLogo.setImageResource(params.logo)
            binding.imgLogo.visibility = View.VISIBLE
        } else {
            binding.imgLogo.visibility = View.GONE
        }

        // --- Configure Listeners ---
        binding.btnPositive.setOnClickListener {
            params.clickListener.onPositive()
            dialog.dismiss()
        }

        binding.btnNegative.setOnClickListener {
            params.clickListener.onNegative()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            currentDialog = null
        }

        // --- Styling and Display ---
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = dialog
        dialog.show()
    }
}

