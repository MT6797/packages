package com.mediatek.galleryfeature.btovgenerator;

import com.mediatek.galleryframework.util.MtkLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Help to write binary file for VideoWriter.
 */
class FileWriter {
    private static final String TAG = "MtkGallery2/FileWriter";

    private static FileChannel sFileChannel;
    private static ByteBuffer sHeaderBuf = ByteBuffer.allocate(2 * 1024);

    public static void openFile(String path) {
        MtkLog.d(TAG, "openFile: " + path);
        try {
            File file = new File(path.substring(0, path.lastIndexOf("/")));
            if (!file.exists()) {
                file.mkdirs();
            }
            sFileChannel = new RandomAccessFile(path, "rw").getChannel();
        } catch (FileNotFoundException e) {
            MtkLog.d(TAG, "openFile: file not found exception");
        }
        sHeaderBuf.clear();
    }

    public static int getCurBufPos() {
        return sHeaderBuf.position();
    }

    public static void setBufferData(int pos, int data) {
        int curPos = sHeaderBuf.position();
        sHeaderBuf.position(pos);
        sHeaderBuf.putInt(data);
        sHeaderBuf.position(curPos);
    }

    public static void setFileData(int pos, int data) {
        if (sFileChannel == null) {
            MtkLog.d(TAG, "setFileData, FileChannel is null");
            return;
        }

        sHeaderBuf.putInt(data);
        sHeaderBuf.flip();
        try {
            sFileChannel.write(sHeaderBuf, pos);
        } catch (IOException e) {
            MtkLog.d(TAG, "set file data error");
        }
        sHeaderBuf.clear();
    }

    public static void writeBufToFile() {
        if (sFileChannel == null) {
            MtkLog.d(TAG, "FileChannel is null");
            return;
        }

        MtkLog.d(TAG, "write buf to file,lenght:" + sHeaderBuf.position());
        sHeaderBuf.flip();
        try {
            sFileChannel.write(sHeaderBuf);
        } catch (IOException e) {
            MtkLog.d(TAG, "write buf to file error");
        }
        sHeaderBuf.clear();
    }

    public static void close() {
        MtkLog.d(TAG, "file writer close");
        if (sFileChannel == null) {
            MtkLog.d(TAG, "close, FileChannel is null");
            return;
        }
        try {
            sFileChannel.close();
        } catch (IOException e) {
            MtkLog.d(TAG, "file writer close error");
        }
    }

    public static void writeInt8(byte data) {
        sHeaderBuf.put(data);
    }
    public static void writeBytes(byte[] data) {
        sHeaderBuf.put(data);
    }
    public static void writeInt16(short data) {
        sHeaderBuf.putShort(data);
    }
    public static void writeInt32(int data) {
        sHeaderBuf.putInt(data);
    }
    public static void writeString(String str, int len) {
        if (str.length() != len) {
            throw new AssertionError();
        }
        sHeaderBuf.put(str.getBytes());
    }

    public static void writeBitStreamToFile(byte[] outData, int length) {
        if (sFileChannel == null) {
            MtkLog.d(TAG, "FileChannel is null");
            return;
        }
        if (outData.length != length) {
            throw new AssertionError();
        }

        MtkLog.d(TAG, "writeBitStream,length:" + outData.length);
        try {
            sFileChannel.write(ByteBuffer.wrap(outData));
        } catch (IOException e) {
            MtkLog.d(TAG, "write bit stream error");
        }
    }
}
