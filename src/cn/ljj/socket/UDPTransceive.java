package cn.ljj.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPTransceive {
    private String mRemoteIPAddress = null;
    private int mRemotePort = 0;
    private int mLocalPort = 0;
    private IUDPTransceiverListener mListener;
    private DatagramSocket mSocket = null;
    private ReadingThread mReadingThread;
    private WritingThread mWritingThread;

    public UDPTransceive(int localPort, IUDPTransceiverListener listener) {
        mListener = listener;
        mLocalPort = localPort;
    }

    public UDPTransceive(IUDPTransceiverListener listener) {
        mListener = listener;
        mLocalPort = 0;
    }

    public void sendData(byte[] data, String remoteIPAddress, int remotePort, long tag) {
        if (mWritingThread == null) {
            mWritingThread = new WritingThread();
            mWritingThread.start();
        }
        mRemoteIPAddress = remoteIPAddress;
        mRemotePort = remotePort;
        mWritingThread.sendData(data, tag);
    }

    public void bind() {
        if (mSocket == null || mSocket.isClosed()) {
            try {
                if (mLocalPort == 0) {
                    mSocket = new DatagramSocket();
                } else {
                    mSocket = new DatagramSocket(mLocalPort);
                }
                System.out.println("UDPTransceive bind " + mSocket.getLocalPort());
            } catch (SocketException e) {
                e.printStackTrace();
                return;
            }
        }
        if (mReadingThread == null) {
            mReadingThread = new ReadingThread();
            mReadingThread.start();
        }
    }

    public void close() {
        System.out.println("UDPTransceive close");
        try {
            if (mSocket != null) {
                mSocket.close();
            }
            if (mReadingThread != null) {
                mReadingThread.cancel();
            }
            if (mWritingThread != null) {
                mWritingThread.cancel();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    class ReadingThread extends Thread {
        private boolean mCancel = false;
        private byte[] mBuffer = new byte[1024 * 10];

        private void cancel() {
            System.out.println("UDPTransceive ReadingThread cancel");
            mCancel = true;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (mCancel) {
                        break;
                    }
                    if (mSocket != null) {
                        DatagramPacket dataPacket = new DatagramPacket(mBuffer, mBuffer.length);
                        mSocket.receive(dataPacket);
                        byte[] copyBuffer = new byte[dataPacket.getLength()];
                        System.arraycopy(dataPacket.getData(), dataPacket.getOffset(), copyBuffer, 0,
                                dataPacket.getLength());
                        InetAddress remoteIP = dataPacket.getAddress();
                        int remotePort = dataPacket.getPort();
                        System.out.println("UDPTransceive ReadingThread read data length=" + dataPacket.getLength()
                                + ", remoteAddress=" + remoteIP + ":" + remotePort);
                        if (mListener != null) {
                            mListener.onReceivedData(copyBuffer, remoteIP.getHostAddress(), remotePort);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mReadingThread = null;
            super.run();
        }
    }

    class WritingThread extends Thread {
        private boolean mCancel = false;
        private byte[] mWritebuffer = null;
        private long mDataTag = 0;

        private void cancel() {
            System.out.println("UDPTransceive WritingThread cancel");
            mCancel = true;
            synchronized (this) {
                notify();
            }
        }

        private void sendData(byte[] data, long tag) {
            if (mWritebuffer != null) {
                System.err.println("UDPTransceive WritingThread sendData busy!");
                if (mListener != null) {
                    mListener.onDataSent(false, tag);
                }
                return;
            }
            mWritebuffer = data;
            mDataTag = tag;
            synchronized (this) {
                notify();
            }
        }

        @Override
        public void run() {
            synchronized (this) {
                try {
                    while (true) {
                        if (mWritebuffer == null) {
                            System.out.println("UDPTransceive WritingThread waiting for new data to send");
                            wait();
                            if (!mCancel) {
                                System.out.println("UDPTransceive WritingThread start to send new data");
                            }
                        }
                        if (mCancel) {
                            break;
                        }
                        boolean writeSuccess = false;
                        if (mWritebuffer != null) {
                            try {
                                InetAddress address = InetAddress.getByName(mRemoteIPAddress);
                                DatagramPacket dataPacket = new DatagramPacket(mWritebuffer, mWritebuffer.length,
                                        address, mRemotePort);
                                try {
                                    mSocket.send(dataPacket);
                                    writeSuccess = true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.err.println("UDPTransceive WritingThread sendData mWritebuffer = null");
                        }
                        mWritebuffer = null;
                        if (mListener != null) {
                            mListener.onDataSent(writeSuccess, mDataTag);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mWritingThread = null;
            super.run();
        }
    }
}
