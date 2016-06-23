/**
 * 
 */
package org.iplantc.service.clients.model;

import java.util.Date;

import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.clients.exceptions.ProfileException;
import org.iplantc.service.clients.model.enumerations.GenderType;
import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * User-facing Profile object to hold information about users.
 * 
 * @author dooley
 *
 */
public class InternalUser extends Profile
{
	protected String 	createdBy;
	private boolean		active =  true;
	protected Date 		lastUpdated = new Date();
	protected Date 		created = new Date();
	
	public InternalUser() {}
	
	public InternalUser(String createdBy, String username, String email) {
		this.createdBy = createdBy;
		this.username = username;
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
	public long getId()
	{
		return id;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 * @throws ProfileArgumentException 
	 */
	public void setUsername(String username) throws ProfileException 
	{
		if (username.matches("(list|internal|active|inactive|profile|user)")) {
			throw new ProfileException("Username cannot be the reserve words 'list', 'internal', 'profile', 'user', 'inactive', or 'active.'");
		}
		
		if (!username.equals(username.replaceAll( "[^0-9a-zA-Z\\.\\-]" , ""))) {
			throw new ProfileException("Username may only contain alphanumeric characters, periods, and dashes.");
		}
		
		this.username = username;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}
	
	/**
	 * @return the active
	 */
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
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the phone
	 */
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
	public String getResearchArea() {
		return researchArea;
	}

	/**
	 * @param researchArea the researchArea to set
	 */
	public void setResearchArea(String researchArea) {
		this.researchArea = researchArea;
	}

	/**
	 * @return the position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 */
	public void setPosition(String position) {
		this.position = position;
	}

	/**
	 * @return the company
	 */
	public String getInstitution() {
		return institution;
	}

	/**
	 * @param company the institution to set
	 */
	public void setInstitution(String institution) {
		this.institution = institution;
	}

	/**
	 * @return the department
	 */
	public String getDepartment() {
		return department;
	}

	/**
	 * @param department the department to set
	 */
	public void setDepartment(String department) {
		this.department = department;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city)
	{
		this.city = city;
	}

	/**
	 * @return the city
	 */
	public String getCity()
	{
		return city;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @param gender the gender to set
	 */
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
	
	public Date getLastUpdated()
	{
		return this.lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	public Date getCreated()
	{
		return this.created;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public JSONObject toJSONProfile() {
		return new JSONObject(this);
	}
	
	public static InternalUser fromJSON(JsonNode jsonProfile) 
	throws ProfileException
	{
		InternalUser profile = new InternalUser();
		profile = (InternalUser)Profile.fromJSON(jsonProfile, profile);
		return profile;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "InternalUser [username=" + getUsername()
				+ ", createdBy=" + getCreatedBy() + "]";
	}
	
	
	@Override
	public String toJSON() throws ProfileException
	{
		try {
			JSONObject jprofile = new JSONObject(super.toJSON());
			jprofile.put("createdBy", createdBy);
			return jprofile.toString();
		} catch (Exception e) {
			throw new ProfileException("Error producing JSON output.", e);
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
	
}
