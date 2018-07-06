package cn.ljj.tester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import cn.ljj.socket.ITCPClientListener;
import cn.ljj.socket.TCPClient;

public class TCPTransferSpeedTester implements ITCPClientListener {
    private String mOutputFilePath = null;
    private TCPClient mTCPClient = null;
    private Timer mTestTimer = null;
    private boolean isTestingReceive = true;
    private int mTestClientDataSize = 0;
    private byte[] mTestClientDataBuffer = null;
    private int mReceiveDataLength = 0;
    private long mReceiveStartTime = 0;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS ");
    private PrintWriter mLogPrintWriter = null;

    public TCPTransferSpeedTester(String outputFilePath) {
        if (outputFilePath == null) {
            throw (new NullPointerException("outputFilePath == null"));
        }
        mOutputFilePath = outputFilePath;
    }

    public void testSpeed(String remoteIP, int remotePort, int intervalSeconds) {
        if (mTestTimer != null) {
            mTestTimer.cancel();
            mTestTimer = null;
        }
        if (intervalSeconds < 5) {
            intervalSeconds = 5;
        }
        mTestTimer = new Timer();
        mTestTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                testSpeed(remoteIP, remotePort);
            }
        }, intervalSeconds * 1000, intervalSeconds * 1000);
    }

    public void testSpeed(String remoteIP, int remotePort) {
        if (mTCPClient != null) {
            logToFile("[time_out]");
            mTCPClient.setListener(null);
            mTCPClient.disconnect();
            mTCPClient = null;
        }
        mReceiveDataLength = 0;
        mReceiveStartTime = 0;
        isTestingReceive = true;
        if (mTestClientDataSize != remotePort * 100) {
            mTestClientDataSize = remotePort * 100;
            mTestClientDataBuffer = new byte[mTestClientDataSize];
            for (int i = 0; i < mTestClientDataSize; i++) {
                mTestClientDataBuffer[i] = (byte) (i % 256);
            }
        }
        mTCPClient = new TCPClient(remoteIP, remotePort, this);
        mTCPClient.connect();
    }

    public void cancelTestSpeed() {
        if (mTestTimer != null) {
            mTestTimer.cancel();
            mTestTimer = null;
        }
        if (mTCPClient != null) {
            mTCPClient.setListener(null);
            mTCPClient.disconnect();
            mTCPClient = null;
        }
        System.out.println("cancelTestSpeed");
    }

    @Override
    public void onSocketConnected(boolean success) {
        System.out.println("onSocketConnected " + success);
        if (!success) {
            if (mTCPClient != null) {
                System.out.println("onSocketConnected mTCPClient != null");
                mTCPClient.setListener(null);
                mTCPClient.disconnect();
                mTCPClient = null;
                logToFile("[connect_fail]");
            }
        }
    }

    @Override
    public void onSocketDisconnected() {
        if (mTCPClient != null) {
            mTCPClient.setListener(null);
            mTCPClient.disconnect();
            mTCPClient = null;
            logToFile("[disconnect_connect]");
        }
    }

    @Override
    public void onDataSent(boolean success, long tag) {

    }

    @Override
    public void onReceivedData(byte[] data) {
        if (isTestingReceive) {
            if (mReceiveDataLength == 0) {
                mReceiveStartTime = System.currentTimeMillis();
            }
            mReceiveDataLength += data.length;
            if (mReceiveDataLength == mTestClientDataSize) {
                isTestingReceive = false;
                long receiveTime = System.currentTimeMillis() - mReceiveStartTime;
                logToFile("[receive]speed=" + (mReceiveDataLength / receiveTime) + "B/ms");
                mTCPClient.sendData(mTestClientDataBuffer, System.currentTimeMillis());
            } else if (mReceiveDataLength > mTestClientDataSize) {
                System.err.println("mReceiveDataLength > mTestClientDataSize");
                logToFile("[package_error]");
            }
        } else {
            String respond = new String(data);
            if (respond.contains("cost_time")) {
                String[] strings = respond.split("=");
                long sendTime = Long.parseLong(strings[1]);
                logToFile("[send]speed=" + (mReceiveDataLength / sendTime) + "B/ms");
            } else {
                logToFile("[send]respond=" + respond);
            }
            mTCPClient.setListener(null);
            mTCPClient.disconnect();
            mTCPClient = null;
        }
    }

    private void logToFile(String info) {
        StringBuilder sb = new StringBuilder(mSimpleDateFormat.format(new Date()));
        sb.append(info);
        if (mLogPrintWriter == null) {
            FileWriter fw = null;
            try {
                File f = new File(mOutputFilePath);
                fw = new FileWriter(f, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mLogPrintWriter = new PrintWriter(fw);
            mLogPrintWriter.println("--------new start:" + mSimpleDateFormat.format(new Date()) + "--------");
        }
        mLogPrintWriter.println(sb.toString());
        mLogPrintWriter.flush();
    }
}
