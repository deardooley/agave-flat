package org.iplantc.service.notification.providers.http.clients;

import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class AgaveWebhookClientTest {

	@DataProvider
	protected Object[][] getFilteredCallbackUrlProvider() {
		return new Object[][] {
				{ "https://vdj-agave-api.tacc.utexas.edu/jobs/v2",
						"vdjserver.org",
						"https://vdjserver-org.api.prod.agaveapi.co/jobs" },
				{ "https://vdj-agave-api.tacc.utexas.edu:443/jobs/v2",
						"vdjserver.org",
						"https://vdjserver-org.api.prod.agaveapi.co/jobs" },
				{ "https://public.agaveapi.co/files/v2/media/systems/foobar-bat.com/path/to/file",
					"agave.prod",
					"https://agave-prod.api.prod.agaveapi.co/files/media/systems/foobar-bat.com/path/to/file" },};
	}

	@Test(dataProvider="getFilteredCallbackUrlProvider")
	public void getFilteredCallbackUrl(String callbackUrl, String tenantId, String expectedValue) 
	throws NotificationException 
	{
		TenancyHelper.setCurrentTenantId(tenantId);
		AgaveWebhookClient client = new AgaveWebhookClient(null);
		String filteredUrl = client.getFilteredCallbackUrl(callbackUrl);
		Assert.assertEquals(filteredUrl, expectedValue,
				"Callback URL was not filtered to the correct internal url.");

	}
}
