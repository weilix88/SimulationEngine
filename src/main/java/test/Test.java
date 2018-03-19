package main.java.test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.LogLevel;

public class Test {
	protected static String get(String address){
        URL url;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }

        HttpURLConnection httpConn;
        try {
            httpConn = (HttpURLConnection)url.openConnection();
        } catch (IOException e) {
        	e.printStackTrace();
            return "";
        }
        httpConn.setRequestProperty("MetaData", "true");
        httpConn.setUseCaches(false);

        int status;
        try {
            status = httpConn.getResponseCode();
            if(status == HttpURLConnection.HTTP_OK){
                try(BufferedInputStream bis = new BufferedInputStream(httpConn.getInputStream());
                    InputStreamReader isReader = new InputStreamReader(bis);
                    BufferedReader reader = new BufferedReader(isReader)){
                    String line;
                    StringBuilder sb = new StringBuilder();

                    while((line=reader.readLine()) != null){
                        sb.append(line);
                    }

                    httpConn.disconnect();

                    return sb.toString();
                }
            }else {
                System.err.println("HTTP request "+address+", Server returned abnormal status: "+status);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }

        return "";
    }
	
    public static void main(String[] args) throws IOException, InterruptedException {
        //String response = Test.get("http://169.254.169.254/metadata/instance?api-version=2017-08-01");
        //System.out.println(response);
    	
    	final File credFile = new File("C:\\auth_file");

    	Azure azure = Azure
    	        .configure()
    	        .withLogLevel(LogLevel.NONE)
    	        .authenticate(credFile)
    	        .withDefaultSubscription();
    	
    	//VirtualMachine vm = azure.virtualMachines().getById("/subscriptions/c0a6b691-fcc7-4a5b-837a-dc20d1293847/resourceGroups/BuildSimHub/providers/Microsoft.Compute/virtualMachines/SimEngine");
    	PagedList<VirtualMachine> list = azure.virtualMachines().list(); // not working
    	
    	for(VirtualMachine vm : list) {
    		System.out.println(vm.vmId());
    	}
    }
}
