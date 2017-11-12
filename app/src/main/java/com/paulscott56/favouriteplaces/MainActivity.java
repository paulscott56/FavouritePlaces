package com.paulscott56.favouriteplaces;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.paulscott56.favouriteplaces.receivers.NetworkChangeReceiver;
import com.paulscott56.favouriteplaces.com.paulscott56.utils.NetworkUtil;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.paulscott56.favouriteplaces.Constants.LOCATION_PERMISSION_PREFERENCE_KEY;
import static com.paulscott56.favouriteplaces.Constants.VERSION_CODE_PREFERENCE_KEY;
import static com.paulscott56.favouriteplaces.Constants.VERSION_NAME_PREFERENCE_KEY;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "fav";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public WebSocketClient mWebSocketClient;
    private String wsip;

    @NonNull
    private final List<LocationDetail> places = new ArrayList<>();

    PlacesAdapter pa = new PlacesAdapter(places);

    NetworkChangeReceiver networkChangeReceiver;
    BroadcastReceiver wearBroadcastReceiver;

    private boolean isNetworkChangeReceiverRegistered;

    RecyclerView recList;

    private SharedPreferences sharedPref;

    boolean launchedFromWidget = false;
    boolean autoPromptForSpeech = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        recList.setAdapter(pa);

        registerReceivers();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPlace();
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

    }

    private void registerReceivers() {
        registerNetworkReceiver();
    }

    private void addPlace() {
    }

    public void connectWebSocket() {
        URI uri = deriveURI();

        if (uri != null) {
            mWebSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i("Websocket", "Opened");
                }

                @Override
                public void onMessage(String s) {
                    // Log.i(TAG, s);
                    runOnUiThread(new MessageParser(s, new SafeCallback<LocationDetail>() {
                        @Override
                        public void call(@NonNull LocationDetail ld) {
                            addData(ld);
                        }
                    }));
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i("Websocket", "Closed " + s);

                }

                @Override
                public void onError(Exception e) {
                    Log.i("Websocket", "Error " + e.getMessage());
                }
            };
            mWebSocketClient.connect();
        }
    }

    private void addData(LocationDetail ld) {
        places.add(ld);
        pa.notifyItemInserted(places.size() - 1);
        recList.smoothScrollToPosition(pa.getItemCount() - 1);
    }

    /**
     * This method will attach the correct path to the
     * {@link #wsip} hostname to allow for communication
     * with a Mycroft instance at that address.
     * <p>
     *     If {@link #wsip} cannot be used as a hostname
     *     in a {@link URI} (e.g. because it's null), then
     *     this method will return null.
     * </p>
     *
     * @return a valid uri, or null
     */
    @Nullable
    private URI deriveURI() {
        URI uri = null;

        if (wsip != null && !wsip.isEmpty()) {
            try {
                uri = new URI("ws://" + wsip + ":8181/core");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            uri = null;
        }
        return uri;
    }

    public void sendMessage(String msg) {
        // let's keep it simple eh?
        //final String json = "{\"message_type\":\"recognizer_loop:utterance\", \"context\": null, \"metadata\": {\"utterances\": [\"" + msg + "\"]}}";
        final String json = "{\"data\": {\"utterances\": [\"" + msg + "\"]}, \"type\": \"recognizer_loop:utterance\", \"context\": null}";

        try {
            if (mWebSocketClient == null || mWebSocketClient.getConnection().isClosed()) {
                // try and reconnect
                if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.NETWORK_STATUS_WIFI) { //TODO: add config to specify wifi only.
                    connectWebSocket();
                }
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // Actions to do after 1 seconds
                    try {
                        mWebSocketClient.send(json);
                    } catch (WebsocketNotConnectedException exception) {
                        showToast(getResources().getString(R.string.websocket_closed));
                    }
                }
            }, 1000);

        } catch (WebsocketNotConnectedException exception) {
            showToast(getResources().getString(R.string.websocket_closed));
        }
    }

    private void showToast(String message) {
        GuiUtilities.showToast(getApplicationContext(), message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void registerNetworkReceiver(){
        if(!isNetworkChangeReceiverRegistered) {
            // set up the dynamic broadcast receiver for maintaining the socket
            networkChangeReceiver = new NetworkChangeReceiver();
            networkChangeReceiver.setMainActivityHandler(this);

            // set up the intent filters
            IntentFilter connChange = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            IntentFilter wifiChange = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
            registerReceiver(networkChangeReceiver, connChange);
            registerReceiver(networkChangeReceiver, wifiChange);

            isNetworkChangeReceiverRegistered = true;
        }
    }

    private void unregisterReceivers() {
        unregisterBroadcastReceiver(networkChangeReceiver);

        isNetworkChangeReceiverRegistered = false;
    }

    private void unregisterBroadcastReceiver(BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isNetworkChangeReceiverRegistered = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadPreferences();
        recordVersionInfo();
        locationPermissionCheckAndSet();
        registerReceivers();
    }

    /**
     * For caching location permission state.
     */
    private void locationPermissionCheckAndSet() {
        try {
            SharedPreferences.Editor editor = sharedPref.edit();

            String valueToSet;

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                valueToSet = "Set";

            } else {
                valueToSet = "Not Set";
            }
            editor.putString(LOCATION_PERMISSION_PREFERENCE_KEY, valueToSet);
            editor.apply();
        } catch (Exception ex){
            Log.d(TAG, ex.getMessage());
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterReceivers();

        if (launchedFromWidget) {
            autoPromptForSpeech = true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void loadPreferences(){
        //Load beacon prefs.
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // get the location service ip
        wsip = sharedPref.getString("ip", "http://localhost:9319");
        if (wsip.isEmpty()) {
            // eep, show the settings intent!
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (mWebSocketClient == null || mWebSocketClient.getConnection().isClosed()) {
            connectWebSocket();
        }
    }

    private void recordVersionInfo() {
        String versionName = "";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(VERSION_CODE_PREFERENCE_KEY, versionCode);
            editor.putString(VERSION_NAME_PREFERENCE_KEY, versionName);
            editor.apply();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.location_result_title);
                    builder.setMessage(R.string.location_result_message);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

}
