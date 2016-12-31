package org.iplantc.service.jobs.model.views;

/**
 * View definitions for {@link Job} and {@link JobDTO} class
 * serialization.
 * 
 * @author dooley
 *
 */
public class View {
    public static class Summary { }
    public static class Full extends Summary { }
    public static class Hypermedia extends Full { }
    public static class ExpandedPermissions extends Full { }
    public static class ExpandedEvents extends Full { }
    public static class ExpandedTags extends Full { }
    public static class ExpandedMetadata extends Full { }
}