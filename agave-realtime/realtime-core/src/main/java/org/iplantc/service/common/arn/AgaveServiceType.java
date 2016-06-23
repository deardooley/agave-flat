/**
 * 
 */
package org.iplantc.service.common.arn;

import org.iplantc.service.common.uuid.UUIDType;

/**
 * All known agave service types for use in ARN
 * 
 * @author dooley
 *
 */
public enum AgaveServiceType {
	
	CLIENTS(new String[] {
			"^clients(?:/[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.CLIENTS.getCode() + ")", // all client by uuid
			"^clients/(\\*|[0-9a-fA-F_\\-]+)", // all clients or named client
			"^clients/(\\*|[0-9a-fA-F_\\-]+)/(pems|history|subscriptions)", // individual or all client subscriptions
			"^clients/(\\*|[0-9a-fA-F_\\-]+)/(pems|history|subscriptions)/(\\*|[0-9a-fA-F_\\-]+)", // individual client subscriptions to a named api
		}),
		
	PROFILE(new String[] {
			"^profiles(?:/[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.PROFILE.getCode() + ")", // all client by uuid
			"^profiles/(\\*|[0-9a-fA-F_\\-]+)", // all clients or named client
			"^profiles/(\\*|[0-9a-fA-F_\\-]+)/(pems|history|internalusers)", // individual or all client subscriptions
			"^profiles/(\\*|[0-9a-fA-F_\\-]+)/(pems|history|internalusers)/(\\*|[0-9a-fA-F_\\-]+)", // individual client subscriptions to a named api
		}),
	
	FILE(new String[] {
			"^files/([0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.FILE.getCode() + ")/(media|pems|listings|history)", // relative file paths
			"^files/(media|pems|listings|history)/([^\\?]*)([\\?]+.*)?", // relative file paths
			"^files/(media|pems|listings|history)/system/([0-9a-zA-Z\\.\\-]{3,64})/([^\\?]*)([\\?]+.*)?", // file path with explicity system id
		}),
	
	TOKEN(new String[] {
			"^tokens/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TOKEN.getCode() + "|[0-9a-fA-F_\\-]+",
			"^tokens/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TOKEN.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^tokens/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TOKEN.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
		
	APP(new String[] {
			"^apps/()", // by uuid
			"^apps/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.APP.getCode() + "|[0-9a-fA-F_\\-\\(\\)]{2,64}-[0-9\\.0-9[\\.0-9]+])", // by app id
			"^apps/(\\*|[0-9a-fA-F_\\-]+)/(pems|history)", // pems and history by app id
			"^apps/(\\*|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
			"^deployment-systems/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})", // by app id
			"^execution-systems/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})", // by app id
		}),
		
	SYSTEM(new String[] {
			"^storage-systems", // by type
			"^execution-systems", // by type
			"^systems/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})", // by app id
			"^systems/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})/(roles|history|batchQueues)", // pems and history by app id
			"^systems/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})/(roles|history|batchQueues)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
		
	JOB(new String[] {
			"^jobs/([0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.JOB.getCode() + ")", // by uuid
			"^jobs/(\\*|[0-9a-fA-F_\\-]+)/(pems|history)", // pems and history by app id
			"^jobs/(\\*|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
			"^job-apps/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.APP.getCode() + "|[0-9a-fA-F_\\-\\(\\)]{2,64}-[0-9\\.0-9[\\.0-9]+])",
			"^job-execution-system/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})",
			"^job-archive-system/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})",
		}),
		
	
	TRANSFORM(new String[] {
			"^transforms/([0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TRANSFORM.getCode() + ")/(media|pems|listings|history)", // relative file paths
			"^transforms/(\\*|[0-9a-fA-F_\\-]+)/([^\\?]*)([\\?]+.*)?", // file path with explicity system id
			"^transforms/(\\*|[0-9a-fA-F_\\-]+)/system/([0-9a-zA-Z\\.\\-]{3,64})/([^\\?]*)([\\?]+.*)?", // file path with explicity system id
		}),
//		ENCODING,
//		DECODING("018"),
//		STAGING("020"),
	TRANSFER(new String[] {
			"^transfers/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TRANSFER.getCode() + ")", // all or by uuid
			"^transfers/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TRANSFER.getCode() + ")/(pems|history|tasks)", // tasks for all or uuid
			"^transfers/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TRANSFER.getCode() + ")/(pems|history|tasks)/([^\\?]*)([\\?]+.*)?", // relative file paths
			"^transfer-source/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})", //  // transfers from a source system
			"^transfer-source/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})/([^\\?]*)([\\?]+.*)?", // transfers from a source system & path
			"^transfer-dest/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})", // transfers to a destination system
			"^transfer-dest/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SYSTEM.getCode() + "|[0-9a-zA-Z\\.\\-]{3,64})/([^\\?]*)([\\?]+.*)?", // transfers to a destination system & path
		}),
//		SCHEDULE
//		TASK
		
	POSTIT(new String[] {
			"^postits/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.POSTIT.getCode() + "|[0-9a-fA-F_\\-]+",
			"^postits/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.POSTIT.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^tokens/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.POSTIT.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
		
	
	NOTIFICATION(new String[] {
			"^notification/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.NOTIFICATION.getCode() + "|[0-9a-fA-F_\\-]+",
			"^notification/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.NOTIFICATION.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^notification/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.NOTIFICATION.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
//		NOTIFICATION_DELIVERY("042"),
	
	METADATA(new String[] {
			"^metadata/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.METADATA.getCode() + "|[0-9a-fA-F_\\-]+",
			"^metadata/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.METADATA.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^metadata/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.METADATA.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
			"^schema/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SCHEMA.getCode() + "|[0-9a-fA-F_\\-]+",
			"^schema/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SCHEMA.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history|metadata)", // individual or all subresources
			"^schema/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.SCHEMA.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
//		SCHEMA("013"),
//		METADATA
	MONITOR(new String[] {
			"^monitors/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.MONITOR.getCode() + "|[0-9a-fA-F_\\-]+",
			"^monitors/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.MONITOR.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history|checks)", // individual or all subresources
			"^monitors/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.MONITOR.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
			"^checks/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.MONITORCHECK.getCode() + "|[0-9a-fA-F_\\-]+"
		}),
//		CHECK("015"),
	
	TENANT(new String[] {
			"^tenants/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TENANT.getCode() + "|[0-9a-fA-F_\\-]+",
			"^tenants/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TENANT.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^tenants/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TENANT.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
	
	USAGE(new String[] {
			"^trigger/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.USAGETRIGGER.getCode() + "|[0-9a-fA-F_\\-]+",
			"^trigger/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.USAGETRIGGER.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^trigger/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.USAGETRIGGER.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
//		USAGE_TRIGGER("019"),

	STATS(new String[] {
			"^stats/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.USAGETRIGGER.getCode() + "|[0-9a-fA-F_\\-]+",
			"^stats/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.USAGETRIGGER.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^stats/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.USAGETRIGGER.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
    
    TAG(new String[] {
    		"^tags/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TAG.getCode() + "|[0-9a-fA-F_\\-]+",
			"^tags/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TAG.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)", // individual or all subresources
			"^tags/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.TAG.getCode() + "|[0-9a-fA-F_\\-]+)/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
			"^tag-resources/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-[0-9]{3}|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
    	
    GROUP(new String[] {
    		"^groups/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.GROUP.getCode() + "|[0-9a-fA-F_\\-]{2-32}",
			"^groups/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.GROUP.getCode() + "|[0-9a-fA-F_\\-]{2-32})/(pems|history,profiles)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
//    	EVENT
    REPOSITORY(new String[] {
    		"^repositories/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.REPOSITORY.getCode() + "|[0-9a-fA-F_\\-]{2-32}",
			"^repositories/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.REPOSITORY.getCode() + "|[0-9a-fA-F_\\-]{2-32})/(pems|history,types)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
//    	REPOSITORY_EVENT("053"),
    LAMBDA(new String[] {
    		"^repositories/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.LAMBDA.getCode() + "|[0-9a-fA-F_\\-]{2-32}",
			"^repositories/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.LAMBDA.getCode() + "|[0-9a-fA-F_\\-]{2-32})/(pems|history,repositories)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		}),
//    	LAMBDA_EVENT("053"),
	REALTIME(new String[] {
			"^groups/\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.REALTIME_CHANNEL.getCode() + "|[0-9a-fA-F_\\-]{2-32}",
			"^groups/(\\*|[0-9a-f]+-[0-9a-f]+-[0-9]+-" + UUIDType.REALTIME_CHANNEL.getCode() + "|[0-9a-fA-F_\\-]{2-32})/(pems|history)/(\\*|[0-9a-fA-F_\\-]+)", // individual or all of a particular subresource
		});

	private String[] resourcePathExpressions;
	
	private AgaveServiceType(String[] resourcePathExpressions) {
		this.resourcePathExpressions = resourcePathExpressions;
	}
	
	/**
	 * Attempts to match the resourcePath against the known set of 
	 * resource path expressions for this service.
	 * 
	 * @param resourcePath
	 * @return true if the path matches any expression. false otherwise
	 */
	public boolean hasResourcePath(String resourcePath) {
		for (String resourcePathExpression: resourcePathExpressions) {
			if (resourcePath.matches(resourcePathExpression)) {
				return true;
			}
		}
		
		return false;
	}
//		CHANNEL("054");
}
