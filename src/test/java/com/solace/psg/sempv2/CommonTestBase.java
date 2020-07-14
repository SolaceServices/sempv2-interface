/**
 * 
 */
package com.solace.psg.sempv2;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;

import com.solace.psg.sempv2.admin.model.ServiceClass;
import com.solace.psg.sempv2.admin.model.ServiceType;

/**
 * Class to hold common test data.
 * @author VictorTsonkov
 *
 */
public abstract class CommonTestBase
{

	protected String user;
	protected String pass;
	protected String accessToken;
	protected String testServiceName;
	protected String localServiceId;
	protected String remoteServiceId;
	protected String testClusterServiceIds;
	protected String testDatacenterId;
	protected String testServiceClass;
	protected String testServiceType;
	protected String newServiceName;
	protected String certContent;	
	protected String certName;	
	protected String testUserId;
	
	/**
	 * @throws IOException 
	 * 
	 */
	public CommonTestBase() 
	{
		user = System.getProperty("user");
		pass = System.getProperty("pass");
		accessToken = System.getProperty("accessToken");

		ConfigReader reader = null;
		try
		{
			reader = new ConfigReader("config.properties");
		}
		catch (FileNotFoundException e)
		{
			fail(e.getMessage());
		}
		catch (IOException e)
		{
			fail(e.getMessage());
		}
		
		if (accessToken == null)
			accessToken = reader.getProperty("accessToken");
	
		testServiceName = reader.getProperty("testServiceName");
		localServiceId = reader.getProperty("localServiceId");
		remoteServiceId = reader.getProperty("remoteServiceId");
		newServiceName = reader.getProperty("newServiceName");
		
		testClusterServiceIds =  reader.getProperty("testClusterServiceIds");
		testDatacenterId = reader.getProperty("testDatacenterId");
		testServiceClass = ServiceClass.fromValue(reader.getProperty("testServiceClass"));
		testServiceType = ServiceType.fromValue(reader.getProperty("testServiceType"));
		certName = reader.getProperty("certName");
		testUserId = reader.getProperty("testUserId");
		String username = reader.getProperty("username");
		if (username != null)
			user = username;
		String password = reader.getProperty("password");
		if (password != null)
			pass = password;
		
		String certContentFile = reader.getProperty("certContentFile");
		try
		{
			certContent = readFileContent(certContentFile);
		}
		catch (IOException e)
		{
			fail(e.getMessage());
		}
		
	}

	@Before
	public void setUp() throws Exception
	{
	}
	
	private String readFileContent(String fileName) throws IOException
	{
		File file = new File(fileName);
		FileInputStream stream = new FileInputStream( file);
		int length = stream.available();
		byte[] bytes = new byte[length]; 
		stream.read(bytes);	
		return new String(bytes);
	}

}
