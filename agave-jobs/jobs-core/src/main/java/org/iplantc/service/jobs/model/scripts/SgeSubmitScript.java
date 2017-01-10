/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import java.util.Arrays;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.model.Job;

/**
 * Concreate class for SGE batch submit scripts.
 * 
 * @author dooley
 * 
 */
public class SgeSubmitScript extends AbstractSubmitScript 
{
	public static final String DIRECTIVE_PREFIX = "#$ ";
	
	/**
	 * 
	 */
	public SgeSubmitScript(Job job)
	{
		super(job);
	}
	
	
	@Override
	public String getScriptText()
	{
		String result = "#!/bin/bash\n\n" 
				+ DIRECTIVE_PREFIX + "-N " + name + "\n"
				+ ( inCurrentWorkingDirectory ? DIRECTIVE_PREFIX + "-cwd\n" : "" )
				+ DIRECTIVE_PREFIX + "-V\n" 
				+ DIRECTIVE_PREFIX + "-o " + standardOutputFile + "\n" 
				+ DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n" 
				+ DIRECTIVE_PREFIX + "-l h_rt=" + time + "\n" 
				// we can remote to the system and find the correct parallel environment
				// using the "qconf -spl" command.
				+ DIRECTIVE_PREFIX + "-pe " + nodes + "way " + processors + "\n"
				+ DIRECTIVE_PREFIX + "-q " + queue.getEffectiveMappedName() + "\n";
				if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
					result += DIRECTIVE_PREFIX + queue.getCustomDirectives() + "\n";
				}
		
		return result;
	}
	
	/**
	 * Generate the queue and wayness values based on the system, queue, memory, and core count
	 * requested.
	 * 
	 * @param hostname
	 * @param requestedMemoryPerTask
	 * @param requestedCores
	 * @param queue
	 * @return
	 */
	protected String adjustProcs(String hostname, double requestedMemoryPerTask, long requestedCores, String queue) 
	{
		double actualCores;
		
		Hashtable<String, Hashtable<String, QueueDescription>> systemQueueTable = loadSystemQueues();
		
		double totalRam = requestedMemoryPerTask * requestedCores;
	
		Hashtable<String, QueueDescription> systemQueues = systemQueueTable.get(hostname);
			
		if (systemQueues == null || systemQueues.isEmpty()) return null;

		double procsNeededPerTask = Math.ceil(totalRam / requestedMemoryPerTask);
		
		//System.out.println("Request: " + totalRam + " GB : " + procsNeededPerTask);
		
		double wayness = Math.floor(systemQueues.get("normal").getMemoryTotal() / requestedMemoryPerTask);
		if (wayness > systemQueues.get("normal").getCoresPerNode())
		{
			wayness = systemQueues.get("normal").getCoresPerNode();
		}
		
		actualCores = Math.ceil(procsNeededPerTask / systemQueues.get("normal").coresPerNode) * systemQueues.get("normal").getCoresPerNode();
		
		if (wayness < 1) 
		{
			wayness = Math.floor(systemQueues.get("largemem").getMemoryTotal() / requestedMemoryPerTask);

			if (wayness > systemQueues.get("largemem").getCoresPerNode()) 
			{
				wayness = systemQueues.get("largemem").getCoresPerNode();
			}
			queue = "largemem";
							
			actualCores = Math.ceil(procsNeededPerTask / systemQueues.get("largemem").getCoresPerNode()) * systemQueues.get("largemem").getCoresPerNode();

			if (wayness == 0) wayness = 1;
			
			wayness = waynessInterval(wayness, systemQueues.get("largemem").getWayness());
		} 
		else 
		{
			wayness = waynessInterval(wayness, systemQueues.get("normal").getWayness());
		}
		
//		System.out.println("Host: " + hostname);
//		System.out.println("Cores requested: " + requestedCores);
//		System.out.println("RAM per core: " + requestedMemoryPerTask + " GB");
//		System.out.println("===========================================");
//		System.out.println("-pe " + (int)wayness + "way " + (int)actualCores );
//		System.out.println("-q " + queue + "\n\n");
//		
		return DIRECTIVE_PREFIX + "-pe " + (int)wayness + "way " + (int)actualCores + "\n" +
			DIRECTIVE_PREFIX + "-q " + queue + "\n";
	}
	
//	public static void main(String[] args) {
//		
//		SgeSubmitScript.adjustProcs("lonestar4.tacc.teragrid.org", 2, 12, "serial");
//		SgeSubmitScript.adjustProcs("lonestar4.tacc.teragrid.org", 16, 12, "normal");
//		SgeSubmitScript.adjustProcs("lonestar4.tacc.teragrid.org", 2, 90, "normal");
//		SgeSubmitScript.adjustProcs("lonestar4.tacc.teragrid.org", 1024, 1, "normal");
//		
//		SgeSubmitScript.adjustProcs("ranger.tacc.teragrid.org", 4, 16, "normal");
//		SgeSubmitScript.adjustProcs("ranger.tacc.teragrid.org", 16, 16, "normal");
//		SgeSubmitScript.adjustProcs("ranger.tacc.teragrid.org", 64, 4, "normal");
//		
//	}
//	
//	
	/**
	 * The queue parameters on the sge systems. Probably could put in a config file,
	 * but it wouldn't be consistent across systems, so we just add it in her until
	 * we really need to move it out.
	 * 
	 * @return
	 */
	private static Hashtable<String, Hashtable<String, QueueDescription>> loadSystemQueues() 
	{
		Hashtable<String, Hashtable<String, QueueDescription>> systemQueueTable = new Hashtable<String, Hashtable<String, QueueDescription>>();
		
		Hashtable<String, QueueDescription> lonestarQueues = new Hashtable<String, QueueDescription>();
		lonestarQueues.put("normal", new QueueDescription("", 23, 12, new int[]{1,2,4,6,12}));
		lonestarQueues.put("largemem", new QueueDescription("largemem", 1022, 24, new int[]{1,2,4,6,12,24,48}));
		
		Hashtable<String, QueueDescription> rangerQueues = new Hashtable<String, QueueDescription>();
		rangerQueues.put("normal", new QueueDescription("", 31, 16, new int[]{1,2,4,8,16}));
		rangerQueues.put("largemem", new QueueDescription("largemem", 255, 16, new int[]{1,2,4,8,16}));
		
		Hashtable<String, QueueDescription> longhornQueues = new Hashtable<String, QueueDescription>();
		longhornQueues.put("normal", new QueueDescription("", 47, 8, new int[]{1,2,4,8}));
		longhornQueues.put("largemem", new QueueDescription("largemem", 143, 8, new int[]{1,2,4,8}));
		
		systemQueueTable.put("lonestar4.tacc.teragrid.org", lonestarQueues);
		systemQueueTable.put("ranger.tacc.teragrid.org", rangerQueues);
		systemQueueTable.put("longhorn.tacc.teragrid.org", longhornQueues);
		
		return systemQueueTable;
	}

	private static double waynessInterval(double wayness, int[] ways) 
	{
		if (wayness == 1) 
		{
			return wayness;
		}
		else
		{
			Arrays.sort(ways);
			
			for(int x: ways)
			{
				if (x > wayness) return x;
			}
			
			return ways[ways.length-1];
		}
	}
}
