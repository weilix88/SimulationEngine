package main.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class ServletUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ServletUtil.class);
    
    public static void returnJsonResult(HttpServletResponse resp, JsonObject jo){
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("json");
        try (PrintWriter pw = resp.getWriter()){
            pw.print(jo);
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void returnString(HttpServletResponse resp, String str){
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("text");
        try(PrintWriter pw = resp.getWriter()){
            pw.write(str);
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void returnFile(HttpServletResponse resp, InputStream is, String fileName){
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment;filename="+fileName);
        
        byte[] outputByte = new byte[2048];
        
        //write binary content to output stream
        int c = 0;
        try(OutputStream os = resp.getOutputStream()){
            while((c=is.read(outputByte, 0, 2048)) != -1){
                os.write(outputByte, 0, c);
            }
            os.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
