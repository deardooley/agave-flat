package org.iplantc.service.clients;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.clients.beans.RemoteFile;
import org.iplantc.service.clients.beans.RemoteFilePermission;
import org.iplantc.service.clients.exceptions.APIClientException;
import org.iplantc.service.clients.exceptions.AuthenticationException;
import org.iplantc.service.clients.exceptions.FileTransferException;
import org.iplantc.service.clients.exceptions.ProfileException;
import org.iplantc.service.clients.exceptions.RemoteFileException;
import org.iplantc.service.clients.model.AuthenticationToken;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.profile.exceptions.RemoteDataException;

import com.fasterxml.jackson.databind.JsonNode;

public class IOService extends AbstractService 
{
	private AuthenticationToken token;
	
	public IOService(String baseUrl, AuthenticationToken token)
	{
		setToken(token);
	}
	
	/**
	 * @return the token
	 */
	public AuthenticationToken getToken()
	{
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(AuthenticationToken token)
	{
		this.token = token;
	}

	public List<RemoteFile> list(String path) 
	throws PermissionException, RemoteFileException, AuthenticationException
	{
		if (isEmpty(path)) {
			path = "";
		}
		
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		
		if (isTokenValid(token)) 
		{
			throw new AuthenticationException("Token is expired");
		} 
		else 
		{
			APIResponse response = null;
			try 
			{
				response = get( Settings.SERVICE_BASE_URL + "io-v1/io/list" + path, token );
			} 
			catch (APIClientException e) {
				throw new RemoteFileException(e);
			}
			
			if (response.isSuccess()) {
				return parseFileList(response.getResult());
			} else {
				throw new RemoteFileException(response.getMessage());
			}
		}	
	}

	public RemoteFile upload(String remoteUrl, String path, String format)
			throws Exception
	{

		String uploadUrl = ( ServiceUtils.isValid(path) ? baseUrl + path : baseUrl );

		try
		{			
			List <NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("format", format));
			
			APIResponse response = post(remoteUrl, token, nvps, new File(path));
			
			if (response.isSuccess()) {
				return parseFileList(response.getResult()).get(0);
			}
			else
			{
				throw new FileTransferException("Failed to upload "
						+ remoteUrl + " to " + uploadUrl + ": "
						+ response.getMessage());
			}
		}
		catch (FileTransferException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new FileTransferException("Failed to upload remote file "
					+ remoteUrl + " to " + uploadUrl, e);
		}
	}

	public boolean hasPermission(String path, String username, String permission)
	throws Exception
	{
		String pemUrl = baseUrl + "io-v1/io/share" + path;
		try
		{
			APIResponse response = get(pemUrl, token);

			if (response.isSuccess()) 
			{	
				List<RemoteFilePermission> pems = parseFilePermissions(response.getResult());
				
				for(RemoteFilePermission pem : pems) {
					if (pem.getUsername().equals(username) && 
							pem.hasPermission(permission)) {
						return true;
					}
				}
				
				return false;
			} 
			else
			{
				throw new RemoteDataException(response.getMessage());
			}
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve output from " + pemUrl, e);
		}
	}

	public void delete()
	{
		// TODO Auto-generated method stub

	}
	
	/**
	 * Parses the repsonse json from the IO service into a list of RemoteFileInfo objects.
	 * 
	 * @param response JsonNode object containing the service result attribute value.
	 * @return List of RemoteFileInfo objects
	 * @throws ProfileException 
	 * @throws RemoteFileException 
	 */
	private List<RemoteFile> parseFileList(JsonNode response) throws RemoteFileException
	{
		List<RemoteFile> files = new ArrayList<RemoteFile>();
		
		if (response == null) {
			return files;
		} 
		else 
		{
			if (response.isArray()) {
				for(int i=0; i<response.size(); i++) {
					JsonNode jsonFile = response.get(i);
					files.add(RemoteFile.fromJSON(jsonFile));
				}
			} else {
				files.add(RemoteFile.fromJSON(response));
			}
			
			return files;
		}
	}
	
	/**
	 * Parses the repsonse json from the IO service into a list of RemoteFileInfo objects.
	 * 
	 * @param response JsonNode object containing the service result attribute value.
	 * @return List of RemoteFileInfo objects
	 * @throws ProfileException 
	 * @throws RemoteFileException 
	 */
	private List<RemoteFilePermission> parseFilePermissions(JsonNode response) throws RemoteFileException
	{
		List<RemoteFilePermission> pems = new ArrayList<RemoteFilePermission>();
		
		if (response == null) {
			return pems;
		} 
		else 
		{
			if (response.isArray()) {
				for(int i=0; i<response.size(); i++) {
					JsonNode jsonPem = response.get(i);
					pems.add(RemoteFilePermission.fromJSON(jsonPem));
				}
			} else {
				pems.add(RemoteFilePermission.fromJSON(response));
			}
			
			return pems;
		}
	}

}
