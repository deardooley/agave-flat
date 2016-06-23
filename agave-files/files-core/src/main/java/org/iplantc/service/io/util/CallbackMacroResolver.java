package org.iplantc.service.io.util;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.io.model.QueueTask;


public class CallbackMacroResolver {

	/**
	 * Resolves the macros in a url to the relevant encoding/staging attributes
	 * @param job
	 * @param callback
	 * @return
	 */
	public static String resolve(QueueTask task, String callback)
	{
		if (StringUtils.isEmpty(callback)) return null;

		if (ServiceUtils.isEmailAddress(callback)) return callback;

		callback = StringUtils.replace(callback, "${NAME}", task.getLogicalFile().getName());

		callback = StringUtils.replace(callback, "${SOURCE_FORMAT}",
				(ServiceUtils.isValid(task.getLogicalFile().getNativeFormat()) ?
						task.getLogicalFile().getNativeFormat() : "raw"));

		callback = StringUtils.replace(callback, "${STATUS}", task.getStatusAsString());

		return callback;

	}

//	/**
//	 * Resolves the macros in a url to the relevant decoding attributes
//	 * @param job
//	 * @param callback
//	 * @return
//	 */
//	public static String resolve(DecodingTask task, String callback)
//	{
//		if (ServiceUtils.isEmailAddress(callback)) return callback;
//
//		callback = callback.replaceAll("\\$\\{NAME\\}",
//				new File(task.getSourcePath()).getName());
//
//		callback = callback.replaceAll("\\$\\{SOURCE_FORMAT\\}",
//				(ServiceUtils.isValid(task.getSrcTransform()) ?
//						task.getSrcTransform() : "raw"));
//
//		callback = callback.replaceAll("\\$\\{DEST_FORMAT\\}",
//				(ServiceUtils.isValid(task.getDestTransform()) ?
//						task.getDestTransform() : "raw"));
//
//		callback = callback.replaceAll("\\$\\{STATUS\\}", task.getStatusAsString());
//
//		return callback;
//
//	}
}
