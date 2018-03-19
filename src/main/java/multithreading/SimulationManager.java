package main.java.multithreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SimulationManager {
    INSTANCE;

    private static ConcurrentHashMap<String, String> reqIdToPIDMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> existingPIDs = new ConcurrentHashMap<>();

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public static volatile AtomicInteger counter = new AtomicInteger(0);

    public int getRunningSimulation(){
        return counter.get();
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

        //synchronized (SimulationManager.class){
            String pid = null;

            try {
                Process pr = Runtime.getRuntime().exec(commandline, null, new File(path));
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));

                /*String s;
                boolean foundPID = false;
                reading: while ((s = stdInput.readLine()) != null) {
                    if (s.contains("EnergyPlus Starting")) {
                    	long tryPid = -1;   // pr's PID is not E+'s PID
                    	try {
                    		//for windows
                            if (pr.getClass().getName().equals("java.lang.Win32Process") ||
                                   pr.getClass().getName().equals("java.lang.ProcessImpl")) 
                            {
                                Field f = pr.getClass().getDeclaredField("handle");
                                f.setAccessible(true);              
                                long handl = f.getLong(pr);
                                Kernel32 kernel = Kernel32.INSTANCE;
                                W32API.HANDLE hand = new W32API.HANDLE();
                                hand.setPointer(Pointer.createConstant(handl));
                                tryPid = kernel.GetProcessId(hand);
                                f.setAccessible(false);
                            }
                            //for unix based operating systems
                            else if (pr.getClass().getName().equals("java.lang.UNIXProcess")) 
                            {
                                Field f = pr.getClass().getDeclaredField("pid");
                                f.setAccessible(true);
                                tryPid = f.getLong(pr);
                                f.setAccessible(false);
                            }
    					} catch (Exception e) {
    						LOG.error(e.getMessage(), e);
    						e.printStackTrace();
    					}
                    	
                        List<String> pids = ProcessUtil.getPIDs();
                        if (pids.size() > 0) {
                            for(String p : pids){
                                if(!existingPIDs.contains(p)){
                                    existingPIDs.put(p, "");
                                    reqIdToPIDMap.put(reqId, p);

                                    pid = p;
                                    LOG.info("PID found "+pid+" vs handle "+tryPid);
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
                }*/
                
                wrapper.process = pr;
                wrapper.pid = pid;
                wrapper.stdInput = stdInput;
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        //}

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
            }else {
                LOG.info("Finish simulation NOT found PID");
            }
            LOG.info("Simulation counter decremented: "+counter.decrementAndGet());
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
