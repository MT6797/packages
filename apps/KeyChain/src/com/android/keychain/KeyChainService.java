/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.keychain;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyStore;
import android.util.Log;
import com.android.internal.util.ParcelableString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.android.org.conscrypt.TrustedCertificateStore;

public class KeyChainService extends IntentService {

    private static final String TAG = "KeyChain";

    private static final String DATABASE_NAME = "grants.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_GRANTS = "grants";
    private static final String GRANTS_ALIAS = "alias";
    private static final String GRANTS_GRANTEE_UID = "uid";

    /** created in onCreate(), closed in onDestroy() */
    public DatabaseHelper mDatabaseHelper;

    private static final String SELECTION_COUNT_OF_MATCHING_GRANTS =
            "SELECT COUNT(*) FROM " + TABLE_GRANTS
                    + " WHERE " + GRANTS_GRANTEE_UID + "=? AND " + GRANTS_ALIAS + "=?";

    private static final String SELECT_GRANTS_BY_UID_AND_ALIAS =
            GRANTS_GRANTEE_UID + "=? AND " + GRANTS_ALIAS + "=?";

    private static final String SELECTION_GRANTS_BY_UID = GRANTS_GRANTEE_UID + "=?";

    public KeyChainService() {
        super(KeyChainService.class.getSimpleName());
    }

    @Override public void onCreate() {
        super.onCreate();
        mDatabaseHelper = new DatabaseHelper(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabaseHelper.close();
        mDatabaseHelper = null;
    }

    private final IKeyChainService.Stub mIKeyChainService = new IKeyChainService.Stub() {
        private final KeyStore mKeyStore = KeyStore.getInstance();
        private final TrustedCertificateStore mTrustedCertificateStore
                = new TrustedCertificateStore();

        @Override
        public String requestPrivateKey(String alias) {
            checkArgs(alias);

            final String keystoreAlias = Credentials.USER_PRIVATE_KEY + alias;
            final int uid = Binder.getCallingUid();
            if (!mKeyStore.grant(keystoreAlias, uid)) {
                return null;
            }
            final int userHandle = UserHandle.getUserId(uid);
            final int systemUidForUser = UserHandle.getUid(userHandle, Process.SYSTEM_UID);

            final StringBuilder sb = new StringBuilder();
            sb.append(systemUidForUser);
            sb.append('_');
            sb.append(keystoreAlias);

            return sb.toString();
        }

        @Override public byte[] getCertificate(String alias) {
            checkArgs(alias);
            return mKeyStore.get(Credentials.USER_CERTIFICATE + alias);
        }

        private void checkArgs(String alias) {
            if (alias == null) {
                throw new NullPointerException("alias == null");
            }
            if (!mKeyStore.isUnlocked()) {
                throw new IllegalStateException("keystore is "
                        + mKeyStore.state().toString());
            }

            final int callingUid = getCallingUid();
            if (!hasGrantInternal(mDatabaseHelper.getReadableDatabase(), callingUid, alias)) {
                throw new IllegalStateException("uid " + callingUid
                        + " doesn't have permission to access the requested alias");
            }
        }

        @Override public void installCaCertificate(byte[] caCertificate) {
            checkCertInstallerOrSystemCaller();
            checkUserRestriction();
            try {
                synchronized (mTrustedCertificateStore) {
                    mTrustedCertificateStore.installCertificate(parseCertificate(caCertificate));
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            }
            broadcastStorageChange();
        }

        @Override public boolean installKeyPair(byte[] privateKey, byte[] userCertificate,
                String alias) {
            checkCertInstallerOrSystemCaller();
            if (!mKeyStore.importKey(Credentials.USER_PRIVATE_KEY + alias, privateKey, -1,
                    KeyStore.FLAG_ENCRYPTED)) {
                Log.e(TAG, "Failed to import private key " + alias);
                return false;
            }
            if (!mKeyStore.put(Credentials.USER_CERTIFICATE + alias, userCertificate, -1,
                    KeyStore.FLAG_ENCRYPTED)) {
                Log.e(TAG, "Failed to import user certificate " + userCertificate);
                if (!mKeyStore.delete(Credentials.USER_PRIVATE_KEY + alias)) {
                    Log.e(TAG, "Failed to delete private key after certificate importing failed");
                }
                return false;
            }
            broadcastStorageChange();
            return true;
        }

        private X509Certificate parseCertificate(byte[] bytes) throws CertificateException {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
        }

        @Override public boolean reset() {
            // only Settings should be able to reset
            checkSystemCaller();
            checkUserRestriction();
            removeAllGrants(mDatabaseHelper.getWritableDatabase());
            boolean ok = true;
            synchronized (mTrustedCertificateStore) {
                // delete user-installed CA certs
                for (String alias : mTrustedCertificateStore.aliases()) {
                    if (TrustedCertificateStore.isUser(alias)) {
                        if (!deleteCertificateEntry(alias)) {
                            ok = false;
                        }
                    }
                }
            }
            broadcastStorageChange();
            return ok;
        }

        @Override public boolean deleteCaCertificate(String alias) {
            // only Settings should be able to delete
            checkSystemCaller();
            checkUserRestriction();
            boolean ok = true;
            synchronized (mTrustedCertificateStore) {
                ok = deleteCertificateEntry(alias);
            }
            broadcastStorageChange();
            return ok;
        }

        private boolean deleteCertificateEntry(String alias) {
            try {
                mTrustedCertificateStore.deleteCertificateEntry(alias);
                return true;
            } catch (IOException e) {
                Log.w(TAG, "Problem removing CA certificate " + alias, e);
                return false;
            } catch (CertificateException e) {
                Log.w(TAG, "Problem removing CA certificate " + alias, e);
                return false;
            }
        }

        private void checkCertInstallerOrSystemCaller() {
            String actual = checkCaller("com.android.certinstaller");
            if (actual == null) {
                return;
            }
            checkSystemCaller();
        }
        private void checkSystemCaller() {
            String actual = checkCaller("android.uid.system:1000");
            if (actual != null) {
                throw new IllegalStateException(actual);
            }
        }
        private void checkUserRestriction() {
            UserManager um = (UserManager) getSystemService(USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                throw new SecurityException("User cannot modify credentials");
            }
        }
        /**
         * Returns null if actually caller is expected, otherwise return bad package to report
         */
        private String checkCaller(String expectedPackage) {
            String actualPackage = getPackageManager().getNameForUid(getCallingUid());
            return (!expectedPackage.equals(actualPackage)) ? actualPackage : null;
        }

        @Override public boolean hasGrant(int uid, String alias) {
            checkSystemCaller();
            return hasGrantInternal(mDatabaseHelper.getReadableDatabase(), uid, alias);
        }

        @Override public void setGrant(int uid, String alias, boolean value) {
            checkSystemCaller();
            setGrantInternal(mDatabaseHelper.getWritableDatabase(), uid, alias, value);
            broadcastStorageChange();
        }

        private ParceledListSlice<ParcelableString> makeAliasesParcelableSynchronised(
                Set<String> aliasSet) {
            List<ParcelableString> aliases = new ArrayList<ParcelableString>(aliasSet.size());
            for (String alias : aliasSet) {
                ParcelableString parcelableString = new ParcelableString();
                parcelableString.string = alias;
                aliases.add(parcelableString);
            }
            return new ParceledListSlice<ParcelableString>(aliases);
        }

        @Override
        public ParceledListSlice<ParcelableString> getUserCaAliases() {
            synchronized (mTrustedCertificateStore) {
                Set<String> aliasSet = mTrustedCertificateStore.userAliases();
                return makeAliasesParcelableSynchronised(aliasSet);
            }
        }

        @Override
        public ParceledListSlice<ParcelableString> getSystemCaAliases() {
            synchronized (mTrustedCertificateStore) {
                Set<String> aliasSet = mTrustedCertificateStore.allSystemAliases();
                return makeAliasesParcelableSynchronised(aliasSet);
            }
        }

        @Override
        public boolean containsCaAlias(String alias) {
            return mTrustedCertificateStore.containsAlias(alias);
        }

        @Override
        public byte[] getEncodedCaCertificate(String alias, boolean includeDeletedSystem) {
            synchronized (mTrustedCertificateStore) {
                X509Certificate certificate = (X509Certificate) mTrustedCertificateStore
                        .getCertificate(alias, includeDeletedSystem);
                if (certificate == null) {
                    Log.w(TAG, "Could not find CA certificate " + alias);
                    return null;
                }
                try {
                    return certificate.getEncoded();
                } catch (CertificateEncodingException e) {
                    Log.w(TAG, "Error while encoding CA certificate " + alias);
                    return null;
                }
            }
        }

        @Override
        public List<String> getCaCertificateChainAliases(String rootAlias,
                boolean includeDeletedSystem) {
            synchronized (mTrustedCertificateStore) {
                X509Certificate root = (X509Certificate) mTrustedCertificateStore.getCertificate(
                        rootAlias, includeDeletedSystem);
                try {
                    List<X509Certificate> chain = mTrustedCertificateStore.getCertificateChain(
                            root);
                    List<String> aliases = new ArrayList<String>(chain.size());
                    final int n = chain.size();
                    for (int i = 0; i < n; ++i) {
                        String alias = mTrustedCertificateStore.getCertificateAlias(chain.get(i),
                                true);
                        if (alias != null) {
                            aliases.add(alias);
                        }
                    }
                    return aliases;
                } catch (CertificateException e) {
                    Log.w(TAG, "Error retrieving cert chain for root " + rootAlias);
                    return Collections.emptyList();
                }
            }
        }
    };

    private boolean hasGrantInternal(final SQLiteDatabase db, final int uid, final String alias) {
        final long numMatches = DatabaseUtils.longForQuery(db, SELECTION_COUNT_OF_MATCHING_GRANTS,
                new String[]{String.valueOf(uid), alias});
        return numMatches > 0;
    }

    private void setGrantInternal(final SQLiteDatabase db,
            final int uid, final String alias, final boolean value) {
        if (value) {
            if (!hasGrantInternal(db, uid, alias)) {
                final ContentValues values = new ContentValues();
                values.put(GRANTS_ALIAS, alias);
                values.put(GRANTS_GRANTEE_UID, uid);
                db.insert(TABLE_GRANTS, GRANTS_ALIAS, values);
            }
        } else {
            db.delete(TABLE_GRANTS, SELECT_GRANTS_BY_UID_AND_ALIAS,
                    new String[]{String.valueOf(uid), alias});
        }
    }

    private void removeAllGrants(final SQLiteDatabase db) {
        db.delete(TABLE_GRANTS, null /* whereClause */, null /* whereArgs */);
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null /* CursorFactory */, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_GRANTS + " (  "
                    + GRANTS_ALIAS + " STRING NOT NULL,  "
                    + GRANTS_GRANTEE_UID + " INTEGER NOT NULL,  "
                    + "UNIQUE (" + GRANTS_ALIAS + "," + GRANTS_GRANTEE_UID + "))");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVersion, final int newVersion) {
            Log.e(TAG, "upgrade from version " + oldVersion + " to version " + newVersion);

            if (oldVersion == 1) {
                // the first upgrade step goes here
                oldVersion++;
            }
        }
    }

    @Override public IBinder onBind(Intent intent) {
        if (IKeyChainService.class.getName().equals(intent.getAction())) {
            return mIKeyChainService;
        }
        return null;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
            purgeOldGrants();
        }
    }

    private void purgeOldGrants() {
        final PackageManager packageManager = getPackageManager();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.query(TABLE_GRANTS,
                    new String[]{GRANTS_GRANTEE_UID}, null, null, GRANTS_GRANTEE_UID, null, null);
            while (cursor.moveToNext()) {
                final int uid = cursor.getInt(0);
                final boolean packageExists = packageManager.getPackagesForUid(uid) != null;
                if (packageExists) {
                    continue;
                }
                Log.d(TAG, "deleting grants for UID " + uid
                        + " because its package is no longer installed");
                db.delete(TABLE_GRANTS, SELECTION_GRANTS_BY_UID,
                        new String[]{Integer.toString(uid)});
            }
            db.setTransactionSuccessful();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
    }

    private void broadcastStorageChange() {
        Intent intent = new Intent(KeyChain.ACTION_STORAGE_CHANGED);
        sendBroadcastAsUser(intent, new UserHandle(UserHandle.myUserId()));
    }

}
