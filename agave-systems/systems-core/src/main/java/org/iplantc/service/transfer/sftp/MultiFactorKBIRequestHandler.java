package org.iplantc.service.transfer.sftp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.maverick.ssh2.KBIPrompt;
import com.maverick.ssh2.KBIRequestHandler;

public class MultiFactorKBIRequestHandler implements KBIRequestHandler {
		
		private static final Logger log = Logger.getLogger(MultiFactorKBIRequestHandler.class);
		
		private static final String[] KNOWN_MFA_PROMPTS = { 
			"[sudo] password for: ", 
			"TACC Token Code:",
			"select one of the following options",
			"Duo two-factor",
			"Yubikey for "
		};
		
		private String username;
		private String hostname;
		private int port;
        private String kbiPass;
        private List<Entry<String,String>> kbiResponses = new ArrayList<Entry<String,String>>();
        
		public MultiFactorKBIRequestHandler(String kbiPass, List<Entry<String,String>> kbiResponses, String username, String host, int port) {
			setUsername(username);
			setHostname(hostname);
			setPort(port);
			setKbiPass(kbiPass);
			setKbiResponses(kbiResponses);
		}
		
		@Override
		public boolean showPrompts(String name, String instruction, KBIPrompt[] prompts) {
        	log.debug("Received keyboard-interactive prompt: [" + name + "] " + instruction);
            for (int i = 0; i < prompts.length; i++) {
            	if (prompts[i].getPrompt().contains("Password:")) {
            		prompts[i].setResponse(getKbiPass());
            	}
            	else {
            		try {
            			String promptResponse = getResponseValueForPrompt(prompts[i].getPrompt());
            			if (promptResponse != null) {
            				prompts[i].setResponse(promptResponse);
            			}
            			else if (isMFAPrompt(prompts[i].getPrompt())) {
            				return false;
            			}
            			else {
            				prompts[i].setResponse("/n");
            			}
            		}
            		catch (IllegalArgumentException e) {
            			return false;
            		}
            	}
            	log.debug(getConnectionString() + prompts[i].getPrompt());
            }
            
            return true;
        }

		/**
		 * Checks the given string for the presence of any of the 
		 * user-provided keyboard-interactive prompt responses given
		 * in {@link #getKbiResponses()}. If found, that value is 
		 * used. If no match is found, {@code null} is returned.
		 * 
		 * @param prompt the message returned from a kbi prompt
		 * @return a user-supplied response if given, null otherwise.
		 */
		protected String getResponseValueForPrompt(String prompt) {
			for(Entry<String,String> response: getKbiResponses()) {
				if (StringUtils.contains(prompt, response.getKey())) {
					return response.getValue();
				}
			}
			
			return null;
		}

		/**
		 * Checks the given string for the presence of any of a known
		 * set of MFA prompts given in {@link #KNOWN_MFA_PROMPTS}.
		 * 
		 * @param prompt the message returned from a kbi prompt
		 * @return true if the prompt is a mfa challenge phrase, false otherwise
		 */
		protected boolean isMFAPrompt(String prompt) {
			for(String knownMFAPrompt: KNOWN_MFA_PROMPTS) {
				if (StringUtils.contains(prompt, knownMFAPrompt)) {
					log.debug("Found MFA prompt in the keyboard-interactive session");
					return true;
				}
			}
			return false;
		}

		/**
		 * @return the username
		 */
		public String getUsername() {
			return username;
		}

		/**
		 * @param username the username to set
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * @return the hostname
		 */
		public String getHostname() {
			return hostname;
		}

		/**
		 * @param hostname the hostname to set
		 */
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		/**
		 * @return the port
		 */
		public int getPort() {
			return port;
		}

		/**
		 * @param port the port to set
		 */
		public void setPort(int port) {
			this.port = port;
		}

		/**
		 * @return the kbiResponses
		 */
		public List<Entry<String,String>> getKbiResponses() {
			return kbiResponses;
		}

		/**
		 * @param kbiResponses the kbiResponses to set
		 */
		public void setKbiResponses(List<Entry<String,String>> kbiResponses) {
			if (kbiResponses != null) {
				this.kbiResponses.clear();
				this.kbiResponses.addAll(kbiResponses);
			}
		}

		/**
		 * @return the kbiPass
		 */
		public String getKbiPass() {
			return kbiPass;
		}

		/**
		 * @param kbiPass the kbiPass to set
		 */
		public void setKbiPass(String kbiPass) {
			this.kbiPass = kbiPass;
		}
		
		/**
		 * Prints a connection string preamble for logging system prompts.
		 * For example, <code>Prompt [jdoe@example.com:22]: </code>
		 * 
		 * @return preamble for logging keyboard-interactive prompt messages
		 */
		protected String getConnectionString() {
			return String.format("Prompt [%s@%s:%d]:\n",
					getUsername(),getHostname(),getPort());
		}
	}