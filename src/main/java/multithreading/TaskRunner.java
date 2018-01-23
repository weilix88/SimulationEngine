package main.java.multithreading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;

import main.java.aws.meta.GlobalConstant;
import main.java.aws.meta.InstanceInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.aws.meta.PathUtil;
import main.java.aws.redis.RedisAccess;
import main.java.aws.redis.RedisAccessFactory;
import main.java.aws.s3.S3FileDownloader;
import main.java.config.EngineConfig;
import main.java.util.RandomUtil;

public class TaskRunner implements Runnable {
    private final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);
    
    //private Task task;
    //private Jedis jedis;

    private JsonObject jo;
    private RedisAccess access;

    /*public TaskRunner(Task task) {
        this.task = task;
    }*/

    public TaskRunner(JsonObject jo){
        this.jo = jo;
    }

    /*public Task getTask() {
        return this.task;
    }*/
    
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
        
        try {
            File src = null;
            String platform = EngineConfig.readProperty("platform");
            if(platform.equalsIgnoreCase("aws")){
                String path = country+"/"+state+"/";
                S3FileDownloader s3FileDownloader = new S3FileDownloader(null);
                src = s3FileDownloader.download(GlobalConstant.WEATHER_FILE_BUCKET, path, weatherFile+".epw");
            }else {
                URI uri = new URI("file:///"+EngineConfig.readProperty("WeatherFilesBasePath")+country+"/"+state+"/"+weatherFile+".epw");
                src = new File(uri);
            }

            File dest = new File(idfPath+"\\weatherfile.epw");
        
            FileUtils.copyFile(src, dest);
            return idfPath+"\\weatherfile.epw";
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
    
    private void downloadCSV(String idfPath, JsonArray csvs, String bucketName){
        //JsonArray ja = task.getCsvs();
        if(csvs!=null && csvs.size()>0){
            //String bucketName = task.getS3Bucket();
            String path = PathUtil.PROJECT_SCHEDULE+"/";
            
            S3FileDownloader downloader = new S3FileDownloader(null);
            for(int i=0;i<csvs.size();i++){
                String fileName = csvs.get(i).getAsString();
                File csvFile = downloader.download(bucketName, path, fileName);
                
                File dest = new File(idfPath+"\\"+fileName);
                try {
                    FileUtils.copyFile(csvFile, dest);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
    
    private String readTextFile(String path){
        //LOG.info(path);
        
        File f = new File(path);
        if(!f.exists()){
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String line;
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
        /*LOG.info("Task runner starts to run "+task.getRequestId());
        
        String path = task.getIdfFilePath();
        String version = task.getVersion();
        String weatherFile = task.getWeatherFile();
        String energyPlusPath = getEnergyPlusPath(version);
        
        String batchPath = createEnergyPlusBatchFile(version, path, energyPlusPath);
        LOG.info("Task runner copied batch file "+task.getRequestId());*/

        LOG.info("Task runner starts to run "+jo.get("request_id").getAsString());

        /** read data */
        String version = jo.get("version").getAsString();
        String weatherFile = jo.get("weather_file").getAsString();
        String requestId = jo.get("request_id").getAsString();
        String energyPlusPath = getEnergyPlusPath(version);

        /** create work directory */
        String simBasePath = EngineConfig.readProperty("SimulationBasePath");
        String newFolder = RandomUtil.genRandomStr();
        File folder = new File(simBasePath+newFolder);
        if(folder.exists()){
            try {
                FileUtils.cleanDirectory(folder);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }else {
            folder.mkdirs();
        }
        LOG.info("Sim request receiver built working directory: "+simBasePath+newFolder);

        /** create IDF file and write content to it */
        File idfFile = new File(simBasePath+newFolder+"\\IDF.idf");
        String idfContent = jo.get("idf_content").getAsString();
        try(FileWriter fw = new FileWriter(idfFile);
            BufferedWriter bw = new BufferedWriter(fw)){
            bw.write(idfContent);
            bw.flush();
        }catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("Sim request receiver copied request IDF into working directory");

        String path = simBasePath+newFolder+"\\";
        String batchPath = createEnergyPlusBatchFile(version, path, energyPlusPath);
        LOG.info("Task runner copied batch file "+requestId);

        if(batchPath!=null && copyWeatherFile(weatherFile, path)!=null){
            LOG.info("Task runner copied weather file "+requestId);
            
            downloadCSV(path, jo.get("csvs").getAsJsonArray(), jo.get("s3_bucket").getAsString());
            LOG.info("Task runner downloaded CSV files "+requestId);
            
            String[] commandline = {batchPath, path+"IDF", "weatherfile"};
            
            BufferedReader stdInput = null;
            try{
            	this.access = RedisAccessFactory.getAccess();
            	access.rpush("TaskStatus#"+requestId, "Starting");
            	access.expire("TaskStatus#"+requestId);

                access.set("TaskServerIP#"+requestId, InstanceInfo.getPublicIP());
                LOG.info("Simulation server public IP: "+InstanceInfo.getPublicIP());

                LOG.info("Task runner going to start simulation "+requestId);

                // SimulationManager will start simulation and collect PID one by one
                StartSimulationWrapper wrapper = SimulationManager.INSTANCE.startSimulation(requestId, commandline, path);

                if(wrapper.pid==null){
                    LOG.error("No process id captured for "+requestId);
                }

                LOG.info("Continue simulation "+requestId);
                stdInput = wrapper.stdInput;
                if(stdInput!=null){
                    // read the output from the command
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        if(s.contains(path) || s.contains(energyPlusPath)){
                            continue;
                        }

                        access.rpush("TaskStatus#"+requestId, s+"<br/>");
                    }

                    String tryCancelled = access.get("TaskCancelled#"+requestId);
                    if(tryCancelled!=null && tryCancelled.equalsIgnoreCase("true")){
                        LOG.info("TaskRunner detected simulation cancellation: "+requestId);

                        access.del("TaskCancelled#"+requestId);

                        access.set("Taskhtml#"+requestId, "");
                        access.set("Taskerr#"+requestId, "");
                        access.set("Taskcsv#"+requestId, "");
                        access.set("Taskeso#"+requestId, "");
                        access.set("Taskmtr#"+requestId, "");
                    }else {
                        access.set("Taskhtml#"+requestId, readTextFile(path+"IDFTable.html"));
                        access.set("Taskerr#"+requestId, readTextFile(path+"IDF.err"));
                        access.set("Taskcsv#"+requestId, readTextFile(path+"IDF.csv"));
                        access.set("Taskeso#"+requestId, readTextFile(path+"IDF.eso"));
                        access.set("Taskmtr#"+requestId, readTextFile(path+"IDF.mtr"));
                    }

                    access.rpush("TaskStatus#"+requestId, "Status_FINISHED");
                    access.del("TaskServerIP#"+requestId);

                    FileUtils.deleteDirectory(new File(path));

                    LOG.info("Task runner simulation finished "+requestId+", path: "+path);
                }else {
                    access.rpush("TaskStatus#"+requestId, "Status_ERROR");
                    access.rpush("TaskErrorMessage#"+requestId, "Simulation output stream not captured");

                    LOG.error("Simulation output stream not captured for "+requestId);
                }

                /**
                 * clean up request id, PID records
                 */
                SimulationManager.INSTANCE.finishSimulation(requestId);

                /**
                 * try to run next simulation
                 */
                SimEngine.wakeSimEngine();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                try {
                    this.access.close();
                } catch (IOException e) {}

                if(stdInput!=null){
                    try {
                        stdInput.close();
                    } catch (IOException e) {}
                }
            }
        }
    }
}

