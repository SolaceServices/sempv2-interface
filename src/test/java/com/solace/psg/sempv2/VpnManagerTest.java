package com.solace.psg.sempv2;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.ServiceManagementContext;
import com.solace.psg.sempv2.admin.model.SmfSubscription;
import com.solace.psg.sempv2.admin.model.Subscription;
import com.solace.psg.sempv2.admin.model.SubscriptionDirection;
import com.solace.psg.sempv2.admin.model.SubscriptionType;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile;
import com.solace.psg.sempv2.config.model.MsgVpnBridge;
import com.solace.psg.sempv2.config.model.MsgVpnQueue;
import com.solace.psg.sempv2.ServiceManager;
import com.solace.psg.sempv2.VpnManager;

public class VpnManagerTest extends CommonTestBase
{

	private ServiceManager sm;
	private VpnManager vm;
	
	private ServiceDetails localService;
	private ServiceDetails remoteService;

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
		
		localService = sm.getServiceDetails(localServiceId);
		remoteService = sm.getServiceDetailsByName(testServiceName);
		vm = new VpnManager(localService);
	}

	@Test @Ignore
	public void testVpnContext()
	{
		ServiceDetails sd;
		try
		{
			ServiceManagementContext oldContext = vm.getDefaultVpnContext();
			assertNotNull(oldContext);
	
			ServiceManagementContext newContext = new ServiceManagementContext(remoteService);			
			vm.setVpnContext(newContext);
			ServiceManagementContext newContext2 = vm.getDefaultVpnContext();
			assertNotNull(newContext2);
			assertEquals(oldContext, newContext2);

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testAclProfileOperations()
	{
		try
		{
			List<MsgVpnAclProfile> profiles = vm.getAclProfiles();
			assertNotNull(profiles);
			int size = profiles.size();

			MsgVpnAclProfile response = vm.addAclProfile("testProfileName2", "allow", "allow", "disallow");
			assertNotNull(response);
			
			profiles = vm.getAclProfiles();
			int newSize = profiles.size();
			assertEquals(size+1, newSize);
			
			assertEquals(response.getAclProfileName(), "testProfileName2");
			
			boolean result = vm.deleteAclProfile("testProfileName2");
			assertTrue(result);		
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}


	@Test @Ignore
	public void testCreateDMRBridge()
	{
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testGetCreateDeleteBridgeWithSubscriptions()
	{
		List<Subscription> subscriptions = new ArrayList<Subscription>();
		SmfSubscription sub = new SmfSubscription("t/indir1", SubscriptionDirection.Ingoing, SubscriptionType.Direct);
		subscriptions.add(sub);
		sub = new SmfSubscription("t/indira1", SubscriptionDirection.Ingoing, SubscriptionType.DirectDeliverAlways);
		subscriptions.add(sub);
		sub = new SmfSubscription("t/ingar1", SubscriptionDirection.Ingoing, SubscriptionType.Guaranteed);
		subscriptions.add(sub);
		sub = new SmfSubscription("t/outgar1", SubscriptionDirection.Outgoing, SubscriptionType.Guaranteed);
		subscriptions.add(sub);
		sub = new SmfSubscription("t/outdir1", SubscriptionDirection.Outgoing, SubscriptionType.Direct);
		subscriptions.add(sub);
		sub = new SmfSubscription("t/outdira1", SubscriptionDirection.Outgoing, SubscriptionType.DirectDeliverAlways);
		subscriptions.add(sub);
		
		try
		{
			List<MsgVpnBridge> bridges = vm.getBridges();
			assertNotNull(bridges);
			int size = bridges.size();
			boolean result = vm.createBridge(remoteService, subscriptions, true, false, null, null, null, null, null, null);
			assertTrue(result);	
			
			bridges = vm.getBridges();
			int newSize = bridges.size();
			assertEquals(size+1, newSize);			
			
			result = vm.deleteBridge(remoteService);
			assertTrue(result);				
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddDeleteQueue()
	{
		try
		{
			MsgVpnQueue request = new MsgVpnQueue();
			request.setQueueName("testQueueName2");
			boolean result = vm.addQueue(request);
			assertNotNull(result);
			
			result = vm.deleteQueue("testQueueName2");
			assertNotNull(result);
			
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}
}
