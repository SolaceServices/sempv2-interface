package com.solace.psg.sempv2.interfaces;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.solace.psg.sempv2.CommonTestBase;
import com.solace.psg.sempv2.admin.model.DataCenter;
import com.solace.psg.sempv2.admin.model.Service;
import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.User;
import com.solace.psg.sempv2.admin.model.UserRoles;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.interfaces.ServiceFacade;

public class ServiceFacadeTest extends CommonTestBase
{
	private ServiceFacade sf;
	
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
	}


	@Test
	public void testGetSetConnectTimeout()
	{
		int value = sf.getConnectTimeout();
		sf.setConnectTimeout(value+1);
		assertEquals(value+1, sf.getConnectTimeout());
	}

	@Test
	public void testGetSetReadTimeout()
	{
		int value = sf.getReadTimeout();
		sf.setReadTimeout(value+1);
		assertEquals(value+1, sf.getReadTimeout());
	}

	@Test
	public void testGetSetBaseServiceAdminUrl()
	{
		String value = sf.getBaseServiceAdminUrl();
		sf.setBaseServiceAdminUrl("Changed");
		assertEquals("Changed", sf.getBaseServiceAdminUrl());
		sf.setBaseServiceAdminUrl(value);
	}

	@Test @Ignore
	public void testGetSetTokenAdminUrl()
	{
		String value = sf.getTokenAdminUrl();
		sf.setTokenAdminUrl("Changed");
		assertEquals("Changed", sf.getTokenAdminUrl());
		sf.setTokenAdminUrl(value);	
	}

	@Test
	public void testGetSetUserAdminUrl()
	{
		String value = sf.getUserAdminUrl();
		sf.setUserAdminUrl("Changed");
		assertEquals("Changed", sf.getUserAdminUrl());
		sf.setUserAdminUrl(value);	
	}

	@Test
	public void testGetSetServiceAdminUrl()
	{
		String value = sf.getServiceAdminUrl();
		sf.setServiceAdminUrl("Changed");
		assertEquals("Changed", sf.getServiceAdminUrl());
		sf.setServiceAdminUrl(value);	
	}

	@Test @Ignore
	public void testGetAllServiceDetails()
	{
		try
		{
			List<ServiceDetails> sd = sf.getAllServiceDetails();
			assertNotNull(sd);
			assertTrue(sd.size() > 0);
			
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetAllServices()
	{
		try
		{
			List<Service> services = sf.getAllServices();
			assertNotNull(services);
			assertTrue(services.size() > 0);
			
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetServiceDetails()
	{
		try
		{
			ServiceDetails sd = sf.getServiceDetails(localServiceId);
			assertNotNull(sd);
			assertTrue(sd.getServiceId().equals(localServiceId));	
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetServiceByName()
	{
		try
		{
			Service sd = sf.getServiceByName(testServiceName);
			assertNotNull(sd);
			assertTrue(sd.getName().equals(testServiceName));	
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetServiceDetailsByName()
	{
		try
		{
			ServiceDetails sd = sf.getServiceDetailsByName(testServiceName);
			assertNotNull(sd);
			assertTrue(sd.getName().equals(testServiceName));	
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test //@Ignore
	public void testAddDeleteCertificateAuthority()
	{
		try
		{
			boolean result = sf.addClientCertificateAuthority(localServiceId, certName, certContent);
			assertTrue(result);
			
			result = sf.deleteClientCertificateAuthority(localServiceId, certName);
			assertTrue(result);
		}
		catch (ApiException | InterruptedException e)
		{
			fail(e.getMessage());
		}
		
	}

	@Test @Ignore
	public void testCreateDeleteService()
	{
		try
		{
			ServiceDetails sd = sf.createService(newServiceName, testServiceType, testServiceClass, testDatacenterId);
			assertNotNull(sd);
			boolean result = sf.deleteService(sd.getServiceId());
			assertTrue(result);
		}
		catch (ApiException | IOException | InterruptedException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testUpdateServiceAuthentication()
	{
		try
		{
			boolean result = sf.updateServiceAuthentication(localServiceId, true, true, true);
			assertTrue(result);
		}
		catch (ApiException | InterruptedException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetAllUsers()
	{
		try
		{
			List<User> users = sf.getAllUsers();
			assertNotNull(users);
		}
		catch (ApiException  e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetDataCenters()
	{
		try
		{
			List<DataCenter> dcs = sf.getDataCenters();
			assertNotNull(dcs);
			assertTrue(dcs.size() > 0);
			
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetUser()
	{
		try
		{
			User user = sf.getUser(testUserId);
			assertNotNull(user);
			assertEquals(testUserId, user.getUserId());
		}
		catch (ApiException  e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testAddDeleteUser()
	{
		try
		{
			List<String> roles = new ArrayList<String>();
			roles.add(UserRoles.MessagingServiceViewer);
			User addedUser = sf.addUser("test2@example.com", roles);
			assertNotNull(addedUser);
			User user = sf.getUserByEmail("test2@example.com");
			assertEquals(user.getUserId(), addedUser.getUserId());
			assertNotNull(user);
			boolean result = sf.deleteUser(user.getUserId());
			assertTrue(result);
		}
		catch (ApiException  e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testGetAllOrganizationRoles()
	{
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testAddRoleToUser()
	{
		fail("Not yet implemented");
	}

	@Test  @Ignore
	public void testGetApiToken()
	{
		try
		{
			String token = sf.getApiToken(user, pass);
			assertNotNull(token);
			assertTrue(token.length() > 1);
			
		}
		catch (ApiException | IOException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testAddDeleteClientProfile()
	{
		try
		{
			boolean result = sf.addClientProfile(localServiceId, "testName");
			assertTrue(result);
			result = sf.deleteClientProfile(localServiceId, "testName");
			assertTrue(result);
		}
		catch (ApiException | InterruptedException  e)
		{
			fail(e.getMessage());
		}
	}

}
