package com.android.soundrecorder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

/** M: use DialogFragment to show Dialog */
public class DeleteDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {

    private static final String TAG = "SR/DeleteDialogFragment";
    private static final String KEY_SINGLE = "single";
    private DialogInterface.OnClickListener mClickListener = null;

    /**
     * M: create a instance of DeleteDialogFragment
     *
     * @param single
     *            if the number of files to be deleted is only one ?
     * @return the instance of DeleteDialogFragment
     */
    public static DeleteDialogFragment newInstance(Boolean single) {
        DeleteDialogFragment frag = new DeleteDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_SINGLE, single);
        frag.setArguments(args);
        return frag;
    }

    @Override
    /**
     * M: create a dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtils.i(TAG, "<onCreateDialog>");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String alertMsg = null;
        if (getArguments().getBoolean(KEY_SINGLE)) {
            alertMsg = getString(R.string.alert_delete_single);
        } else {
            alertMsg = getString(R.string.alert_delete_multiple);
        }

        builder.setTitle(R.string.delete).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                alertMsg).setPositiveButton(getString(R.string.ok), this).setNegativeButton(
                getString(R.string.cancel), null);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    /**
     * M: the process of click OK button on dialog
     */
    @Override
    public void onClick(DialogInterface arg0, int arg1) {
        if (null != mClickListener) {
            mClickListener.onClick(arg0, arg1);
        }
    }

    /**
     * M: set listener of OK button
     *
     * @param listener the listener to be set
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mClickListener = listener;
    }

    /**
     * M: update the message of dialog, single/multi file/files to be deleted
     *
     * @param single
     *            if single file to be deleted
     */
    public void setSingle(boolean single) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (null != dialog) {
            if (single) {
                dialog.setMessage(getString(R.string.alert_delete_single));
            } else {
                dialog.setMessage(getString(R.string.alert_delete_multiple));
            }
        }
    }
    /**
     * M: change the buttons to disable or enable
     *
     * @param whichButton to be setting state
     * @param isEnable whether enable button or disable
     */
    public void setButton(int whichButton, boolean isEnable) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (null != dialog) {
            Button btn = dialog.getButton(whichButton);
            if (btn != null) {
                btn.setEnabled(isEnable);
                LogUtils.d(TAG, " set button state to " + btn.isEnabled());
            } else {
                LogUtils.d(TAG, "get button" + whichButton + " from dialog is null ");
            }
        }
    }
}