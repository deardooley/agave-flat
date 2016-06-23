/**
 * 
 */
package org.iplantc.service.profile.model.dto;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.InternalUser;
import org.json.JSONObject;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * User-facing Profile object to hold information about users.
 * 
 * @author dooley
 *
 */
@XStreamAlias("user")
public class InternalUserDto extends InternalUser
{
	public InternalUserDto(InternalUser internalUser) {
		
	}
	
	@Override
	public String toJSON() throws ProfileException
	{
		try {
			JSONObject jprofile = new JSONObject(super.toJSON())
				.put("active", active)
				.put("createdBy", createdBy)
				.put("uuid", uuid)
				.put("_links", new JSONObject()
					.put("self", new JSONObject()
						.put("href", Settings.IPLANT_PROFILE_SERVICE + this.createdBy + "/users/" + this.username)
					)
					.put("profile", new JSONObject()
						.put("href", Settings.IPLANT_PROFILE_SERVICE + this.createdBy)
					)
			);
				
			return jprofile.toString();
		} catch (Exception e) {
			throw new ProfileException("Error producing JSON output.", e);
		}
	}

	public InternalUser clone()
	{
		InternalUser clonedInternalUser = new InternalUser();
		try 
		{
			clonedInternalUser.setActive(active);
			clonedInternalUser.setUuid(new AgaveUUID(UUIDType.INTERNALUSER).toString());
			clonedInternalUser.setCity(city);
			clonedInternalUser.setCountry(country);
			clonedInternalUser.setCreatedBy(createdBy);
			clonedInternalUser.setDepartment(department);
			clonedInternalUser.setEmail(email);
			clonedInternalUser.setFax(fax);
			clonedInternalUser.setFirstName(firstName);
			clonedInternalUser.setGender(gender);
			clonedInternalUser.setInstitution(institution);
			clonedInternalUser.setLastName(lastName);
			clonedInternalUser.setPhone(phone);
			clonedInternalUser.setPosition(position);
			clonedInternalUser.setResearchArea(researchArea);
			clonedInternalUser.setState(state);
			clonedInternalUser.setUsername(username);
		} catch (ProfileArgumentException e) {}
		
		return clonedInternalUser;
	}
	
}
