package org.iplantc.service.transfer.sftp;

import org.apache.log4j.Logger;

import com.sshtools.ssh2.KBIPrompt;
import com.sshtools.ssh2.KBIRequestHandler;

public class PublicKeyKBIRequestHandler implements KBIRequestHandler {
		
		private static final Logger log = Logger.getLogger(PublicKeyKBIRequestHandler.class);
        
		public PublicKeyKBIRequestHandler() {
			
		}
		
		@Override
		public boolean showPrompts(String name, String instruction, KBIPrompt[] prompts) {
        	log.debug("Received keyboard_interactive prompt: [" + name + "] " + instruction);
            for (int i = 0; i < prompts.length; i++) {
            	log.debug("prompt => " + prompts[i].getPrompt());
            	System.out.println(prompts[i].getPrompt());
                prompts[i].setResponse("\n");
            }
            
            return true;
        }
	}