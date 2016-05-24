package com.android.music;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

/** M: use DialogFragment to show Dialog */
public class DeleteDialogFragment extends DialogFragment{

    private static final String TAG = "DeleteItems";
    private static final String KEY_SINGLE = "single";
    private static final String TRACK_ID = "track";
    private long [] mItemList = {-1};

    /**
     * M: create a instance of DeleteItems
     *
     * @param single
     *            if the number of files to be deleted is only one ?
     * @return the instance of DeleteDialogFragment
     */
    public static DeleteDialogFragment newInstance(Boolean single,
                                                   long id,
                                                   int str_id,
                                                   String track_name) {
        DeleteDialogFragment frag = new DeleteDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_SINGLE, single);
        args.putLong(TRACK_ID, id);
        args.putInt(MusicUtils.DELETE_DESC_STRING_ID,str_id);
        args.putString(MusicUtils.DELETE_DESC_TRACK_INFO,track_name);
        frag.setArguments(args);
        return frag;
    }

    @Override
    /**
     * M: create a dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MusicLogUtils.i(TAG, "<onDELTEDialog>");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String alertMsg = null;
        mItemList[0] = (int) getArguments().getLong(TRACK_ID);
        MusicLogUtils.i(TAG, "Delete mList item id" + mItemList[0]+
        ", Track id = " + getArguments().getLong(TRACK_ID));
        alertMsg = String.format(getString(
                   getArguments().getInt(MusicUtils.DELETE_DESC_STRING_ID)),
                   getArguments().getString(MusicUtils.DELETE_DESC_TRACK_INFO));
        builder.setMessage(alertMsg).setPositiveButton
        (getString(R.string.delete_confirm_button_text),
         new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
               MusicUtils.deleteTracks(getActivity().getApplicationContext(), mItemList);
               MusicUtils.showDeleteToast(1,getActivity().getApplicationContext());
                        }
                    }
                )
                .setNegativeButton(
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }
                    );
        builder.setTitle(R.string.delete_item);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    /**
     * M: the process of click OK button on dialog
     */
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
                dialog.setMessage(getString(R.string.delete_progress_title));
            } else {
                dialog.setMessage(getString(R.string.delete_progress_title));
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
                MusicLogUtils.d(TAG, " set button state to " + btn.isEnabled());
            } else {
                MusicLogUtils.d(TAG, "get button" + whichButton + " from dialog is null ");
            }
        }
    }
}

   /**
     * M: use handler to start and finish the operation.
     */
