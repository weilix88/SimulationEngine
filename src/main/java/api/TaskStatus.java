package main.java.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import main.java.util.ServletUtil;
import redis.clients.jedis.Jedis;

@WebServlet("/TaskStatus")
public class TaskStatus extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public TaskStatus() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestId = request.getParameter("requestId");
        String type = request.getParameter("type");

        JsonObject jo = new JsonObject();
        String line = null;
        
        boolean isFinished = false;
        StringBuilder res = new StringBuilder();
        
        try(Jedis jedis = new Jedis("localhost")) {
            while((line = jedis.lpop("Task"+type+"#"+requestId)) != null){
                if(line.equals(type+"_FINISHED")){
                    isFinished = true;
                    //jedis.del("Task"+type+"#"+requestId);
                    break;
                }
                res.append(line);
            }
            
            if(isFinished){
                jedis.del("TaskRunning#"+requestId);
            }else if(res.length()==0){
                String running = jedis.get("TaskRunning#"+requestId);
                if(running==null){
                    isFinished = true;
                }
            }
        }
        
        jo.addProperty("status", "success");
        jo.addProperty("isFinished", isFinished);
        jo.addProperty("res", res.toString());
        
        ServletUtil.returnJsonResult(response, jo);

    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

}

