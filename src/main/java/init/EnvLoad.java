package main.java.init;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.EngineConfig;

public class EnvLoad extends HttpServlet{
private static final long serialVersionUID = 8760402126588557090L;
	
	private final Logger LOG = LoggerFactory.getLogger(EnvLoad.class);

	@Override
	public void init() throws ServletException {
		super.init();
		
		// Load configuration file
		String configFilePath = this.getServletContext().getRealPath("/WEB-INF/engine.config");		
		EngineConfig.setConfigPath(configFilePath);
		
		LOG.info("Simulation Engine Config file path: "+configFilePath);
	}
}
