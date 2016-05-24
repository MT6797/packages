package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.preference.PreferenceGroup;
import android.util.Log;

public class DefaultMdmPermControlExt extends ContextWrapper implements IMdmPermissionControlExt {
    private static final String TAG = "DefaultMdmPermControlExt";
    public DefaultMdmPermControlExt(Context context) {
        super(context);
    }

    public void addMdmPermCtrlPrf(PreferenceGroup prefGroup) {
        Log.d(TAG,"will not add mdm permission control");
    }
}
