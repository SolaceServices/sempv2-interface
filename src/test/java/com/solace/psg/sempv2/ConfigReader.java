/**
 * 
 */
package com.solace.psg.sempv2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ConfigReader
{
	private static final Logger logger = LogManager.getLogger(ConfigReader.class);

	private String configFile;
	private Properties properties;

	/**
	 * Initialise a new instance of the class.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public ConfigReader(String configFile) throws FileNotFoundException, IOException
	{
		this.configFile = configFile;
		
		parseConfig();
	}

	private void parseConfig() throws FileNotFoundException, IOException 
	{
    	logger.info( "Parsing config ..." );    
    	
    	properties = new Properties();
    	properties.load(new FileInputStream( new File(configFile)));
	}
	
	/**
	 * Gets properties.
	 * @return All props
	 */
	public Properties getProperties()
	{
		return properties;
	}
	
	/**
	 * Gets property.
	 * @return Value
	 */
	public String getProperty(String key)
	{
		return properties.getProperty(key);
	}
}
