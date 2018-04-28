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

public class TCPChannelDetecter extends TimerTask implements ITCPClientListener {
    private String mOutputFilePath = null;
    private TCPClient mTCPClient = null;
    private int mIntervalSeconds = 5;
    private Timer mHeartBeatTimer = null;
    private String mRemoteIPAddress = null;
    private int mRemotePort = 0;
    private long mCurrentHeartBeatTag = 0;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS ");
    private PrintWriter mLogPrintWriter = null;
    // statistics
    public long mStartTime = 0;
    public long mHeartBeatSentCount = 0;
    public long mHeartBeatEchoCount = 0;
    public long mDisconnectCount = 0;
    public long mHeartBeatTime = 0;

    public TCPChannelDetecter(String outputFilePath) {
        if (outputFilePath == null) {
            throw (new NullPointerException("outputFilePath == null"));
        }
        mOutputFilePath = outputFilePath;
    }

    // client
    public void startHeartBeat(String remoteIP, int remotePort, int intervalSeconds) {
        if (mTCPClient != null) {
            mTCPClient.disconnect();
            mTCPClient = null;
        }
        mRemoteIPAddress = remoteIP;
        mRemotePort = remotePort;
        mTCPClient = new TCPClient(mRemoteIPAddress, mRemotePort, this);
        mTCPClient.connect();
        if (intervalSeconds > 5) {
            mIntervalSeconds = intervalSeconds;
        }
        if (mHeartBeatTimer != null) {
            mHeartBeatTimer.cancel();
        }
        mHeartBeatTimer = new Timer();
        mHeartBeatTimer.schedule(this, mIntervalSeconds * 1000, mIntervalSeconds * 1000);
        mStartTime = System.currentTimeMillis();
    }

    public void stopHeartBeat() {
        if (mTCPClient != null) {
            mTCPClient.disconnect();
            mTCPClient = null;
        }
        if (mHeartBeatTimer != null) {
            mHeartBeatTimer.cancel();
            mHeartBeatTimer = null;
        }
        if (mLogPrintWriter != null) {
            mLogPrintWriter
                    .println("--------end:cost_time=" + (System.currentTimeMillis() - mStartTime) + " sent_count="
                            + mHeartBeatSentCount + " success_count=" + mHeartBeatEchoCount + " disconnect_count="
                            + mDisconnectCount + " average_time=" + (mHeartBeatTime / mHeartBeatEchoCount)
                            + " success_rate=" + (1.0 * mHeartBeatEchoCount / mHeartBeatSentCount) + "--------");
            mLogPrintWriter.close();
            mLogPrintWriter = null;
        }
        System.out.println("stopHeartBeat");
    }

    @Override
    public void onSocketDisconnected() {
        if (mTCPClient != null) {
            mTCPClient = null;
            mDisconnectCount++;
            logToFile("[disconnect]");
        }
    }

    @Override
    public void onSocketConnected(boolean success) {
        if (!success) {
            if (mTCPClient != null) {
                mTCPClient = null;
                mDisconnectCount++;
                logToFile("[connect_failed]");
            }
        }
    }

    @Override
    public void onReceivedData(byte[] data) {
        String receiveString = new String(data);
        try {
            long echoTag = Long.parseLong(receiveString);
            if (echoTag == mCurrentHeartBeatTag) {
                long costTime = System.currentTimeMillis() - echoTag;
                logToFile("[success] tag=" + echoTag + " time=" + costTime);
                mHeartBeatTime += costTime;
                mHeartBeatEchoCount++;
            } else {
                logToFile("[receive_failed] currentTag=" + mCurrentHeartBeatTag + " echoTag=" + echoTag);
            }
            mCurrentHeartBeatTag = 0;
        } catch (Exception e) {
        }
    }

    @Override
    public void onDataSent(boolean success, long tag) {
        if (!success) {
            logToFile("[send_failed] tag=" + tag);
        } else if (tag != mCurrentHeartBeatTag) {
            logToFile("[send_timeout] currentTag=" + mCurrentHeartBeatTag + " sendTag=" + tag);
        }
    }

    // timer schedule
    @Override
    public void run() {
        if (mCurrentHeartBeatTag != 0) {
            logToFile("[timeout_failed] tag=" + mCurrentHeartBeatTag);
        }
        mCurrentHeartBeatTag = System.currentTimeMillis();
        byte[] data = Long.toString(mCurrentHeartBeatTag).getBytes();
        if (mTCPClient == null) {
            mTCPClient = new TCPClient(mRemoteIPAddress, mRemotePort, this);
            mTCPClient.connect();
        } else {
            mTCPClient.sendData(data, mCurrentHeartBeatTag);
            mHeartBeatSentCount++;
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
