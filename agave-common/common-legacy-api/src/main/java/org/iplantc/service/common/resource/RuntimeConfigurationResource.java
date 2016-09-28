package org.iplantc.service.common.resource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple API to manage runtime configurations of this container.
 * Though no sensitive information will be provided, a user can 
 * still add/update the runtime configuration of this container
 * allowing for repurposing on the fly.
 * 
 * @author dooley
 *
 */
public class RuntimeConfigurationResource extends AgaveResource 
{
	private static final Logger log = Logger.getLogger(RuntimeConfigurationResource.class);
	
	private ObjectMapper mapper = new ObjectMapper();
    
	public RuntimeConfigurationResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);
		
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
//		getVariants().add(new Variant(MediaType.TEXT_HTML));
	}
    
    @Override
	public Representation represent(Variant variant) throws ResourceException
	{
        if (AuthorizationHelper.isTenantAdmin(getAuthenticatedUsername())) 
        {
            try 
            {
                ObjectNode json = getRuntimeConfiguration();
                
                return new IplantSuccessRepresentation(json.toString());
            }
            catch (ResourceException e) {
                log.error(e);
                throw e;
            }
            catch (Throwable e)
            {
                log.error("Error reading runtime environment", e);
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                        "Error while reading the runtime environment");
            }
        }
        else 
        {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                    "User does not have permission to view this service configuration", 
                    new PermissionException());
            
        }
	}
    
    @Override
    public void storeRepresentation(Representation entity)
    throws ResourceException
    {
        if (AuthorizationHelper.isTenantAdmin(getAuthenticatedUsername()))
        {
            Map<String,String> form = getPostedEntityAsMap();
            
            List<String> editableConfigurationSettings = getEditableRuntimeConfigurationFields();
            
            for (Entry<String,String> entry: form.entrySet()) {
                if (editableConfigurationSettings.contains(entry.getKey())) {
                    setRuntimeConfigurationField(entry.getKey(), entry.getValue());
                }
            }
            
            getResponse().setStatus(Status.SUCCESS_OK);
            getResponse().setEntity(new IplantSuccessRepresentation(
                    getRuntimeConfiguration().toString()));
        }
        else 
        {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                    "User does not have permission to change this service configuration");
            
        }
    }
    
    /* (non-Javadoc)
     * @see org.restlet.resource.Resource#removeRepresentations()
     */
    @Override
    public void removeRepresentations() throws ResourceException 
    {
        if (AuthorizationHelper.isTenantAdmin(getAuthenticatedUsername()))
        {
            Map<String,String> form = getPostedEntityAsMap();
            
            List<String> editableConfigurationSettings = getEditableRuntimeConfigurationFields();
            
            for (Entry<String,String> entry: form.entrySet()) {
                if (editableConfigurationSettings.contains(entry.getKey())) {
                    setRuntimeConfigurationField(entry.getKey(), "");
                }
            }
            
            getResponse().setStatus(Status.SUCCESS_OK);
            getResponse().setEntity(new IplantSuccessRepresentation());
        }
        else 
        {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                    "User does not have permission to change this service configuration");
            
        }
    }
    

    /**
     * Reads in runtime configuration from service properties file,
     * environment, and container settings.
     * 
     * @return {@link ObjectNode} with container, environment, and configuration objects.
     */
    private ObjectNode getRuntimeConfiguration() 
    throws ResourceException
    {
        try {
//            Properties props = Settings.loadRuntimeProperties();
            
            ObjectNode container = mapper.createObjectNode();
            container.put("hostname", Settings.getLocalHostname());
            container.put("ipAddress", Settings.getIpLocalAddress());
            container.put("containerId", Settings.getContainerId());
            
            ObjectNode config = mapper.createObjectNode();
            
            for (Field field: Settings.class.getDeclaredFields()) {
                int mods = field.getModifiers();
                
                if (Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
                    continue;
                } else {
//                for (Entry<Object,Object> entry: props.entrySet()) {
//                if (field.isAccessible()) {
                	try {
	                	System.out.println(field.getName());
	                	System.out.println(field.getName() + "=" + field.get(null).toString());
	                	if (field.get(null) == null) {
	                		config.putNull(field.getName());
	                	}
	                	else {
	                		Class<?> t = field.getType();
	                		if (t == int.class) {
	                			config.put(field.getName(), field.getInt(null));
	                		}
	                		else if (t == double.class){ 
	                			config.put(field.getName(), field.getDouble(null));
	                		}
	                		else if (t == long.class){ 
	                			config.put(field.getName(), field.getLong(null));
	                		}
	                		else if (t == boolean.class){ 
	                			config.put(field.getName(), field.getBoolean(null));
	                		}
	                		else if (t == float.class){ 
	                			config.put(field.getName(), field.getFloat(null));
	                		}
	                		else if (t == byte.class){ 
	                			config.put(field.getName(), field.getByte(null));
	                		}
	                		else if (t == char.class){ 
	                			config.put(field.getName(), field.getChar(null));
	                		}
	                		else {
	                			config.put(field.getName(), field.get(null).toString());
	                		}
	                	}
                	} 
                	catch (Throwable t) {
                		log.error("Failed to read property " + field.getName() + " from runtime environment.", t);
                	}
                    
//                }
                }
            }
            
            for (String editableFieldName: getEditableRuntimeConfigurationFields()) {
                Field field = Settings.class.getDeclaredField(editableFieldName);
                int mods = field.getModifiers();
                if (Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
                    continue;
                } else {
                	config.put(editableFieldName, field.get(Settings.class).toString());
                }
            }
            
            ObjectNode environment = mapper.createObjectNode();
            for (Entry<String, String> entry: System.getenv().entrySet()) {
                environment.put(entry.getKey(), entry.getValue());
            }
            
            ObjectNode json = (ObjectNode)mapper.createObjectNode();
            json.set("container", container);
            json.set("configuration", config);
            json.set("environment", environment);
            
            return json;
        } 
        catch (Exception e) {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Error reading runtime configuration.", e);
        }
    }

    
    
	/**
	 * Updates the value of a runtime configuration value used by this service
	 * instance.
	 * 
	 * @param key
	 * @param value
	 */
	private void setRuntimeConfigurationField(String fieldName, String value) 
	throws ResourceException
	{
	    try {
            Field f = Settings.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.isAccessible()){
                if (f.getType().isArray()) {
                    f.set(null, StringUtils.split(value, ","));
                } else if (f.getType() == Boolean.class) {
                    f.set(null,  BooleanUtils.toBoolean(value));
                } else {
                    f.set(null, value);
                }
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                    fieldName + " cannot be updated at runtime due to system security settings.");
            }
             
        } catch (NoSuchFieldException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    " Unrecognized field " + fieldName, e);
        } catch (IllegalAccessException | SecurityException e) {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                    fieldName + " cannot be updated at runtime due to system security settings.");
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    " Invalid value for " + fieldName, e);
        } catch (ResourceException e) {
            throw e;
        }
    }

    /**
	 * Gets white list of runtime configuration values for a given 
	 * service implementation.
	 * 
	 * @return list of valid values to be updated.
	 */
	protected List<String> getEditableRuntimeConfigurationFields() 
	{
	    return Settings.getEditableRuntimeConfigurationFields();
    }
	
	

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return true;
	}
    
    
}
