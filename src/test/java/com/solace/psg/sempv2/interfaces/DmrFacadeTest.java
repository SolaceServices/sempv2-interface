package com.solace.psg.sempv2.interfaces;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.FacadeTestBase;

import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.config.model.DmrCluster;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridge;
import com.solace.psg.sempv2.interfaces.DmrFacade;
import com.solace.psg.sempv2.interfaces.ServiceFacade;

public class DmrFacadeTest extends FacadeTestBase
{
	private ServiceFacade sf;
	private ServiceDetails sdl;
	private ServiceDetails sdr;
	private DmrFacade dmr;
	
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
		

		sdl = sf.getServiceDetails(localServiceId);
		sdr = sf.getServiceDetails(remoteServiceId);
		dmr = new DmrFacade(sdl, sdr);
	}
	
	@Test  @Ignore
	public void testGetDMRCluster()
	{
		try
		{
			List<DmrCluster> resp = dmr.getDMRClusters();
			assertNotNull(resp);
			
			if (resp.size() > 0)
			{
				DmrCluster cl = resp.get(0);
				String dmrClusterName = cl.getDmrClusterName();
				assertNotNull(dmrClusterName);
				DmrCluster clNew = dmr.getDMRCluster(dmrClusterName);
				assertNotNull(clNew);
				assertEquals(dmrClusterName, clNew.getDmrClusterName());
			}
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test  @Ignore
	public void testGetDMRNodeName()
	{
		try
		{
			String resp = dmr.getDMRNodeName();
			assertNotNull(resp);
			assertTrue(resp.length() > 0);			
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test  @Ignore
	public void testGetDMRBridgeName()
	{
		try
		{
			List<MsgVpnDmrBridge> resp = dmr.getDmrBridges();
			assertNotNull(resp);
	
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

	@Test @Ignore
	public void testCreateDeleteBiDMRCluster()
	{
		try
		{
			boolean result = dmr.createBiDMRCluster();
			assertTrue(result);
			result = dmr.deleteBiDMRCluster();
			assertTrue(result);
		}
		catch (ApiException e)
		{
			fail(e.getMessage());
		}
	}

}
