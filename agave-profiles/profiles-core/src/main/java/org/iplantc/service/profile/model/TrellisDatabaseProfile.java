package org.iplantc.service.profile.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.iplantc.service.profile.model.enumeration.GenderType;

public class TrellisDatabaseProfile extends Profile {

	public TrellisDatabaseProfile(ResultSet rs) throws SQLException, ProfileArgumentException
	{
		this.setId(rs.getInt("id"));
    	this.setInstitution(rs.getString("institution"));
    	this.setCountry(rs.getString("country"));
    	this.setDepartment(rs.getString("department"));
    	this.setEmail(rs.getString("email"));
    	this.setFax(rs.getString("fax"));
    	this.setFirstName(rs.getString("first_name"));
    	//this.addFundingAgency(rs.getString("funding_agency"));
    	this.setCity(rs.getString("city"));
    	this.setLastName(rs.getString("last_name"));
    	this.setPhone(rs.getString("phone"));
    	this.setPosition(rs.getString("position"));
    	this.setResearchArea(rs.getString("research_area"));
    	this.setState(rs.getString("state"));
    	this.setUsername(rs.getString("username"));
    	this.setGender(GenderType.valueOf(rs.getString("gender").toUpperCase()));
	}

}
