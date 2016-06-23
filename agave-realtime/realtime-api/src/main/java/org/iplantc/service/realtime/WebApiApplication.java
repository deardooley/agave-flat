package org.iplantc.service.realtime;

import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.realtime.resource.impl.RealtimeCollectionImpl;
import org.iplantc.service.realtime.resource.impl.RealtimeResourceImpl;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;
import org.restlet.service.CorsService;

public class WebApiApplication extends Application {

    /*
     * Define route constants
     */
    public static final String ROUTE_REALTIME_RESOURCE = "/";
    public static final String ROUTE_REALTIME_COLLECTION = "/";

    /*
     * Define role names
     */
    public static final String ROLE_ADMIN = "admin";

    public static final String ROLE_ANYONE = "anyone";

    public static final String ROLE_DEV = "cellroledev";

    public static final String ROLE_OWNER = "cellroleowner";

    public static final String ROLE_USER = "cellroleuser";

    private String versionFull;

    private int versionMajor;

    private int versionMicro;

    private int versionMinor;
    
    public WebApiApplication() {
    	setName("agaveRealtimeApi");
        CorsService corsService = new CorsService();
        corsService.setAllowedCredentials(true);
        corsService.setSkippingResourceForCorsOptions(true);
        getServices().add(corsService);
    }

	private ChallengeAuthenticator createApiGuard(Restlet next) {

//        ChallengeAuthenticator apiGuard = new ChallengeAuthenticator(
//                getContext(), ChallengeScheme.HTTP_BASIC, "realm");
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator apiGuard = new ChallengeAuthenticator(getContext(),
  				 ChallengeScheme.HTTP_BASIC, "The Agave Platform");
  		

        // Create in-memory users and roles.
//        MemoryRealm realm = new MemoryRealm();
//        User owner = new User("owner", "owner");
//        realm.getUsers().add(owner);
//        realm.map(owner, Role.get(this, ROLE_OWNER));
//        realm.map(owner, Role.get(this, ROLE_USER));
//        realm.map(owner, Role.get(this, ROLE_DEV));
//        User admin = new User("admin", "admin");
//        realm.getUsers().add(admin);
//        realm.map(admin, Role.get(this, ROLE_ADMIN));
//        realm.map(admin, Role.get(this, ROLE_OWNER));
//        realm.map(admin, Role.get(this, ROLE_USER));
//        realm.map(admin, Role.get(this, ROLE_DEV));
//        User user = new User("user", "user");
//        realm.getUsers().add(user);
//        realm.map(user, Role.get(this, ROLE_USER));

        // Verifier : to check authentication
//        apiGuard.setVerifier(realm.getVerifier());
        
        apiGuard.setVerifier(verifier);
        // Enroler : add authorization roles
//        apiGuard.setEnroler(realm.getEnroler());
        
        apiGuard.setNext(next);

        // In case of anonymous access supported by the API.
//        apiGuard.setOptional(true);

        return apiGuard;
    }
    
    public Router createApiRouter() {
        Router apiRouter = new Router(getContext());
        apiRouter.attach(ROUTE_REALTIME_RESOURCE, RealtimeResourceImpl.class);
        apiRouter.attach(ROUTE_REALTIME_COLLECTION, RealtimeCollectionImpl.class);
        return apiRouter;
	}

    public Restlet createInboundRoot() {

        // Router for the API's resources
        Router apiRouter = createApiRouter();
        // Protect the set of resources
        ChallengeAuthenticator guard = createApiGuard(apiRouter);

        return guard;
    }

}
