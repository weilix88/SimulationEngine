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

@WebServlet(urlPatterns="/TaskOutput")
public class TaskOutput extends HttpServlet{
    private static final long serialVersionUID = 7393081520272311289L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestId = req.getParameter("requestId");
        String type = req.getParameter("type");

        JsonObject jo = new JsonObject();
        String content = null;
        try(Jedis jedis = new Jedis("localhost")) {
            content = jedis.get("Task"+type+"#"+requestId);
            if(content==null){
                content = "";
            }else {
                jedis.del("Task"+type+"#"+requestId);
                jo.addProperty("isFinished", true);
            }
            jo.addProperty("res", content);
        }
        
        jo.addProperty("status", "success");
        
        ServletUtil.returnJsonResult(resp, jo);
    }

}
