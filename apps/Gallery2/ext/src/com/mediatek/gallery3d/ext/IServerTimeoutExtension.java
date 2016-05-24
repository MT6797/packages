package com.mediatek.gallery3d.ext;

import android.media.MediaPlayer;
import android.media.Metadata;
import android.os.Bundle;

/**
 * ServerTimeout extension interface.
 */
public interface IServerTimeoutExtension {
    /**
      * record the time disconnect from server.
      * @internal
      */
    void recordDisconnectTime();
    /**
      * clear server timout info.
      * @internal
      */
    void clearServerInfo();
    /**
      * dismiss server timout dialog.
      * @internal
      */

    void clearTimeoutDialog();
    /**
      * restore last disconnect time
      * @param icicle
      * @internal
      */

    void onRestoreInstanceState(Bundle icicle);
    /**
      * save last disconnect time
      * @param outState
      * @internal
      */
    void onSaveInstanceState(Bundle outState);
    /**
      * @return true if a dialog is showing and the streaming is not httplive,
      * or have notifed user server timeout
      * @internal
      */
    boolean handleOnResume();
    /**
      * @return true if a dialog is showing
      * @param mp
      * @param what
      * @param extra
      * @internal
      */
    boolean onError(MediaPlayer mp, int what, int extra);
    /**
      * get server timeout from metadata
      * @param data
      * @internal
      */
    void setVideoInfo(Metadata data);
}
