package main.java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtil {
    private static Object GET_PID_LOCK = new Object();
    public static List<String> getPIDs(){
        synchronized (GET_PID_LOCK) {
            List<String> res = new ArrayList<>();
            try {
                Process pr = Runtime.getRuntime().exec("tasklist /fi \"imagename eq energyplus.exe\"");
                try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                    if(stdInput.readLine() != null
                        && stdInput.readLine() != null
                        && stdInput.readLine() != null){

                        String s;
                        while ((s = stdInput.readLine()) != null) {
                            String[] split = s.split("\\s+");
                            res.add(split[1]);
                        }
                    }
                }
            } catch (IOException e) {}
            return res;
        }
    }
}
