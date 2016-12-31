package org.iplantc.service.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * Just a helper class to simplify usage
 */
public class JsonPathPropertyFilter {
	
	private static final Logger log = Logger
			.getLogger(JsonPathPropertyFilter.class);
	
	static {
		Configuration.setDefaults(new Configuration.Defaults() {
	
		    private final JsonProvider jsonProvider = new JacksonJsonProvider();
		    private final MappingProvider mappingProvider = new JacksonMappingProvider();
	
		    @Override
		    public JsonProvider jsonProvider() {
		        return jsonProvider;
		    }
	
		    @Override
		    public MappingProvider mappingProvider() {
		        return mappingProvider;
		    }
	
		    @Override
		    public Set<Option> options() {
		        return EnumSet.noneOf(Option.class);
		    }
		});
	}
	
	 public JsonPathPropertyFilter() {}
    
//    /**
//     * Applies path filters to a serialized JSON object and returns the result with optional
//     * pretty printing.
//     * @param content
//     * @param filters
//     * @param prettyPrint
//     * @return
//     * @throws IOException
//     */
//    public String getFilteredContent(String content, String[] filters, boolean prettyPrint) throws IOException {
//    	ObjectMapper mapper = new ObjectMapper();
//    	
//    	return getJsonPathFilteredContent(mapper.readTree(content), filters, prettyPrint);
//    }
    
//    /**
//     * Applies path filters to a {@link JsonNode} and returns the result with optional
//     * pretty printing.
//     * @param json
//     * @param filters
//     * @param prettyPrint
//     * @return
//     * @throws IOException
//     */
//    public String getJsonPathFilteredContent(JsonNode json, String[] filters, boolean prettyPrint) 
//    throws IOException 
//    {	
//    	try {
//    		ObjectMapper mapper = new ObjectMapper();
//    		Configuration conf = Configuration.builder()
//    				   .options(Option.AS_PATH_LIST)
//    				   .options(Option.SUPPRESS_EXCEPTIONS)
//    				   .options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
//    		
//    		Object document = conf.jsonProvider().parse(json.toString());
//    		
//    		for (String filter: filters) {
////	    		if (json.isObject()) {
//	    			List<String> validPaths = new ArrayList<String>();
//	    		
//	    			try {
//	    				String path = JsonPath.read(document, filter);
//	    				validPaths.add(path);
//	    			}
//	    			catch (Exception e) {
//	    				log.debug("Filtering field from response: " + filter);
//	    			}
//	    			
//	    			// rebuild json object based on requested fields
//	    			for (String validPath: validPaths) {
//	    				
//	    			}
////	    		}
////	    		else {
////		    	
////		    		JsonPath.parse(json).read(filter)
////	    		}
//	    			
//    		}
//    	}
//    	catch (Exception e) {
//    		
//    	}
//    	return null;
//    }
    	
    
//    /**
//     * Applies path filters to a {@link JsonNode} and returns the result with optional
//     * pretty printing.
//     * @param json
//     * @param filters
//     * @param prettyPrint
//     * @return
//     * @throws IOException
//     */
//    public String getFilteredContent(JsonNode json, String[] filters, boolean prettyPrint) 
//    throws IOException 
//    {	
//    	ObjectMapper mapper = new ObjectMapper();
//    	
//    	
//        
//    	JsonPath.parse(json).read("$.store.book[0]");
//    	if (ArrayUtils.isEmpty(filters) || ArrayUtils.contains(filters, "*")) {
//    		if (prettyPrint) {
//	    		DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
//	    		return mapper.writer(pp).writeValueAsString(json);
//	    	} else {
//	    		return json.toString();
//	    	}
//    	}
//    	
//    	// iterate over array
//    	if (json.isArray()) {
//	    	ArrayNode filteredJson = mapper.createArrayNode();
//	    	for (Iterator<JsonNode> iter=json.iterator(); iter.hasNext(); ) {
////	    		String childJson = mapper.writer().writeValueAsString(child);
//	    		
//	    		ObjectNode child = (ObjectNode)iter.next();
//	    		ObjectNode filteredChild = mapper.createObjectNode();
//	    		for (String filter: filters) {
//	    			String[] tokens = StringUtils.split(filter, ".");
//	    			if (tokens.length == 2) {
//    					if (child.has(tokens[0])) {
//    						if (child.get(tokens[0]).has(tokens[1])) {
//    							if (filteredChild.has(tokens[0])) {
//	    							((ObjectNode)filteredChild.get(tokens[0])).put(tokens[1], filteredChild.get(tokens[0]).get(tokens[1]));
//    							}
//    							else {
//    								filteredChild.putObject(tokens[0]).put(tokens[1], child.get(tokens[0]).get(tokens[1]));
//    							}
//    						}
//    						else {
//    							if (filteredChild.has(tokens[0])) {
//    								((ObjectNode)filteredChild.get(tokens[0])).putNull(tokens[1]);
//    							}
//    							else {
//    								filteredChild.putObject(tokens[0]).putNull(tokens[1]);
//    							}
//    						}
//    					}
//    				} 
//	    			else if (tokens.length == 1) { 
//		    			if (child.has(filter)) {
//		    				filteredChild.put(filter, child.get(filter));
//		    			}
//	    			}
//	    		}
//	    			    		
//	    		if (!filteredChild.isNull() && filteredChild.size() > 0) {
//	    			filteredJson.add(filteredChild);
//	    		}
//    		}
//	    	
//	    	if (prettyPrint) {
//	    		DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
//	    		return mapper.writer(pp).writeValueAsString(filteredJson);
//	    	} else {
//	    		return mapper.writeValueAsString(filteredJson);
//	    	}
//    	}
//    	else {
//    		ObjectNode filteredChild = mapper.createObjectNode();
//    		for (String filter: filters) {
//    			String[] tokens = StringUtils.split(filter, ".");
//    			if (tokens.length == 2) {
//					if (json.has(tokens[0])) {
//						if (json.get(tokens[0]).has(tokens[1])) {
//							if (filteredChild.has(tokens[0])) {
//    							((ObjectNode)filteredChild.get(tokens[0])).put(tokens[1], filteredChild.get(tokens[0]).get(tokens[1]));
//							}
//							else {
//								filteredChild.putObject(tokens[0]).put(tokens[1], json.get(tokens[0]).get(tokens[1]));
//							}
//						}
//						else {
//							if (filteredChild.has(tokens[0])) {
//								((ObjectNode)filteredChild.get(tokens[0])).putNull(tokens[1]);
//							}
//							else {
//								filteredChild.putObject(tokens[0]).putNull(tokens[1]);
//							}
//						}
//					}
//				} 
//    			else if (tokens.length == 1) { 
//	    			if (json.has(filter)) {
//	    				filteredChild.put(filter, json.get(filter));
//	    			}
//    			}
//    		}
//    		
//    		if (prettyPrint) {
//	    		DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
//	    		return mapper.writer(pp).writeValueAsString(filteredChild);
//	    	} else {
//	    		return mapper.writeValueAsString(filteredChild);
//	    	}
//    	}
//    }
    
//    /**
//     * Retrieves the subpath specified in json dot notation from the given {@link JsonNode}. 
//     * A {@link JsonNode} will be returned regardless. A NullNode will be returned if the
//     * path does not exist. 
//     * 
//     * @param json the full resource representation to check
//     * @param jsonPath the path to return in json dot notation
//     * @return
//     * @throws IOException
//     */
//    public JsonNode getContentPath(JsonNode json, String jsonPath) 
//    throws IOException 
//    {	
//    	ObjectMapper mapper = new ObjectMapper();
//    	String[] jsonPathTokens = StringUtils.split(jsonPath, ".");
//    	// if we're at an empty node, return null
//    	if (json == null || json instanceof NullNode) {
//    		return null;
//    	}
//    	// if this is the leaf node in the path, return the entire JsonNode
//    	else if (ArrayUtils.isEmpty(jsonPathTokens) || 
//    			ArrayUtils.contains(jsonPathTokens, "*")) {
//    		return json;
//    	}
//    	// we have a valid node and path
//    	else {
//	    	String currentField = jsonPathTokens[0];
//	    	String remainingPath = StringUtils.removeEnd(StringUtils.substringAfter(jsonPath, currentField), ".");
//	    	
//    	// iterate over array
//    	if (json.isArray()) {
//	    	ArrayNode filteredJson = mapper.createArrayNode();
//	    	for (Iterator<JsonNode> iter=json.iterator(); iter.hasNext(); ) {
////	    		String childJson = mapper.writer().writeValueAsString(child);
//	    		
//	    		ObjectNode child = (ObjectNode)iter.next();
//	    		ObjectNode filteredChild = mapper.createObjectNode();
//	    		for (String filter: jsonPathTokens) {
//	    			
//	    			if (tokens.length == 2) {
//    					if (child.has(tokens[0])) {
//    						if (child.get(tokens[0]).has(tokens[1])) {
//    							if (filteredChild.has(tokens[0])) {
//	    							((ObjectNode)filteredChild.get(tokens[0])).put(tokens[1], filteredChild.get(tokens[0]).get(tokens[1]));
//    							}
//    							else {
//    								filteredChild.putObject(tokens[0]).put(tokens[1], child.get(tokens[0]).get(tokens[1]));
//    							}
//    						}
//    						else {
//    							if (filteredChild.has(tokens[0])) {
//    								((ObjectNode)filteredChild.get(tokens[0])).putNull(tokens[1]);
//    							}
//    							else {
//    								filteredChild.putObject(tokens[0]).putNull(tokens[1]);
//    							}
//    						}
//    					}
//    				} 
//	    			else if (tokens.length == 1) { 
//		    			if (child.has(filter)) {
//		    				filteredChild.put(filter, child.get(filter));
//		    			}
//	    			}
//	    		}
//	    			    		
//	    		if (!filteredChild.isNull() && filteredChild.size() > 0) {
//	    			filteredJson.add(filteredChild);
//	    		}
//    		}
//    	}
//    	else {
//    		ObjectNode filteredChild = mapper.createObjectNode();
//			// we are still traversing the object, so pass it back.
//			if (tokens.length > 1) {
//				JsonNode searchResult = getContentPath(json.get(tokens[0]), StringUtils.substringAfter(jsonPath, "."));
//				if (searchResult == null || searchResult instanceof NullNode) {
//					return filteredChild.set(tokens[0], searchResult);
//				}
//				else {
//					return null;
//				}
//			}
//			// we are at the end of the original path string
//			else {
//				// if the path exists, return the value.
//				if (json.hasNonNull(tokens[0])) {
//					return json.get(tokens[0]);
//				}
//				// otherwise, return null here so we don't need to worry 
//				// about checking for NullNodes as the result rolls back up
//				else {
//					return null;
//				}
//			}
//    	}
//    	
////			if (tokens.length == 2) {
////				if (json.has(tokens[0])) {
////					if (json.get(tokens[0]).has(tokens[1])) {
////						if (filteredChild.has(tokens[0])) {
////							((ObjectNode)filteredChild.get(tokens[0])).put(tokens[1], filteredChild.get(tokens[0]).get(tokens[1]));
////						}
////						else {
////							filteredChild.putObject(tokens[0]).put(tokens[1], json.get(tokens[0]).get(tokens[1]));
////						}
////					}
////					else {
////						if (filteredChild.has(tokens[0])) {
////							((ObjectNode)filteredChild.get(tokens[0])).putNull(tokens[1]);
////						}
////						else {
////							filteredChild.putObject(tokens[0]).putNull(tokens[1]);
////						}
////					}
////				}
////			} 
////			else if (tokens.length == 1) { 
////    			if (json.has(filter)) {
////    				filteredChild.put(filter, json.get(filter));
////    			}
////			}
////    	}
//    }
//    
//    protected Integer[] getArrayRange(String pathToken) {
//    	Pattern pattern = Pattern.compile("(?<=\\[)([^\\]]+)(?=\\])");
//    	Matcher arrayMatcher = pattern.matcher(pathToken);
//    	String arrayRange = null;
//    	
//    	while(arrayMatcher.find()) {
//    		arrayRange = arrayMatcher.group(1);
//    		break;
//    	}
//    }
//    
//    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
//
//        Iterator<String> fieldNames = updateNode.fieldNames();
//
//        while (fieldNames.hasNext()) {
//            String updatedFieldName = fieldNames.next();
//            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
//            JsonNode updatedValue = updateNode.get(updatedFieldName);
//
//            // If the node is an @ArrayNode
//            if (valueToBeUpdated != null && valueToBeUpdated.isArray() && 
//                updatedValue.isArray()) {
//                // running a loop for all elements of the updated ArrayNode
//                for (int i = 0; i < updatedValue.size(); i++) {
//                    JsonNode updatedChildNode = updatedValue.get(i);
//                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
//                    // Use-case - where the updateNode will have a new element in its Array
//                    if (valueToBeUpdated.size() <= i) {
//                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
//                    }
//                    // getting reference for the node to be updated
//                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
//                    merge(childNodeToBeUpdated, updatedChildNode);
//                }
//            // if the Node is an @ObjectNode
//            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
//                merge(valueToBeUpdated, updatedValue);
//            } else {
//                if (mainNode instanceof ObjectNode) {
//                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
//                }
//            }
//        }
//        return mainNode;
//    }
}