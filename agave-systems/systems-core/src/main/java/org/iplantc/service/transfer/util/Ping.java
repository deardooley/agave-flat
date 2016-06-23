package org.iplantc.service.transfer.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.iplantc.service.common.Settings;

public class Ping {

    public static void runSystemCommand(String command) {

        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            String s = "";
            // reading output stream of the command
            while ((s = inputStream.readLine()) != null) {
                System.out.println(s);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        
        if (args.length < 1) {
            System.err.println("Usage: java Ping host...");
            return;
        }
        System.out.println(Settings.fork("ping -c 10 -f -q " + args[0]));

    }
}