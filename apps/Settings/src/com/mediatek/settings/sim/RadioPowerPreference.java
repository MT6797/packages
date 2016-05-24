package com.mediatek.settings.sim;

import android.content.Context;
import android.preference.Preference;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.util.Log;

import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

/**
 * A preference for radio switch function.
 */
public class RadioPowerPreference extends Preference {

    private static final String TAG = "RadioPowerPreference";
    private boolean mPowerState;
    private boolean mPowerEnabled = true;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private Switch mRadioSwith = null;
    private RadioPowerController mController;

    /**
     * Construct of RadioPowerPreference.
     * @param context Context.
     */
    public RadioPowerPreference(Context context) {
        super(context);
        mController = RadioPowerController.getInstance(context);
        setWidgetLayoutResource(R.layout.radio_power_switch);
    }

    /**
     * Set the radio switch state.
     * @param state On/off.
     */
    public void setRadioOn(boolean state) {
        mPowerState = state;
        if (mRadioSwith != null) {
            mRadioSwith.setChecked(state);
        }
    }

    /**
     * Set the radio switch enable state.
     * @param enable Enable.
     */
    public void setRadioEnabled(boolean enable) {
        mPowerEnabled = enable;
        if (mRadioSwith != null) {
            mRadioSwith.setEnabled(enable);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mRadioSwith = (Switch) view.findViewById(R.id.radio_state);
        if (mRadioSwith != null) {
            if (FeatureOption.MTK_A1_FEATURE) {
                mRadioSwith.setVisibility(View.GONE);
            }
            mRadioSwith.setChecked(mPowerState);
            mRadioSwith.setEnabled(mPowerEnabled);
            mRadioSwith.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, "checked state change, mPowerState = " + mPowerState
                            + ", isChecked = " + isChecked + ", subId = " + mSubId);
                    if (mPowerState != isChecked) {
                        if (mController.setRadionOn(mSubId, isChecked)) {
                            // disable radio switch to prevent continuous click
                            setRadioEnabled(false);
                        } else {
                            // if set radio fail, revert button status.
                            Log.w(TAG, "set radio power FAIL!");
                            setRadioOn(!isChecked);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mPowerEnabled = enabled;
        super.setEnabled(enabled);
    }

    /**
     * Bind the preference with corresponding property.
     * @param preference {@link RadioPowerPreference}.
     * @param subId subId
     */
    public void bindRadioPowerState(final int subId) {
        mSubId = subId;
        setRadioOn(TelephonyUtils.isRadioOn(subId, getContext()));
        setRadioEnabled(SubscriptionManager.isValidSubscriptionId(subId));
    }
}
