package org.techtown.my_jubgging.jubgging;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.techtown.my_jubgging.PloggingInfo;
import org.techtown.my_jubgging.R;
import org.techtown.my_jubgging.UserInfo;
import org.techtown.my_jubgging.retrofit.RetrofitAPI;
import org.techtown.my_jubgging.retrofit.RetrofitClient;
import org.techtown.my_jubgging.trashmap.TrashMapFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class JubggingFragment extends TrashMapFragment implements SensorEventListener {
    private RetrofitAPI retrofitApi;
    private Context context;
    UserInfo userInfo;
    SensorManager sensorManager;
    Sensor stepCountSensor;

    /* Instance Value */
    private int textColor;
    private boolean isRunning = false;

    long runtime = 0;
    public static int step;

    /* View Reference */
    ViewGroup rootView;

    private Chronometer chronometer;
    private Button start_end_Btn;
    private TextView countDownTxt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        context = rootView.getContext();

        retrofitApi = RetrofitClient.getApiService();
        userInfo = (UserInfo)getActivity().getIntent().getSerializableExtra("userInfo");

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {

            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }

        chronometer = new Chronometer(context);
        textColor = getResources().getColor(R.color.text_color);

        step = 0;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        setViewById();
        setOnClick();

        return rootView;
    }

    private void setViewById() {
        /* ???????????? */
        FrameLayout.LayoutParams paramsW128 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            128);
        paramsW128.setMargins(0, 0, 0, 172);
        paramsW128.gravity = Gravity.BOTTOM | Gravity.CENTER;

        chronometer.setLayoutParams(paramsW128);
        chronometer.setTypeface(chronometer.getTypeface(), Typeface.BOLD);
        chronometer.setTextSize(36);
        chronometer.setTextColor(getResources().getColor(R.color.black));
        chronometer.setGravity(Gravity.CENTER);
        chronometer.setPadding(40, 0, 40, 0);

        rootView.addView(chronometer);

        /* ?????? ?????? */
        start_end_Btn = new Button(context);

        FrameLayout.LayoutParams paramsM128 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                128);
        paramsM128.setMargins(40, 40, 40, 40);
        paramsM128.gravity = Gravity.BOTTOM | Gravity.CENTER;

        start_end_Btn.setLayoutParams(paramsM128);
        start_end_Btn.setText("?????? ?????? !");
        start_end_Btn.setTypeface(start_end_Btn.getTypeface(), Typeface.BOLD);
        start_end_Btn.setTextSize(18);
        start_end_Btn.setTextColor(textColor);
        start_end_Btn.setBackgroundResource(R.drawable.rounded_rectangle);

        rootView.addView(start_end_Btn);

        /* ????????? ?????? ????????? */
        countDownTxt = new TextView(context);

        FrameLayout.LayoutParams paramsMM = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        countDownTxt.setLayoutParams(paramsMM);
        countDownTxt.setText("");
        countDownTxt.setTypeface(start_end_Btn.getTypeface(), Typeface.BOLD);
        countDownTxt.setTextSize(128);
        countDownTxt.setTextColor(getResources().getColor(R.color.black));
        countDownTxt.setGravity(Gravity.CENTER);

        rootView.addView(countDownTxt);
    }

    private void setOnClick() {
        start_end_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    isRunning = true;
                    Calendar cal = Calendar.getInstance();
                    startCnt();

                    start_end_Btn.setText("?????? ??????");
                    start_end_Btn.setTextColor(getResources().getColor(R.color.main_color_5));
                    start_end_Btn.setBackgroundResource(R.drawable.rounded_rectangle_bold);

                }
                else {
                    isRunning = false;
                    chronometer.stop();

                    Calendar cal = Calendar.getInstance();

                    runtime = SystemClock.elapsedRealtime() - chronometer.getBase();
                    saveInfo();

                    Intent intent = new Intent(context, JubggingResultActivity.class);
                    intent.putExtra("runtime", runtime);
                    intent.putExtra("step", step);
                    intent.putExtra("userInfo",userInfo);
                    context.startActivity(intent);

                    getActivity().finish();
                }
            }
        });
    }

    private void saveInfo() {
        PloggingInfo info = new PloggingInfo();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(runtime);
        if (runtime < 60 * 60 * 1000)
            date.setHours(0);

        info.userId = Long.parseLong(userInfo.userId);
        info.walkingNum = step;
        info.walkingTime = timeFormat.format(date);
        Call<Map<String, Long>> call = retrofitApi.savePloggingInfo(info);

        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(Call<Map<String, Long>> call, Response<Map<String, Long>> response) {
                if (!response.isSuccessful()) {
                    customToast("Code : " + response.code() + response.message() + response.errorBody());
                    return;
                }
            }

            @Override
            public void onFailure(Call<Map<String, Long>> call, Throwable t) {

            }
        });
    }

    public void startCnt() {
        Animation cntDownAni = AnimationUtils.loadAnimation(context, R.anim.count_down);

        countDownTxt.setText("3");
        countDownTxt.startAnimation(cntDownAni);

        start_end_Btn.setEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTxt.setTextSize(128);
                countDownTxt.setText("");
            }
        }, 4000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();

                countDownTxt.setTextSize(64);
                countDownTxt.setText("START!");

                start_end_Btn.setEnabled(true);
            }
        }, 3000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTxt.setText("1");
                countDownTxt.startAnimation(cntDownAni);
            }
        }, 2000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTxt.setText("2");
                countDownTxt.startAnimation(cntDownAni);
            }
        }, 1000);

        sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0f) {
                ++step;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
    private void readData() {
        Fitness.getHistoryClient(context,
                        GoogleSignIn.getLastSignedInAccount(context))
                .readData(new DataReadRequest.Builder()
                        .read(DataType.TYPE_STEP_COUNT_DELTA)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse response) {
                        DataSet dataSet = response.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);

                        for (DataPoint dp : dataSet.getDataPoints()) {
                            step += dp.getValue(Field.FIELD_STEPS).asInt();
                        }

                        Log.d("aaa", step + " ");
                        //result();
                    }
                });
    }
     */
    private void customToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}
