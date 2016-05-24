package com.mediatek.gallery3d.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;

/** Check and request permission in kinds of cases,
 * now used in GalleryActivity and FiltershowActivity.
 */
public class PermissionHelper {
    private static final String TAG = "MtkGallery2/PermissionHelper";

    /** Check WRITE_EXTERNAL_STORAGE/READ_SMS permissions for gallery activity.
     * If all permissions are granted, return true.
     * If one of them is denied, request permissions and return false.
     * @param activity GalleryActivity
     * @return If all permissions are granted, return true.
     *         If one of them is denied, request permissions and return false.
     */
    public static boolean checkAndRequestForGallery(Activity activity) {
        // get permissions needed in current scenario
        ArrayList<String> permissionsNeeded = new ArrayList<String>();
        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Uri uri = activity.getIntent().getData();
        if (uri != null && uri.toString().startsWith("content://mms")) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        // check status of permissions, get which permissions need to request
        ArrayList<String> permissionsNeedRequest = new ArrayList<String>();
        for (String permission : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            permissionsNeedRequest.add(permission);
        }
        // request permissions
        if (permissionsNeedRequest.size() == 0) {
            MtkLog.i(TAG, "<checkAndRequestForGallery> all permissions are granted");
            return true;
        } else {
            MtkLog.i(TAG, "<checkAndRequestForGallery> not all permissions are granted, reuqest");
            String[] permissions = new String[permissionsNeedRequest.size()];
            permissions = permissionsNeedRequest.toArray(permissions);
            ActivityCompat.requestPermissions(activity, permissions, 0);
            return false;
        }
    }

    /** Check ACCESS_FINE_LOCATION permission for location cluster.
     * @param activity GalleryActivity
     * @return If permission is granted, return true, or else request permission and return false.
     */
    public static boolean checkAndRequestForLocationCluster(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            MtkLog.i(TAG, "<checkAndRequestForLocationCluster> permission not granted, reuqest");
            ActivityCompat.requestPermissions(activity,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 0);
            return false;
        }
        MtkLog.i(TAG, "<checkAndRequestForLocationCluster> all permissions are granted");
        return true;
    }

    /** Check WRITE_EXTERNAL_STORAGE/READ_SMS permissions for movie activity.
     * If all permissions are granted, return true.
     * If one of them is denied, request permissions and return false.
     * @param activity MovieActivity
     * @return If all permissions are granted, return true.
     *         If one of them is denied, request permissions and return false.
     */
    public static boolean checkAndRequestForVP(Activity activity) {
        // get permissions needed in current scenario
        ArrayList<String> permissionsNeeded = new ArrayList<String>();
        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Uri uri = activity.getIntent().getData();
        if (uri != null && uri.toString().startsWith("content://mms")) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        // check status of permissions, get which permissions need to request
        ArrayList<String> permissionsNeedRequest = new ArrayList<String>();
        for (String permission : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            permissionsNeedRequest.add(permission);
        }
        // request permissions
        if (permissionsNeedRequest.size() == 0) {
            MtkLog.i(TAG, "<checkAndRequestForVP> all permissions are granted");
            return true;
        } else {
            MtkLog.i(TAG, "<checkAndRequestForVP> not all permissions are granted, reuqest");
            String[] permissions = new String[permissionsNeedRequest.size()];
            permissions = permissionsNeedRequest.toArray(permissions);
            ActivityCompat.requestPermissions(activity, permissions, 0);
            return false;
        }
    }

    /**Check WRITE_EXTERNAL_STORAGE permission for filter show.
     * @param activity WidgetConfigure or WidgetClickHandler
     * @return If permission is granted, return true, or else request permission and return false.
     */
    public static boolean checkAndRequestForWidget(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            MtkLog.i(TAG, "<checkAndRequestForWidget> permission not granted, reuqest");
            ActivityCompat.requestPermissions(activity,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
            return false;
        }
        MtkLog.i(TAG, "<checkAndRequestForWidget> all permissions are granted");
        return true;
    }

    /**Check WRITE_EXTERNAL_STORAGE permission for filter show.
     * @param activity FilterShowActivity
     * @return If permission is granted, return true, or else finish activity and return false.
     */
    public static boolean checkForFilterShow(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            MtkLog.i(TAG, "<checkForFilterShow> permission not granted, finish");
            PermissionHelper.showDeniedPrompt(activity);
            activity.finish();
            return false;
        }
        MtkLog.i(TAG, "<checkForFilterShow> all permissions are granted");
        return true;
    }

    /** Check if all permissions in String[] are granted.
     * @param permissions A group of permissions
     * @param grantResults The granted status of permissions
     * @return If all permissions are granted, return true, or else return false.
     */
    public static boolean isAllPermissionsGranted(String[] permissions, int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if WRITE_EXTERNAL_STORAGE is granted.
     * @param context Current application environment
     * @return WRITE_EXTERNAL_STORAGE is granted or not.
     */
    public static boolean checkStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if ACCESS_FINE_LOCATION is granted.
     * @param context Current application environment
     * @return ACCESS_FINE_LOCATION is granted or not.
     */
    public static boolean checkLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Show toast after permission is denied.
     * @param context Current application environment
     */
    public static void showDeniedPrompt(Context context) {
        Toast.makeText(context, com.mediatek.internal.R.string.denied_required_permission,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Show toast if permission is denied and "never ask again".
     * @param activity Current activity
     * @param permission The permission your app want to request
     * @return Whether toast has been shown.
     */
    public static boolean showDeniedPromptIfNeeded(Activity activity, String permission) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            Toast.makeText(activity.getApplicationContext(),
                    com.mediatek.internal.R.string.denied_required_permission, Toast.LENGTH_SHORT)
                    .show();
            return true;
        }
        return false;
    }
}