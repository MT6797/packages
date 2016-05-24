/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
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
package com.mediatek.contacts;

import android.app.Application;
import android.content.Context;

import com.mediatek.common.MPlugin;

import com.mediatek.contacts.ext.*;

import java.util.HashMap;

public final class ExtensionManager {
    //private static final String TAG = ExtensionManager.class.getSimpleName();
    private static ExtensionManager sInstance = null;
    private static Context sContext = null;

    /**
     * Map the plugin instance by ExtensionManager
     */
    private static HashMap<Class, Object> sPluginMap = new HashMap<Class, Object>();

    /**
     * Map the default plugin implements object
     */
    private static HashMap<Class, Object> sDefaulthPluginMap = new HashMap<Class, Object>();
    static {
        sDefaulthPluginMap.put(ICtExtension.class, new DefaultCtExtension());
        sDefaulthPluginMap.put(IOp01Extension.class, new DefaultOp01Extension());
        sDefaulthPluginMap.put(IRcsExtension.class, new DefaultRcsExtension());
       // sDefaulthPluginMap.put(IIccCardExtension.class, new DefaultIccCardExtension());
       // sDefaulthPluginMap.put(IImportExportExtension.class, new DefaultImportExportExtension());
        sDefaulthPluginMap.put(IViewCustomExtension.class, new DefaultViewCustomExtension());
        sDefaulthPluginMap.put(IAasExtension.class, new DefaultAasExtension());
        sDefaulthPluginMap.put(ISneExtension.class, new DefaultSneExtension());
        sDefaulthPluginMap.put(IRcsRichUiExtension.class, new DefaultRcsRichUiExtension());
        sDefaulthPluginMap.put(IContactsCommonPresenceExtension.class,
            new DefaultContactsCommonPresenceExtension());
    }

    private ExtensionManager() {
        //do-nothing
    }

    public static ExtensionManager getInstance() {
        if (sInstance == null) {
            createInstanceSynchronized();
        }
        return sInstance;
    }

    private static synchronized void createInstanceSynchronized() {
        if (sInstance == null) {
            sInstance = new ExtensionManager();
        }
    }

    /**
     *
     * @param iclass interface class
     * @return plugin object created
     */
    private static <I> Object getExtension(Class<I> iclass) {
        if (sPluginMap.get(iclass) != null) {
            return sPluginMap.get(iclass);
        }
        synchronized (iclass) {
            if (sPluginMap.get(iclass) != null) {
                return sPluginMap.get(iclass);
            }
            //check if the object create is instance of type I.
            I instance = (I) createPluginObject(iclass, sDefaulthPluginMap.get(iclass));
            sPluginMap.put(iclass, instance);
        }
        return sPluginMap.get(iclass);
    }

    private static <T> T createPluginObject(Class cls, Object defaultObject) {
        T pluginObject = (T) MPlugin.createInstance(cls.getName(), sContext);
        return pluginObject != null ? pluginObject : (T) defaultObject;
    }

    public static void registerApplicationContext(Application application) {
        sContext = application.getApplicationContext();
    }

    /**
     * used for test case.
     * reset all plugins,only use default plugin
     */
    public static void resetPlugins() {
        for (Class key : sDefaulthPluginMap.keySet()) {
            sPluginMap.put(key, sDefaulthPluginMap.get(key));
        }
    }

   //-------------------below are getXxxExtension() methonds-----------------//

    public static ICtExtension getCtExtension() {
        ICtExtension extension = (ICtExtension) getExtension(ICtExtension.class);
        return extension;
    }

    public static IOp01Extension getOp01Extension() {
        IOp01Extension extension = (IOp01Extension) getExtension(IOp01Extension.class);
        return extension;
    }


    public static IRcsExtension getRcsExtension() {
        IRcsExtension extension = (IRcsExtension) getExtension(IRcsExtension.class);
        return extension;
    }

    public static IContactsCommonPresenceExtension getContactsCommonPresenceExtension() {
        IContactsCommonPresenceExtension extension =
            (IContactsCommonPresenceExtension) getExtension(IContactsCommonPresenceExtension.class);
        return extension;
    }

    /*public static IIccCardExtension getIccCardExtension() {
        IIccCardExtension extension = (IIccCardExtension) getExtension(IIccCardExtension.class);
        return extension;
    }*/

    /*public static IImportExportExtension getImportExportExtension() {
        IImportExportExtension extension =
        (IImportExportExtension) getExtension(IImportExportExtension.class);
        return extension;
    }*/

    public static IViewCustomExtension getViewCustomExtension() {
        IViewCustomExtension extension =
                (IViewCustomExtension) getExtension(IViewCustomExtension.class);
        return extension;
    }

    public static IAasExtension getAasExtension() {
        IAasExtension extension = (IAasExtension) getExtension(IAasExtension.class);
        return extension;
    }

    public static ISneExtension getSneExtension() {
        ISneExtension extension = (ISneExtension) getExtension(ISneExtension.class);
        return extension;
    }

    /**
     * Get OP01 RCS RichUi plugin.
     * @return IRcsRichUiExtension.
     */
    public static IRcsRichUiExtension getRcsRichUiExtension() {
        IRcsRichUiExtension extension = (IRcsRichUiExtension) getExtension(
                IRcsRichUiExtension.class);
        return extension;
    }

}
