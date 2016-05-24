/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.vcs;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.activities.ActionBarAdapter.TabState;
import com.android.contacts.list.DefaultContactBrowseListFragment;

import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.vcs.VcsAppGuide.OnGuideFinishListener;

import java.util.ArrayList;

public class VcsController {
    private static final String TAG = "VcsController";

    private static final long DELAY_TIME_AFTER_TOUCH = 400;

    private Activity mActivity = null;
    private ActionBarAdapter mActionBarAdapter = null;
    private DefaultContactBrowseListFragment mAllFragment;

    private VoiceSearchManager mVoiceSearchManager = null;
    private VoiceSearchDialogFragment mVoiceSearchDialogFragment;
    private int mContactsCount = 0;
    private MenuItem mVcsItem = null;
    private boolean mIsShowingGuide = false;
    private VoiceSearchIndicator mVoiceIndicator = null;
    private Handler mHandler = new Handler();

    public VcsController(Activity activity, ActionBarAdapter actionBarAdapter,
            DefaultContactBrowseListFragment allFragment) {
        Log.i(TAG, "[VcsController]new..");
        mActivity = activity;
        mActionBarAdapter = actionBarAdapter;
        mAllFragment = allFragment;
        mAllFragment.setContactsLoadListener(mContactsLoadListener);
    }

    /**
     * VoiceListener
     */
    private VoiceSearchManager.VoiceListener mVoiceListener =
            new VoiceSearchManager.VoiceListener() {
        @Override
        public void onVoiceStop() {
            Log.d(TAG, "[onVoiceStop]...");
            if (mVoiceSearchDialogFragment.isShowing()) {
                mVoiceSearchDialogFragment.updateVcsIndicator(false);
                mVoiceIndicator.updateIndicator(false);
            } else if (mVoiceIndicator != null && mVoiceIndicator.isIndicatorEnable()) {
                mVoiceIndicator.updateIndicator(false);
            }
        }

        @Override
        public void onVoiceEnable() {
            Log.d(TAG, "[onVoiceEnable]...");
            if (!checkStartVcsCondition()) {
                Log.w(TAG, "[onVoiceEnable] can not enable voice..");
                updateVoice();
                return;
            }
            if (mVcsItem == null) {
                Log.w(TAG, "[onVoiceEnable] the mVCSItem is null..");
            }
            if (mVoiceIndicator == null) {
                Log.w(TAG, "[onVoiceEnable] the mVoiceIndicator is null..");
                return;
            }

            if (mVoiceSearchDialogFragment.isShowing()) {
                mVoiceSearchDialogFragment.updateVcsIndicator(true);
                mVoiceIndicator.updateIndicator(false);
            } else if (mVoiceIndicator != null && !mVoiceIndicator.isIndicatorEnable()) {
                mVoiceIndicator.updateIndicator(true);
            }
        }

        @Override
        public void onVoiceDisble() {
            Log.d(TAG, "[onVoiceDisble]...");
            if (mVoiceSearchDialogFragment.isShowing()) {
                mVoiceSearchDialogFragment.updateVcsIndicator(false);
                mVoiceIndicator.updateIndicator(false);
            } else if (mVoiceIndicator != null && mVoiceIndicator.isIndicatorEnable()) {
                mVoiceIndicator.updateIndicator(false);
            }
        }

        @Override
        public void onVoiceConnected() {
            boolean enable = updateVoice();
            Log.d(TAG, "[onVoiceConnected]enable:" + enable);
        }
    };

    /**
     * SpeechLister
     */
    private VoiceSearchManager.SpeechLister mSpeechLister = new VoiceSearchManager.SpeechLister() {
        @Override
        public void onSpeechResult(ArrayList<String> nameList) {
            AsyncTask<ArrayList<String>, Void, Cursor> task = new AsyncTask<ArrayList<String>,
                    Void, Cursor>() {
                CursorLoader loader = new CursorLoader(mActivity.getApplicationContext());

                @Override
                public void onPreExecute() {
                    mVoiceSearchManager.disableVoice();
                    Log.i(
                            TAG,
                            "[vcs][performance],onQueryContactsInfo start,time:"
                                    + System.currentTimeMillis());
                }

                @Override
                public Cursor doInBackground(ArrayList<String>... names) {
                    //return VcsUtils.getCursorByAudioName(mAllFragment, names[0], loader);
                    //ALPS02009019:
                    return VcsUtils.getCursorByAudioName(mAllFragment, names[0], loader,
                            mActivity.getApplicationContext());
                }

                @Override
                protected void onPostExecute(Cursor cursor) {
                    Log.i(
                            TAG,
                            "[vcs][performance],onQueryContactsInfo end,time:"
                                    + System.currentTimeMillis());
                    if (cursor == null) {
                        Log.w(TAG, "[onPostExecute][vcs] cursor is null");
                    }
                    if (!mActivity.isResumed()) {
                        Log.w(TAG, "[onPostExecute] Activity is not in Resumed,ignore... ");
                        return;
                    }
                    int count = cursor == null ? -1 : cursor.getCount();
                    Log.d(TAG, "[onPostExecute][vcs] cursor counts:" + count);

                    Dialog dialog = mVoiceSearchDialogFragment.getDialog();
                    // Bug fix ALPS01647494, if current tab is not in people
                    // list, dismiss the dialog.
                    if (dialog != null && dialog.isShowing()
                            && mActionBarAdapter.getCurrentTab() != TabState.ALL) {
                        dialog.dismiss();
                    }
                    if (dialog != null && dialog.isShowing()) {
                        mVoiceSearchDialogFragment.searchDone(cursor);
                        // Bug fix ALPS01999747
                        Log.d(TAG, "[onPostExecute][vcs] disable");
                        mVoiceSearchManager.disableVoice();
                    } else {
                        updateVoice();
                        Log.w(TAG, "[onPostExecute] Dialog is not showing..dialog:" + dialog);
                    }
                }
            };
            // execute
            task.execute(nameList);
        }

        @Override
        public void onSpeechDetected() {
            if (mVcsItem == null || mVoiceIndicator == null) {
                Log.w(TAG, "[onSpeechDetected] UI not ready,ignore.mVcsItem:" + mVcsItem
                        + ",mVoiceIndicator:" + mVoiceIndicator);
                return;
            }
            Log.d(TAG, "[onSpeechDetected]...");
            if (!mActivity.isResumed()) {
                Log.w(TAG, "[onSpeechDetected] Activity is not in Resumed,ignore... ");
                return;
            }
            if (!mVoiceSearchDialogFragment.isShowing() && (mContactsCount > 0)
                    && (mActionBarAdapter.getCurrentTab() == TabState.ALL)) {
                Log.d(TAG, "[onSpeechDetected][vcs] need show wave dialog");
                // remove or dismiss dialog fragment ahead of time
                FragmentManager fManager = mActivity.getFragmentManager();
                dismissDlgFrag(fManager);
                // show dialog
                mVoiceSearchDialogFragment.show(fManager, "VoiceSearchDialog");
            } else {
                Log.w(TAG, "[onSpeechDetected] not show Dialog,mContactsCount:"
                        + mContactsCount + ",tab:" + mActionBarAdapter.getCurrentTab()
                        + ",isDialogShowing:" + mVoiceSearchDialogFragment.isShowing());
            }
        }
    };

    /**
     * VoiceDialogListener
     */
    private VoiceSearchDialogFragment.VoiceDialogListener mVoiceDialogListener =
            new VoiceSearchDialogFragment.VoiceDialogListener() {
        @Override
        public boolean onSearchPanelClick() {
            updateVoice();
            return true;
        }

        @Override
        public void onRefreshDone() {
            // Refresh done
            Dialog dialog = mVoiceSearchDialogFragment.getDialog();
            // Bug fix ALPS01897914 if current tab is not in people list,
            // dismiss the dialog.
            if (dialog != null && dialog.isShowing()
                    && mActionBarAdapter.getCurrentTab() != TabState.ALL) {
                dialog.dismiss();
            }
            Log.d(TAG, "[refreshDone][vcs] table: " + mActionBarAdapter.getCurrentTab());
        }

        @Override
        public void onContactsRowClick(Uri uri, String name) {
            Log.d(TAG, "[onContactsRowClick] uri:" + uri + ",name:" + name);
            // dismiss dialog
            FragmentManager fManager = mActivity.getFragmentManager();
            dismissDlgFrag(fManager);
            // show contact detail info when clicking the item
            mAllFragment.viewContact(uri);
            // to learn user selected result
            mVoiceSearchManager.setVoiceLearn(name);
        }

        @Override
        public void onCancel() {
            Log.d(TAG, "[onCancel]...");
            if (mVoiceSearchManager.isInEnableStatus()) {
                if (mVoiceIndicator != null && !mVoiceIndicator.isIndicatorEnable()) {
                    mVoiceIndicator.updateIndicator(true);
                }
            } else {
                updateVoice();
            }
        }
    };

    /**
     * Vcs Guide Listener
     */
    private OnGuideFinishListener mGuideFinishListener = new OnGuideFinishListener() {
        @Override
        public void onGuideFinish() {
            if (VcsUtils.isVcsFeatureEnable()) {
                mIsShowingGuide = false;
                updateVoice();
            }
        }
    };

    /**
     * M: [vcs] for vcs.
     */
    public interface ContactsLoadListener {
        public void onContactsLoad(int count);
    }

    /**
     * DefaultContactBrowseListFragment show count listener.
     */
    private ContactsLoadListener mContactsLoadListener = new ContactsLoadListener() {
        /**
         * M: default contacts list load finished, not to activate VCS if no
         * contacts in database.
         */
        @Override
        public void onContactsLoad(final int count) {
            if (!VcsUtils.isVcsFeatureEnable()) {
                Log.i(TAG, "[onContactsLoad]isVcsFeatureEnable is false,return.");
                return;
            }
            // can not do transaction in onLoadFinish()
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mContactsCount = count;
                    if (count <= 0) {
                        dismissDlgFrag(mActivity.getFragmentManager());
                        mVoiceSearchManager.disableVoice();
                    } else if (!mVoiceSearchDialogFragment.isShowing()) {
                        updateVoice();
                    }
                }
            });
            return;
        }
    };

    /**
     *
     * @return true if can start vcs,false else;
     */
    private boolean checkStartVcsCondition() {
        int tab = mActionBarAdapter == null ? TabState.DEFAULT : mActionBarAdapter.getCurrentTab();
        boolean isSearchMode = mActionBarAdapter == null ? false : mActionBarAdapter.isSearchMode();
        boolean isSelectionMode = mActionBarAdapter == null ? false : mActionBarAdapter
                .isSelectionMode();

        if ((!mIsShowingGuide) && mActivity.isResumed() && (!isSearchMode) && (!isSelectionMode)
                && (tab == TabState.ALL) && mContactsCount > 0
                && VcsUtils.isVcsEnableByUser(mActivity)) {
            return true;
        }
        return false;
    }

    private boolean updateVoice() {
        if (checkStartVcsCondition()) {
            mVoiceSearchManager.enableVoice();
            return true;
        } else {
            mVoiceSearchManager.disableVoice();
        }
        return false;
    }

    /**
     * M: to dismiss the voice search process or results list dialog
     *
     * @param fManager
     */
    private void dismissDlgFrag(FragmentManager fManager) {
        Log.d(TAG, "[dismissDlgFrag][vcs]");
        if (mVoiceSearchDialogFragment.isAdded()) {
            mVoiceSearchDialogFragment.dismiss();
        }
    }

    public void init() {
        if (VcsUtils.isVcsFeatureEnable()) {
            mVoiceSearchManager = new VoiceSearchManager(mActivity);
            mVoiceSearchManager.setSpeechLister(mSpeechLister);
            mVoiceSearchManager.setVoiceListener(mVoiceListener);
            mVoiceSearchDialogFragment = new VoiceSearchDialogFragment(mActivity);
            mVoiceSearchDialogFragment.setVoiceDialogListener(mVoiceDialogListener);
        }
    }

    public void dispatchTouchEventVcs(MotionEvent ev) {
        if (mActionBarAdapter == null) {
            Log.w(TAG, "[dispatchTouchEventVcs] mActionBarAdapter is null");
            return;
        }
        if (VcsUtils.isVcsFeatureEnable()) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) {
                if (mActionBarAdapter.getCurrentTab() == TabState.ALL) {
                    mVoiceSearchManager.disableVoice();
                }
            } else if (action == MotionEvent.ACTION_UP && !mVoiceSearchDialogFragment.isShowing()) {
                if (mActionBarAdapter.getCurrentTab() == TabState.ALL) {
                    mHandler.removeCallbacks(mUpdateVoiceRunnable);
                    mHandler.postDelayed(mUpdateVoiceRunnable, DELAY_TIME_AFTER_TOUCH);
                }
            }
        }
    }

    private Runnable mUpdateVoiceRunnable = new Runnable() {
        @Override
        public void run() {
            updateVoice();
        }
    };

    public void onPauseVcs() {
        if (VcsUtils.isVcsFeatureEnable()) {
            Log.i(TAG, "[onPause] [vcs] call stopVoiceSearch");
            // remove or dismiss no contacts dialog fragment when "home"
            FragmentManager fManager = mActivity.getFragmentManager();
            dismissDlgFrag(fManager);
            mVoiceSearchManager.stopVoice();
        }
    }

    public void onVoiceDialogClick(View v) {
        Log.d(TAG, "[onClickDialog][vcs] view id:" + v.getId());
        FragmentManager fManager = mActivity.getFragmentManager();
        dismissDlgFrag(fManager);
    }

    public void onResumeVcs() {
        if (VcsUtils.isVcsFeatureEnable()) {
            // /M:show vcs app guide when first launch contacts
            mIsShowingGuide = mVoiceSearchManager.setVcsAppGuideVisibility(mActivity, true,
                    mGuideFinishListener);
            updateVoice();
        }
    }

    public void onDestoryVcs() {
        if (VcsUtils.isVcsFeatureEnable()) {
            // destroy the last loader with id:0
            mVoiceSearchManager.destoryVoice();
            mVoiceSearchManager.setVcsAppGuideVisibility(mActivity, false, mGuideFinishListener);
            if (mVoiceIndicator != null) {
                mVoiceIndicator.updateIndicator(false);
                mVoiceIndicator = null;
            }
        }
    }

    public void onPageSelectedVcs() {
        if (VcsUtils.isVcsFeatureEnable() && mActionBarAdapter.getCurrentTab() == TabState.ALL) {
            Log.d(TAG, "[onPageSelectedVcs] [vcs] onVoiceSearchProcess.");
            updateVoice();
        } else {
            Log.d(TAG, "[onPageSelectedVcs] [vcs] " + "disbale vcs and current tab:"
                    + mActionBarAdapter.getCurrentTab());
        }
    }

    public void onActionVcs(int action) {
        switch (action) {
        case ActionBarAdapter.Listener.Action.START_SEARCH_MODE:
            // M: add for VCS-need to stop voice search contacts.@{
            if (VcsUtils.isVcsFeatureEnable()) {
                mVoiceSearchManager.disableVoice();
            }
            break;
        //case ActionBarAdapter.Listener.Action.STOP_SEARCH_MODE:
        case ActionBarAdapter.Listener.Action.STOP_SEARCH_AND_SELECTION_MODE:
            // M: add for VCS-need to restart voice search contacts.@{
            if (VcsUtils.isVcsFeatureEnable()) {
                updateVoice();
            }
            break;

        default:
            break;
        }

    }

    public void onSelectedTabChangedEx() {
        if (VcsUtils.isVcsFeatureEnable()) {
            Log.i(TAG, "[onSelectedTabChanged] [vcs] onVoiceSearchProcess");
            updateVoice();
        }
    }

    public void onCreateOptionsMenuVcs(Menu men) {
        if (VcsUtils.isVcsFeatureEnable()) {
            // Bug fix ALPS02071015, If the vcs status is disable,
            // we can't set enable when dialog is showing in here.
            Dialog dialog = (mVoiceSearchDialogFragment == null ? null : mVoiceSearchDialogFragment
                    .getDialog());
            if (dialog != null && mVoiceSearchManager != null && dialog.isShowing()
                    && !mVoiceSearchManager.isInEnableStatus()) {
                Log.i(TAG, "[onCreateOptionsMenuVcs]Don't enable vcs icon");
                return;
            }
            Log.d(TAG, "[onCreateOptionsMenuVcs]");
            mVcsItem = men.findItem(com.android.contacts.R.id.menu_vcs);
            // set item not clickable if need
            if (mVcsItem != null) {
                if (mVoiceIndicator != null) {
                    mVoiceIndicator.updateIndicator(false);
                    mVoiceIndicator = null;
                }
                mVoiceIndicator = new VoiceSearchIndicator(mVcsItem);
                updateVoice();
            }
        }
    }

    public void onPrepareOptionsMenuVcs(Menu menu) {
        boolean showVcsItem = VcsUtils.isVcsFeatureEnable()
                && (mActionBarAdapter.getCurrentTab() == TabState.ALL)
                && (mActionBarAdapter.isSearchMode() == false)
                && (mActionBarAdapter.isSelectionMode() == false);
        MenuItem item = menu.findItem(com.android.contacts.R.id.menu_vcs);
        if (item != null) {
            item.setVisible(showVcsItem);
        }
        if (VcsUtils.isVcsFeatureEnable() && !showVcsItem) {
            // if current not show vcs item,stop voice service.
            mVoiceSearchManager.disableVoice();
        }
        if (VcsUtils.isVcsFeatureEnable() && !VcsUtils.isVcsEnableByUser(mActivity)
                && showVcsItem) {
            // if current will show vcs item,and the vcs if disable by user.show
            // disable icon.
            // make sure the vcs be stop.
            mVoiceSearchManager.disableVoice();
            if (mVoiceIndicator == null) {
                Log.w(TAG, "[onPrepareOptionsMenuVcs] the mVoiceIndicator is null..");
            } else {
                mVoiceIndicator.updateIndicator(false);
            }
        }
        if (VcsUtils.isVcsFeatureEnable() && checkStartVcsCondition()) {
            if (mVoiceIndicator != null) {
                mVoiceIndicator.updateIndicator(true);
            } else {
                Log.w(TAG, "[onPrepareOptionsMenuVcs] the mVoiceIndicator is null..");
            }
        }
    }

    public void onVcsItemSelected() {
        boolean enableByUser = VcsUtils.isVcsEnableByUser(mActivity);
        boolean enableByUserCurrent = !enableByUser;
        VcsUtils.setVcsEnableByUser(enableByUserCurrent, mActivity);
        if (enableByUserCurrent) {
            updateVoice();
        } else {
            mVoiceSearchManager.disableVoice();
            Dialog dialog = mVoiceSearchDialogFragment == null ? null : mVoiceSearchDialogFragment
                    .getDialog();
            // Bug fix ALPS01647494, if current tab is not in people list,
            // dismiss the dialog.
            if (dialog != null && dialog.isShowing()) {
                Log.i(TAG, "[onVcsItemSelected] dismiss Dialog..");
                dialog.dismiss();
            }
        }
    }

}
