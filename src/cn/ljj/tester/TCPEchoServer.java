package cn.ljj.tester;

import java.net.Socket;

import cn.ljj.socket.ITCPClientListener;
import cn.ljj.socket.ITCPServerListener;
import cn.ljj.socket.TCPClient;
import cn.ljj.socket.TCPServer;

class TCPEchoHandler implements ITCPClientListener {
    public TCPClient mClient = null;

    @Override
    public void onSocketConnected(boolean success) {
        System.out.println("TCPEcho onSocketConnected success=" + success);
    }

    @Override
    public void onSocketDisconnected() {
        System.out.println("TCPEcho onSocketDisconnected");
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

public class TCPEchoServer {
    private TCPServer mServer;

    public TCPEchoServer() {

    }

    public void start(int port) {
        if (mServer != null) {
            stop();
        }
        mServer = new TCPServer();
        mServer.startListen(port, new ITCPServerListener() {
            @Override
            public void onSocketConnected(Socket clientSocket) {
                TCPEchoHandler echo = new TCPEchoHandler();
                TCPClient client = new TCPClient(clientSocket, echo);
                echo.mClient = client;
                client.connect();
            }

            @Override
            public void onBindPortFailed() {

            }

            @Override
            public void onAcceptFailed() {

            }
        });
    }

    public void stop() {
        if (mServer != null) {
            mServer.stopListen();
            mServer = null;
        }
    }
}
