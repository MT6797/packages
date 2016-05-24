/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.xmp;

import android.util.Log;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.options.SerializeOptions;
import com.adobe.xmp.properties.XMPProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * XmpInterface.
 *
 */
public class XmpInterface {
    private static final String TAG = "MtkGallery2/Xmp/XmpInterface";
    public static final String XMP_EXT_MAIN_HEADER1 =
            "http://ns.adobe.com/xmp/extension/";
    public static final String EXIF_HEADER = "Exif";
    public static final String XMP_HEADER_START = "http://ns.adobe.com/xap/1.0/\0";
    public static final String XMP_EXT_HEADER = "http://ns.adobe.com/xmp/extension/";
    public static final String MEDIATEK_SEGMENT_NAMESPACE = "http://ns.mediatek.com/segment/";
    public static final String MTK_SEGMENT_PREFIX = "MSegment";
    public static final String SEGMENT_MASK_WIDTH = "SegmentMaskWidth";
    public static final String SEGMENT_MASK_HEIGHT = "SegmentMaskHeight";
    public static final String SEGMENT_X = "SegmentX";
    public static final String SEGMENT_Y = "SegmentY";
    public static final String SEGMENT_LEFT = "SegmentLeft";
    public static final String SEGMENT_TOP = "SegmentTop";
    public static final String SEGMENT_RIGHT = "SegmentRight";
    public static final String SEGMENT_BOTTOM = "SegmentBottom";

    public static final String SEGMENT_FACE_STRUCT_NAME = "FDInfo";
    public static final String SEGMENT_FACE_FIELD_NS = "FD";
    public static final String SEGMENT_FACE_PREFIX = "FD";
    public static final String SEGMENT_FACE_LEFT = "FaceLeft";
    public static final String SEGMENT_FACE_TOP = "FaceTop";
    public static final String SEGMENT_FACE_RIGHT = "FaceRight";
    public static final String SEGMENT_FACE_BOTTOM = "FaceBottom";
    public static final String SEGMENT_FACE_RIP = "FaceRip";
    public static final String SEGMENT_FACE_COUNT = "FaceCount";

    public static final int SOI = 0xFFD8;
    public static final int SOS = 0xFFDA;
    public static final int APP1 = 0xFFE1;
    public static final int APP15 = 0xFFEF;
    public static final int DQT = 0xFFDB;
    public static final int DHT = 0xFFC4;

    public static final int WRITE_XMP_AFTER_SOI = 0;
    public static final int WRITE_XMP_BEFORE_FIRST_APP1 = 1;
    public static final int WRITE_XMP_AFTER_FIRST_APP1 = 2;
    public static final int FIXED_BUFFER_SIZE = 1024 * 10;
    public static final long COPY_ALL_REMAINING_DATA = -1;
    public static final int MAX_BYTE_PER_APP1 = 0XFFB2;
    public static final int MD5_BYTE_COUNT = 32;
    public static final int TOTAL_LENGTH_BYTE_COUNT = 4;
    public static final int PARTITION_OFFSET_BYTE_COUNT = 4;
    public static final int XMP_COMMON_HEADER_LEN = XMP_EXT_HEADER.getBytes().length + 1
            + MD5_BYTE_COUNT + TOTAL_LENGTH_BYTE_COUNT + PARTITION_OFFSET_BYTE_COUNT;
    public static final int MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 = MAX_BYTE_PER_APP1
            - XMP_COMMON_HEADER_LEN;
    public static final int EXT_XMP_COMMON_HEADER_TOTAL_LEN_OFFSET = XMP_EXT_HEADER.length() + 1
            + MD5_BYTE_COUNT;
    public static final int EXT_XMP_COMMON_HEADER_REAL_DATA_OFFSET =
            EXT_XMP_COMMON_HEADER_TOTAL_LEN_OFFSET
            + TOTAL_LENGTH_BYTE_COUNT + PARTITION_OFFSET_BYTE_COUNT;

    private static final int APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT = 4;
    private static final int APP_SECTION_MAX_LENGTH = 0xffb2;
    private static final int APP15_LENGTHTAG_BYTE_COUNT = 4;
    private static final int TYPE_BUFFER_COUNT = 7;
    private static final int BIT_SHIFT_COUNT_8 = 8;
    private static final int BIT_SHIFT_COUNT_16 = 16;
    private static final int BIT_SHIFT_COUNT_24 = 24;
    private static final int BYTE_MASK_FF = 0xff;

    private static final int LOW_HALF_BYTE_MASK = 0X0F;
    private static final int HIGH_HALF_BYTE_MASK = 0XF0;
    private static final int BYTE_MASK = 0XFF;
    private static final int SHIFT_BIT_COUNT_4 = 4;
    private static final int SHIFT_BIT_COUNT_8 = 8;
    private static final int XMP_HEADER_TAIL_BYTE = 0X0;
    private static final int BYTE_COUNT_4 = 4;

    /*
    JPS DATA format:
    APP15 TAG (2 BYTES)
    APP15 Length (2 BYTES)
    JPS DATA length (4 BYTES)
    "JPSDATA"(7 BYTES)
    JPS SerialNumber(1 BYTE)
    JPSDATA (whole jps data)

    JPS MASK format:
    APP15 TAG (2 BYTES)
    APP15 Length (2 BYTES)
    JPS MASK length (4 BYTES)
    "JPSMASK"(7 BYTES)
    JPS SerialNumber(1 BYTE)
    JPSMASK (whole jps mask data)
    */

    public static final String TYPE_JPS_DATA = "JPSDATA";
    public static final String TYPE_JPS_MASK = "JPSMASK";
    public static final String TYPE_DEPTH_DATA = "DEPTHBF";
    public static final String TYPE_XMP_DEPTH = "XMPDEPT";
    public static final String TYPE_SEGMENT_MASK = "SEGMASK";
    // use 4 bytes for JPS data/mask length
    public static final int TOTAL_LENGTH_TAG_BYTE = 4;
    // use 1 byte for package serial number
    public static final int JPS_SERIAL_NUM_TAG_BYTE = 1;
    // same with XMP, but reserve 4 bytes for total length
    public static final int JPS_PACKET_SIZE = APP_SECTION_MAX_LENGTH
            - TOTAL_LENGTH_TAG_BYTE;
    public static final int JPS_PURE_DATA_SIZE_PER_PACKET = JPS_PACKET_SIZE
            - TYPE_JPS_DATA.length() - JPS_SERIAL_NUM_TAG_BYTE;
    public static final int JPS_PACKET_HEAD_SIZE_EXCLUDE_DATA = 2 + 2 + TOTAL_LENGTH_TAG_BYTE
            + TYPE_JPS_DATA.length() + JPS_SERIAL_NUM_TAG_BYTE;

    public static final int DEPTH_SERIAL_NUM_TAG_BYTE = JPS_SERIAL_NUM_TAG_BYTE;
    public static final int DEPTH_PACKET_SIZE = JPS_PACKET_SIZE;

    public static final int SEGMENT_MASK_PACKET_SIZE = JPS_PACKET_SIZE;
    public static final int SEGMENT_SERIAL_NUM_TAG_BYTE = JPS_SERIAL_NUM_TAG_BYTE;

    private ArrayList<Section> mParsedSectionsForGallery;
    private XMPSchemaRegistry mRegister = XMPMetaFactory.getSchemaRegistry();
    private XMPSchemaRegistry mExtXmpRegister = XMPMetaFactory.getSchemaRegistry();

    /**
     * Get md5 value.
     *
     * @param in
     *            byte array
     * @return md5 value
     */
    public static String getMd5(byte[] in) {
        byte[] md5 = md5(in);
        if (md5 == null || md5.length <= 0) {
            Log.d(TAG, "<getMd5> error!!");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = md5.length;
        for (int i = 0; i < len; i++) {
            int high = (md5[i] & HIGH_HALF_BYTE_MASK) >> SHIFT_BIT_COUNT_4;
            int low = md5[i] & LOW_HALF_BYTE_MASK;
            sb.append(Integer.toHexString(high));
            sb.append(Integer.toHexString(low));
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Get extend xmp header.
     *
     * @param md5
     *            md5 value
     * @param totalLength
     *            total length
     * @param sectionNumber
     *            section number
     * @return header
     */
    public static byte[] getXmpCommonHeader(String md5, int totalLength, int sectionNumber) {
        int offset = MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 * sectionNumber;
        byte[] header = new byte[XMP_COMMON_HEADER_LEN];
        // 1. copy header
        System.arraycopy(XMP_EXT_HEADER.getBytes(), 0, header, 0, XMP_EXT_HEADER.length());
        int currentPos = XMP_EXT_HEADER.length();
        // 2. copy tail byte
        header[currentPos] = XMP_HEADER_TAIL_BYTE;
        currentPos += 1;
        // 3. copy md5
        byte[] md5Buffer = md5.getBytes();
        System.arraycopy(md5Buffer, 0, header, currentPos, md5Buffer.length);
        currentPos += md5Buffer.length;
        // 4. copy 4 bytes totalLen
        byte[] totalLenBuffer = intToByteBuffer(totalLength, BYTE_COUNT_4);
        System.arraycopy(totalLenBuffer, 0, header, currentPos, totalLenBuffer.length);
        currentPos += totalLenBuffer.length;
        // 5. copy 4 bytes offset
        byte[] offsetBuffer = intToByteBuffer(offset, BYTE_COUNT_4);
        System.arraycopy(offsetBuffer, 0, header, currentPos, offsetBuffer.length);
        return header;
    }

    /**
     * ByteArrayInputStreamExt.
     *
     */
    public static class ByteArrayInputStreamExt extends ByteArrayInputStream {
        /**
         * ByteArrayInputStreamExt.
         *
         * @param buf
         *            buffer
         */
        public ByteArrayInputStreamExt(byte[] buf) {
            super(buf);
            Log.d(TAG, "<ByteArrayInputStreamExt> new instance, buf count 0x"
                    + Integer.toHexString(buf.length));
        }

        /**
         * read unsigned shot.
         *
         * @return read result
         */
        public final int readUnsignedShort() {
            int hByte = read();
            int lByte = read();
            return hByte << BIT_SHIFT_COUNT_8 | lByte;
        }

        /**
         * high byte first int.
         *
         * @return read result
         */
        public final int readInt() {
            int firstByte = read();
            int secondByte = read();
            int thirdByte = read();
            int forthByte = read();
            return firstByte << BIT_SHIFT_COUNT_24 | secondByte << BIT_SHIFT_COUNT_16
                    | thirdByte << BIT_SHIFT_COUNT_8 | forthByte;
        }

        /**
         * low byte first int.
         *
         * @return read result
         */
        public final int readReverseInt() {
            int forthByte = read();
            int thirdByte = read();
            int secondByte = read();
            int firstByte = read();
            return firstByte << BIT_SHIFT_COUNT_24 | secondByte << BIT_SHIFT_COUNT_16
                    | thirdByte << BIT_SHIFT_COUNT_8 | forthByte;
        }

        /**
         * seek.
         *
         * @param offset
         *            offext
         * @throws IOException
         *             exception
         */
        public synchronized void seek(long offset) throws IOException {
            if (offset > count - 1) {
                throw new IOException("offset out of buffer range: offset " + offset
                        + ", buffer count " + count);
            }
            pos = (int) offset;
        }

        /**
         * get file pointer.
         *
         * @return result
         */
        public synchronized long getFilePointer() {
            return pos;
        }

        /**
         * read.
         *
         * @param buffer
         *            buffer
         * @return result
         */
        public int read(byte[] buffer) {
            return read(buffer, 0, buffer.length);
        }
    }

    /**
     * ByteArrayOutputStreamExt.
     *
     */
    public static class ByteArrayOutputStreamExt extends ByteArrayOutputStream {
        /**
         * write short.
         *
         * @param val
         *            write data
         */
        public final void writeShort(int val) {
            int hByte = val >> BIT_SHIFT_COUNT_8;
            int lByte = val & BYTE_MASK_FF;
            write(hByte);
            write(lByte);
        }

        /**
         * write int.
         *
         * @param val
         *            data
         */
        public final void writeInt(int val) {
            int firstByte = val >> BIT_SHIFT_COUNT_24;
            int secondByte = (val >> BIT_SHIFT_COUNT_16) & BYTE_MASK_FF;
            int thirdByte = (val >> BIT_SHIFT_COUNT_8) & BYTE_MASK_FF;
            int forthByte = val & BYTE_MASK_FF;
            write(firstByte);
            write(secondByte);
            write(thirdByte);
            write(forthByte);
        }
    }

    /**
     * Section.
     *
     */
    public static class Section {
        // e.g. 0xffe1, exif
        public int mMarker;
        // marker offset from start of file
        public long mOffset;
        // app length, follow spec, include 2 length bytes
        public int mLength;
        public boolean mIsXmpMain;
        public boolean mIsXmpExt;
        public boolean mIsExif;
        public boolean mIsJpsData;
        public boolean mIsJpsMask;
        public boolean mIsDepthData;
        public boolean mIsXmpDepth;
        public boolean mIsSegmentMask;

        /**
         * Section.
         *
         * @param marker
         *            marker
         * @param offset
         *            offset
         * @param length
         *            length
         * @param isXmpMain
         *            isXmpMain
         * @param isXmpExt
         *            isXmpExt
         * @param isExif
         *            isExif
         * @param isJpsData
         *            isJpsData
         * @param isJpsMask
         *            isJpsMask
         * @param isDepthData
         *            isDepthData
         * @param isXmpDepth
         *            isXmpDepth
         * @param isSegmentMask
         *            isSegmentMask
         */
        public Section(int marker, long offset, int length, boolean isXmpMain, boolean isXmpExt,
                boolean isExif, boolean isJpsData, boolean isJpsMask, boolean isDepthData,
                boolean isXmpDepth, boolean isSegmentMask) {
            this.mMarker = marker;
            this.mOffset = offset;
            this.mLength = length;
            this.mIsXmpMain = isXmpMain;
            this.mIsXmpExt = isXmpExt;
            this.mIsExif = isExif;
            this.mIsJpsData = isJpsData;
            this.mIsJpsMask = isJpsMask;
            this.mIsDepthData = isDepthData;
            this.mIsXmpDepth = isXmpDepth;
            this.mIsSegmentMask = isSegmentMask;
        }
    }

    /**
     * setSectionInfo.
     *
     * @param sections
     *            sections array
     */
    public void setSectionInfo(ArrayList<Section> sections) {
        mParsedSectionsForGallery = sections;
    }

    /**
     * Copy file.
     *
     * @param rafIn
     *            src file
     * @param rafOut
     *            output file
     */
    public void copyFileWithFixBuffer(RandomAccessFile rafIn, RandomAccessFile rafOut) {
        if (rafIn == null || rafOut == null) {
            Log.d(TAG, "<copyFileWithFixBuffer> params error!!");
            return;
        }
        byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
        int readCount = 0;
        try {
            while ((readCount = rafIn.read(readBuffer)) != -1) {
                if (readCount == FIXED_BUFFER_SIZE) {
                    rafOut.write(readBuffer);
                } else {
                    rafOut.write(readBuffer, 0, readCount);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "<copyFileWithFixBuffer> IOException", e);
        }
    }

    /**
     * Copy file from start with copy length.
     *
     * @param rafIn
     *            src file
     * @param rafOut
     *            output file
     * @param start
     *            copy start
     * @param length
     *            copy length if length == COPY_ALL_REMAINING_DATA, copy all
     *            data from start to file end if length > 0, copy data with
     *            length
     */
    public void copyFileWithFixBuffer(RandomAccessFile rafIn, RandomAccessFile rafOut, long start,
            long length) {
        if (rafIn == null || rafOut == null || start < 0) {
            Log.d(TAG, "<copyFileWithFixBuffer> params error!!");
            return;
        }
        Log.d(TAG, "<copyFileWithFixBuffer> start " + start + ", length " + length);
        byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
        int readCount = 0;
        try {
            rafIn.seek(start);
            // copy all, end condition is that read() returns -1 when reach
            // streaming end
            if (length == COPY_ALL_REMAINING_DATA) {
                while ((readCount = rafIn.read(readBuffer)) != -1) {
                    if (readCount == FIXED_BUFFER_SIZE) {
                        rafOut.write(readBuffer);
                    } else {
                        rafOut.write(readBuffer, 0, readCount);
                    }
                }
            } else {
                // copy certain length
                int mod = (int) (length % FIXED_BUFFER_SIZE);
                Log.d(TAG, "<copyFileWithFixBuffer> mod " + mod);
                while (length > 0) {
                    if (length >= FIXED_BUFFER_SIZE) {
                        rafIn.read(readBuffer);
                        rafOut.write(readBuffer);
                        length -= FIXED_BUFFER_SIZE;
                    } else {
                        rafIn.read(readBuffer, 0, mod);
                        rafOut.write(readBuffer, 0, mod);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "<copyFileWithFixBuffer> IOException", e);
        }
    }

    /**
     * getXmpMetaFromFile.
     *
     * @param filePath
     *            file path
     * @return result
     */
    public XMPMeta getXmpMetaFromFile(String filePath) {
        File srcFile = new File(filePath);
        if (!srcFile.exists()) {
            Log.d(TAG, "<getXmpMetaFromFile> " + filePath + " not exists!!!");
            return null;
        }

        RandomAccessFile rafIn = null;
        XMPMeta meta = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Section sec = null;
            for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (sec.mIsXmpMain) {
                    rafIn.seek(sec.mOffset + 2);
                    int len = rafIn.readUnsignedShort() - 2;
                    int xmpLen = len - XMP_HEADER_START.length();
                    byte[] buffer = new byte[xmpLen];
                    rafIn.skipBytes(XMP_HEADER_START.length());
                    rafIn.read(buffer, 0, buffer.length);
                    meta = XMPMetaFactory.parseFromBuffer(buffer);
                    if (meta == null) {
                        Log.d(TAG, "<getXmpMetaFromFile> parsed XMPMeta is null, create one!!!");
                        meta = XMPMetaFactory.create();
                    } else {
                        Log.d(TAG, "<getXmpMetaFromFile> return parsed XMPMeta");
                    }
                    return meta;
                }
            }
            meta = XMPMetaFactory.create();
            Log.d(TAG, "<getXmpMetaFromFile> no xmp main, then create XMPMeta!!!");
            return meta;
        } catch (IOException e) {
            Log.e(TAG, "<getXmpMetaFromFile> IOException ", e);
            return null;
        } catch (XMPException e) {
            Log.e(TAG, "<getXmpMetaFromFile> XMPException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getXmpMetaFromFile> IOException when close ", e);
            }
        }
    }

    /**
     * Get extend xmp meta.
     *
     * @param extXmpData
     *            extend xmp data buffer
     * @return xmp meta
     */
    public XMPMeta getExtXmpMetaFromBuffer(byte[] extXmpData) {
        if (extXmpData == null) {
            Log.d(TAG, "<getExtXmpMetaFromBuffer> extXmpData is null!!");
            return null;
        }
        try {
            XMPMeta meta = XMPMetaFactory.parseFromBuffer(extXmpData);
            Log.d(TAG, "<getExtXmpMetaFromBuffer> " + meta);
            return meta;
        } catch (XMPException e) {
            Log.e(TAG, "<getExtXmpMetaFromBuffer> XMPException ", e);
            return null;
        }
    }

    /**
     * Get main xmp meta from section.
     *
     * @param rafIn
     *            rafIn
     * @param mainXmpSection
     *            main xmp section
     * @return main xmp meta
     */
    public XMPMeta getXmpMetaFromSection(RandomAccessFile rafIn, Section mainXmpSection) {
        if (rafIn == null || mainXmpSection == null) {
            Log.d(TAG, "<getXmpMetaFromSection> rafIn or mainXmpSection is null!!");
            return null;
        }

        try {
            rafIn.seek(mainXmpSection.mOffset + 2);
            int len = rafIn.readUnsignedShort() - 2;
            int xmpLen = len - XMP_HEADER_START.length();
            byte[] buffer = new byte[xmpLen];
            rafIn.skipBytes(XMP_HEADER_START.length());
            rafIn.read(buffer, 0, buffer.length);
            XMPMeta meta = XMPMetaFactory.parseFromBuffer(buffer);
            Log.d(TAG, "<getXmpMetaFromSection> " + meta);
            return meta;
        } catch (XMPException e) {
            Log.e(TAG, "<getXmpMetaFromSection> XMPException ", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "<getXmpMetaFromSection> IOException ", e);
            return null;
        }
    }

    /**
     * copyToStreamWithFixBuffer.
     *
     * @param is
     *            is
     * @param os
     *            os
     */
    public void copyToStreamWithFixBuffer(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os) {
        byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
        int readCount = 0;
        try {
            while ((readCount = is.read(readBuffer)) != -1) {
                if (readCount == FIXED_BUFFER_SIZE) {
                    os.write(readBuffer);
                } else {
                    os.write(readBuffer, 0, readCount);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "<copyToStreamWithFixBuffer> Exception", e);
        }
    }

    /**
     * writeSectionToFile.
     *
     * @param rafIn
     *            rafIn
     * @param rafOut
     *            rafOut
     * @param sec
     *            sec
     */
    public void writeSectionToFile(RandomAccessFile rafIn, RandomAccessFile rafOut, Section sec) {
        try {
            rafOut.writeShort(sec.mMarker);
            rafOut.writeShort(sec.mLength);
            rafIn.seek(sec.mOffset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT);
            byte[] buffer = null;
            buffer = new byte[sec.mLength - 2];
            rafIn.read(buffer, 0, buffer.length);
            rafOut.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "<writeSectionToFile> IOException", e);
        }
    }

    /**
     * writeSectionToStream.
     *
     * @param is
     *            is
     * @param os
     *            os
     * @param sec
     *            sec
     */
    public void writeSectionToStream(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os,
            Section sec) {
        try {
            os.writeShort(sec.mMarker);
            os.writeShort(sec.mLength);
            is.seek(sec.mOffset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT);
            byte[] buffer = null;
            buffer = new byte[sec.mLength - 2];
            is.read(buffer, 0, buffer.length);
            os.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "<writeSectionToStream> IOException", e);
        }
    }

    /**
     * registerNamespace.
     *
     * @param nameSpace
     *            name space
     * @param prefix
     *            prefix
     */
    public void registerNamespace(String nameSpace, String prefix) {
        if (nameSpace == null || prefix == null) {
            Log.d(TAG, "<registerNamespace> params error!!");
            return;
        }
        try {
            mRegister.registerNamespace(nameSpace, prefix);
        } catch (XMPException e) {
            Log.e(TAG, "<registerNamespace> XMPException, " + nameSpace + ": " + prefix);
        }
    }

    /**
     * registerExtXmpNamespace.
     *
     * @param nameSpace
     *            name space
     * @param prefix
     *            prefix
     */
    public void registerExtXmpNamespace(String nameSpace, String prefix) {
        if (nameSpace == null || prefix == null) {
            Log.d(TAG, "<registerExtXmpNamespace> params error!!");
            return;
        }
        try {
            mExtXmpRegister.registerNamespace(nameSpace, prefix);
        } catch (XMPException e) {
            Log.e(TAG, "<registerExtXmpNamespace> XMPException, " + nameSpace + ": " + prefix);
        }
    }

    /**
     * setPropertyString.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @param value
     *            value
     */
    public void setPropertyString(XMPMeta meta, String nameSpace, String propName, String value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyString> meta is null, return!!!");
            return;
        }
        try {
            meta.setProperty(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyString> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyString> NullPointerException!!!");
        }
    }

    /**
     * setPropertyBase64.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            name space
     * @param propName
     *            property name
     * @param value
     *            property value
     */
    public void setPropertyBase64(XMPMeta meta, String nameSpace, String propName, byte[] value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyString> meta is null, return!!!");
            return;
        }
        try {
            meta.setPropertyBase64(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyBase64> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyBase64> NullPointerException!!!");
        }
    }

    /**
     * setPropertyInteger.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @param value
     *            value
     */
    public void setPropertyInteger(XMPMeta meta, String nameSpace, String propName, int value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyInteger> meta is null, return!!!");
            return;
        }
        try {
            meta.setPropertyInteger(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyInteger> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyInteger> NullPointerException!!!");
        }
    }

    /**
     * Set property with double value.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            namespace to write
     * @param propName
     *            property name
     * @param value
     *            double value to write
     */
    public void setPropertyDouble(XMPMeta meta, String nameSpace, String propName, double value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyDouble> meta is null, return!!!");
            return;
        }
        try {
            meta.setPropertyDouble(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyDouble> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyDouble> NullPointerException!!!");
        }
    }

    /**
     * setPropertyBoolean.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @param value
     *            value
     */
    public void setPropertyBoolean(XMPMeta meta, String nameSpace, String propName, boolean value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyBoolean> meta is null, return!!!");
            return;
        }
        try {
            meta.setPropertyBoolean(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyBoolean> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyBoolean> NullPointerException!!!");
        }
    }

    /**
     * getPropertyBoolean.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @return result
     */
    public boolean getPropertyBoolean(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyBoolean> meta is null, return false!!!");
            return false;
        }
        try {
            return meta.getPropertyBoolean(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyBoolean> XMPException, " + nameSpace + ": " + propName);
            return false;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyBoolean> NullPointerException!!!");
            return false;
        }
    }

    /**
     * getPropertyDouble.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @return result
     */
    public double getPropertyDouble(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyInteger> meta is null, return -1!!!");
            return -1.0;
        }
        try {
            return meta.getPropertyDouble(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyDouble> XMPException, " + nameSpace + ": " + propName);
            return -1.0;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyDouble> NullPointerException!!!");
            return -1.0;
        }
    }

    /**
     * getPropertyString.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @return result
     */
    public String getPropertyString(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyString> meta is null, return -1!!!");
            return "";
        }
        try {
            return meta.getPropertyString(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyString> XMPException, " + nameSpace + ": " + propName);
            return "";
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyString> NullPointerException!!!");
            return "";
        }
    }

    /**
     * getPropertyInteger.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @return result
     */
    public int getPropertyInteger(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyInteger> meta is null, return -1!!!");
            return -1;
        }
        try {
            return meta.getPropertyInteger(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyInteger> XMPException, " + nameSpace + ": " + propName);
            return -1;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyInteger> NullPointerException!!!");
            return -1;
        }
    }

    /**
     * Get property base64.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            name space
     * @param propName
     *            property name
     * @return byte buffer
     */
    public byte[] getPropertyBase64(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyBase64> meta is null, return -1!!!");
            return null;
        }
        try {
            return meta.getPropertyBase64(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyBase64> XMPException, " + nameSpace + ": " + propName);
            return null;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyBase64> NullPointerException!!!");
            return null;
        }
    }

    /**
     * serialize.
     *
     * @param meta
     *            meta
     * @return result
     */
    public byte[] serialize(XMPMeta meta) {
        try {
            return XMPMetaFactory.serializeToBuffer(meta, new SerializeOptions()
                    .setUseCompactFormat(true).setOmitPacketWrapper(true));
        } catch (XMPException e) {
            Log.e(TAG, "<serialize> XMPException", e);
        }
        Log.d(TAG, "<serialize> return null!!!");
        return null;
    }

    /**
     * writeBufferToFile.
     *
     * @param desFile
     *            desFile
     * @param buffer
     *            buffer
     * @return result
     */
    public static boolean writeBufferToFile(String desFile, byte[] buffer) {
        if (buffer == null) {
            Log.d(TAG, "<writeBufferToFile> buffer is null");
            return false;
        }
        File out = new File(desFile);
        if (out.exists()) {
            out.delete();
        }
        FileOutputStream fops = null;
        try {
            if (!(out.createNewFile())) {
                Log.d(TAG, "<writeBufferToFile> createNewFile error");
                return false;
            }
            fops = new FileOutputStream(out);
            fops.write(buffer);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeBufferToFile> IOException", e);
            return false;
        } finally {
            try {
                if (fops != null) {
                    fops.close();
                    fops = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<writeBufferToFile> close, IOException", e);
            }
        }
    }

    /**
     * readFileToBuffer.
     *
     * @param filePath
     *            filePath
     * @return result
     */
    public static byte[] readFileToBuffer(String filePath) {
        File inFile = new File(filePath);
        if (!inFile.exists()) {
            Log.d(TAG, "<readFileToBuffer> " + filePath + " not exists!!!");
            return null;
        }

        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(inFile, "r");
            int len = (int) inFile.length();
            byte[] buffer = new byte[len];
            rafIn.read(buffer);
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "<readFileToBuffer> Exception ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<readFileToBuffer> close IOException ", e);
            }
        }
    }

    /**
     * writeStringToFile.
     *
     * @param desFile
     *            desFile
     * @param value
     *            value
     */
    public static void writeStringToFile(String desFile, String value) {
        if (value == null) {
            Log.d(TAG, "<writeStringToFile> input string is null, return!!!");
            return;
        }
        File out = new File(desFile);
        PrintStream ps = null;
        try {
            if (out.exists()) {
                out.delete();
            }
            if (!(out.createNewFile())) {
                Log.d(TAG, "<writeStringToFile> createNewFile error");
                return;
            }
            ps = new PrintStream(out);
            ps.println(value);
            ps.flush();
        } catch (IOException e) {
            Log.e(TAG, "<writeStringToFile> Exception ", e);
        } finally {
            out = null;
            if (ps != null) {
                ps.close();
                ps = null;
            }
        }
    }

    /**
     * parseAppInfo.
     *
     * @param filePath
     *            filePath
     * @return result
     */
    public ArrayList<Section> parseAppInfo(String filePath) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(filePath, "r");
            int value = raf.readUnsignedShort();
            if (value != SOI) {
                Log.d(TAG, "<parseAppInfo> error, find no SOI");
                return new ArrayList<Section>();
            }
            int marker = -1;
            long offset = -1;
            int length = -1;
            ArrayList<Section> sections = new ArrayList<Section>();

            while ((value = raf.readUnsignedShort()) != -1 && value != SOS) {
                marker = value;
                offset = raf.getFilePointer() - 2;
                length = raf.readUnsignedShort();
                sections.add(new Section(marker, offset, length, false, false, false, false, false,
                        false, false, false));
                raf.skipBytes(length - 2);
            }
            // write exif/isXmp flag
            for (int i = 0; i < sections.size(); i++) {
                checkIfXmpOrExifOrJps(raf, sections.get(i));
                Log.d(TAG, "<parseAppInfo> " + getSectionTag(sections.get(i)));
            }
            return sections;
        } catch (IOException e) {
            Log.e(TAG, "<parseAppInfo> IOException, path " + filePath, e);
            return null;
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                    raf = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<parseAppInfo> IOException, path " + filePath, e);
            }
        }
    }

    /**
     * parseAppInfoFromStream.
     *
     * @param is
     *            input stream
     * @return result
     */
    public ArrayList<Section> parseAppInfoFromStream(ByteArrayInputStreamExt is) {
        if (is == null) {
            Log.d(TAG, "<parseAppInfoFromStream> input stream is null!!!");
            return new ArrayList<Section>();
        }
        try {
            // reset position at the file start
            is.seek(0);
            int value = is.readUnsignedShort();
            if (value != SOI) {
                Log.d(TAG, "<parseAppInfoFromStream> error, find no SOI");
                return new ArrayList<Section>();
            }
            Log.d(TAG, "<parseAppInfoFromStream> parse begin!!!");
            int marker = -1;
            long offset = -1;
            int length = -1;
            ArrayList<Section> sections = new ArrayList<Section>();

            while ((value = is.readUnsignedShort()) != -1 && value != SOS) {
                marker = value;
                offset = is.getFilePointer() - 2;
                length = is.readUnsignedShort();
                sections.add(new Section(marker, offset, length, false, false, false, false, false,
                        false, false, false));
                is.skip(length - 2);
            }

            // write exif/isXmp flag
            for (int i = 0; i < sections.size(); i++) {
                checkIfXmpOrExifOrJpsInStream(is, sections.get(i));
                Log.d(TAG, "<parseAppInfoFromStream> " + getSectionTag(sections.get(i)));
            }
            // reset position at the file start
            is.seek(0);
            Log.d(TAG, "<parseAppInfoFromStream> parse end!!!");
            return sections;
        } catch (IOException e) {
            Log.e(TAG, "<parseAppInfoFromStream> IOException ", e);
            return new ArrayList<Section>();
        }
    }

    /**
     * return marker: write xmp after this marker.
     *
     * @param sections
     *            section array
     * @return result
     */
    public int findProperLocationForXmp(ArrayList<Section> sections) {
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            if (sec.mMarker == APP1) {
                if (sec.mIsExif) {
                    return WRITE_XMP_AFTER_FIRST_APP1;
                } else {
                    return WRITE_XMP_BEFORE_FIRST_APP1;
                }
            }
        }
        // means no app1, write after SOI
        return WRITE_XMP_AFTER_SOI;
    }

    /**
     * printXMPMeta.
     *
     * @param meta
     *            XMPMeta
     * @param title
     *            title
     */
    public static void printXMPMeta(XMPMeta meta, String title) {
        String name = meta.getObjectName();
        if (name != null && name.length() > 0) {
            Log.d(TAG, title + " (Name: '" + name + "'):");
        } else {
            Log.d(TAG, title + ":");
        }
        Log.d(TAG, meta.dumpObject());
    }

    /**
     * setStructField.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param structName
     *            structName
     * @param fieldNS
     *            fieldNS
     * @param fieldName
     *            fieldName
     * @param fieldValue
     *            fieldValue
     */
    public void setStructField(XMPMeta meta, String nameSpace, String structName, String fieldNS,
            String fieldName, String fieldValue) {
        if (meta == null) {
            Log.d(TAG, "<setStructField> meta is null, return!!!");
            return;
        }
        try {
            meta.setStructField(nameSpace, structName, fieldNS, fieldName, fieldValue);
        } catch (XMPException e) {
            Log.e(TAG, "<setStructField> XMPException, " + structName + ": " + fieldName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setStructField> NullPointerException!!!");
        }
    }

    /**
     * getStructFieldInt.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param structName
     *            structName
     * @param fieldNS
     *            fieldNS
     * @param fieldName
     *            fieldName
     * @return result
     */
    public int getStructFieldInt(XMPMeta meta, String nameSpace, String structName, String fieldNS,
            String fieldName) {
        if (meta == null) {
            Log.d(TAG, "<getStructFieldInt> meta is null, return -1");
            return -1;
        }
        try {
            XMPProperty property = meta.getStructField(nameSpace, structName, fieldNS, fieldName);
            if (property == null) {
                Log.d(TAG, "<getStructFieldInt> property is null, return -1");
                return -1;
            }
            return Integer.valueOf((String) property.getValue());
        } catch (XMPException e) {
            Log.e(TAG, "<getStructFieldInt> XMPException, " + structName + ": " + fieldName);
            return -1;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getStructFieldInt> NullPointerException!!!");
            return -1;
        }
    }

    /**
     * array index: start from 1, not 0.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param arrayName
     *            arrayName
     * @param index
     *            index
     * @param itemValue
     *            itemValue
     */
    public void setArrayItem(XMPMeta meta, String nameSpace, String arrayName, int index,
            String itemValue) {
        if (meta == null) {
            Log.d(TAG, "<setArrayItem> meta is null, return!!!");
            return;
        }
        try {
            meta.setArrayItem(nameSpace, arrayName, index, itemValue);
        } catch (XMPException e) {
            Log.e(TAG, "<setArrayItem> XMPException, " + nameSpace + ": " + arrayName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setArrayItem> NullPointerException!!!");
        }
    }

    /**
     * appendArrayItem.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param arrayName
     *            arrayName
     * @param arrayOption
     *            arrayOption
     * @param itemValue
     *            itemValue
     * @param itmeOption
     *            itmeOption
     */
    public void appendArrayItem(XMPMeta meta, String nameSpace, String arrayName,
            PropertyOptions arrayOption, String itemValue, PropertyOptions itmeOption) {
        if (meta == null) {
            Log.d(TAG, "<appendArrayItem> meta is null, return!!!");
            return;
        }
        try {
            meta.appendArrayItem(nameSpace, arrayName, arrayOption, itemValue, itmeOption);
        } catch (XMPException e) {
            Log.e(TAG, "<appendArrayItem> XMPException, " + nameSpace + ": " + arrayName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<appendArrayItem> NullPointerException!!!");
        }
    }

    /**
     * appendArrayItem.
     *
     * @param meta
     *            meta
     * @param nameSpace
     *            nameSpace
     * @param arrayName
     *            arrayName
     * @param itemValue
     *            itemValue
     */
    public void appendArrayItem(XMPMeta meta, String nameSpace, String arrayName,
            String itemValue) {
        if (meta == null) {
            Log.d(TAG, "<appendArrayItem> meta is null, return!!!");
            return;
        }
        try {
            meta.appendArrayItem(nameSpace, arrayName, itemValue);
        } catch (XMPException e) {
            Log.e(TAG, "<appendArrayItem> XMPException, " + nameSpace + ": " + arrayName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<appendArrayItem> NullPointerException!!!");
        }
    }

    /**
     * checkIfXmpOrExifOrJps.
     *
     * @param raf
     *            raf
     * @param section
     *            section
     */
    private void checkIfXmpOrExifOrJps(RandomAccessFile raf, Section section) {
        if (section == null) {
            Log.d(TAG, "<checkIfXmpOrExifOrJps> section is null!!!");
            return;
        }
        byte[] buffer = null;
        String str = null;
        try {
            if (section.mMarker == APP15) {
                raf.seek(section.mOffset + 2 + 2 + APP15_LENGTHTAG_BYTE_COUNT);
                buffer = new byte[TYPE_BUFFER_COUNT];
                raf.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (TYPE_JPS_DATA.equals(str)) {
                    section.mIsJpsData = true;
                    return;
                }
                if (TYPE_JPS_MASK.equals(str)) {
                    section.mIsJpsMask = true;
                    return;
                }
                if (TYPE_DEPTH_DATA.equals(str)) {
                    section.mIsDepthData = true;
                    return;
                }
                if (TYPE_XMP_DEPTH.equals(str)) {
                    section.mIsXmpDepth = true;
                    return;
                }
                if (TYPE_SEGMENT_MASK.equals(str)) {
                    section.mIsSegmentMask = true;
                    return;
                }
            } else if (section.mMarker == APP1) {
                raf.seek(section.mOffset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT);
                buffer = new byte[XMP_EXT_MAIN_HEADER1.length()];
                raf.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (XMP_EXT_MAIN_HEADER1.equals(str)) {
                    section.mIsXmpExt = true;
                    return;
                }
                str = new String(buffer, 0, XMP_HEADER_START.length());
                if (XMP_HEADER_START.equals(str)) {
                    section.mIsXmpMain = true;
                    return;
                }
                str = new String(buffer, 0, EXIF_HEADER.length());
                if (EXIF_HEADER.equals(str)) {
                    section.mIsExif = true;
                    return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJps> UnsupportedEncodingException" + e);
        } catch (IOException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJps> IOException" + e);
        }
    }

    private void checkIfXmpOrExifOrJpsInStream(ByteArrayInputStreamExt is, Section section) {
        if (is == null || section == null) {
            Log.d(TAG, "<checkIfXmpOrExifOrJpsInStream> input stream or section is null!!!");
            return;
        }
        byte[] buffer = null;
        String str = null;
        try {
            if (section.mMarker == APP15) {
                is.seek(section.mOffset + 2 + 2 + APP15_LENGTHTAG_BYTE_COUNT);
                buffer = new byte[TYPE_BUFFER_COUNT];
                is.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (TYPE_JPS_DATA.equals(str)) {
                    section.mIsJpsData = true;
                    return;
                }
                if (TYPE_JPS_MASK.equals(str)) {
                    section.mIsJpsMask = true;
                    return;
                }
                if (TYPE_DEPTH_DATA.equals(str)) {
                    section.mIsDepthData = true;
                    return;
                }
                if (TYPE_XMP_DEPTH.equals(str)) {
                    section.mIsXmpDepth = true;
                    return;
                }
                if (TYPE_SEGMENT_MASK.equals(str)) {
                    section.mIsSegmentMask = true;
                    return;
                }
            } else if (section.mMarker == APP1) {
                is.seek(section.mOffset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT);
                buffer = new byte[XMP_EXT_MAIN_HEADER1.length()];
                is.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (XMP_EXT_MAIN_HEADER1.equals(str)) {
                    // ext main header is same as ext slave header
                    section.mIsXmpExt = true;
                    return;
                }
                str = new String(buffer, 0, XMP_HEADER_START.length());
                if (XMP_HEADER_START.equals(str)) {
                    section.mIsXmpMain = true;
                    return;
                }
                str = new String(buffer, 0, EXIF_HEADER.length());
                if (EXIF_HEADER.equals(str)) {
                    section.mIsExif = true;
                    return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJpsInStream> UnsupportedEncodingException" + e);
        } catch (IOException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJpsInStream> IOException" + e);
        }
    }

    private String getSectionTag(Section section) {
        String tag = "marker 0x" + Integer.toHexString(section.mMarker) + ", offset 0x"
                + Long.toHexString(section.mOffset) + ", length 0x"
                + Integer.toHexString(section.mLength);
        if (section.mIsXmpMain) {
            tag += ", XmpMain";
        } else if (section.mIsXmpExt) {
            tag += ", XmpExt";
        } else if (section.mIsExif) {
            tag += ", Exif";
        } else if (section.mIsJpsData) {
            tag += ", JpsData";
        } else if (section.mIsJpsMask) {
            tag += ", JpsMask";
        } else if (section.mIsDepthData) {
            tag += ", DepthData";
        } else if (section.mIsXmpDepth) {
            tag += ", XmpDepth";
        } else if (section.mIsSegmentMask) {
            tag += ", SegmentMask";
        }
        return tag;
    }


    private static byte[] md5(byte[] in) {
        if (in == null || in.length <= 0) {
            Log.d(TAG, "<md5> input error!!");
            return null;
        }
        byte[] out = null;
        try {
            MessageDigest digestor = MessageDigest.getInstance("MD5");
            digestor.update(in);
            out = digestor.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "<md5> NoSuchAlgorithmException ", e);
        }
        return out;
    }

    private static byte[] intToByteBuffer(int value, int byteCount) {
        int in = value;
        byte[] byteBuffer = new byte[byteCount];
        for (int i = byteCount - 1; i >= 0; i--) {
            byteBuffer[i] = (byte) (in & BYTE_MASK);
            in = in >> SHIFT_BIT_COUNT_8;
        }
        return byteBuffer;
    }
}
