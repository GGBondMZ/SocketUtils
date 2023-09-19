package com.devicetools.socketutils.utils;

import android.util.Log;

import com.devicetools.socketutils.bean.MessageClient;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by mz on 2023/09/19.
 * Time: 09:10
 * Description: Server
 */

public class TcpClient {

    private static final String TAG = "SocketUtils-TcpClient";

    public static Socket socket;

    public static void startClient(final String address, final int port) {
        if (address == null) {
            return;
        }
        if (socket == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "启动客户端");
                        socket = new Socket(address, port);
                        Log.d(TAG, "客户端连接成功");
                        PrintWriter pw = new PrintWriter(socket.getOutputStream());

                        InputStream inputStream = socket.getInputStream();

                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = inputStream.read(buffer)) != -1) {
                            String data = new String(buffer, 0, len);
                            Log.d(TAG, "收到服务器的数据---------------------------------------------:" + data);
                            EventBus.getDefault().post(new MessageClient(data));
                        }
                        Log.d(TAG, "客户端断开连接");
                        pw.close();

                    } catch (Exception EE) {
                        EE.printStackTrace();
                        Log.d(TAG, "客户端无法连接服务器");

                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        socket = null;
                    }
                }
            }).start();
        }
    }

    public static void sendTcpMessage(final String msg) {
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
}
