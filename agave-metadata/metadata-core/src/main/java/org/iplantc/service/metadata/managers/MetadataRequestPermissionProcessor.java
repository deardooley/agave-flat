package org.iplantc.service.metadata.managers;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataPermission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author dooley
 *
 */
public class MetadataRequestPermissionProcessor {

	private List<MetadataPermission> permissions;
	private String owner;
	private String uuid;
	
	public MetadataRequestPermissionProcessor(String owner, String uuid) {
		this.setPermissions(new ArrayList<MetadataPermission>());
		this.setOwner(owner);
		this.setUuid(uuid);
	}
	
	/**
	 * Processes a {@link JsonNode} passed in with a job request as a 
	 * permission configuration. Accepts an array of {@link MetadataPermission} 
	 * request objects or a simple string;
	 *  
	 * @param json
	 * @throws MetadataException
	 * @throws PermissionException
	 */
	public void process(ArrayNode json) throws MetadataException, PermissionException {
		
		getPermissions().clear();
		
		if (json == null || json.isNull()) {
			// ignore the null value
			return;
		}
		else
		{
			for (int i=0; i<json.size(); i++)
			{
				JsonNode jsonPermission = json.get(i);
				if (!jsonPermission.isObject())
				{
					throw new MetadataException("Invalid permissions["+i+"] value given. "
						+ "Each permission objects should specify a "
						+ "valid username and permission.");
				}
				else
				{
					// here we reuse the validation built into the {@link MetadataPermissionManager}
					// to validate the embedded {@link MetadataPermission}.
					try {
						MetadataPermissionManager pm = new MetadataPermissionManager(getUuid(), getOwner());
						
						// extract the username and permission from the json node. we leave validation up 
						// to the manager class.
						String pemUsername = jsonPermission.hasNonNull("username") ? jsonPermission.get("username").asText() : null;
						String pemPem = jsonPermission.hasNonNull("permission") ? jsonPermission.get("permission").asText() : null;
						
						pm.setPermission(pemUsername, pemPem);
						
						getPermissions().add(pm.getPermission(pemUsername));
					} 
					catch (MetadataException | PermissionException e) {
						throw e;
					} 
					catch (Throwable e) {
						throw new MetadataException("Unable to process metadata permission["+i+"].", e);
					}
				}
			}
		}
		
	}

	/**
	 * @return the permissions
	 */
	public List<MetadataPermission> getPermissions() {
		return permissions;
	}

	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(List<MetadataPermission> permissions) {
		this.permissions = permissions;
	}

	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
