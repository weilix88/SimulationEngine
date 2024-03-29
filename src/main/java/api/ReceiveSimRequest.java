package main.java.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.multithreading.SimEngine;

@WebServlet(urlPatterns="/ReceiveSimRequest")
@MultipartConfig
public class ReceiveSimRequest extends HttpServlet{
    private static final Logger LOG = LoggerFactory.getLogger(ReceiveSimRequest.class);
    
    private static final long serialVersionUID = 6839124705054392196L;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SimEngine.wakeSimEngine();
        /*req.setCharacterEncoding("utf-8");
        
        String version = null;
        String weatherFile = null;
        String commitId = null;
        String s3Bucket = null;
        JsonArray csvs = new JsonArray();
        File idfFile = null;
        String simBasePath = EngineConfig.readProperty("SimulationBasePath");
        
        String newFolder = RandomUtil.genRandomStr();
        File folder = new File(simBasePath+newFolder);
        if(folder.exists()){
            FileUtils.cleanDirectory(folder);
        }else {
            folder.mkdirs();
        }
        
        LOG.info("Sim request receiver built working directory: "+simBasePath+newFolder);
        
        for(Part part : req.getParts()){
            String fieldName = part.getName();
            InputStream is = part.getInputStream();

            if(fieldName.equals("idf")){
                try {
                    idfFile = new File(simBasePath+newFolder+"\\IDF.idf");
                    try(FileOutputStream out = new FileOutputStream(idfFile)){
                        IOUtils.copy(is, out);
                    }
                    
                    LOG.info("Sim request receiver copied request IDF into working directory");
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
                break;
            }
            if(fieldName.equals("version")){
                version = IOUtils.toString(is, "UTF-8");
            }
            if(fieldName.equals("weather_file")){
                weatherFile = IOUtils.toString(is, "UTF-8");
            }
            if(fieldName.equals("sim_commit_id")){
                commitId = IOUtils.toString(is, "UTF-8");
            }
            if(fieldName.equals("s3_bucket")){
                s3Bucket = IOUtils.toString(is, "UTF-8");
            }
            if(fieldName.equals("csvs")){
                String jaContent = IOUtils.toString(is, "UTF-8");
                csvs = new JsonParser().parse(jaContent).getAsJsonArray();
            }
            
            is.close();
        }
        
        LOG.info("Sim request receiver read multi-part data finished");
        
        JsonObject jo = new JsonObject();
        
        if(version==null || idfFile==null || weatherFile==null){
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "Cannot read version or weather file or IDF from request");
        }else {
            String requestId = UUID.randomUUID().toString();
            
            Task task = new Task();
            task.setStatus("Received");
            task.setRequestId(requestId);
            task.setName("IDFVC_Simulation");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            task.setReceiveTime(df.format(new Date()));
            task.setIdfFilePath(simBasePath+newFolder+"\\");
            task.setVersion(version);
            task.setWeatherFile(weatherFile);
            task.setCommitId(commitId);
            task.setS3Bucket(s3Bucket);
            task.setCsvs(csvs);
            
            LOG.info("Sim request receiver built task "+requestId);
            
            task.enqueue();
            task.update();
            
            LOG.info("Sim request receiver put task enqueue "+requestId);
            
            SimEngine.wakeSimEngine();
            
            LOG.info("Sim request receiver wakes up simulation engine "+requestId);
            
            jo.addProperty("status", "success");
            jo.addProperty("requestId", requestId);
            
            try(Jedis cache = new Jedis()){
                cache.set("TaskRunning#"+requestId, "true");
            }
        }
        
        ServletUtil.returnJsonResult(resp, jo);
        LOG.info("Sim request receiver sends return results");*/
    }
}
