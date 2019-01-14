package com.example.jeyun.garam_main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //MODE
    public enum MODE_STATE {
        Calculate, Calibration, Initialization, WaitingMove, DR
    }

    final int k = 7;

    //GUI
    MapView mapViewFragment;
    TextView temptext;

   // MySQLiteOpenHelper mySQLiteOpenHelper_third;
  //  DBHelper dbHelper_third;
    // db 배정.
    SQLiteDatabase third_mDB;
    String name1;
    int[] pos_arr;
    double[] in_mag_arr;

    //Sensor
    private SensorManager sensorManager;
    private TextView count;
    private TextView number;
    private int num;
    private int count_num = 0;
    private int count_temp;
    private int matchingPNM = 0;

    //Variable
    int direction_frombt = 0; // 0 : UP, 1 : RIGHT, 2 : DOWN, 3 : LEFT
    Activity thisActivity = this;

    private accSensor SDhandler;
    private gyroSensor GShandler;

    // for magnetic
    private float[] magneticValue;

    //경로 탐색에 사용할 배열
    private ArrayList<Float> arr_mag_X = new ArrayList<Float>();
    private ArrayList<Float> arr_mag_Y = new ArrayList<Float>();
    private ArrayList<Float> arr_mag_Z = new ArrayList<Float>();



    private ArrayList<Integer[]> locationNode = new ArrayList<Integer[]>();
    private ArrayList<Integer[]> candidate1 = new ArrayList<Integer[]>();
    private ArrayList<Integer[]> candidate2 = new ArrayList<Integer[]>();
    private ArrayList<Integer[]> candidate3 = new ArrayList<Integer[]>();
    private float prevStepCount = 0;
    private int searchState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        in_mag_arr = new double[110];
        temptext = (TextView) findViewById(R.id.textView_temp);
        pos_arr = new int[3];
/*
//뒤로가기 버튼 생성
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

// DB 이름 배정
        name1 = "new_third.db";
        // name1 = "pdr_kalman_third.db";
//DB 값 복사 후 앱에 저장 //새로운 지도 = 새로운 helper ==> 추후 수정
        mySQLiteOpenHelper_third = new MySQLiteOpenHelper(getApplicationContext(), name1);
//DB 사용 등록
        dbHelper_third = new DBHelper(getApplicationContext(), name1, null, 1);
//sql db 배정
        third_mDB = dbHelper_third.getReadableDatabase();
*/
//Sensor
        num = 0;
        count_num = 0;
        count_temp = 0;
        //count = (TextView) findViewById(R.id.count);
        //number = (TextView) findViewById(R.id.number);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        SDhandler = new accSensor(sensorManager, (MapView) getSupportFragmentManager().findFragmentById(R.id.fragMapView));
        GShandler = new gyroSensor(sensorManager);

        mapViewFragment = (MapView) getSupportFragmentManager().findFragmentById(R.id.fragMapView);
        textView_state = (TextView) findViewById(R.id.textView_state);
        textView_temp = (TextView) findViewById(R.id.textView_temp);
        q_mag_x = new float[QSIZE];
        q_mag_z = new float[QSIZE];
        q_mag_2 = new float[QSIZE];
/*
        //DB값 load
        third_mDB = dbHelper_third.getReadableDatabase();
        Cursor cursor = third_mDB.rawQuery("SELECT * FROM data", null);
        while (cursor.moveToNext()) {
            arr_mag_X.add(cursor.getFloat(1));
            arr_mag_Y.add(cursor.getFloat(2));
            arr_mag_Z.add(cursor.getFloat(3));
            locationNode.add( new Integer[] {cursor.getInt(4), cursor.getInt(5), cursor.getInt(6)} );
        }

        Log.i("MainAct", "arr_mag_Size : " + arr_mag_X.size());
    }

    //ActionBar menu 생성
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gps, menu);
        return super.onCreateOptionsMenu(menu);
    }


    //메뉴 버튼 선택 시
    public boolean onOptionsItemSelected(MenuItem item) {
//GeoMap 버튼 클릭 시
        if (item.getItemId() == R.id.GeoMagMap) {
            Intent intent = new Intent(MainActivity.this, GeoMagMapActivity.class);
            intent.putExtra("DIRECTION", direction_frombt);
            intent.putExtra("DBNAME", name1);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
        */
    }

    @Override
    public void onBackPressed() {
        mapViewFragment.moveMap.clearExpectedCircle();
        AlertDialog.Builder alert_exit = new AlertDialog.Builder(this);
        alert_exit.setTitle("종료확인");
        alert_exit.setMessage("종료하시겠습니까?");
        alert_exit.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alert_exit.setPositiveButton("종료", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ActivityCompat.finishAffinity(thisActivity);
                System.runFinalizersOnExit(true);
                System.exit(0);
            }
        });
        AlertDialog alert = alert_exit.create();
        alert.show();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updateThread();
        }
    };

    private void updateThread() {//GUI 분리
        num++;
        number.setText(String.valueOf(num));

        if (CURRENT_STATE == MODE_STATE.Calibration) {
            textView_state.setText("Calibration 중입니다.");
        } else if (CURRENT_STATE == MODE_STATE.Calculate) {

        } else if (CURRENT_STATE == MODE_STATE.Initialization) {
            textView_state.setText("초기화 중입니다.");
        } else if (CURRENT_STATE == MODE_STATE.WaitingMove) {
            textView_state.setText("움직임을 기다립니다.");
        } else if (CURRENT_STATE == MODE_STATE.DR) {
            textView_state.setText("est x: " + m_stateEstimate_x + " est z : " + m_stateEstimate_z);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Thread myThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        handler.sendMessage(handler.obtainMessage());
                        Thread.sleep(1000);
                    } catch (Throwable t) {
                    }
                }
            }
        });

        myThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();


        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Magnetic sensor not available!", Toast.LENGTH_LONG).show();
        }

        SDhandler.start();
        GShandler.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // if you unregister the last listener, the hardware will stop detecting step events
        SDhandler.stop();
        GShandler.stop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        magneticValue = new float[3];
        System.arraycopy(sensorEvent.values, 0, magneticValue, 0, sensorEvent.values.length);
        MagneticSensing();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void MagneticSensing() {

        System.arraycopy(magneticValue, 0, magnetic, 0, magneticValue.length);//
        if (CURRENT_STATE == MainActivity.MODE_STATE.Calibration) {
            push_mag_Q(magnetic[X_AXIS], magnetic[Y_AXIS], magnetic[2]);
            if (isFull_mag()) {
                CURRENT_STATE = MODE_STATE.Calculate;//값을 잠시 흘린다
                calibration_mode();
            }
        } else if (CURRENT_STATE == MODE_STATE.Initialization) {
            initialization_mode();
        } else if (CURRENT_STATE == MODE_STATE.DR)//initialization 함수에서 넘어옴
            dR_mode();
    }
/*
    public void onClickButtion(View v) {
        switch (v.getId()) {
            case R.id.btDirection_mainAct:
                Log.i("buttonAction", "Direction Changed");
                Button btDirection = findViewById(R.id.btDirection_mainAct);
                if (direction_frombt == 0) {
//UP->RIGHT
                    btDirection.setText("RIGHT");
                } else if (direction_frombt == 1) {
//RIGHT->DOWN
                    btDirection.setText("DOWN");
                } else if (direction_frombt == 2) {
//DOWN->LEFT
                    btDirection.setText("LEFT");
                } else if (direction_frombt == 3) {
//LEFT->UP
                    btDirection.setText("UP");
                }
                direction_frombt = (direction_frombt + 1) % 4;
                mapViewFragment.changeDirecton(90);
                break;
            case R.id.btUp:
                Log.i("buttonAction", "Up pressed");
                mapViewFragment.buttonPressed("UP");
                break;
            case R.id.btDown:
                Log.i("buttonAction", "Down pressed");
                mapViewFragment.buttonPressed("DOWN");
                break;
            case R.id.btLeft:
                CURRENT_STATE = MODE_STATE.Initialization;
                Log.i("buttonAction", "Left pressed");
                mapViewFragment.buttonPressed("LEFT");
                break;
            case R.id.btRight:
                Log.i("buttonAction", "Right pressed");
                mapViewFragment.buttonPressed("RIGHT");
                break;
        }
    }
*/
    //GUI
    TextView textView_state;
    TextView textView_temp;

    public static MODE_STATE CURRENT_STATE = MODE_STATE.Calibration;

    int head_mag = 0; // circular, head == rear : 비었다/ head + 1 == rear : 가득 찼다
    int rear_mag = 0;

    private final int QSIZE = 30;   // Qsize. calibration 시간에 영향 + cal 시 처음 x개는 흘린다 *
    private final int THROW_SENSORVALUE_INDEX = 10;
    float[] q_mag_x; //초기값 측정용 moving average 구하기
    float[] q_mag_z;
    float[] q_mag_2; // for magnetic[2]
    float HEADDETECT_STAND = 2.0f;  // 방향 변경 감지 민감도
    float HEADDETECT_MOVE = 8.5f;
    float DETECT_ALARM = 26.0f;  //calibration 해야할 민감도
    float DETECT_AMBIGUOUS = 30.0f; //제곱값 ~ 방향 애매할때 민감도
    float DETECT_CHANGE_GYRO = 35.0f;  // 방향 변경 감지 민감도 - gyro

    final int X_AXIS = 0;
    final int Y_AXIS = 1;
    final int Z_AXIS = 2;

    float front_x, front_z; //calibration 시 보고 있던 방향 자기장 값

    //다중 센서 사용하기 위해// 값 받는 통
    private float[] magnetic = new float[3];

    //for kalman 초기화
    float m_stateEstimate_x = 0.0f; //초기값 문제 -> 초기 cal 단계에서 moving average
    float m_stateEstimate_z = 0.0f;
    float m_stateEstimate_2 = 0.0f;

    public void calibration_mode() {
        // TODO : 추후에 지도 또는 사람 돌리는걸 결정할 부분
        //cal 후에는 반드시 정면을 향한다.
        int before_direction_frombt = direction_frombt;
        mapViewFragment.changeDirecton((4 - before_direction_frombt) * 90);
        direction_frombt = 0;

        candidate1.clear();
        candidate2.clear();
        searchState = 0;    // 주의


//Q가 찰 때가지 얻은 측정값으로 평균, 분산을 구한다. 평균은 true라고 생각한다.
//초기값 THROW_SENSORVALUE_INDEX개는 버린다
        float[] current_mag_x = new float[q_mag_x.length - THROW_SENSORVALUE_INDEX];
        float[] current_mag_z = new float[q_mag_z.length - THROW_SENSORVALUE_INDEX];
        float[] current_mag_2 = new float[q_mag_2.length - THROW_SENSORVALUE_INDEX];
        System.arraycopy(q_mag_x, (head_mag + THROW_SENSORVALUE_INDEX) % QSIZE, current_mag_x, 0, q_mag_x.length - THROW_SENSORVALUE_INDEX);
        System.arraycopy(q_mag_z, (head_mag + THROW_SENSORVALUE_INDEX) % QSIZE, current_mag_z, 0, q_mag_z.length - THROW_SENSORVALUE_INDEX);
        System.arraycopy(q_mag_2, (head_mag + THROW_SENSORVALUE_INDEX) % QSIZE, current_mag_2, 0, q_mag_z.length - THROW_SENSORVALUE_INDEX);
        //현재 방향이 Front이다 - !
        front_x = m_stateEstimate_x = cal_Average(current_mag_x);
        front_z = m_stateEstimate_z = cal_Average(current_mag_z);
        m_stateEstimate_2 = cal_Average(current_mag_2); //front가 있을 필요 없다.

        CURRENT_STATE = MODE_STATE.Initialization;  //go to next mode - initalization
    }

    //방향 전환 감지때마다 스텝수 초기화 ?
    public void initialization_mode()//초기화
    {//for accelerometer
        SDhandler.clearPreviousRunningTime();
        SDhandler.clearRunningAcc();
        //for gyroscope
        GShandler.clearTotalOmega();
        //최초 0, 0
        Log.i("initial test", "arr[0] : " + pos_arr[0] + ", arr[1] : " + pos_arr[1]);
        mapViewFragment.moveMap.MU.setInitialPos(pos_arr[0], pos_arr[1]);

        Log.i("initial test", "result Pos_X : " + mapViewFragment.moveMap.MU.positionX + ", result Pos_Y : " + mapViewFragment.moveMap.MU.positionY);
        CURRENT_STATE = MODE_STATE.WaitingMove;  //go to next mode
    }

    public void dR_mode() {
        uni_kalmanFilter(magnetic[X_AXIS], magnetic[Y_AXIS], magnetic[2]);
        detect_change(magnetic[X_AXIS], magnetic[Y_AXIS]);  //방향 탐지 중!

        pathSearching();

        if (count_num > count_temp + 1) {
            update_front(magnetic[X_AXIS], magnetic[Y_AXIS]);
            count_temp = count_num;
        }
    }

    //반드시 원소 개수가 k개 이상이여야해

    static int CONSTANT_STEP_TERM = 1;

    public synchronized void pathSearching() {
        int i, j;
        switch (searchState) {
            case 0:    //첫번째 데이터 입력 경우
                //searchState : 0 == 첫번째 입력, 1 == 두번째 입력 대기, 2== 두번째 입력.
                if (candidate1.size() <= 0 && prevStepCount != count_num) {
                    prevStepCount = count_num;  //이전 스탭 수 기억
                    for (i = 0; i < arr_mag_X.size(); i++) {
                        //현재 지자기 값과 DB를 비교하여 유사한 지점을 Check, 이후 후보 목록에 추가
                        if (checkCorrectLocation(arr_mag_X.get(i), arr_mag_Y.get(i), arr_mag_Z.get(i))) {
                            candidate1.add(new Integer[] {locationNode.get(i)[0], locationNode.get(i)[1], locationNode.get(i)[2]});
                        }
                    }
                }

                double[][] temp = new double[candidate1.size()][3];
                for(i=0; i<candidate1.size();i++) {
                    temp[i][0] = candidate1.get(i)[0];
                    temp[i][1] = candidate1.get(i)[1];
                    temp[i][2] = candidate1.get(i)[2];
                }
/*
                //input 개수 이상이 되면 ERROR / 3x2행렬 되는 확인
                if(candidate1.size() > k)
                {
                    Array2DRowRealMatrix mat = new Array2DRowRealMatrix(temp);
                    KMedoids km2 = new KMedoidsParameters(k).fitNewModel(mat); //이거써
                    ArrayList<double[]> temp2 = km2.getCentroids(); //소속 그룹을 대표하는 값을 리턴  ex) (1, 1)
                    candidate1.clear();
                    for(i = 0; i < temp2.size(); i++)
                    {
                        candidate1.add(new Integer[] {(int)temp2.get(i)[0], (int)temp2.get(i)[1], (int)temp2.get(i)[2]});
                    }
                }
                */
                //ideal case : candidate1이 1개만 들어있어서 위치를 바로 찾았다!
                if (candidate1.size() == 1) {
                    mapViewFragment.moveMap.clearExpectedCircle();
                    System.out.println("Marker pos : " + candidate1.get(0)[0] + ", " + candidate1.get(0)[1]);
                    mapViewFragment.moveMap.MU.MarkerX = (float) candidate1.get(0)[0];
                    mapViewFragment.moveMap.MU.MarkerY = (float) candidate1.get(0)[1];
                    changeDirection( candidate1.get(0)[2]);
                    mapViewFragment.addExpectedCircle(mapViewFragment.moveMap.MU.MarkerX, mapViewFragment.moveMap.MU.MarkerY);
                    mapViewFragment.drawImage();
                    searchState = 1;
                    return;
                }
                searchState = 1;
                break;
            case 1:
                if (searchState == 1 && count_num - prevStepCount > CONSTANT_STEP_TERM && CURRENT_STATE == MODE_STATE.DR) { // for Performance
                    //prevStepCount = count_num; //이거 넣으면 뻗음
                    System.out.println("Search_state = 1");
                    searchState = 2;
                }
                break;
            case 2:  //첫/두번째 입력일 경우
                System.out.println("Search_state = 2");
                //전체 좌표 중에서 코사인정확도가 70%이상, RMS거리차가 ~이하로 Matching이 되는 지점을 List에 넣는다.
                for (i = 0; i < arr_mag_X.size(); i++) {
                    //현재 지자기 값과 DB를 비교하여 유사한 지점을 Check, 이후 후보 목록에 추가
                    if (checkCorrectLocation(arr_mag_X.get(i), arr_mag_Y.get(i), arr_mag_Z.get(i))) {
                        candidate2.add(new Integer[] {locationNode.get(i)[0], locationNode.get(i)[1], locationNode.get(i)[2]});
                    }
                }

                double[][] temp_can2 = new double[candidate2.size()][3];
                for(i=0; i<candidate2.size();i++) {
                    temp_can2[i][0] = candidate2.get(i)[0];
                    temp_can2[i][1] = candidate2.get(i)[1];
                    temp_can2[i][2] = candidate2.get(i)[2];
                }
/*
                if(candidate2.size() > k) // 다수에서 k개 이하로
                {
                    Array2DRowRealMatrix mat = new Array2DRowRealMatrix(temp_can2);
                    KMedoids km2 = new KMedoidsParameters(k).fitNewModel(mat);
                    ArrayList<double[]> temp2 = km2.getCentroids();
                    candidate2.clear();
                    for(i = 0; i < temp2.size(); i++)
                    {
                        System.out.println("k-mean value2 : " + temp2.get(i)[0] + ", " + temp2.get(i)[1]);
                        candidate2.add(new Integer[] {(int)temp2.get(i)[0], (int)temp2.get(i)[1], (int)temp2.get(i)[2]});
                    }

                }
*/
                if (candidate2.size() == 1) { //ideal case : candidate2이 1개만 들어있어서 위치를 바로 찾았다!
                    mapViewFragment.moveMap.clearExpectedCircle();
                    System.out.println("Marker pos : " + candidate2.get(0)[0] + ", " + candidate2.get(0)[1] + ", " + candidate2.get(0)[2]);
                    mapViewFragment.moveMap.MU.MarkerX = (float) candidate2.get(0)[0];
                    mapViewFragment.moveMap.MU.MarkerY = (float) candidate2.get(0)[1];
                    changeDirection( candidate2.get(0)[2]);
                    mapViewFragment.addExpectedCircle(mapViewFragment.moveMap.MU.MarkerX, mapViewFragment.moveMap.MU.MarkerY);
                    mapViewFragment.drawImage();
                    candidate1 = candidate2;
                    candidate2.clear();
                    searchState = 1;
                    return;
                }

                ArrayList<double[]> tempArray = new ArrayList<double[]>();
                ArrayList<double[]> tempArray2 = new ArrayList<double[]>();
                ArrayList<Double> tempLengthArray = new ArrayList<Double>();
                ArrayList<Double> tempLengthArray2 = new ArrayList<Double>();
                //이전 후보와 이번 후보를 비교 (i_x, i_y, j_x, j_y, 세타)
                System.out.println("count = " + candidate1.size() + ", " + candidate2.size());
                for (i = 0; i < candidate1.size(); i++) {
                    for (j = 0; j < candidate2.size(); j++)
                    {
                        tempArray.add(new double[]{candidate1.get(i)[0], candidate1.get(i)[1],
                                candidate2.get(j)[0], candidate2.get(j)[1], candidate2.get(j)[2]});
                        tempLengthArray.add(Math.pow(candidate1.get(i)[0] - candidate2.get(j)[0], 2) +
                                Math.pow(candidate1.get(i)[1] - candidate2.get(j)[1], 2));
                    }
                }
                //sorting
                if (tempLengthArray.size() == 0 ) {
                    System.out.println("잘못된 경우, 초기화");
                    candidate1.clear();
                    candidate2.clear();
                    searchState = 0;
                    return;
                }
                for ( ; tempLengthArray.size() > 0; ) {//sort
                    int index = tempLengthArray.indexOf(Collections.min(tempLengthArray));
                    tempArray2.add(tempArray.get(index));
                    tempLengthArray2.add(tempLengthArray.get(index));
                    tempLengthArray.remove(index);
                    tempArray.remove(index);
                }
                if(tempLengthArray2.get(0) < 3 && tempLengthArray2.get(0) != 0)//이전값이 정확하다는 가정이 있을때.
                {
                    //mapViewFragment.moveMap.clearExpectedCircle();
                    mapViewFragment.moveMap.MU.MarkerX = (float) tempArray2.get(0)[2];
                    mapViewFragment.moveMap.MU.MarkerY = (float) tempArray2.get(0)[3];
                    changeDirection( (int)tempArray2.get(0)[4]);
                    mapViewFragment.addExpectedCircle(mapViewFragment.moveMap.MU.MarkerX, mapViewFragment.moveMap.MU.MarkerY);
                    mapViewFragment.drawImage();
                    candidate2.clear();
                    candidate1.clear();
                    candidate1.add(new Integer[] {(int) tempArray2.get(0)[2], (int) tempArray2.get(0)[3] , (int) tempArray2.get(0)[4]});
                    searchState = 1;
                    return;
                }                //한 값이 유난히 작은 값인가? 참고-> 거리:root 안씌운 값
                else if (tempLengthArray2.get(1) - tempLengthArray2.get(0) > 9) {
                    //mapViewFragment.moveMap.clearExpectedCircle();
                    mapViewFragment.moveMap.MU.MarkerX = (float) tempArray2.get(0)[2];
                    mapViewFragment.moveMap.MU.MarkerY = (float) tempArray2.get(0)[3];
                    changeDirection((int)tempArray2.get(0)[4]);
                    mapViewFragment.addExpectedCircle(mapViewFragment.moveMap.MU.MarkerX, mapViewFragment.moveMap.MU.MarkerY);
                    mapViewFragment.drawImage();
                }
                else
                    //원그리기
                    for (i = 0; i < candidate2.size(); i++) {
                        mapViewFragment.addExpectedCircle(candidate2.get(i)[0],candidate2.get(i)[1]);
                    }
                mapViewFragment.drawImage();

                //이후 반복
                //candiate1값을 2로 교체하고, candiate2를 비워준다. //바로 직전 값만 비교에 사용할 것이기 때문
                candidate1.clear();
                candidate1.add(new Integer[] {(int) tempArray2.get(0)[2], (int) tempArray2.get(0)[3], (int) tempArray2.get(0)[4]});//항상 직전값만
                candidate2.clear();
                searchState = 0;
                prevStepCount = count_num;
                break;
        }
    }

    static double CONSTANT_COS_SIMILARITY = 0.92;
    static double CONSTANT_RMS = 9;

    //지자기 값을 비교, 유사한 위치인지 Check
    public boolean checkCorrectLocation(float x, float y, float z) {//보정된 값을 넣어준다
        double cossimilarity = ((m_stateEstimate_x * x) + (m_stateEstimate_z * y) + (m_stateEstimate_2 * z))
                / (Math.sqrt((m_stateEstimate_x * m_stateEstimate_x)
                + (m_stateEstimate_z * m_stateEstimate_z)
                + (m_stateEstimate_2 * m_stateEstimate_2)) * Math.sqrt((x * x) + (y * y) + (z * z)));
        //Cosine Similiarity Check
        if (cossimilarity < CONSTANT_COS_SIMILARITY) {
            //코사인유사도가 특정 수보다 작으면, 해당 위치가 아니라고 판단
            return false;
        }
        //RMS Check
        double rmsValue = Math.sqrt((((x - m_stateEstimate_x) * (x - m_stateEstimate_x))
                + ((y - m_stateEstimate_z) * (y - m_stateEstimate_z)) + ((z - m_stateEstimate_2) * (z - m_stateEstimate_2))) / 3);

        //DataSheet참조하여 예정한 수, 정확도 조정을 하고싶으면 언제든지 변경할 것
        if (rmsValue > CONSTANT_RMS) {
            //편차가 특정 수보다 크면, 해당 위치가 아니라고 판단
            return false;
        }
        System.out.println("rms = " + rmsValue);
        return true;
    }

    float var_noise_x = 1;//0이 아닌 값
    float var_noise_z = 1;//0이 아닌 값
    float var_noise_2 = 1;//0이 아닌 값
    float MEASURE_NOISE_VAR = 2f; //가정
    float PE_NOISE_VAR = 10f; //임이로

    public void uni_kalmanFilter(float meas_x, float meas_z, float meas_2) {
        var_noise_x = var_noise_x + PE_NOISE_VAR;
        float kal = var_noise_x / (var_noise_x + MEASURE_NOISE_VAR);
        m_stateEstimate_x = m_stateEstimate_x + kal * (meas_x - m_stateEstimate_x);
        var_noise_x = (1 - kal) * var_noise_x;

        var_noise_z = var_noise_z + PE_NOISE_VAR;
        kal = var_noise_z / (var_noise_z + MEASURE_NOISE_VAR);
        m_stateEstimate_z = m_stateEstimate_z + kal * (meas_z - m_stateEstimate_z);
        var_noise_z = (1 - kal) * var_noise_z;

        var_noise_2 = var_noise_2 + PE_NOISE_VAR;
        kal = var_noise_2 / (var_noise_2 + MEASURE_NOISE_VAR);
        m_stateEstimate_2 = m_stateEstimate_2 + kal * (meas_2 - m_stateEstimate_2);
        var_noise_2 = (1 - kal) * var_noise_2;
    }

    //방향 감지 / gUI 이미지 처리 /
    void detect_change(float meas_x, float meas_z) {
        float threshold = (float) Math.sqrt((m_stateEstimate_x - meas_x) * (m_stateEstimate_x - meas_x)
                + (m_stateEstimate_z - meas_z) * (m_stateEstimate_z - meas_z));
        if (threshold > DETECT_ALARM) {//recalibration
            textView_temp.setText("값이 튀어서 cal 모드로 진입");
            flush_Q();
            CURRENT_STATE = MODE_STATE.Calibration;
        } else if (((threshold > HEADDETECT_STAND) && (CURRENT_STATE == MODE_STATE.WaitingMove)
                ||
                ((threshold > HEADDETECT_MOVE) && (CURRENT_STATE == MODE_STATE.DR))
                ||//자기 센서// 정지 상태에서의 역치와 움직이는 상태에서 역치가 다르다.
                Math.toDegrees(GShandler.getTotalOmega()) > DETECT_CHANGE_GYRO))//자이로 센서
        {
//방향 전환 탐지
            int cal_index = cal_direction(meas_x, meas_z); // 0 : up; 1 :  R 2 : B 3 : R
            //System.out.println("방향전환 : " + cal_index);
            if (cal_index >= 0)// 음수값 : 애매하다~
            {
                GShandler.clearTotalOmega();    //각속도 감지
                // TODO : 방향 탐지 후 변경 부분 막음 : 어떤 각도 지도에서 지자기 값을 찾았나로 결정.
                mapViewFragment.changeDirecton((cal_index - direction_frombt) * 90);
                direction_frombt = cal_index;
            }
            // TODO : 현재 잦은 calibration으로 주석 처리 추후 방도 탐색
            /*else {
                textView_temp.setText("애매모호 하군 넌 calibration감이야");
                flush_Q();
                CURRENT_STATE = MODE_STATE.Calibration;
            }*/

        } else
            textView_temp.setText("편 - 안");
    }

    //Q 비우기
    private void flush_Q() {
        head_mag = rear_mag = 0;
    }

    public boolean isFull_mag() {
        if (((head_mag + 1) % QSIZE) == rear_mag)
            return true;
        return false;
    }

    private void push_mag_Q(float value_x, float value_z, float value_2)
    {
        if (isFull_mag()) {
//System.out.println("mag Q is full");
//return; q가 가득차도 그냥 넣는다 ㅎ _ㅎ for moving average
        }
        head_mag = (head_mag + 1) % QSIZE;
        q_mag_x[head_mag] = value_x;
        q_mag_z[head_mag] = value_z;
        q_mag_2[head_mag] = value_2;
    }

    private float cal_Average(float[] input) {
        float sum = 0.0f;

        for (int i = 0; i < input.length; i++) {
            sum = sum + input[i];
        }

        return sum / input.length;
    }

    // *수정 * = > 0 1 2 3 ambigue : -1
    public int cal_direction(float meas_x, float meas_z) {
        //System.out.println("meas x,z : " + meas_x + ", " + meas_z);
        float[] temp = new float[4];
        //root 씌운 값은 구하지 않는다. 왜냐하면 어짜피 min만 알면 되기 때문에. sqrt() 사용 x

        temp[0] = ((front_x - meas_x) * (front_x - meas_x) + (front_z - meas_z) * (front_z - meas_z));
        temp[1] = ((front_x - meas_z) * (front_x - meas_z) + (front_z + meas_x) * (front_z + meas_x));
        temp[2] = ((front_x + meas_x) * (front_x + meas_x) + (front_z + meas_z) * (front_z + meas_z));
        temp[3] = ((front_x + meas_z) * (front_x + meas_z) + (front_z - meas_x) * (front_z - meas_x));

        return findMin(temp);
    }

    //값 차이가 얼마 나지 않을 때 -1 출력 핸들링
    //return : index // ambigue : -1
    public int findMin(float[] input) {
        int index = 0, temp = 1;
        float min = input[0];

        for (int i = 1; i < input.length; i++) {
            if (input[i] < min) {
                temp = index;
                min = input[i];
                index = i;
            }
        }

        //ambiguous
        if (abs(input[temp] - input[index]) < DETECT_AMBIGUOUS)
            return -1;
        return index;
    }


    float BETA_MOVINGAVERAGE = 0.8f;

    public void update_front(float meas_x, float meas_z) {//DR 모드에서만 작동, step수 5 마다? 주기 설정 문제
        if (direction_frombt % 3 == 0)
            front_x = BETA_MOVINGAVERAGE * front_x + (1 - BETA_MOVINGAVERAGE) * meas_x;
        else
            front_x = BETA_MOVINGAVERAGE * front_x + (1 - BETA_MOVINGAVERAGE) * meas_x * (-1);

        if (direction_frombt < 2)
            front_z = BETA_MOVINGAVERAGE * front_z + (1 - BETA_MOVINGAVERAGE) * meas_z;
        else
            front_z = BETA_MOVINGAVERAGE * front_z + (1 - BETA_MOVINGAVERAGE) * meas_z * (-1);
    }


    public int getCurrentDirection() {
        return direction_frombt;
    }

    public void stepCountIncrease(int inputCount) {
        count_num += inputCount;
        count.setText(String.valueOf(count_num));
    }

    //입력한 각도로 마커의 방향을 변경한다
    public void changeDirection(int degree) {
        GShandler.clearTotalOmega();
        mapViewFragment.changeDirecton((4 - direction_frombt) * 90 + degree);
        direction_frombt = degree/90;
    }
}

