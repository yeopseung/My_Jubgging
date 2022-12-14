package org.techtown.my_jubgging.together;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.techtown.my_jubgging.MainMenu;
import org.techtown.my_jubgging.R;
import org.techtown.my_jubgging.RegionPickerActivity;
import org.techtown.my_jubgging.UserInfo;
import org.techtown.my_jubgging.retrofit.RetrofitAPI;
import org.techtown.my_jubgging.retrofit.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NewpageActivity extends AppCompatActivity {
    UserInfo userInfo;

    /* View Reference */
    ImageButton backBtn;

    TextView regionBtn[];

    EditText titleText;
    EditText contentText;

    ImageButton removeBtn;
    TextView peopleNumText;
    ImageButton addBtn;

    Spinner genderSpinner;
    TextView dateBtn;
    TextView timeBtn;

    EditText placeText;
    EditText linkText;

    Button makeBtn;

    /* Instance Value */
    boolean isAmend;
    long boardId;

    Context context;
    int mainColor;

    int regionNum = 0;

    int lowerBound = 3;

    boolean isDateSet = false;
    int year;
    int month;
    int date;

    boolean isTimeSet = false;
    int hour;
    int min;

    Post post;

    private RetrofitAPI retrofitApi;

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    setRegionBtn(data);
                }
            }
    );

    private void setRegionBtn(Intent data) {
        regionNum = data.getIntExtra("regionCnt", 0);

        String get;
        String key[] = new String[3];
        key[0] = "region1";
        key[1] = "region2";
        key[2] = "region3";

        for (int i = 0; i < 3; ++i) {
            get = data.getStringExtra(key[i]);
            regionBtn[i].setText(get);
        }

        for (int i = 0; i < regionNum; ++i) {
            regionBtn[i].setBackgroundResource(R.drawable.rounded_rectangle);
            regionBtn[i].setVisibility(View.VISIBLE);
        }
        if (regionNum < 3) {
            regionBtn[regionNum].setBackgroundResource(R.drawable.rounded_rectangle_gray);
            regionBtn[regionNum].setVisibility(View.VISIBLE);

            for (int i = regionNum + 1; i < 3; ++i)
                regionBtn[i].setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() { super.onBackPressed(); }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_together_newpage);

        context = getApplicationContext();
        userInfo = (UserInfo)getIntent().getSerializableExtra("userInfo");

        retrofitApi = RetrofitClient.getApiService();

        post = new Post();
        regionBtn = new TextView[3];

        mainColor = context.getResources().getColor(R.color.main_color_4);

        setViewById();
        setOnClick();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.target_array, R.layout.spinner_item_list);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        genderSpinner.setAdapter(adapter);

        getContent();
    }

    private void setViewById() {
        backBtn = (ImageButton) findViewById(R.id.together_newpage_back_button);

        regionBtn[0] = (TextView) findViewById(R.id.together_newpage_region1);
        regionBtn[1] = (TextView) findViewById(R.id.together_newpage_region2);
        regionBtn[2] = (TextView) findViewById(R.id.together_newpage_region3);

        titleText = (EditText) findViewById(R.id.together_newpage_title_text);
        contentText = (EditText) findViewById(R.id.together_newpage_content_text);

        removeBtn = (ImageButton) findViewById(R.id.together_newpage_remove_people);
        peopleNumText = (TextView) findViewById(R.id.together_newpage_people_num);
        addBtn = (ImageButton) findViewById(R.id.together_newpage_add_people);

        genderSpinner = (Spinner) findViewById(R.id.together_newpage_gender_spinner);
        dateBtn = (TextView) findViewById(R.id.together_newpage_date_button);
        timeBtn = (TextView) findViewById(R.id.together_newpage_time_button);

        placeText = (EditText) findViewById(R.id.together_newpage_place);
        linkText = (EditText) findViewById(R.id.together_newpage_link);

        makeBtn = (Button) findViewById(R.id.together_newpage_make_button);
    }

    private void setOnClick() {
        /* 뒤로가기 버튼 */
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainMenu.class);
                startActivity(intent);
            }
        });

        /* 지역 선택 버튼 */
        for (int i = 0; i < 3; ++i) {
            regionBtn[i].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), RegionPickerActivity.class);

                    intent.putExtra("region1", regionBtn[0].getText().toString());
                    intent.putExtra("region2", regionBtn[1].getText().toString());
                    intent.putExtra("region3", regionBtn[2].getText().toString());
                    intent.putExtra("regionCnt", regionNum);

                    mStartForResult.launch(intent);
                }
            });
        }

        /* 인원 */
        // 인원 감소
        removeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                --post.peopleNum;

                if (post.peopleNum < lowerBound)
                    post.peopleNum = lowerBound;

                peopleNumText.setText(post.peopleNum + "명");
            }
        });
        // 인원 증가
        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ++post.peopleNum;

                if (post.peopleNum > 10)
                    post.peopleNum = 10;

                peopleNumText.setText(post.peopleNum + "명");
            }
        });

        /* 날짜 */
        dateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDate();
            }
        });


        /* 시간 */
        timeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTime(); }
        });

        makeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(checkIsSatisfy()) {
                    Toast.makeText(getApplicationContext(), "저장중...", Toast.LENGTH_SHORT).show();

                    if(buildPost());
                        savePost();
                }
            }
        });

    }

    private void getContent() {
        Intent data = getIntent();
        isAmend = data.getBooleanExtra("isAmend", false);

        if (isAmend) {
            boardId = data.getLongExtra("boardId", 0L);

            titleText.setText(data.getStringExtra("title"));
            contentText.setText(data.getStringExtra("content"));

            lowerBound = data.getIntExtra("lowerBound", 3);
            peopleNumText.setText(data.getIntExtra("peopleNum", lowerBound) + "명");

            placeText.setText(data.getStringExtra("place"));
            linkText.setText(data.getStringExtra("link"));
        }
    }

    private void showDate() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                year = y;
                month = m + 1;
                date = d;

                if (y != calendar.get(Calendar.YEAR))
                    dateBtn.setText(year + "년 " + month + "월 " + date + "일");
                else
                    dateBtn.setText(month + "월 " + date + "일");

                isDateSet = true;
            }
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        int textColor = ContextCompat.getColor(getApplicationContext(), R.color.text_color);

        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();

        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(textColor);
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(textColor);
    }

    private void showTime() {
        Calendar calendar = Calendar.getInstance();

        int style;

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int h, int m) {
                hour = h;
                min = m;

                if (m == 0)
                    timeBtn.setText(h + "시 ");
                else
                    timeBtn.setText(h + "시 " + m + "분");

                isTimeSet = true;
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

        timePickerDialog.show();
    }

    private boolean checkIsSatisfy() {
        if (regionNum <= 0)
            return customErrorToast("지역을 추가해 주세요");

        if (titleText.getText().length() <= 0)
            return customErrorToast("제목을 입력해 주세요");

        if (contentText.getText().length() <= 0)
            return customErrorToast("활동 내역을 입력해 주세요");

        if (!isDateSet)
            return customErrorToast("날짜를 선택해 주세요");

        if (!isTimeSet)
            return customErrorToast("시간을 선택해 주세요");

        if (placeText.getText().length() <= 0)
            return customErrorToast("장소를 입력해 주세요");

        if (linkText.getText().length() <= 0)
            return customErrorToast("오픈 채팅방 주소를 입력해 주세요");

        if (!(linkText.getText().toString().startsWith("https://open.kakao.com")))
            return customErrorToast("올바른 오픈 채팅방 주소를 입력해주세요");

        // Case : All Clear
        return true;
    }

    private boolean buildPost() {
        post.userId = Long.parseLong(userInfo.userId);

        post.region1 = regionBtn[0].getText().toString();
        post.region2 = regionBtn[1].getText().toString();
        post.region3 = regionBtn[2].getText().toString();

        post.title = titleText.getText().toString();
        post.content = contentText.getText().toString();
        post.place = placeText.getText().toString();

        String gender[] = { "All", "Male", "Female" };
        post.possibleGender = gender[genderSpinner.getSelectedItemPosition()];

        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy MM dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh mm");
        Calendar calendar = new GregorianCalendar(year, month - 1, date, hour, min);
        Date newDate = calendar.getTime();

        post.localDate = dataFormat.format(newDate);
        post.localTime = timeFormat.format(newDate);

        post.kakaoChatAddress = linkText.getText().toString();

        return true;
    }

    private boolean savePost() {
        if (!isAmend) {
            Call<Map<String, Long>> call = retrofitApi.postNewPosting(post);

            call.enqueue(new Callback<Map<String, Long>>() {
                @Override
                public void onResponse(Call<Map<String, Long>> call, Response<Map<String, Long>> response) {
                    if (!response.isSuccessful()) {
                        customErrorToast("Code : " + response.code() + response.message() + response.errorBody());
                        return;
                    }

                    Map<String, Long> data = response.body();
                    customErrorToast(data.get("boardId") + " ");

                    Toast.makeText(getApplicationContext(), "저장 성공!", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }

                @Override
                public void onFailure(Call<Map<String, Long>> call, Throwable t) {
                    customErrorToast("저장 실패...! 다시 시도해주세요!");
                }
            });
        }
        else {
            Call<Map<String, Long>> call = retrofitApi.amendPost(boardId, post);

            call.enqueue(new Callback<Map<String, Long>>() {
                @Override
                public void onResponse(Call<Map<String, Long>> call, Response<Map<String, Long>> response) {
                    if (!response.isSuccessful()) {
                        customErrorToast("Code : " + response.code() + response.message() + response.errorBody());
                        return;
                    }

                    Map<String, Long> data = response.body();
                    customErrorToast(data.get("boardId") + " ");

                    Toast.makeText(getApplicationContext(), "수정 성공!", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }

                @Override
                public void onFailure(Call<Map<String, Long>> call, Throwable t) {
                    customErrorToast("수정 실패...! 다시 시도해주세요!");
                }
            });
        }
        return true;
    }

    private boolean customErrorToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        return false;
    }
}
