package org.techtown.my_jubgging.retrofit;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


//Retrofit 객체를 제작하는 클래스
//1->2 순서로 사용
public class RetrofitClient {

    //BASE_URL :  "ipv4" + ":8080" 로 조합하여 사용
    private static final String BASE_URL = "http://10.0.2.2:8080";

    //1.Retrofit 객체 제작 메소드
    public static Retrofit getInstance(){
        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }


    //2.RetrofitAPI 와 연결하는 메소드
    public static RetrofitAPI getApiService()
    {
        return getInstance().create(RetrofitAPI.class);
    }



}