package cn.ljj.tester;

public class AddressEchoServer {
    private TCPEchoServer mTCPEchoServer;
    private UDPEcho mUDPEcho;

    public void start(int port) {
        // TCP Echo
        mTCPEchoServer = new TCPEchoServer() {

            protected TCPEchoHandler getHandler() {
                return new TCPEchoHandler() {
                    @Override
                    public void onReceivedData(byte[] data) {
                        System.out.println("TCPAddressEcho onReceivedData data=" + new String(data));
                        if (mClient != null) {
                            mClient.sendData(mClient.getRemoteAddress().getBytes(), System.currentTimeMillis());
                        }
                    }
                };
            }
        };
        mTCPEchoServer.start(port);
        // UDP Echo
        mUDPEcho = new UDPEcho() {
            @Override
            public void onReceivedData(byte[] data, String remoteIP, int remotePort) {
                System.out.println("UDPAddressEcho onReceivedData data=" + new String(data) + ", remoteAddress="
                        + remoteIP + ":" + remotePort);
                if (mUDPTransceive != null) {
                    mUDPTransceive.sendData((remoteIP + ":" + remotePort).getBytes(), remoteIP, remotePort,
                            System.currentTimeMillis());
                }
            }
        };
        mUDPEcho.start(port);
    }

    public void stop() {
        if (mTCPEchoServer != null) {
            mTCPEchoServer.stop();
        }
        if (mUDPEcho != null) {
            mUDPEcho.stop();
        }
    }
}
