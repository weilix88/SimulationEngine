package main.java.api;

import com.google.gson.JsonObject;
import main.java.cloud.InstanceInfo;
import main.java.multithreading.SimEngine;
import main.java.util.ServletUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ServerIdentity")
public class ServerIdentity extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        this.doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String id = SimEngine.EngineID;
        String publicIP = InstanceInfo.getPublicIP();

        JsonObject res = new JsonObject();
        res.addProperty("engine_id", id);
        res.addProperty("public_ip", publicIP);
        ServletUtil.returnJsonResult(response, res);
    }
}
