package org.iplantc.service.jobs.model.enumerations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.model.Job;

public enum WrapperTemplateStatusVariableType implements WrapperTemplateVariableType
{
		// status macros
		IPLANT_RUNNING(JobStatusType.RUNNING),
		AGAVE_JOB_CALLBACK_RUNNING(JobStatusType.RUNNING),
		IPLANT_SUCCESS(JobStatusType.FINISHED),
		AGAVE_JOB_CALLBACK_SUCCESS(JobStatusType.FINISHED),
		IPLANT_CLEANING_UP(JobStatusType.CLEANING_UP),
		AGAVE_JOB_CALLBACK_CLEANING_UP(JobStatusType.CLEANING_UP),
		IPLANT_FAILURE(JobStatusType.FAILED),
		AGAVE_JOB_CALLBACK_FAILURE(JobStatusType.FAILED),
		IPLANT_ARCHIVING_START(JobStatusType.ARCHIVING),
		AGAVE_JOB_CALLBACK_ARCHIVING_START(JobStatusType.ARCHIVING),
		IPLANT_ARCHIVING_SUCCESS(JobStatusType.ARCHIVING_FINISHED),
		AGAVE_JOB_CALLBACK_ARCHIVING_SUCCESS(JobStatusType.ARCHIVING_FINISHED),
		IPLANT_ARCHIVING_FAILURE(JobStatusType.ARCHIVING_FAILED),
		AGAVE_JOB_CALLBACK_ARCHIVING_FAILURE(JobStatusType.ARCHIVING_FAILED),

		AGAVE_JOB_CALLBACK_ALIVE(JobStatusType.HEARTBEAT),
		AGAVE_JOB_CALLBACK_NOTIFICATION(JobStatusType.HEARTBEAT);

		private JobStatusType status = null;

		private WrapperTemplateStatusVariableType(JobStatusType status) {
			this.status = status;
		}

		/* (non-Javadoc)
		 * @see org.iplantc.service.jobs.model.enumerations.WrapperTemplateVariableType#resolveForJob(org.iplantc.service.jobs.model.Job)
		 */
		@Override
		public String resolveForJob(Job job) {
			if (this == AGAVE_JOB_CALLBACK_NOTIFICATION) {
				return WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, null, null);
			}
			else {
				return String.format("curl -sSk \"%strigger/job/%s/token/%s/status/%s?pretty=true&filter=id,status\" 1>&2 \n\n",
						TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, job.getTenantId()),
						job.getUuid(),
						job.getUpdateToken(),
						status.name());
			}
		}
		
		/**
		 * Resolves a template variable with optional runtime arguments supplied which will then
		 * be passed as a json object in a postback to the trigger service. Syntax is of the form
		 * as <pre>${AGAVE_JOB_CALLBACK_NOTIFICATION::FOO,BAR,BAM}</pre>
		 * @param job
		 * @return
		 */
		public static String resolveNotificationEventMacro(Job job, String eventName, String[] callbackVariableNames) {
		    
			if (callbackVariableNames == null) {
				callbackVariableNames = new String[]{};
			}
			
			eventName = StringUtils.trimToNull(eventName);
			
            StringBuilder sb = new StringBuilder();
            
            sb.append("# Building job callback document to send to any notifications\n");
            sb.append("AGAVE_CALLBACK_FILE=\".callback-$(date +%Y-%m-%dT%H:%M:%S%z)\"\n");
            sb.append("echo -e \"{\" > \"$AGAVE_CALLBACK_FILE\"\n");
            if (!ArrayUtils.isEmpty(callbackVariableNames)) {
                for (String varName: new HashSet<String>(Arrays.asList(callbackVariableNames))) {
                	varName = StringUtils.trimToNull(varName);
                	if (varName != null) {
                		sb.append(String.format("echo '  \"%s\": \"'$(printf %%q \"$%s\")'\",' >> \"$AGAVE_CALLBACK_FILE\"\n", varName, varName));
                	}
                }
            }
            
            // add the user-defined event if provided
            if (StringUtils.isEmpty(eventName)) {
            	eventName = "JOB_RUNTIME_CALLBACK_EVENT";
            }
        
            sb.append("echo '  \"CUSTOM_USER_JOB_EVENT_NAME\": \""+eventName+"\"' >> \"$AGAVE_CALLBACK_FILE\"\n");
            
            sb.append("echo -e \"}\" >> \"$AGAVE_CALLBACK_FILE\"\n\n");
            sb.append(String.format("cat \"$AGAVE_CALLBACK_FILE\" | sed  -e \"s#: \\\"''\\\"#: \\\"\\\"#g\" | curl -sSk -H \"Content-Type: application/json\" -X POST --data-binary @- \"%strigger/job/%s/token/%s/status/HEARTBEAT?pretty=true\" 1>&2 \n\n",
                    TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, job.getTenantId()),
                    job.getUuid(),
                    job.getUpdateToken()));
            
            return sb.toString();
        }


		/**
		 * These are the white listed status callbacks users can add in their wrapper templates
		 * to be called by the API at runtime. Anything not in this list will be removed when
		 * generating the job ipcexe file.
		 * @return array of WrapperTemplateStatusVariableType approved for use by users.
		 */
		public static WrapperTemplateStatusVariableType[] getUserAccessibleStatusCallbacks()
		{
			return new WrapperTemplateStatusVariableType[]{
					IPLANT_FAILURE,
    				AGAVE_JOB_CALLBACK_FAILURE,
    				AGAVE_JOB_CALLBACK_ALIVE,
    				AGAVE_JOB_CALLBACK_NOTIFICATION
			};
		}
}
