package main.java.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

public class Task {
    
    String status = null;
    String requestId = null;
    String name = null;
    String receiveTime = null;
    String finishTime = null;
    Double progress = null;
    String errorText = null;
    String resultText = null;
    String stdOut = null;
    //String modelText = null;
    String idfFilePath = null;
    String version = null;
    String weatherFile = null;
    String commitId = null;
    
    String htmlContent = "";
    String errorContent = "";
   
    public String getHtmlContent() {
		return htmlContent;
	}
	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}
	public String getErrorContent() {
		return errorContent;
	}
	public void setErrorContent(String errorContent) {
		this.errorContent = errorContent;
	}
	public String getWeatherFile() {
		return weatherFile;
	}
	public void setWeatherFile(String weatherFile) {
		this.weatherFile = weatherFile;
	}
	public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }   

    public String getRequestId() {
        return this.requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getReceiveTime() {
        return this.receiveTime;
    }
    public void setReceiveTime(String receiveTime) {
        this.receiveTime = receiveTime;
    }

    public String getFinishTime() {
        return this.finishTime;
    }
    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }
    
    public Double getProgress() {
        return this.progress;
    }
    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getErrorText() {
        return this.errorText;
    }
    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public String getResultText() {
        return this.resultText;
    }
    public void setResultText(String resultText) {
        this.resultText = resultText;
    }
   
    public String getStdOut() {
        return this.stdOut;
    }
    public void setStdOut(String stdOut) {
        this.stdOut = stdOut;
    }
    
    public String getCommitId(){
    	return this.commitId;
    }
    
    public void setCommitId(String commitId){
    	this.commitId = commitId;
    }
    
    /*public String getModelText() {
        return this.modelText;
    }
    public void setModelText(String modelText) {
        this.modelText = modelText;
    }*/
    
    public void enqueue() {
        Gson gson = new Gson();
        String jsonInString = gson.toJson(this);
        try(Jedis jedis = new Jedis("localhost")) {
            jedis.rpush("TaskQueue", jsonInString);
            jedis.quit();
        }
    }
    
    public void update() {
        Gson gson = new Gson();
        String jsonInString = gson.toJson(this);
        try(Jedis jedis = new Jedis("localhost")) {
            jedis.set("TaskStatus" + "#" + this.getName() + "#" + this.getRequestId(), jsonInString);
            jedis.quit();
        }
    }
    
    public void load(String name, String requestId) {
        Gson gson = new Gson();
        try(Jedis jedis = new Jedis("localhost")) {
            Set<String> keys = jedis.keys("TaskStatus" + "#" + name + "#" + requestId);
            String key = keys.toArray(new String[keys.size()])[0];
            String jsonInString = jedis.get(key);
            Task task = gson.fromJson(jsonInString, Task.class);
            this.status = task.getStatus();
            this.requestId = task.getRequestId();
            this.name = task.getName();
            this.receiveTime = task.getReceiveTime();
            this.finishTime = task.getFinishTime();
            this.progress = task.getProgress();
            this.errorText = task.getErrorText();
            this.resultText = task.getResultText();
            this.stdOut = task.getStdOut();
            //this.modelText = task.getModelText();
            this.idfFilePath = task.getIdfFilePath();
            this.version = task.getVersion();
            this.commitId = task.getCommitId();
            
            jedis.quit();
        }
    }
    
    /*public void writeModelFile(String filePath) {
        List<String> lines = Arrays.asList(this.getModelText().split("\n"));
        Path file = Paths.get(filePath);
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    
    public void loadErrorText(String filePath) {
        File f = new File(filePath);
        if (f.exists() && !f.isDirectory()) {
            this.errorText = readFile(filePath);
        }
    }
    
    public String getIdfFilePath() {
		return idfFilePath;
	}
	public void setIdfFilePath(String idfFilePath) {
		this.idfFilePath = idfFilePath;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public void loadResultText(String filePath) {
        File f = new File(filePath);
        if (f.exists() && !f.isDirectory()) {
            this.resultText = readFile(filePath);
        }
    }
    
    public String readFile(String filePath) {
            StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
        		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        		BufferedReader br = new BufferedReader(isr)) {
    
            String sCurrentLine;
    
            while ((sCurrentLine = br.readLine()) != null) {
            	sb.append(sCurrentLine).append("\r\n");
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return sb.toString();
    }
}    
