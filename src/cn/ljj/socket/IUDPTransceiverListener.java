package cn.ljj.socket;

public interface IUDPTransceiverListener {

    void onDataSent(boolean success, long tag);

    void onReceivedData(byte[] data, String remoteIP, int remotePort);

}
