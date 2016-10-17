/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.enumerations.ExecutionType;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.resources.SoftwareFormResource;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Path("/{softwareId}/form")
public class SoftwareFormResourceImpl extends AbstractSoftwareResource implements SoftwareFormResource {
    @GET
	public Response getNotification(@PathParam("softwareId") String softwareId) {
        logUsage(AgaveLogServiceClient.ActivityKeys.JobSubmissionForm);
        
        
        try 
        {
            Software software = getSoftwareFromPathValue(softwareId);
            
            if (!ApplicationManager.isVisibleByUser(software, getAuthenticatedUsername()))
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "Permission denied. You do not have permission to view this application");
            } 
            else if (software.isPubliclyAvailable() && !software.isAvailable()) 
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "This application has been removed by the administrator.");
            }
            
            String submitFormHeader = "<form name=\"" + software.getUniqueName() + "\" " +
                                "method=\"POST\" " +
                                "action=\"" + TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + "\"" +
                                "class=\"job_submission_form\">";
            
            String submitForm = "<table align=\"center\" id=\"contactArea\">\n";
            submitForm += "<tr><td style=\"text-align:center;\" colspan=\"2\">Base Parameters</td></tr>\n";
            submitForm += "<tr><td>Job Name:</td><td><input type=\"text\" name=\"name\" value=\"\"></td></tr>\n";
            submitForm += "<tr><td>Software Name:</td><td><input type=\"text\" name=\"appId\" value=\"" + software.getUniqueName() + "\"></td></tr>\n";
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
            submitForm += "<tr><td>Requested Time:</td><td><input type=\"text\" name=\"maxRunTime\" value=\"" + maxRunTime + "\" title=\"Enter in hh:mm:ss format\"></td></tr>\n";
            
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
                ArrayNode defaultValues = input.getDefaultValueAsJsonArray();
                String defaultValue = defaultValues.size() > 0  ? defaultValues.path(0).textValue() : "";
                
                if (!input.isVisible()) continue;
                submitForm += "<tr><td>"
                        + WordUtils.capitalizeFully(input.getKey())
                        + ": </td><td><input type=\"text\" name=\"" + input.getKey()
                        + "\" value=\"" + defaultValue 
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
            
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode json = mapper.createObjectNode().put("submitForm", submitFormHeader + submitForm);
            return Response.ok(new AgaveSuccessRepresentation(json.toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "There was an error generating the job submission form for " + softwareId, e);
        }
    }
}
