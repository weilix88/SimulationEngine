package main.java.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import main.java.config.EngineConfig;
import main.java.core.Task;
import main.java.multithreading.SimEngine;
import main.java.util.RandomUtil;
import main.java.util.ServletUtil;

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
		LOG.info("Sim request receiver receives request");
		
		String version = null;
		String weatherFile = null;
		String commitId = null;
		File idfFile = null;
		String simBasePath = EngineConfig.readProperty("SimulationBasePath");
		
		String newFolder = RandomUtil.genRandomStr();
		File folder = new File(simBasePath+newFolder);
		if(folder.exists()){
			FileUtils.cleanDirectory(folder);
		}else {
			folder.mkdirs();
		}
		
		LOG.info("Sim request receiver built working directory");
		
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
	        
	        LOG.info("Sim request receiver built task "+requestId);
	        
	        task.enqueue();
	        task.update();
	        
	        LOG.info("Sim request receiver put task equeue "+requestId);
	        
	        SimEngine.wakeSimEngine();
	        
	        LOG.info("Sim request receiver wakes up simulation engine "+requestId);
			
			jo.addProperty("status", "success");
			jo.addProperty("requestId", requestId);
		}
		
		ServletUtil.returnJsonResult(resp, jo);
		LOG.info("Sim request receiver sends return results");
	}
}
