/**
 *
 */
package org.iplantc.service.profile.model;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.enumeration.GenderType;
import org.iplantc.service.profile.util.ServiceUtils;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * User-facing Profile object to hold information about users.
 *
 * @author dooley
 *
 */
@Entity
@Table(name = "internalusers", uniqueConstraints=
	@UniqueConstraint(columnNames={"username", "created_by", "tenant_id"}))
@JsonSerialize(using=InternalUserSerializer.class)
@FilterDef(name="internalUserTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="internalUserTenantFilter", condition="tenant_id=:tenantId"))
public class InternalUser extends Profile
{
	protected String 	createdBy;
	protected boolean	active =  true;
	protected String 	tenantId;			// tenant to which this entity belongs
	protected Date 		lastUpdated = new Date();
	protected Date 		created = new Date();
	protected String	street1;
	protected String	street2;

	public InternalUser() {
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.uuid = new AgaveUUID(UUIDType.INTERNALUSER).toString();
	}

	public InternalUser(String apiUsername, String internalUsername,
			String email)
	{
		this();
		this.createdBy = apiUsername;
		this.username = internalUsername;
		this.email = email;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id)
	{
		this.id = id;
	}

	/**
	 * @return the id
	 */
	@JsonIgnore
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public long getId()
	{
		return id;
	}

	/**
	 * @return the username
	 */
	@NotNull
//	@Pattern(regexp="[^0-9a-zA-Z\\.\\-]+")
//	@Size(min=3, max=32)
	@Column(name = "username", nullable = false, length = 32)
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 * @throws ProfileArgumentException
	 */
	public void setUsername(String username) throws ProfileArgumentException
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new ProfileArgumentException("username must be 32 characters or less");
		}

		if (username.matches("(list|internal|active|inactive|profile|user)")) {
			throw new ProfileArgumentException("username cannot be the reserve words 'list', 'internal', 'profile', 'user', 'inactive', or 'active.'");
		}

		if (!username.equals(username.replaceAll( "[^0-9a-zA-Z\\.\\-]" , ""))) {
			throw new ProfileArgumentException("username may only contain alphanumeric characters, periods, and dashes.");
		}

		this.username = username;
	}

	/**
	 * @return the createdBy
	 */
//	@NotNull
//	@Size(min=3, max=32)
	@Column(name = "created_by", nullable = false, length = 32)
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 * @throws ProfileArgumentException
	 */
	public void setCreatedBy(String createdBy) throws ProfileArgumentException
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new ProfileArgumentException("username must be less than 32 characters.");
		}

		this.createdBy = createdBy;
	}

	/**
	 * @return the active
	 */
//	@NotNull
	@Column(name = "currently_active", columnDefinition = "TINYINT(1)")
	public boolean isActive()
	{
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active)
	{
		this.active = active;
	}

	/**
	 * @return the firstName
	 */
	@Size(max=32)
	@Column(name = "first_name", length = 32)
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 * @throws ProfileArgumentException
	 */
	public void setFirstName(String firstName) throws ProfileArgumentException {
		if (firstName != null && firstName.length() > 32) {
			throw new ProfileArgumentException("firstName must be less than 32 characters.");
		}
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
//	@Size(max=32, message="lastName must be less than 32 characters.")
	@Column(name = "last_name", length = 32)
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 * @throws ProfileArgumentException
	 */
	public void setLastName(String lastName) throws ProfileArgumentException {
//		if (lastName != null && lastName.length() > 32) {
//			throw new ProfileArgumentException("lastName must be less than 32 characters.");
//		}
		this.lastName = lastName;
	}

	/**
	 * @return the email
	 */
//	@Email
//	@Size(max=128, message="email must be less than 32 characters.")
	@Column(name = "email", length = 128)
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 * @throws ProfileArgumentException
	 */
	public void setEmail(String email) throws ProfileArgumentException {
//		if (email != null && email.length() > 32) {
//			throw new ProfileArgumentException("email must be less than 32 characters.");
//		}
		this.email = email;
	}

	/**
	 * @return the phone
	 */
//	@Size(min=7, max=15, message="Phone number must be between 7 and 15 characters in length.")
//	@Pattern(regexp="^(?=.{7,32}$)(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*((\\s?x\\s?|ext\\s?|extension\\s?)\\d{1,5}){0,1}$",
//			message="Phone number is badly formatted")
	@Column(name = "phone", length = 15)
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = ServiceUtils.formatPhoneNumber(phone);
	}

	/**
	 * @return the fax
	 */
//	@Size(min=7, max=15, message="fax must be between 7 and 15 characters in length.")
//	@Pattern(regexp="^(?=.{7,32}$)(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*((\\s?x\\s?|ext\\s?|extension\\s?)\\d{1,5}){0,1}$",
//			message="fax is badly formatted")
	@Column(name = "fax", length = 32)
	public String getFax() {
		return fax;
	}

	/**
	 * @param fax the fax to set
	 */
	public void setFax(String fax) {
		this.fax = ServiceUtils.formatPhoneNumber(fax);
	}

	/**
	 * @return the researchArea
	 */
//	@Size(max=64, message="researchArea must be less than 64 characters.")
	@Column(name = "research_area", length = 64)
	public String getResearchArea() {
		return researchArea;
	}

	/**
	 * @param researchArea the researchArea to set
	 * @throws ProfileArgumentException
	 */
	public void setResearchArea(String researchArea) throws ProfileArgumentException {
		if (researchArea != null && researchArea.length() > 64) {
			throw new ProfileArgumentException("researchArea must be less than 64 characters.");
		}
		this.researchArea = researchArea;
	}

	/**
	 * @return the position
	 */
//	@Size(max=32, message="position must be less than 32 characters.")
	@Column(name = "position", length = 32)
	public String getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 * @throws ProfileArgumentException
	 */
	public void setPosition(String position) throws ProfileArgumentException {
		if (position != null && position.length() > 32) {
			throw new ProfileArgumentException("position must be less than 32 characters.");
		}
		this.position = position;
	}

	/**
	 * @return the company
	 */
//	@Size(max=64, message="institution must be less than 64 characters.")
	@Column(name = "institution", length = 64)
	public String getInstitution() {
		return institution;
	}

	/**
	 * @param company the institution to set
	 * @throws ProfileArgumentException
	 */
	public void setInstitution(String institution) throws ProfileArgumentException {
		if (institution != null && institution.length() > 64) {
			throw new ProfileArgumentException("institution must be less than 64 characters.");
		}
		this.institution = institution;
	}

	/**
	 * @return the department
	 */
//	@Size(max=64, message="department must be less than 64 characters.")
	@Column(name = "department", length = 64)
	public String getDepartment() {
		return department;
	}

	/**
	 * @param department the department to set
	 * @throws ProfileArgumentException
	 */
	public void setDepartment(String department) throws ProfileArgumentException {
		if (department != null && department.length() > 64) {
			throw new ProfileArgumentException("department must be less than 64 characters.");
		}
		this.department = department;
	}

	/**
	 * @return the street1
	 */
	@Column(name = "street1", length = 64)
	public String getStreet1() {
		return street1;
	}

	/**
	 * @param street1 the street1 to set
	 */
	public void setStreet1(String street1) throws ProfileArgumentException {
		if (StringUtils.length(street1) > 64) {
			throw new ProfileArgumentException("street1 must be less than 64 characters.");
		}
		this.street1 = street1;
	}

	/**
	 * @return the street2
	 */
	public String getStreet2() {
		return street2;
	}

	/**
	 * @param street2 the street2 to set
	 */
	@Column(name = "street2", length = 64)
	public void setStreet2(String street2) throws ProfileArgumentException {
		if (StringUtils.length(street2) > 64) {
			throw new ProfileArgumentException("street2 must be less than 64 characters.");
		}
		this.street2 = street2;
	}

	/**
	 * @return the city
	 * @throws ProfileArgumentException
	 */
//	@Size(max=32, message="city must be less than 32 characters.")
	@Column(name = "city", length = 32)
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
		if (city != null && city.length() > 32) {
			throw new ProfileArgumentException("city must be less than 32 characters.");
		}
		this.city = city;
	}

	/**
	 * @return the state
	 */
//	@Size(max=32, message="state must be less than 32 characters.")
	@Column(name = "state", length = 32)
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 * @throws ProfileArgumentException
	 */
	public void setState(String state) throws ProfileArgumentException {
		if (state != null && state.length() > 32) {
			throw new ProfileArgumentException("state must be less than 32 characters.");
		}
		this.state = state;
	}

	/**
	 * @return the country
	 */
//	@Size(max=32, message="country must be less than 32 characters.")
	@Column(name = "country", length = 32)
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 * @throws ProfileArgumentException
	 */
	public void setCountry(String country) throws ProfileArgumentException {
		if (country != null && country.length() > 32) {
			throw new ProfileArgumentException("country must be less than 32 characters.");
		}
		this.country = country;
	}

	/**
	 * @param gender the gender to set
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "gender", length = 6)
	public void setGender(GenderType gender)
	{
		this.gender = gender;
	}

	/**
	 * @return the gender
	 */
	public GenderType getGender()
	{
		return gender;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return this.lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return this.created;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	/**
	 * @return the uuid
	 */
//	@Size(max=32, message="uuid must be less than 64 characters.")
	@Column(name = "uuid", nullable = false, length = 64, unique = true)
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * @return the tenantId
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	public JSONObject toJSONProfile() {
		return new JSONObject(this);
	}

	/**
	 * Parses and validates a JSON representation into a new Profile object.
	 *
	 * @param jsonProfile
	 * @return Profile object
	 * @throws ProfileArgumentException
	 */
	public static InternalUser fromJSON(JSONObject jsonProfile)
	throws ProfileException, ProfileArgumentException
	{
		return fromJSON(jsonProfile, null);
	}

	/**
	 * Parses and validates a JSON representation, updating the passed in
	 * Profile object. If null, a new Profile object is returned.
	 *
	 * @param jsonProfile
	 * @return Profile object
	 * @throws ProfileArgumentException
	 */
	public static InternalUser fromJSON(JSONObject jsonProfile, InternalUser profile)
	throws ProfileException, ProfileArgumentException
	{
		if (profile == null) {
			profile = new InternalUser();
		}
		try
		{
			if (ServiceUtils.isNonEmptyString(jsonProfile, "username")) {
				profile.setUsername(jsonProfile.getString("username"));
			} else {
				throw new ProfileArgumentException("Please specify a valid string value for the 'username' field.");
			}

			if (ServiceUtils.isNonEmptyString(jsonProfile, "email")) {
				String email = jsonProfile.getString("email");
				if (ServiceUtils.isValidEmailAddress(email)) {
					profile.setEmail(email);
				} else {
					throw new ProfileArgumentException("Invalid 'email' value given. " +
							"Please specify a valid 'email' value for this user.");
				}
				profile.setEmail(jsonProfile.getString("email"));
			} else {
				throw new ProfileArgumentException("Please specify a valid 'email' value for this user.");
			}

			if (jsonProfile.has("firstName")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "firstName")) {
					profile.setFirstName(jsonProfile.getString("firstName"));
				} else {
					throw new ProfileArgumentException("Invalid 'firstName' value. " +
							"If specified, please specify a valid first name for this user.");
				}
			} else {
				profile.setFirstName(null);
			}

			if (jsonProfile.has("lastName")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "lastName")) {
					profile.setLastName(jsonProfile.getString("lastName"));
				} else {
					throw new ProfileArgumentException("Invalid 'lastName' value. " +
							"If specified, please specify a valid last name for this user.");
				}
			} else {
				profile.setLastName(null);
			}

			if (jsonProfile.has("position")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "position")) {
					profile.setPosition(jsonProfile.getString("position"));
				} else {
					throw new ProfileArgumentException("Invalid 'position' value. " +
							"If specified, please specify a valid job position for this user.");
				}
			} else {
				profile.setPosition(null);
			}

			if (jsonProfile.has("institution")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "institution")) {
					profile.setInstitution(jsonProfile.getString("institution"));
				} else {
					throw new ProfileArgumentException("Invalid 'institution' value. " +
							"If specified, please specify a valid institution for this user.");
				}
			} else {
				profile.setInstitution(null);
			}

			if (jsonProfile.has("phone")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "phone")) {
					String phone = jsonProfile.getString("phone");
					if (ServiceUtils.isValidPhoneNumber(phone))
						profile.setPhone(phone);
					else
						throw new ProfileArgumentException("Invalid 'phone' value. " +
								"If specified, please specify a valid phone number for this user " +
								"in ###-###-#### format.");
				} else {
					throw new ProfileArgumentException("Invalid 'phone' value. " +
							"If specified, please specify a valid phone number for this user" +
							"in ###-###-#### format.");
				}
			}
			else {
				profile.setPhone(null);
			}

			if (jsonProfile.has("fax")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "fax")) {
					String fax = jsonProfile.getString("fax");
					if (ServiceUtils.isValidPhoneNumber(fax))
						profile.setFax(fax);
					else
						throw new ProfileArgumentException("Invalid 'fax' value. " +
								"If specified, please specify a valid fax number for this user " +
								"in ###-###-#### format.");
				} else {
					throw new ProfileArgumentException("Invalid 'fax' value. " +
							"If specified, please specify a valid fax number for this user" +
							"in ###-###-#### format.");
				}
			}
			else {
				profile.setFax(null);
			}

			if (jsonProfile.has("researchArea")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "researchArea")) {
					profile.setResearchArea(jsonProfile.getString("researchArea"));
				} else {
					throw new ProfileArgumentException("Invalid 'researchArea' value. " +
							"If specified, please specify a valid researchArea for this user.");
				}
			}
			else {
				profile.setResearchArea(null);
			}

//			profile.fundingAgencies = new HashSet<String>();
//			if (jsonProfile.has("fundingAgencies")) {
//				try {
//					JSONArray agencies = jsonProfile.getJSONArray("fundingAgencies");
//					for (int i=0; i< agencies.length(); i++) {
//						profile.fundingAgencies.add(agencies.getString(i));
//					}
//				} catch (Exception e) {
//					throw new ProfileArgumentException("Invalid 'fundingAgencies' value. " +
//							"If specified, please specify a valid array of funding agency " +
//							"names for this user.");
//				}
//			}

			if (jsonProfile.has("department")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "department")) {
					profile.setDepartment(jsonProfile.getString("department"));
				} else {
					throw new ProfileArgumentException("Invalid 'department' value. " +
							"If specified, please specify a valid department for this user.");
				}
			}
			else {
				profile.setDepartment(null);
			}

			if (jsonProfile.has("street1")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "street1")) {
					profile.setStreet1(jsonProfile.getString("street1"));
				} else {
					throw new ProfileArgumentException("Invalid 'street1' value. " +
							"If specified, please specify a valid street1 for this user.");
				}
			}
			else {
				profile.setStreet1(null);
			}

			if (jsonProfile.has("street2")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "street2")) {
					profile.setStreet2(jsonProfile.getString("street2"));
				} else {
					throw new ProfileArgumentException("Invalid 'street2' value. " +
							"If specified, please specify a valid street2 for this user.");
				}
			}
			else {
				profile.setStreet2(null);
			}

			if (jsonProfile.has("city")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "city")) {
					profile.setCity(jsonProfile.getString("city"));
				} else {
					throw new ProfileArgumentException("Invalid 'city' value. " +
							"If specified, please specify a valid city for this user.");
				}
			}
			else {
				profile.setCity(null);
			}

			if (jsonProfile.has("state")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "state")) {
					profile.setState(jsonProfile.getString("state"));
				} else {
					throw new ProfileArgumentException("Invalid 'state' value. " +
							"If specified, please specify a valid state or state abbreviation for this user.");
				}
			}
			else {
				profile.setState(null);
			}

			if (jsonProfile.has("country")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "country")) {
					profile.setCountry(jsonProfile.getString("country"));
				} else {
					throw new ProfileArgumentException("Invalid 'country' value. " +
							"If specified, please specify a valid country or country code for this user.");
				}
			}
			else {
				profile.setCountry(null);
			}

			if (jsonProfile.has("gender")) {
				if (ServiceUtils.isNonEmptyString(jsonProfile, "gender")) {
					try {
						GenderType genderType = GenderType.valueOf(jsonProfile.getString("gender").toUpperCase());
						profile.setGender(genderType);
					} catch (Exception e) {
						throw new ProfileArgumentException("Invalid 'gender' provided. " +
								"If specified, 'gender' must be either MALE or FEMALE.");
					}
				} else {
					throw new ProfileArgumentException("Invalid 'gender' provided. " +
							"If specified, 'gender' must be either MALE or FEMALE.");
				}
			}
			else {
				profile.setGender(null);
			}

			return profile;
		}
		catch (ProfileArgumentException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ProfileException("Failed to parse profile. " + e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getUsername() + "<" + getEmail() + ">"
				+ ", createdBy " + getCreatedBy();
	}


	@Override
	public String toJSON() throws ProfileException
	{
		JSONWriter writer = new JSONStringer();

		try {
			writer.object()
				.key("uuid").value(getUuid())
				.key("active").value(isActive())
				.key("createdBy").value(getCreatedBy())
				.key("city").value(city)
				.key("country").value(country)
				.key("department").value(department)
				.key("email").value(email)
				.key("fax").value(fax)
				.key("firstName").value(getFirstName())
				.key("gender").value(gender)
				.key("institution").value(institution)
				.key("lastName").value(getLastName())
				.key("phone").value(phone)
				.key("position").value(position)
				.key("researchArea").value(researchArea)
				.key("state").value(state)
				.key("street1").value(street1)
				.key("street2").value(street2)
				.key("username").value(getUsername())

				.key("_links").object()
		        	.key("self").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getCreatedBy() + "/" + getUuid())
			        .endObject()
					.key("profile").object()
			        	.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getCreatedBy())
				    .endObject()
				    .key("metadata").object()
		    			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/?q=" + URLEncoder.encode("{\"associationIds\":\"" + uuid + "\"}"))
		    		.endObject()
				.endObject()
	        .endObject();

			return writer.toString();
		} catch (Exception e) {
			throw new ProfileException("Error formatting profile response.", e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( username == null ) ? 0 : username.hashCode() );
		result = prime * result
				+ ( ( createdBy == null ) ? 0 : createdBy.hashCode() );
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InternalUser other = (InternalUser) obj;
		if (username == null)
		{
			if (other.username != null)
				return false;
		}
		else if (!username.equals(other.username))
			return false;
		if (createdBy == null)
		{
			if (other.createdBy != null)
				return false;
		}
		else if (!createdBy.equals(other.createdBy))
			return false;
		return true;
	}


	public InternalUser clone()
	{
		InternalUser clonedInternalUser = new InternalUser();
		try
		{
			clonedInternalUser.setActive(active);
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

class InternalUserSerializer extends JsonSerializer<InternalUser> {
    @Override
    public void serialize(InternalUser internalUser, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {

    	try
		{
			jsonGenerator.writeRaw(internalUser.toJSON());
		}
		catch (ProfileException e)
		{
			throw new IOException(e);
		}
//
//        jsonGenerator.writeStartObject();
//
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        jsonGenerator.writeStringField("username", user.getUsername());
//        HALObject hal = new HALObject();
//        hal.add("self", Settings.IPLANT_PROFILE_SERVICE + internalUser.getCreatedBy() + "/users/" + internalUser.getUsername());
//        hal.add("profile", Settings.IPLANT_PROFILE_SERVICE + internalUser.getCreatedBy());
//
//        jsonGenerator.writeObject(hal);
//
//
//        .put("active", active)
//		.put("createdBy", createdBy)
//		.put("uuid", uuid)
//		.put("_links", new JSONObject()
//			.put("self", new JSONObject()
//				.put("href", Settings.IPLANT_PROFILE_SERVICE + this.createdBy + "/users/" + this.username)
//			)
//			.put("profile", new JSONObject()
//				.put("href", Settings.IPLANT_PROFILE_SERVICE + this.createdBy)
//			)
//        jsonGenerator.writeEndObject();
    }
}
