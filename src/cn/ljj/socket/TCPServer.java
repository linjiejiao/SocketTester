package cn.ljj.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer implements Runnable {
    public ServerSocket mServerSocket = null;
    public int mListenPort = 0;
    public ITCPServerListener mListener = null;
    public boolean isListening = false;

    public void startListen(int port, ITCPServerListener listener) {
        synchronized (this) {
            isListening = true;
            if (mServerSocket != null) {
                System.out.println(
                        "TCPServer startListen on:" + port + " fail!, already listening on port: " + mListenPort);
                return;
            }
            mListenPort = port;
            mListener = listener;
            try {
                mServerSocket = new ServerSocket(mListenPort);
                new Thread(this).start();
            } catch (IOException e) {
                e.printStackTrace();
                if (mListener != null) {
                    mListener.onBindPortFailed();
                }
            }
        }
    }

    public void stopListen() {
        System.out.println("TCPServer stopListen");
        synchronized (this) {
            isListening = false;
            mListenPort = 0;
            mListener = null;
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mServerSocket = null;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("TCPServer start listening on port: " + mListenPort);
            while (true) {
                Socket client = mServerSocket.accept();
                if (client != null) {
                    System.out.println("TCPServer accept client: " + client.getRemoteSocketAddress().toString());
                    if (mListener != null) {
                        mListener.onSocketConnected(new TCPClient(client, null));
                    }
                }
            }
        } catch (Exception e) {
            if (isListening) {
                e.printStackTrace();
                if (mListener != null) {
                    mListener.onAcceptFailed();
                }
            }
        }
    }

}
