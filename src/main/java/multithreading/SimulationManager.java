package main.java.multithreading;

import main.java.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public enum SimulationManager {
    INSTANCE;

    private static ConcurrentHashMap<String, String> reqIdToPIDMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> existingPIDs = new ConcurrentHashMap<>();

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static volatile AtomicInteger counter = new AtomicInteger(0);

    public int getRunningSimulation(){
        return ProcessUtil.getPIDs().size();
    }

    public Map<String, String> getReqIdToPIDMap(){
        Map<String, String> res = new HashMap<>(reqIdToPIDMap);
        return res;
    }

    /**
     * Share the Class lock with startSimulation to prevent race condition.
     * @param reqId
     * @param commandline
     * @param path
     * @return
     */
    public StartSimulationWrapper startSimulation(String reqId, String[] commandline, String path){
        StartSimulationWrapper wrapper = new StartSimulationWrapper();

        synchronized (SimulationManager.class){
            String pid = null;

            try {
                Process pr = Runtime.getRuntime().exec(commandline, null, new File(path));
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));

                String s;
                boolean foundPID = false;
                reading: while ((s = stdInput.readLine()) != null) {
                    if (s.contains("EnergyPlus Starting")) {
                        List<String> pids = ProcessUtil.getPIDs();
                        if (pids.size() > 0) {
                            for(String p : pids){
                                if(!existingPIDs.contains(p)){
                                    existingPIDs.put(p, "");
                                    reqIdToPIDMap.put(reqId, p);

                                    pid = p;
                                    LOG.info("PID found "+pid);
                                    LOG.info("Simulation counter incremented: "+counter.incrementAndGet());

                                    foundPID = true;
                                    break reading;
                                }
                            }
                        }
                        LOG.warn("PID not found for "+reqId);
                    }
                }

                if(!foundPID){
                    LOG.info("PID NOT found");
                    LOG.info("Simulation counter stays same: "+counter.get());
                }

                wrapper.process = pr;
                wrapper.pid = pid;
                wrapper.stdInput = stdInput;
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        return wrapper;
    }

    /**
     * Share the Class lock with startSimulation to prevent race condition.
     * E.g., startSimulation get list of PIDs first, during iteration, one of the
     * simulation finished, the finished simulation's PID could be recognized as
     * new simulation's PID
     * @param reqId
     */
    public void finishSimulation(String reqId){
        synchronized (SimulationManager.class){
            String pid = reqIdToPIDMap.remove(reqId);
            if(pid!=null){
                existingPIDs.remove(pid);
                LOG.info("Finish simulation found PID");
                LOG.info("Simulation counter decremented: "+counter.decrementAndGet());
            }else {
                LOG.info("Finish simulation NOT found PID");
                LOG.info("Simulation counter stays same: "+counter.get());
            }
        }
    }

    public int getSimulationCounter(){
        return counter.get();
    }

    public boolean cancelSimulation(String reqId){
        synchronized (SimulationManager.class){
            String pid = reqIdToPIDMap.remove(reqId);
            if(pid!=null){
                try {
                    Runtime.getRuntime().exec("taskkill /PID "+pid+" /F");
                    LOG.info("Cancel simulation stops process");
                    LOG.info("Simulation counter decremented: "+counter.decrementAndGet());
                } catch (IOException e) {
                    LOG.info("Cancel simulation stops process failed, "+e.getMessage());
                    LOG.info("Simulation counter stays same: "+counter.get());
                }

                existingPIDs.remove(pid);
                return true;
            }else {
                LOG.info("Cancel simulation process id NOT found");
                LOG.info("Simulation counter stays same: "+counter.get());
            }
            return false;
        }
    }
}
