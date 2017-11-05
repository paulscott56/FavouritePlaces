package com.paulscott56.favouriteplaces;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Created by paul on 2017/11/05.
 */

class MessageParser implements Runnable {

    private static final String TAG = "MessageParser";

    @NonNull
    private final String message;
    @NonNull
    private final SafeCallback<LocationDetail> callback;

    /**
     * MessageParser is a simple mechanism for parsing a {@link JSONObject} out
     * of a String in a way conducive to scheduling.
     *
     * @param message any String. Must not be null
     * @param callback will be referenced if a {@link LocationDetail}'
     *                 {@link LocationDetail#place}
     *                 is found within the message. May not be null
     */
    public MessageParser(@NonNull String message, @NonNull SafeCallback<LocationDetail> callback) {
        this.message = message;
        this.callback = callback;
    }

    @Override
    public void run() {
        Log.i(TAG, message);

        try {
            JSONObject obj = new JSONObject(message);
            String msgType = obj.optString("type");
            if (Objects.equals(msgType, "speak")) {
                String ret = obj.getJSONObject("data").getString("utterance");
                LocationDetail mu = new LocationDetail();
                mu.place = ret;
                callback.call(mu);
            }

        } catch (JSONException e) {
            Log.w(TAG, "The response received did not conform to our expected JSON formats.", e);
        }

    }
}
