package com.dotsoft.smartsonia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Name: Smart Sonia - WearOS Application
 * Author: Evangelos Kiosis
 * Tested and developed for : Samsung Galaxy Watch 4 Active
 * -----------------------
 * Notes: Measures not in background. App keeps the screen always on. Like Youtube. It's measures all the time like this.
 * For better battery performance , app supports ambient mode.
 * Back button is inactive when the measures are running.
 * Swipe to dismiss is also inactive.
 * User can close the app only by pressing the home button or back button (when the measures are not running).
 */

public class MainActivity extends FragmentActivity implements AmbientModeSupport.AmbientCallbackProvider, BeaconConsumer {

    /**
     * DEBUG NOTE: connect wearable to COMITECH wifi
     * 1. cd platform-tools
     * 2. adb connect 192.168.1.XX:5555 (IP from wearable - must being checked before every connection)
     */

    // settings
    private final int LOCATION_REFRESH_MILL = 1000 * 60 * 10; // 10 min
    private final int BEACONS_BETWEEN_SCAN_PERIOD_MILL = 1000 * 60 * 15; //15 min
    private final int BEACONS_SCAN_PERIOD_MILL = 1000 * 10; // 10 sec
    private final int SOS_TICKS = 3; // 3 times the back button
    private final int RECEIVE_ALERTS_DB_EVERY_MILL = 1000 * 60; // 1 min
    private final int SEND_DATA_EVERY_MILL = 1000 * 5; // 5 sec
    private final double RED_ZONE_AREA = 2.00; // 2 meters

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private List<Integer> areaMajorsIDs;
    private static final String FILE_NAME = "data.txt";
    private static final String TAG = "MainActivity";
    private static final String FILE_NAME_USER = "user_name.txt";
    private boolean isHeartBeatAccessible = false;
    private boolean isPressureAccessible = false;
    private boolean shouldBlockBack = false;
    private boolean isWakeLockAcquire = false;
    private boolean isUserLogin = false;
    private Sensor mHeartSensor, mPressureSensor;
    private SensorManager mSensorManager;
    private ScrollView scrollView;
    private ImageView wifiIcon, locationIcon, bluetoothIcon, icon_heartRate, icon_pressure, icon_battery, icon_location_cancel,
            icon_location_yes, icon_confirm_cancel, icon_confirm_yes,icon_bluetooth_cancel, icon_bluetooth_yes, dismissErrorDialog;
    private TextView value_heartRate, value_pressure, value_battery, actionButton, welcomeText, permissionText,
            logoutButton, loginButton, errorMessage, cancelButton, counterText, confirmText, notificationBubble,
            notificationTitle,notificationText, errorDialogText;
    private EditText usernameInput, passwordInput;
    private RelativeLayout loginLayout, measureLayout, sosLayout, locationDialog, confirmDialog,bluetoothDialog, notificationDialog,loadingLayout,errorDialog;
    private LinearLayout sensorsLayout, environmentLayout;
    private SimpleDateFormat sdf;
    private int step = 0;
    private int batLevel;
    private int sosButtonCount = 0;
    private long lastTimeSosButtonPressed = 0;
    private int allowedTimeBetweenClick = 1000;
    private String timestampValue;
    private String heartRateValue = "none";
    private String userName = "none";
    private String password = "none";
    private String pressureValue = "0 hPa";
    private String pressureZeroValue = "0";
    private String beaconIdValue = "none";
    private String beaconDistanceValue = "none";
    private float pressureZero = -10000;
    private final String defaultHeartRate = "0 bpm";
    private String locationValue = "0,0";
    private JSONObject dataJson,cacheDataJson;
    private JSONArray measurementsArray,cacheMeasurementsArray;
    private final Handler handler = new Handler();
    private final Handler handler1 = new Handler();
    private int lastMinute = -100;
    private final Handler notificationHandler = new Handler();
    private final int notificationRepeater = 2000;
    private Runnable r, r1, notificationRunnable;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private BatteryManager bm;
    public FusedLocationProviderClient fusedLocationClient;
    public LocationCallback locationCallback;
    public LocationRequest locationRequest;
    private Animation heartBeatAnimation, countAnimation, notificationAnimation;
    private ProgressBar progressBarSOS;
    private String fileRootPath;
    private boolean isFirstTime = true;
    // Beacons
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private BeaconManager beaconManager;
    private boolean isBluetoothEnable = false;
    private boolean cacheEnable = false;
    private ArrayList<BeaconModel> currentBeaconList = new ArrayList<>();
    private boolean unreadNotification = false;
    private String displayedNotification = "none";
    private boolean appInAmbient = false;
    private BeaconModel beaconModel;
    private User user;
    private ArrayList<String> beaconAlertsCache = new ArrayList<>();
    private ArrayList<NotificationObject> unreadNotifications = new ArrayList<>();
    private ArrayList<NotificationObject> alertList = new ArrayList<>();

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AmbientModeSupport.AmbientController ambientController = AmbientModeSupport.attach(this);
        fileRootPath = getApplicationContext().getExternalFilesDir(null).toString();

        //set date
        sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        //set battery manager
        bm = (BatteryManager) getApplicationContext().getSystemService(BATTERY_SERVICE);

        value_heartRate = (TextView) findViewById(R.id.value_HEART_RATE);
        icon_heartRate = (ImageView) findViewById(R.id.icon_HEART_RATE);
        value_pressure = (TextView) findViewById(R.id.value_PRESSURE);
        icon_pressure = (ImageView) findViewById(R.id.icon_PRESSURE);
        value_battery = (TextView) findViewById(R.id.value_BATTERY);
        icon_battery = (ImageView) findViewById(R.id.icon_BATTERY);
        actionButton = (TextView) findViewById(R.id.action_button);
        logoutButton = (TextView) findViewById(R.id.logout_button);
        loginButton = (TextView) findViewById(R.id.login_button);
        welcomeText = (TextView) findViewById(R.id.welcome_text);
        wifiIcon = (ImageView) findViewById(R.id.icon_wifi);
        locationIcon = (ImageView) findViewById(R.id.icon_location);
        permissionText = (TextView) findViewById(R.id.permission_text);
        sensorsLayout = (LinearLayout) findViewById(R.id.sensors_section);
        usernameInput = (EditText) findViewById(R.id.username_input);
        passwordInput = (EditText) findViewById(R.id.password_input);
        loginLayout = (RelativeLayout) findViewById(R.id.login_layout);
        measureLayout = (RelativeLayout) findViewById(R.id.measure_layout);
        errorMessage = (TextView) findViewById(R.id.error_message);
        scrollView = (ScrollView) findViewById(R.id.scrollview);
        sosLayout = (RelativeLayout) findViewById(R.id.sos_section);
        cancelButton = (TextView) findViewById(R.id.cancel_button);
        counterText = (TextView) findViewById(R.id.counter);
        passwordInput.setTypeface(usernameInput.getTypeface());
        locationDialog = (RelativeLayout) findViewById(R.id.location_dialog);
        icon_location_cancel = (ImageView) findViewById(R.id.location_dialog_cancel);
        icon_location_yes = (ImageView) findViewById(R.id.location_dialog_yes);
        heartBeatAnimation = AnimationUtils.loadAnimation(this, R.anim.beat);
        countAnimation = AnimationUtils.loadAnimation(this, R.anim.count);
        notificationAnimation = AnimationUtils.loadAnimation(this, R.anim.beat_notification);
        environmentLayout = (LinearLayout) findViewById(R.id.linear_environment);
        confirmDialog = (RelativeLayout) findViewById(R.id.confirm_dialog);
        icon_confirm_cancel = (ImageView) findViewById(R.id.confirm_dialog_cancel);
        icon_confirm_yes = (ImageView) findViewById(R.id.confirm_dialog_yes);
        confirmText = (TextView) findViewById(R.id.confirm_text_dialog);
        progressBarSOS = (ProgressBar) findViewById(R.id.progress_bar_sos);
        bluetoothDialog = (RelativeLayout) findViewById(R.id.bluetooth_dialog);
        icon_bluetooth_cancel = (ImageView) findViewById(R.id.bluetooth_dialog_cancel);
        icon_bluetooth_yes = (ImageView) findViewById(R.id.bluetooth_dialog_yes);
        bluetoothIcon = (ImageView) findViewById(R.id.icon_bluetooth);
        notificationBubble = (TextView) findViewById(R.id.notification_bubble);
        notificationDialog = (RelativeLayout) findViewById(R.id.notification_dialog);
        notificationTitle = (TextView) findViewById(R.id.notification_title);
        notificationText = (TextView) findViewById(R.id.notification_text);
        loadingLayout = (RelativeLayout) findViewById(R.id.loading_layout);
        errorDialog = (RelativeLayout) findViewById(R.id.error_dialog);
        dismissErrorDialog = (ImageView) findViewById(R.id.error_dialog_cancel);
        errorDialogText = (TextView) findViewById(R.id.error_text_dialog);

        if (fileExists(this, FILE_NAME_USER)) {
            String userRaw = getUserName(FILE_NAME_USER);
            if (!userRaw.equals("none")) {
                user = new Gson().fromJson(userRaw, User.class);
                userName = user.getUserName();
                if (!user.isLogin()) {
                    isUserLogin = false;
                    measureLayout.setVisibility(View.GONE);
                    loginLayout.setVisibility(View.VISIBLE);
                } else {
                    userName = user.getUserName();
                    //set "Hello user" text
                    isUserLogin = true;
                    String finalWelcomeText = getString(R.string.hello) + " " + userName;
                    welcomeText.setText(finalWelcomeText);
                    measureLayout.setVisibility(View.VISIBLE);
                    loginLayout.setVisibility(View.GONE);
                }
            }else {
                isUserLogin = false;
                loginLayout.setVisibility(View.VISIBLE);
                measureLayout.setVisibility(View.GONE);
            }
        } else {
            isUserLogin = false;
            loginLayout.setVisibility(View.VISIBLE);
            measureLayout.setVisibility(View.GONE);
        }

        //set wakelock
        powerManager = (PowerManager) this.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag");

        //check internet connection and set wifi icon
        if (checkNetwork()) {
            wifiIcon.setImageResource(R.drawable.ic_wifi);
            cacheEnable = false;
        }else {
            wifiIcon.setImageResource(R.drawable.ic_wifi_off);
            cacheEnable = true;
        }

        value_heartRate.setText(defaultHeartRate);
        value_pressure.setText("0 hPa");
        value_battery.setText("N/A");

        /**
         *  set repeated process, every 5 sec. Get measures and send them on database (if wifi on), or store them to cache (not bigger than 240 entries)
         */
        r = new Runnable() {
            public void run() {
                //get date value
                Date now = new Date();
                timestampValue = sdf.format(now);
                batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                String batteryValue = batLevel + "%";
                value_battery.setText(batteryValue);
                String[] spitTimestamp = timestampValue.split(" ",2);
                String date = spitTimestamp[0];
                //update-add json object
                JSONObject currentMeasurement = new JSONObject();
                try {
                    currentMeasurement.put("Timestamp", timestampValue);
                    currentMeasurement.put("HeartRate", heartRateValue);
                    currentMeasurement.put("Pressure", pressureValue);
                    currentMeasurement.put("pressurezero", pressureZeroValue);
                    currentMeasurement.put("Location", locationValue);
                    currentMeasurement.put("Beacon_id",beaconIdValue);
                    currentMeasurement.put("Beacon_distance",beaconDistanceValue);
                    currentMeasurement.put("date_split_worker",date);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (measurementsArray.length() >= 1) {
                    measurementsArray.remove(0);
                }
                measurementsArray.put(currentMeasurement);

                if (isFirstTime) {
                    //saveDataToOfflineFile(currentMeasurement.toString() + "\n");
                    isFirstTime = false;
                }
                //saveDataToOfflineFile("," + currentMeasurement.toString() + "\n");
                //check internet connection and set wifi icon

                if (!checkNetwork()) {
                    wifiIcon.setImageResource(R.drawable.ic_wifi_off);
                    if(!cacheEnable){
                        cacheDataJson = new JSONObject();
                        cacheMeasurementsArray = new JSONArray();
                        try {
                            cacheDataJson.put("user", userName);
                            cacheDataJson.put("measurements", cacheMeasurementsArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        cacheEnable = true;
                    }else{
                        if (cacheMeasurementsArray.length() >= 240) {
                            cacheMeasurementsArray.remove(0);
                        }
                        cacheMeasurementsArray.put(currentMeasurement);
                    }
                } else {
                    updateUserData();
                    wifiIcon.setImageResource(R.drawable.ic_wifi);
                }

               // Log.e("JSON CACHED DATA", cacheDataJson.toString());
                //Log.e("JSON DATA", dataJson.toString());

                handler.postDelayed(this, SEND_DATA_EVERY_MILL);
            }
        };

        /**
         *  set repeated process, every 1 min. Get alerts from database
         */
        r1 = new Runnable() {
            public void run() {
                getMessagesList();
                handler1.postDelayed(this, RECEIVE_ALERTS_DB_EVERY_MILL);
            }
        };

        actionButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View view) {
                if (actionButton.getText().equals(getString(R.string.start))) {
                    //check if the app has grant the permissions to enable sensors measurements
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            permissionText.setVisibility(View.VISIBLE);
                            sensorsLayout.setVisibility(View.GONE);
                        } else {
                            getBeaconList();
                        }
                    } else {
                        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            permissionText.setVisibility(View.VISIBLE);
                            sensorsLayout.setVisibility(View.GONE);
                        } else {
                            getBeaconList();
                        }
                    }

                } else {
                    measureLayout.setVisibility(View.GONE);
                    showConfirmDialog("STOP_DATA");
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                measureLayout.setVisibility(View.GONE);
                showConfirmDialog("LOGOUT");
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //do login
                if (usernameInput.getText().toString().equals("")) {
                    errorMessage.setText(getString(R.string.empty_field_username));
                    errorMessage.setVisibility(View.VISIBLE);
                } else if (passwordInput.getText().toString().equals("")) {
                    errorMessage.setText(getString(R.string.empty_field_password));
                    errorMessage.setVisibility(View.VISIBLE);
                } else {
                    errorMessage.setVisibility(View.GONE);
                    userName = usernameInput.getText().toString();
                    password = passwordInput.getText().toString();
                    if(checkNetwork()){
                        loadingLayout.setVisibility(View.VISIBLE);
                        loginUser(userName,password);
                    }else {
                        errorMessage.setText(getString(R.string.no_network));
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        icon_location_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationDialog.setVisibility(View.GONE);
                if(isBluetoothEnable){
                    measureLayout.setVisibility(View.VISIBLE);
                }
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        icon_location_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationDialog.setVisibility(View.GONE);
                measureLayout.setVisibility(View.VISIBLE);
                locationIcon.setImageResource(R.drawable.ic_location_off);
            }
        });

        icon_confirm_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDialog.setVisibility(View.GONE);
                measureLayout.setVisibility(View.VISIBLE);
            }
        });

        icon_bluetooth_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothDialog.setVisibility(View.GONE);
                measureLayout.setVisibility(View.VISIBLE);
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }
        });

        icon_bluetooth_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothDialog.setVisibility(View.GONE);
                measureLayout.setVisibility(View.VISIBLE);
                bluetoothIcon.setImageResource(R.drawable.ic_bluetooth_off);
            }
        });

        dismissErrorDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errorDialog.setVisibility(View.GONE);
            }
        });

        checkPermissions();
        checkSensorAvailability();

    }

    private void unlockScreen() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void loginUser(String username, String password){
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String registerUrl = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/login/?username="+username+"&password="+password+"&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq&scope=public,core,posts,taxonomies,comments,profiles,publish_posts,publish_comments,edit_profile,manage_posts";
        Request request = new Request.Builder()
                .url(registerUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                showError();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();

                if (response.isSuccessful()){
                    //Log.e("RESPONSE - onResponse", "register call isSuccessful" + myResponse);
                    CmsUserObject.Root rootResponse = new Gson().fromJson(myResponse, CmsUserObject.Root.class);
                    if(rootResponse.respond.equals("1")){
                        JsonObject jsonUser = new JsonObject();
                        jsonUser.addProperty("userName",rootResponse.result.get(0).nickname);
                        jsonUser.addProperty("login","IN");
                        jsonUser.addProperty("userId",rootResponse.result.get(0).ID);
                        jsonUser.addProperty("accessToken",rootResponse.result.get(0).Access_Token);
                        String userString = jsonUser.toString();
                        saveUserName(userString);
                        //set "Hello user" text
                        user = new Gson().fromJson(userString, User.class);
                        String finalWelcomeText = getString(R.string.hello) + " " + user.getUserName();
                        welcomeText.setText(finalWelcomeText);
                        usernameInput.setText("");
                        passwordInput.setText("");
                        isUserLogin = true;
                        getWorkerInfo(rootResponse.result.get(0).ID,rootResponse.result.get(0).nickname);
                    }else{
                        hideLoading();
                        showUserNotFound();
                    }
                }else{
                    hideLoading();
                    showError();
                }
            }
        });
    }

    private void checkInUser(String userID, String oldInfo){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        Date now = new Date();
        String time = sdf.format(now);
        String smartwatch = "";
        String[] revision = oldInfo.split("\\|");
        if(revision.length>0){
            String[] value = revision[0].split(",");
            if (value.length == 3) {
                smartwatch = value[1];
            }
        }

        String newInfo = "1," + smartwatch + "," + time + "|" + oldInfo ;
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String Url = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/custom_service/?service=update_meta&my_meta_value="+newInfo+"&my_user_id="+userID+
                "&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq";

        Request request = new Request.Builder()
                .url(Url)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                // logout user
                userName = "none";
                JsonObject jsonUser = new JsonObject();
                jsonUser.addProperty("userName", "null");
                jsonUser.addProperty("userId", "null");
                jsonUser.addProperty("accessToken", "null");
                jsonUser.addProperty("login", "OUT");
                String logsString = jsonUser.toString();
                saveUserName(logsString);
                isUserLogin = false;
                showError();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    hideLoading();
                    loginProcess();
                } else {
                    hideLoading();
                    // logout user
                    userName = "none";
                    JsonObject jsonUser = new JsonObject();
                    jsonUser.addProperty("userName", "null");
                    jsonUser.addProperty("userId", "null");
                    jsonUser.addProperty("accessToken", "null");
                    jsonUser.addProperty("login", "OUT");
                    String logsString = jsonUser.toString();
                    saveUserName(logsString);
                    isUserLogin = false;
                    showError();
                }
            }
        });
    }

    private void checkOutUser(String userID, String oldInfo){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        Date now = new Date();
        String time = sdf.format(now);
        String smartwatch = "";
        String[] revision = oldInfo.split("\\|");
        if(revision.length>0){
            String[] value = revision[0].split(",");
            if (value.length == 3) {
                smartwatch = value[1];
            }
        }

        String newInfo = "0," + smartwatch + "," + time + "|" + oldInfo ;
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String Url = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/custom_service/?service=update_meta&my_meta_value="+newInfo+"&my_user_id="+userID+
                "&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq";

        Request request = new Request.Builder()
                .url(Url)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                showMeasure();
                showErrorDialog(getString(R.string.error_dialog_general));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    logoutProcess();
                    hideLoading();
                } else {
                    hideLoading();
                    showMeasure();
                    showErrorDialog(getString(R.string.error_dialog_general));
                }
            }
        });
    }

    public void getWorkerInfo(String userID,String userN){
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String registerUrl = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/authors/?role=subscriber&perpage=100000&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq" +
                "&custom_search_or={\"user_login\":\"=\'"+userN+"\'\",\"user_nicename\":\"=\'"+userN+"\'\"}";
        //Log.e(TAG,registerUrl);
        Request request = new Request.Builder()
                .url(registerUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                // logout user
                userName = "none";
                JsonObject jsonUser = new JsonObject();
                jsonUser.addProperty("userName", "null");
                jsonUser.addProperty("userId", "null");
                jsonUser.addProperty("accessToken", "null");
                jsonUser.addProperty("login", "OUT");
                String logsString = jsonUser.toString();
                saveUserName(logsString);
                isUserLogin = false;
                showError();

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                if (response.isSuccessful()){
                        //Log.e("RESPONSE - onResponse", "register call isSuccessful" + myResponse);
                        UserCMS.Root userResponse = new Gson().fromJson(myResponse, UserCMS.Root.class);
                        if(userResponse.respond.equals("1")) {
                            for (int i = 0; i < userResponse.result.size(); i++) {
                                if (userResponse.result.get(i).custom_fields.user_role.equals("WORKER")) {
                                    // get user raw format history info
                                    String info = userResponse.result.get(i).custom_fields.user_info;

                                    //Build user history info
                                    String active,smartwatch,timestamp;
                                    if(info==null){
                                        info = ",,";
                                    }
                                    checkInUser(userID,info);
                                }else{
                                    hideLoading();
                                    // logout user
                                    userName = "none";
                                    JsonObject jsonUser = new JsonObject();
                                    jsonUser.addProperty("userName", "null");
                                    jsonUser.addProperty("userId", "null");
                                    jsonUser.addProperty("accessToken", "null");
                                    jsonUser.addProperty("login", "OUT");
                                    String logsString = jsonUser.toString();
                                    saveUserName(logsString);
                                    isUserLogin = false;
                                    showError();
                                }
                            }
                        }else{
                            hideLoading();
                            // logout user
                            userName = "none";
                            JsonObject jsonUser = new JsonObject();
                            jsonUser.addProperty("userName", "null");
                            jsonUser.addProperty("userId", "null");
                            jsonUser.addProperty("accessToken", "null");
                            jsonUser.addProperty("login", "OUT");
                            String logsString = jsonUser.toString();
                            saveUserName(logsString);
                            isUserLogin = false;
                            showError();
                        }
                }else{
                    hideLoading();
                    // logout user
                    userName = "none";
                    JsonObject jsonUser = new JsonObject();
                    jsonUser.addProperty("userName", "null");
                    jsonUser.addProperty("userId", "null");
                    jsonUser.addProperty("accessToken", "null");
                    jsonUser.addProperty("login", "OUT");
                    String logsString = jsonUser.toString();
                    saveUserName(logsString);
                    isUserLogin = false;
                    showError();
                }
            }
        });
    }

    public void getWorkerInfoLogout(String userID,String userN){
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String registerUrl = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/authors/?role=subscriber&perpage=100000&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq" +
                "&custom_search_or={\"user_login\":\"=\'"+userN+"\'\",\"user_nicename\":\"=\'"+userN+"\'\"}";
        //Log.e(TAG,registerUrl);
        Request request = new Request.Builder()
                .url(registerUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                showMeasure();
                showErrorDialog(getString(R.string.error_dialog_general));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                if (response.isSuccessful()){
                    Log.e("RESPONSE - onResponse", "register call isSuccessful" + myResponse);
                    UserCMS.Root userResponse = new Gson().fromJson(myResponse, UserCMS.Root.class);
                    if(userResponse.respond.equals("1")) {
                        for (int i = 0; i < userResponse.result.size(); i++) {
                                // get user raw format history info
                                String info = userResponse.result.get(i).custom_fields.user_info;

                                if(info==null){
                                    info = ",,";
                                }
                                checkOutUser(userID,info);
                        }
                    }else{
                        hideLoading();
                        showMeasure();
                        showErrorDialog(getString(R.string.error_dialog_general));
                    }
                }else{
                    hideLoading();
                    showMeasure();
                    showErrorDialog(getString(R.string.error_dialog_general));
                }
            }
        });
    }

    private void hideLoading(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        loadingLayout.setVisibility(View.GONE);
                    }
                }, 100);
    }

    private void showError(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        errorMessage.setText(getString(R.string.error));
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                }, 100);
    }

    private void showMeasure(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        measureLayout.setVisibility(View.VISIBLE);
                    }
                }, 100);
    }

    private void showErrorDialog(String text){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        errorDialogText.setText(text);
                        errorDialog.setVisibility(View.VISIBLE);
                    }
                }, 100);
    }

    private void showUserNotFound(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        errorMessage.setText(getString(R.string.invalid_user));
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                }, 100);

    }

    private void loginProcess(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        scrollView.scrollTo(0, 0);
                        loginLayout.setVisibility(View.GONE);
                        measureLayout.setVisibility(View.VISIBLE);
                    }
                }, 100);
    }

    private void logoutProcess(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        userName = "none";
                        JsonObject jsonUser = new JsonObject();
                        jsonUser.addProperty("userName", "null");
                        jsonUser.addProperty("userId", "null");
                        jsonUser.addProperty("accessToken", "null");
                        jsonUser.addProperty("login", "OUT");
                        String logsString = jsonUser.toString();
                        saveUserName(logsString);
                        isUserLogin = false;
                        scrollView.scrollTo(0, 0);
                        measureLayout.setVisibility(View.GONE);
                        loginLayout.setVisibility(View.VISIBLE);
                    }
                }, 100);

    }

    private void getMessagesList(){
        alertList = new ArrayList<>();
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String registerUrl = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/getposts/?orderby=date&order=desc&perpage=100000&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq&custom_post=messages";
        Request request = new Request.Builder()
                .url(registerUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                Log.e("getMessagesList()","ERROR FAILED");
                getRecommendationsList();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                hideLoading();

                if (response.isSuccessful()){
                    CmsMessagesObject.Root messagesResponse = new Gson().fromJson(myResponse, CmsMessagesObject.Root.class);
                    if(messagesResponse.respond.equals("1")){
                        for(int i=0;i<messagesResponse.result.size();i++){
                            if(messagesResponse.result.get(i).custom_fields.worker_name.equals(user.getUserName()) &&
                                    messagesResponse.result.get(i).custom_fields.read.equals("1")){
                                if(!idExitInNotificationsList(messagesResponse.result.get(i).ID)){
                                    if(!idExitInUnreadNotificationsList(messagesResponse.result.get(i).ID)){
                                        alertList.add(new NotificationObject(messagesResponse.result.get(i).post_title,
                                                messagesResponse.result.get(i).custom_fields.notes,
                                                messagesResponse.result.get(i).ID));
                                    }
                                }
                            }
                        }
                        for(int i=0;i<alertList.size();i++){
                            newNotificationProcessHandel(false,alertList.get(i).getId(),alertList.get(i).getTitle(), alertList.get(i).getBody(),"NONE");
                        }
                    }else{
                        Log.e("getMessagesList()","ERROR RESPONSE 0 ");
                    }
                }else{
                    Log.e("getMessagesList()","ERROR NOT SUCCESSFUL");
                }
                getRecommendationsList();
            }
        });
    }

    private void getRecommendationsList(){

        alertList = new ArrayList<>();
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String registerUrl = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/getposts/?orderby=date&order=desc&perpage=100000&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq&custom_post=recommendations";
        Request request = new Request.Builder()
                .url(registerUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                Log.e("getMessagesList()","ERROR FAILED");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                hideLoading();

                if (response.isSuccessful()){
                    CmsRecommendationObject.Root recommendationResponse = new Gson().fromJson(myResponse, CmsRecommendationObject.Root.class);
                    if(recommendationResponse.respond.equals("1")){
                        for(int i=0;i<recommendationResponse.result.size();i++){
                            if(recommendationResponse.result.get(i).postmeta.worker_name.equals(user.getUserName()) &&
                                    recommendationResponse.result.get(i).postmeta.read.equals("1")){
                                if(!idExitInNotificationsList(recommendationResponse.result.get(i).ID)){
                                    if(!idExitInUnreadNotificationsList(recommendationResponse.result.get(i).ID)){
                                        alertList.add(new NotificationObject(recommendationResponse.result.get(i).post_title,
                                                "",
                                                recommendationResponse.result.get(i).ID));
                                    }
                                }
                            }
                        }
                        for(int i=0;i<alertList.size();i++){
                            newNotificationProcessHandel(false,alertList.get(i).getId(),alertList.get(i).getTitle(), alertList.get(i).getBody(),"NONE");
                        }
                    }else{
                        Log.e("getMessagesList()","ERROR RESPONSE 0 ");
                    }
                }else{
                    Log.e("getMessagesList()","ERROR NOT SUCCESSFUL");
                }
            }
        });
    }

    private boolean idExitInNotificationsList(String id){
        for(int i=0;i<alertList.size();i++){
            if(alertList.get(i).getId().equals(id)){
                return true;
            }
        }
        return false;
    }

    private boolean idExitInUnreadNotificationsList(String id){
        for(int i=0;i<unreadNotifications.size();i++){
            if(unreadNotifications.get(i).getId().equals(id)){
                return true;
            }
        }
        return false;
    }


    private void newNotificationProcessHandel(boolean beacon,String id, String t, String b,String flag) {
        //send alert to db
        if(beacon){
            sendBeaconAlert(flag);
        }
        if(!flag.equals("BEACONSAFE101")){
            unreadNotifications.add(new NotificationObject(t, b,id));
            newNotificationProcess(t, b,id);
        }

    }

    private void newNotificationProcess(String t, String b, String id){
        // remove vibration if a notification is already in process
        notificationHandler.removeCallbacks(notificationRunnable);
        notificationTitle.setText(unreadNotifications.get(0).getTitle());
        notificationText.setText(unreadNotifications.get(0).getBody());
        unreadNotification = true;
        vibrateWatch(1000);
        displayedNotification = id;
        if (appInAmbient) {
            showNotificationBubble();
            notificationBubble.startAnimation(notificationAnimation);
        } else {
            showNotification();
        }


        /**
         *  set repeated process for notification, every 1 sec. Vibrate and animation (if in ambient)
         */
        notificationRunnable = new Runnable() {
            public void run() {
                vibrateWatch(1000);
                if(appInAmbient){
                    notificationBubble.startAnimation(notificationAnimation);
                }
                notificationHandler.postDelayed(this, notificationRepeater);
            }
        };
        //start repeated process (vibration and animation)
        notificationHandler.postDelayed(notificationRunnable, notificationRepeater);
    }

    private void sendBeaconAlert(String alertNote){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf2 = new SimpleDateFormat("yy-MM-dd");
        Date now = new Date();
        String date = sdf2.format(now);
        //get time
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        String h = hour+"";
        String min = minute+"";
        if(hour<10){
            h = "0"+hour;
        }
        if(minute<10){
            min = "0"+minute;
        }
        String time = h +":"+min+":00";
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String endpoint = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/newpost/?subject="+user.getUserName().toUpperCase()+" IN RED AREA&custom_field={\"risk_grading\":\"HIGH\",\"worker_Name\":\""+user.getUserName()+"\",\"date_split_alert\":\""+date+"\",\"read\":\"0\",\"timestamp\":\""+time+"\"}" +
                "&custom_post=alerts&post_status=publish&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq"+"&ACCESS_TOKEN=" +user.getAccessToken();
        if(alertNote.equals("BEACONSAFE101")){
            endpoint = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/newpost/?subject="+user.getUserName().toUpperCase()+" IN SAFE AREA&custom_field={\"risk_grading\":\"VERY LOW\",\"worker_Name\":\""+user.getUserName()+"\",\"date_split_alert\":\""+date+"\",\"read\":\"0\",\"timestamp\":\""+time+"\"}" +
                    "&custom_post=alerts&post_status=publish&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq"+"&ACCESS_TOKEN=" +user.getAccessToken();
        }

        //Log.e("sendBeaconAlert()",endpoint);
        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                // if alert can not been send, save it and retry on the next "send data" process.
                beaconAlertsCache.add(alertNote);
                Log.e("sendBeaconAlert()","BEACON ALERT FAILED");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.e("sendBeaconAlert()","BEACON ALERT SEND SUCCESSFUL");
                } else {
                    // if alert can not been send, save it and retry on the next "send data" process.
                    beaconAlertsCache.add(alertNote);
                    Log.e("sendBeaconAlert()","BEACON ALERT NOT SEND SUCCESSFUL");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.e("OnDestroy()", "App on Destroy");
        super.onDestroy();
        if (beaconManager != null) {
            stopBeaconSearch();
        }
        handler.removeCallbacks(r);
        notificationHandler.removeCallbacks(notificationRunnable);
        mSensorManager.unregisterListener(mSensorEventListener);

        if (isWakeLockAcquire) {
            wakeLock.release();
            isWakeLockAcquire = false;
        }

        if (fusedLocationClient != null) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        checkBackgroundLocationPermissions();
    }

    @Override
    public void onBackPressed() {

        if (!shouldBlockBack) {
            super.onBackPressed();
        } else {
            if(notificationDialog.getVisibility()==View.VISIBLE){
                if(!unreadNotifications.get(0).getId().equals("none")){
                    updateAlertByIdRead(unreadNotifications.get(0).getId());
                }
                unreadNotifications.remove(0);
                if(unreadNotifications.size()==0){
                    unreadNotification = false;
                    notificationHandler.removeCallbacks(notificationRunnable);
                    hideNotification();
                }else {
                    notificationHandler.removeCallbacks(notificationRunnable);
                    newNotificationProcess(unreadNotifications.get(0).getTitle(),unreadNotifications.get(0).getBody(),unreadNotifications.get(0).getId());
                }
            }
            Date date = new Date();
            if (sosButtonCount == 0) {
                lastTimeSosButtonPressed = date.getTime();
                sosButtonCount = 1;
            } else {
                if (date.getTime() - lastTimeSosButtonPressed < allowedTimeBetweenClick) {
                    lastTimeSosButtonPressed = date.getTime();
                    sosButtonCount++;
                    if (sosButtonCount == SOS_TICKS) {
                        sosAction();
                    }
                } else {
                    sosButtonCount = 1;
                    lastTimeSosButtonPressed = date.getTime();
                }
            }
        }
    }
    private void updateAlertByIdRead(String id){
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String sendURL = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/updatepost/?id="+id+"&post_status=publish&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq" +
                "&ACCESS_TOKEN=" +user.getAccessToken()+"&custom_field={\"read\":\"0\"}&postmeta={\"read\":\"0\"}";
        Request request = new Request.Builder()
                .url(sendURL)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                Log.e(" updateAlertByIdRead","ERROR FAILED");

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                Log.e("",response.body().string());
                if (response.isSuccessful()) {
                    Log.e(" updateAlertByIdRead","successful : ");

                } else {
                    Log.e(" updateAlertByIdRead","ERROR not successful");
                }
            }
        });
    }


    public boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if (file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    public String getUserName(String name) {
        FileInputStream fis = null;
        String text = "none";
        try {
            fis = openFileInput(name);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            text = br.readLine();
            return text.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "none";
    }

    public void saveUserName(String userNameString) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(FILE_NAME_USER, Context.MODE_PRIVATE));
            outputStreamWriter.write(userNameString);
            outputStreamWriter.close();
            Log.i("USER NAME", "WRITE DONE");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showConfirmDialog(String action) {
        confirmDialog.setVisibility(View.VISIBLE);

        if (action.equals("LOGOUT")) {
            confirmText.setText(getString(R.string.confirm_logout));
            icon_confirm_yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmDialog.setVisibility(View.GONE);
                    loadingLayout.setVisibility(View.VISIBLE);
                    // if there is a data measurement , stop it
                    if (actionButton.getText().equals(getString(R.string.stop))) {
                        stopDataMeasurement();
                    }
                    // logout user
                    // getUserInfo
                    String userRaw = getUserName(FILE_NAME_USER);
                    if (!userRaw.equals("none")) {
                        user = new Gson().fromJson(userRaw, User.class);
                        getWorkerInfoLogout(user.getUserId(),user.getUserName());
                    }
                }
            });
        }

        if (action.equals("STOP_DATA")) {
            confirmText.setText(getString(R.string.confirm_stop));
            icon_confirm_yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmDialog.setVisibility(View.GONE);
                    measureLayout.setVisibility(View.VISIBLE);
                    stopDataMeasurement();
                }
            });
        }

    }

    private void sosAction() {
        scrollView.setVisibility(View.GONE);
        sosLayout.setVisibility(View.VISIBLE);
        final int[] i = {0};

        progressBarSOS.setProgress(i[0]);
        CountDownTimer mCountDownTimer = new CountDownTimer(5000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.v("Log_tag", "Tick of Progress" + i[0] + millisUntilFinished);
                i[0]++;
                vibrateWatch(500);
                progressBarSOS.setProgress((int) i[0] * 100 / (5000 / 1000), true);
                int num = 6 - i[0];
                String numStr = String.valueOf(num);
                counterText.setText(numStr);
                counterText.startAnimation(countAnimation);
            }

            @Override
            public void onFinish() {
                i[0]++;
                progressBarSOS.setProgress(100);
                counterText.clearAnimation();
                sosLayout.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                sosButtonCount = 0;
                lastTimeSosButtonPressed = 0;
                sendAnSos();
            }
        };
        mCountDownTimer.start();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCountDownTimer.cancel();
                sosLayout.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void sendAnSos(){
        loadingLayout.setVisibility(View.VISIBLE);
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String endpoint = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/newpost/?subject=SOS "+user.getUserName()+"&custom_field={\"Timestamp\":\""+timestampValue+"\",\"Location\":\""+locationValue+"\",\"read\":\"0\"}" +
                "&custom_post=sos&post_status=publish&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq"+"&ACCESS_TOKEN=" +user.getAccessToken();
        //Log.e("sendAnSos()",endpoint);
        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                showErrorDialog(getString(R.string.error_dialog_general));
                Log.e("sendAnSos()","SOS FAILED");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                hideLoading();

                if (response.isSuccessful()) {
                    Log.e("sendAnSos()","SOS SUCCESSFUL");
                } else {
                    Log.e("sendAnSos()","SOS - NOT SUCCESSFUL");
                    showErrorDialog(getString(R.string.error_dialog_general));
                }
            }
        });
    }

    /**
     * Functions below are for permissions
     */
    private void checkPermissions() {
        // If BODY_SENSORS permission has not been taken before then ask for the permission with popup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void checkBackgroundLocationPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 2);
            }
        }
    }

    /**
     * Functions below are for start and stop data measurement (data collection)
     */

    @SuppressLint("SetTextI18n")
    private void stopDataMeasurement() {
        icon_heartRate.clearAnimation();
        actionButton.setText(getString(R.string.start));
        actionButton.setTextColor(getColor(R.color.blue_dark));
        actionButton.setBackground(getDrawable(R.drawable.blue_button));
        shouldBlockBack = false;
        step = 0;
        pressureZero = -10000;
        handler.removeCallbacks(r);
        wakeLock.release();
        isWakeLockAcquire = false;
        value_battery.setText("N/A");
        // stop location
        //get locations
        if (fusedLocationClient != null) {
            stopLocationUpdates();
        }

        if (isHeartBeatAccessible) {
            value_heartRate.setText(defaultHeartRate);
        }


        if (isPressureAccessible) {
            value_pressure.setText("0 hPa");
        }

        //stop sensors
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
            //Log.d(TAG, "Sensor Manager successfully unregistered.");
        }


        //stop beacons
        if (beaconManager != null) {
            stopBeaconSearch();
        }
        // clear area major id for memory
        areaMajorsIDs = new ArrayList<>();
        bluetoothIcon.setImageResource(R.drawable.ic_bluetooth_off);
        // load the offline data and write it to the public doc folder
        //saveDataToOfflineFile("]}");
        //writeFileToPublicDir();
    }


    @SuppressLint("BatteryLife")
    private void starDataMeasurement() {

        // Check the location settings of the user and show dialog if location is off
        if (!isGPSEnable()) {
            locationDialog.setVisibility(View.VISIBLE);
            measureLayout.setVisibility(View.GONE);
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            isBluetoothEnable = false;
        }
        else {
            isBluetoothEnable = mBluetoothAdapter.isEnabled();
        }

        if (!isBluetoothEnable){
            bluetoothDialog.setVisibility(View.VISIBLE);
            measureLayout.setVisibility(View.GONE);
        }else{
            bluetoothIcon.setImageResource(R.drawable.ic_bluetooth_on);
        }

        actionButton.setText(getString(R.string.stop));
        actionButton.setTextColor(getColor(R.color.white));
        actionButton.setBackground(getDrawable(R.drawable.red_button));
        shouldBlockBack = true;
        lastMinute = -100;
        //get location
        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();


        //start sensors
        if (isHeartBeatAccessible) {
            mSensorManager.registerListener(mSensorEventListener, mHeartSensor, 5000000); //5sec
            heartRateValue = "none";
            icon_heartRate.startAnimation(heartBeatAnimation);
        }

        if (isPressureAccessible) {
            mSensorManager.registerListener(mSensorEventListener, mPressureSensor, 5000000); //5sec
            pressureValue = "0 hPa";
        }

        pressureZero = -10000;
        // make json object
        measurementsArray = new JSONArray();
        dataJson = new JSONObject();
        beaconIdValue = "none";
        beaconDistanceValue = "none";

        try {
            dataJson.put("measurements", measurementsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // make json object
        cacheMeasurementsArray = new JSONArray();
        cacheDataJson = new JSONObject();

        try {
            cacheDataJson.put("measurements", cacheMeasurementsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
       // saveDataToOfflineFile("{\"user\":\"" + userName + "\",\"measurements\": [\n");
        //start wakelock
        wakeLock.acquire();
        isWakeLockAcquire = true;
        handler.postDelayed(r, SEND_DATA_EVERY_MILL);
        handler1.postDelayed(r1, RECEIVE_ALERTS_DB_EVERY_MILL);

        //Log.d(TAG, "Sensor Manager successfully registered.");
    }

    /**
     * Functions below are for beacons
     */

    private void getBeaconList(){
        areaMajorsIDs = new ArrayList<>();
        loadingLayout.setVisibility(View.VISIBLE);
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String registerUrl = "http://smart-sonia.eu.144-76-38-75.comitech.gr/api/getposts/?custom_post=beacons&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq&perpage=100000";
        Request request = new Request.Builder()
                .url(registerUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                hideLoading();
                showErrorDialog(getString(R.string.error_dialog_data));
                Log.e("getBeaconList()","ERROR FAILED");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                hideLoading();

                if (response.isSuccessful()){
                    //Log.e("RESPONSE - onResponse", "register call isSuccessful" + myResponse);
                    CmsBeaconObject.Root rootResponse = new Gson().fromJson(myResponse, CmsBeaconObject.Root.class);
                    if(rootResponse.respond.equals("1")){
                        int majorId;
                        for(int i=0;i<rootResponse.result.size();i++){
                            if(rootResponse.result.get(i).custom_fields.active.equals("1")){
                                majorId = Integer.parseInt(rootResponse.result.get(i).custom_fields.beacon_id);
                                addNewBeaconInArea(majorId);
                            }
                        }
                        Log.e("getBeaconList()","SUCCESS : "+areaMajorsIDs);

                        new Handler(Looper.getMainLooper()).postDelayed(
                                new Runnable() {
                                    @Override public void run() {
                                        starDataMeasurement();
                                    }
                                }, 100);

                    }else{
                        showErrorDialog(getString(R.string.error_dialog_data));
                        Log.e("getBeaconList()","ERROR RESPONSE 0 ");
                    }
                }else{
                    showErrorDialog(getString(R.string.error_dialog_data));
                    Log.e("getBeaconList()","ERROR NOT SUCCESSFUL");
                }
            }
        });
    }

    private void addNewBeaconInArea(int majorID){
        if(!areaMajorsIDs.contains(majorID)){
            areaMajorsIDs.add(majorID);
        }
    }

    // Start ranging the beacons
    private void startRangingBeacons() {
        beaconIdValue = "none";
        beaconDistanceValue = "none";
        currentBeaconList = new ArrayList<>();
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("uniqueId", null, null, null));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start ranging beacons", e);
        }
    }

    private boolean checkPreviousTimeScan(){
        final Calendar c = Calendar.getInstance();
        int minute = c.get(Calendar.MINUTE);
        //Log.e("checkPreviousTimeScan","MINUTE = "+minute);
            if(minute>lastMinute){
                int timePassed = minute-lastMinute;
                if(timePassed>9){
                    lastMinute = minute;
                    //Log.e("checkPreviousTimeScan","ok");
                    return true;
                }else {
                   // Log.e("checkPreviousTimeScan","not ok");

                    return false;
                }
            }else if(minute==lastMinute) {
                //Log.e("checkPreviousTimeScan","not ok");
                return false;

            }else{
                int tempMinute = minute + 60;
                int timePassed = tempMinute-lastMinute;
                if(timePassed>9){
                    lastMinute = minute;
                    //Log.e("checkPreviousTimeScan","ok");
                    return true;
                }else {
                    //Log.e("checkPreviousTimeScan","not ok");

                    return false;
                }
            }
    }

    // Need to override onBeaconServiceConnect
    @Override
    public void onBeaconServiceConnect() {
        //Log.e(TAG, "onBeaconServiceConnect called");

        // First remove all Range Notifiers
        beaconManager.removeAllRangeNotifiers();
        // Add RangeNotifier
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                beaconIdValue = "none";
                beaconDistanceValue = "none";

                currentBeaconList = new ArrayList<>();
                if (beacons.size() > 0) {
                    if(checkPreviousTimeScan()){
                        for (Beacon beacon : beacons) {
                            //get beacon id
                            BeaconModel beaconModel = new BeaconModel(beacon.getId2().toInt(), beacon.getDistance());
                            //Log.e("BEACON", beaconModel.majorId + " " + beaconModel.distance);

                            if (areaMajorsIDs.contains(beaconModel.majorId)) {
                                boolean exist = false;
                                for (int i = 0; i < currentBeaconList.size(); i++) {
                                    if (currentBeaconList.get(i).majorId == beaconModel.majorId) {
                                        exist = true;
                                    }
                                }
                                if (!exist) {
                                    currentBeaconList.add(beaconModel);
                                } else {
                                    Log.e("BEACON", "Already in the list");
                                }
                            } else {
                                Log.e("BEACON", "Unknown beacon");
                            }
                        }
                        double min = 100.00;
                        int minIndex = -1;
                        for(int i=0;i<currentBeaconList.size();i++){
                            if(min>=currentBeaconList.get(i).distance){
                                minIndex = i;
                            }
                            //Log.e("BEACON", i + " ID: " + currentBeaconList.get(i).majorId+", Distance: "+currentBeaconList.get(i).distanceFormatted);
                        }
                        if(minIndex != -1){
                            //Log.e("CLOSER BEACON", "ID "+currentBeaconList.get(minIndex).majorId);
                            if(currentBeaconList.get(minIndex).distance <= RED_ZONE_AREA){
                               // Log.e("USER IN AREA", "RED");
                                if(beaconModel == null){
                                    beaconModel = currentBeaconList.get(minIndex);
                                    //Log.e("BEACON", "Showing notification");
                                    String bid = String.valueOf(beaconModel.majorId);
                                    newNotificationProcessHandel(true,bid,"You are in a RED area","Make sure you have the necessary equipment for your best protection.","BEACONRED101");
                                }else if (beaconModel.majorId != currentBeaconList.get(minIndex).majorId){
                                    beaconModel = currentBeaconList.get(minIndex);
                                    //Log.e("BEACON", "Showing notification");
                                    String bid = String.valueOf(beaconModel.majorId);
                                    newNotificationProcessHandel(true,bid,"You are in a RED area","Make sure you have the necessary equipment for your best protection.","BEACONRED101");
                                }else {
                                    beaconModel = currentBeaconList.get(minIndex);
                                    //Log.e("BEACON", "Showing notification");
                                    String bid = String.valueOf(beaconModel.majorId);
                                    newNotificationProcessHandel(true,bid,"You are in a RED area","Make sure you have the necessary equipment for your best protection.","BEACONRED101");
                                    //Log.e("BEACON", "In the same area but show notification");
                                }
                            }else {
                                newNotificationProcessHandel(true,"000","",".","BEACONSAFE101");
                                //Log.e("USER IN AREA", "GREEN");
                            }
                            // build beacon json
                            JSONObject beaconInfo = new JSONObject();
                            try {
                                beaconInfo.put("id", String.valueOf(currentBeaconList.get(minIndex).majorId));
                                beaconInfo.put("distance", currentBeaconList.get(minIndex).distanceFormatted);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            // add beacon on beacons array
                            beaconIdValue = String.valueOf(currentBeaconList.get(minIndex).majorId);
                            beaconDistanceValue = currentBeaconList.get(minIndex).distanceFormatted;
                        }else{
                                newNotificationProcessHandel(true,"000","",".","BEACONSAFE101");
                                //Log.e("USER IN AREA", "GREEN");
                        }
                    }
                }else {
                    if(checkPreviousTimeScan()){
                        newNotificationProcessHandel(true,"000","",".","BEACONSAFE101");
                        //Log.e("USER IN AREA", "GREEN");
                    }
                }
            }
        });
    }

    private void startBeaconSearch() {
        //Log.e(TAG, "startBeaconSearch()");
        // Construct Beacon Manager
        beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
        // Set IBEACON Layout
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_LAYOUT));
        // Set persistence state false in order to reset the region state on development mode
        beaconManager.setRegionStatePersistenceEnabled(false);
        // Need this to work on Android 5-7
        beaconManager.setEnableScheduledScanJobs(true);

        beaconManager.setForegroundScanPeriod(BEACONS_SCAN_PERIOD_MILL);
        beaconManager.setForegroundBetweenScanPeriod(BEACONS_BETWEEN_SCAN_PERIOD_MILL);

        beaconManager.setBackgroundMode(true);

        beaconManager.setBackgroundScanPeriod(BEACONS_SCAN_PERIOD_MILL);
        beaconManager.setBackgroundBetweenScanPeriod(BEACONS_BETWEEN_SCAN_PERIOD_MILL);

        beaconManager.bind(this);

        startRangingBeacons();
    }


    private void stopBeaconSearch() {
        beaconIdValue = "none";
        beaconDistanceValue = "none";
        beaconModel = null;
        Log.e(TAG, "stopBeaconSearch()");
        beaconManager.stopRangingBeacons(new Region("uniqueId", null, null, null));
        beaconManager.unbind(this);
        beaconManager = null;
    }

    /* OLD VERSION
    public boolean checkNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        //Log.e(TAG, "There is no internet connection");
        return false;
    }
     */


    /**
     * Functions below send the json object to "DATABASE"
     */
    public boolean checkNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) );
        }
        return false;
    }

    private void updateUserData() {
        //send data to server
        //Log.e(TAG, dataJson.toString());
        if(cacheEnable){
            try {
                JSONArray m = cacheDataJson.getJSONArray("measurements");
                for(int i = 0;i<m.length();i++){
                    sendAction(m.get(i).toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            try {
                JSONArray m = dataJson.getJSONArray("measurements");
                for(int i = 0;i<m.length();i++){
                    sendAction(m.get(i).toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendAction(String jsonString){
        //Log.e("SMART SONIA SEND", jsonString);
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "content="+jsonString);
        Request request = new Request.Builder()
                .url("http://smart-sonia.eu.144-76-38-75.comitech.gr/api/newpost/?subject="+user.getUserName()+"&custom_field="+jsonString+
                        "&custom_post=worker_data&post_status=publish&auth_key=l8qjQ5vXHfDlsmwkD4tPZantq"+"&ACCESS_TOKEN=" +user.getAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                Log.e("sendAction()", "================================== FAILED ==========================");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                String myResponse = response.body().string();
                if (response.isSuccessful()){
                    cacheEnable = false;
                    Log.d("RESPONSE - onResponse", "update call isSuccessful" + myResponse );
                }else{
                    Log.e("sendAction()", "====== NOT SUCCESSFUL ======="+ myResponse);
                }
            }
        });

        // send alerts if there is any in cache.
        if(beaconAlertsCache.size()>0){
            //Log.e("sendAction()", "There is(are) " + beaconAlertsCache.size() + "alert(s) in cache");
            ArrayList<String> alertList= new ArrayList<>(beaconAlertsCache);
            beaconAlertsCache = new ArrayList<>();
            for(int i=0;i<alertList.size();i++){
                sendBeaconAlert(alertList.get(i));
            }
        }
    }

    /**
     * Functions below are for the offline data file log
     *
     */
    // save the current data into the offline file for debug proposes
    public void saveDataToOfflineFile(String text) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(FILE_NAME, Context.MODE_APPEND));
            outputStreamWriter.write(text);
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // delete all data that have been saved into the offline file
    public void deleteDataFromOfflineFile(){
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(FILE_NAME, Context.MODE_PRIVATE));
            outputStreamWriter.write("");
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFileToPublicDir() {
        //Write to file
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        Date now = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat fileFormat = new SimpleDateFormat("MMddyyHHmmss");
        String timestampFile = fileFormat.format(now);
        String fileName = "smartSoniaTestOutputLogs_" + timestampFile + ".txt";
        File file = new File(dir, fileName);
        String text = loadDataFromFile();
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.append(text);
            //Log.e("writeFileToPublicDir()", "write done");
            //deleteDataFromOfflineFile();
        } catch (IOException e) {
            //Log.e("writeFileToPublicDir()","catch! " + e.getMessage());
            //Handle exception
        }
    }

    public String loadDataFromFile() {
        String str = "";
        if (fileExists(this,FILE_NAME)) {
            List<String> tmpList = new ArrayList<String>();
            FileInputStream fis = null;
            try {
                fis = openFileInput(FILE_NAME);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String text;
                while ((text = br.readLine()) != null) {
                    tmpList.add(text);
                }
                for(int i=0;i<=tmpList.size()-1;i++) {
                    sb.append(tmpList.get(i)).append("\n");
                }
                str = sb.toString();
                return str;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return str;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


    /**
     * Function below are for sensors
     */

    @SuppressLint("SetTextI18n")
    private void checkSensorAvailability() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        //List of integrated sensor of device
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Sensor sensor : sensors) {
            arrayList.add(sensor.getName()); // put integrated sensor list in arraylist
            // Log.d(TAG, " " + sensor.getName()); // print the arraylist in log.
        }

        // check if HEART RATE sensor is Accessible
        if ((mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)) != null) {
            isHeartBeatAccessible = true;
            //get HEART RATE sensor
            mHeartSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            ////Log.d(TAG, "HEART RATE sensor is Accessible");
        } else {
            value_heartRate.setText(getString(R.string.not_available));
            ////Log.d(TAG, "HEART RATE sensor is Inaccessible");
        }

        if ((mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)) != null) {
            isPressureAccessible = true;
            //get PRESSURE sensor
            mPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            //Log.d(TAG, "PRESSURE sensor is Accessible");
        } else {
            value_pressure.setText(getString(R.string.not_available));
            //Log.d(TAG, "PRESSURE sensor is Inaccessible");
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent event) {

            //get sensor value
            if(event.sensor.getType() == Sensor.TYPE_HEART_RATE){
                int hv = (int) event.values[0];
                if(heartRateValue.equals("none")){
                    heartRateValue = String.valueOf(hv);
                }else if(hv!=0){
                    heartRateValue = String.valueOf(hv);
                }
                String heartRateWithUnit = heartRateValue + " bpm";
                value_heartRate.setText(heartRateWithUnit);
            }

            float pressure = 0;
            if(event.sensor.getType() == Sensor.TYPE_PRESSURE){
                pressure = event.values[0];
                if(pressureZero == -10000){
                    pressureZero = pressure;
                }
                pressureZeroValue = String.valueOf(pressureZero);
                pressureValue = String.valueOf(pressure);
                String pressureWithUnit = String.format("%.01f", pressure) + " hPa";
                value_pressure.setText(pressureWithUnit);
            }

        }
    };

    /**
     * Function below are for notifications (alerts)
     */


    private void vibrateWatch(int time) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 1000 milliseconds
        v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void showNotification(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        measureLayout.setVisibility(View.GONE);
                        notificationDialog.setVisibility(View.VISIBLE);
                    }
                }, 100);

    }

    private void hideNotificationBubble(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        notificationBubble.setVisibility(View.GONE);
                        notificationBubble.clearAnimation();
                    }
                }, 100);

    }

    private void hideNotification(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        notificationDialog.setVisibility(View.GONE);
                        measureLayout.setVisibility(View.VISIBLE);
                    }
                }, 100);

    }

    private void showNotificationBubble(){
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        String bubbleText = String.valueOf(unreadNotifications.size());
                        notificationBubble.setText(bubbleText);
                        notificationBubble.setVisibility(View.VISIBLE);
                    }
                }, 100);

    }

    /**
     * Handle Ambient Mode support
     */

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            //Log.e(TAG,"EnterAmbient");
            appInAmbient = true;
            scrollView.scrollTo(0,0);
            if(isUserLogin){
                value_heartRate.setTextColor(getColor(R.color.white));
                icon_heartRate.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white));
                icon_heartRate.clearAnimation();
                value_pressure.setTextColor(getColor(R.color.white));
                icon_pressure.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white));
                actionButton.setVisibility(View.GONE);
                logoutButton.setVisibility(View.INVISIBLE);
                welcomeText.setVisibility(View.GONE);
                environmentLayout.setBackground(getDrawable(R.color.black));
                icon_battery.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white));
                value_battery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                if(unreadNotification){
                    hideNotification();
                    showNotificationBubble();
                }
            }else {
                loginLayout.setVisibility(View.GONE);
            }

        }

        @Override
        public void onExitAmbient() {
            //Log.e(TAG,"ExitAmbient");
            appInAmbient = false;
            if(isUserLogin){
                actionButton.setVisibility(View.VISIBLE);
                logoutButton.setVisibility(View.VISIBLE);
                welcomeText.setVisibility(View.VISIBLE);
                icon_heartRate.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                value_heartRate.setTextColor(getColor(R.color.blue));
                value_pressure.setTextColor(getColor(R.color.blue));
                environmentLayout.setBackground(getDrawable(R.drawable.blue_dark_outline_section));
                icon_pressure.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                icon_battery.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                value_battery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                if(unreadNotification){
                    hideNotificationBubble();
                    showNotification();
                }
                if(actionButton.getText().equals(getString(R.string.stop))){
                    //start sensors
                    if(isHeartBeatAccessible){
                        icon_heartRate.startAnimation(heartBeatAnimation);
                    }
                }
            }else {
                loginLayout.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
            //Log.d(TAG,"UpdateAmbient");
        }
    }

    /**
     * Functions below are for the location updates
     */

    private boolean isGPSEnable(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void startLocationUpdates() {
        //start beacon search
        startBeaconSearch();

        Log.i("Location", "startLocationUpdates()");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        //Log.e("Location", "stopLocationUpdates()" );
        fusedLocationClient.removeLocationUpdates(locationCallback);
        fusedLocationClient = null;
        locationRequest = null;
        locationCallback = null;
        locationIcon.setImageResource(R.drawable.ic_location_off);
    }

    private void createLocationRequest() {
        //Log.e("Location", "createLocationRequest()");
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_REFRESH_MILL);
        locationRequest.setFastestInterval(LOCATION_REFRESH_MILL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationIcon.setImageResource(R.drawable.ic_location_on);
    }

    private void createLocationCallback(){
        Log.i("Location", "createLocationCallback()");

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    locationValue = location.getLatitude() +","+ location.getLongitude();
                    Log.e(TAG,""+location.getLatitude() +","+ location.getLongitude());
                }
            }
        };
    }

}