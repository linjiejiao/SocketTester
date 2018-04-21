package cn.ljj.socket;

public interface ITCPClientListener {
    void onSocketConnected(boolean success);

    void onSocketDisconnected();

    void onDataSent(boolean success, long tag);

    void onReceivedData(byte[] data);

}
