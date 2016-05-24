package com.mediatek.galleryfeature.btovgenerator;

import com.mediatek.galleryframework.util.MtkLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Counterpart of "full box" concept in MP4 spec.
 * A full box is a box which has additional version and flags in MP4.
 */
class FullBox extends Box {
    private static final String TAG = "MtkGallery2/FullBox";
    private static final int TIME_SCALE = 1000;

    public static int sCreateTime;
    public static int sFrameNumber = 0;
    public static float sFps = 0;
    public static int sWidth;
    public static int sHeight;
    public static int sMediaTimeScale;

    public int mTrackID = 1;
    public boolean mIsAudio = false;
    private short mVersion;
    private short mFlags;
    private ArrayList<Entries> mArray = new ArrayList<Entries>();

    public FullBox(String type, int version, int flags) {
        super(type);
        mVersion = (short) version;
        mFlags = (short) flags;
    }

    public void add(int...data) {
        mArray.add(new Entries(data));
    }

    public void write() {
        super.write();
        FileWriter.writeInt16(mVersion);
        FileWriter.writeInt16(mFlags);
        String name = mType.replaceFirst(mType.trim().substring(0, 1), mType.trim().substring(0, 1)
                .toUpperCase());
        String methodName = "write" + name + "Box";

        try {
            Method method = this.getClass().getMethod(methodName);
            method.invoke(this);
        } catch (NoSuchMethodException e) {
            MtkLog.d(TAG, "not find method:" + methodName + ",type:" + mType);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void writeMvhdBox() {
        FileWriter.writeInt32(sCreateTime); //crate time
        FileWriter.writeInt32(sCreateTime); //modification time
        FileWriter.writeInt32(TIME_SCALE); //time scale
        FileWriter.writeInt32((int) (sFrameNumber * TIME_SCALE / sFps)); //duration
        FileWriter.writeInt32(0x00010000); //rate
        FileWriter.writeInt16((short) 0x0100); //volume
        FileWriter.writeInt16((short) 0); //reversed
        FileWriter.writeInt32(0); //reversed
        FileWriter.writeInt32(0); //reversed

        FileWriter.writeInt32(0x00010000); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0x00010000); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0x40000000); //matrix

        FileWriter.writeInt32(0); //pre_defined
        FileWriter.writeInt32(0); //pre_defined
        FileWriter.writeInt32(0); //pre_defined
        FileWriter.writeInt32(0); //pre_defined
        FileWriter.writeInt32(0); //pre_defined
        FileWriter.writeInt32(0); //pre_defined

        FileWriter.writeInt32(2); //next_track_ID
    }

    public void writeTkhdBox() {
        FileWriter.writeInt32(sCreateTime); //crate time
        FileWriter.writeInt32(sCreateTime); //modification time
        FileWriter.writeInt32(mTrackID); //track id
        FileWriter.writeInt32(0); //reversed
        FileWriter.writeInt32((int) (sFrameNumber * TIME_SCALE / sFps));
        FileWriter.writeInt32(0); //reversed
        FileWriter.writeInt32(0); //reversed
        FileWriter.writeInt16((short) 0); //layer
        FileWriter.writeInt16((short) 0); //alternate_group
        if (mIsAudio) {
            FileWriter.writeInt16((short) 0x0100); //volume
        } else {
            FileWriter.writeInt16((short) 0); //volume
        }
        FileWriter.writeInt16((short) 0); //reversed

        FileWriter.writeInt32(0x00010000); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0x00010000); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0); //matrix
        FileWriter.writeInt32(0x40000000); //matrix

        FileWriter.writeInt32(sWidth << 16); //width
        FileWriter.writeInt32(sHeight << 16); //height
    }

    public void writeMdhdBox() {
        FileWriter.writeInt32(sCreateTime); //crate time
        FileWriter.writeInt32(sCreateTime); //modification time
        FileWriter.writeInt32(sMediaTimeScale); //media time scale
        FileWriter.writeInt32((int) (sFrameNumber * sMediaTimeScale / sFps)); //duration
        FileWriter.writeInt32(0); //pad,language,pre_defined
    }

    public void writeHdlrBox() {
        FileWriter.writeInt32(0); //pre_defined
        FileWriter.writeString(mIsAudio ? "soun" : "vide", 4); //handler_type
        FileWriter.writeInt32(0); //reserved
        FileWriter.writeInt32(0); //reserved
        FileWriter.writeInt32(0); //reserved
        FileWriter.writeString(mIsAudio ? "SoundHandle " : "VideoHandle ", 12); //handler_type
    }

    public void writeVmhdBox() {
        FileWriter.writeInt16((short) 0);
        FileWriter.writeInt16((short) 0);
        FileWriter.writeInt16((short) 0);
        FileWriter.writeInt16((short) 0);
    }

    public void writeDrefBox() {
        FileWriter.writeInt32(1); //entry_count
    }

    public void writeStsdBox() {
        FileWriter.writeInt32(1); //entry_count
    }

    public void writeSttsBox() {
        FileWriter.writeInt32(mArray.size()); //entry_count
        for (Entries entries : mArray) {
            entries.write();
        }
    }

    public void writeStssBox() {
        FileWriter.writeInt32(mArray.size()); //entry_count
        for (Entries entries : mArray) {
            entries.write();
        }
    }

    public void writeStszBox() {
        FileWriter.writeInt32(0); //sample_size
        FileWriter.writeInt32(mArray.size()); //entry_count
        for (Entries entries : mArray) {
            entries.write();
        }
    }

    public void writeStscBox() {
        FileWriter.writeInt32(mArray.size()); //entry_count
        for (Entries entries : mArray) {
            entries.write();
        }
    }

    public void writeStcoBox() {
        FileWriter.writeInt32(mArray.size()); //entry_count
        for (Entries entries : mArray) {
            entries.write();
        }
    }

    /**
     * Helper class to write a number of integers.
     */
    private class Entries {
        private int mData[];
        public Entries(int...data) {
            mData = data;
        }
        public void write() {
            for (int data : mData) {
                FileWriter.writeInt32(data);
            }
        }
    }
}
