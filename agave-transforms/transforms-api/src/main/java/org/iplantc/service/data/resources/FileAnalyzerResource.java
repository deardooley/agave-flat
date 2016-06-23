/**
 * 
 */
package org.iplantc.service.data.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.data.Settings;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Class to handle get and post requests for jobs
 * 
 * @author dooley
 * @deprecated
 */
public class FileAnalyzerResource extends AbstractTransformResource 
{
	private static final Logger log = Logger.getLogger(FileAnalyzerResource.class); 

	private String owner;  		// username listed at the root of the url path
	private RemoteSystem system;
	private String internalUsername;
	private String username;
	private String path;

	private SystemManager manager;
	private SystemDao dao;
	private RemoteDataClient remoteClient;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public FileAnalyzerResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.owner = (String)request.getAttributes().get("owner");
		this.internalUsername = (String)request.getAttributes().get("internal.username");
        String sysId = (String)request.getAttributes().get("systemId");
		
        manager = new SystemManager();
        dao = new SystemDao();
/*
        if (StringUtils.isEmpty(systemId)) {
        	system = manager.getUserDefaultStorageSystem(owner);
        } else {
        	system = dao.findBySystemId(systemId);
        }
*/
        try {
            if (sysId != null) {
                this.system = dao.findBySystemId(sysId);
            } else {
                // If no system specified, select the user default system

                this.system = manager.getUserDefaultStorageSystem(username);
            }

            this.remoteClient = new RemoteDataClientFactory().getInstance(
                    system, (String) context.getAttributes().get("internalUsername"));

        } catch (Exception e) {
            log.error("Failed to connect to remote system");
            remoteClient.disconnect();
        }

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.TRANSFORMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.DataSearchByFile.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
	}

	/** 
	 * This method represents the HTTP GET action. The transforms available to the file path 
	 * terminating this endpoint are pulled from the transforms.xml file and listed here as a 
	 * {@link org.json.JSONArray JSONArray} of {@link org.json.JSONObject JSONObject}. 
	 * <p>
	 * The format for the returned transforms is:
	 * <p>
	 * [{<br>
	 *  &nbsp "name":"newick",<br>
	 *  &nbsp "pattern":"newick*",<br>
	 *  &nbsp "handler":"newick.pl",<br>
	 *  &nbsp "enabled":"true",<br>
	 *  }, {<br>
	 *  &nbsp "name":"contrast-tree",<br>
	 *  &nbsp "pattern":"[0-9]^.[0-9]",<br>
	 *  &nbsp "handler":"contrast-tree.pl",<br>
	 *  &nbsp "enabled":"false",<br>
	 *  }]<br>
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		try {
			List<FileTransform> transforms = new ArrayList<FileTransform>();
			
			try {
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolve(owner, originalPath);
			} catch (Exception e) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new IplantErrorRepresentation("Invalid file path");
			}
	        
			LogicalFile logicalFile = null;
            try {
                logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
            } catch(Exception e) {
                // Same odd issue
            }

			PermissionManager pm = new PermissionManager(system, remoteClient, logicalFile, username);

            remoteClient.authenticate();
			if (!remoteClient.doesExist(path)) { 
				// if it doesn't exist, it was deleted outside of the api, 
				// so clean up the file entry in the db and its permissions
				if (logicalFile != null) {
//					// permissions should be deleted on cascade
//					logicalFile.addEvent(new FileEvent("DELETED", 
//							"File or directory deleted outside of API",
//							logicalFile.getOwner()));
					LogicalFileDao.removeSubtree(logicalFile);
				}
				
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return new IplantErrorRepresentation("File does not exist");
			
			} else if (remoteClient.isDirectory(path)) { // return file listing
				
				getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
				return new IplantErrorRepresentation("Directory analysis not supported");
				
			} else {
				
				try {
					if (!pm.canRead(remoteClient.resolvePath(path))) {
						getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
						return new IplantErrorRepresentation("User does not have access to view the requested resource");
					}
				} catch (PermissionException e) {
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
					return new IplantErrorRepresentation("Failed to retrieve permissions for " + path + ", " + e.getMessage());
				}
				
				if (logicalFile == null) {
					if (!owner.equals(username)) {
						getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
						return new IplantErrorRepresentation("User does not have access to view the requested resource");
					}
					// add the file
					logicalFile = new LogicalFile();
					logicalFile.setName(FilenameUtils.getName(path));
					logicalFile.setSystem(system);
					logicalFile.setInternalUsername(internalUsername);
					logicalFile.setSourceUri("");
					logicalFile.setNativeFormat("raw");
					logicalFile.setPath(remoteClient.resolvePath(path));
					logicalFile.setOwner(owner);
					logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED.name());
					LogicalFileDao.persist(logicalFile);
				} 
				
				try 
				{
					// look up the file's original format. 
					FileTransformProperties props = new FileTransformProperties();
					FileTransform inTransform = props.getTransform(logicalFile.getNativeFormat());

                    if (inTransform != null) {

					    for (FilterChain outTransform: inTransform.getDecoders()) {
						    transforms.add(props.getTransform(outTransform.getId()));
					    }
                    }
					
				} catch (Exception e) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					return new IplantErrorRepresentation( "Failed to retrieve transforms for " + path + ". " + e.getMessage());
				} 
			} 
			
			JSONWriter writer = new JSONStringer().array();
			for (int i=Math.min(offset, transforms.size()-1); i< Math.min((limit+offset), transforms.size()); i++)
			{
				FileTransform transform = transforms.get(i);
				writer.object()
					.key("name").value(transform.getId())
					.key("description").value(transform.getDescription())
					.key("descriptionurl").value(transform.getDescriptionURI())
					.key("enabled").value(transform.isEnabled())
					.key("tags").array();
				
				for(String tag: StringUtils.split(transform.getTags(), ',')) {
					writer.object().key("name").value(tag).endObject();
				}
				
				writer.endArray().key("encoder")
						.object()
							.key("name").value(transform.getEncodingChain().getId())
							.key("description").value(transform.getEncodingChain().getDescription())
						.endObject()
					.key("decoders").array();
					
				for (FilterChain decoder: transform.getDecoders()) {
					writer.object()
						.key("name").value(decoder.getId())
						.key("description").value(decoder.getDescription())
					.endObject();
				}		
				writer.endArray()
				.key("_links").object()
                	.key("self").object()
                		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TRANSFORMS_SERVICE) + transform.getId())
                	.endObject()
            	.endObject()
            	.endObject();
			}
			
			return new IplantSuccessRepresentation(writer.endArray().toString());
		}
		catch (Exception e) 
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Failed to retrieve transform listing " + e.getMessage());
		} finally {
			try { remoteClient.disconnect(); } catch (Exception e1) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut() {
		return false;
	}
	
}
