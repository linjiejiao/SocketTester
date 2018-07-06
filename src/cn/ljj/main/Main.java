package cn.ljj.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cn.ljj.tester.AddressEchoServer;
import cn.ljj.tester.TCPChannelDetecter;
import cn.ljj.tester.TCPEchoServer;
import cn.ljj.tester.TCPTransferSpeedTestServer;
import cn.ljj.tester.TCPTransferSpeedTester;
import cn.ljj.tester.UDPEcho;

public class Main {

    public static final int DEFAULT_PORT = 12345;
    public static final int DEFAULT_INTERVAL = 60; // 1 minute

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            String mode = args[0];
            if ("server".equals(mode)) {
                int tcpEchoServerPort = getPort(args, DEFAULT_PORT);
                int udpEchoServerPort = tcpEchoServerPort;
                int addressEchoServerPort = udpEchoServerPort + 1;
                int tcpSpeedServerPort = addressEchoServerPort + 1;
                // TCP Echo
                System.out.println("TCPEchoServer start listening on:" + tcpEchoServerPort);
                TCPEchoServer tcpEchoServer = new TCPEchoServer();
                tcpEchoServer.start(tcpEchoServerPort);
                // UDP Echo
                System.out.println("UDPEcho start listening on:" + udpEchoServerPort);
                UDPEcho updEcho = new UDPEcho();
                updEcho.start(udpEchoServerPort);
                // Address Echo
                System.out.println("AddressEchoServer start listening on:" + addressEchoServerPort);
                AddressEchoServer addressEchoServer = new AddressEchoServer();
                addressEchoServer.start(addressEchoServerPort);
                // speed test
                System.out.println("TCPTransferSpeedTestServer start listening on:" + tcpSpeedServerPort);
                TCPTransferSpeedTestServer tcpTransferSpeedTestServer = new TCPTransferSpeedTestServer();
                tcpTransferSpeedTestServer.startListen(tcpSpeedServerPort);
                if (isWaitingForUserCmd(args)) {
                    waitForExit();
                    tcpEchoServer.stop();
                    updEcho.stop();
                    addressEchoServer.stop();
                    tcpTransferSpeedTestServer.stopListen();
                }
            } else if ("channelDetect".equals(mode)) {
                String targetIP = getIP(args);
                if (targetIP == null) {
                    System.err.println("ip argument can not be empty!");
                    return;
                }
                int targetPort = getPort(args, DEFAULT_PORT);
                int heartBeatInterval = getInterval(args);
                String channelDetecterLog = getOutputPath(args);
                if (channelDetecterLog == null || channelDetecterLog.length() <= 0) {
                    channelDetecterLog = "ChannelDetecter_" + targetIP + ":" + targetPort + "_"
                            + System.currentTimeMillis() + ".log";
                }
                System.out.println("TCPChannelDetecter start heart beat to:" + targetIP + ":" + targetPort + ", every "
                        + heartBeatInterval + " seconds. Log output>" + channelDetecterLog);
                TCPChannelDetecter detecter = new TCPChannelDetecter(channelDetecterLog);
                detecter.startHeartBeat(targetIP, targetPort, heartBeatInterval);
                if (isWaitingForUserCmd(args)) {
                    waitForExit();
                    detecter.stopHeartBeat();
                }
            } else if ("speedTest".equals(mode)) {
                String speedTargetIP = getIP(args);
                if (speedTargetIP == null) {
                    System.err.println("ip argument can not be empty!");
                    return;
                }
                int speedTargetPort = getPort(args, DEFAULT_PORT + 2);
                int speedTestInterval = getInterval(args);
                String speedTestLog = getOutputPath(args);
                if (speedTestLog == null || speedTestLog.length() <= 0) {
                    speedTestLog = "SpeedTest_" + speedTargetIP + ":" + speedTargetPort + "_"
                            + System.currentTimeMillis() + ".log";
                }
                System.out.println("TCPTransferSpeedTester start speed test to:" + speedTargetIP + ":" + speedTargetPort
                        + ", every " + speedTestInterval + " seconds. Log output>" + speedTestLog);
                TCPTransferSpeedTester tcpTransferSpeedTester = new TCPTransferSpeedTester(speedTestLog);
                tcpTransferSpeedTester.testSpeed(speedTargetIP, speedTargetPort, speedTestInterval);
                if (isWaitingForUserCmd(args)) {
                    waitForExit();
                    tcpTransferSpeedTester.cancelTestSpeed();
                }
            } else {
                System.out.println(
                        "SocketTester [server/channelDetect/speedTest] ip=127.0.0.1 [port=12345] [interval=60] [output=./123.log] [bg]");
            }
        }
    }

    private static void waitForExit() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String input;
            try {
                input = br.readLine();
                if ("exit".equals(input) || "quit".equals(input)) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getPort(String[] args, int defaultPort) {
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String keyWord = "port=";
            if (arg.startsWith(keyWord)) {
                String portString = arg.substring(keyWord.length());
                try {
                    return Integer.parseInt(portString);
                } catch (Exception e) {
                }
            }
        }
        return defaultPort;
    }

    private static String getIP(String[] args) {
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String keyWord = "ip=";
            if (arg.startsWith(keyWord)) {
                return arg.substring(keyWord.length());
            }
        }
        return null;
    }

    private static int getInterval(String[] args) {
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String keyWord = "interval=";
            if (arg.startsWith(keyWord)) {
                String interval = arg.substring(keyWord.length());
                try {
                    return Integer.parseInt(interval);
                } catch (Exception e) {
                }
            }
        }
        return DEFAULT_INTERVAL;
    }

    private static String getOutputPath(String[] args) {
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String keyWord = "output=";
            if (arg.startsWith(keyWord)) {
                return arg.substring(keyWord.length());
            }
        }
        return null;
    }

    private static boolean isWaitingForUserCmd(String[] args) {
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg.equals("bg")) {
                return false;
            }
        }
        return true;
    }
}
