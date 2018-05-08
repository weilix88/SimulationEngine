package main.java.api;

import com.google.gson.JsonObject;
import main.java.config.EngineConfig;
import main.java.util.FileUtil;
import main.java.util.RandomUtil;
import main.java.util.ServletUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;

@WebServlet(name = "ConvertIDFEPJson", urlPatterns = "/ConvertIDFToEPJson")
@MultipartConfig
public class ConvertIDFEPJson extends HttpServlet {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("IDF to epJSON receives request");

        String version = null;
        File file = null;

        String basePath = EngineConfig.readProperty("tmpfolder");
        String newFolder = RandomUtil.genRandomStr();
        String path = basePath+newFolder;

        File folder = new File(path);
        if(folder.exists()){
            FileUtils.cleanDirectory(folder);
        }else {
            folder.mkdirs();
        }

        LOG.info("IDF to epJSON request receiver built working directory");

        for(Part part : req.getParts()){
            String fieldName = part.getName();
            InputStream is = part.getInputStream();

            if(fieldName.equals("idf")){
                try {
                    file = new File(path+"\\test.idf");
                    try(FileOutputStream out = new FileOutputStream(file)){
                        IOUtils.copy(is, out);
                    }

                    LOG.info("IDF to epJSON request receiver copied idf into working directory");
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
                break;
            }
            if(fieldName.equals("version")){
                version = IOUtils.toString(is, "UTF-8");
            }

            is.close();
        }

        LOG.info("IDF to epJSON request receiver read multi-part data finished");

        JsonObject jo = new JsonObject();
        if(file==null){
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "Cannot read IDF file from request");
            ServletUtil.returnJsonResult(resp, jo);
            return;
        }

        String energyPlusPath = FileUtil.getEnergyPlusPath("8.9");
        String[] commandLines = {energyPlusPath+"energyplus.exe", "-c" ,"test.idf"};
        try {
            Process pr = Runtime.getRuntime().exec(commandLines, null, folder);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            // read the output from the command
            while (stdInput.readLine() != null);

            String content = FileUtil.readStringFromFile(new File(path+"\\test.epJSON"));
            jo.addProperty("status", "success");
            jo.addProperty("content", content);
        }catch (IOException e) {
            LOG.error(e.getMessage(), e);
            jo.addProperty("status", "error");
            jo.addProperty("content", e.getMessage());
        }

        ServletUtil.returnJsonResult(resp, jo);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }
}
