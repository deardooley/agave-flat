package org.iplantc.service.jobs.dao;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class TaskDistributor
{   
    private static final Logger log = Logger.getLogger(TaskDistributor.class);
    
    public static List<List<Integer>> getDistribution(Integer[] weights, int taskCount)
    {
        List<List<Integer>> buckets = new ArrayList<List<Integer>>(weights.length);

        int s = 0, i = 0;
        for (int x = 0 ; x < weights.length; x++)
        {
            buckets.add(new ArrayList<Integer>());
            s += weights[x];
        }

        for (int nv = 0; nv < taskCount; nv++)
        {
            int r = 1 + nv / s;

            while (true)
            {
                if (buckets.get(i).size() < weights[i] * r)
                {
                    buckets.get(i).add(nv);
                    i = ++i % weights.length;
                    break;
                }
                else
                    i = ++i % weights.length;
            }
        }

        // print distribution
        for(List<Integer> bucket : buckets) {
            log.debug(StringUtils.join(bucket, " ") + "\n");
        }
        
        return buckets;
    }
}