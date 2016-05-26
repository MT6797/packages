package com.android.incallui;

import java.util.List;

import com.android.incallui.AnswerFragment.RespondViaSmsItemClickListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.android.incallui.widget.SlidingTab.NbSlidingTab;
import android.telecom.VideoProfile;
public class HallCallFragment extends
		BaseFragment<AnswerPresenter, AnswerPresenter.AnswerUi> implements
		NbSlidingTab.OnTriggerListener, AnswerPresenter.AnswerUi {
	private NbSlidingTab mGlowpad;
	private ImageView mPhoto;
	private TextView mCallStatus;
	private TextView mName;
	private ImageButton mEndCall;
	private CallCardFragment mCallCardFragment;
	private View mFloatingActionButtonContainer;
	private LinearLayout mHallMain;
	private ImageView mEendCallView;
	

	private static final int ANSWER_CALL_ID = 1; // drag right
	private static final int SEND_SMS_ID = 3; // drag up
	private static final int DECLINE_CALL_ID = 2; // drag left

    public HallCallFragment()
    {
    	
    }

	@Override
	public AnswerPresenter createPresenter() {
		return new AnswerPresenter();
	}

	@Override
	public AnswerPresenter.AnswerUi getUi() {
		return this;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("bll", "xxx====>onCreateView");
		final View parent = inflater.inflate(R.layout.hall_call_fragment,
				container, false);
		mGlowpad = (NbSlidingTab) parent.findViewById(R.id.glow_pad_view);
		parent.setBackgroundResource(android.R.color.black);
		mPhoto = (ImageView) parent.findViewById(R.id.hall_photo);
		mCallStatus = (TextView) parent.findViewById(R.id.callStatus);
		mName = (TextView) parent.findViewById(R.id.name);
		mEndCall = (ImageButton) parent
				.findViewById(R.id.end_call_action_button);
		mHallMain = (LinearLayout)parent
				.findViewById(R.id.hall_main);
		mEndCall.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mCallCardFragment.getPresenter().endCallClicked();
			}
		});
		mFloatingActionButtonContainer = parent
				.findViewById(R.id.floating_end_call_action_button_container);
		mEendCallView = (ImageView)parent.findViewById(R.id.end_call_view);
		// mGlowpad.setAnswerListener(this);
		// mGlowpad.startPing();
		mGlowpad.setOnTriggerListener(this);
		return parent;
	}

	@Override
	public void onDestroyView() {
		Log.d(this, "onDestroyView");
		super.onDestroyView();
	}

	public void dismissPendingDialogues() {

	}


	@Override
	public Context getContext() {
		return getActivity();
	}

	@Override
	public void configureMessageDialog(List<String> textResponses) {

	}

	@Override
	public void showMessageDialog() {

	}
	@Override
	public void showTargets(int targetSet) {
	}
	@Override
	public void showTargets(int targetSet,int parma) {
	}
	@Override
	public void dismissPendingDialogs(){}
	@Override
	public void onShowAnswerUi(boolean show) {
		mGlowpad.setVisibility(show ? View.VISIBLE : View.GONE);
		mGlowpad.reset();
		Log.d("bll", "======>showAnswerUi: " + show);
	}

	@Override
	public void onTrigger(View view, int whichHandle) {
		Log.d(this, "onTrigger(whichHandle = " + whichHandle + ")...");

		switch (whichHandle) {
		case ANSWER_CALL_ID:

			//getPresenter().onAnswer(VideoProfile.STATE_AUDIO_ONLY, getContext());
            InCallPresenter.getInstance().answerIncomingCall(
            		getContext(), VideoProfile.STATE_AUDIO_ONLY);
			break;

		case SEND_SMS_ID:
			getPresenter().onText();
			break;

		case DECLINE_CALL_ID:
			//getPresenter().onDecline(getContext());
			InCallPresenter.getInstance().declineIncomingCall(getContext());
			break;

		default:
			break;
		}
	}

	@Override
	public void onGrabbedStateChange(View v, int grabbedState) {

	}

	public void setPhoto(Drawable image) {
		if (image != null) {
			mPhoto.setImageDrawable(image);
		}
	}

	public void setCallStatus(String callStatus) {
		mCallStatus.setText(callStatus);
	}

	public void showEndCallUi(boolean show) {
		mFloatingActionButtonContainer.setVisibility(show ? View.VISIBLE
				: View.GONE);
		if(show)
			onShowAnswerUi(false);
		Log.d("bll", "======>showEndCallUi: " + show);
	}
	
	public void setHallMainBackground(int state) {
		if(state == Call.State.DISCONNECTING|| state == Call.State.DISCONNECTED)
		{
			mHallMain.setBackgroundResource(R.drawable.hall_end_back);
			mEendCallView.setVisibility(View.VISIBLE);
		}else
		{
			mHallMain.setBackgroundResource(R.drawable.hall_calling_back);
			mEendCallView.setVisibility(View.GONE);
		}
	}

	public void setName(String name) {
		mName.setText(name);
	}

	public void setCallCardFragment(CallCardFragment callCardFragment) {
		mCallCardFragment = callCardFragment;
	}

}
