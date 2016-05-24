/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.htmlviewer;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Simple activity that shows the requested HTML page. This utility is
 * purposefully very limited in what it supports, including no network or
 * JavaScript.
 */
public class HTMLViewerActivity extends Activity {
    private static final String TAG = "HTMLViewer";

    private WebView mWebView;
    private View mLoading;
    /// M: add auto-detector for HTML viewer
    private static final String ENCODING_AUTO_DETECTED = "auto-detector";
    private static final int HTMLVIEWER_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private Intent mRCVDIntent;

    private void processRcvdIntent() {
        setContentView(R.layout.main);

        mWebView = (WebView) findViewById(R.id.webview);
        mLoading = findViewById(R.id.loading);

        mWebView.setWebChromeClient(new ChromeClient());
        mWebView.setWebViewClient(new ViewClient());

        WebSettings s = mWebView.getSettings();
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSavePassword(false);
        s.setSaveFormData(false);
        s.setBlockNetworkLoads(true);

        // Javascript is purposely disabled, so that nothing can be
        // automatically run.
        s.setJavaScriptEnabled(false);
        /// M: add auto-detector for HTML viewer
        //s.setDefaultTextEncodingName("utf-8");
        s.setDefaultTextEncodingName(ENCODING_AUTO_DETECTED);

        if (mRCVDIntent.hasExtra(Intent.EXTRA_TITLE)) {
            setTitle(mRCVDIntent.getStringExtra(mRCVDIntent.EXTRA_TITLE));
        }

        mWebView.loadUrl(String.valueOf(mRCVDIntent.getData()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mRCVDIntent = getIntent();
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "checkSelfPermission return false");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    HTMLVIEWER_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }

        processRcvdIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
            Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
              case HTMLVIEWER_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    processRcvdIntent();
                }
                else {
                    Log.d(TAG, "onRequestPermissionsResult return PERMISSION NOT GRANTED");
                    Toast.makeText(getApplicationContext(), getResources()
                        .getString(com.mediatek.R.string.denied_required_permission),
                        Toast.LENGTH_LONG).show();
                    finish();
                }

                break;
            default:
            //do nothing
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
    }

    private class ChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (!getIntent().hasExtra(Intent.EXTRA_TITLE)) {
                HTMLViewerActivity.this.setTitle(title);
            }
        }
    }

    private class ViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            mLoading.setVisibility(View.GONE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                WebResourceRequest request) {
            final Uri uri = request.getUrl();
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())
                    && uri.getPath().endsWith(".gz")) {
                Log.d(TAG, "Trying to decompress " + uri + " on the fly");
                try {
                    final InputStream in = new GZIPInputStream(
                            getContentResolver().openInputStream(uri));
                    final WebResourceResponse resp = new WebResourceResponse(
                            getIntent().getType(), "utf-8", in);
                    resp.setStatusCodeAndReasonPhrase(200, "OK");
                    return resp;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to decompress; falling back", e);
                }
            }
            return null;
        }
    }
}
