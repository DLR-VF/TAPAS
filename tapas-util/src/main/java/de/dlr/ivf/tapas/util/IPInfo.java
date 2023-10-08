/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * This class provides methods to determine the IP addresses of the computer's network adapters
 *
 * @author mark_ma
 */
public class IPInfo {

    /**
     * @return IP address of ethernet network adapter
     * @throws IOException thrown if a error occurs
     */
    public static InetAddress getEthernetInetAddress() throws IOException {
        return InetAddress.getLocalHost();
        //we have linux servers, which have different interface names than ethX
        //return getInetAddress("eth");
    }

    /**
     * retrieves the HOSTNAME where the JVM is currently running using the 'hostname' runtime command
     *
     * @return HOSTNAME
     */
    public static String getHostname() throws IOException {

        String line;
        StringBuilder result = new StringBuilder();
        Process p = Runtime.getRuntime().exec(new String[]{"hostname"});

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

        while ((line = br.readLine()) != null) result.append(line);

        br.close();

        return result.toString();
    }

    /**
     * @param namePrefix prefix of the network adapter name e.g. "lo" for localhost
     * @return IP address of the network adapter with the given name prefix
     * @throws IOException thrown if a error occurs
     */
    public static InetAddress getInetAddress(String namePrefix) throws IOException {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if (ni.getName().startsWith(namePrefix)) {
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    InetAddress ia = ias.nextElement();

                    byte b4 = ia.getAddress()[3];
                    if (b4 == 0 || b4 == 1 || b4 == 255) {
                        continue;
                    }
                    // if (ia.isReachable(2500)) {
                    return ia;
                    // }
                }
            }
        }
        return null;
    }

    /**
     * @return IP address of the localhost
     * @throws IOException thrown if a error occurs
     */
    public static InetAddress getLocalhost() throws IOException {
        return getInetAddress("lo");
    }
}
