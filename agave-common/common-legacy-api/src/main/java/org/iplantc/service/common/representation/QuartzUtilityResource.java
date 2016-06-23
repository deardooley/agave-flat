package org.iplantc.service.common.representation;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.quartz.util.QuartzUtility;
import org.iplantc.service.common.resource.AgaveResource;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.testng.log4testng.Logger;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Simple API to manage quartz scheduler running in an 
 * API container. Even when running headless, this api will 
 * be available and protected by the same auth as the rest 
 * of the APIs. Minimum of tenant admin access is required
 * to interact with it.
 * 
 * Adapted from work by Silviu Ilie
 * @author dooley
 *
 */
public class QuartzUtilityResource extends AgaveResource 
{
	private static final Logger log = Logger.getLogger(QuartzUtilityResource.class);
	private final QuartzUtility quartzUtility = QuartzUtility.getInstance();
	private final String action;
	
    public QuartzUtilityResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);
		String scheduler = "AgaveProducerScheduler";
		try
		{
		    StdSchedulerFactory factory = new StdSchedulerFactory();
		    Form query = request.getOriginalRef().getQueryAsForm();
		    if (StringUtils.isNotEmpty(query.getFirstValue("scheduler"))) {
		        scheduler = query.getFirstValue("scheduler");
		    }
			quartzUtility.setQuartzScheduler(factory.getScheduler(scheduler));
		}
		catch (SchedulerException e)
		{
			log.error("Unable to get " + scheduler + " scheduler", e);
//			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to get handle on default scheduler.");
		}
		
		action = (String) request.getAttributes().get("quartzaction");
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
	}
    
    @Override
	public Representation represent(Variant variant) throws ResourceException
	{
    	Representation rep = null;
		
    	if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_HOME)) 
    	{
    		Configuration cfg = new Configuration();
    		Template template;
			try
			{	
				cfg.setTemplateLoader(new ContextTemplateLoader(getContext(), "clap:///"));
				template = cfg.getTemplate("quartz.ftl");
				HashMap<String, String> dataModel = new HashMap<String, String>();
				dataModel.put("QUARTZ_UTILITY_HOME", QuartzUtility.QUARTZ_UTILITY_HOME);
				dataModel.put("QUARTZ_UTILITY_CHANGE_TRIGGER", QuartzUtility.QUARTZ_UTILITY_CHANGE_TRIGGER);
				dataModel.put("QUARTZ_UTILITY_REVERT_TRIGGER_CHANGES", QuartzUtility.QUARTZ_UTILITY_REVERT_TRIGGER_CHANGES);
				dataModel.put("QUARTZ_UTILITY_LIST_CHANGES", QuartzUtility.QUARTZ_UTILITY_LIST_CHANGES);
				dataModel.put("QUARTZ_UTILITY_INTERRUPT_JOB", QuartzUtility.QUARTZ_UTILITY_INTERRUPT_JOB);
				dataModel.put("QUARTZ_UTILITY_PAUSE_JOB", QuartzUtility.QUARTZ_UTILITY_PAUSE_JOB);
				dataModel.put("QUARTZ_UTILITY_RESUME_JOB", QuartzUtility.QUARTZ_UTILITY_RESUME_JOB);
				dataModel.put("QUARTZ_UTILITY_PAUSE_TRIGGER", QuartzUtility.QUARTZ_UTILITY_PAUSE_TRIGGER);
				dataModel.put("QUARTZ_UTILITY_RESUME_TRIGGER", QuartzUtility.QUARTZ_UTILITY_RESUME_TRIGGER);
				dataModel.put("QUARTZ_UTILITY_RESUME_ALL", QuartzUtility.QUARTZ_UTILITY_RESUME_ALL);
				dataModel.put("QUARTZ_UTILITY_PAUSE_ALL", QuartzUtility.QUARTZ_UTILITY_PAUSE_ALL);
				dataModel.put("QUARTZ_UTILITY_LIST", QuartzUtility.QUARTZ_UTILITY_LIST);
				rep = new TemplateRepresentation(template, dataModel, MediaType.TEXT_HTML);
			}
			catch (IOException e)
			{
				log.error("Failed to create quartz freemarker template.", e);
				rep = new IplantErrorRepresentation(e.getMessage());
			}
    	} 
    	else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_LIST)) 
    	{
    		rep = new JsonRepresentation(quartzUtility.schedulerDetails());
    	}
    	
    	return rep;
	}
    
    @Override
	public void acceptRepresentation(Representation entity)
	throws ResourceException
	{
    	Form form = getRequest().getEntityAsForm();
    	
        if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_LIST_CHANGES)) {
        	getResponse().setEntity(new JsonRepresentation(quartzUtility.listChanges())
            );
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_PAUSE_ALL)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.pauseAll()
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_RESUME_ALL)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.resumeAll()
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_PAUSE_JOB)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.pauseJob(form.getFirstValue("target"))
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_RESUME_JOB)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.resumeJob(form.getFirstValue("target"))
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_INTERRUPT_JOB)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.interruptJob(form.getFirstValue("target"))
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_CHANGE_TRIGGER)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.setNewTrigger(
                    		form.getFirstValue("target")
                            , form.getFirstValue("trigger")
                            , form.getFirstValue("newExpression")
                            , form.getFirstValue("oldExpression")                            
                    )
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_REVERT_TRIGGER_CHANGES)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.resetCronExpression(
                    		form.getFirstValue("trigger")           
                    )
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_PAUSE_TRIGGER)) {
        	getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.pauseTrigger(
                    		form.getFirstValue("triggerName")
                            , form.getFirstValue("triggerGroup")          
                    )
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_RESUME_TRIGGER)) {
            getResponse().setEntity(new JsonRepresentation(
                    quartzUtility.resumeTrigger(
                            form.getFirstValue("triggerName")
                            , form.getFirstValue("triggerGroup")       
                    )
            ));
        } else if (StringUtils.equals(action, QuartzUtility.QUARTZ_UTILITY_LIST)) {
            getResponse().setEntity(new JsonRepresentation(
            		quartzUtility.schedulerDetails()
            ));
        }
	}
	/**
     * quartz priority reload 'home'.
     *
     * @return view name.
     */
//    @RequestMapping(value = QUARTZ_UTILITY_HOME)
    public final String quartzChange(HttpSession session) {
        if (quartzUtility.isAuthorized()) {
            return "quartzReload";
        }
        throw new IllegalStateException();
    }

    /**
     * lists scheduler jobs.
     * @param session {@code HttpSession} used for authorization.
     * @return JSON containing list of {@link org.iplantc.service.common.quartz.util.ui.JobDescription}s and general scheduler
     * data.
     */
//    @RequestMapping(value = QUARTZ_UTILITY_LIST)
//    @ResponseBody
    public final String listJobs() {
        String result = "{}";

        if (quartzUtility.isAuthorized()) 
        	result = quartzUtility.schedulerDetails();

        return result;
    }

    /**
     * sets new cron expression on trigger.
     *
     * @param target        job name
     * @param trigger       trigger name
     * @param newExpression old cron expression
     * @param oldExpression new cron expression
     *
     * @return new cron expression or failure message if change is not executed.
     */
//    @RequestMapping(value = QUARTZ_UTILITY_CHANGE_TRIGGER, method = RequestMethod.POST)
//    @ResponseBody
    public final String changeTrigger(final String target,
                                      final String trigger,
                                      final String newExpression,
                                      final String oldExpression) {
    	
        return quartzUtility.setNewTrigger(target, trigger, oldExpression, newExpression);
    }

    /**
     * revert to the original trigger cron expression.
     *
     * @param trigger job name
     * @param session session
     * @return
     */
//    @RequestMapping(value = QUARTZ_UTILITY_REVERT_TRIGGER_CHANGES, method = RequestMethod.POST)
//    @ResponseBody
    public final String revertPriority(final String trigger) {
        return quartzUtility.resetCronExpression(trigger);
    }

    /**
     * provides all changes as json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_LIST_CHANGES, method = RequestMethod.GET)
//    @ResponseBody
    public final String listChangeLog() {
        return quartzUtility.listChanges();
    }

    /**
     * interrupts Job identified by {@code target}.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_INTERRUPT_JOB, method = RequestMethod.POST)
//    @ResponseBody
    public final String interruptJob(String target) {
        return quartzUtility.interruptJob(target);
    }

    /**
     * pauses job identified by {@code target}.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_PAUSE_JOB, method = RequestMethod.POST)
//    @ResponseBody
    public final String pauseJob(String target) {
        return quartzUtility.pauseJob(target);
    }

    /**
     * resumes job identified by {@code target}.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_RESUME_JOB, method = RequestMethod.POST)
//    @ResponseBody
    public final String resumeJob(String target, HttpSession session) {
        return quartzUtility.resumeJob(target);
    }

    /**
     * pauses trigger identified by {@code triggerName} from group identified by {@code triggerGroup}.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_PAUSE_TRIGGER, method = RequestMethod.POST)
//    @ResponseBody
    public final String pauseTrigger(String triggerName, String triggerGroup) {
        return quartzUtility.pauseTrigger(triggerName, triggerGroup);
    }

    /**
     * resumes trigger identified by {@code triggerName} from group identified by {@code triggerGroup}.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_RESUME_TRIGGER, method = RequestMethod.POST)
//    @ResponseBody
    public final String resumeTrigger(String triggerName, String triggerGroup) {
        return quartzUtility.resumeTrigger(triggerName, triggerGroup);
    }

    /**
     * resumes all scheduled jobs.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_RESUME_ALL, method = RequestMethod.POST)
//    @ResponseBody
    public final String resumeAll() {
        return quartzUtility.resumeAll();
    }

    /**
     * pauses all scheduled jobs.
     *
     * @return json array of {@link org.iplantc.service.common.quartz.util.QuartzConfigResetResponse}
     */
//    @RequestMapping(value = QUARTZ_UTILITY_PAUSE_ALL, method = RequestMethod.POST)
//    @ResponseBody
    public final String pauseAll() {
        return quartzUtility.pauseAll();
    }

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return false;
	}
    
    
}
