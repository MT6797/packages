package com.mediatek.galleryfeature.panorama;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PanoramaMesh {
    private static final String TAG = "MtkGallery2/PanoramaMesh";
    private static final int MAP_SIZE = 8;
    private static final int FRAG_ANGLE = 4;
    private static final float DEGREE_360 = 360.f;
    private static final int DIMENSION_PER_VERTEX = 3;
    private static final int VERTEX_PER_TRIANGLE = 3;
    private static final int BYTE_PER_FLOAT = 4;

    private int mVertexCount;
    private float mRadius;
    private int mHeightAngle;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mNormalBuffer;
    private float[] mTexCoordAry = null;

    private static LinkedHashMap<Integer, PanoramaMesh> sMeshMap =
            new LinkedHashMap<Integer, PanoramaMesh>() {
        private static final long serialVersionUID = 1L;
        protected boolean removeEldestEntry(java.util.Map.Entry<Integer, PanoramaMesh> eldest) {
            return size() > MAP_SIZE;
        }
    };

    public static PanoramaMesh getInstance(int width, int height) {
        synchronized (PanoramaMesh.class) {
            PanoramaMesh mesh = null;
            int scale = width / height;
            if (sMeshMap.containsKey(scale)) {
                mesh = sMeshMap.get(scale);
                sMeshMap.remove(scale);
                sMeshMap.put(scale, mesh);
                return mesh;
            } else {
                mesh = new PanoramaMesh(width, height);
                sMeshMap.put(scale, mesh);
                return mesh;
            }
        }
    }

    private PanoramaMesh(int width, int height) {
        mRadius = PanoramaHelper.MESH_RADIUS;
        mHeightAngle = (int) (DEGREE_360 * height / width);
        mHeightAngle = (int) (((float) mHeightAngle / FRAG_ANGLE / 2.f + 1.f) * FRAG_ANGLE * 2.f);
        initMesh();
    }

    public FloatBuffer getVertexBuffer() {
        return mVertexBuffer;
    }

    public FloatBuffer getNormalBuffer() {
        return mNormalBuffer;
    }

    public FloatBuffer getTexCoordsBuffer(float scale) {
        FloatBuffer texCoordsBuffer;
        float[] texCoordAry = new float[mTexCoordAry.length];
        for (int i = 0; i < mTexCoordAry.length / 2; i += 1) {
            texCoordAry[2 * i] = mTexCoordAry[2 * i] / scale;
            texCoordAry[2 * i + 1] = 1 - mTexCoordAry[2 * i + 1];
        }
        ByteBuffer bytes = ByteBuffer.allocateDirect(texCoordAry.length * BYTE_PER_FLOAT);
        bytes.order(ByteOrder.nativeOrder());
        texCoordsBuffer = bytes.asFloatBuffer();
        texCoordsBuffer.put(texCoordAry);
        texCoordsBuffer.position(0);
        return texCoordsBuffer;
    }

    public int getVertexCount() {
        return mVertexCount;
    }

    public int getTriangleCount() {
        return mVertexCount / VERTEX_PER_TRIANGLE;
    }

    private void initMesh() {
        ArrayList<Float> alVertix = new ArrayList<Float>();

        for (int rowAngle = -mHeightAngle / 2; rowAngle <= mHeightAngle / 2;
                rowAngle += FRAG_ANGLE) {
            for (int colAngleAngle = 0; colAngleAngle < DEGREE_360; colAngleAngle += FRAG_ANGLE) {
                alVertix.add((float) (mRadius * Math.cos(Math.toRadians(colAngleAngle)))); // x
                alVertix.add((float) (mRadius * Math.sin(Math.toRadians(colAngleAngle)))); // y
                alVertix.add((float) (mRadius * Math.tan(Math.toRadians(rowAngle)))); // z
            }
        }
        mVertexCount = alVertix.size() / DIMENSION_PER_VERTEX;

        float vertices[] = new float[mVertexCount * DIMENSION_PER_VERTEX];
        for (int i = 0; i < alVertix.size(); i++) {
            vertices[i] = alVertix.get(i);
        }
        alVertix.clear();
        ArrayList<Float> alTexture = new ArrayList<Float>();

        int row = (mHeightAngle / FRAG_ANGLE) + 1;
        int col = (int) (DEGREE_360 / FRAG_ANGLE);

        float splitRow = row - 1;
        float splitCol = col;

        for (int i = 0; i < row; i++) {
            if (i != row - 1) {
                for (int j = 0; j < col; j++) {
                    int k = i * col + j;
                    alVertix.add(vertices[(k + col) * DIMENSION_PER_VERTEX]);
                    alVertix.add(vertices[(k + col) * DIMENSION_PER_VERTEX + 1]);
                    alVertix.add(vertices[(k + col) * DIMENSION_PER_VERTEX + 2]);

                    alTexture.add(j / splitCol);
                    alTexture.add((i + 1) / splitRow);

                    int tmp = k + 1;
                    if (j == col - 1) {
                        tmp = (i) * col;
                    }
                    alVertix.add(vertices[(tmp) * DIMENSION_PER_VERTEX]);
                    alVertix.add(vertices[(tmp) * DIMENSION_PER_VERTEX + 1]);
                    alVertix.add(vertices[(tmp) * DIMENSION_PER_VERTEX + 2]);

                    alTexture.add((j + 1) / splitCol);
                    alTexture.add(i / splitRow);

                    alVertix.add(vertices[k * DIMENSION_PER_VERTEX]);
                    alVertix.add(vertices[k * DIMENSION_PER_VERTEX + 1]);
                    alVertix.add(vertices[k * DIMENSION_PER_VERTEX + 2]);

                    alTexture.add(j / splitCol);
                    alTexture.add(i / splitRow);
                }
            }
            if (i != 0) {
                for (int j = 0; j < col; j++) {
                    int k = i * col + j;
                    alVertix.add(vertices[(k - col) * DIMENSION_PER_VERTEX]);
                    alVertix.add(vertices[(k - col) * DIMENSION_PER_VERTEX + 1]);
                    alVertix.add(vertices[(k - col) * DIMENSION_PER_VERTEX + 2]);
                    if (j == 0) {
                        alTexture.add(1.0f);
                    } else {
                        alTexture.add(j / splitCol);
                    }
                    alTexture.add((i - 1) / splitRow);

                    int tmp = k - 1;
                    if (j == 0) {
                        tmp = i * col + col - 1;
                    }
                    alVertix.add(vertices[(tmp) * DIMENSION_PER_VERTEX]);
                    alVertix.add(vertices[(tmp) * DIMENSION_PER_VERTEX + 1]);
                    alVertix.add(vertices[(tmp) * DIMENSION_PER_VERTEX + 2]);
                    if (j == 0) {
                        alTexture.add(1 - 1 / splitCol);
                    } else {
                        alTexture.add((j - 1) / splitCol);
                    }
                    alTexture.add(i / splitRow);

                    alVertix.add(vertices[k * DIMENSION_PER_VERTEX]);
                    alVertix.add(vertices[k * DIMENSION_PER_VERTEX + 1]);
                    alVertix.add(vertices[k * DIMENSION_PER_VERTEX + 2]);
                    if (j == 0) {
                        alTexture.add(1.0f);
                    } else {
                        alTexture.add(j / splitCol);
                    }
                    alTexture.add(i / splitRow);
                }
            }
        }

        mVertexCount = alVertix.size() / DIMENSION_PER_VERTEX;

        float[] vertexAry = new float[mVertexCount * DIMENSION_PER_VERTEX];
        float[] normalAry = new float[mVertexCount * DIMENSION_PER_VERTEX];
        for (int i = 0; i < alVertix.size(); i++) {
            vertexAry[i] = alVertix.get(i);
            normalAry[i] = -vertexAry[i];
        }

        mTexCoordAry = new float[mVertexCount * 2];
        for (int i = 0; i < alTexture.size(); i++) {
            mTexCoordAry[i] = alTexture.get(i);
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(vertexAry.length * BYTE_PER_FLOAT);
        bytes.order(ByteOrder.nativeOrder());
        mVertexBuffer = bytes.asFloatBuffer();
        mVertexBuffer.put(vertexAry);
        mVertexBuffer.position(0);

        ByteBuffer bytes2 = ByteBuffer.allocateDirect(normalAry.length * BYTE_PER_FLOAT);
        bytes2.order(ByteOrder.nativeOrder());
        mNormalBuffer = bytes2.asFloatBuffer();
        mNormalBuffer.put(normalAry);
        mNormalBuffer.position(0);
    }
}