package com.example.knockly.utils;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SSHUtils {
    private static List<String> cachedHosts = null;  // Cache reachable hosts


    public static List<String> ping(String subnetIP) {
        final ExecutorService es = Executors.newFixedThreadPool(128);
        final List<Future<String>> futures = new ArrayList<>();
        final List<String> hosts = new ArrayList<>();
        final int timeout = 300;

        for (int i = 1; i < 255; i++) {
            String ip = subnetIP + "." + i;
            futures.add(ipIsReachable(es, ip, timeout));
        }

        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null) {
                    hosts.add(result);
                }
            } catch (Exception ignored) {}
        }

        es.shutdown();
        return hosts;
    }

    private static boolean isReachable(String ip, int port, int timeout) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Future<String> ipIsReachable(final ExecutorService es, final String ip, final int timeout) {
        return es.submit(() -> {
            try {
                if (InetAddress.getByName(ip).isReachable(timeout) || isReachable(ip, 80, timeout)) {
                    Log.d(TAG, "ping() :: "+ip + " is reachable");
                    return ip;
                }
            } catch (Exception ignored) {}
            return null;
        });
    }

    public static int getLocalHost(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wm.getConnectionInfo().getIpAddress();
    }

    public static String formatToIPV4(int address) {
        return Formatter.formatIpAddress(address);
    }

    public static short getSubnetFormat() throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime()
                .exec(new String[] {"su", "--command", "Djava.net.preferIPv4Stack=true"});
        proc.waitFor();

        InetAddress local = Inet4Address.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(local);
        return networkInterface.getInterfaceAddresses().get(1).getNetworkPrefixLength();
    }

    public static String getSubnetIP(int address)
    {
        String ipString = String.format(
                "%d.%d.%d",
                (address & 0xff),
                (address >> 8 & 0xff),
                (address >> 16 & 0xff));

        return ipString;
    }

    public static void initializeNetworkScan(Context context) {
        if (cachedHosts != null) return;

        int localHost = getLocalHost(context);
        String subnet = getSubnetIP(localHost);
        cachedHosts = ping(subnet);
        Log.d(TAG, "initializeNetworkScan() :: Found hosts = " + cachedHosts);
    }

    public static String getPiIP(Context context, String username, String password) {
        if (cachedHosts == null) initializeNetworkScan(context); // Ensure ping is done

        final ExecutorService es = Executors.newFixedThreadPool(32);
        List<Future<String>> futures = new ArrayList<>();

        for (String ip : cachedHosts) {
            futures.add(es.submit(() -> {
                Session session = startSSH(username, ip, password);
                if (session != null && session.isConnected()) {
                    session.disconnect();
                    Log.d(TAG, "getPiIP() :: SSH successful on " + ip);
                    return ip;
                }
                return null;
            }));
        }

        try {
            for (Future<String> future : futures) {
                try {
                    String result = future.get(1500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (result != null) {
                        es.shutdownNow();
                        return result;
                    }
                } catch (Exception ignored) {}
            }
        } finally {
            es.shutdownNow();
        }

        return "";
    }

    public static Session startSSH(String username, String host, String password) {
        try{
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, 22);
            session.setPassword(password);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);

            session.connect();

            System.out.println("SSH session to "+username+" established");

            return session;
        } catch (JSchException e) {
            e.getMessage();
            return null;
        }
    }

    public static int execSSHCmd(String cmd, Session session) {
        try{
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(cmd);
            channel.setInputStream(null);
            ((ChannelExec)channel).setErrStream(System.err);
            channel.setOutputStream(System.out);

            InputStream in=channel.getInputStream();
            StringBuilder output = new StringBuilder();
            int exitStatus;

            channel.connect();
            while (true) {
                for (int c; ((c = in.read()) >= 0);) {
                    output.append((char) c);
                }
                System.out.println(output);

                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    exitStatus = channel.getExitStatus();
                    System.out.println("EXIT STATUS: "+exitStatus);
                    break;
                }
            }
            channel.disconnect();

            System.out.println("CMD:\n"+cmd+"\nSTATUS COMPLETE");
            return 200;
        } catch (JSchException | IOException e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    public static void stopSSH(Session sshSession) {
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
            System.out.println("SSH session disconnected.");
        }
    }

}
