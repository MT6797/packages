package com.mediatek.xmp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JsonParser.
 *
 */
public class JsonParser {
    private static final String TAG = "mtkGallery2/JsonParser";
    private JSONObject mJsonObject;
    private static final int INVALID_VALUE_INT = -1;
    private static final double INVALID_VALUE_DOUBLE = -1.0;

    /**
     * constructor,create JSON object.
     *
     * @param jsonString
     *            jsonString
     */
    public JsonParser(String jsonString) {
        try {
            mJsonObject = new JSONObject(jsonString);
        } catch (JSONException exception) {
            Log.e(TAG, "<JsonParser> exception", exception);
        }
    }

    /**
     * constructor,create JSON object.
     *
     * @param jsonBuffer
     *            jsonBuffer
     */
    public JsonParser(byte[] jsonBuffer) {
        try {
            mJsonObject = new JSONObject(new String(jsonBuffer));
        } catch (JSONException exception) {
            Log.e(TAG, "<JsonParser> exception", exception);
        }
    }

    /**
     * get INT value from object.
     *
     * @param objectName
     *            object name
     * @param subObjectName
     *            sub object name
     * @param propertyName
     *            property name
     * @return value data
     */
    public int getValueIntFromObject(String objectName, String subObjectName,
            String propertyName) {
        int value = INVALID_VALUE_INT;
        if (mJsonObject == null || propertyName == null) {
            Log.d(TAG, "<getValueIntFromObject> error!!");
            return INVALID_VALUE_INT;
        }
        try {
            if (objectName == null) {
                return mJsonObject.getInt(propertyName);
            }
            JSONObject object = mJsonObject.getJSONObject(objectName);
            object = subObjectName == null ? object : object.getJSONObject(subObjectName);
            if (object != null) {
                value = object.getInt(propertyName);
            }
            return value;
        } catch (JSONException exception) {
            Log.e(TAG, "<getValueIntFromObject> exception", exception);
            return INVALID_VALUE_INT;
        }
    }

    /**
     * get double value from object.
     *
     * @param objectName
     *            object name
     * @param subObjectName
     *            sub object name
     * @param propertyName
     *            property name
     * @return value data
     */
    public double getValueDoubleFromObject(String objectName, String subObjectName,
            String propertyName) {
        double value = INVALID_VALUE_DOUBLE;
        if (mJsonObject == null || objectName == null || propertyName == null) {
            Log.d(TAG, "<getValueDoubleFromObject> error!!");
            return INVALID_VALUE_DOUBLE;
        }
        try {
            JSONObject object = mJsonObject.getJSONObject(objectName);
            object = subObjectName == null ? object : object.getJSONObject(subObjectName);
            if (object != null) {
                value = object.getDouble(propertyName);
            }
            return value;
        } catch (JSONException exception) {
            Log.e(TAG, "<getValueDoubleFromObject> exception", exception);
            return INVALID_VALUE_DOUBLE;
        }
    }

    /**
     * get String value from object.
     *
     * @param objectName
     *            object name
     * @param subObjectName
     *            sub object name
     * @param propertyName
     *            property name
     * @return value data
     */
    public String getValueStringFromObject(String objectName, String subObjectName,
            String propertyName) {
        String value = "";
        if (mJsonObject == null || objectName == null || propertyName == null) {
            Log.d(TAG, "<getValueStringFromObject> error!!");
            return "";
        }
        try {
            JSONObject object = mJsonObject.getJSONObject(objectName);
            object = subObjectName == null ? object : object.getJSONObject(subObjectName);
            if (object != null) {
                value = object.getString(propertyName);
            }
            return value;
        } catch (JSONException exception) {
            Log.e(TAG, "<getValueStringFromObject> exception", exception);
            return "";
        }
    }

    /**
     * get 2D array from object.
     *
     * @param objectName
     *            object name
     * @param arrayName
     *            array name
     * @return 2D array
     */
    public int[][] getInt2DArrayFromObject(String objectName, String arrayName) {
        int[][] array = null;
        if (mJsonObject == null || arrayName == null) {
            Log.d(TAG, "<getInt2DArrayFromObject> error!!");
            return null;
        }
        try {
            JSONObject object = objectName == null ? mJsonObject : mJsonObject
                    .getJSONObject(objectName);
            JSONArray jsonArray = object.getJSONArray(arrayName);
            if (jsonArray != null) {
                int len = jsonArray.length();
                JSONArray jsonSubArray = jsonArray.getJSONArray(0);
                int subArrayLen = jsonSubArray.length();
                array = new int[len][subArrayLen];
                for (int i = 0; i < len; i++) {
                    jsonSubArray = jsonArray.getJSONArray(i);
                    if (jsonSubArray != null) {
                        for (int j = 0; j < subArrayLen; j++) {
                            array[i][j] = jsonSubArray.getInt(j);
                        }
                    }
                }
            }
            return array;
        } catch (JSONException exception) {
            Log.e(TAG, "<getInt2DArrayFromObject> exception", exception);
            return null;
        }
    }

    /**
     * get array from object.
     *
     * @param objectName
     *            object name
     * @param arrayName
     *            array name
     * @return array
     */
    public int[] getIntArrayFromObject(String objectName, String arrayName) {
        int[] array = null;
        if (mJsonObject == null || arrayName == null) {
            Log.d(TAG, "<getIntArrayFromObject> error!!");
            return null;
        }
        try {
            JSONObject object = objectName == null ? mJsonObject : mJsonObject
                    .getJSONObject(objectName);
            if (object != null) {
                JSONArray jsonArray = object.getJSONArray(arrayName);
                if (jsonArray != null) {
                    int len = jsonArray.length();
                    array = new int[len];
                    for (int i = 0; i < len; i++) {
                        array[i] = jsonArray.getInt(i);
                    }
                }
            }
            return array;
        } catch (JSONException exception) {
            Log.e(TAG, "<getIntArrayFromObject> exception", exception);
            return null;
        }
    }

    /**
     * get property value form array.
     *
     * @param arrayName
     *            array name
     * @param index
     *            index
     * @param propertyName
     *            property name
     * @return property value
     */
    public int getObjectPropertyValueFromArray(String arrayName, int index, String propertyName) {
        if (mJsonObject == null || arrayName == null || propertyName == null) {
            Log.d(TAG, "<getObjectPropertyValueFromArray> error!!");
            return -1;
        }
        try {
            JSONArray jsonArray = mJsonObject.getJSONArray(arrayName);
            if (jsonArray != null) {
                int len = jsonArray.length();
                if (index < 0 || index > len) {
                    Log.d(TAG, "<getObjectPropertyValueFromArray> index error: " + index);
                    return -1;
                }
                JSONObject object = jsonArray.getJSONObject(index);
                if (object != null) {
                    return object.getInt(propertyName);
                }
            }
            return -1;
        } catch (JSONException exception) {
            Log.e(TAG, "<getObjectPropertyValueFromArray> exception", exception);
            return -1;
        }
    }

    /**
     * get array length.
     *
     * @param arrayName
     *            array name
     * @return array length
     */
    public int getArrayLength(String arrayName) {
        if (mJsonObject == null || arrayName == null) {
            Log.d(TAG, "<getArrayLength> error!!");
            return -1;
        }
        try {
            JSONArray jsonArray = mJsonObject.getJSONArray(arrayName);
            if (jsonArray != null) {
                return jsonArray.length();
            }
            return -1;
        } catch (JSONException exception) {
            Log.e(TAG, "<getArrayLength> exception", exception);
            return -1;
        }
    }
}
