package main.java.util;

import java.io.*;
import java.util.Base64;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.EngineConfig;

public class FileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    public static File createTempFile(String fileName){
        File res;
        
        String tmpfolder = EngineConfig.readProperty("tmpfolder");
        String randomFolderPath = tmpfolder+"ISToFile_"+RandomUtil.genRandomStr()+"\\";
        
        File randomFolder = new File(randomFolderPath);
        randomFolder.mkdir();
        
        res = new SelfDestryoFile(randomFolderPath+fileName, true);
        res.deleteOnExit();
        
        return res;
    }

    public static String readBase64CompressedString(String path){
        try {
            String content = readTextFile(path);
            if(content.isEmpty()){
                return "";
            }
            byte[] compressed = compressString(content);
            return Base64.getEncoder().encodeToString(compressed);
        } catch (Throwable throwable) {
            LOG.error("convert file content to base64 compressed string failed: " + throwable.getMessage());
            return null;
        }
    }

    public static String readTextFile(String path){
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

    public static File compressFile(String zipFilePath, String filePath){
        File zipFile = new File(zipFilePath);

        byte[] b = new byte[1024];
        int count;
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream ous = new ZipOutputStream(fos)) {
            File file = new File(filePath);
            ous.putNextEntry(new ZipEntry(file.getName()));

            try (FileInputStream fis = new FileInputStream(file)) {
                while ((count = fis.read(b)) > 0) {
                    ous.write(b, 0, count);
                }
                ous.flush();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            zipFile = null;
        }

        return zipFile;
    }

    public static byte[] compressString(String raw){
        if(raw==null){
            return null;
        }

        byte[] compressed = null;
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream(raw.length())){
            try(GZIPOutputStream gzippedOut = new GZIPOutputStream(bos)){
                gzippedOut.write(raw.getBytes("utf-8"));
                gzippedOut.flush();
            }

            compressed = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return compressed;
    }

    public final static String TAG = "tableid";
    public final static String TYPE = "reportType";
    public final static String NAME = "tableName";
    public final static String REPORT = "reportName";
    public final static String CAT = "reportCat";
    public static void processHTML(Document rawHTMLDoc){
        String reportName = "";
        String reportFor = "";
        String tableName = "";

        Element body = rawHTMLDoc.body();
        Elements all = body.children();
        Iterator<Element> iter = all.iterator();
        while(iter.hasNext()){
            Element ele = iter.next();
            String nodeName = ele.nodeName();

            switch(nodeName){
                case "p":
                    String ownText = ele.ownText().trim();

                    if(ownText.equals("Report:")){
                        reportName = ele.select("b").text().trim().replaceAll("\\W", "");
                    }else if(ownText.equals("For:")){
                        reportFor = ele.select("b").text().trim().replaceAll("\\W", "");
                    }
                    break;
                case "b":
                    tableName = ele.text().trim().replaceAll("\\W", "");
                    break;
                case "table":
                    String tableId = reportName+":"+reportFor+":"+tableName;
                    ele.attr(TAG, tableId);
                    ele.attr(TYPE, reportName+":" +reportFor);
                    ele.attr(NAME, tableName);
                    ele.attr(REPORT, reportName);
                    ele.attr(CAT, reportFor);

                    //we assume one table display at a time - utilities will give table a nice export feature
                    ele.attr("class", "table table-striped table-bordered table-hover dataTables-utilities");
                    break;
            }
        }
    }

    public static boolean appendToFile(File file, String content){
        /*try {
            Files.write(file.toPath(), (content+System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            return false;
        }*/

        try(FileWriter fw = new FileWriter(file, true);
                BufferedWriter br = new BufferedWriter(fw)){
            br.write(content+System.lineSeparator());
        }catch (IOException e){
            return false;
        }

        return true;
    }

    public static String readStringFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, "utf-8");
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while((line=br.readLine())!=null){
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }

    public static String getEnergyPlusPath(String version) {
        return EngineConfig.readProperty("EnergyPlusBasePath") + "EnergyPlusV" + version.replaceAll("\\.", "-") + "-0\\";
    }
}
