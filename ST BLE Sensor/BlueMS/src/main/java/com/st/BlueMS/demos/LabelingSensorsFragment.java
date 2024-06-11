/*
 * Copyright (c) 2017  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *   STMicroelectronics company nor the names of its contributors may be used to endorse or
 *   promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *   in a directory whose title begins with st_images may only be used for internal purposes and
 *   shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *   icons, pictures, logos and other images that are provided with the source code in a directory
 *   whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package com.st.BlueMS.demos;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;

import com.st.BlueMS.R;
import com.st.BlueMS.demos.NodeStatus.NodeStatusFragment;
import com.st.BlueMS.demos.util.BaseDemoFragment;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureBattery;
import com.st.BlueSTSDK.Features.FeatureHumidity;
import com.st.BlueSTSDK.Features.FeatureLuminosity;
import com.st.BlueSTSDK.Features.FeaturePressure;
import com.st.BlueSTSDK.Features.FeatureTemperature;
import com.st.BlueSTSDK.Features.Field;
import com.st.BlueSTSDK.GlobalVars;
import com.st.BlueSTSDK.LabelState;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.gui.NodeGui;
import com.st.BlueSTSDK.gui.demos.DemoDescriptionAnnotation;

import java.util.List;

/**
 * Show the Temperature, Humidity, lux and barometer values. Each value will have also its icon
 * that will change in base of the feature value
 */
@DemoDescriptionAnnotation(name = "ACC/GYRO/MAG labeling", iconRes = R.drawable.demo_sensors_fusion,
        requareOneOf = {FeatureHumidity.class, FeatureLuminosity.class, FeaturePressure.class,
                FeatureTemperature.class,
                FeatureAcceleration.class

        })
public class LabelingSensorsFragment extends BaseDemoFragment {

    private static final String BATTERY_CAPACITY = NodeStatusFragment.class.getName()+".BATTERY_CAPACITY";

    private static long RSSI_UPDATE_PERIOD_MS=1000;

    private Handler mUpdateRssiRequestQueue;
    private Runnable mAskNewRssi = new Runnable() {
        @Override
        public void run() {
            Node n = getNode();
            if(n!=null){
                n.readRssi();
                mUpdateRssiRequestQueue.postDelayed(mAskNewRssi,RSSI_UPDATE_PERIOD_MS);
            }//if
        }//run
    };

    private FeatureBattery mBatteryFeature;
    private TextView mBatteryStatusText;
    private TextView mBatteryPercentageText;
    private ImageView mBatteryIcon;
    private CardView mBatteryCardView;
    private static int[] BATTERY_CHARGING_IMAGES = new int[]{
            R.drawable.battery_00c,
            R.drawable.battery_20c,
            R.drawable.battery_40c,
            R.drawable.battery_60c,
            R.drawable.battery_80c,
            R.drawable.battery_100c
    };

    private static int[] BATTERY_DISCHARGE_IMAGES = new int[]{
            R.drawable.battery_00,
            R.drawable.battery_20,
            R.drawable.battery_40,
            R.drawable.battery_60,
            R.drawable.battery_80,
            R.drawable.battery_100
    };

    private float mBatteryCapacity;


    private FeatureBattery.FeatureBatteryListener mBatteryListener =
        new FeatureBattery.FeatureBatteryListener() {
            @Override
            public void onCapacityRead(FeatureBattery featureBattery, int batteryCapacity) {
                mBatteryCapacity=batteryCapacity;
            }

            @Override
            public void onMaxAssorbedCurrentRead(FeatureBattery featureBattery, float current) {
            }

            private int getIconIndex(float percentage, int nIcons){
                int iconIndex = (((int) percentage) * nIcons) / 100;
                return MathUtils.clamp(iconIndex,0,nIcons-1);
            }

            private @DrawableRes
            int getBatteryIcon(float percentage, FeatureBattery.BatteryStatus status){
                int index;
                switch (status){
                    case LowBattery:
                    case Discharging:
                    case PluggedNotCharging:
                        index = getIconIndex(percentage, BATTERY_DISCHARGE_IMAGES.length);
                        return BATTERY_DISCHARGE_IMAGES[index];
                    case Charging:
                        index = getIconIndex(percentage, BATTERY_CHARGING_IMAGES.length);
                        return BATTERY_CHARGING_IMAGES[index];
                    case Unknown:
                    case Error:
                        return R.drawable.battery_missing;
                }
                return R.drawable.battery_missing;
            }

            @Override
            public void onUpdate(@NonNull Feature f, @NonNull Feature.Sample data) {
                final Field[] fieldsDesc = f.getFieldsDesc();
                final Resources res = LabelingSensorsFragment.this.getResources();
                final float percentage = FeatureBattery.getBatteryLevel(data);
                final FeatureBattery.BatteryStatus status = FeatureBattery.getBatteryStatus(data);
                final float voltage = FeatureBattery.getVoltage(data);
                final float current = FeatureBattery.getCurrent(data);

                final @DrawableRes int batteryIcon = getBatteryIcon(percentage, status);
                final Drawable icon = ContextCompat.getDrawable(requireContext(),batteryIcon);

                final String batteryStatus = "Status: "+status;

                final String batteryPercentage = res.getString(R.string.nodeStatus_battery_percentage,
                        percentage,fieldsDesc[FeatureBattery.PERCENTAGE_INDEX].getUnit());
                final String batteryVoltage = res.getString(R.string.nodeStatus_battery_voltage,
                        voltage, fieldsDesc[FeatureBattery.VOLTAGE_INDEX].getUnit());

                updateGui(() -> {
                    try {

                        if(status!= FeatureBattery.BatteryStatus.Unknown) {
                            mBatteryStatusText.setVisibility(View.VISIBLE);
                            mBatteryStatusText.setText(batteryStatus);
                        } else {
                            mBatteryStatusText.setVisibility(View.INVISIBLE);
                        }

                        if(Float.isNaN(percentage) | (percentage==0.0)) {
                            mBatteryPercentageText.setVisibility(View.INVISIBLE);
                        } else {
                            mBatteryPercentageText.setVisibility(View.VISIBLE);
                            mBatteryPercentageText.setText(batteryPercentage);
                        }

                        mBatteryIcon.setImageDrawable(icon);
                    }catch (NullPointerException e){
                        //this exception can happen when the task is run after the fragment is
                        // destroyed
                    }
                });
            }//onUpdate
        };



    private interface ExtractDataFunction {
        float getData(Feature.Sample s);
    }

    /**
     * get a sample for each feature and extract the float data from it
     *
     * @param features            list of feature
     * @param extractDataFunction object that will extract the data from the sample
     * @return list of values inside the feature
     */
    private static float[] extractData(final List<? extends Feature> features,
                                       final ExtractDataFunction extractDataFunction) {
        int nFeature = features.size();
        float data[] = new float[nFeature];
        for (int i = 0; i < nFeature; i++) {
            data[i] = extractDataFunction.getData(features.get(i).getSample());
        }//for
        return data;
    }//extractData


    ////////////////////ACCELERATION///////////////////////////////////////////////////////////////////
    /**
     * feature where we will read the humidity_icon value
     */
    private List<FeatureAcceleration> mAcceleration;

    private ToggleButton[] labelButtons;

    //getData
    /**
     * object that extract the humidity_icon from a feature sample
     */
    private final static ExtractDataFunction sExtractDataAccX = FeatureAcceleration::getAccX;
    private final static ExtractDataFunction sExtractDataAccY = FeatureAcceleration::getAccY;
    private final static ExtractDataFunction sExtractDataAccZ = FeatureAcceleration::getAccZ;
    /**
     * listener for the humidity_icon feature, it will update the humidity_icon value and change the alpha
     * value for the {@code mHumidityImage} image
     */
    private class AccelerationListener implements Feature.FeatureListener {
        AccelerationListener(Resources res) {

        }

        @Override
        public void onUpdate(@NonNull Feature f, @NonNull Feature.Sample sample) {
            float dataX[] = extractData(mAcceleration, sExtractDataAccX);

            /*updateGui(() -> {
                try {
                } catch (NullPointerException e) {
                    //this exception can happen when the task is run after the fragment is
                    // destroyed
                }
            });*/

        }//on update
    }

    //we cant initialize the listener here because we need to wait that the fragment is attached
    // to an activity
    private Feature.FeatureListener mAccelerationListener;


    ///////// BATTERY INFO /////////////////////////////////////////////////////////////////////////

    private void loadBatteryCapacity(@Nullable Bundle savedInstanceState) {
        if(savedInstanceState==null) {
            mBatteryCapacity = Float.NaN;
        }else{
            mBatteryCapacity = savedInstanceState.getFloat(BATTERY_CAPACITY,Float.NaN);
        }
    }

    private void loadBatteryCapacity() {
        if(!Float.isNaN(mBatteryCapacity))
            return;
        mBatteryFeature.readBatteryCapacity();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    public LabelingSensorsFragment() {
        // Required empty public constructor
    }

    /**
     * Resets all toggle buttons to OFF state
     */
    private void resetLabelButtonState() {
        if (labelButtons != null)
            for (ToggleButton tb: labelButtons) {
                tb.setChecked(false);
            }
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        HandlerThread looper = new HandlerThread(NodeStatusFragment.class.getName()+".UpdateRssi");
        looper.start();
        mUpdateRssiRequestQueue = new Handler(looper.getLooper());

        loadBatteryCapacity(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View root = inflater.inflate(R.layout.fragment_labeling_sensors, container, false);
        labelButtons = new ToggleButton[]{
            root.findViewById(R.id.walkingLabelButton),
            root.findViewById(R.id.standingLabelButton),
            root.findViewById(R.id.standUpLabelButton),
            root.findViewById(R.id.sitDownLabelButton),
            root.findViewById(R.id.sittingLabelButton),
            root.findViewById(R.id.lyingToSittingLabelButton),
            root.findViewById(R.id.lieDownLabelButton),
            root.findViewById(R.id.lyingLabelButton),
            root.findViewById(R.id.fallLabelButton)
        };
        for (ToggleButton tb: labelButtons) {
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {    // The toggle is enabled
                        // Reset all buttons OFF state
                        resetLabelButtonState();
                        buttonView.setChecked(true);
                        // Set the corresponding label
                        switch (buttonView.getResources().getResourceName(buttonView.getId()).split(":id/")[1]) {
                            case "walkingLabelButton": GlobalVars.currentState = LabelState.WALKING; break;
                            case "standingLabelButton": GlobalVars.currentState = LabelState.STANDING; break;
                            case "standUpLabelButton": GlobalVars.currentState = LabelState.STAND_UP; break;
                            case "sitDownLabelButton": GlobalVars.currentState = LabelState.SIT_DOWN; break;
                            case "sittingLabelButton": GlobalVars.currentState = LabelState.SITTING; break;
                            case "lyingToSittingLabelButton": GlobalVars.currentState = LabelState.LYING_TO_SITTING; break;
                            case "lieDownLabelButton": GlobalVars.currentState = LabelState.LIE_DOWN; break;
                            case "lyingLabelButton": GlobalVars.currentState = LabelState.LYING; break;
                            case "fallLabelButton": GlobalVars.currentState = LabelState.FALL; break;
                            default: GlobalVars.currentState = LabelState.UNDEFINED;
                        }
                        System.out.println(GlobalVars.currentState);
                    } else {     // The toggle is disabled
                        buttonView.setChecked(false);
                        GlobalVars.currentState = LabelState.UNDEFINED;
                    }
                }
            });
        }

        mBatteryPercentageText = root.findViewById(R.id.status_batteryPercentageText);
        mBatteryStatusText = root.findViewById(R.id.status_batteryStatusText);
        mBatteryCardView = root.findViewById(R.id.status_batteryCard);
        mBatteryCardView.setVisibility(View.GONE);

        mBatteryIcon = root.findViewById(R.id.status_batteryImage);

        return root;
    }


    /**
     * enable the notification if the feature is not null + attach the listener for update the
     * gui when an update arrive + if you click the image we ask to read the value
     * if a feature is not available by the node a toast message is shown
     *
     * @param node node where the notification will be enabled
     */
    @Override
    protected void enableNeededNotification(@NonNull Node node) {

        mAcceleration = node.getFeatures(FeatureAcceleration.class);
        if (!mAcceleration.isEmpty()) {
            View.OnClickListener forceUpdate = new ForceUpdateFeature(mAcceleration);
            mAccelerationListener = new AccelerationListener(getResources());
            //mAccelerationImage.setOnClickListener(forceUpdate);
            for (Feature f : mAcceleration) {
                f.addFeatureListener(mAccelerationListener);
                node.enableNotification(f);
            }//for
            /*updateGui(() -> {
                mHumidityCard.setVisibility(View.VISIBLE);
            });*/
        } else {
            /*updateGui(() -> {
                mHumidityImage.setImageResource(R.drawable.humidity_missing);
                mHumidityCard.setVisibility(View.GONE);
            });*/
        }

        // BATTERY INFO
        mBatteryFeature = node.getFeature(FeatureBattery.class);
        //node.addBleConnectionParamListener(this);
        mUpdateRssiRequestQueue.postDelayed(mAskNewRssi, RSSI_UPDATE_PERIOD_MS);
        if(mBatteryFeature!=null){
            mBatteryCardView.setVisibility(View.VISIBLE);
            mBatteryFeature.addFeatureListener(mBatteryListener);
            node.enableNotification(mBatteryFeature);
            loadBatteryCapacity();
        }

    }//enableNeededNotification


    /**
     * remove the listener and disable the notification
     *
     * @param node node where disable the notification
     */
    @Override
    protected void disableNeedNotification(@NonNull Node node) {
        if (mAcceleration != null && !mAcceleration.isEmpty()) {
            for (Feature f : mAcceleration) {
                f.removeFeatureListener(mAccelerationListener);
                node.disableNotification(f);
            }//for
        }

        // BATTERY INFO
        //node.removeBleConnectionParamListener(this);
        mUpdateRssiRequestQueue.removeCallbacks(mAskNewRssi);
        if(mBatteryFeature!=null){
            mBatteryFeature.removeFeatureListener(mBatteryListener);
            node.disableNotification(mBatteryFeature);
        }

    }//disableNeedNotification

    /**
     * simple callback class that will request to read the feature value when the user click on
     * the attached
     */
    static private class ForceUpdateFeature implements View.OnClickListener {

        /**
         * feature to read
         */
        private List<? extends Feature> mFeatures;

        /**
         * @param feature feature to read when the user click on a view
         */
        ForceUpdateFeature(List<? extends Feature> feature) {
            mFeatures = feature;
        }

        /**
         * send the read request to the feature
         *
         * @param v clicked view
         */
        @Override
        public void onClick(View v) {
            for (Feature f : mFeatures) {
                Node node = f.getParentNode();
                if (node != null)
                    node.readFeature(f);
            }//if
        }//onClick

    }//ForceUpdateFeature
}
