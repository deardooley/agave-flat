package org.iplantc.service.metadata;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.iplantc.service.common.restlet.AgaveApplication;
import org.iplantc.service.metadata.resources.MetadataCollection;
import org.iplantc.service.metadata.resources.MetadataDocumentationResource;
import org.iplantc.service.metadata.resources.MetadataResource;
import org.iplantc.service.metadata.resources.MetadataSchemaCollection;
import org.iplantc.service.metadata.resources.MetadataSchemaResource;
import org.iplantc.service.metadata.resources.MetadataSchemaShareResource;
import org.iplantc.service.metadata.resources.MetadataShareResource;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.util.Template;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 7/30/13
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetadataApplication extends AgaveApplication 
{
	private static final Logger log = Logger.getLogger(MetadataApplication.class);
	
	private MongoClient mongoClient = null;
	
    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createRoot() 
    {
        Router router = (Router)super.createRoot();
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
        return router;
    }

    @Override
    protected void mapServiceEndpoints(Router router) 
    {
        router.attach(getStandalonePrefix() + "/", MetadataDocumentationResource.class);
        
        if (!Settings.SLAVE_MODE) 
        {
        	secureEndpoint(router, "/schemas", MetadataSchemaCollection.class);
            secureEndpoint(router, "/schemas/", MetadataSchemaCollection.class);
            secureEndpoint(router, "/schemas/{schemaId}", MetadataSchemaResource.class);
            secureEndpoint(router, "/schemas/{schemaId}/", MetadataSchemaResource.class);
            secureEndpoint(router, "/schemas/{schemaId}/pems", MetadataSchemaShareResource.class);
            secureEndpoint(router, "/schemas/{schemaId}/pems/", MetadataSchemaShareResource.class);
            secureEndpoint(router, "/schemas/{schemaId}/pems/{user}", MetadataSchemaShareResource.class);
            secureEndpoint(router, "/schemas/{schemaId}/pems/{user}/", MetadataSchemaShareResource.class);
            secureEndpoint(router, "/data", MetadataCollection.class);
            secureEndpoint(router, "/data/", MetadataCollection.class);
            secureEndpoint(router, "/data/{uuid}", MetadataResource.class);
            secureEndpoint(router, "/data/{uuid}/", MetadataResource.class);
            secureEndpoint(router, "/data/{uuid}/pems", MetadataShareResource.class);
            secureEndpoint(router, "/data/{uuid}/pems/", MetadataShareResource.class);
            secureEndpoint(router, "/data/{uuid}/pems/{user}", MetadataShareResource.class);
            secureEndpoint(router, "/data/{uuid}/pems/{user}/", MetadataShareResource.class);
        }

    }
    
    @Override
	protected String getStandalonePrefix() {
		return !isStandaloneMode() ? "" : "/meta";
	}
    
    public MongoClient getMongoClient() throws UnknownHostException
    {
    	if (mongoClient == null ) 
    	{
	    	mongoClient = new MongoClient(new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT), Arrays.asList(getMongoCredential()));
    	} 
    	else if (!mongoClient.getConnector().isOpen()) 
    	{
    		try { mongoClient.close(); } catch (Exception e) { log.error("Failed to close mongo client.", e); }
    		mongoClient = null;
    		mongoClient = new MongoClient(new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT), Arrays.asList(getMongoCredential()));
    	}
    		
        return mongoClient;
    }
    
    private MongoCredential getMongoCredential() {
    	return MongoCredential.createMongoCRCredential(
                Settings.METADATA_DB_USER, "api", Settings.METADATA_DB_PWD.toCharArray());
    }
}