package gachon.mpclass.smartplant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class SocketActivity extends AppCompatActivity {
    SocketChannel clientSocket;
    String ip;
    int port;
    int tempInt;
    int humInt;
    String getTempHum="";
    Thread thread;
    Handler mHandler;

    TextView txtClient;
    TextView txtTemp;
    TextView txtHumidity;
    TextView txtAutoTemp;
    TextView txtAutoHumidity;
    TextView txtAlarmTemp;
    TextView txtAlarmHum;

    Button butWeb;



    int lowTemp,highTemp;
    int lowHum,highHum;
    String avgTmp;
    String avgHum;

    int autotemp;
    int autohumidity;

    SQLiteDatabase db;

    int checkedposition;

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);

        ip = "192.168.0.102";
        port = 7777;
        pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        editor = pref.edit();

        txtClient = (TextView)findViewById(R.id.txtClient);
        txtClient.setText(ip + " 접속중");
        txtTemp = (TextView)findViewById(R.id.txt_temp);
         txtAlarmTemp=(TextView)findViewById(R.id.txt_tempAlarm) ;
        txtAlarmHum=(TextView)findViewById(R.id.txt_humAlarm) ;

        butWeb=findViewById(R.id.webcam);


        Intent intent=getIntent();
        avgTmp=intent.getStringExtra("tmp");
        avgHum=intent.getStringExtra("hum");

//        온도 관련
        if(avgTmp.equals("10~15℃"))
        {
            lowTemp=10;highTemp=15;
        }
        else if(avgTmp.equals("16~20℃"))
        {
            lowTemp=16;highTemp=20;
        }
        else if(avgTmp.equals("21~25℃"))
        {
            lowTemp=21;highTemp=25;
        }
        else
        {
            lowTemp=26;highTemp=30;
        }

//        습도 관련
        if(avgHum.equals("항상 흙을 축축하게 유지해야함"))
        {
            lowHum=113;highHum=134;
        }
        else if(avgHum.equals("흙을 촉촉하게 유지해야함(물에 잠기지 않게 주의)"))
        {
            lowHum=103;highHum=110;
        }
        else if(avgHum.equals("토양 표면이 말랐을때 충분히 관수해야함"))
        {
            lowHum=81;highHum=102;
        }
        else{
            lowHum=64;highHum=80;
        }




// 실시간 영상 확인

        butWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager pm=getPackageManager();
                Intent intent= pm.getLaunchIntentForPackage("org.videolen.vlc");
                startActivity(intent);
            }

        });


        mHandler = new Handler(Looper.getMainLooper());
        connect();

          Log.i("log",getTempHum);
                    txtAlarmTemp.setText(getTempHum);

//           String getTemp=data.substring(14,15);
//            String getHum=data.substring(29,30);
//        Log.i("log","temp"+getTemp);
//        Log.i("log","hum"+getHum);
//
//            int tempInt=Integer.parseInt(getTemp);
//            int humInt=Integer.parseInt(getHum);
////            txtHumidity=문자 자르기
//
////        비교해서 알림에 표시하기
//
//        if(tempInt<lowTemp){
//            Toast.makeText(getApplication(),"화분을 따뜻한곳으로 옮겨주세요", Toast.LENGTH_SHORT).show();
//            txtAlarmTemp.setText("화분을 따뜻한곳으로 옮겨주세요");
//
//        }
//        else if(tempInt>highTemp)
//        {
//            Toast.makeText(getApplication(),"화분을 시원한곳으로 옮겨주세요", Toast.LENGTH_SHORT).show();
//            txtAlarmTemp.setText("화분을 시원한곳으로 옮겨주세요");
//        }
//        else
//        {
//            Toast.makeText(getApplication(),"딱 좋은 온도입니다", Toast.LENGTH_SHORT).show();
//            txtAlarmTemp.setText("딱 좋은 온도입니다");
//        }
//
//
//        if(humInt<lowHum){
//            Toast.makeText(getApplication(),"물을 좀 줘야해요ㅠㅠ", Toast.LENGTH_SHORT).show();
//            txtHumidity.setText("물을 좀 줘야해요ㅠㅠ");
//        }
//        else if(humInt>highHum)
//        {
//            Toast.makeText(getApplication(),"당분간 물을 주지마세요ㅠㅠ", Toast.LENGTH_SHORT).show();
//            txtHumidity.setText("당분간 물을 주지마세요ㅠㅠ");
//        }
//
//        else
//            Toast.makeText(getApplication(),"지금 딱 좋습니다", Toast.LENGTH_SHORT).show();
//            txtHumidity.setText("지금 딱 좋습니다");



    }


    @Override
    protected void onPause() {
        send("clientOFF");
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void connect() {
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    clientSocket = SocketChannel.open();
                    clientSocket.configureBlocking(true);
//                    소켓열고
                    clientSocket.connect(new InetSocketAddress(ip, port));
//                    온도 습도 받기
                    receive();
                } catch (Exception e) {
                    try {
                        clientSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                 }
            }
        };
        thread.start();
    }

    public void send(final String data){
        Thread thread1 = new Thread(){
            @Override
            public void run(){
                try{
                    Charset charset = Charset.forName("UTF-8");
                    ByteBuffer byteBuffer = charset.encode(data);
                    clientSocket.write(byteBuffer);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread1.start();
    }

    public void receive() {
        while (true) {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(100);
                clientSocket.read(byteBuffer);

                byteBuffer.flip();
                Charset charset = Charset.forName("UTF-8");
//                final String data = charset.decode(byteBuffer).toString();
                  final String data=charset.decode(byteBuffer).toString();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        txtTemp.setText(data);
                        Log.e("temp",data);
                        String temp=data.substring(0,20);
                        txtAlarmTemp.setText(temp);
                        String hum=data.substring(20);
                        txtAlarmHum.setText(hum);
//여기 체크하기
                        int tempInt = Integer.parseInt(data.substring(13,15));
                       int humInt = Integer.parseInt(data.substring(data.length()-5,data.length()-3));
//
//

//                         txtTemp.setText("temp:"+getTemp+"hum:"+getHum);
//
                        if (tempInt < lowTemp) {
                            Toast.makeText(getApplication(), "화분을 따뜻한곳으로 옮겨주세요", Toast.LENGTH_SHORT).show();
                            txtAlarmTemp.setText("화분을 따뜻한곳으로 옮겨주세요");

                        } else if (tempInt > highTemp) {
                            Toast.makeText(getApplication(), "화분을 시원한곳으로 옮겨주세요", Toast.LENGTH_SHORT).show();
                            txtAlarmTemp.setText("화분을 시원한곳으로 옮겨주세요");
                        } else {
                            Toast.makeText(getApplication(), "딱 좋은 온도입니다", Toast.LENGTH_SHORT).show();
                            txtAlarmTemp.setText("딱 좋은 온도입니다");
                        }


                        if (humInt < lowHum) {
                            Toast.makeText(getApplication(), "물을 좀 줘야해요ㅠㅠ", Toast.LENGTH_SHORT).show();
                            txtHumidity.setText("물을 좀 줘야해요ㅠㅠ");
                        } else if (humInt > highHum) {
                            Toast.makeText(getApplication(), "당분간 물을 주지마세요ㅠㅠ", Toast.LENGTH_SHORT).show();
                            txtHumidity.setText("당분간 물을 주지마세요ㅠㅠ");
                        } else
                            Toast.makeText(getApplication(), "지금 딱 좋습니다", Toast.LENGTH_SHORT).show();
                        txtHumidity.setText("지금 딱 좋습니다");

                    }
                });


            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}

