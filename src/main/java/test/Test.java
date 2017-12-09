package main.java.test;

import main.java.util.ProcessUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        String batchPath = "D:\\TestOut\\Test\\RunEPlus.bat";
        String path = "D:\\TestOut\\Test\\";
        String[] commandline = {batchPath, path+"IDF", "weatherfile"};

        Process pr = Runtime.getRuntime().exec(commandline, null, new File(path));

        String pid = "";

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        String s;
        int i=0;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);

            if(s.contains("EnergyPlus Starting")){
                List<String> pids = ProcessUtil.getPIDs();
                if(pids.size()>0){
                    pid = pids.get(0);
                }
                System.out.println("PID: "+pid);
                i = 0;
            }
            i++;
            if(i==100){
                if(!pid.isEmpty()){
                    Runtime.getRuntime().exec("taskkill /PID "+pid+" /F");
                    System.out.println("Taskkilled, "+pid);
                }
            }
        }

        stdInput.close();
    }
}
