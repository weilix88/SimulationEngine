package main.java.api;

import com.google.gson.JsonObject;
import main.java.multithreading.SimulationManager;
import main.java.util.ProcessUtil;
import main.java.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "SimulationNumInfo")
public class SimulationNumInfo extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int counter = SimulationManager.INSTANCE.getRunningSimulation();
        int eplus = ProcessUtil.getPIDs().size();

        JsonObject jo = new JsonObject();
        jo.addProperty("counter", counter);
        jo.addProperty("eplus", eplus);
        ServletUtil.returnJsonResult(response, jo);
    }
}
