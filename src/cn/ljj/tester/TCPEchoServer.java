package cn.ljj.tester;

import java.util.ArrayList;

import cn.ljj.socket.ITCPClientListener;
import cn.ljj.socket.ITCPServerListener;
import cn.ljj.socket.TCPClient;
import cn.ljj.socket.TCPServer;

interface EchoDisconnectListener {
    void onEchoDisconnected(TCPEchoHandler echo);
}

class TCPEchoHandler implements ITCPClientListener {
    public TCPClient mClient = null;
    public EchoDisconnectListener mDisconnectListener = null;

    @Override
    public void onSocketConnected(boolean success) {
        System.out.println("TCPEcho onSocketConnected success=" + success);
    }

    @Override
    public void onSocketDisconnected() {
        System.out.println("TCPEcho onSocketDisconnected");
        if (mDisconnectListener != null) {
            mDisconnectListener.onEchoDisconnected(this);
        }
    }

    @Override
    public void onDataSent(boolean success, long tag) {
        System.out.println("TCPEcho onDataSent success=" + success + ", tag=" + tag);
    }

    @Override
    public void onReceivedData(byte[] data) {
        System.out.println("TCPEcho onReceivedData data=" + new String(data));
        if (mClient != null) {
            mClient.sendData(data, System.currentTimeMillis());
        }
    }
}

public class TCPEchoServer implements EchoDisconnectListener {
    private TCPServer mServer;
    private ArrayList<TCPEchoHandler> mConnectingEchoList = new ArrayList<TCPEchoHandler>();

    public TCPEchoServer() {

    }

    public void start(int port) {
        if (mServer != null) {
            stop();
        }
        mServer = new TCPServer();
        mServer.startListen(port, new ITCPServerListener() {
            @Override
            public void onSocketConnected(TCPClient client) {
                TCPEchoHandler echo = getHandler();
                client.setListener(echo);
                echo.mClient = client;
                echo.mDisconnectListener = TCPEchoServer.this;
                client.connect();
                mConnectingEchoList.add(echo);
            }

            @Override
            public void onBindPortFailed() {

            }

            @Override
            public void onAcceptFailed() {

            }
        });
    }

    protected TCPEchoHandler getHandler() {
        return new TCPEchoHandler();
    }

    public void stop() {
        System.out.println("TCPEchoServer stop");
        if (mServer != null) {
            mServer.stopListen();
            mServer = null;
        }
        for (TCPEchoHandler handler : mConnectingEchoList) {
            handler.mClient.disconnect();
        }
        mConnectingEchoList.clear();
    }

    @Override
    public void onEchoDisconnected(TCPEchoHandler echo) {
        mConnectingEchoList.remove(echo);
    }
}
