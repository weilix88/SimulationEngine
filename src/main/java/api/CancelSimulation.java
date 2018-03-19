package main.java.api;

import com.google.gson.JsonObject;

import main.java.cloud.RedisAccess;
import main.java.cloud.RedisAccessFactory;
import main.java.multithreading.SimulationManager;
import main.java.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "CancelSimulation", urlPatterns = "/CancelSimulation")
public class CancelSimulation extends HttpServlet {
	private static final long serialVersionUID = 4676653221110488912L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String reqId = request.getParameter("req_id");

        JsonObject jo = new JsonObject();


        if(reqId==null || reqId.isEmpty()){
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "No request id provided");
        }else {
            RedisAccess access = RedisAccessFactory.getAccess();
            access.set("TaskCancelled#"+reqId, "true");
            access.close();

            boolean foundProcess = SimulationManager.INSTANCE.cancelSimulation(reqId);

            jo.addProperty("status", "success");
            jo.addProperty("found_process", foundProcess);
        }

        ServletUtil.returnJsonResult(response, jo);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }
}
