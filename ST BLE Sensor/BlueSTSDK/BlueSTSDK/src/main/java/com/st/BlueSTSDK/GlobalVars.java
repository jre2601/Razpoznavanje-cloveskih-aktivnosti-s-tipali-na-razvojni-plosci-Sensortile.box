package com.st.BlueSTSDK;

import android.app.Application;

public class GlobalVars extends Application {

    //add this variable declaration:
    public static LabelState currentState = LabelState.UNDEFINED;
    public static LabelState currentPrediction = LabelState.UNDEFINED;

    private static GlobalVars singleton;

    public static GlobalVars getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }
}
