package cn.ljj.tester;

import java.util.ArrayList;

import cn.ljj.socket.ITCPClientListener;
import cn.ljj.socket.ITCPServerListener;
import cn.ljj.socket.TCPClient;
import cn.ljj.socket.TCPServer;

public class TCPTransferSpeedTestServer implements ITCPServerListener {
    private TCPServer mTCPServer = null;
    private ArrayList<TCPClient> mConnectingClientList = new ArrayList<TCPClient>();
    private int mTestServerDataSize = 0;
    private byte[] mTestServerDataBuffer = null;

    public void startListen(int port) {
        if (mTCPServer != null) {
            mTCPServer.stopListen();
            mTCPServer = null;
        }
        mTestServerDataSize = port * 100;
        mTestServerDataBuffer = new byte[mTestServerDataSize];
        for (int i = 0; i < mTestServerDataSize; i++) {
            mTestServerDataBuffer[i] = (byte) (i % 256);
        }
        mTCPServer = new TCPServer();
        mTCPServer.startListen(port, this);
    }

    public void stopListen() {
        System.out.println("TCPTransferSpeedTestServer stopListen");
        if (mTCPServer != null) {
            mTCPServer.stopListen();
            mTCPServer = null;
        }
        for (TCPClient client : mConnectingClientList) {
            client.disconnect();
        }
        mConnectingClientList.clear();
    }

    @Override
    public void onBindPortFailed() {
        mTCPServer = null;
        for (TCPClient client : mConnectingClientList) {
            client.disconnect();
        }
        mConnectingClientList.clear();
    }

    @Override
    public void onAcceptFailed() {
        mTCPServer = null;
        for (TCPClient client : mConnectingClientList) {
            client.disconnect();
        }
        mConnectingClientList.clear();
    }

    @Override
    public void onSocketConnected(TCPClient client) {
        client.setListener(new ITCPClientListener() {
            private int mReceiveDataSize = 0;
            private long mReceiveStartTime = 0;

            @Override
            public void onSocketDisconnected() {
                mConnectingClientList.remove(client);
            }

            @Override
            public void onSocketConnected(boolean success) {
                if (!success) {
                    mConnectingClientList.remove(client);
                }
            }

            @Override
            public void onReceivedData(byte[] data) {
                if (mReceiveDataSize == 0) {
                    mReceiveStartTime = System.currentTimeMillis();
                }
                mReceiveDataSize += data.length;
                if (mReceiveDataSize >= mTestServerDataSize) {
                    String respond = "cost_time=" + (System.currentTimeMillis() - mReceiveStartTime);
                    client.sendData(respond.getBytes(), System.currentTimeMillis());
                }
            }

            @Override
            public void onDataSent(boolean success, long tag) {
            }
        });
        mConnectingClientList.add(client);
        client.connect();
        client.sendData(mTestServerDataBuffer, System.currentTimeMillis());
    }

}
