package main.java.multithreading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.EngineConfig;
import main.java.core.Task;
import redis.clients.jedis.Jedis;

public class TaskRunner implements Runnable {
    private final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);
    
    private Task task;
    //private String output;
    private Jedis jedis;

    public TaskRunner(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return this.task;
    }
    
    private String getEnergyPlusPath(String version){
        return EngineConfig.readProperty("EnergyPlusBasePath")+"EnergyPlusV"+version.replaceAll("\\.", "-")+"-0\\";
    }
    
    /**
     * Return created energy plus batch file path
     * @param version
     * @param targetFolder
     * @return
     */
    private String createEnergyPlusBatchFile(String version, String targetFolder, String energyPlusPath){
        String programPath = "set program_path=";
        String weatherPath = "set weather_path=";
        
        File batchFile = new File(energyPlusPath+"RunEPlus.bat");
        File destFile = new File(targetFolder+"RunEPlus.bat");
        
        // reading file and write to the new file
        String line = null;
        String lineBreaker = System.lineSeparator();
        try(FileInputStream fis = new FileInputStream(batchFile);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader batchBR = new BufferedReader(isr);
                
                FileOutputStream fos = new FileOutputStream(destFile);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                BufferedWriter destBW = new BufferedWriter(osw)){
            while((line=batchBR.readLine()) != null){
                if(line.contains(programPath)){
                    destBW.write(programPath+energyPlusPath+lineBreaker);
                }else if(line.contains(weatherPath)){
                    destBW.write(weatherPath+targetFolder+lineBreaker);
                }else {
                    destBW.write(line+lineBreaker);
                }
            }
            destBW.flush();
            
            return targetFolder+"RunEPlus.bat";
        }catch(IOException e){
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
    
    private String copyWeatherFile(String weatherFile, String idfPath){
        String country = weatherFile.split("_")[0];
        String state = weatherFile.split("_")[1];
        
        File src = new File(EngineConfig.readProperty("WeatherFilesBasePath")+country+"\\"+state+"\\"+weatherFile+".epw");
        File dest = new File(idfPath+"\\weatherfile.epw");
        try {
            FileUtils.copyFile(src, dest);
            return idfPath+"\\weatherfile.epw";
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
    
    private String readTextFile(String path){
        //LOG.info(path);
        
        File f = new File(path);
        if(!f.exists()){
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String line = null;
        try(FileInputStream fis = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr)){
            while((line=br.readLine()) != null){
                sb.append(line).append(System.lineSeparator());
            }
        }catch(IOException e){
            LOG.error(e.getMessage(), e);
            sb.append("Encounter Error While Retrieving Content: "+e.getMessage()+System.lineSeparator());
        }
        return sb.toString();
    }

    @Override
    public void run() {
        LOG.info("Task runner starts to run "+task.getRequestId());
        
        String path = task.getIdfFilePath();
        String version = task.getVersion();
        String weatherFile = task.getWeatherFile();
        String energyPlusPath = getEnergyPlusPath(version);
        
        String batchPath = createEnergyPlusBatchFile(version, path, energyPlusPath);
        LOG.info("Task runner copied batch file "+task.getRequestId());
        
        if(batchPath!=null && copyWeatherFile(weatherFile, path)!=null){
            LOG.info("Task runner copied weather file "+task.getRequestId());
            
            String[] commandline = {batchPath, path+"IDF", "weatherfile"};
            
            BufferedReader stdInput = null;
            BufferedReader stdError = null;
            try{
                LOG.info("Task runner going to start simulation "+task.getRequestId());
                Process pr = Runtime.getRuntime().exec(commandline, null, new File(path));
                
                stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                stdError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                
                this.jedis = new Jedis("localhost");

                // read the output from the command
                String s;
                while ((s = stdInput.readLine()) != null) {
                    //LOG.info(s);
                    if(s.contains(path) || s.contains(energyPlusPath)){
                        continue;
                    }
                    
                    jedis.rpush("TaskStatus#"+task.getRequestId(), s+"<br/>");
                }
                jedis.rpush("TaskStatus#"+task.getRequestId(), "Status_FINISHED");

                // read any errors from the attempted command
                while ((s = stdError.readLine()) != null) {
                    //System.err.println(s);
                    if(s.contains(path) || s.contains(energyPlusPath)){
                        continue;
                    }
                    
                    jedis.rpush("TaskError#"+task.getRequestId(), s+"<br/>");
                }
                jedis.rpush("TaskError#"+task.getRequestId(), "Error_FINISHED");
                
                jedis.set("Taskhtml#"+task.getRequestId(), readTextFile(path+"IDFTable.html"));
                jedis.set("Taskerr#"+task.getRequestId(), readTextFile(path+"IDF.err"));
                jedis.set("Taskhtmlcommitid#"+task.getRequestId(), task.getCommitId());
                jedis.set("Taskerrcommitid#"+task.getRequestId(), task.getCommitId());
                
                
                FileUtils.deleteDirectory(new File(path));
                
                //TODO delete old keys, in case client failed to request status or output file
                
                LOG.info("Task runner simulation finished "+task.getRequestId()+", path: "+path);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                this.jedis.quit();
                this.jedis.close();
                
                if(stdInput!=null){
                    try {
                        stdInput.close();
                    } catch (IOException e) {}
                }
                if(stdError!=null){
                    try {
                        stdError.close();
                    } catch (IOException e) {}
                }
            }
        }
    }
}

