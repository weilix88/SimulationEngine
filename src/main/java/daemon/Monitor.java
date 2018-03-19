package main.java.daemon;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import main.java.cloud.InstanceInfo;
import main.java.config.EngineConfig;
import main.java.multithreading.SimEngine;
import main.java.multithreading.SimulationManager;

public class Monitor implements Runnable {
    private static final long THRESHOLD = 20 * 1000;  // 20 minutes
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private long idleStart = 0;

    private boolean isOnCloud = false;
    private String platform = "";

    public Monitor() {
    	this.platform = EngineConfig.readProperty("platform").toLowerCase();
        this.isOnCloud = platform.equals("aws") || platform.equals("azure");
    }

    @Override
    public void run() {
        while (isOnCloud) {
            int simulationCounter = SimulationManager.INSTANCE.getSimulationCounter();
            int runingSimulationNum = SimulationManager.INSTANCE.getRunningSimulation();

            if ((simulationCounter == 0 && runingSimulationNum != 0)
                    || (simulationCounter != 0 && runingSimulationNum == 0)) {
                LOG.warn("Simulation counter isn't consistant with running simulation num: " + simulationCounter + " vs " + runingSimulationNum);
            }

            if (simulationCounter > 0) {
                idleStart = 0;
            }

            if (simulationCounter == 0 && idleStart > 0) {
                if (runingSimulationNum == 0) {
                    if (System.currentTimeMillis() - idleStart > THRESHOLD) {
                    	if(platform.equals("aws")) {
	                        AWSCredentialsProvider provider = DefaultAWSCredentialsProviderChain.getInstance();
	
	                        AmazonAutoScalingClientBuilder autoScalingClientBuilder = AmazonAutoScalingClientBuilder.standard();
	                        AmazonAutoScaling autoScalingClient = autoScalingClientBuilder.withCredentials(provider).build();
	
	                        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
	                        request.setAutoScalingGroupNames(Arrays.asList(EngineConfig.readProperty("AutoscalingGroupName")));
	                        DescribeAutoScalingGroupsResult res = autoScalingClient.describeAutoScalingGroups(request);
	
	                        AutoScalingGroup asg = res.getAutoScalingGroups().get(0);
	                        if(asg.getMinSize()<asg.getInstances().size()){
	                            SimEngine.shutdown();
	
	                            String instanceId = InstanceInfo.getInstanceID();
	                            AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard();
	                            String region = EngineConfig.readProperty("DefaultAWSRegion");
	                            builder.setRegion(region);
	
	                            AmazonEC2 client = builder.withCredentials(provider).build();
	                            TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();
	                            terminateRequest.setInstanceIds(Arrays.asList(instanceId));
	                            client.terminateInstances(terminateRequest);
	                        }
                    	}else if(platform.equals("azure")) {
                    		/*final File credFile = new File("C:\\auth_file");

                        	Azure azure;
							try {
								azure = Azure
								        .configure()
								        .withLogLevel(LogLevel.NONE)
								        .authenticate(credFile)
								        .withDefaultSubscription();
								//TODO check current running number in LB
								
								
								PagedList<VirtualMachine> list = azure.virtualMachines().list(); // not working
	                        	
	                        	String vmId = InstanceInfo.getInstanceID();
	                        	for(VirtualMachine vm : list) {
	                        		if(vm.vmId().equals(vmId)) {
	                        			vm.deallocate();
	                        		}
	                        	}
	                        	
							} catch (CloudException | IOException e) {
								LOG.error(e.getMessage(), e);
							}*/
                    	}
                    }
                }
            }

            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
            }
        }
    }
}
