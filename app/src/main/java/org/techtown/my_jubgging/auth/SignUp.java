package org.techtown.my_jubgging.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.kakao.usermgmt.response.model.Gender;

import org.techtown.my_jubgging.MainActivity;
import org.techtown.my_jubgging.MainMenu;
import org.techtown.my_jubgging.R;
import org.techtown.my_jubgging.UserInfo;
import org.techtown.my_jubgging.retrofit.RetrofitAPI;
import org.techtown.my_jubgging.retrofit.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SignUp extends AppCompatActivity {
    // 주소 요청코드 상수 requestCode
    private static final int SEARCH_ADDRESS_ACTIVITY = 10000;

    private UserInfo userInfo;
    private EditText name;
    private EditText nickName;
    private RadioGroup gender;
    private RadioButton male;
    private RadioButton female;
    private EditText email;
    private EditText address;
    private Button check;
    private Button finish;


    private boolean isChecked = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);


        Intent intent = getIntent();
        userInfo = (UserInfo) intent.getSerializableExtra("userInfo");


        name = findViewById(R.id.sign_up_name);
        nickName = findViewById(R.id.sign_up_nickName);
        gender = findViewById(R.id.sign_up_gender);
        male = findViewById(R.id.sign_up_male);
        female = findViewById(R.id.sign_up_female);
        email = findViewById(R.id.sign_up_email);
        address = findViewById(R.id.sign_up_address);
        check = findViewById(R.id.sign_up_check);
        finish = findViewById(R.id.sign_up_finish);


        //유저의 카카오 계정 정보를 이용하여 초기값 설정
        //이름 (초기값은 프로필 닉네임으로)
        name.setText(userInfo.getNickName());

        //닉네임 (초기값은 프로필 닉네임으로)
        nickName.setText(userInfo.getNickName());
        nickName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 입력난에 변화가 있을 때
                // 중복체크한 뒤 닉네임을 변경하는 것을 방지
                isChecked = false;
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // 입력이 끝났을 때 조치
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 입력하기 전에 조치
            }
        });

        //성별
        if(userInfo.getGender().equals("MALE"))
        {
            male.setChecked(true);
            male.setEnabled(false);
            female.setEnabled(false);
        }
        else if(userInfo.getGender().equals("FEMALE"))
        {
            female.setChecked(true);
            male.setEnabled(false);
            female.setEnabled(false);
        }

        //이메일
        email.setText(userInfo.getEmail());

        //닉네임중복체크 버튼
        check.setOnClickListener(new ButtonClickListener());
        finish.setOnClickListener(new ButtonClickListener());

        //주소
        // 터치 안되게 막기
        address.setFocusable(false);
        // 주소입력창 클릭
        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("주소설정페이지", "주소입력창 클릭");
                int status = NetworkStatus.getConnectivityStatus(getApplicationContext());
                if(status == NetworkStatus.TYPE_MOBILE || status == NetworkStatus.TYPE_WIFI) {

                    Log.i("주소설정페이지", "주소입력창 클릭");
                    Intent i = new Intent(getApplicationContext(), AddressAPI.class);
                    // 화면전환 애니메이션 없애기
                    overridePendingTransition(0, 0);
                    // 주소결과
                    startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);

                }else {
                    Toast.makeText(getApplicationContext(), "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                }


            }
        });

    }

    class ButtonClickListener implements Button.OnClickListener {
        Retrofit retrofit = RetrofitClient.getInstance();
        RetrofitAPI retrofitAPI = RetrofitClient.getApiService();
        @Override
        public void onClick(View view) {

            switch (view.getId()) {

                case R.id.sign_up_check:
                    //닉네임 중복 여부 판단
                    Call<String> call_nickname = retrofitAPI.checkNickName(nickName.getText().toString());
                    call_nickname.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            //통신 실패
                            if (!response.isSuccessful()) {
                                Log.e("SignUp_check_NickName_F",String.valueOf(response.code()));
                                return;
                            }

                            String result  = response.body();

                            //중복된 닉네임일 경우
                            if(result.equals("{\"nickName\":\"Y\"}"))
                            {
                                Log.i("SignUp_check_NickName_Y",result);
                                Toast.makeText(getApplicationContext(),"이미 존재하는 닉네임입니다.",Toast.LENGTH_SHORT).show();
                            }
                            else if(result.equals("{\"nickName\":\"N\"}"))
                            {
                                Log.i("SignUp_check_NickName_N",result);
                                Toast.makeText(getApplicationContext(),"사용가능한 닉네임입니다.",Toast.LENGTH_SHORT).show();
                                isChecked = true;
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            //통신 실패
                            Log.e("SignUp_check_NickName_F",t.getLocalizedMessage());
                        }

                    });
                    break ;
                case R.id.sign_up_finish:
                    //입력받은 값들이 올바르게 입력되었을 경우 회원가입 완료
                    if(checkInput())
                    {
                        //입력받은 값 (이름, 닉네임, 주소) 갱신
                        userInfo.setName(name.getText().toString());
                        userInfo.setNickName(nickName.getText().toString());
                        userInfo.setAddress(address.getText().toString());

                        //회원정보 POST
                        Call<String> call_userinfo = retrofitAPI.createUserInfo(userInfo);
                        call_userinfo.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                //통신 실패
                                if (!response.isSuccessful()) {
                                    Log.e("SignUp_Post_UserInfo_F",String.valueOf(response.code()));
                                    return;
                                }

                                String result  = response.body();
                                Log.i("SignUp_Post_UserInfo_T",result);
                                Intent intent = new Intent(SignUp.this, MainMenu.class);
                                intent.putExtra("userInfo",userInfo);
                                startActivity(intent);
                                finish();

                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                //통신 실패
                                Log.e("SignUp_Post_UserInfo_F",t.getLocalizedMessage());
                            }

                        });
                    }

                    break;
            }
        }
    }


    //입력받은 값들이 올바른지 체크하는 메소드
    private boolean checkInput()
    {
        //이름을 입력했는지 체크
        if(name.getText().toString().replace(" ", "").equals(""))
        {
            Toast.makeText(getApplicationContext(),"이름을 입력하세요.",Toast.LENGTH_SHORT).show();
            return false;
        }

        //닉네임을 입력했는지 체크
        if(nickName.getText().toString().replace(" ", "").equals(""))
        {
            Toast.makeText(getApplicationContext(),"닉네임을 입력하세요.",Toast.LENGTH_SHORT).show();
            return false;
        }

        //닉네임 중복체크를 했는지 체크
        if(!isChecked)
        {
            Toast.makeText(getApplicationContext(),"닉네임 중복체크를 하셔야합니다.",Toast.LENGTH_SHORT).show();
            return false;
        }



        return true;
    }

}