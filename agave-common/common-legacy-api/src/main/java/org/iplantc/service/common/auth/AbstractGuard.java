/**
 * 
 */
package org.iplantc.service.common.auth;

import java.util.List;

import org.restlet.Context;
import org.restlet.Guard;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Request;

/**
 * @author dooley
 *
 */
public abstract class AbstractGuard extends Guard {
	
	protected List<Method> unprotectedMethods = null;
	/**
	 * @param context
	 * @param scheme
	 * @param realm
	 * @throws IllegalArgumentException
	 */
	public AbstractGuard(Context context, ChallengeScheme scheme,
			String realm, List<Method> unprotectedMethods) throws IllegalArgumentException
	{
		super(context, scheme, realm);
		this.unprotectedMethods = unprotectedMethods;
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.Guard#authenticate(org.restlet.data.Request)
	 */
	@Override
	public int authenticate(Request request)
	{
		if (request.getMethod().equals(Method.OPTIONS)) {
			return 1;
		} else if (this.unprotectedMethods == null) {
			return super.authenticate(request);
		} else {
			int authResponse = super.authenticate(request);
			if (authResponse == 0 && this.unprotectedMethods.contains(request.getMethod()) 
					&&  request.getChallengeResponse() == null) {
				return 1;
			} else {
				return authResponse;
			}
		}
	}
}