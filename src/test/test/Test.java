package test.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSets;
import com.microsoft.rest.LogLevel;

import main.java.cloud.InstanceInfo;
import main.java.config.EngineConfig;

public class Test {
	public static void main(String[] args){
		Logger LOG = LoggerFactory.getLogger(Test.class);
		final File credFile = new File("C:\\auth_file");
		EngineConfig.setConfigPath("C:\\workspace\\SimulationEngine\\WebContent\\WEB-INF\\engine.config");
		
		Azure azure;
		try {
			azure = Azure
			        .configure()
			        .withLogLevel(LogLevel.NONE)
			        .authenticate(credFile)
			        .withDefaultSubscription();
			//TODO check current running number in LB
			
			int running = 0;
			String autoScalingGroupName = EngineConfig.readProperty("AutoscalingGroupName");
			VirtualMachineScaleSets vmsses = azure.virtualMachineScaleSets();
			List<VirtualMachineScaleSet> list = vmsses.list();
			for(VirtualMachineScaleSet vmss : list){
				if(vmss.name().equalsIgnoreCase(autoScalingGroupName)){
					running = vmss.capacity();
					break;
				}
			}
			
			LOG.info("Running instance in VMSS: "+running);
			int minNum = Integer.parseInt(EngineConfig.readProperty("AusoscalingMinInstance"));
			if(running>minNum){
				// shutdown itself
				
				String vmName = InstanceInfo.getVMName();
				List<VirtualMachine> vms = azure.virtualMachines().list();
				for(VirtualMachine vm : vms){
					if(vm.name().equalsIgnoreCase(vmName)){
						LOG.info("Shutting down itself");
						
						vm.deallocate();
						break;
					}
				}
			}			
		} catch (CloudException | IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
