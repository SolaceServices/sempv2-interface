package com.solace.psg.sempv2.interfaces;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


import com.solace.psg.sempv2.FacadeTestBase;
import com.solace.psg.sempv2.admin.model.ServiceDetails;

import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.interfaces.ClusterFacade;
import com.solace.psg.sempv2.interfaces.ServiceFacade;

public class ClusterFacadeTest extends FacadeTestBase
{
	private ServiceFacade sf;
	
	private ClusterFacade cf;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
		if (accessToken != null && accessToken.length() > 1)
		{
			sf = new ServiceFacade(accessToken);
		}
		else if (user != null && pass != null)
		{
			sf = new ServiceFacade(user, pass);
		}
		else
			fail("Credentials not provided. Set user and pass, or accessToken parameter.");
		
		
		ArrayList<ServiceDetails> clusterServices = getTestClusterServices();
		cf = new ClusterFacade(sf, clusterServices);		
	}

	@Test
	public void testGetAllClusterServiceDetails()
	{
		try
		{
			List<ServiceDetails> services = cf.getAllClusterServiceDetails();
			assertNotNull(services);
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testCreateDeleteClusterService()
	{
		try
		{
			int clusterCount = cf.getClusterServiceList().size();
			ServiceDetails service = cf.createClusterService(newServiceName, testServiceType, testServiceClass, testDatacenterId, null);
			assertNotNull(service);
			assertEquals(service.getName(), newServiceName);
			int newClusterCount = cf.getClusterServiceList().size();
			assertEquals(clusterCount + 1, newClusterCount);
			boolean result = cf.deleteClusterService(newServiceName);
			assertTrue(result);
		}
		catch (ApiException | IOException | InterruptedException e)
		{
			fail(e.getMessage());
		}
	}
	
	/**
	 * Limits the list of services to the list in config.
	 * @throws ApiException 
	 */
	private ArrayList<ServiceDetails> getTestClusterServices() throws ApiException
	{
		ArrayList<ServiceDetails> clusterServices = new ArrayList<ServiceDetails>();
		if (testClusterServiceIds != null)
		{
			String[] serviceIds = testClusterServiceIds.split(",");
			
			for (String s : serviceIds)
			{
				ServiceDetails sd = sf.getServiceDetails(s);
				clusterServices.add(sd);
			}
		}
		
		return clusterServices;
	}
}
