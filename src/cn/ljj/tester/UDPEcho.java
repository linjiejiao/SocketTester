package cn.ljj.tester;

import cn.ljj.socket.IUDPTransceiverListener;
import cn.ljj.socket.UDPTransceive;

public class UDPEcho implements IUDPTransceiverListener {
    public UDPTransceive mUDPTransceive;

    public void start(int port) {
        if (mUDPTransceive != null) {
            stop();
        }
        mUDPTransceive = new UDPTransceive(port, this);
        mUDPTransceive.bind();
    }

    public void stop() {
        if (mUDPTransceive != null) {
            mUDPTransceive.close();
            mUDPTransceive = null;
        }
    }

    @Override
    public void onDataSent(boolean success, long tag) {
        System.out.println("UDPEcho onDataSent success=" + success + ", tag=" + tag);
    }

    @Override
    public void onReceivedData(byte[] data, String remoteIP, int remotePort) {
        System.out.println(
                "UDPEcho onReceivedData data=" + new String(data) + ", remoteAddress=" + remoteIP + ":" + remotePort);
        if (mUDPTransceive != null) {
            mUDPTransceive.sendData(data, remoteIP, remotePort, System.currentTimeMillis());
        }
    }

}
