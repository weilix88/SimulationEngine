package main.java.multithreading;

import static main.java.util.FileUtil.TAG;

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
import java.util.Base64;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.cloud.CloudFileDownloadFactory;
import main.java.cloud.CloudFileDownloader;
import main.java.cloud.GlobalConstant;
import main.java.cloud.InstanceInfo;
import main.java.cloud.PathUtil;
import main.java.cloud.RedisAccess;
import main.java.cloud.RedisAccessFactory;
import main.java.config.EngineConfig;
import main.java.util.FileUtil;
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

    public TaskRunner(JsonObject jo) {
        this.jo = jo;
    }

    /*public Task getTask() {
        return this.task;
    }*/

    private String getEnergyPlusPath(String version) {
        return EngineConfig.readProperty("EnergyPlusBasePath") + "EnergyPlusV" + version.replaceAll("\\.", "-") + "-0\\";
    }

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

    private String copyWeatherFile(String weatherFile, String commitId, String idfPath) {
        File src = null;

        CloudFileDownloader downloader = CloudFileDownloadFactory.getCloudFileDownloader();        
        if(weatherFile!=null && !weatherFile.isEmpty()){
            String country = weatherFile.split("_")[0];
            String state = weatherFile.split("_")[1];

            if (downloader!=null) {
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
        }else {
            if (downloader!=null) {
                src = downloader.downloadCustomeWeatherFile(GlobalConstant.CUSTOM_WEATHER_FILE_PATH, commitId + ".epw");
            }else {
                String path = EngineConfig.readProperty("SimulationBasePath")+commitId+".epw";
                src = new File(path);
            }
        }

        if(src != null) {
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

    private void downloadCSV(String idfPath, JsonArray csvs, String bucketName) {
        //JsonArray ja = task.getCsvs();
        if (csvs != null && csvs.size() > 0) {
            //String bucketName = task.getS3Bucket();
            String path = PathUtil.PROJECT_SCHEDULE;

            CloudFileDownloader downloader = CloudFileDownloadFactory.getCloudFileDownloader();
            if(downloader==null) {
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


    @Override
    public void run() {
        LOG.info("Task runner starts to run " + jo.get("request_id").getAsString());

        /** read data */
        String version = jo.get("version").getAsString();
        String weatherFile = jo.get("weather_file").getAsString();
        String requestId = jo.get("request_id").getAsString();
        String commitId = jo.get("sim_commit_id").getAsString();
        String energyPlusPath = getEnergyPlusPath(version);

        /** create work directory */
        String simBasePath = EngineConfig.readProperty("SimulationBasePath");
        String newFolder = RandomUtil.genRandomStr();
        File folder = new File(simBasePath + newFolder);
        if (folder.exists()) {
            try {
                FileUtils.cleanDirectory(folder);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            folder.mkdirs();
        }
        LOG.info("Sim request receiver built working directory: " + simBasePath + newFolder);

        String path = simBasePath + newFolder + "\\";
        String batchPath = createEnergyPlusBatchFile(version, path, energyPlusPath);
        LOG.info("Task runner copied batch file " + requestId);

        if (batchPath != null && copyWeatherFile(weatherFile, commitId, path) != null) {
            LOG.info("Task runner copied weather file " + requestId);

            downloadCSV(path, jo.get("csvs").getAsJsonArray(), jo.get("s3_bucket").getAsString());
            LOG.info("Task runner downloaded CSV files " + requestId);

            /** create IDF file and write content to it */
            File idfFile = new File(simBasePath + newFolder + "\\IDF.idf");
            String idfContent = jo.get("idf_content").getAsString();
            try (FileWriter fw = new FileWriter(idfFile);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(idfContent);
                bw.flush();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            LOG.info("Sim request receiver copied request IDF into working directory");

            /** create tmp file to save E+ output */
            File eplusOutput = new File(simBasePath + newFolder + "\\EPlus.out");

            String[] commandline = {batchPath, path + "IDF", "weatherfile"};

            BufferedReader stdInput = null;
            try {
                this.access = RedisAccessFactory.getAccess();
                access.rpush("TaskStatus#" + requestId, "Starting");
                access.expire("TaskStatus#" + requestId);

                access.set("TaskServerIP#" + requestId, InstanceInfo.getPublicIP());
                LOG.info("Simulation server public IP: " + InstanceInfo.getPublicIP());

                LOG.info("Task runner going to start simulation " + requestId);

                // SimulationManager will start simulation and collect PID one by one
                StartSimulationWrapper wrapper = SimulationManager.INSTANCE.startSimulation(requestId, commandline, path);

                if (wrapper.pid == null) {
                    LOG.error("No process id captured for " + requestId);
                }

                LOG.info("Continue simulation " + requestId);
                stdInput = wrapper.stdInput;
                if (stdInput != null) {
                    // read the output from the command
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        if (s.contains(path) || s.contains(energyPlusPath)) {
                            continue;
                        }

                        access.rpush("TaskStatus#" + requestId, s + "<br/>");
                        FileUtil.appendToFile(eplusOutput, System.currentTimeMillis()+" === "+s);
                    }

                    String tryCancelled = access.get("TaskCancelled#" + requestId);
                    if (tryCancelled != null && tryCancelled.equalsIgnoreCase("true")) {
                        LOG.info("TaskRunner detected simulation cancellation: " + requestId);

                        access.del("TaskCancelled#" + requestId);

                        access.set("Taskhtml#" + requestId, "");
                        access.set("Taskerr#" + requestId, "");
                        access.set("Taskcsv#" + requestId, "");
                        access.set("Taskeso#" + requestId, "");
                        access.set("Taskmtr#" + requestId, "");
                    } else {
                        String html = FileUtil.readTextFile(path + "IDFTable.html");
                        if (html != null && !html.isEmpty()) {
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

                            if (html == null || html.isEmpty()) {
                                access.set("Taskhtml#" + requestId, "");
                            } else {
                                String processedHTML = htmlDoc.outerHtml();
                                byte[] compressed = FileUtil.compressString(processedHTML);
                                String base64Encoded = Base64.getEncoder().encodeToString(compressed);
                                access.set("Taskhtml#" + requestId, base64Encoded);
                            }
                        }

                        access.set("Taskerr#" + requestId, readCompressedBase64String(path + "IDF.err"));
                        access.set("Taskcsv#" + requestId, readCompressedBase64String(path + "IDF.csv"));
                        access.set("Taskeso#" + requestId, readCompressedBase64String(path + "IDF.eso"));
                        access.set("Taskmtr#" + requestId, readCompressedBase64String(path + "IDF.mtr"));
                    }

                    access.rpush("TaskStatus#" + requestId, "Status_FINISHED");
                    access.del("TaskServerIP#" + requestId);

                    FileUtils.deleteDirectory(new File(path));

                    LOG.info("Task runner simulation finished " + requestId + ", path: " + path);
                } else {
                    access.rpush("TaskStatus#" + requestId, "Status_ERROR");
                    access.set("TaskErrorMessage#" + requestId, "Simulation output stream not captured");

                    LOG.error("Simulation output stream not captured for " + requestId);
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
                } catch (IOException e) {
                }

                if (stdInput != null) {
                    try {
                        stdInput.close();
                    } catch (IOException e) {
                    }
                }
            }
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
}

