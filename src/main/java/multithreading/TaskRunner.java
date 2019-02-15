package main.java.multithreading;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import main.java.azure.fileStorage.AzureFileUploader;
import main.java.cloud.*;
import main.java.config.EngineConfig;
import main.java.httpClientConnect.StatusReporter;
import main.java.util.FileUtil;
import main.java.util.ProcessUtil;
import main.java.util.RandomUtil;
import main.java.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static main.java.util.FileUtil.TAG;

public class TaskRunner implements Runnable {
    private final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    //private Task task;
    //private Jedis jedis;

    private JsonObject jo;
    private RedisAccess access;

    /*public TaskRunner(Task task) {
        this.task = task;
    }*/

    public TaskRunner(JsonObject jo, RedisAccess access) {
        this.jo = jo;
        this.access = access;
    }

    /*public Task getTask() {
        return this.task;
    }*/

    /**
     * Return created energy plus batch file path
     *
     * @param version
     * @param targetFolder
     * @return
     */
    private String createEnergyPlusBatchFile(String version, String targetFolder, String energyPlusPath) {
        String programPath = "set program_path=";
        String weatherPath = "set weather_path=";

        File batchFile = new File(energyPlusPath + "RunEPlus.bat");
        File destFile = new File(targetFolder + "RunEPlus.bat");

        // reading file and write to the new file
        String line = null;
        String lineBreaker = System.lineSeparator();
        try (FileInputStream fis = new FileInputStream(batchFile);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader batchBR = new BufferedReader(isr);

             FileOutputStream fos = new FileOutputStream(destFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
             BufferedWriter destBW = new BufferedWriter(osw)) {
            while ((line = batchBR.readLine()) != null) {
                if (line.contains(programPath)) {
                    destBW.write(programPath + energyPlusPath + lineBreaker);
                } else if (line.contains(weatherPath)) {
                    destBW.write(weatherPath + targetFolder + lineBreaker);
                } else {
                    destBW.write(line + lineBreaker);
                }
            }
            destBW.flush();

            return targetFolder + "RunEPlus.bat";
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    private String copyWeatherFile(String weatherFile, String branchKey, String idfPath) {
        File src = null;

        CloudFileDownloader downloader = CloudFileDownloadFactory.getCloudFileDownloader();
        if (weatherFile != null && !weatherFile.isEmpty()) {
            String country = weatherFile.split("_")[0];
            String state = weatherFile.split("_")[1];

            if (downloader != null) {
                String path = country + "/" + state;
                src = downloader.downloadWeatherFile(path, weatherFile + ".epw");
            } else {
                try {
                    URI uri = new URI("file:///" + EngineConfig.readProperty("WeatherFilesBasePath") + country + "/" + state + "/" + weatherFile + ".epw");
                    src = new File(uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (branchKey != null && !branchKey.isEmpty()) {
                if (downloader != null) {
                    src = downloader.downloadCustomeWeatherFile(GlobalConstant.CUSTOM_WEATHER_FILE_PATH, branchKey + ".epw");
                } else {
                    String path = EngineConfig.readProperty("SimulationBasePath") + branchKey + ".epw";
                    src = new File(path);
                }
            }
        }

        if (src != null) {
            try {
                File dest = new File(idfPath + "\\weatherfile.epw");

                FileUtils.copyFile(src, dest);
                return idfPath + "\\weatherfile.epw";
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private String downLoadCustomScheduleCSVFile(String branchKey, String idfPath) {
        File zipFile = null;

        if (branchKey != null && !branchKey.isEmpty()) {
            CloudFileDownloader downloader = CloudFileDownloadFactory.getCloudFileDownloader();
            if (downloader != null) {
                zipFile = downloader.downloadCustomeScheduleCSVFile(GlobalConstant.CUSTOM_SCHEDULE_FILE_PATH, branchKey + ".zip");
            } else {
                String path = EngineConfig.readProperty("SimulationBasePath") + branchKey + ".zip";
                zipFile = new File(path);
            }
        }

        if (zipFile != null) {
            // unzip file
            int bytesRead;
            byte[] dataBuffer = new byte[1024];
            try (FileInputStream zipFis = new FileInputStream(zipFile);
                 ZipInputStream zipIs = new ZipInputStream(zipFis)) {
                ZipEntry entry = zipIs.getNextEntry();
                while (entry != null) {
                    OutputStream outputStream = new FileOutputStream(idfPath + "\\" + entry.getName());
                    while ((bytesRead = zipIs.read(dataBuffer)) != -1) {
                        outputStream.write(dataBuffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    outputStream.close();

                    entry = zipIs.getNextEntry();
                }
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return null;
    }

    private void downloadCSV(String idfPath, JsonArray csvs, String bucketName) {
        //JsonArray ja = task.getCsvs();
        if (csvs != null && csvs.size() > 0) {
            //String bucketName = task.getS3Bucket();
            String path = PathUtil.PROJECT_SCHEDULE;

            CloudFileDownloader downloader = CloudFileDownloadFactory.getCloudFileDownloader();
            if (downloader == null) {
                LOG.error("Cannot download schedule CSV", new IllegalStateException());
                return;
            }

            for (int i = 0; i < csvs.size(); i++) {
                String fileName = csvs.get(i).getAsString();
                File csvFile = downloader.downloadScheduleFile(bucketName, path, fileName);

                File dest = new File(idfPath + "\\" + fileName);
                try {
                    FileUtils.copyFile(csvFile, dest);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private boolean checkVersion(String version, String target) {
        String[] splitVersion = version.split("\\.");
        String[] splitTarget = target.split("\\.");

        try {
            int v1 = Integer.parseInt(splitVersion[0]);
            int t1 = Integer.parseInt(splitTarget[0]);

            if (v1 < t1) {
                return false;
            } else if (v1 > t1) {
                return true;
            } else {
                int v2 = Integer.parseInt(splitVersion[1]);
                int t2 = Integer.parseInt(splitTarget[1]);
                return v2 >= t2;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
        }
        return false;
    }

    @Override
    public void run() {
        String weatherFile;
        String branchKey;
        String commitId = "";
        String parallelAgent = "";
        String simBasePath;
        String newFolder;
        String version;
        String requestId = null;
        String energyPlusPath;

        boolean expandObjects = false;
        boolean outputESO = true;
        boolean hasScheduleCSV = false;

        File folder = null;

        try {
            /** read data */
            version = jo.get("version").getAsString();
            weatherFile = jo.get("weather_file").getAsString();
            requestId = jo.get("request_id").getAsString();
            branchKey = jo.get("sim_branch_key").getAsString();

            if (jo.has("commit_id")) {
                commitId = jo.get("commit_id").getAsString();
            }

            if (jo.has("parallel_agent")) {
                parallelAgent = jo.get("parallel_agent").getAsString();
            }

            StatusReporter.sendLog(commitId, parallelAgent, "Task runner starts to run " + requestId, "log");

            List<String> pids = ProcessUtil.getPIDs();
            StatusReporter.sendLog(commitId, parallelAgent, "Running simulation: " + SimulationManager.INSTANCE.getRunningSimulation() +" vs "+pids.size(), "log");

            if (jo.has("expand_objects")) {
                expandObjects = jo.get("expand_objects").getAsBoolean();
            }

            if (jo.has("output_eso")) {
                outputESO = jo.get("output_eso").getAsString().equalsIgnoreCase("yes");
            }

            if (jo.has("customize_csv")) {
                hasScheduleCSV = jo.get("customize_csv").getAsBoolean();
            }

            StatusReporter.sendLog(commitId, parallelAgent, "Expand: " + expandObjects + ", eso: " + outputESO + ", csv: " + hasScheduleCSV, "log");

            energyPlusPath = FileUtil.getEnergyPlusPath(version);

            /** create work directory */
            simBasePath = EngineConfig.readProperty("SimulationBasePath");
            newFolder = RandomUtil.genRandomStr();
            folder = new File(simBasePath + newFolder);
            if (folder.exists()) {
                try {
                    FileUtils.cleanDirectory(folder);
                } catch (IOException e) {
                    StatusReporter.sendLog(commitId, parallelAgent, "Create work dir failed: " + e.getMessage(), "error");
                }
            } else {
                folder.mkdirs();
            }
        }catch (Throwable e){
        	LOG.error("Init simulation folder failed: " + e.getMessage());
            StatusReporter.sendLog(commitId, parallelAgent, "Init simulation folder failed: " + e.getMessage(), "error");

            try(StringWriter errors = new StringWriter();
                PrintWriter pw = new PrintWriter(errors)){
                e.printStackTrace(pw);
                StatusReporter.sendLog(commitId, parallelAgent, "Error stack trace: " + errors.toString(), "log");
            }catch (IOException ex){
            }

            /**
             * clean up request id, PID records
             */
            SimulationManager.INSTANCE.finishSimulation(requestId);
            StatusReporter.sendStatus(commitId, parallelAgent, "finished", "finished");

            if(folder!=null){
                try {
                    folder.delete();
                }catch(Exception ex){}
            }

            /**
             * try to run next simulation
             */
            SimEngine.wakeSimEngine();
            try {
                this.access.close();
            } catch (IOException e1) {}
            return;
        }


        String[] commandline;
        String path;
        try {
            StatusReporter.sendLog(commitId, parallelAgent, "Sim request receiver built working directory: " + simBasePath + newFolder, "log");

            path = simBasePath + newFolder + "\\";
            String batchPath = createEnergyPlusBatchFile(version, path, energyPlusPath);

            StatusReporter.sendLog(commitId, parallelAgent, "Task runner copied batch file: " + batchPath, "log");

            if (batchPath == null) {
                StatusReporter.sendLog(commitId, parallelAgent, "Cannot create simulation batch file", "severe_error");
                return;
            }

            String weatherFileFlag = copyWeatherFile(weatherFile, branchKey, path);
            StatusReporter.sendLog(commitId, parallelAgent, "Weather file " + weatherFileFlag, "log");

            if (weatherFileFlag == null) {
                StatusReporter.sendLog(commitId, parallelAgent, "Weather file cannot found: " + weatherFileFlag, "severe_error");
                return;
            }

            downloadCSV(path, jo.get("csvs").getAsJsonArray(), jo.get("s3_bucket").getAsString());
            StatusReporter.sendLog(commitId, parallelAgent, "Task runner downloaded CSV files", "log");

            /** create IDF file and write content to it */
            File idfFile = new File(path + "IDF.idf");
            String idfContent = jo.get("idf_content").getAsString();
            try (FileWriter fw = new FileWriter(idfFile);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(idfContent);
                bw.flush();
            } catch (IOException e) {
                StatusReporter.sendLog(commitId, parallelAgent, "Write IDF failed: " + e.getMessage(), "error");
            }
            StatusReporter.sendLog(commitId, parallelAgent, "Sim request receiver copied request IDF into working directory", "log");

            /** create tmp file to save E+ output */
            //File eplusOutput = new File(simBasePath + newFolder + "\\EPlus.out");

            /** if there is external schedule CSV, download and unzip them */
            if (hasScheduleCSV) {
                downLoadCustomScheduleCSVFile(branchKey, path);
            }

            StatusReporter.sendLog(commitId, parallelAgent, "Building command line", "log");

            if (!expandObjects && checkVersion(version, "8.5")) {
                commandline = expandObjects
                        ? new String[]{energyPlusPath + "energyplus.exe", "-x", "-w", "weatherfile.epw", "IDF.idf"}
                        : new String[]{energyPlusPath + "energyplus.exe", "-w", "weatherfile.epw", "IDF.idf"};
            } else {
                commandline = new String[]{batchPath, path + "IDF", "weatherfile"};
            }
        }catch(Throwable e){
        	LOG.error("Prepare simulation failed: " + e.getMessage());
            StatusReporter.sendLog(commitId, parallelAgent, "Prepare simulation failed: " + e.getMessage(), "error");

            try(StringWriter errors = new StringWriter();
                PrintWriter pw = new PrintWriter(errors)){
                e.printStackTrace(pw);
                StatusReporter.sendLog(commitId, parallelAgent, "Error stack trace: " + errors.toString(), "log");
            }catch (IOException ex){
            }

            /**
             * clean up request id, PID records
             */
            SimulationManager.INSTANCE.finishSimulation(requestId);
            StatusReporter.sendStatus(commitId, parallelAgent, "finished", "finished");

            try {
                folder.delete();
            }catch(Exception ex){}

            /**
             * try to run next simulation
             */
            SimEngine.wakeSimEngine();
            try {
                this.access.close();
            } catch (IOException e1) {}
            return;
        }

        BufferedReader stdInput = null;
        try {
            access.rpush("TaskStatus#" + requestId, "Starting");

            access.set("TaskServerIP#" + requestId, InstanceInfo.getPublicIP());
            //LOG.info("Simulation server public IP: " + InstanceInfo.getPublicIP());

            LOG.info("Task runner going to start simulation " + commitId +" - " + requestId);

            // SimulationManager will start simulation and collect PID one by one
            StartSimulationWrapper wrapper = SimulationManager.INSTANCE.startSimulation(requestId, commandline, path);

            /*if (wrapper.pid == null) {
                LOG.error("No process id captured for " + requestId);
            }*/

            StatusReporter.sendLog(commitId, parallelAgent, "Continue simulation", "log");
            stdInput = wrapper.stdInput;
            if (stdInput != null) {
                // read the output from the command
                String s;
                while ((s = stdInput.readLine()) != null) {
                    if (s.contains(path) || s.contains(energyPlusPath)) {
                        continue;
                    }

                    access.rpush("TaskStatus#" + requestId, s + "<br/>");
                    //FileUtil.appendToFile(eplusOutput, System.currentTimeMillis()+" === "+s);

                    StatusReporter.sendStatus(commitId, parallelAgent, s, "Running");
                }

                /*String tryCancelled = access.get("TaskCancelled#" + requestId);
                if (tryCancelled != null && tryCancelled.equalsIgnoreCase("true")) {
                    LOG.info("TaskRunner detected simulation cancellation: " + requestId);

                    access.del("TaskCancelled#" + requestId);

                    access.set("Taskhtml#" + requestId, "");
                    access.set("Taskerr#" + requestId, "");
                    access.set("Taskcsv#" + requestId, "");
                    access.set("Taskmtr#" + requestId, "");
                    access.set("Taskeio#" + requestId, "");
                    access.set("Taskrdd#" + requestId, "");
                } else {*/

                String[] files = new String[9];
                File dir = new File(path);
                for (File f : dir.listFiles()) {
                    String fName = f.getName();
                    if (fName.endsWith(".html") || fName.endsWith(".htm")) {
                        files[0] = fName;
                    } else if (fName.equalsIgnoreCase("eplusout.err") || fName.equalsIgnoreCase("idf.err")) {
                        files[1] = fName;
                    } else if(fName.endsWith("Table.csv")){
                        files[7] = fName;
                    } else if (fName.endsWith(".csv")) {
                        files[2] = fName;
                    } else if (fName.endsWith(".eso")) {
                        files[3] = fName;
                    } else if (fName.endsWith(".mtr")) {
                        files[4] = fName;
                    } else if (fName.endsWith(".eio")) {
                        files[5] = fName;
                    } else if (fName.endsWith(".rdd")) {
                        files[6] = fName;
                    }
                }

                String html = FileUtil.readTextFile(path + files[0]);
                if (html == null || html.isEmpty()) {
                    access.set("Taskhtml#" + requestId, "");
                } else {
                    Document htmlDoc = Jsoup.parse(html);
                    FileUtil.processHTML(htmlDoc);

                    // try to extract EUI value and unit
                    String tableId = "AnnualBuildingUtilityPerformanceSummary:EntireFacility:SiteandSourceEnergy";
                    String firstCellContent = "Net Site Energy";
                    String columnTitle = "Energy Per Total Building Area";

                    Element table = null;
                    Elements tables = htmlDoc.getElementsByAttributeValue(TAG, tableId);
                    if (tables.size() > 0) {
                        table = tables.get(0);
                    }
                    if (table != null) {
                        Elements rows = table.select("tr");
                        JsonObject info = extractHeaderInfo(columnTitle, rows.get(0));

                        if (info != null) {
                            int idx = info.get("idx").getAsInt();
                            String value = readValueFromTable(firstCellContent, idx, rows);

                            if (value != null) {
                                String unit = info.get("unit").getAsString();

                                access.set("Taskeui_unit#" + requestId, unit);
                                access.set("Taskeui_value#" + requestId, value);
                            }
                        }
                    }

                  
                    String processedHTML = htmlDoc.outerHtml();
                    byte[] compressed = FileUtil.compressString(processedHTML);
                    String base64Encoded = Base64.getEncoder().encodeToString(compressed);
                    access.set("Taskhtml#" + requestId, base64Encoded);

                    StatusReporter.sendLog(commitId, parallelAgent, "HTML extraction finished", "log");

                 }

                access.set("Taskerr#" + requestId, readCompressedBase64String(path + files[1]));
                access.set("Taskcsv#" + requestId, readCompressedBase64String(path + files[2]));
                access.set("Tasktable_csv#" + requestId, readCompressedBase64String(path + files[7]));
                if (outputESO && !StringUtil.isNullOrEmpty(files[3])) {
                    String msg = saveLargeResultToFileStorage(commitId, path, "eso" + parallelAgent, path + files[3]);

                    if (!msg.isEmpty()) {
                        StatusReporter.sendLog(commitId, parallelAgent, "Upload ESO failed: " + msg, "error");
                    }
                }
                access.set("Taskmtr#" + requestId, readCompressedBase64String(path + files[4]));
                access.set("Taskeio#" + requestId, readCompressedBase64String(path + files[5]));
                access.set("Taskrdd#" + requestId, readCompressedBase64String(path + files[6]));

                access.rpush("TaskStatus#" + requestId, "Status_FINISHED");
                access.del("TaskServerIP#" + requestId);

                FileUtils.deleteDirectory(new File(path));
            } else {
            	LOG.info("Simulation output stream not captured");
                access.rpush("TaskStatus#" + requestId, "Status_ERROR");
                access.set("TaskErrorMessage#" + requestId, "Simulation output stream not captured");

                StatusReporter.sendLog(commitId, parallelAgent, "Simulation output stream not captured", "severe_error");
            }
        } catch (Throwable e) {
        	LOG.error("Run simulation encounters exception: " + e.getMessage());
            StatusReporter.sendLog(commitId, parallelAgent, "Run simulation encounters exception: " + e.getMessage(), "severe_error");

            try(StringWriter errors = new StringWriter();
                PrintWriter pw = new PrintWriter(errors)){
                e.printStackTrace(pw);
                StatusReporter.sendLog(commitId, parallelAgent, "Error stack trace: " + errors.toString(), "log");
            }catch (IOException ex){
            }
        } finally {
            try {
                this.access.close();
            } catch (IOException e) {
            }

            if (stdInput != null) {
                try {
                    stdInput.close();
                } catch (IOException e) {
                }
            }

            /**
             * clean up request id, PID records
             */
            SimulationManager.INSTANCE.finishSimulation(requestId);

            StatusReporter.sendLog(commitId, parallelAgent, "Running simulation: "+SimulationManager.INSTANCE.getRunningSimulation(), "log");
            StatusReporter.sendStatus(commitId, parallelAgent, "finished", "finished");
            /**
             * try to run next simulation
             */
            SimEngine.wakeSimEngine();
            try {
                this.access.close();
            } catch (IOException e1) {}
        }
    }

    private String readValueFromTable(String firstCellContent, int columnIdx, Elements rows) {
        Iterator<Element> rowIter = rows.iterator();
        rowIter.next();  //skip header

        while (rowIter.hasNext()) {
            Elements tds = rowIter.next().select("td");

            if (tds.get(0).ownText().trim().equalsIgnoreCase(firstCellContent)) {
                return tds.get(columnIdx).ownText();
            }
        }

        return null;
    }

    private JsonObject extractHeaderInfo(String columnTitle, Element header) {
        JsonObject jo = new JsonObject();

        Elements tds = header.select("td");

        int idx = 0;

        Iterator<Element> tdIter = tds.iterator();
        String target = columnTitle + " [";
        while (tdIter.hasNext()) {
            Element td = tdIter.next();
            String content = td.ownText();
            if (content.startsWith(target)) {
                jo.addProperty("unit", content.substring(content.indexOf("[") + 1, content.indexOf("]")));
                jo.addProperty("idx", idx);

                return jo;
            }

            idx++;
        }

        return null;
    }

    private String readCompressedBase64String(String path) {
        String result;
        while ((result = FileUtil.readBase64CompressedString(path)) == null) {
            try {
                Thread.sleep(RandomUtil.getRandom(5 * 60 * 1000));  //sleep at most 5 minutes and try again
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private String saveLargeResultToFileStorage(String commitId, String folderPath, String type, String filePath) {
        String compressedFilePath = folderPath + type + ".zip";
        File compressed = FileUtil.compressFile(compressedFilePath, filePath);

        if (compressed == null) {
            return "Compress " + filePath + " failed";
        }

        // save to file storage
        CloudFileUploader uploader = CloudFileUploaderFactory.getCloudFileUploader();
        if (!EngineConfig.readProperty("platform").equals("azure")) {
            uploader.createFolder(EngineConfig.readProperty("LargeSimResultFileSave"), commitId);
        }

        JsonObject jo = uploader.upload(EngineConfig.readProperty("LargeSimResultFileSave"), commitId + "/", compressed, compressed.getName());
        if (jo.get("status").getAsString().equals("error")) {
            return jo.get("error_msg").getAsString();
        }
        return "";
    }
}

