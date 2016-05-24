
package com.mediatek.gallery3d.video;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.android.gallery3d.R;

import com.mediatek.galleryfeature.animshare.AnimatedContentSharer;
import com.mediatek.galleryfeature.animshare.AnimatedContentSharer.IMediaDataGetter;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.RotateProgressFragment;
import com.mediatek.galleryframework.util.SimpleThreadPool;
import com.mediatek.galleryframework.util.SimpleThreadPool.Job;

public class SlowMotionSharer {
    private static final String TAG = "MtkGallery2/AnimatedContentSharerForThumbnails";
    private static final String DIALOG_TAG_GENERATING_PROGRESS = "DIALOG_TAG_GENERATING_PROGRESS";
    private final IShareContext mShareContext;
    private final Activity mActivity;
    private DialogFragment mDialogProgress;
    private ArrayList<Uri> mOutUris;
    private int mResultCode;
    private int mResultMessageResourceId;

    public static class ShareHooker implements AnimatedContentSharer.IShareHooker {
        @Override
        public boolean share(final Activity activity, final Intent intent,
                final IMediaDataGetter mediaDataGetter) {
            MediaData mediaData = mediaDataGetter.getMediaData();
            if ((mediaData != null) && mediaData.isSlowMotion) {
                // It's a bug that mediaData.uri == null
                if (mediaData.uri == null) {
                    mediaData.uri = (Uri) (intent.getParcelableExtra(Intent.EXTRA_STREAM));
                }
                // check the slowmotion speed whether is 16x
                SlowMotionItem item = new SlowMotionItem(activity, mediaData.uri);
                if (item.getSpeed() == SlowMotionItem.SLOW_MOTION_ONE_SIXTEENTH_SPEED
                        || item.getSpeed() == SlowMotionItem.SLOW_MOTION_ONE_THIRTY_TWO_SPEED) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(activity,
                                    R.string.not_avaliable_high_speed_single_share,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    return true;
                }

                final List<MediaData> mediaDatas = new ArrayList<MediaData>();
                mediaDatas.add(mediaData);
                final SlowMotionSharer animSharer = new SlowMotionSharer(
                        new SlowMotionSharer.IShareContext() {
                            public boolean isCancelled() {
                                // share context needs redefining
                                return false; // shareContext.isCancelled();
                            }

                            public Activity getActivity() {
                                return activity;
                            }
                        });

                if (animSharer != null) {
                    Job job = new Job() {
                        public boolean isCanceled() {
                            return false; // to mean, do not support cancel in
                                          // PhotoPage
                        }

                        public void onDo() {
                            animSharer.share(mediaDatas);
                            ArrayList<Uri> uris = animSharer.getShareUris();
                            // ignore cancel, and go on sharing
                            // int resCode = animSharer.getResultCode();
                            // if(resCode==AnimatedContentSharerForThumbnails.RESULT_CODE_CANCEL){
                            // return null;
                            // }
                            if (!uris.isEmpty()) {
                                final Uri uri = uris.get(0);
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                                        mediaDataGetter.setShareUri(uri);
                                        activity.startActivity(intent);
                                    }
                                });
                            }
                        }
                    };

                    SimpleThreadPool.getInstance().submitAsyncJob(job);
                }
                return true;
            }

            return false;
        }
    }

    public SlowMotionSharer(final IShareContext shareContext) {
        MtkLog.i(TAG, "<new>");
        mShareContext = shareContext;
        mActivity = shareContext.getActivity();
        removeOldFragmentByTag(DIALOG_TAG_GENERATING_PROGRESS);
    }

    private void removeOldFragmentByTag(String tag) {
        MtkLog.i(TAG, "<removeOldFragmentByTag> start, tag = " + tag);
        FragmentManager fragmentManager = mActivity.getFragmentManager();
        DialogFragment oldFragment = (DialogFragment) fragmentManager.findFragmentByTag(tag);
        MtkLog.i(TAG, "<removeOldFragmentByTag> oldFragment = " + oldFragment);
        if (null != oldFragment) {
            oldFragment.dismissAllowingStateLoss();
            MtkLog.i(TAG, "<removeOldFragmentByTag> remove oldFragment success");
        }
        MtkLog.i(TAG, "<removeOldFragmentByTag> end");
    }

    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_CANCEL = -1;

    public static final int RESULT_MESSAGE_OK = 0;

    public interface IShareContext {
        public boolean isCancelled();

        public Activity getActivity();
    }

    private class SlowMotionTranscoder {
        private final IShareContext mShareContext;
        private int mResultCode;
        private int mResultMsgResId;

        public SlowMotionTranscoder(final IShareContext shareContext) {
            mShareContext = shareContext;
        }

        public Uri transcode(final Uri inUri) {
            // simulate transcoding
            // out: outUri, error code and/or error message
            // if shareContext.isCancelled(), then return as soon as possible
            MtkLog.i("matt", "transcode uri " + inUri);
            Uri outUri = inUri;
            try {
                Thread.sleep(8000);
                mResultCode = RESULT_CODE_OK; // just simulation
                // mErrMsgResId = R.string.xxx; // if mErrCode != 0;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return outUri;
        }

        public int getErrorCode() {
            return mResultCode;
        }

        public int getErrorMessageResourceId() {
            return mResultMsgResId;
        }
    }

    private void startShare() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                MtkLog.i(TAG, "show genProgressDialog.");
                final String generateTip = mActivity.getString(R.string.m_please_wait);
                removeOldFragmentByTag(DIALOG_TAG_GENERATING_PROGRESS);
                final DialogFragment genProgressDialog = RotateProgressFragment
                        .newInstance(generateTip);
                genProgressDialog.setCancelable(false);
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.add(genProgressDialog, DIALOG_TAG_GENERATING_PROGRESS);
                ft.commitAllowingStateLoss();
                mDialogProgress = genProgressDialog;
            }
        });
    }

    private void endShare() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                // !_! why this can't work?
                // removeOldFragmentByTag(DIALOG_TAG_GENERATING_PROGRESS);
                MtkLog.i(TAG, "dismissAllowingStateLoss.");
                if (mDialogProgress != null) {
                    mDialogProgress.dismissAllowingStateLoss();
                }
            }
        });
    }

    private void promptMessage(final int msgId) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivity,
                        msgId, Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * @Title countSlowmotion
     * @Description count slow motion media
     * @param List<MediaData> mediaDatas meidas goting to share
     * @return slowMotionCounts counts of slow motion media
     */
    private int countSlowmotion(List<MediaData> mediaDatas) {
        int slowMotionCounts = 0;
        for (MediaData mediaData : mediaDatas) {
            if (!mediaData.isSlowMotion) {
                continue;
            } else {
                slowMotionCounts++;
                if (slowMotionCounts >= 2) {
                    break;
                }
            }
        }
        return slowMotionCounts;
    }

    /**
     * @Title addforShare
     * @Description share midiaDate
     * @param List<MediaData> mediaDatas meida waiting to share TranscodeSMVideo
     *            transcoder slow motion video going to share
     * @return smFailCounts slow moting video fail to share count
     */
    private int addforShare(List<MediaData> mediaDatas, TranscodeSMVideo transcoder) {
        int smFailCounts = 0;
        int resultCode = RESULT_CODE_OK;
        ArrayList<Uri> outUris = new ArrayList<Uri>();
        for (MediaData mediaData : mediaDatas) {
            // cancelled, return as soon as possible
            if (mShareContext.isCancelled()) {
                mResultCode = RESULT_CODE_CANCEL;
                return -1;
            }

            if (!mediaData.isSlowMotion) {
                // not slow motion, normal sharing way
                outUris.add(mediaData.uri);
                continue;
            }

            // if transcoder is null, don't do transcode and continue
            if (transcoder == null) {
                continue;
            }

            Uri outUri = transcoder.transcode(mediaData.uri);
            if (mShareContext.isCancelled()) {
                mResultCode = RESULT_CODE_CANCEL;
                return -1;
            }

            resultCode = transcoder.getErrorCode();
            MtkLog.i(TAG, "transcoder.getErrorCode = " + resultCode);
            if (resultCode == RESULT_CODE_OK) {
                outUris.add(outUri);
            } else {
                // TODO error handle

                if (resultCode == TranscodeSMVideo.TRANSCODE_FULL_STORAGE) {
                    smFailCounts++;
                }
                continue;
            }
        }
        // outUris contains the final Uris for share
        // result msg would be used for toast tip (or there isn't any tip by UX)
        mOutUris = outUris;
        mResultCode = resultCode;
        return smFailCounts;

    }

    /**
     * @Title share
     * @Description share midiaDate
     * @param List<MediaData> mediaDatas meida waiting to share
     * @return smFailCounts shareAngel.resultCode: OK: caller should go on
     *         computing share intent CANCEL: caller should cancel computing
     *         share intent
     */
    public void share(List<MediaData> mediaDatas) {
        MtkLog.i(TAG, "share");
        startShare();

        int resultMsgResId = RESULT_MESSAGE_OK;
        TranscodeSMVideo transcoder = null;

        // check whether is single share
        MtkLog.i(TAG, "check whether is single share");
        boolean isSingleShare = false;
        int slowMotionCounts = 0;
        int smFailCounts = 0;
        int totalShareCounts = mediaDatas.size();

        slowMotionCounts = countSlowmotion(mediaDatas);
        isSingleShare = slowMotionCounts >= 2 ? false : true;

        // initialize TranscodeSMVideo
        // do slow motion transcoding and share the out uri
        if (slowMotionCounts > 0) {
            transcoder = new TranscodeSMVideo(mActivity);
            transcoder.initialize(isSingleShare, totalShareCounts);
        }
        MtkLog.i(TAG, "initialize TranscodeSMVideo, isSingleShare = " + isSingleShare
                + ", slowMotionCounts = " + slowMotionCounts);

        smFailCounts = addforShare(mediaDatas, transcoder);
        if (smFailCounts == -1) {
            return;
        }
        // error toast
        if (smFailCounts == 1 && totalShareCounts == 1) {
            promptMessage(R.string.m_insufficient_space_one);
        } else if (smFailCounts > 0 && smFailCounts < slowMotionCounts) {
            promptMessage(R.string.m_insufficient_space_some);
        } else if (smFailCounts > 0 && smFailCounts == slowMotionCounts) {
            promptMessage(R.string.m_insufficient_space_all);
        }

        mResultMessageResourceId = resultMsgResId;
        MtkLog.i(TAG, "endShare ");
        endShare();
    }

    public ArrayList<Uri> getShareUris() {
        return mOutUris;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public int getResultMessageResourceId() {
        return mResultMessageResourceId;
    }
}
