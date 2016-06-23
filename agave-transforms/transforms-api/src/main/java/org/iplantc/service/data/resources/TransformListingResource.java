/**
 * 
 */
package org.iplantc.service.data.resources;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.data.Settings;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.data.util.ServiceUtils;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
//import org.iplantc.service.io.dao.StatsDao;

/**
 * Class to handle get and post requests for transforms. This supports an optional 
 * tags url parameter as a search option.
 * 
 * @author dooley
 * 
 */
public class TransformListingResource extends AbstractTransformResource 
{
	private String transformName;
	private String tag;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public TransformListingResource(Context context, Request request, Response response) {
		super(context, request, response);

		transformName = (String)request.getAttributes().get("transformId");
		
		Form form = request.getOriginalRef().getQueryAsForm();
        
		if (StringUtils.isEmpty(transformName)) {
			tag = (String)form.getFirstValue("tag");
			try {
				if (!StringUtils.isEmpty(tag)) {
					tag = URLDecoder.decode(tag, "utf-8");
				}
			} catch (UnsupportedEncodingException e) {}
			
			AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.TRANSFORMS02.name(), 
					AgaveLogServiceClient.ActivityKeys.DataSearchByTag.name(), 
					getAuthenticatedUsername(), "", getRequest().getClientInfo().getUpstreamAddress());
		} 
		else
		{
			AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.TRANSFORMS02.name(), 
					AgaveLogServiceClient.ActivityKeys.DataList.name(), 
					"guest", "", getRequest().getClientInfo().getUpstreamAddress());
		}
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}

	/** 
	 * This method represents the HTTP GET action. The transforms matching the request name
	 * is returned in JSON format. If no format is specified, all transforms are returned
	 * as a {@link org.json.JSONArray JSONArray} of {@link org.json.JSONObject JSONObject}. 
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		
		FileTransformProperties transformProps = new FileTransformProperties();
		
		if (ServiceUtils.isValid(transformName)) {
			
			FileTransform transform;
			try 
			{
				transform = transformProps.getTransform(transformName);
				
				if (transform == null) 
				{
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No tranform found matching " + transformName);
				} 
				else 
				{	
					JSONWriter writer = new JSONStringer();
					
					writer.object()
						.key("name").value(transform.getId())
						.key("description").value(transform.getDescription())
						.key("descriptionurl").value(transform.getDescriptionURI())
						.key("enabled").value(transform.isEnabled())
						.key("tags").array();
					
						if (!StringUtils.isEmpty(transform.getTags())) 
						{
							for(String tag: StringUtils.split(transform.getTags(), ',')) {
								writer.value(tag);
							}
						}
						
						writer.endArray()
						.key("encoder")
							.object()
								.key("name").value(transform.getEncodingChain().getId())
								.key("description").value(transform.getEncodingChain().getDescription())
							.endObject()
						.key("decoders").array();
						
						if (transform.getDecoders() != null)
						{
							for (FilterChain decoder: transform.getDecoders()) {
								writer.object()
									.key("name").value(decoder.getId())
									.key("description").value(decoder.getDescription())
								.endObject();
							}		
						}
					
						writer.endArray()
					.endObject();
			
					return new IplantSuccessRepresentation(writer.toString());
				}
			} catch (ResourceException e) {
				getResponse().setStatus(e.getStatus());
	            return new IplantErrorRepresentation(e.getMessage());
			} catch (Exception e) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
	            return new IplantErrorRepresentation("Failed to retrieve transform listing. " + e.getMessage());
			}
		} 
		else 
		{
			try 
			{
				List<FileTransform> transforms = null;
				if (StringUtils.isEmpty(tag)) {
					transforms = transformProps.getTransforms();
				} else {
					transforms = transformProps.getTransformsByTag(tag);
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
						
						writer.endArray()
						.key("encoder")
							.object()
								.key("name").value(transform.getEncodingChain().getId())
								.key("description").value(transform.getEncodingChain().getDescription())
							.endObject()
						.key("decoders").array();

		                    //Ignore Decoders for one-way transforms, such as HEAD, TAIL, etc
		                    if (transform.getDecoders() != null) {
		                        for (FilterChain decoder : transform.getDecoders()) {
		                            writer.object()
		                                    .key("name").value(decoder.getId())
		                                    .key("description").value(decoder.getDescription())
		                                    .endObject();
		                        }
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
				
			} catch (Exception e) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
	            return new IplantErrorRepresentation("Failed to retrieve transform listing. " + e.getMessage());
			}
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