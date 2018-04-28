package cn.ljj.socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidParameterException;

public class TCPClient {
    private Socket mClientSocket = null;
    private boolean isConnecting = false;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private String mRemoteIPAddress = null;
    private int mRemotePort = 0;
    private ITCPClientListener mListener;
    private ReadingThread mReadingThread;
    private WritingThread mWritingThread;

    public TCPClient(Socket clientSocket, ITCPClientListener listener) {
        if (clientSocket == null) {
            throw (new NullPointerException("TCPClient() clientSocket == null"));
        }
        mClientSocket = clientSocket;
        mListener = listener;
    }

    public TCPClient(String remoteIPAddress, int remotePort, ITCPClientListener listener) {
        if (remoteIPAddress == null || remoteIPAddress.length() < 7 || remoteIPAddress.length() > 15 || remotePort < 0
                || remotePort > 65536) {
            throw (new InvalidParameterException(
                    "TCPClient() remoteIPAddress=" + remoteIPAddress + ", remotePort=" + remotePort));
        }
        mRemoteIPAddress = remoteIPAddress;
        mRemotePort = remotePort;
        mListener = listener;
    }

    public void setListener(ITCPClientListener listener) {
        mListener = listener;
    }

    public String getRemoteAddress() {
        if (mClientSocket != null) {
            return mClientSocket.getRemoteSocketAddress().toString();
        } else {
            return mRemoteIPAddress + ":" + mRemotePort;
        }
    }

    public void connect() {
        if (mReadingThread == null) {
            mReadingThread = new ReadingThread();
            mReadingThread.start();
        } else {
            System.err.println("TCPClient mReadingThread != null!");
            if (mListener != null) {
                mListener.onSocketConnected(false);
            }
            return;
        }
        if (mWritingThread == null) {
            mWritingThread = new WritingThread();
            mWritingThread.start();
        } else {
            System.err.println("TCPClient mWritingThread != null!");
            if (mListener != null) {
                mListener.onSocketConnected(false);
            }
            return;
        }
    }

    public void disconnect() {
        closeAllResourse();
        System.out.println("TCPClient disconnect");
    }

    public void sendData(byte[] data, long tag) {
        if (mWritingThread != null) {
            mWritingThread.sendData(data, tag);
        } else {
            System.err.println("TCPClient mWritingThread = null!");
        }
    }

    private void closeAllResourse() {
        try {
            if (mReadingThread != null) {
                mReadingThread.cancel();
                mReadingThread = null;
            }
            if (mWritingThread != null) {
                mWritingThread.cancel();
                mWritingThread = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mClientSocket != null) {
                mClientSocket.close();
                mClientSocket = null;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    class ReadingThread extends Thread {
        private boolean mCancel = false;
        private byte[] mBuffer = new byte[1024 * 10];

        private void cancel() {
            System.out.println("TCPClient ReadingThread cancel");
            mCancel = true;
        }

        @Override
        public void run() {
            // connect
            isConnecting = true;
            try {
                if (mClientSocket == null) {
                    mClientSocket = new Socket(mRemoteIPAddress, mRemotePort);
                }
                if (!mClientSocket.isConnected()) {
                    if (mListener != null) {
                        mListener.onSocketConnected(false);
                    }
                    isConnecting = false;
                    return;
                }
                mInputStream = mClientSocket.getInputStream();
                mOutputStream = mClientSocket.getOutputStream();
                isConnecting = false;
            } catch (Exception e) {
                if (!mCancel) {
                    e.printStackTrace();
                    closeAllResourse();
                    if (mListener != null) {
                        mListener.onSocketConnected(false);
                    }
                }
                isConnecting = false;
                return;
            }
            // reading
            try {
                while (true) {
                    if (mCancel) {
                        break;
                    }
                    if (mInputStream != null) {
                        int length = mInputStream.read(mBuffer);
                        if (length > 0) {
                            if (mListener != null) {
                                byte[] copyBuffer = new byte[length];
                                System.arraycopy(mBuffer, 0, copyBuffer, 0, length);
                                mListener.onReceivedData(copyBuffer);
                            }
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                if (!mCancel) {
                    e.printStackTrace();
                }
            }
            if (!mCancel) {
                if (mListener != null) {
                    mListener.onSocketDisconnected();
                }
                closeAllResourse();
            }
            super.run();
        }
    }

    class WritingThread extends Thread {
        private boolean mCancel = false;
        private byte[] mWritebuffer = null;
        private long mDataTag = 0;

        private void cancel() {
            System.out.println("TCPClient WritingThread cancel");
            synchronized (this) {
                mCancel = true;
                notify();
            }
        }

        private void sendData(byte[] data, long tag) {
            if (mCancel) {
                System.err.println("TCPClient WritingThread sendData already closed!");
                if (mListener != null) {
                    mListener.onDataSent(false, tag);
                }
            }
            if (mWritebuffer != null) {
                System.err.println("TCPClient WritingThread sendData busy!");
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
                            wait();
                        }
                        if (mCancel) {
                            break;
                        }
                        boolean writeSuccess = false;
                        if (mWritebuffer != null) {
                            if (mOutputStream != null) {
                                try {
                                    mOutputStream.write(mWritebuffer);
                                    writeSuccess = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.err.println("TCPClient WritingThread sendData mOutputStream = null isConnecting="
                                        + isConnecting);
                            }
                        } else {
                            System.err.println("TCPClient WritingThread sendData mWritebuffer = null isConnecting="
                                    + isConnecting);
                        }
                        mWritebuffer = null;
                        if (mListener != null) {
                            mListener.onDataSent(writeSuccess, mDataTag);
                        }
                    }
                } catch (InterruptedException e) {
                    if (!mCancel) {
                        e.printStackTrace();
                    }
                }
            }
            if (!mCancel) {
                closeAllResourse();
                if (mListener != null) {
                    mListener.onSocketDisconnected();
                }
            }
            super.run();
        }
    }
}
