/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.mediatek.dialer.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.android.dialer.R;
import com.mediatek.dialer.calllog.CallLogMultipleDeleteFragment;
import com.mediatek.dialer.list.DropMenu;
import com.mediatek.dialer.list.DropMenu.DropDownMenu;

/**
 * M: Add for [Multi-Delete], Displays a list of call log entries.
 */
public class CallLogMultipleDeleteActivity extends Activity {
    private static final String TAG = "CallLogMultipleDeleteActivity";

    protected CallLogMultipleDeleteFragment mFragment;

    //the dropdown menu with "Select all" and "Deselect all"
    private DropDownMenu mSelectionMenu;
    private boolean mIsSelectedAll = false;
    private boolean mIsSelectedNone = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mtk_call_log_multiple_delete_activity);

        // Typing here goes to the dialer
        //setDefaultKeyMode(DEFAULT_KEYS_DIALER);

        mFragment = (CallLogMultipleDeleteFragment) getFragmentManager().findFragmentById(
                R.id.call_log_fragment);
        configureActionBar();
        updateSelectedItemsView(0);

    }

    private void configureActionBar() {
        log("configureActionBar()");
        // Inflate a custom action bar that contains the "done" button for
        // multi-choice
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBarView = inflater.inflate(
                R.layout.mtk_call_log_multiple_delete_custom_action_bar, null);

        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        selectView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectionMenu == null || !mSelectionMenu.isShown()) {
                    View parent = (View) v.getParent();
                    mSelectionMenu = updateSelectionMenu(parent);
                    mSelectionMenu.show();
                } else {
                    log("mSelectionMenu is already showing, ignore this click");
                }
                return;
            }
        });

        //dispaly the "OK" button.
        Button deleteView = (Button) customActionBarView
                .findViewById(R.id.delete);
        //display the "confirm" button
        Button confirmView = (Button) customActionBarView.findViewById(R.id.confirm);
        if (mIsSelectedNone) {
            // if there is no item selected, the "OK" button is disable.
            deleteView.setEnabled(false);
            confirmView.setEnabled(false);
            confirmView.setTextColor(Color.GRAY);
        } else {
            deleteView.setEnabled(true);
            confirmView.setEnabled(true);
            confirmView.setTextColor(Color.WHITE);
        }
        deleteView.setOnClickListener(getClickListenerOfActionBarOKButton());

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setActionBarView(customActionBarView);
    }

    public void updateSelectedItemsView(final int checkedItemsCount) {
        Button selectedItemsView =
                (Button) getActionBar().getCustomView().findViewById(R.id.select_items);
        if (selectedItemsView == null) {
            log("Load view resource error!");
            return;
        }
        selectedItemsView.setText(getString(R.string.selected_item_count, checkedItemsCount));
        //if no item selected, the "OK" button is disable.
        Button optionView = (Button) getActionBar().getCustomView()
        .findViewById(R.id.delete);
        Button confirmView = (Button) getActionBar().getCustomView().findViewById(R.id.confirm);
        if (checkedItemsCount == 0) {
            optionView.setEnabled(false);
            confirmView.setEnabled(false);
            confirmView.setTextColor(Color.GRAY);
        } else {
            optionView.setEnabled(true);
            confirmView.setEnabled(true);
            confirmView.setTextColor(Color.WHITE);
        }
        /** M: Fix CR ALPS01677733. Disable the selected view if it has no data. @{ */
        if (mFragment.getItemCount() > 0) {
            selectedItemsView.setEnabled(true);
        } else {
            selectedItemsView.setEnabled(false);
        }
        /** @} */
    }

    private void log(final String log) {
        Log.d(TAG, log);
    }

    private void showDeleteDialog() {
        DeleteComfigDialog.newInstance().show(getFragmentManager(), "DeleteComfigDialog");
    }

    /**
     * add dropDown menu on the selectItems.The menu is "Select all" or "Deselect all"
     * @param customActionBarView
     * @return The updated DropDownMenu instance
     */
    private DropDownMenu updateSelectionMenu(View customActionBarView) {
        DropMenu dropMenu = new DropMenu(this);
        // new and add a menu.
        DropDownMenu selectionMenu = dropMenu.addDropDownMenu((Button) customActionBarView
                .findViewById(R.id.select_items), R.menu.mtk_selection);
        // new and add a menu.
        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        selectView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectionMenu == null || !mSelectionMenu.isShown()) {
                    View parent = (View) v.getParent();
                    mSelectionMenu = updateSelectionMenu(parent);
                    mSelectionMenu.show();
                } else {
                    log("mSelectionMenu is already showing, ignore this click");
                }
                return;
            }
        });
        MenuItem item = selectionMenu.findItem(R.id.action_select_all);
        mIsSelectedAll = mFragment.isAllSelected();
        // if select all items, the menu is "Deselect all"; else the menu is "Select all".
        if (mIsSelectedAll) {
            item.setChecked(true);
            item.setTitle(R.string.menu_select_none);
            // click the menu, deselect all items
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    configureActionBar();
                    mFragment.unSelectAllItems();
                    updateSelectedItemsView(0);
                    return false;
                }
            });
        } else {
            item.setChecked(false);
            item.setTitle(R.string.menu_select_all);
            //click the menu, select all items.
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    configureActionBar();
                    updateSelectedItemsView(mFragment.selectAllItems());
                    return false;
                }
            });
        }
        return selectionMenu;
    }

    protected OnClickListener getClickListenerOfActionBarOKButton() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFragment.getSelectedItemCount() == 0) {
                    Toast.makeText(v.getContext(), R.string.multichoice_no_select_alert,
                                 Toast.LENGTH_SHORT).show();
                  return;
              }
              showDeleteDialog();
              return;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /// M: for ALPS01375185 @{
    // amend it for querying all CallLog on choice interface
    public Fragment getMultipleDeleteFragment() {
        return mFragment;
    }
    /// @}

    // amend it for action bar view on CallLogMultipleChoiceActivity interface
    protected void setActionBarView(View view) {
    }

    private void deleteSelectedCallItems() {
        if (mFragment != null) {
            mFragment.deleteSelectedCallItems();
            updateSelectedItemsView(0);
        }
    }

    public static class DeleteComfigDialog extends DialogFragment {
        static DeleteComfigDialog newInstance() {
            return new DeleteComfigDialog();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.deleteCallLogConfirmation_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.deleteCallLogConfirmation_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (getActivity() != null) {
                                        ((CallLogMultipleDeleteActivity) getActivity())
                                                .deleteSelectedCallItems();
                                    }
                                }
                            });
            return builder.create();
        }
    }
}
