package main.java.daemon;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSets;
import com.microsoft.rest.LogLevel;

import main.java.cloud.InstanceInfo;
import main.java.config.EngineConfig;
import main.java.httpClientConnect.StatusReporter;
import main.java.multithreading.SimEngine;
import main.java.multithreading.SimulationManager;

public class Monitor implements Runnable {
    private static final long THRESHOLD = 20 * 1000;  // 20 minutes
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private long idleStart = 0;

    private boolean isOnCloud = false;
    private String platform = "";

    public boolean running = false;
    
    final File azureCredFile = new File("C:\\auth_file");

    public Monitor() {
    	this.platform = EngineConfig.readProperty("platform").toLowerCase();
        this.isOnCloud = platform.equals("aws") || platform.equals("azure");
        running = true;
    }

    @Override
    public void run() {
    	try {
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
							if (platform.equals("aws")) {
								AWSCredentialsProvider provider = DefaultAWSCredentialsProviderChain.getInstance();

								AmazonAutoScalingClientBuilder autoScalingClientBuilder = AmazonAutoScalingClientBuilder.standard();
								AmazonAutoScaling autoScalingClient = autoScalingClientBuilder.withCredentials(provider).build();

								DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
								request.setAutoScalingGroupNames(Arrays.asList(EngineConfig.readProperty("AutoscalingGroupName")));
								DescribeAutoScalingGroupsResult res = autoScalingClient.describeAutoScalingGroups(request);

								AutoScalingGroup asg = res.getAutoScalingGroups().get(0);
								if (asg.getMinSize() < asg.getInstances().size()) {
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
							} else if (platform.equals("azure")) {
								Azure azure;
								try {
									azure = Azure
											.configure()
											.withLogLevel(LogLevel.NONE)
											.authenticate(azureCredFile)
											.withDefaultSubscription();

									//check current running number in LB

									int running = 0;
									String autoScalingGroupName = EngineConfig.readProperty("AutoscalingGroupName");
									VirtualMachineScaleSets vmsses = azure.virtualMachineScaleSets();
									List<VirtualMachineScaleSet> list = vmsses.list();
									for (VirtualMachineScaleSet vmss : list) {
										if (vmss.name().equalsIgnoreCase(autoScalingGroupName)) {
											running = vmss.capacity();
											break;
										}
									}

									LOG.info("Running instance in VMSS: " + running);
									StatusReporter.sendDaemonStatus("Running instance in VMSS: " + running, "log");

									int minNum = Integer.parseInt(EngineConfig.readProperty("AusoscalingMinInstance"));
									if (running > minNum) {
										// shutdown itself

										String vmName = InstanceInfo.getVMName();
										List<VirtualMachine> vms = azure.virtualMachines().list();
										for (VirtualMachine vm : vms) {
											if (vm.name().equalsIgnoreCase(vmName)) {
												LOG.info("Shutting down itself");
												StatusReporter.sendDaemonStatus("Shutting down itself", "log");

												vm.deallocate();

												StatusReporter.sendDaemonStatus("Shuted down itself", "log");
												break;
											}
										}
									}
								} catch (Throwable e) {
									LOG.error(e.getMessage(), e);
									StatusReporter.sendDaemonStatus(e.getMessage(), "error");
								}
							}
						}
					}
				}

				try {
					Thread.sleep(30 * 60 * 1000);
				} catch (InterruptedException e) {
				}
			}
		}catch (Throwable e){
            StatusReporter.sendEngineLog("Monitor encounters error and break while loop: "+e.getMessage(), "error");
            running = false;
        }
    }
}
