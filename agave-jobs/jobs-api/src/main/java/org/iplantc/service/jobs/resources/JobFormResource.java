/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.enumerations.ExecutionType;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Generates a HTML form to submit a job for an app.
 * 
 * @author dooley
 * 
 */
public class JobFormResource extends AbstractJobResource 
{
	private String softwareName; // unique name of the app

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobFormResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
			
		softwareName = (String) request.getAttributes().get("name");

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobSubmissionForm.name(), 
				username, "", request.getClientInfo().getUpstreamAddress());

	}

	/**
	 * This method represents the HTTP GET action. Without specifying a job
	 * handle, there is no job information to retrieve, so we bind this action
	 * to simply serving a static HTML form for job submission.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		if (!ServiceUtils.isValid(softwareName))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Software name cannot be empty");
		} 
		else if (!softwareName.contains("-") || softwareName.endsWith("-")) 
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid software name. " +
							"Please specify an application using its unique id. " +
							"The unique id is defined by the application name " +
							"and version separated by a hyphen. eg. example-1.0");
		}
		
		Software software = SoftwareDao.getSoftwareByUniqueName(softwareName.trim());
		try {
			if (!ApplicationManager.isInvokableByUser(software, username)) {
				throw new SoftwareException("User does not have permission to access this application");
			}
		} catch (SoftwareException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new IplantErrorRepresentation(e.getMessage());
		}
		
		try 
		{
			String submitFormHeader = "<form name=\"" + software.getUniqueName() + "\" " +
								"method=\"POST\" " +
								"action=\"" + Settings.IPLANT_JOB_SERVICE + "\" " +
								"class=\"job_submission_form\"> ";
			
			String submitForm = "<table align=\"center\" id=\"contactArea\">\n";
			submitForm += "<tr><td style=\"text-align:center;\" colspan=\"2\">Base Parameters</td></tr>\n";
			submitForm += "<tr><td>Job Name:</td><td><input type=\"text\" name=\"jobName\" value=\"\"></td></tr>\n";
			submitForm += "<tr><td>Software Name:</td><td><input type=\"text\" name=\"softwareName\" value=\"" + software.getUniqueName() + "\"></td></tr>\n";
			if (software.getExecutionSystem().getExecutionType().equals(ExecutionType.HPC) ||
					software.getExecutionSystem().getExecutionType().equals(ExecutionType.CONDOR)) {
				submitForm += "<tr><td>Batch Queue:</td><td><select name=\"batchQueue\" >";
					for(BatchQueue queue: software.getExecutionSystem().getBatchQueues()) {
						submitForm += "<option value=\"" + queue.getName() + "\">" + queue.getName() + "</value>";
					}
				submitForm += "</select></td></tr>\n";
			}
			if (software.getParallelism().equals(ParallelismType.PARALLEL))
			{
				long nodeCount = 1;
				if (software.getDefaultNodes() != null && software.getDefaultNodes() > 0) {
					nodeCount = software.getDefaultNodes();
				}
				submitForm += "<tr><td>Nodes:</td><td><input type=\"text\" name=\"nodeCount\" value=\"" + nodeCount + "\"></td></tr>\n";
			} 
			else
			{
				submitForm = "<input type=\"hidden\" name=\"nodeCount\" value=\"1\">\n" + submitForm;
			}
			
			long processorsPerNode = 1;
			if (software.getDefaultProcessorsPerNode() != null && software.getDefaultProcessorsPerNode() > 0) {
				processorsPerNode = software.getDefaultProcessorsPerNode();
			}
			submitForm += "<tr><td>Processors Per Node:</td><td><input type=\"text\" name=\"processorsPerNode\" value=\"" + processorsPerNode + "\" title=\"Enter a positive integer value\"></td></tr>\n";
			
			String memoryPerNode = "2GB";
			if (software.getDefaultMemoryPerNode() != null && software.getDefaultMemoryPerNode() > 0) {
				memoryPerNode = BatchQueue.formatMaxMemoryPerNode(software.getDefaultMemoryPerNode());
			}
			submitForm += "<tr><td>Memory Per Node:</td><td><input type=\"text\" name=\"memoryPerNode\" value=\"" + memoryPerNode + "\" title=\"Enter a positive integer value\"></td></tr>\n";
			
			String maxRunTime = "04:00:00";
			if (!StringUtils.isEmpty(software.getDefaultMaxRunTime())) {
				maxRunTime = software.getDefaultMaxRunTime();
			}
			submitForm += "<tr><td>Requested Time:</td><td><input type=\"text\" name=\"requestedTime\" value=\"" + maxRunTime + "\" title=\"Enter in hh:mm:ss format\"></td></tr>\n";
			
			submitForm += "<tr><td>Notification Url:</td><td><input type=\"text\" name=\"notifications\" value=\"\"></td></tr>\n";
			submitForm += "<tr><td>Archive:</td><td><input type=\"checkbox\" value=\"1\" name=\"archive\" checked></td></tr>\n";
			submitForm += "<tr><td>Archive System:</td><td><select name=\"archiveSystem\" >";
					for(RemoteSystem storageSystem: new SystemDao().getUserSystems(getAuthenticatedUsername(), true, RemoteSystemType.STORAGE)) {
						submitForm += "<option value=\"" + storageSystem.getSystemId() + "\">" + StringEscapeUtils.escapeHtml(storageSystem.getName()) + "</value>";
					}
			submitForm += "</select></td></tr>\n";
			
			submitForm += "<tr><td>Archive Path:</td><td><input type=\"input\" name=\"archivePath\" value=\"\"></td></tr>\n";
			
			submitForm += "<tr><td style=\"text-align:center;\" colspan=\"2\">Input Files</td></tr>";
			for (SoftwareInput input : software.getInputs())
			{
				if (!input.isVisible()) continue;
				submitForm += "<tr><td>"
						+ WordUtils.capitalizeFully(input.getKey())
						+ ": </td><td><input type=\"text\" name=\"" + input.getKey()
						+ "\" value=\"" + 
						(input.getDefaultValueAsJsonArray().iterator().next().asText() == null ? 
								"" : input.getDefaultValueAsJsonArray().iterator().next().asText()) 
						+ "\" title=\"" + input.getDescription() + "\"></td></tr>\n";
			}
	
			submitForm += "<tr><td style=\"text-align:center;\" colspan=\"2\">Input Parameters</td></tr>\n";
			for (SoftwareParameter param : software.getParameters())
			{
				if (!param.isVisible()) continue;
				if (param.getType().equals(SoftwareParameterType.enumeration))
				{
					// get the defaults so we can mark them as selected
					List<String> defaults = new ArrayList<String>();
					for (Iterator<JsonNode> iter = param.getDefaultValueAsJsonArray().iterator(); iter.hasNext();)
					{
						JsonNode child = iter.next();
						defaults.add(child.fieldNames().next());
					}
					
					// select box
					submitForm += "<tr><td>"
						+ WordUtils.capitalizeFully(param.getLabel())
						+ ": </td><td><select name=\"" + param.getKey() + "\" " 
						+ (defaults.size() > 1 ? "multiple=\"multiple\"" : "") + ">";
					
					ArrayNode enumeratedJsonArray = param.getEnumeratedValues();
					for (Iterator<JsonNode> iter = enumeratedJsonArray.iterator(); iter.hasNext();)
					{
						JsonNode child = iter.next();
						String key = child.fieldNames().next();
						submitForm += "<option value=\"" + key + "\"" + 
							(defaults.contains(key) ? "selected" : "" ) + 
							">" + child.get(key).asText() + "</option>";
					}
					submitForm += "</select></td></tr>\n";
					
				} 
				else if (param.getType().equals(SoftwareParameterType.bool) || 
						param.getType().equals(SoftwareParameterType.flag))
				{
					ArrayNode defaultJson = param.getDefaultValueAsJsonArray();
					boolean defaultTrue = defaultJson.iterator().next().asBoolean(false);
					
					submitForm += "<tr><td>"
						+ WordUtils.capitalizeFully(param.getLabel())
						+ ": </td><td><select name=\"" + param.getKey() + "\">\n" 
						+ "<option value=\"1\""+ (defaultTrue ? " selected=\"selected\"" : "") + ">True</option>\n"
						+ "<option value=\"0\""+ (!defaultTrue ? " selected=\"selected\"" : "") + ">False</option>\n"
						+ "</select></td></tr>\n";
				}
				else
				{
					ArrayNode defaultJson = param.getDefaultValueAsJsonArray();
					String defaultValue = "";
					if (defaultJson.iterator().hasNext()) {
						defaultValue = defaultJson.iterator().next().textValue();
					}
					
					submitForm += "<tr><td>"
						+ WordUtils.capitalizeFully(param.getLabel())
						+ ": </td><td><input type=\"text\" name=\"" + param.getKey()
						+ "\" value=\"" + defaultValue + "\" title=\"" + param.getDescription() + "\"></td></tr>\n";
				}
			}
			submitForm += "<tr><td style=\"text-align:center;\" colspan=\"2\"><input type=\"submit\" name=\"submit\" value=\"Submit\"/></td></tr>\n";
			submitForm += "</table></form>";
			JSONWriter writer = new JSONStringer();
			writer.object().key("submitForm").value(submitFormHeader + submitForm).endObject();
			return new IplantSuccessRepresentation(writer.toString());
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(
					"There was an error generating the job submission form for " + software.getUniqueName());
		}
	}
}
