package org.iplantc.service.profile.model;

import java.io.IOException;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.enumeration.GenderType;
import org.json.JSONStringer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XmlRootElement
@XStreamAlias("profile")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
public class Profile {

	@XStreamOmitField
	@JsonIgnore
	protected long id;
	protected String username;
	@JsonIgnore
	protected String email;
	protected String firstName;
	protected String lastName;
	protected String position;
	protected String institution;
	protected String phone;
	protected String fax;
	protected String researchArea;
	protected String department;
	protected String city;
	protected String state;
	protected String country;
	protected GenderType gender;
	protected String uuid;
	
	public Profile() {}
	
	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username the username to set
	 * @throws ProfileArgumentException 
	 */
	public void setUsername(String username) throws ProfileArgumentException
	{
		this.username = username;
	}

	/**
	 * @return the email
	 */
	public String getEmail()
	{
		return email;
	}

	/**
	 * @param email the email to set
	 * @throws ProfileArgumentException 
	 */
	public void setEmail(String email) throws ProfileArgumentException
	{
		this.email = email;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName()
	{
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 * @throws ProfileArgumentException 
	 */
	public void setFirstName(String firstName) throws ProfileArgumentException
	{
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName()
	{
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 * @throws ProfileArgumentException 
	 */
	public void setLastName(String lastName) throws ProfileArgumentException
	{
		this.lastName = lastName;
	}

	/**
	 * @return the position
	 */
	public String getPosition()
	{
		return position;
	}

	/**
	 * @param position the position to set
	 * @throws ProfileArgumentException 
	 */
	public void setPosition(String position) throws ProfileArgumentException
	{
		this.position = position;
	}

	/**
	 * @return the institution
	 */
	public String getInstitution()
	{
		return institution;
	}

	/**
	 * @param institution the institution to set
	 * @throws ProfileArgumentException 
	 */
	public void setInstitution(String institution) throws ProfileArgumentException
	{
		this.institution = institution;
	}

	/**
	 * @return the id
	 */
	public long getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id)
	{
		this.id = id;
	}

	/**
	 * @return the phone
	 */
	public String getPhone()
	{
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone)
	{
		this.phone = phone;
	}

	/**
	 * @return the fax
	 */
	public String getFax()
	{
		return fax;
	}

	/**
	 * @param fax the fax to set
	 */
	public void setFax(String fax)
	{
		this.fax = fax;
	}

	/**
	 * @return the researchArea
	 */
	public String getResearchArea()
	{
		return researchArea;
	}

	/**
	 * @param researchArea the researchArea to set
	 * @throws ProfileArgumentException 
	 */
	public void setResearchArea(String researchArea) throws ProfileArgumentException
	{
		this.researchArea = researchArea;
	}
	
	/**
	 * @return the department
	 */
	public String getDepartment()
	{
		return department;
	}

	/**
	 * @param department the department to set
	 * @throws ProfileArgumentException 
	 */
	public void setDepartment(String department) throws ProfileArgumentException
	{
		this.department = department;
	}

	/**
	 * @return the city
	 */
	public String getCity()
	{
		return city;
	}

	/**
	 * @param city the city to set
	 * @throws ProfileArgumentException 
	 */
	public void setCity(String city) throws ProfileArgumentException
	{
		this.city = city;
	}

	/**
	 * @return the state
	 */
	public String getState()
	{
		return state;
	}

	/**
	 * @param state the state to set
	 * @throws ProfileArgumentException 
	 */
	public void setState(String state) throws ProfileArgumentException
	{
		this.state = state;
	}

	/**
	 * @return the country
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * @param country the country to set
	 * @throws ProfileArgumentException 
	 */
	public void setCountry(String country) throws ProfileArgumentException
	{
		this.country = country;
	}

	/**
	 * @return the gender
	 */
	public GenderType getGender()
	{
		return gender;
	}

	/**
	 * @param gender the gender to set
	 */
	public void setGender(GenderType gender)
	{
		this.gender = gender;
	}
	
	/**
	 * @return the uuid
	 */
	public String getUuid()
	{
		String tenantId = TenancyHelper.getCurrentTenantId();
		TenantDao tenantDao = new TenantDao();
		String uuid = null;
		try
		{
			Tenant tenant = tenantDao.findByTenantId(tenantId);
			if (tenant != null) 
			{
				String[] tenantTokens = StringUtils.split(tenant.getUuid(), "-");
				if (tenantTokens != null) 
				{
					uuid = String.format("%s-%s-%s-%s", 
										tenantTokens[0], 
										getUsername(), 
										"0001", 
										UUIDType.PROFILE.getCode());
				}
			}
		}
		catch (TenantException e) {}
		
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Profile [username=" + username + "]";
	}
	
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
				.key("uuid").value(getUuid())
                .key("_links").object()
	            	.key("self").object()
	            		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + this.username)
	            	.endObject()
	            	.key("users").object()
	            		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + this.username + "/users")
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

@JsonSerialize(using=ProfileSerializer.class)
class ProfileSerializer extends JsonSerializer<Profile> {
    @Override
    public void serialize(Profile profile, JsonGenerator jsonGenerator, 
            SerializerProvider serializerProvider) throws IOException {
    	
    	try
		{
			jsonGenerator.writeRaw(profile.toJSON());
		}
		catch (ProfileException e)
		{
			throw new IOException(e);
		}
    }
}
