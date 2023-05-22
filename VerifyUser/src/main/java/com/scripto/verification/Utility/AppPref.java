package com.scripto.verification.Utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.scripto.verification.R;

public class AppPref {

    private static final String PREF_NAME = "AppData";
    final SharedPreferences pref;
    final SharedPreferences.Editor editor;
    final int PRIVATE_MODE = 0;

    public AppPref(Context mContext) {
        pref = mContext.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void clearData(){
        editor.clear();
        editor.commit();
    }

    public void clearSelfieData(Context mContext){
        editor.remove(mContext.getString(R.string.face_count));
        editor.remove(mContext.getString(R.string.face_angle_up_down));
        editor.remove(mContext.getString(R.string.face_angle_left_right));
        editor.remove(mContext.getString(R.string.face_angle_tilted));
        editor.remove(mContext.getString(R.string.face_id));
        editor.remove(mContext.getString(R.string.face_open_prob_right));
        editor.remove(mContext.getString(R.string.face_open_prob_left));
        editor.commit();
    }

    public void putResponse(String name, String data){
        editor.putString(name, data);
        editor.commit();
    }

    public String getResponse(String name) {
        return pref.getString(name, null);
    }
}
