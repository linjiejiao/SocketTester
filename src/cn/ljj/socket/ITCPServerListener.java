package cn.ljj.socket;

public interface ITCPServerListener {
    public void onBindPortFailed();

    public void onAcceptFailed();

    public void onSocketConnected(TCPClient client);
}