package org.iplantc.service.profile.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("profile")
public class TrellisProfile extends Profile{

	public TrellisProfile(JSONObject profile) throws JSONException {
		if (profile.has("username")) {
			this.username = profile.getString("username");
		}
		if (profile.has("firstname")) {
			this.firstName = profile.getString("firstname");
		}
		if (profile.has("lastname")) {
			this.lastName = profile.getString("lastname");
		}
		if (profile.has("email")) {
			this.email = profile.getString("email");
		}
		
		if (profile.has("position")) {
			this.position = profile.getString("position");
		}
		if (profile.has("institution")) {
			this.institution = profile.getString("institution");
		}
	}
}
