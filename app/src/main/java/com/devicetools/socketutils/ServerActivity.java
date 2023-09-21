package com.devicetools.socketutils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.devicetools.socketutils.adapter.DataListAdapter;
import com.devicetools.socketutils.bean.MessageServer;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mz on 2023/09/19.
 * Time: 09:10
 * Description: Server
 */
public class ServerActivity extends AppCompatActivity {

    private static final String TAG = "SocketUtils-ServerActivity";
    private ServerSocket serverSocket;// 创建ServerSocket对象
    private Socket socket;
    List<Socket> mSocketList;
    private boolean isStop;
    private EditText mEtPort;
    private EditText mEtMessage;
    private EditText mEtReceive;
    private Button mBtStart;
    private Button mBtStop;
    private Button mBtSend;
    private TextView mTvIp;
    private RecyclerView recy;
    private DataListAdapter dataListAdapter;
    private String mPath = "";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mSocketList = new ArrayList<>();
        mTvIp = findViewById(R.id.tv_ip);
        mEtPort = findViewById(R.id.et_port);
        mEtMessage = findViewById(R.id.et_message);
        mEtReceive = findViewById(R.id.et_receive);
        mBtStart = findViewById(R.id.bt_start);
        mBtStop = findViewById(R.id.bt_stop);
        mBtSend = findViewById(R.id.bt_send);
        recy = findViewById(R.id.recyclerView);

        dataListAdapter = new DataListAdapter(null);
        recy.setLayoutManager(new LinearLayoutManager(this));
        recy.setAdapter(dataListAdapter);
        recy.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mTvIp.setText(getLocalIpAddress());
        mBtStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServer();
            }
        });
        mBtStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServerSocket();
            }
        });
        mBtSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTcpMessage(String.valueOf(mEtMessage.getText()));
            }
        });
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServerSocket();
    }

    public void startServer() {
        if (serverSocket == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int port = Integer.valueOf(mEtPort.getText().toString());// 获取portEditText中的端口号
                        serverSocket = new ServerSocket(port);
                        Log.d(TAG, "服务器等待连接中");
                        socket = serverSocket.accept();
                        String socketAddress = socket.getRemoteSocketAddress().toString();
                        Log.d(TAG, "客户端连接:" + socketAddress);
                        InputStream inputStream = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = inputStream.read(buffer)) != -1) {
                            String data = new String(buffer, 0, len);
                            Log.d(TAG, "收到客户端的数据:" + "\n" + data);
                            JSONObject jsonData = JSON.parseObject(data);
                            Log.d(TAG, "json:" + "\n" + jsonData.toString());

                            String type = jsonData.getString("type");
                            Log.d(TAG, "AirHumidity = " + type);
                            String airTemp = jsonData.getJSONObject("Content").getString("AirTemp");
                            Log.d(TAG, "AirTemp = " + airTemp);
                            String airHumidity = jsonData.getJSONObject("Content").getString("AirHumidity");
                            Log.d(TAG, "AirHumidity = " + airHumidity);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dataListAdapter.addData(data);
                                    if (dataListAdapter.getData() != null && dataListAdapter.getData().size() > 0) {
                                        recy.smoothScrollToPosition(dataListAdapter.getData().size());
                                    }
                                }
                            });
                            EventBus.getDefault().post(new MessageServer(data));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        socket = null;
                        serverSocket = null;
                    }
                }
            }).start();
        }
    }

    public void sendTcpMessage(final String msg) {
        if (socket != null && socket.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.getOutputStream().write(msg.getBytes());
                        socket.getOutputStream().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void stopServerSocket() {
        isStop = true;
        try {
            for (Socket socket : mSocketList) {
                if (socket != null) {
                    socket.close();
                }
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
            Toast.makeText(ServerActivity.this, "停止服务", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "停止服务");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取WIFI下ip地址
     */
    @SuppressLint("DefaultLocale")
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        // 返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}
