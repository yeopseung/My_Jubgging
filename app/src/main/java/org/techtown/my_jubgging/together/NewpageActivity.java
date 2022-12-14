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
        /* ???????????? ?????? */
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainMenu.class);
                startActivity(intent);
            }
        });

        /* ?????? ?????? ?????? */
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

        /* ?????? */
        // ?????? ??????
        removeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                --post.peopleNum;

                if (post.peopleNum < lowerBound)
                    post.peopleNum = lowerBound;

                peopleNumText.setText(post.peopleNum + "???");
            }
        });
        // ?????? ??????
        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ++post.peopleNum;

                if (post.peopleNum > 10)
                    post.peopleNum = 10;

                peopleNumText.setText(post.peopleNum + "???");
            }
        });

        /* ?????? */
        dateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDate();
            }
        });


        /* ?????? */
        timeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTime(); }
        });

        makeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(checkIsSatisfy()) {
                    Toast.makeText(getApplicationContext(), "?????????...", Toast.LENGTH_SHORT).show();

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
            peopleNumText.setText(data.getIntExtra("peopleNum", lowerBound) + "???");

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
                    dateBtn.setText(year + "??? " + month + "??? " + date + "???");
                else
                    dateBtn.setText(month + "??? " + date + "???");

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
                    timeBtn.setText(h + "??? ");
                else
                    timeBtn.setText(h + "??? " + m + "???");

                isTimeSet = true;
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

        timePickerDialog.show();
    }

    private boolean checkIsSatisfy() {
        if (regionNum <= 0)
            return customErrorToast("????????? ????????? ?????????");

        if (titleText.getText().length() <= 0)
            return customErrorToast("????????? ????????? ?????????");

        if (contentText.getText().length() <= 0)
            return customErrorToast("?????? ????????? ????????? ?????????");

        if (!isDateSet)
            return customErrorToast("????????? ????????? ?????????");

        if (!isTimeSet)
            return customErrorToast("????????? ????????? ?????????");

        if (placeText.getText().length() <= 0)
            return customErrorToast("????????? ????????? ?????????");

        if (linkText.getText().length() <= 0)
            return customErrorToast("?????? ????????? ????????? ????????? ?????????");

        if (!(linkText.getText().toString().startsWith("https://open.kakao.com")))
            return customErrorToast("????????? ?????? ????????? ????????? ??????????????????");

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

                    Toast.makeText(getApplicationContext(), "?????? ??????!", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }

                @Override
                public void onFailure(Call<Map<String, Long>> call, Throwable t) {
                    customErrorToast("?????? ??????...! ?????? ??????????????????!");
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

                    Toast.makeText(getApplicationContext(), "?????? ??????!", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }

                @Override
                public void onFailure(Call<Map<String, Long>> call, Throwable t) {
                    customErrorToast("?????? ??????...! ?????? ??????????????????!");
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
