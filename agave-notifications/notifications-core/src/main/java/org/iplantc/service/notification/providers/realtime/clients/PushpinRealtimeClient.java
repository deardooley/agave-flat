package org.iplantc.service.notification.providers.realtime.clients;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.util.ServiceUtils;

/**
 * Client utility class to push messages to a pushpin server.
 * @author dooley
 *
 */
public class PushpinRealtimeClient extends AbstractRealtimeClient {
    
    private static final Logger log = Logger.getLogger(PushpinRealtimeClient.class);
    
    public PushpinRealtimeClient(NotificationAttempt attempt) {
        super(attempt);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.notification.http.clients.WebhookClient#getFilteredHeaders(java.util.Map)
	 */
	@Override
	public String getFilteredCallbackUrl(String callbackUrl) throws NotificationException {
		if (ServiceUtils.isValidPushpinChannel(attempt.getCallbackUrl())) {
			return attempt.getCallbackUrl();
		} else {
			return Settings.REALTIME_BASE_URL + "/publish/";
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.http.clients.WebhookClient#getSupportedCallbackProviderType()
	 */
	@Override
	public String getSupportedCallbackProviderType() {
		return "pushpin " + NotificationCallbackProviderType.REALTIME.name().toLowerCase();
	}
    
//    /* (non-Javadoc)
//	 * @see org.iplantc.service.notification.realtime.clients.RealtimeClient#publish(org.iplantc.service.notification.realtime.model.RealtimeMessageItems, java.lang.String)
//	 */
//    @Override
//	public int publish(RealtimeMessageItems items, String attemptUuid) throws NotificationException {
//        int responseCode = 0;
//        
//        try 
//        {
//            URI pushUri = new URI(Settings.REALTIME_BASE_URL + "/publish/");
//            
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.writeValueAsString(items);
//            
//            CloseableHttpClient httpclient = null;
//            if (pushUri.getScheme().equalsIgnoreCase("https"))
//            {
//                SSLContextBuilder builder = new SSLContextBuilder();
//                builder.loadTrustMaterial(null, new TrustStrategy() {
//
//                    public boolean isTrusted(
//                            final X509Certificate[] chain, final String authType) throws CertificateException {
//                        return true;
//                    }
//
//                });
//                
//                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
//                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//                
//                httpclient = HttpClients.custom()
//                        .setSSLSocketFactory(sslsf)
//                        .setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
//                        .build();
//            }
//            else
//            {
//                httpclient = HttpClients.custom()
//                        .setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
//                        .build();
//            }
//            
//            HttpPost httpPost = new HttpPost(pushUri);
//            CloseableHttpResponse response = null;
//            RequestConfig config = RequestConfig.custom()
//                                                .setConnectTimeout(20000)
//                                                .setSocketTimeout(10000)
//                                                .build();
//            httpPost.setConfig(config);
//            
//            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(items)));
//            httpPost.setHeader("Content-Type", "application/json");
//            httpPost.setHeader("User-Agent", "Agave-Hookbot/"+ org.iplantc.service.common.Settings.getContainerId());
//            httpPost.setHeader("X-Agave-Delivery", attemptUuid);
//            httpPost.setHeader("X-Agave-Notification", notification.getUuid());
//            
//            long callstart = System.currentTimeMillis();
//            
//            try
//            {
//                response = httpclient.execute(httpPost);
//                
//                responseCode = response.getStatusLine().getStatusCode();
//                if (responseCode >= 200 && responseCode < 300) {
//                	log.debug("[" + attemptUuid + "] Successfully sent " + event + " notification realtime message to " + notification.getCallbackUrl());
//                } else {
//                    log.error("[" + attemptUuid + "] Failed to send " + event + " notification realtime message to " + notification.getCallbackUrl() + 
//                            ". Server responded with: " + responseCode + " - " + response.getStatusLine().getReasonPhrase());
//                }
//                
////                HttpEntity entity = response.getEntity();
////                byte[] bis = StreamUtils.getBytes(entity.getContent());
////                System.out.println(new String(bis));
//            } 
//            catch (ConnectTimeoutException e) {
//                responseCode = 408;
//                log.debug("[" + attemptUuid + "] Failed to send " + event + " notification realtime message to " + notification.getCallbackUrl() +
//                        ". Remote call to " + pushUri.toString() + " timed out after " + 
//                        (System.currentTimeMillis() - callstart) + " milliseconds.", e);
//            } 
//            catch (SSLException e) {
//                responseCode = 404;
//                notification.setTerminated(true);
//                
//                if (StringUtils.equalsIgnoreCase(pushUri.getScheme(), "https")) {
//                    throw new NotificationException("Failed to send " + event + " notification realtime message to " + notification.getCallbackUrl() +
//                            ". Remote call to " + pushUri.toString() + " failed due to the remote side not supporting SSL.", e);
//                } else {
//                    throw new NotificationException("Failed to send " + event + " notification realtime message to " + notification.getCallbackUrl() +
//                            ". Remote call to " + pushUri.toString() + " failed due a server side SSL failure.", e);
//                }
//            } 
//            catch (Exception e) {
//                responseCode = 500;
//                log.debug("Failed to send " + event + " notification realtime message to " + notification.getCallbackUrl() +
//                        ". Remote call to " + pushUri.toString() + " failed after " + 
//                        (System.currentTimeMillis() - callstart) + " milliseconds.", e);
//            } 
//            finally {
//                try { response.close(); } catch (Exception e) {}
//            }   
//        }
//        catch (NotificationException e) {
//            throw e;
//        }
//        catch(Exception e) {
//            log.error("[" + attemptUuid + "] Failed to send " + event + " notification email to " + notification.getCallbackUrl() +
//                    " due to internal server error.", e);
//            responseCode = 500;
//        }
//        
//        return responseCode;
//    }
}
