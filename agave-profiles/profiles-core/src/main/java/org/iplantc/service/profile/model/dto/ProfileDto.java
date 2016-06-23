package org.iplantc.service.profile.model.dto;

import javax.xml.bind.annotation.XmlRootElement;

import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.Profile;
import org.json.JSONStringer;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XmlRootElement
@XStreamAlias("profile")
public class ProfileDto extends Profile {

	public ProfileDto() {}
	
	public String toJSON() throws ProfileException {
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{	
			js.object()
				.key("username").value(this.username)
				.key("email").value(this.email)
				.key("firstName").value(this.firstName)
				.key("lastName").value(this.lastName)
				.key("position").value(this.position)
				.key("institution").value(this.institution)
				.key("phone").value(phone)
				.key("fax").value(this.fax)
				.key("researchArea").value(this.researchArea)
				.key("department").value(this.department)
				.key("city").value(this.city)
				.key("state").value(this.state)
				.key("country").value(this.country)
				.key("gender").value(this.gender == null ? "" : gender.name())
                .key("_links").object()
	            	.key("self").object()
	            		.key("href").value(Settings.IPLANT_PROFILE_SERVICE + this.username)
	            	.endObject()
	            	.key("users").object()
	            		.key("href").value(Settings.IPLANT_PROFILE_SERVICE + this.username + "/users")
	            	.endObject()
	            .endObject()
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			throw new ProfileException("Error producing JSON output.", e);
		}

		return output;
	}
	
	
}
