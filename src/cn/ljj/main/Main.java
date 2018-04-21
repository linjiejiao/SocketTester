package cn.ljj.main;

import cn.ljj.tester.TCPEchoServer;
import cn.ljj.tester.UDPEcho;

public class Main {

    public static void main(String[] args) {
        // TCP Echo
        new TCPEchoServer().start(12345);
        // UDP Echo
        new UDPEcho().start(12345);;
    }
}
