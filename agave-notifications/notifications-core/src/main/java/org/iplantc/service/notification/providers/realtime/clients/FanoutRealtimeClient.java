package org.iplantc.service.notification.providers.realtime.clients;

import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;

import com.nimbusds.jose.JOSEException;

/**
 * Client utility class to push messages to a fanout.io server.
 * @author dooley
 *
 */
public class FanoutRealtimeClient extends AbstractRealtimeClient implements NotificationAttemptProvider {
    
    private static final Logger log = Logger.getLogger(FanoutRealtimeClient.class);
    
    public FanoutRealtimeClient(NotificationAttempt attempt) {
        super(attempt);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.notification.http.clients.WebhookClient#getSupportedCallbackProviderType()
	 */
	@Override
	public String getSupportedCallbackProviderType() {
		return "fanout.io " + NotificationCallbackProviderType.REALTIME.name().toLowerCase();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.http.clients.WebhookClient#getFilteredHeaders(java.util.Map)
	 */
	@Override
	public Map<String, String> getFilteredHeaders(Map<String, String> headers) throws NotificationException {
		try {
			headers.put("Authorization", "Bearer " + SignedFanoutJWT.getInstance());
		}
		catch (JOSEException e) {
			throw new NotificationException("Failed to create JWT to authenticate to to fanout.io", e);
		}
		
		return headers;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.http.clients.WebhookClient#getFilteredHeaders(java.util.Map)
	 */
	@Override
	public String getFilteredCallbackUrl(String callbackUrl) throws NotificationException {
		return Settings.REALTIME_BASE_URL + Settings.REALTIME_REALM_ID + "/publish/";
	}
	
	
//    
//    /* (non-Javadoc)
//     * @see org.iplantc.service.notification.realtime.clients.RealtimeClient#publish(org.iplantc.service.notification.realtime.model.RealtimeMessageItems, java.lang.String)
//     */
//    @Override
//    public NotificationAttemptResponse publish() throws NotificationException {
//    	
//    	NotificationAttemptResponse attemptResponse = new NotificationAttemptResponse();
//    	
//        try 
//        {
//            
//            
//            ObjectMapper mapper = new ObjectMapper();
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
//            String messageBody = "";
//            // disabling web socket support for the moment
////            if (false) {
////            	httpPost.setHeader("Content-Type", "application/websocket-events");
////            	messageBody = "TEXT 2F\\\r\\\n\n" + 
////    				"m:" + mapper.writeValueAsString(items) + "\\\r\\\n";
////    		} 
//            // data push is what we're supporting in this release
////    		else {
//    			httpPost.setHeader("Content-Type", "application/json");
//    			messageBody =  mapper.writeValueAsString(getMessageItemsForAttempt());
////    		}
//    		
//            httpPost.setEntity(new StringEntity(messageBody));
//            
//            httpPost.setHeader("Content-Type", "application/json");
//            httpPost.setHeader("User-Agent", "Agave-Hookbot/"+ org.iplantc.service.common.Settings.getContainerId());
//            httpPost.setHeader("X-Agave-Delivery", attempt.getUuid());
//            httpPost.setHeader("X-Agave-Notification", attempt.getNotificationId());
//            httpPost.setHeader("Authorization", "Bearer " + SignedFanoutJWT.getInstance());
//            
//            long callstart = System.currentTimeMillis();
//            
//            try
//            {
//                response = httpclient.execute(httpPost);
//                attemptResponse.setCode(response.getStatusLine().getStatusCode());
//                if (attemptResponse.getCode() >= 200 && attemptResponse.getCode() < 300) {
//                	attemptResponse.setMessage("200 ok");
//					log.debug("[" + attempt.getUuid() + "] Successfully sent " + attempt.getEventName() + 
//							getSupportedCallbackProviderType() + " notification  to " + attempt.getCallbackUrl());
//                } else {
//					InputStream in = null;
//					byte[] bs = new byte[2048]; 
//					try {
//						in = response.getEntity().getContent();
//						in.read(bs);
//						attemptResponse.setMessage(new String(bs));
//					} catch (Exception e) {
//						attemptResponse.setMessage("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
//								getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() + 
//								". Server responded with: " + attemptResponse.getCode() + " - " + response.getStatusLine().getReasonPhrase());
//						log.error(attemptResponse.getMessage(), e);
//					}
//					finally {
//						try {in.close();} catch (Exception e){}
//					}
//				}
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
