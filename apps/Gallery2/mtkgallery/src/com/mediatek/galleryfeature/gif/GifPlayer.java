package com.mediatek.galleryfeature.gif;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Debug;
import android.os.ParcelFileDescriptor;
import com.android.internal.util.MemInfoReader;
import com.mediatek.galleryfeature.drm.DrmHelper;
import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.Player;
import com.mediatek.galleryframework.base.ThumbType;
import com.mediatek.galleryframework.base.Work;
import com.mediatek.galleryframework.gl.GLIdleExecuter;
import com.mediatek.galleryframework.gl.MBitmapTexture;
import com.mediatek.galleryframework.gl.MGLCanvas;
import com.mediatek.galleryframework.gl.MTexture;
import com.mediatek.galleryframework.gl.GLIdleExecuter.GLIdleCmd;
import com.mediatek.galleryframework.util.BitmapUtils;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.Utils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

public class GifPlayer extends Player {
    private static final String TAG = "MtkGallery2/GifPlayer";
    // same as setting in FancyHelper
    public static final float FANCY_CROP_RATIO = (5.0f / 2.0f);
    public static final float FANCY_CROP_RATIO_LAND = (2.0f / 5.0f);
    public static final float RESIZE_RATIO = 3.0f;
    public static final int FRAME_COUNT_MAX = 20;
    public static final long BYTES_IN_KILOBYTE = 1024;
    public static final int MEMORY_HTRESHOLD = 100;
    public static final int FRAME_HTRESHOLD_MIN = 6;
    private ThumbType mThumbType;
    private GifDecoderWrapper mGifDecoderWrapper;
    private ParcelFileDescriptor mFD;
    private int mFrameCount;
    private int mCurrentFrameDuration;
    private int mCurrentFrameIndex;
    private int mWidth;
    private int mHeight;
    private boolean mIsPlaying = false;

    private Bitmap mTempBitmap;
    private Bitmap mNextBitmap;
    private MBitmapTexture mTexture;
    private DecodeJob mCurrentJob;
    private FrameBuffer[] mFrameBuffers = null;
    private boolean mIsCancelled;
    private GLIdleExecuter mGlIdleExecuter;
    private long mMiniMemFreeMb;
    private int mTargetSize;
    private long[] memInfos;

    public GifPlayer(Context context, MediaData data, OutputType outputType, ThumbType type,
            GLIdleExecuter glIdleExecuter) {
        super(context, data, outputType);
        mWidth = mMediaData.width;
        mHeight = mMediaData.height;
        mThumbType = type;
        mGlIdleExecuter = glIdleExecuter;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
        mMiniMemFreeMb = mi.foregroundAppThreshold / BYTES_IN_KILOBYTE / BYTES_IN_KILOBYTE;
        MtkLog.i(TAG, "<GifPlayer> mMiniMemFreeMb=" + mMiniMemFreeMb + " MB");
    }

    @Override
    protected boolean onPrepare() {
        return true;
    }

    @Override
    protected synchronized void onRelease() {
        MtkLog.i(TAG, "<onRelease> caption = " + mMediaData.caption);
        removeAllMessages();
    }

    @Override
    protected boolean onStart() {
        MtkLog.i(TAG, "<onStart> caption = " + mMediaData.caption);
        if (mMediaData.isDRM != 0) {
            byte[] buffer = DrmHelper.forceDecryptFile(mMediaData.filePath, false);
            if (buffer == null) {
                MtkLog.i(TAG, "<onPrepare> buffer == null, return false");
                return false;
            }
            mGifDecoderWrapper = GifDecoderWrapper
                    .createGifDecoderWrapper(buffer, 0, buffer.length);
        } else if (mMediaData.filePath != null && !mMediaData.filePath.equals("")) {
            mGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(mMediaData.filePath);
        } else if (mMediaData.uri != null) {
            try {
                mFD = mContext.getContentResolver().openFileDescriptor(mMediaData.uri, "r");
                FileDescriptor fd = mFD.getFileDescriptor();
                mGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(fd);
            } catch (FileNotFoundException e) {
                MtkLog.w(TAG, "<onPrepare> FileNotFoundException", e);
                Utils.closeSilently(mFD);
                mFD = null;
                return false;
            }
        }
        if (mGifDecoderWrapper == null) {
            return false;
        }
        if (mIsCancelled) {
            recycleDecoderWrapper();
            return false;
        }
        mWidth = mGifDecoderWrapper.getWidth();
        mHeight = mGifDecoderWrapper.getHeight();
        mFrameCount = getGifTotalFrameCount();
        mTargetSize = mThumbType.getTargetSize();
        MtkLog.d(TAG, " The image width and height = " + mWidth + " " + mHeight
                + " mThumbType.getTargetSize() = " + mTargetSize + " mFrameCount = "
                + mFrameCount);
        if (ThumbType.MICRO == mThumbType || ThumbType.FANCY == mThumbType) {
            resetFrameCount();
            MtkLog.d(TAG, " mFrameCount = " + mFrameCount + " frameNumber = " + mFrameCount
                    + " mIsCancelled = " + mIsCancelled);
            mFrameBuffers = new FrameBuffer[mFrameCount];
            for (int i = 0; i < mFrameCount; i++) {
                if (mIsCancelled) {
                    recycleDecoderWrapper();
                    releaseFrameBuffer();
                    return false;
                }
                Bitmap bitmap = mGifDecoderWrapper.getFrameBitmap(i);
                int frameDuration = mGifDecoderWrapper.getFrameDuration(i);
                if (bitmap != null) {
                    bitmap = resizeBitmap(bitmap);
                    byte[] array = BitmapUtils.compressToBytes(bitmap);
                    mFrameBuffers[i] = new FrameBuffer(array, frameDuration);
                    bitmap.recycle();
                    bitmap = null;
                }
            }
            recycleDecoderWrapper();
            if (mFrameBuffers[0] != null) {
                PlatformHelper.submitJob(new DecodeJob(0, mFrameBuffers[0].data));
            }
        } else {
            PlatformHelper.submitJob(new DecodeJob(0));
        }

        mIsPlaying = true;
        mCurrentFrameIndex = 0;
        mCurrentFrameDuration = getGifFrameDuration(mCurrentFrameIndex);
        sendFrameAvailable();
        sendPlayFrameDelayed(0);
        return true;
    }

    @Override
    protected synchronized boolean onPause() {
        MtkLog.i(TAG, "<onPause> caption = " + mMediaData.caption);
        mIsPlaying = false;
        removeAllMessages();
        if (mCurrentJob != null) {
            mCurrentJob.cancel();
            mCurrentJob = null;
        }
        recycleDecoderWrapper();
        if (mNextBitmap != null) {
            mNextBitmap.recycle();
            mNextBitmap = null;
        }
        if (mTempBitmap != null) {
            mTempBitmap.recycle();
            mTempBitmap = null;
        }
        if (mGlIdleExecuter != null) {
            mGlIdleExecuter.addOnGLIdleCmd(new GLIdleCmd() {

                @Override
                public boolean onGLIdle(MGLCanvas canvas) {
                    if (mTexture != null) {
                        mTexture.recycle();
                        mTexture = null;
                    }
                    return false;
                }
            });
        } else {
            if (mTexture != null) {
                mTexture.recycle();
                mTexture = null;
            }
        }
        if (mFD != null) {
            Utils.closeSilently(mFD);
            mFD = null;
        }
        releaseFrameBuffer();
        return true;
    }

    @Override
    protected boolean onStop() {
        MtkLog.i(TAG, "<onStop> caption = " + mMediaData.caption);
        removeAllMessages();
        mCurrentFrameIndex = 0;
        mCurrentFrameDuration = getGifFrameDuration(mCurrentFrameIndex);
        return true;
    }

    public int getOutputWidth() {
        if (mTexture != null) {
            return mWidth;
        }
        return 0;
    }

    public int getOutputHeight() {
        if (mTexture != null) {
            return mHeight;
        }
        return 0;
    }

    protected synchronized void onPlayFrame() {
        if (!mIsPlaying) {
            return;
        }
        if (mNextBitmap != null) {
            mNextBitmap.recycle();
            mNextBitmap = null;
        }
        mNextBitmap = mTempBitmap;
        mTempBitmap = null;
        sendFrameAvailable();
        sendPlayFrameDelayed(mCurrentFrameDuration);

        if (mCurrentJob != null) {
            return;
        }

        mCurrentFrameIndex++;
        if (mCurrentFrameIndex >= mFrameCount) {
            mCurrentFrameIndex = 0;
        }
        mCurrentFrameDuration = getGifFrameDuration(mCurrentFrameIndex);
        if (mFrameBuffers != null && mCurrentFrameDuration != GifDecoderWrapper.INVALID_VALUE) {
            mCurrentJob = new DecodeJob(mCurrentFrameIndex, mFrameBuffers[mCurrentFrameIndex].data);
        } else {
            mCurrentJob = new DecodeJob(mCurrentFrameIndex);
        }
        PlatformHelper.submitJob(mCurrentJob);
    }

    public synchronized MTexture getTexture(MGLCanvas canvas) {
        if (mNextBitmap != null) {
            if (mTexture != null) {
                mTexture.recycle();
            }
            mTexture = new MBitmapTexture(mNextBitmap);
            mNextBitmap = null;
        }
        return mTexture;
    }

    @Override
    public void onCancel() {
        mIsCancelled = true;
    }

    private int getGifTotalFrameCount() {
        if (mGifDecoderWrapper == null) {
            return 0;
        }
        return mGifDecoderWrapper.getTotalFrameCount();
    }

    private int getGifFrameDuration(int frameIndex) {
        int index = GifDecoderWrapper.INVALID_VALUE;
        if (mFrameBuffers != null && mFrameBuffers[frameIndex] != null) {
            index = mFrameBuffers[frameIndex].frameDuration;
        } else if (mGifDecoderWrapper != null) {
            index = mGifDecoderWrapper.getFrameDuration(frameIndex);
        }
        return index;
    }

    class DecodeJob implements Work<Bitmap> {
        private int mIndex;
        private boolean mCanceled = false;
        private byte[] mCurrentFrameBuffer;

        public DecodeJob(int index, byte[] data) {
            mIndex = index;
            mCurrentFrameBuffer = data;
        }

        public DecodeJob(int index) {
            mIndex = index;
        }

        @Override
        public Bitmap run() {
            Bitmap bitmap = null;
            synchronized (GifPlayer.this) {
                if (isCanceled() || (mFrameBuffers == null && mGifDecoderWrapper == null)) {
                    MtkLog.i(TAG, "<DecodeJob.onDo> isCanceled() = " + isCanceled()
                            + ",mGifDecoderWrapper = " + mGifDecoderWrapper + ", return");
                    onDoFinished(bitmap);
                    return null;
                }
                if (mFrameBuffers != null) {
                    bitmap = decodeBitmap(mCurrentFrameBuffer);
                    mCurrentFrameBuffer = null;
                } else {
                    bitmap = mGifDecoderWrapper.getFrameBitmap(mIndex);
                    bitmap = resizeBitmap(bitmap);
                }
            }
            onDoFinished(bitmap);
            return bitmap;
        }

        private void onDoFinished(Bitmap bitmap) {
            synchronized (GifPlayer.this) {
                if (mTempBitmap != null) {
                    mTempBitmap.recycle();
                    mTempBitmap = null;
                }
                if (bitmap != null) {
                    if (!isCanceled()) {
                        mTempBitmap = bitmap;
                    } else {
                        bitmap.recycle();
                        bitmap = null;
                    }
                }
                mCurrentJob = null;
            }
        }

        @Override
        public boolean isCanceled() {
            return mCanceled;
        }

        public void cancel() {
            mCanceled = true;
        }
    }

    /**
     * This class is used to cache the buffer of gif frames.
     */
    class FrameBuffer {
        public byte[] data;
        public int frameDuration;

        public FrameBuffer(byte[] bitmapBuffer, int duration) {
            this.data = bitmapBuffer;
            frameDuration = duration;
        }

        public void recycle() {
            if (data != null) {
                data = null;
            }
        }
    }

    private Bitmap decodeBitmap(byte[] frameBuffer) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        return BitmapFactory.decodeByteArray(frameBuffer, 0, frameBuffer.length, options);
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        float ratio = (float) mHeight / (float) mWidth;
        if (mThumbType == ThumbType.MICRO) {
            bitmap = BitmapUtils.resizeAndCropCenter(bitmap, mTargetSize, true);
        } else if (mThumbType == ThumbType.FANCY && ratio > FANCY_CROP_RATIO) {
            bitmap = BitmapUtils.resizeAndCropByScale(bitmap, mTargetSize, Math.round(mTargetSize
                    * FANCY_CROP_RATIO), true, true);
        } else if (mThumbType == ThumbType.FANCY && ratio < FANCY_CROP_RATIO_LAND) {
            bitmap = BitmapUtils.resizeAndCropByScale(bitmap, mTargetSize, Math.round(mTargetSize
                    * FANCY_CROP_RATIO_LAND), false, true);
        } else if (mThumbType == ThumbType.MIDDLE && ratio > RESIZE_RATIO) {
            bitmap = BitmapUtils.resizeDownBySideLength(bitmap, (int) (mTargetSize * RESIZE_RATIO),
                    true);
        } else {
            bitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
        }
        bitmap = BitmapUtils.replaceBackgroundColor(bitmap,
                GifItem.GIF_BACKGROUND_COLOR, true);
        return bitmap;
    }

    private void resetFrameCount() {
        if (mFrameCount > FRAME_COUNT_MAX) {
            mFrameCount = FRAME_COUNT_MAX;
            MtkLog.d(TAG, "reset frame count " + mFrameCount);
        }
        if (mFrameCount > FRAME_HTRESHOLD_MIN) {
            MemInfoReader memInfoReader = new MemInfoReader();
            memInfoReader.readMemInfo();
            long[] memInfos = memInfoReader.getRawInfo();
            long cached = memInfos[Debug.MEMINFO_CACHED] / BYTES_IN_KILOBYTE;
            long free = memInfos[Debug.MEMINFO_FREE] / BYTES_IN_KILOBYTE;
            long memFreeDiffMb = (cached > free) ? cached : free - mMiniMemFreeMb;
            if (memFreeDiffMb < MEMORY_HTRESHOLD) {
                mFrameCount = mFrameCount / 2;
            }
            MtkLog.d(TAG, "<resetFrameCount> memFreeDiffMb = " + memFreeDiffMb + " mFrameCount = "
                    + mFrameCount);
        }
    }

    private void releaseFrameBuffer() {
        if (mFrameBuffers != null) {
            for (int i = 0; i < mFrameCount; i++) {
                if (mFrameBuffers[i] != null) {
                    mFrameBuffers[i].recycle();
                    mFrameBuffers[i] = null;
                }
            }
            mFrameBuffers = null;
            System.gc();
        }
    }

    private void recycleDecoderWrapper() {
        if (mGifDecoderWrapper != null) {
            mGifDecoderWrapper.close();
            mGifDecoderWrapper = null;
        }
    }

}
