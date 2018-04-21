package cn.ljj.socket;

import java.net.Socket;

public interface ITCPServerListener {
    public void onBindPortFailed();

    public void onAcceptFailed();

    public void onSocketConnected(Socket clientSocket);
}