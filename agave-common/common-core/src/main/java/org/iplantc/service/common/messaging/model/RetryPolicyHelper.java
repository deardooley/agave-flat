package org.iplantc.service.common.messaging.model;

import static org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType.*;
import org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType;

public class RetryPolicyHelper {

	public RetryPolicyHelper() {
	}
	
	public static RetryPolicy getDefaultRetryPolicyForRetryStrategyType(RetryStrategyType strategy) {
		if (IMMEDIATE == strategy) {
			return new RetryPolicy(IMMEDIATE, 5, 0, 0, false);
		}
		else if (DELAYED == strategy) {
			return new RetryPolicy(DELAYED, 1440, 60, 60, false);
		}
		else if (EXPONENTIAL == strategy) {
			return new RetryPolicy(EXPONENTIAL, 1440, 0, 0, false);
		}
		else {
			return new RetryPolicy(NONE, 0, 0, 0, false);
		}
	}

}
