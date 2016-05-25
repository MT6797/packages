package com.nb.mmitest;

import java.io.IOException;

import com.nb.mmitest.R;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.KeyEvent;
import android.os.SystemClock;


class ReceiverTest extends Test {

	private TestLayout1 tl = null;

	private MediaPlayer mMediaPlayer = null;

	private AudioManager am;
   private int defaultVol;
	ReceiverTest(ID pid, String s) {
		this(pid, s, 0);
	}

	ReceiverTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		/*
		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}
		};
		*/
	}

	private void startMelody() throws java.io.IOException,
			IllegalArgumentException, IllegalStateException {
		mMediaPlayer.setLooping(true);
		mMediaPlayer.prepare();
		mMediaPlayer.start();

	}

	void setDataSourceFromResource(Resources resources, MediaPlayer player,
			int res) throws java.io.IOException {
		AssetFileDescriptor afd = resources.openRawResourceFd(res);
		if (afd != null) {
			player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
					afd.getLength());
			afd.close();
		}
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Receiver INIT");
			am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			//mMediaPlayer = new MediaPlayer();	
			
			//mMediaPlayer.setVolume(11.0f,11.0f);		
			//mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);	
			//am.setMode(AudioManager.MODE_IN_COMMUNICATION);
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setSpeakerphoneOn(false);
			am.setWiredHeadsetOn(false);

			//mContext.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
			int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
			Log.d(TAG, "MAX VOL : " + maxVol);
		    defaultVol = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0);
		//	am.setStreamVolume(AudioManager.STREAM_MUSIC,
		//			am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			mMediaPlayer = new MediaPlayer();
			try {
				setDataSourceFromResource(mContext.getResources(),
						mMediaPlayer, R.raw.bootaudio);
				startMelody();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			tl = new TestLayout1(mContext, mName, getResource(R.string.receiver_test));
			mContext.setContentView(tl.ll);
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			
			break;

		case END:
			if (tl != null)
				tl.setEnabledButtons(false);
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Receiver END1");
			//if(MMITest.mgMode == MMITest.MANU_MODE){
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, defaultVol, 0);
			am.setMode(AudioSystem.MODE_NORMAL);
            am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
			am.setStreamMute(AudioManager.STREAM_RING, false);
			//}
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Receiver END");
			SystemClock.sleep(100);
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onTimeInFinished() {
	    if (tl != null)
			tl.showButtons();
	}
}
