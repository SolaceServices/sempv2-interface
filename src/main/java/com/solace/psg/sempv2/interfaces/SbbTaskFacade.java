package com.solace.psg.sempv2.interfaces;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solace.psg.sempv2.apiclient.ApiException;


/**
 * Class to represent the whole SbbTaskFacade with all the operations available
 * .
 * @author VictorTsonkov
 *
 */
public class SbbTaskFacade
{
	private static final Logger logger = LogManager.getLogger(SbbTaskFacade.class);
	
	private ClusterFacade cf;
	private ServiceFacade sf;
	private DmrFacade df;
	private VpnFacade vf;
	
	/**
	 * Initialises a new instance of the class.
	 * @param accessToken
	 * @throws ApiException
	 */
	public SbbTaskFacade(String accessToken) throws ApiException
	{		
		sf = new ServiceFacade(accessToken);
		cf = new ClusterFacade(accessToken);
		//df = new DmrFacade(localService, remoteService);
		//vf = new VpnFacade(service);
	}

	/**
	 * Initialises a new instance of the class.
	 * @param accountUsername
	 * @param accountPassword
	 * @throws ApiException
	 * @throws IOException
	 */
	public SbbTaskFacade(String accountUsername, String accountPassword) throws ApiException, IOException
	{		
		sf = new ServiceFacade(accountUsername, accountPassword);
	}

	public static void main(String[] args)
	{
		String user = System.getProperty("user");
		String pass = System.getProperty("pass");
		SbbTaskFacade facade = null;
		
		try
		{
			if (args.length < 1 || args.length > 2 )
			{		
				logger.error("Arguments must contain accessToken or username and password parameters passed: 'app.jar <accessToken>' or 'app.jar <username> <password>'");				
				
				System.exit(-1);
			}
			else if (args.length == 1)
			{		
				facade = new SbbTaskFacade(args[0]);
			}
			else if (args.length == 2)
			{
				facade = new SbbTaskFacade(args[0], args[1]);
			}
			
			// TODO: Add relevant application logic in case of more complex construct of cluster is needed.
			
		}
		catch (Exception e)
		{
			logger.info("Error occured" + e.getMessage());
		}
	}
	
}
