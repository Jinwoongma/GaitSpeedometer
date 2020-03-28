package com.jw.stepcounter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jw.stepcounter.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    private SensorManager sensorManager;
    String strDir = Environment.getExternalStorageDirectory()+"/gyro";
    private float[] prev = {0f,0f,0f};
    private double x;
    private double y;
    private double z;
    private Menu menu;
    private TextView stepView;
    private TextView speedView;
    private int stepCount = 0;
    private float gaitSpeed = 0f;
    private float height = 0f;
    private float speedConstant = 0f;
    private static final int ABOVE = 1;
    private static final int BELOW = 0;
    private static int CURRENT_STATE = 0;
    private static int PREVIOUS_STATE = BELOW;
    private LineGraphSeries<DataPoint> rawData;
    private LineGraphSeries<DataPoint> lpData;
    private GraphView graphView;
    private GraphView graphView1;
    private GraphView combView;
    private int rawPoints = 0;
    private int sampleCount = 0;
    private long startTime;
    boolean SAMPLING_ACTIVE = true;
    private long streakStartTime;
    private long streakPrevTime;
    private float intervalTime;
    private long aboveStartTime, aboveEndTime;
    private int flag=0;
    //실제 걷는 중인지 판단
    boolean WALK_ACTIVE = false;
    //10초간 체크중인지 체크 내용
    boolean IS_STEP_CHECK = false;
    //체크시 더할 걸음수
    int checkCount = 0;
    //시작,종료,저장 버튼
    Button btn_start,btn_stop,btn_save;
    //입력 텍스트
    EditText text_filename, text_height;
    //시작여부
    boolean isStart = false;
    //저장가능여부
    boolean isSave = false;
    //파일명
    String strFilename = "";
    //데이터 베이스
    GyroDBAdapter gyroDBAdapter;
    //파일 이름
    String strMakeFileName="";
    long MillisecondTime,  TimeBuff, UpdateTime = 0L ;
    Handler savehandler =  new Handler();
    int Seconds, Minutes, MilliSeconds ;
    String strX="", strY="", strZ="", strTime="";
    Spinner spinner_sex;

    //퍼미션 관련
    private final int MY_PERMISSION_REQUEST_STORAGE = 100;
    /**
     * Application permission 목록, android build target 23
     */
    public static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            checkPermission(MANDATORY_PERMISSIONS);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 파일명 입력칸 생성
        text_filename = (EditText) findViewById(R.id.file_name);
        text_filename.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(text_filename.getWindowToken(), 0);    //hide keyboard
                    return true;
                }
                return false;
            }
        });

        // 신장 입력칸 생성
        text_height = (EditText) findViewById(R.id.height);
        text_height.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(text_filename.getWindowToken(), 0);    //hide keyboard
                    return true;
                }
                return false;
            }
        });

        // 성별 입력칸 생성
        spinner_sex = findViewById(R.id.spinner_sex);
        // 걸음수 텍스트 생성
        stepView = (TextView) findViewById(R.id.count);
        // 보행속도 텍스트 생성
        speedView = (TextView) findViewById(R.id.speed);

        // 실시간 그래프 생성 (raw data)
        rawData = new LineGraphSeries<>();
        rawData.setTitle("Raw Data");
        rawData.setColor(Color.RED);
        //lpData = new LineGraphSeries<>();
        //lpData.setTitle("Smooth Data");
        //lpData.setColor(Color.BLUE);
        graphView = (GraphView) findViewById(R.id.rawGraph);
        //graphView1 = (GraphView) findViewById(R.id.lpGraph);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(-40);
        graphView.getViewport().setMaxY(30);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(4);
        graphView.getViewport().setMaxX(80);
        // enable scaling and scrolling
//        graphView.getViewport().setScalable(true);
//        graphView.getViewport().setScalableY(true);
//        graphView.getViewport().setScrollable(true); // enables horizontal scrolling
//        graphView.getViewport().setScrollableY(true); // enables vertical scrolling
//        graphView.getViewport().setScalable(true); // enables horizontal zooming and scrolling
//        graphView.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        graphView.addSeries(rawData);

        // set manual X bounds
//        graphView1.getViewport().setYAxisBoundsManual(true);
//        graphView1.getViewport().setMinY(-30);
//        graphView1.getViewport().setMaxY(30);
//        graphView1.getViewport().setXAxisBoundsManual(true);
//        graphView1.getViewport().setMinX(4);
//        graphView1.getViewport().setMaxX(80);

        // enable scaling and scrolling
//        graphView1.getViewport().setScalable(true);
//        graphView1.getViewport().setScalableY(true);
//        graphView1.getViewport().setScrollable(true); // enables horizontal scrolling
//        graphView1.getViewport().setScrollableY(true); // enables vertical scrolling
//        graphView1.getViewport().setScalable(true); // enables horizontal zooming and scrolling
//        graphView1.getViewport().setScalableY(true); // enables vertical zooming and scrolling
//        graphView1.addSeries(lpData);
//
//        combView = (GraphView) findViewById(R.id.combGraph);
//        combView.getViewport().setYAxisBoundsManual(true);
//        combView.getViewport().setMinY(-70);
//        combView.getViewport().setMaxY(70);
//        combView.getViewport().setXAxisBoundsManual(true);
//        combView.getViewport().setMinX(4);
//        combView.getViewport().setMaxX(80);
//        combView.addSeries(rawData);
//        combView.addSeries(lpData);

        // 시작 버튼 생성
        btn_start = findViewById(R.id.btn_start);
        btn_start.setOnClickListener(this);
        // 중지 버튼 생성
        btn_stop = findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(this);
        // 저장 버튼 생성
        btn_save =findViewById(R.id.btn_save);
        btn_save.setOnClickListener(this);

        // 성별 선택칸 활성화
        initSpinnerSex();

        // DB 생성
        gyroDBAdapter = new GyroDBAdapter(this);
        gyroDBAdapter.open();

        // 초기 시간 설정
        streakPrevTime = System.currentTimeMillis() - 500;


    }


    public void initSpinnerSex(){
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View v = super.getView(position, convertView, parent);
                if (position == getCount()) {
                    ((TextView)v.findViewById(android.R.id.text1)).setText("");
                    ((TextView)v.findViewById(android.R.id.text1)).setHint(getItem(getCount())); //"Hint to be displayed"
                }
                return v;
            }

            @Override
            public int getCount() {
                return super.getCount()-1; // you dont display last item. It is used as hint.
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add("남성");
        adapter.add("여성");
        adapter.add("선택");

        spinner_sex.setAdapter(adapter);
        spinner_sex.setSelection(adapter.getCount()); //display hint
        spinner_sex.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(isStart){
                    btn_stop.performClick();
                    Toast.makeText(MainActivity.this, "변경으로 인해 기록이 종료됩니다.", Toast.LENGTH_SHORT).show();
                }
                switch (i) {
                    case 0:
                        speedConstant = 0.415f;
                        break;
                    case 1:
                        speedConstant = 0.413f;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }


    @TargetApi(23)
    private void checkPermission(String[] permissions) {
        requestPermissions(permissions, MY_PERMISSION_REQUEST_STORAGE);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.instrumentation) {
            //SAMPLING_ACTIVE = true;
            //sampleCount = 0;
            //startTime = System.currentTimeMillis();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        MenuItem rate = menu.findItem(R.id.instrumentation);
        if(view == btn_start){
            if(!isStart){
                if(text_filename.getText().toString().equals("")){
                    Toast.makeText(this, "파일명을 적은 후 해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    sensorManager.registerListener(this,
                            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                            20000);

                    strMakeFileName = makeFileName();
                    isSave = false;
                }
            }
            isStart=true;
            startTime = SystemClock.uptimeMillis();
            rate.setTitle("Running");
            SAMPLING_ACTIVE = true;
            sampleCount = 0;
            text_filename.setFocusable(false);
            text_height.setFocusable(false);
            height = Float.valueOf(text_height.getText().toString());
        }else if(view == btn_stop){
            if(isStart) {
                sensorManager.unregisterListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            }
            rate.setTitle("Stopped");
            isStart=false;
            isSave = true;
            text_filename.setFocusable(true);
            text_filename.setFocusableInTouchMode(true);
            text_height.setFocusable(true);
            text_height.setFocusableInTouchMode(true);
            stepCount = 0;
        }else if(view == btn_save){
            if(isSave) {
                saveFile();
            }else{
                Toast.makeText(this, "저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void handleEvent(SensorEvent event) {
        prev = lowPassFilter(event.values,prev);
        Accelerometer raw = new Accelerometer(event.values);
        Accelerometer data = new Accelerometer(prev);
        x=data.X;
        y=data.Y;
        z=data.Z;
        rawData.appendData(new DataPoint(rawPoints++,raw.R), true,1000);
        //lpData.appendData(new DataPoint(rawPoints, data.R), true, 1000);
        if(data.R > 10.0f){
            CURRENT_STATE = ABOVE;
            if(PREVIOUS_STATE != CURRENT_STATE) {
                streakStartTime = System.currentTimeMillis();
                aboveStartTime = System.currentTimeMillis();

                if (streakStartTime - streakPrevTime> 300f && CURRENT_STATE == ABOVE) {
                    intervalTime = streakStartTime - streakPrevTime;
                    streakPrevTime = streakStartTime;
                    flag = 1;

                    if(IS_STEP_CHECK){
                        checkCount++;
                    }else if(WALK_ACTIVE) {
                        Log.d("STATES:", "" + streakPrevTime + " " + streakStartTime+ " " + intervalTime+ " " + gaitSpeed);
                        stepCount++;
                        flag = 2;
                    }else{
                        IS_STEP_CHECK = true;
                        checkCount++;
                        handler.postDelayed(r,10000);
                        Log.d("STATES:", "ACTIVE 10초뒤"+checkCount );
                    }
                }else{
                    flag = 0;
                }
            }
            PREVIOUS_STATE = CURRENT_STATE;
        }
        else if(data.R < 10.0f) {
            CURRENT_STATE = BELOW;
            flag = 0;

            if(PREVIOUS_STATE != CURRENT_STATE) {
                aboveEndTime = System.currentTimeMillis();
                if(aboveEndTime-aboveStartTime >= 500f) {
                    intervalTime = 0f;
                    gaitSpeed = 0f;
                    if(IS_STEP_CHECK && checkCount>0){
                        checkCount--;
                    }else if(WALK_ACTIVE && stepCount>0){
                        stepCount--;
                    }
                }
            }

            if(streakStartTime+2000<=System.currentTimeMillis()){
                WALK_ACTIVE =false;
                Log.d("STATES:", "ACTIVE false" );
            }

            PREVIOUS_STATE = CURRENT_STATE;
        }

        stepView.setText("step: "+(stepCount));
        if(!WALK_ACTIVE){
            speedView.setText("Not enough step counts");
            intervalTime = 0f;
            gaitSpeed = 0f;
        }else{
            if (intervalTime == 0f){
                gaitSpeed = 0f;
            }else{
                gaitSpeed = (height/100f*speedConstant) / (intervalTime/1000f);
            }
            speedView.setText("gait speed(m/s): "+(gaitSpeed));
        }

    }

    Handler  handler = new Handler();
    //10초 이후 체크 관련
    Runnable r = new Runnable() {
        @Override
        public void run() {
            Log.d("STATES:", "ACTIVE check"+checkCount );
            if(checkCount>=10){
                WALK_ACTIVE =true;
                stepCount += checkCount;
                Log.d("STATES:", "ACTIVE true" );
            }
            checkCount = 0;
            IS_STEP_CHECK = false;
        }
    };

    private void saveFile() {

        Cursor cursor = gyroDBAdapter.fetchAllEntry(strMakeFileName);
        File f= new File(strDir+"/"+strMakeFileName);
        if(f.exists()) {
            f.delete();
        }
        try {
            writeSDcard(strMakeFileName,"X,Y,Z,TIME,flag,step,interval,speed"+"\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (cursor.moveToNext()){
            try {
                writeSDcard(strMakeFileName,cursor.getString(1)+","+cursor.getString(2)+","+cursor.getString(3)+","+cursor.getString(5)+","+cursor.getString(4)+","+cursor.getString(6)+","+cursor.getString(7)+","+cursor.getString(8)+"\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    /**
     *
     * @param fileName
     * @param content
     * @throws IOException , FileNotFoundException , Exception
     */
    public void writeSDcard(String fileName, String content) throws IOException, FileNotFoundException, Exception {
        File fdir =  new File(strDir);
        if(!fdir.exists()){
            fdir.mkdirs();
        }
        File file = new File(strDir + "/" + fileName);
        BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(file, true));
        buf.write(content.getBytes());

        buf.close();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleEvent(event);

            strX =Double.toString(x);
            strY =Double.toString(y);
            strZ =Double.toString(z);

            Log.e("LOG", "GYROSCOPE           [X]:" + String.format("%.4f", event.values[0])
                    + "           [Y]:" + String.format("%.4f", event.values[1])
                    + "           [Z]:" + String.format("%.4f", event.values[2])
                    + "           [flag]: " + String.format("%d", flag)
                    + "           [step]: " + String.format("%d", stepCount)
                    + "           [interval]: " + String.format("%.4f", intervalTime)
                    + "           [speed]: " + String.format("%.4f", gaitSpeed));

            savehandler.post(runnable);
        }
    }

    public Runnable runnable = new Runnable() {

        public void run() {
            MillisecondTime = SystemClock.uptimeMillis() - startTime;
            UpdateTime = TimeBuff + MillisecondTime;
            Seconds = (int) (UpdateTime / 1000);

            MilliSeconds = (int) (UpdateTime % 1000);

            strTime = String.format("%02d", Seconds) + "."
                    + String.format("%03d", MilliSeconds);

            //gyroDBAdapter.createEntry(strMakeFileName,strX+"",strY+"",strZ+"",strTime);
            gyroDBAdapter.createEntry(strMakeFileName,strX,strY,strZ,String.format("%d", flag),strTime,String.format("%d", stepCount),String.format("%.4f", intervalTime),String.format("%.4f", gaitSpeed));
        }

    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public String makeFileName(){
        strFilename = text_filename.getText().toString() ;
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        sb.append(sdf.format(new Date()));
        sb.append("_");
        sb.append(strFilename);
        sb.append(".csv");

        return sb.toString();
    }

    private float[] lowPassFilter(float[] input, float[] prev) {
        float ALPHA = 0.1f;
        if(input == null || prev == null) {
            return null;
        }
        for (int i=0; i< input.length; i++) {
            prev[i] = prev[i] + ALPHA * (input[i] - prev[i]);
        }
        return prev;
    }

}
