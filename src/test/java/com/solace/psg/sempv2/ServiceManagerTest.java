package com.solace.psg.sempv2;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.solace.psg.sempv2.admin.model.DataCenter;
import com.solace.psg.sempv2.admin.model.Service;
import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.User;
import com.solace.psg.sempv2.admin.model.UserRoles;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.ServiceManager;

public class ServiceManagerTest extends CommonTestBase
{
	private ServiceManager sm;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
		if (accessToken != null && accessToken.length() > 1)
		{
			sm = new ServiceManager(accessToken);
		}
		else if (user != null && pass != null)
		{
			sm = new ServiceManager(user, pass);
		}
		else
			fail("Credentials not provided. Set user and pass, or accessToken parameter.");
	}


	@Test
	public void testGetSetConnectTimeout()
	{
		int value = sm.getConnectTimeout();
		sm.setConnectTimeout(value+1);
		assertEquals(value+1, sm.getConnectTimeout());
	}

	@Test
	public void testGetSetReadTimeout()
	{
		int value = sm.getReadTimeout();
		sm.setReadTimeout(value+1);
		assertEquals(value+1, sm.getReadTimeout());
	}

	@Test
	public void testGetSetBaseServiceAdminUrl()
	{
		String value = sm.getBaseServiceAdminUrl();
		sm.setBaseServiceAdminUrl("Changed");
		assertEquals("Changed", sm.getBaseServiceAdminUrl());
		sm.setBaseServiceAdminUrl(value);
	}

	@Test @Ignore
	public void testGetSetTokenAdminUrl()
	{
		String value = sm.getTokenAdminUrl();
		sm.setTokenAdminUrl("Changed");
		assertEquals("Changed", sm.getTokenAdminUrl());
		sm.setTokenAdminUrl(value);	
	}

	@Test
	public void testGetSetUserAdminUrl()
	{
		String value = sm.getUserAdminUrl();
		sm.setUserAdminUrl("Changed");
		assertEquals("Changed", sm.getUserAdminUrl());
		sm.setUserAdminUrl(value);	
	}

	@Test
	public void testGetSetServiceAdminUrl()
	{
		String value = sm.getServiceAdminUrl();
		sm.setServiceAdminUrl("Changed");
		assertEquals("Changed", sm.getServiceAdminUrl());
		sm.setServiceAdminUrl(value);	
	}

	@Test @Ignore
	public void testGetAllServiceDetails()
	{
		try
		{
			List<ServiceDetails> sd = sm.getAllServiceDetails();
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
			List<Service> services = sm.getAllServices();
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
			ServiceDetails sd = sm.getServiceDetails(localServiceId);
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
			Service sd = sm.getServiceByName(testServiceName);
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
			ServiceDetails sd = sm.getServiceDetailsByName(testServiceName);
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
			boolean result = sm.addClientCertificateAuthority(localServiceId, certName, certContent);
			assertTrue(result);
			
			result = sm.deleteClientCertificateAuthority(localServiceId, certName);
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
			ServiceDetails sd = sm.createService(newServiceName, testServiceType, testServiceClass, testDatacenterId);
			assertNotNull(sd);
			boolean result = sm.deleteService(sd.getServiceId());
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
			boolean result = sm.updateServiceAuthentication(localServiceId, true, true, true);
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
			List<User> users = sm.getAllUsers();
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
			List<DataCenter> dcs = sm.getDataCenters();
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
			User user = sm.getUser(testUserId);
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
			User addedUser = sm.addUser("test2@example.com", roles);
			assertNotNull(addedUser);
			User user = sm.getUserByEmail("test2@example.com");
			assertEquals(user.getUserId(), addedUser.getUserId());
			assertNotNull(user);
			boolean result = sm.deleteUser(user.getUserId());
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
			String token = sm.getApiToken(user, pass);
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
			boolean result = sm.addClientProfile(localServiceId, "testName");
			assertTrue(result);
			result = sm.deleteClientProfile(localServiceId, "testName");
			assertTrue(result);
		}
		catch (ApiException | InterruptedException  e)
		{
			fail(e.getMessage());
		}
	}

}
