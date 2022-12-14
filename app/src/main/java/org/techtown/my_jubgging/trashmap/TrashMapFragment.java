package org.techtown.my_jubgging.trashmap;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.techtown.my_jubgging.MyProfile;
import org.techtown.my_jubgging.R;
import org.techtown.my_jubgging.UserInfo;
import org.techtown.my_jubgging.retrofit.RetrofitAPI;
import org.techtown.my_jubgging.retrofit.RetrofitClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class TrashMapFragment extends Fragment implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener,MapView.POIItemEventListener {
    private static final String LOG_TAG = "TrashMapFragment";
    private MapView mapView;
    private ImageView imageView;
    private Bitmap bitmap;

    private LinearLayout linearLayout;
    private MapPoint.GeoCoordinate geoCoordinate;
    private ImageButton refreshTrash;
    private ImageButton cur_location;
    private ImageButton zoom_in;
    private ImageButton zoom_out;

    private Context context;

    private DBHelper dbHelper;
    private List<PublicTrash> publicTrashList;
    private List<MapPOIItem> publicMarkerList = new ArrayList<>();

    private UserInfo userInfo;
    private ImageButton profileImgBtn;

    private Retrofit retrofit = RetrofitClient.getInstance();
    private RetrofitAPI retrofitAPI = RetrofitClient.getApiService();

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION};

    private CustomCallOutBalloonAdapter customCallOutBalloonAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userInfo = (UserInfo) getActivity().getIntent().getSerializableExtra("userInfo");
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_map, container, false);
        context = getActivity();

        //MapView ??????
        mapView = new MapView(rootView.getContext());
        rootView.addView(mapView);




        //MapView ?????????????????? ??????
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);
        customCallOutBalloonAdapter = new CustomCallOutBalloonAdapter();
        mapView.setCalloutBalloonAdapter(customCallOutBalloonAdapter);

        //MapView ?????? ?????? ??????????????? ??????
        startTracking();

        //GPS ???????????? ?????? ?????? ??????
        if (!checkLocationServicesStatus())
            showDialogForLocationServiceSetting();
        else
            checkRunTimePermission();

        mapView.setZoomLevel(3,true);

        //??????????????? ???????????? ???????????? SQLite ?????? ?????????
        dbHelper = new DBHelper(getContext());
        publicTrashList = dbHelper.getAddressList();

//        for(int i=8950; i<=14242;i++)
//        {
//            dbHelper.deleteAddress(i);
//        }

        //?????? SQLite ??? ?????? ???????????? ?????????
        /*
        if(publicTrashList.size()==0)
        {
            //??????????????? ???????????? ?????????  Retrofit ??? ?????? GET
            Call<HashMap<String, List<PublicTrash>>> call_public = retrofitAPI.getPublicTrashList();
            call_public.enqueue(new Callback<HashMap<String, List<PublicTrash>>>() {
                @Override
                public void onResponse(Call<HashMap<String, List<PublicTrash>>> call, Response<HashMap<String, List<PublicTrash>>> response) {

                    //?????? ??????
                    if (!response.isSuccessful()) {
                        Log.e(LOG_TAG, String.valueOf(response.code()));
                        return;
                    }

                    //?????? ????????? ??????????????? ???????????? ??????
                    HashMap<String, List<PublicTrash>> result = response.body();
                    publicTrashList = result.get("results");


                    int i  = 0;
                    for(PublicTrash pt : publicTrashList)
                    {
                        //SQLite ??? ??????
                        dbHelper.InsertAddress(pt.getAddress(),pt.getKind(),pt.getLatitude(),pt.getLongitude(),pt.getSpec());

                        MapPOIItem customMarker = new MapPOIItem();
                        customMarker.setUserObject(pt);
                        // ?????? ??????
                        customMarker.setItemName("Custom Marker");
                        // ?????? ??????
                        customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(pt.getLatitude()),Double.parseDouble(pt.getLongitude())));
                        // ??????????????? ????????? ????????? ??????.
                        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                        // ?????? ?????????.
                        switch (pt.getKind())
                        {
                            case "General":
                                customMarker.setCustomImageResourceId(R.drawable.trash_general_red);
                                break;
                            case "Recycle":
                                customMarker.setCustomImageResourceId(R.drawable.trash_recycle_red);
                                break;
                            case "Smoking":
                                customMarker.setCustomImageResourceId(R.drawable.trash_smoking_red);
                                break;
                        }
                        // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                        customMarker.setCustomImageAutoscale(false);
                        //?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                        customMarker.setCustomImageAnchor(0.5f, 1.0f);

                        mapView.addPOIItem(customMarker);
                    }
                }

                @Override
                public void onFailure(Call<HashMap<String, List<PublicTrash>>> call, Throwable t) {
                    //?????? ??????
                    Log.e(LOG_TAG, t.getLocalizedMessage());
                }
            });
        }
        else
        {
            for(PublicTrash pt : publicTrashList)
            {
                //Log.i(LOG_TAG,pt.getAddress());
                MapPOIItem customMarker = new MapPOIItem();
                customMarker.setUserObject(pt);
                // ?????? ??????
                customMarker.setItemName("Custom Marker");
                // ?????? ??????
                customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(pt.getLatitude()),Double.parseDouble(pt.getLongitude())));
                // ??????????????? ????????? ????????? ??????.
                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                // ?????? ?????????.
                switch (pt.getKind())
                {
                    case "General":
                        customMarker.setCustomImageResourceId(R.drawable.trash_general_red);
                        break;
                    case "Recycle":
                        customMarker.setCustomImageResourceId(R.drawable.trash_recycle_red);
                        break;
                    case "Smoking":
                        customMarker.setCustomImageResourceId(R.drawable.trash_smoking_red);
                        break;
                }
                // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                customMarker.setCustomImageAutoscale(false);
                //?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                customMarker.setCustomImageAnchor(0.5f, 1.0f);

                mapView.addPOIItem(customMarker);
            }
        }
        */


        //????????? ???????????? ???????????? ?????????
        //????????? ???????????? ?????? GET
        Call<List<CustomTrash>> call_custom = retrofitAPI.getCustomTrashList();
        call_custom.enqueue(new Callback<List<CustomTrash>>() {
            @Override
            public void onResponse(Call<List<CustomTrash>> call, Response<List<CustomTrash>> response) {
                View callOutBalloon;

                //?????? ??????
                if (!response.isSuccessful()) {
                    Log.e(LOG_TAG, String.valueOf(response.code()));
                    return;
                }

                //?????? ????????? ??????????????? (????????? ????????????) ??????
                List<CustomTrash> result = response.body();
                Log.i(LOG_TAG,"????????? ???????????? ?????? Get ?????? "+result.size()+"???");

                for(CustomTrash ct : result)
                {

                    MapPOIItem customMarker = new MapPOIItem();
                    customMarker.setUserObject(ct);
                    // ?????? ??????
                    customMarker.setItemName("Custom Marker");
                    // ?????? ??????
                    customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(ct.getLatitude()),Double.parseDouble(ct.getLongitude())));
                    // ??????????????? ????????? ????????? ??????.
                    customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                    // ?????? ?????????.
                    switch (ct.getKind())
                    {
                        case "GENERAL":
                            customMarker.setCustomImageResourceId(R.drawable.trash_general_blue);
                            break;
                        case "RECYCLE":
                            customMarker.setCustomImageResourceId(R.drawable.trash_recycle_blue);
                            break;
                        case "SMOKING":
                            customMarker.setCustomImageResourceId(R.drawable.trash_smoking_blue);
                            break;
                    }
                    // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                    customMarker.setCustomImageAutoscale(false);
                    //?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                    customMarker.setCustomImageAnchor(0.5f, 1.0f);

                    mapView.addPOIItem(customMarker);
                }


            }

            @Override
            public void onFailure(Call<List<CustomTrash>> call, Throwable t) {
                //?????? ??????
                Log.e(LOG_TAG, t.getLocalizedMessage());
            }
        });


        setView(rootView);
        setButtonClickListener();

        return rootView;
    }

    private void setView(ViewGroup rootView)
    {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams linear_params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(linear_params);
        linear_params.setMargins(10,10,10,10);
        linear_params.gravity = Gravity.RIGHT;

        //?????? 3km ?????????????????? ??????
        FrameLayout.LayoutParams trash_param = new FrameLayout.LayoutParams(100,100);
        trash_param.setMargins(10,10,10,10);

        refreshTrash = new ImageButton(getContext());
        refreshTrash.setLayoutParams(trash_param);
        refreshTrash.setImageResource(R.drawable.trash_general_black);
        refreshTrash.setMaxWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        refreshTrash.setMaxHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        refreshTrash.setBackgroundResource(R.drawable.shape_for_circle_button);
        linearLayout.addView(refreshTrash);


        //????????? ??????
        FrameLayout.LayoutParams cur_param = new FrameLayout.LayoutParams(100,100);
        cur_param.setMargins(10,10,10,10);

        cur_location = new ImageButton(getContext());
        cur_location.setLayoutParams(cur_param);
        cur_location.setBackgroundResource(R.drawable.shape_for_circle_button);
        cur_location.setImageResource(R.drawable.ic_baseline_my_location_24);
        cur_location.setMaxWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        cur_location.setMaxHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(cur_location);

        //????????? ??????
        FrameLayout.LayoutParams in_param = new FrameLayout.LayoutParams(100,100);
        in_param.setMargins(10,10,10,10);

        zoom_in = new ImageButton(getContext());
        zoom_in.setLayoutParams(in_param);
        zoom_in.setBackgroundResource(R.drawable.shape_for_circle_button);
        zoom_in.setImageResource(R.drawable.ic_baseline_add_circle_outline_24);
        zoom_in.setMaxWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        zoom_in.setMaxHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(zoom_in);

        //????????? ??????
        FrameLayout.LayoutParams out_param = new FrameLayout.LayoutParams(100,100);
        out_param.setMargins(10,10,10,10);

        zoom_out = new ImageButton(getContext());
        zoom_out.setLayoutParams(out_param);
        zoom_out.setBackgroundResource(R.drawable.shape_for_circle_button);
        zoom_out.setImageResource(R.drawable.ic_baseline_remove_circle_outline_24);
        zoom_out.setMaxWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        zoom_out.setMaxHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(zoom_out);



        rootView.addView(linearLayout);

    }

    private void setButtonClickListener()
    {
        refreshTrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(publicMarkerList.size() != 0)
                {
                    for (MapPOIItem mapPOIItem : publicMarkerList)
                    {
                        mapView.removePOIItem(mapPOIItem);
                    }
                }
                publicMarkerList = new ArrayList<>();

                Thread thread = new Thread()
                {
                    @Override
                    public void run()
                    {
                        MapPoint mapPoint = mapView.getMapCenterPoint();
                        geoCoordinate = mapPoint.getMapPointGeoCoord();
                        Call<HashMap<String, List<PublicTrash>>> call_custom = retrofitAPI.getPublicTrashList(geoCoordinate.latitude,geoCoordinate.longitude,3000);
                        try {
                            HashMap<String, List<PublicTrash>> result = call_custom.execute().body();
                            publicTrashList = result.get("results");

                            for(PublicTrash pt : publicTrashList)
                            {
                                MapPOIItem customMarker = new MapPOIItem();
                                customMarker.setUserObject(pt);
                                // ?????? ??????
                                customMarker.setItemName("Custom Marker");
                                // ?????? ??????
                                customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(pt.getLatitude()),Double.parseDouble(pt.getLongitude())));
                                // ??????????????? ????????? ????????? ??????.
                                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                                // ?????? ?????????.
                                switch (pt.getKind())
                                {
                                    case "General":
                                        customMarker.setCustomImageResourceId(R.drawable.trash_general_red);
                                        break;
                                    case "Recycle":
                                        customMarker.setCustomImageResourceId(R.drawable.trash_recycle_red);
                                        break;
                                    case "Smoking":
                                        customMarker.setCustomImageResourceId(R.drawable.trash_smoking_red);
                                        break;
                                }
                                // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                customMarker.setCustomImageAutoscale(false);
                                //?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                                customMarker.setCustomImageAnchor(0.5f, 1.0f);
                                customMarker.setShowCalloutBalloonOnTouch(false);

                                mapView.addPOIItem(customMarker);
                                publicMarkerList.add(customMarker);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                };
                thread.start();
                try {
                    thread.join();
                }
                catch (InterruptedException e)
                {

                }
            }
        });

        cur_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        zoom_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int zoomLevel = mapView.getZoomLevel();
                mapView.setZoomLevel(zoomLevel-1,true);

            }
        });

        zoom_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int zoomLevel = mapView.getZoomLevel();
                mapView.setZoomLevel(zoomLevel+1,true);
            }
        });

    }

    class CustomCallOutBalloonAdapter implements CalloutBalloonAdapter
    {
        private final  View callOutBalloon;
        private Thread thread;

        public CustomCallOutBalloonAdapter() {
            callOutBalloon = getLayoutInflater().inflate(R.layout.adapter_custom_callout_balloon, null);;
        }

        @Override
        public View getCalloutBalloon(MapPOIItem mapPOIItem) {

            if(publicMarkerList.contains(mapPOIItem))
            {
                return null;
            }

            CustomTrash customTrash = (CustomTrash)mapPOIItem.getUserObject();
            thread = new Thread()
            {
                @Override
                public void run()
                {
                    Call<UserInfo> call_custom = retrofitAPI.getCustomTrashUser(customTrash.getCustomTrashAddressId());
                    try {
                        UserInfo result = call_custom.execute().body();
                        Log.i(LOG_TAG,result.getProfileURL()+""+result.getNickName()+""+result.getHeart());


                        //???????????? ???????????? ?????????
                        imageView = ((ImageView) callOutBalloon.findViewById(R.id.custom_trash_balloon_imageView));

                        //Glide ??? ???????????? ????????? String -> URL -> bitmap ?????? ???????????? imageView ??????
                        Thread thread = new Thread()
                        {
                            @Override
                            public void run()
                            {
                                try{
                                    URL url = new URL(result.getProfileURL());
                                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                    connection.setDoInput(true);
                                    connection.connect();
                                    InputStream is = connection.getInputStream();
                                    bitmap = BitmapFactory.decodeStream(is);

                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        thread.start();
                        try {

                            thread.join();
                            bitmap = Bitmap.createScaledBitmap(bitmap,100,100,true);
                            imageView.setImageBitmap(bitmap);
                            //???????????? ???????????? ?????????
                            ((TextView) callOutBalloon.findViewById(R.id.custom_trash_balloon_nickName)).setText(result.getNickName());
                            //???????????? ???????????? ???????????????
                            ((TextView) callOutBalloon.findViewById(R.id.custom_trash_balloon_heart)).setText("?????????: "+ result.getHeart());
                            Log.i(LOG_TAG,"????????????");
                        }
                        catch (InterruptedException e)
                        {

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            };
            thread.start();
            try {

                thread.join();
                Log.i(LOG_TAG, String.valueOf(((TextView)callOutBalloon.findViewById(R.id.custom_trash_balloon_nickName)).getText()));
                return callOutBalloon;
            }
            catch (InterruptedException e)
            {

            }
            return callOutBalloon;
        }

        @Override
        public View getPressedCalloutBalloon(MapPOIItem mapPOIItem) {
            return null;
        }
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {}

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {}

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        //????????? ???????????? ???????????? ???
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());



        CustomTrash  ct = (CustomTrash) mapPOIItem.getUserObject();
        Log.i(LOG_TAG,ct.getCustomTrashAddressId()+" "+userInfo.getUserId());

        //????????? ???????????? ????????? ?????? ??????
        Call<String> call_check = retrofitAPI.checkUserHeart(new Heart(ct.getCustomTrashAddressId(),userInfo.getUserId()));
        call_check.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                View callOutBalloon;

                //?????? ??????
                if (!response.isSuccessful()) {
                    Log.e(LOG_TAG, String.valueOf(response.code()));
                    return;
                }

                //?????? ????????? ??????????????? (????????? ????????????) ??????
                String result = response.body();

                if(result.equals("{\"heart\":\"Y\"}"))
                {
                    String[] items = {"????????? ??????","????????????","??????"};
                    builder.setIcon(R.drawable.trash_heart_filled);
                    builder.setTitle("????????? ??????????????????");

                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i)
                            {
                                case 0:
                                    Log.i(LOG_TAG,"????????? ??????");

                                    //????????? ???????????? ????????? ??????
                                    Call<String> call_add = retrofitAPI.cancelUserHeart(new Heart(ct.getCustomTrashAddressId(),userInfo.getUserId()));
                                    Thread thread = new Thread()
                                    {
                                        @Override
                                        public void run() {
                                            try {
                                                String result = call_add.execute().body();
                                                CustomTrash customTrash = (CustomTrash)mapPOIItem.getUserObject();
                                                MapPOIItem customMarker = new MapPOIItem();

                                                customMarker.setUserObject(customTrash);
                                                // ?????? ??????
                                                customMarker.setItemName("Custom Marker");
                                                // ?????? ??????
                                                customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(customTrash.getLatitude()),Double.parseDouble(customTrash.getLongitude())));
                                                // ??????????????? ????????? ????????? ??????.
                                                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                                                // ?????? ?????????.
                                                switch (customTrash.getKind())
                                                {
                                                    case "GENERAL":
                                                        customMarker.setCustomImageResourceId(R.drawable.trash_general_blue);
                                                        break;
                                                    case "RECYCLE":
                                                        customMarker.setCustomImageResourceId(R.drawable.trash_recycle_blue);
                                                        break;
                                                    case "SMOKING":
                                                        customMarker.setCustomImageResourceId(R.drawable.trash_smoking_blue);
                                                        break;
                                                }
                                                // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                customMarker.setCustomImageAutoscale(false);
                                                //?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                                                customMarker.setCustomImageAnchor(0.5f, 1.0f);

                                                mapView.addPOIItem(customMarker);
                                                mapView.removePOIItem(mapPOIItem);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    };
                                    thread.start();
                                    try {
                                        thread.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 1:
                                    Log.i(LOG_TAG,"????????????");
                                    Toast.makeText(context, "????????? ?????????????????????.",Toast.LENGTH_SHORT).show();
                                    break;
                                case 2:
                                    Log.i(LOG_TAG,"??????");
                                    break;
                            }
                        }
                    });
                }
                else if(result.equals("{\"heart\":\"N\"}"))
                {
                    String[] items = {"?????????","????????????","??????"};
                    builder.setIcon(R.drawable.trash_heart_empty);
                    builder.setTitle("????????? ??????????????????");

                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i)
                            {
                                case 0:
                                    Log.i(LOG_TAG,"?????????");
                                    //????????? ???????????? ?????????
                                    Call<String> call_add = retrofitAPI.addUserHeart(new Heart(ct.getCustomTrashAddressId(),userInfo.getUserId()));
                                    Thread thread = new Thread()
                                    {
                                        @Override
                                        public void run() {
                                            try {
                                                String result = call_add.execute().body();
                                                CustomTrash customTrash = (CustomTrash)mapPOIItem.getUserObject();
                                                MapPOIItem customMarker = new MapPOIItem();

                                                customMarker.setUserObject(customTrash);
                                                // ?????? ??????
                                                customMarker.setItemName("Custom Marker");
                                                // ?????? ??????
                                                customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(customTrash.getLatitude()),Double.parseDouble(customTrash.getLongitude())));
                                                // ??????????????? ????????? ????????? ??????.
                                                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                                                // ?????? ?????????.
                                                switch (customTrash.getKind())
                                                {
                                                    case "GENERAL":
                                                        customMarker.setCustomImageResourceId(R.drawable.trash_general_blue);
                                                        break;
                                                    case "RECYCLE":
                                                        customMarker.setCustomImageResourceId(R.drawable.trash_recycle_blue);
                                                        break;
                                                    case "SMOKING":
                                                        customMarker.setCustomImageResourceId(R.drawable.trash_smoking_blue);
                                                        break;
                                                }
                                                // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                customMarker.setCustomImageAutoscale(false);
                                                //?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                                                customMarker.setCustomImageAnchor(0.5f, 1.0f);

                                                mapView.addPOIItem(customMarker);
                                                mapView.removePOIItem(mapPOIItem);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    };
                                    thread.start();
                                    try {
                                        thread.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 1:
                                    Log.i(LOG_TAG,"????????????");
                                    Toast.makeText(context,"????????? ?????????????????????.",Toast.LENGTH_SHORT).show();
                                    break;
                                case 2:
                                    Log.i(LOG_TAG,"??????");
                                    break;
                            }
                        }
                    });
                }

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }


            @Override
            public void onFailure(Call<String> call, Throwable t) {
                //?????? ??????
                Log.e(LOG_TAG, t.getLocalizedMessage());
            }
        });
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    // ActivityCompat.requestPermissions??? ????????? ????????? ????????? ????????? ???????????? ?????????
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // ?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????????????????

            boolean check_result = true;


            // ?????? ???????????? ??????????????? ???????????????.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {
                Log.d(LOG_TAG, "start");
                //?????? ?????? ????????? ??? ??????
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
            }
        }
    }

    void checkRunTimePermission(){

        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED ) {

            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)


            // 3.  ?????? ?????? ????????? ??? ??????
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);

        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.

            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), REQUIRED_PERMISSIONS[0])) {

                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Toast.makeText(getActivity(), "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    //GPS_PROVIDER & NETWORK_PROVIDER ??? ???????????? ????????? ??????
    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.i(LOG_TAG, "onActivityResult : GPS ????????? ?????????");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
        //??????????????? ?????? ?????? ?????? ???????????? ?????? ????????? ???????????? AlertDialog ??????
        //??????????????? ???????????? ??? ?????? activity ??????
        UserInfo userInfo = (UserInfo)getActivity().getIntent().getSerializableExtra("userInfo");
        MapPoint.GeoCoordinate geoCoordinate = mapPoint.getMapPointGeoCoord();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("???????????? ????????????");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                Log.i(LOG_TAG,"???????????? ?????? Activity ??????");
                Intent intent = new Intent(getActivity(),CustomTrashAdd.class);
                intent.putExtra("latitude",geoCoordinate.latitude);
                intent.putExtra("longitude",geoCoordinate.longitude);
                intent.putExtra("userInfo",userInfo);
                startActivity(intent);

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                Log.i(LOG_TAG,"???????????? ?????? ??????");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Log.i(LOG_TAG,geoCoordinate.latitude+" "+geoCoordinate.longitude);
    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
            stopTracking();
    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    private void startTracking()
    {
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
    }

    private void stopTracking()
    {
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeadingWithoutMapMoving);

    }

}