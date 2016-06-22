package com.nb.aging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class NbAgingTestConfigActivity extends Activity implements View.OnClickListener{
	private CheckBox mVsBox;
	private CheckBox mVrBox;
	private CheckBox mVirbateBox;
	private CheckBox mMrBox;
	private CheckBox mFrontCBox;
	private CheckBox mBackCBox;
	private EditText mHourEdt;
	private EditText mMinEdt;
	private TextView mVsResultTest;
	private TextView mVrResultText;
	private TextView mVirabteResultText;
	private TextView mMrResultText;
	private TextView mBackCameraResultText;
	private TextView mFrontCameraResultText;
	private SharedPreferences mSharePreference;
	private Editor mEditor;
	private Button mStartBtn;
	private final String TAG="NbAgingTestConfigActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aging_test_config);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);
		mSharePreference = getSharedPreferences("AgingConfig", Context.MODE_PRIVATE);
		mEditor = mSharePreference.edit();
		mVsBox = (CheckBox)findViewById(R.id.vedioSpearChb);
		mVrBox = (CheckBox)findViewById(R.id.vedioReceiveChb);
		mVirbateBox = (CheckBox)findViewById(R.id.vibrateChb);
		mMrBox = (CheckBox)findViewById(R.id.micReceiverChb);
		mFrontCBox = (CheckBox)findViewById(R.id.backCameraChb);
		mBackCBox = (CheckBox)findViewById(R.id.forntCameraChb);
		mHourEdt = (EditText)findViewById(R.id.hourEdt);
		mMinEdt = (EditText)findViewById(R.id.minEdt);
		mVsResultTest = (TextView)findViewById(R.id.vsResultTxt);
		mVrResultText = (TextView)findViewById(R.id.vrResultTxt);
		mVirabteResultText = (TextView)findViewById(R.id.vibrateResultTxt);
		mMrResultText = (TextView)findViewById(R.id.mrResultTxt);
		mBackCameraResultText = (TextView)findViewById(R.id.backCameraResultTxt);
		mFrontCameraResultText = (TextView)findViewById(R.id.frontCameraResultTxt);
		
		
		mVsBox.setChecked(mSharePreference.getBoolean("VS", true));
		mVrBox.setChecked(mSharePreference.getBoolean("VR", true));
		mVirbateBox.setChecked(mSharePreference.getBoolean("Virbate", true));
		mMrBox.setChecked(mSharePreference.getBoolean("MR", true));
		mFrontCBox.setChecked(mSharePreference.getBoolean("FRONTCAMERA", true));
		mBackCBox.setChecked(mSharePreference.getBoolean("BACKCAMERA", true));
		mHourEdt.setText(mSharePreference.getString("TEST_Hours", "3"));
		mMinEdt.setText(mSharePreference.getString("TEST_Mins", "0"));
		
		mVsBox.setOnClickListener(this);
		mVrBox.setOnClickListener(this);
		mVirbateBox.setOnClickListener(this);
		mMrBox.setOnClickListener(this);
		mFrontCBox.setOnClickListener(this);
		mBackCBox.setOnClickListener(this);
		mHourEdt.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				mEditor.putString("TEST_Hours", mHourEdt.getText().toString());
				mEditor.commit();
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub				
			}
			
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub				
			}
		});
		mMinEdt.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				mEditor.putString("TEST_Mins", mMinEdt.getText().toString());
				mEditor.commit();
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub				
			}
			
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub				
			}
		});
		mStartBtn = (Button)findViewById(R.id.start);
		mStartBtn.setOnClickListener(this);

		mVsResultTest.setText(this.getResources().getString(R.string.vedioSpeak)+":"+mSharePreference.getString("vedioSpeakTestTime", "00:00:00"));
		mVrResultText.setText(this.getResources().getString(R.string.vedioReceive)+":"+mSharePreference.getString("vedioReceiveTestTime", "00:00:00"));
		mVirabteResultText.setText(this.getResources().getString(R.string.Virbate)+":"+mSharePreference.getString("VirbateTestTime", "00:00:00"));
		mMrResultText.setText(this.getResources().getString(R.string.micReceive)+":"+mSharePreference.getString("micReceiveTestTime", "00:00:00"));
		mBackCameraResultText.setText(this.getResources().getString(R.string.backCamera)+":"+mSharePreference.getString("backCameraTestTime", "00:00:00"));
		mFrontCameraResultText.setText(this.getResources().getString(R.string.frontCamera)+":"+mSharePreference.getString("frontCameraTestTime", "00:00:00"));
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
		{
			case R.id.vedioSpearChb:					
				mEditor.putBoolean("VS", mVsBox.isChecked());
				break;
			case R.id.vedioReceiveChb:
				mEditor.putBoolean("VR", mVrBox.isChecked());
				break;
			case R.id.vibrateChb:
				mEditor.putBoolean("Virbate", mVirbateBox.isChecked());
				break;
			case R.id.micReceiverChb:
				mEditor.putBoolean("MR", mMrBox.isChecked());
				break;
			case R.id.backCameraChb:
				mEditor.putBoolean("FRONTCAMERA", mFrontCBox.isChecked());
				break;
			case R.id.forntCameraChb:
				mEditor.putBoolean("BACKCAMERA", mBackCBox.isChecked());
				break;
			case R.id.start:
				Intent intent = new Intent();
				intent.setClass(this, NbAgingTest.class);
				this.startActivity(intent);
				break;
		}
		mEditor.commit();
		
	}
	protected void onResume()
	{
		super.onResume();
		mVsResultTest.setText(this.getResources().getString(R.string.vedioSpeak)+":"+mSharePreference.getString("vedioSpeakTestTime", "00:00:00"));
		mVrResultText.setText(this.getResources().getString(R.string.vedioReceive)+":"+mSharePreference.getString("vedioReceiveTestTime", "00:00:00"));
		mVirabteResultText.setText(this.getResources().getString(R.string.Virbate)+":"+mSharePreference.getString("VirbateTestTime","00:00:00"));
		mMrResultText.setText(this.getResources().getString(R.string.micReceive)+":"+mSharePreference.getString("micReceiveTestTime", "00:00:00"));
		mBackCameraResultText.setText(this.getResources().getString(R.string.backCamera)+":"+mSharePreference.getString("backCameraTestTime", "00:00:00"));
		mFrontCameraResultText.setText(this.getResources().getString(R.string.frontCamera)+":"+mSharePreference.getString("frontCameraTestTime", "00:00:00"));
		Log.d(TAG, "mSharePreference.getString "+mSharePreference.getString("VirbateTestTime", "00:00:00"));
	}

}
