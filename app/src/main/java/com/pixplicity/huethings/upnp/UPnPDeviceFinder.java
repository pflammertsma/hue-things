/*
 * Copyright (C) 2015 Doug Melton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pixplicity.huethings.upnp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscriber;

/**
 * Based on:
 * https://github.com/heb-dtc/SSDPDiscovery/blob/master/src/main/java/com/flo/upnpdevicedetector/UPnPDeviceFinder.java
 */
public class UPnPDeviceFinder {

    private static String TAG = UPnPDeviceFinder.class.getSimpleName();
    private static final boolean VERBOSE = false;

    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int MULTICAST_PORT = 1900;

    private static final int MAX_REPLY_TIME_SECONDS = 60;
    private static final int MULTICAST_TIMEOUT_MILLISECONDS = MAX_REPLY_TIME_SECONDS * 1000;

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private final boolean mUseIPv4;

    public UPnPDeviceFinder() {
        this(true);
    }

    public UPnPDeviceFinder(boolean useIPv4) {
        mUseIPv4 = useIPv4;
    }

    public Observable<UPnPDevice> observe() {
        return Observable.create(new Observable.OnSubscribe<UPnPDevice>() {
            @Override
            public void call(Subscriber<? super UPnPDevice> subscriber) {
                InetAddress localIp = getDeviceLocalIP(mUseIPv4);

                UPnPSocket socket;
                try {
                    socket = new UPnPSocket(localIp);
                } catch (IOException e) {
                    subscriber.onError(e);
                    return;
                }

                try {
                    // Broadcast SSDP search messages
                    socket.sendMulticastMsg();

                    // Listen to responses from network until the socket timeout
                    while (true) {
                        DatagramPacket dp = socket.receiveMulticastMsg();
                        String receivedString = new String(dp.getData());
                        receivedString = receivedString.substring(0, dp.getLength());
                        UPnPDevice device = UPnPDevice.getInstance(receivedString);
                        if (VERBOSE) {
                            Log.d(TAG, "UPnP response from " + dp.getAddress());
                        }
                        if (device != null) {
                            if (VERBOSE) {
                                String name = device.getFriendlyName();
                                if (name == null) {
                                    name = "(unknown device)";
                                }
                                Log.d(TAG, "found device: " + device.getHost() + "; " + name);
                            }
                            subscriber.onNext(device);
                        } else if (VERBOSE) {
                            Log.d(TAG, "found unknown device");
                        }
                    }
                } catch (IOException e) {
                    // Socket timeout will get us out of the loop
                    socket.close();
                    if (e instanceof SocketTimeoutException) {
                        if (VERBOSE) {
                            Log.w(TAG, "finished reading from UPnP responses");
                        }
                        subscriber.onCompleted();
                    } else {
                        Log.e(TAG, "failed reading UPnP responses", e);
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////
    // UPnPSocket
    ////////////////////////////////////////////////////////////////////////////////

    private static class UPnPSocket {

        private static String TAG = UPnPSocket.class.getSimpleName();

        private SocketAddress mMulticastGroup;
        private MulticastSocket mMultiSocket;

        UPnPSocket(InetAddress deviceIp) throws IOException {
            if (VERBOSE) {
                Log.v(TAG, "device IP: " + deviceIp.toString());
            }

            mMulticastGroup = new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT);
            mMultiSocket = new MulticastSocket(new InetSocketAddress(deviceIp, 0));

            mMultiSocket.setSoTimeout(MULTICAST_TIMEOUT_MILLISECONDS);
        }

        public void sendMulticastMsg() throws IOException {
            String ssdpMsg = buildSSDPSearchString();

            if (VERBOSE) {
                Log.v(TAG, "sending multicast: " + ssdpMsg);
            }

            DatagramPacket dp = new DatagramPacket(ssdpMsg.getBytes(), ssdpMsg.length(), mMulticastGroup);
            mMultiSocket.send(dp);
        }

        public DatagramPacket receiveMulticastMsg() throws IOException {
            byte[] buf = new byte[2048];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);

            mMultiSocket.receive(dp);

            return dp;
        }

        /**
         * Closing the Socket.
         */
        public void close() {
            if (mMultiSocket != null) {
                mMultiSocket.close();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Utils
    ////////////////////////////////////////////////////////////////////////////////

    public static final String NEWLINE = "\r\n";

    private static String buildSSDPSearchString() {
        StringBuilder content = new StringBuilder();

        content.append("M-SEARCH * HTTP/1.1").append(NEWLINE);
        content.append("Host: " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT).append(NEWLINE);
        content.append("Man:\"ssdp:discover\"").append(NEWLINE);
        content.append("MX: " + MAX_REPLY_TIME_SECONDS).append(NEWLINE);
        content.append("ST: upnp:rootdevice").append(NEWLINE);
        content.append(NEWLINE);

        if (VERBOSE) {
            Log.v(TAG, content.toString());
        }

        return content.toString();
    }

    private static InetAddress getDeviceLocalIP(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        if (VERBOSE) {
                            Log.v(TAG, "IP from inet is: " + addr);
                        }
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4) {
                                return addr;
                            }
                        } else {
                            if (!isIPv4) {
                                return addr;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            // for now ignore exceptions
        }
        return null;
    }

    private static final boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

}