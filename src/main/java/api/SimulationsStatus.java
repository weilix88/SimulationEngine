package main.java.api;

import com.google.gson.JsonArray;
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
import java.util.List;
import java.util.Map;

@WebServlet(name = "SimulationsStatus", urlPatterns = "/SimulationsStatus")
public class SimulationsStatus extends HttpServlet {
	private static final long serialVersionUID = 3745652788704405881L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //List<String> pids = ProcessUtil.getPIDs();

        //Map<String, String> reqIdToPID = SimulationManager.INSTANCE.getReqIdToPIDMap();

        JsonArray ja = new JsonArray();
        /*for(String reqId : reqIdToPID.keySet()){
            JsonObject jo = new JsonObject();
            jo.addProperty("req_id", reqId);

            String pid = reqIdToPID.get(reqId);
            if(pids.contains(pid)) {
                jo.addProperty("pid", pid);
                pids.remove(pid);
            }else {
                jo.addProperty("pid", "No running process found");
            }

            ja.add(jo);
        }

        for(String pid : pids){
            JsonObject jo = new JsonObject();
            jo.addProperty("req_id", "No request id record found");
            jo.addProperty("pid", pid);

            ja.add(jo);
        }*/

        ServletUtil.returnJsonResult(response, ja);
    }
}
