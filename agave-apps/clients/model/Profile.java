package org.iplantc.service.clients.model;

import org.iplantc.service.clients.exceptions.ProfileException;
import org.iplantc.service.clients.model.enumerations.GenderType;
import org.iplantc.service.clients.util.ClientUtils;
import org.json.JSONStringer;

import com.fasterxml.jackson.databind.JsonNode;

public class Profile {

	protected long id;
	protected String username;
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
	 * @throws ProfileException 
	 */
	public void setUsername(String username) throws ProfileException
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
	 */
	public void setEmail(String email)
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
	 */
	public void setFirstName(String firstName)
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
	 */
	public void setLastName(String lastName)
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
	 */
	public void setPosition(String position)
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
	 */
	public void setInstitution(String institution)
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
	 */
	public void setResearchArea(String researchArea)
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
	 */
	public void setDepartment(String department)
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
	 */
	public void setCity(String city)
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
	 */
	public void setState(String state)
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
	 */
	public void setCountry(String country)
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
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			throw new ProfileException("Error producing JSON output.", e);
		}

		return output;
	}
	
	/**
	 * Parses and validates a JSON representation, updating the passed in
	 * Profile object. If null, a new Profile object is returned.
	 * 
	 * @param jsonProfile
	 * @return Profile object
	 * @throws ProfileException
	 */
	public static Profile fromJSON(JsonNode jsonProfile) 
	throws ProfileException
	{
		return fromJSON(jsonProfile, null);
	}
	
	/**
	 * Parses and validates a JSON representation, updating the passed in
	 * Profile object. If null, a new Profile object is returned.
	 * 
	 * @param jsonProfile
	 * @return Profile object
	 * @throws ProfileException
	 */
	public static Profile fromJSON(JsonNode jsonProfile, Profile profile) 
	throws ProfileException
	{
		if (profile == null) {
			profile = new Profile();
		}
		
		try 
		{
			if (ClientUtils.isNonEmptyString(jsonProfile.get("username"))) {
				profile.setUsername(jsonProfile.get("username").asText());
			} else {
				throw new ProfileException("Please specify a valid string value for the 'username' field.");
			}
			
			if (ClientUtils.isNonEmptyString(jsonProfile.get("email"))) {
				String email = jsonProfile.get("email").asText();
				if (ClientUtils.isValidEmailAddress(email)) {
					profile.setEmail(email);
				} else {
					throw new ProfileException("Invalid 'email' value given. " +
							"Please specify a valid 'email' value for this user.");
				}
				profile.setEmail(jsonProfile.get("email").asText());
			} else {
				throw new ProfileException("Please specify a valid 'email' value for this user.");
			}
			
			if (jsonProfile.has("firstName")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("firstName"))) {
					profile.setFirstName(jsonProfile.get("firstName").asText());
				} else {
					throw new ProfileException("Invalid 'firstName' value. " +
							"If specified, please specify a valid first name for this user.");
				}
			}
			
			if (jsonProfile.has("lastName")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("lastName"))) {
					profile.setLastName(jsonProfile.get("lastName").asText());
				} else {
					throw new ProfileException("Invalid 'lastName' value. " +
							"If specified, please specify a valid last name for this user.");
				}
			}
			
			if (jsonProfile.has("position")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("position"))) {
					profile.setPosition(jsonProfile.get("position").asText());
				} else {
					throw new ProfileException("Invalid 'position' value. " +
							"If specified, please specify a valid job position for this user.");
				}
			}
			if (jsonProfile.has("institution")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("institution"))) {
					profile.setInstitution(jsonProfile.get("institution").asText());
				} else {
					throw new ProfileException("Invalid 'institution' value. " +
							"If specified, please specify a valid institution for this user.");
				}
			}
			
			if (jsonProfile.has("phone")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("phone"))) {
					String phone = jsonProfile.get("phone").asText();
					if (ClientUtils.isValidPhoneNumber(phone))
						profile.setPhone(phone);
					else
						throw new ProfileException("Invalid 'phone' value. " +
								"If specified, please specify a valid phone number for this user " +
								"in ###-###-#### format.");
				} else {
					throw new ProfileException("Invalid 'phone' value. " +
							"If specified, please specify a valid phone number for this user" +
							"in ###-###-#### format.");
				}
			}
			
			if (jsonProfile.has("fax")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("fax"))) {
					String fax = jsonProfile.get("fax").asText();
					if (ClientUtils.isValidPhoneNumber(fax))
						profile.setFax(fax);
					else
						throw new ProfileException("Invalid 'fax' value. " +
								"If specified, please specify a valid fax number for this user " +
								"in ###-###-#### format.");
				} else {
					throw new ProfileException("Invalid 'fax' value. " +
							"If specified, please specify a valid fax number for this user" +
							"in ###-###-#### format.");
				}
			}
			
			if (jsonProfile.has("researchArea")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("researchArea"))) {
					profile.setResearchArea(jsonProfile.get("researchArea").asText());
				} else {
					throw new ProfileException("Invalid 'researchArea' value. " +
							"If specified, please specify a valid researchArea for this user.");
				}
			}
			
			if (jsonProfile.has("department")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("department"))) {
					profile.setDepartment(jsonProfile.get("department").asText());
				} else {
					throw new ProfileException("Invalid 'department' value. " +
							"If specified, please specify a valid department for this user.");
				}
			}
			
			if (jsonProfile.has("city")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("city"))) {
					profile.setCity(jsonProfile.get("city").asText());
				} else {
					throw new ProfileException("Invalid 'city' value. " +
							"If specified, please specify a valid city for this user.");
				}
			}
			
			if (jsonProfile.has("state")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("state"))) {
					profile.setState(jsonProfile.get("state").asText());
				} else {
					throw new ProfileException("Invalid 'state' value. " +
							"If specified, please specify a valid state or state abbreviation for this user.");
				}
			}
			
			if (jsonProfile.has("country")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("country"))) {
					profile.setCountry(jsonProfile.get("country").asText());
				} else {
					throw new ProfileException("Invalid 'country' value. " +
							"If specified, please specify a valid country or country code for this user.");
				}
			}
			
			if (jsonProfile.has("gender")) {
				if (ClientUtils.isNonEmptyString(jsonProfile.get("gender"))) {
					try {
						GenderType genderType = GenderType.valueOf(jsonProfile.get("gender").asText().toUpperCase());
						profile.setGender(genderType);
					} catch (Exception e) {
						throw new ProfileException("Invalid 'gender' provided. " +
								"If specified, 'gender' must be either MALE or FEMALE.");
					}
				} else {
					throw new ProfileException("Invalid 'gender' provided. " +
							"If specified, 'gender' must be either MALE or FEMALE.");
				}
			}
			
			return profile;
		} 
		catch (Exception e) {
			throw new ProfileException("Failed to parse profile. " + e.getMessage(), e);
		}
	}
}
