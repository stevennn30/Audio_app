package com.serafimtech.serafimaudio.FileData;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public class UserSetting {
    Context context;
    private final String FirstTime = "FirstTime";
    private final String Test = "Test";
    private final String VideoDay = "VideoDay";

    public UserSetting(Context context){
        this.context = context;
    }

    public void setFirstTime(Boolean bool) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            SharedPreferences getPrefs = context.getSharedPreferences("serafim_audio", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = getPrefs.edit();
            editor.putBoolean(FirstTime, bool);
            editor.apply();
        }else{
            SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = getPrefs.edit();
            editor.putBoolean(FirstTime, bool);
            editor.apply();
        }
    }

    public Boolean getFirstTime() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            SharedPreferences getPrefs = context.getSharedPreferences("serafim_audio", Context.MODE_PRIVATE);
            return getPrefs.getBoolean(FirstTime, true);
        }else{
            SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            return getPrefs.getBoolean(FirstTime, true);
        }
    }

    public void setVideoDay(String format) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            SharedPreferences getPrefs = context.getSharedPreferences("serafim_audio", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = getPrefs.edit();
            editor.putString(FirstTime, format);
            editor.apply();
        }else{
            SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = getPrefs.edit();
            editor.putString(VideoDay, format);
            editor.apply();
        }
    }

    public String getVideoDay() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            SharedPreferences getPrefs = context.getSharedPreferences("serafim_audio", Context.MODE_PRIVATE);
            return getPrefs.getString(VideoDay, "");
        }else{
            SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            return getPrefs.getString(VideoDay, "");
        }

    }

    public void setTest(Boolean bool) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            SharedPreferences getPrefs = context.getSharedPreferences("serafim_audio", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = getPrefs.edit();
            editor.putBoolean(Test, bool);
            editor.apply();
        }else{
            SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = getPrefs.edit();
            editor.putBoolean(Test, bool);
            editor.apply();
        }
    }

    public Boolean getTest() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            SharedPreferences getPrefs = context.getSharedPreferences("serafim_audio", Context.MODE_PRIVATE);
            return getPrefs.getBoolean(Test, false);
        }else{
            SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            return getPrefs.getBoolean(Test, false);
        }
    }
}
