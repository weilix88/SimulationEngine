package main.java.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.EngineConfig;

public class FileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);
    
    public static File createTempFile(String fileName){
        File res = null;
        
        String tmpfolder = EngineConfig.readProperty("tmpfolder");
        String randomFolderPath = tmpfolder+"ISToFile_"+RandomUtil.genRandomStr()+"\\";
        
        File randomFolder = new File(randomFolderPath);
        randomFolder.mkdir();
        
        res = new File(randomFolderPath+fileName);
        res.deleteOnExit();
        
        return res;
    }
    
    public static File convertInputStreamToFile(InputStream is){
        File res = createTempFile("ISToFile.tmp");

        try(FileOutputStream out = new FileOutputStream(res)){
            IOUtils.copy(is, out);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return res;
    }
    
    public static File convertStringToFile(String str){
        File res = createTempFile("StrToFile.tmp");
        try(FileWriter fw = new FileWriter(res);
                BufferedWriter bw = new BufferedWriter(fw)){
            bw.write(str);
            bw.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return res;
    }
    
    public static void writeStringToFile(String str, String path){
        try(FileWriter fw = new FileWriter(path);
                BufferedWriter bw = new BufferedWriter(fw)){
            bw.write(str);
            bw.flush();
        }catch (IOException e){
            LOG.error(e.getMessage(), e);
        }
    }
    
    public static boolean saveFileToPath(File file, String path){
        try(                
            RandomAccessFile raf = new RandomAccessFile(new File(path), "rw");
            FileChannel fc = raf.getChannel();
            
            FileInputStream fis = new FileInputStream(file);
            ReadableByteChannel rbc = Channels.newChannel(fis);
        ){            
            fc.transferFrom(rbc, 0, file.length());
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * suffix with heading dot
     * @param s
     * @param suffix
     * @return
     */
    public static String makeFileName(String s, String suffix){
        String fileName =  s.replaceAll("[:\\\\/*\"?|<>']", " ");
        int suffixLen = suffix.length();
        if(fileName.length()>255-suffixLen){
            fileName = fileName.substring(0, 255-suffixLen);
        }
        return fileName+suffix;
    }
    
    public static String getSuffix(String fileName){
        if(fileName==null){
            return null;
        }
        
        fileName = fileName.toLowerCase();
        int lastDot = fileName.lastIndexOf(".");
        if(lastDot==-1){
            return "NO_TYPE";
        }
        
        return fileName.substring(lastDot+1, fileName.length());
    }
}
