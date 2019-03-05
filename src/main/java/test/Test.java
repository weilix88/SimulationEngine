package main.java.test;

import main.java.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        String[] commandline = {
            "C:\\EnergyPlusV8-8-0\\energyplus.exe",
            "-w",
            "weather.epw",
            "5ZoneAirCooled.idf",
        };
        Process pr = Runtime.getRuntime().exec(commandline, null, new File("D:\\TestOut\\test\\"));
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
    }
}
