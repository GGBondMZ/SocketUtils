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
    //    Socket socket;
    private ServerThread mServerThread;
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
                        Log.d(TAG, "客户端连接上来了");
                        InputStream inputStream = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = inputStream.read(buffer)) != -1) {
                            String data = new String(buffer, 0, len);
                            Log.d(TAG, "收到客户端的数据-----------------------------:" + data);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServerSocket();
    }

    class ServerThread extends Thread {
        public void run() {
            try {
                Log.i(TAG, "port " + mEtPort.getText().toString());
                int port = Integer.valueOf(mEtPort.getText().toString());// 获取portEditText中的端口号
                serverSocket = new ServerSocket(port);// 首先创建一个服务端口
                Log.i(TAG, "等待客户端的连接请求");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ServerActivity.this, "启动服务", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "启动服务");
                    }
                });
                while (!isStop) {
                    // 等待客户端的连接请求
                    final Socket socket = serverSocket.accept();
                    mSocketList.add(socket);
                    final String socketAddress = socket.getRemoteSocketAddress().toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ServerActivity.this, "成功建立与客户端的连接 : " + socketAddress, Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "成功建立与客户端的连接 : " + socketAddress);
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!isStop) try {
                                {
                                    Log.i(TAG, "Receive message");
                                    // InputStream inputStream = socket.getInputStream();
                                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                                    String type = "";
                                    byte[] typebytes = new byte[Constant.SERVER_TYPE];
                                    if (inputStream.read(typebytes) != -1) {
                                        type = nullOfString(new String(typebytes));
                                    }

                                    switch (type) {
                                        case Constant.SERVER_TEXT:
                                            Log.i(TAG, "MAZHUANG");
                                            byte[] bytes = new byte[1];
                                            StringBuilder info = new StringBuilder();
                                            while (inputStream.read(bytes) != -1) {
                                                String str = new String(bytes);
                                                if (str.equals("\n")) {
                                                    break;
                                                }
                                                info.append(new String(bytes));
                                            }
                                            final String finalInfo = info.toString();
                                            Log.i(TAG, "text = " + finalInfo);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // mEtReceive.setText(mEtReceive.getText().toString() + socketAddress + " : " + finalInfo + "\n");
                                                    dataListAdapter.addData(mEtReceive.getText().toString() + socketAddress + " : " + finalInfo + "\n");
                                                    if (dataListAdapter.getData() != null && dataListAdapter.getData().size() > 0) {
                                                        recy.smoothScrollToPosition(dataListAdapter.getData().size());
                                                    }
                                                }
                                            });
                                            break;
                                        case Constant.SERVER_FILE:
                                            byte[] remote = new byte[32];
                                            String md5 = "";
                                            if (inputStream.read(remote) != -1) {
                                                md5 = nullOfString(new String(remote));
                                            }

                                            final String root = Environment.getExternalStorageDirectory().getPath();
                                            Log.i(TAG, root);
                                            byte[] inputByte = new byte[1024 * 1024];
                                            int len = 0;
                                            long fileSize = 0;

                                            DataInputStream dis = new DataInputStream(inputStream);
                                            // 文件名和长度
                                            String fileName = dis.readUTF();
                                            final long fileLength = dis.readLong();
                                            Log.i(TAG, "fileName = " + fileName);
                                            Log.i(TAG, "fileLength = " + fileLength);
                                            mPath = root + "/ECG/" + fileName;
                                            File file = new File(root + "/ECG/");
                                            if (!file.exists()) file.mkdir();
                                            file = new File(mPath);
                                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                                            String fileMD5 = nullOfString(getFileMD5(new File(mPath)));
                                            while (!md5.equals(fileMD5) && (len = dis.read(inputByte, 0, inputByte.length)) > 0) {
                                                fileSize += len;
                                                fileOutputStream.write(inputByte, 0, len);
                                                fileOutputStream.flush();
                                                fileMD5 = nullOfString(getFileMD5(new File(mPath)));
                                                Log.i(TAG, "md5 = " + md5 + " file = " + fileMD5);
                                                Log.i(TAG, "fileLength = " + fileLength + " fileSize = " + fileSize + " " + (fileSize * 100 / fileLength) + "%");
                                                final long finalFileSize = fileSize;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mProgressDialog.setMessage((finalFileSize * 100 / fileLength) + "%");
                                                        mProgressDialog.show();
                                                    }
                                                });
                                                if (md5.equals(fileMD5)) {
                                                    fileOutputStream.close();
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mProgressDialog.hide();
                                                        }
                                                    });
                                                }
                                            }
                                            Log.i(TAG, "md52 = " + md5 + " file2 = " + getFileMD5(file));
                                            fileMD5 = nullOfString(getFileMD5(new File(mPath)));
                                            Log.i(TAG, "file = " + fileMD5);
                                            final String finalFileMD = fileMD5;
                                            final String finalMd = md5;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dataListAdapter.addData(mEtReceive.getText().toString() + "文件路径：" + mPath + "\n");
                                                    dataListAdapter.addData(mEtReceive.getText().toString() + "file = " + finalFileMD + "\n");
                                                    dataListAdapter.addData(mEtReceive.getText().toString() + "text = " + finalMd + "\n");
                                                    if (dataListAdapter.getData() != null && dataListAdapter.getData().size() > 0) {
                                                        recy.smoothScrollToPosition(dataListAdapter.getData().size());
                                                    }
                                                }
                                            });
                                            break;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.i(TAG, e.toString());
                            }
                        }
                    }).start();

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }
        }
    }

    private static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        try {
            byte[] buffer = new byte[1024];
            int len;
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String nullOfString(String str) {
        if (str == null) {
            str = "";
        }
        return str;
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
