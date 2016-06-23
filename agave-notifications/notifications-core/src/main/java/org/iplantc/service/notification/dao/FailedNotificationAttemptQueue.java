/**
 * 
 */
package org.iplantc.service.notification.dao;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.CommandFailureException;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

/**
 * Manages the failed notification stack in MongoDB. The stack is implemented
 * as a capped collection with a fixed of 1000 entries.
 * 
 * @author dooley
 *
 */
public class FailedNotificationAttemptQueue {

	private static final Logger	log	= Logger.getLogger(FailedNotificationAttemptQueue.class);

	private MongoClient mongoClient;
	private DB db;
	
	private static FailedNotificationAttemptQueue queue;
	
	private FailedNotificationAttemptQueue() {
		
	}
	
	public static FailedNotificationAttemptQueue getInstance() {
		if (queue == null) {
			queue = new FailedNotificationAttemptQueue();
		}
		
		return queue;
	}
	
	/**
	 * Establishes a connection to the mongo server
	 * 
	 * @return
	 * @throws UnknownHostException
	 */
	public MongoClient getMongoClient() throws UnknownHostException
    {
		
		if (mongoClient == null ) 
    	{
	    	mongoClient = new MongoClient(new ServerAddress(Settings.FAILED_NOTIFICATION_DB_HOST, Settings.FAILED_NOTIFICATION_DB_PORT), Arrays.asList(getMongoCredential()));
    	} 
    	else if (!mongoClient.getConnector().isOpen()) 
    	{
    		try { mongoClient.close(); } catch (Exception e) { log.error("Failed to close mongo client.", e); }
    		mongoClient = null;
    		mongoClient = new MongoClient(new ServerAddress(Settings.FAILED_NOTIFICATION_DB_HOST, Settings.FAILED_NOTIFICATION_DB_PORT), Arrays.asList(getMongoCredential()));
    	}
    		
        return mongoClient;
    }
    
    /**
     * Creates a new MongoDB credential for the database collections
     * @return
     */
    private MongoCredential getMongoCredential() {
//    	return MongoCredential.createMongoCRCredential(
//                Settings.METADATA_DB_USER, "api", Settings.METADATA_DB_PWD.toCharArray());
    	return MongoCredential.createCredential(
                Settings.FAILED_NOTIFICATION_DB_USER, "api", Settings.FAILED_NOTIFICATION_DB_PWD.toCharArray());
    }
    
    /**
     * Fetches or creates a MongoDB capped collection with the given name, 
     * bound in byte size by {@link Settings#FAILED_NOTIFICATION_COLLECTION_SIZE} and
     * bound in length by {@link Settings#FAILED_NOTIFICATION_COLLECTION_SIZE}.
     * 
     * @param collectionName
     * @return
     * @throws NotificationException
     */
    private DBCollection getOrCreateCappedCollection(String collectionName) 
    throws NotificationException 
    {	
    	// Set up MongoDB connection
        try
        {
        	db = getMongoClient().getDB("api");//Settings.FAILED_NOTIFICATION_DB_SCHEME);
        	
            // Gets a collection, if it does not exist creates it
        	DBCollection cappedCollection = null;
        	
        	try {
        		DBObject options = BasicDBObjectBuilder.start()
                		.add("capped", true)
                		.add("size", Settings.FAILED_NOTIFICATION_COLLECTION_SIZE)
                		.add("max", Settings.FAILED_NOTIFICATION_COLLECTION_LIMIT)
                		.get();
                cappedCollection = db.createCollection(collectionName, options);
        	}
        	catch (CommandFailureException e) {
        		cappedCollection = db.getCollection(collectionName);
        		if (!cappedCollection.isCapped()) {
        			try {
        				BasicDBObject command = new BasicDBObject("convertToCapped", collectionName);
	        			command.append("size", Settings.FAILED_NOTIFICATION_COLLECTION_SIZE);
	            		command.append("max", Settings.FAILED_NOTIFICATION_COLLECTION_LIMIT);
	        			db.command(command);
        			}
        			catch (Throwable t) {
        				log.error("Failed to convert standard collection " + 
        						collectionName + " into capped collection", t);
        			}
        		}
        	}
        	
        	return cappedCollection;
        }
        catch (Exception e) {
        	throw new NotificationException("Failed to get capped collection " + collectionName, e);
        }
    }
    
	/**
	 * Pushes a {@link NotificationAttempt} into a MongoDB capped collection
	 * named after the associationed {@link Notification} uuid.
	 * 
	 * @param attempt
	 * @throws NotificationException
	 */
	public void push(NotificationAttempt attempt) 
	throws NotificationException
	{
		String json = null;
		try {
			DBCollection cappedCollection = getOrCreateCappedCollection(attempt.getNotificationId());
			ObjectMapper mapper = new ObjectMapper();
			json = mapper.writeValueAsString(attempt);
			
			DBObject doc = (DBObject)JSON.parse(json);
			
			cappedCollection.insert(doc);			
		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize attempt " + attempt.getUuid() + 
					" prior to sending to the failed queue for "  + attempt.getNotificationId(), e);
		}
		catch (MongoException e) {
			log.error("Failed to push failed notification attempt for " + attempt.getUuid() + 
					" on to the failed queue for "  + attempt.getNotificationId(), e);
		}
		catch (Exception e) {
			log.error("Unexpected server error while writing attempt " + attempt.getUuid() + 
					" on to the failed queue for "  + attempt.getNotificationId(), e);
		}
		finally {
			try {
				NotificationManager.process(attempt.getNotificationId(), "FAILURE", attempt.getOwner(), json);
			} 
			catch (Exception e) {
				log.error("Failed to raise failed notification event for " + attempt.getNotificationId() + 
						" after attempt " + attempt.getUuid() + " failed.", e);
			}
		}
	}
	
	/**
	 * Deletes a single {@link NotificationAttempt} from the queue.
	 * 
	 * @param notificationId
	 * @param attemptId
	 * @return the {@link NotificationAttempt} at the top of the queue or null if the queue is empty.
	 * @throws NotificationException
	 */
	public NotificationAttempt remove(String notificationId, String attemptId) 
	throws NotificationException
	{
		try {
			DBCollection cappedCollection = getOrCreateCappedCollection(notificationId);
			
			ObjectMapper mapper = new ObjectMapper();
			
			DBObject query = BasicDBObjectBuilder.start().add("id", attemptId).get();
			
			cappedCollection.remove(query);
		}
		catch (MongoException e) {
			log.error("Failed to fetch last failed notification attempt for " + notificationId, e);
		}
		catch (Exception e) {
			log.error("Unexpected server error while fetching last failed notification attempt for " + 
					notificationId, e);
		}
		
		return null;
	}
	
	/**
	 * Returns the next entry on the queue. This will be the oldest item,
	 * or the "top" of the queue.
	 * 
	 * @param notificationId
	 * @return the {@link NotificationAttempt} at the top of the queue or null if the queue is empty.
	 * @throws NotificationException
	 */
	public NotificationAttempt next(String notificationId) 
	throws NotificationException
	{
		try {
			DBCollection cappedCollection = getOrCreateCappedCollection(notificationId);
			
			ObjectMapper mapper = new ObjectMapper();
			
			// capped collections guarantee results return in the insertion order
			Cursor cursor = cappedCollection.find().limit(1);
			if (cursor.hasNext()) {
				return mapper.readValue(cursor.next().toString(), NotificationAttempt.class);
			}	
		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize last attempt for notification " + notificationId, e);
		}
		catch (MongoException e) {
			log.error("Failed to fetch last failed notification attempt for " + notificationId, e);
		}
		catch (Exception e) {
			log.error("Unexpected server error while fetching last failed notification attempt for " + 
					notificationId, e);
		}
		
		return null;
	}
	
	/**
	 * Removes all {@link NotificationAttempt} from a {@link Notification} attempt queue.
	 * 
	 * @param notificationId
	 * @param attemptId
	 * @return the {@link NotificationAttempt} at the top of the queue or null if the queue is empty.
	 * @throws NotificationException
	 */
	public NotificationAttempt removeAll(String notificationId) 
	throws NotificationException
	{
		try {
			DBCollection cappedCollection = getOrCreateCappedCollection(notificationId);
			// technically we drop the collection
			cappedCollection.drop();
		}
		catch (MongoException e) {
			log.error("Failed to fetch last failed notification attempt for " + notificationId, e);
		}
		catch (Exception e) {
			log.error("Unexpected server error while fetching last failed notification attempt for " + 
					notificationId, e);
		}
		
		return null;
	}
	
	/**
	 * Finds matching 
	 * 
	 * @param notificationId
	 * @return the {@link NotificationAttempt} at the top of the queue or null if the queue is empty.
	 * @throws NotificationException
	 */
	public List<NotificationAttempt> findMatching(String notificationId, Map<SearchTerm, Object> searchCriteria, int limit, int offset)  
	throws NotificationException
	{
		List<NotificationAttempt> attempts = new ArrayList<NotificationAttempt>();
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
			SimpleDateFormat beforeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'", Locale.ENGLISH);
			SimpleDateFormat afterFormatter = new SimpleDateFormat("yyyy-MM-dd'T'23:59:59'Z'", Locale.ENGLISH);
			DBCollection cappedCollection = getOrCreateCappedCollection(notificationId);
			
			ObjectMapper mapper = new ObjectMapper();
			
			BasicDBObjectBuilder builder = BasicDBObjectBuilder.start()
								.add("notificationId", notificationId);
			
			for (SearchTerm searchTerm: searchCriteria.keySet()) {
				if (searchTerm.getOperator() == SearchTerm.Operator.EQ) {
					builder.add(searchTerm.getSearchField(), searchCriteria.get(searchTerm));
				} 
				else if (searchTerm.getOperator() == SearchTerm.Operator.NEQ) {
					builder.push(searchTerm.getSearchField()).add("$ne", searchCriteria.get(searchTerm));
				} 
				else if (searchTerm.getOperator() == SearchTerm.Operator.GT) {
					builder.push(searchTerm.getSearchField()).add("$gt", searchCriteria.get(searchTerm));
				} 
				else if (searchTerm.getOperator() == SearchTerm.Operator.GTE) {
					builder.push(searchTerm.getSearchField()).add("$gte", searchCriteria.get(searchTerm));
				} 
				else if (searchTerm.getOperator() == SearchTerm.Operator.LT) {
					builder.push(searchTerm.getSearchField()).add("$lt", searchCriteria.get(searchTerm));
				} 
				else if (searchTerm.getOperator() == SearchTerm.Operator.LTE) {
					builder.push(searchTerm.getSearchField()).add("$lte", searchCriteria.get(searchTerm));
				}
				else if (searchTerm.getOperator() == SearchTerm.Operator.IN) {
					BasicDBList array = new BasicDBList();
					array.addAll((List<Object>)searchCriteria.get(searchTerm));
					builder.add(searchTerm.getSearchField(), array);
				}
				else if (searchTerm.getOperator() == SearchTerm.Operator.ON) {
					builder.push(searchTerm.getSearchField()) 
							.add("$gte", afterFormatter.format((Date)searchCriteria.get(searchTerm)))
							.add("$lte", beforeFormatter.format((Date)searchCriteria.get(searchTerm)));
				}
				else if (searchTerm.getOperator() == SearchTerm.Operator.AFTER) {
					builder.push(searchTerm.getSearchField()) 
						.add("$gt", formatter.format((Date)searchCriteria.get(searchTerm)));
				}
				else if (searchTerm.getOperator() == SearchTerm.Operator.BEFORE) {
					builder.push(searchTerm.getSearchField()) 
						.add("$lt", formatter.format((Date)searchCriteria.get(searchTerm)));
				}
			}
			
			DBObject query = builder.get();
			
			Cursor cursor = cappedCollection.find(query);
			if (cursor.hasNext()) {
				attempts.add(mapper.readValue(cursor.next().toString(), NotificationAttempt.class));
			}	
		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize last attempt for notification " + notificationId, e);
		}
		catch (MongoException e) {
			log.error("Failed to fetch last failed notification attempt for " + notificationId, e);
		}
		catch (Exception e) {
			log.error("Unexpected server error while fetching last failed notification attempt for " + 
					notificationId, e);
		}
		
		return attempts;
	}

}
