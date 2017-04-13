package notUsed;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.core.Task;
import main.java.multithreading.TaskRunner;

public class SimCommander {
    private final Logger LOG = LoggerFactory.getLogger(SimCommander.class);
    
    /*private String database;
    private String dbuser;
    private String dbpassword;*/
    public Task[] commands;
    public TaskRunner[] commandRunners;
    public String output;

    public SimCommander() {
        this.commandRunners = new TaskRunner[20];
    }

    public String getOutput() {
        return this.output;
    }

    /*protected void loadConfig(String filePath) {
        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(filePath);

            prop.load(input);

            this.database = prop.getProperty("database");
            this.dbuser = prop.getProperty("dbuser");
            this.dbpassword = prop.getProperty("dbpassword");

            // for testing
            System.out.println("config: ");
            System.out.println("database: " + this.database);
            System.out.println("dbuser: " + this.dbuser);
            System.out.println("dbpassword: " + this.dbpassword);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    public void simulate() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i = 0; i < this.commands.length; i++) {
            if (i > 19) {
                LOG.warn("No more than 20 tasks!");
                break;
            }
            this.commandRunners[i] = new TaskRunner(this.commands[i]);
            LOG.info("A new task has been added : Task request id: " + this.commands[i].getRequestId() );
            executor.execute(this.commandRunners[i]);
        }
        executor.shutdown();
    }

    public void simulate(Task[] commands) {
        this.commands = commands;
        simulate();
    }
}

