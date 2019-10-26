package com.example.iot_linux_subject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean flagConnection = true;
    boolean isConnected = false;
    boolean flagRead = true;

    Handler writeHandler;

    Socket socket;
    BufferedInputStream bin;
    BufferedOutputStream bout;

    SocketThread st;
    ReadThread rt;
    WriteThread wt;

    String serverIp="203.253.177.228";
    int serverPort=7070;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }


    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        st=new SocketThread();
        st.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        flagConnection = false;
        isConnected = false;

        if (socket != null) {
            flagRead = false;
            writeHandler.getLooper().quit();
            try {
                bout.close();
                bin.close();
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private void showToast(String message){
        Toast toast=Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    Handler mainHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==10){
                //connection ok~~
                showToast("connection ok~~");
            }else if(msg.what==20){
                //connection fail~~~
                showToast("connection fail~~");
            }else if(msg.what==100){
                //message read....
            }else if(msg.what==200){
                //message write...
            }
        }
    };



    class SocketThread extends Thread {

        public void run() {

            while (flagConnection){
                try{
                    if(!isConnected){
                        socket=new Socket();
                        SocketAddress remoteAddr=new InetSocketAddress(serverIp, serverPort);
                        socket.connect(remoteAddr, 10000);

                        bout=new BufferedOutputStream(socket.getOutputStream());
                        bin=new BufferedInputStream(socket.getInputStream());

                        if(rt != null){
                            flagRead=false;
                        }
                        if(wt != null){
                            writeHandler.getLooper().quit();
                        }

                        wt=new WriteThread();
                        wt.start();
                        rt=new ReadThread();
                        rt.start();

                        isConnected=true;

                        Message msg=new Message();
                        msg.what=10;
                        mainHandler.sendMessage(msg);
                    }else {
                        SystemClock.sleep(10000);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    SystemClock.sleep(10000);
                }
            }

        }
    }

    class WriteThread extends Thread {

        @Override
        public void run() {
            Looper.prepare();
            writeHandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    try{
                        bout.write(((String)msg.obj).getBytes());
                        bout.flush();

                        Message message=new Message();
                        message.what=200;
                        message.obj=msg.obj;
                        mainHandler.sendMessage(message);
                    }catch (Exception e){
                        e.printStackTrace();
                        isConnected=false;
                        writeHandler.getLooper().quit();
                        try{
                            flagRead=false;
                        }catch (Exception e1){}
                    }
                }
            };
            Looper.loop();
        }
    }

    class ReadThread extends Thread {

        @Override
        public void run() {
            byte[] buffer=null;
            while(flagRead){
                buffer=new byte[1024];
                try{
                    String message=null;
                    int size=bin.read(buffer);
                    if(size>0){
                        message=new String(buffer, 0, size, "utf-8");
                        if(message != null && !message.equals("")){
                            Message msg=new Message();
                            msg.what=100;
                            msg.obj=message;
                            mainHandler.sendMessage(msg);
                        }
                    }else {
                        flagRead=false;
                        isConnected=false;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    flagRead=false;
                    isConnected=false;
                }
            }
            Message msg=new Message();
            msg.what=20;
            mainHandler.sendMessage(msg);
        }

    }


}

