package notUsed;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import main.java.core.Task;

//@WebServlet("/ModelFile")
public class ModelFile/* extends HttpServlet*/ {

    //private static final long serialVersionUID = 1L;

    public ModelFile() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String name = request.getParameter("name");
        //String modelText = request.getParameter("modelText");

        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        response.setHeader("Cache-control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Max-Age", "86400");

        Gson gson = new Gson(); 
        JsonObject myObj = new JsonObject();

        Task task = new Task();
        task.setStatus("Received");
        task.setRequestId(UUID.randomUUID().toString());
        task.setName(name);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        task.setReceiveTime(df.format(new Date()));
        //task.setModelText(modelText);
        
        task.enqueue();
        task.update();
        
        JsonElement taskObj = gson.toJsonTree(task);
        myObj.add("task", taskObj);
        out.println(myObj.toString());

        out.close();

    }
}

