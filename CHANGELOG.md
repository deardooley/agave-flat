# Change Log
All notable changes to this project will be documented in this file.

## 2.1.9 - 2016-11-11

### Added
- METADATA: AH-146 Adding support for json-schema v4 by default.  
- METADATA: Adding update to support identical scope and publishing of metadata schema as metadata objects.  
- METADATA: Adding support for using metadata schema URL in json schema references. Service will pre authenticate the call and honor any permissions that have been set.  
- METADATA: Extending Jackson json schema support to handle internal agave URL.
- METADATA: Added support for referencing other agave metadata schema URL as `$ref` values in json schema definitions. As long as the user has permission to view the referenced metadata schema resource, its `schema` value will be fetched and resolved for in the referring document. This allows you to include all partials in the API and build reusable json schema definitions for use validating other elements.

### Changed
- FILES: AH-147 Fixed a bug copying files with readonly permissions to the same directory.
- FILES: AD-934 Fixed a bug rendering the `_links.self.href` value returned on file indexing results.
- APPS: AD-943 Fixed a bug allowing special characters in the file name.  

### Removed
- nothing


## 2.1.9 - 2016-11-02

### Added
- nothing

### Changed
- FILES: Fixed bad mime-type resolution on public file downloads

### Removed
- nothing


## 2.1.9 - 2016-10-30

### Added
- nothing

### Changed
- FILES: Fixed bug resolving tenant from public file download requests.  
- FILES: Fixed error response for malformed public download requests.  
- FILES: Fixed ownership resolution and logical file creation for unknown publicly shared file items. 
- FILES: Fixed a bug in parsing of `x-forward-host` header to identity tenant by any matching hostname in the header.  
- NOTIFICATIONS: Added conditional logic for the `JobsNotificationEvent` class to properly resolve messages for slack publication.
- NOTIFICATIONS: Added proper filtering and formatting based on event status, text, and event type to publish messages as properly marked up Slack messages.
- JOBS: AH-143, AD-898 Refactored job monitoring by breaking out the response parsing from the remote host into a separate package similar to the way job submission responses are parsed for job ids. This allows for more flexibility handling the customizations common to many schedulers.
- JOBS: AH-144 Added code to pipe the response from the Agave status callbacks into `AGAVE_LOG_FILE`, which defaults to `$(pwd)/.agave.log`. This allows the job error logs to remain empty when the job runs cleanly.
- JOBS: Fixed a bug in the job submission process where public apps could have overly aggressive command stripping enforced by agave.
- JOBS: Updating slum job behavior to ignore empty responses from the `sacct` command.  
- JOBS: Updating field names used when querying slurm job statuses. Was JobIDRaw. Now JobID.  

### Removed
- nothing


## 2.1.9 - 2016-10-13

### Added
- nothing

### Changed
- SYSTEMS: AH-137 Updating response on system setDefault and unsetDefault responses to reflect the current system settings rather than the stale previous system settings. This prevents stale responses even when `HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE` is set to `true` in the container.
- SYSTEMS: AD-858 Fixed a bug causing the wrong status to be written to the history log on role change.
- SYSTEMS: AD-857 Fixed a bug causing system permission revocations not to persist.
- JOBS: AH-74 Updated parsing of `AGAVE_JOB_CALLBACK_NOTIFICATION` template macro and the regex used to parse the various options out of a job template script.
- JOBS: AH-73 `JOB_RUNTIME_CALLBACK_EVENT` event was not being sent for jobs.
- 

### Removed
- nothing


## 2.1.8 - 2016-10-08

### Added
- FILES: Adding url query based search by username, permission to file permissions collection
- FILES: Added support for recursive permission revocation by specifying a username of `*` in the POST operation.
- FILES: Added support for recursive permission deletion by adding `recursive=true` to a DELETE request
- JOBS: Added configurable retries on the condor side when jobs are preempted.
- JOBS: Added support for the `docker` Condor universe and dynamic argument building based on agave app parameters. order is preserved. full command line argument is built.
- JOBS: Added `CUSTOM_CONDOR` scheduler for specifying everything but input log, error, and output files, executable. This gives better control over requirements, etc.
- JOBS: Added support for filtering the `customDirectives` on condor systems for inclusion in the condorSubmit script used to kick off a condor job.
- JOBS: AH-134 Switched to joda time for all date handling in the jobs api. There was an ongoing issue caused by dates being formatted from the db in the wrong time zone, then persisted with the new, incorrect time zone. We fixed this in two places. First, we ensured all dates were written with new DateTime().toDate() rather than new Date(). This kept timezones defined at runtime being honored. Second, we updated the container jdbc connection to use `useLegacyDatetimeCode=false`.

### Changed
- ALL: Fixed bug in runtime configuration service preventing runtime settings from being formatted.
- FILES: AH-127 Fixed a bug preventing files from being copied unless the requestor has WRITE permissions.
- FILES: Fixed bug where unknown folders were assigned a type of "raw" rather than "dir".
- PROFILES: Fixed inconsistency between java and python profiles apis so that they both support underscore field names in the search query.
- JOBS: Fixing `_links.self.href` hypermedia URL in all job permissions objects to include the username in the URL query for direct reference.
- JOBS: Set default requirements to `( OpSys == "LINUX" )`
- JOBS: Updated condor support to allow for overriding the default universe and adding custom directives.
- SYSTEMS: Updating IRODS4 IRODSAccount and IRODSAccessObjectFactory attributes to threadlocal singleton objects. This prevents a NPE from being thrown when reusing the object after a disconnect() has been called.
- SYSTEMS: Reverting to use of IRODS4.stat(String) to check for existence over IRODS4.getFile(String) to avoid ambiguous socket failures.

### Removed
- nothing



## 2.1.8 - 2016-09-22

### Added
- COMMON: Added explicit date handling in the hibernate mapping and JNDI connection to force `America/Chicago` timezone.

### Changed
- APIDOCS: Updating the container init script to update the service url to the point to the public tenant.  
- SYSTEMS: Relaxed StorageConfig and LoginConfig validation to allow systems to be updated without having to provide the credentials again. This addresses a large usability issue centered around effectively delegating system maintenance to another person without having to give them the system credentials.  
- FILES: Fixing typo in deletion history event on a copy operation.  
- JOBS: Pushed a fix for parsing of condor job ids after submission.  

### Removed
- nothing



## 2.1.8 - 2016-09-15

### Added
- SYSTEMS: Adding code to handle MFA prompts for warnings and requirements for MFA tokens when using SSH/SFTP. This patch will respond to non-password challenges with a carriage return for each subsequent prompt. In the case of TACC's pseudo MFA, this will accept the countdown prompt and allow auth to complete. In the case of actual MFA prompts from TACC MFA or other MFA solutions, a black list of known prompts from Yubikey, Google Authenticator, Duo, RSA, and TACC Auth is consulted. If found, the auth attempt is terminated with responding to the MFA prompt so user's don't get their accounts locked for excessive failures. This works for password and ssh key access alike.
- SYSTEMS: Added same MFA changes into the MaverickSSHSubmissionClient for all remote access.

### Changed
- METADATA: AH-123 Adding `privileged` query parameter to metadata listings and searches. When `privileged=false`, the search will be performed as the requesting user without any implied permissions imposed on the results. 
- STATS: Adding check for chicken and egg function definition when looking for environment variables.

### Removed
- nothing



## 2.1.8 - 2016-10-14

### Added
- SYSTEMS: Adding code to handle MFA prompts for warnings and requirements for MFA tokens when using SSH/SFTP. This patch will respond to non-password challenges with a carriage return for each subsequent prompt. In the case of TACC's pseudo MFA, this will accept the countdown prompt and allow auth to complete. In the case of actual MFA prompts from TACC MFA or other MFA solutions, a black list of known prompts from Yubikey, Google Authenticator, Duo, RSA, and TACC Auth is consulted. If found, the auth attempt is terminated with responding to the MFA prompt so user's don't get their accounts locked for excessive failures. This works for password and ssh key access alike.
- SYSTEMS: Added same MFA changes into the MaverickSSHSubmissionClient for all remote access.

### Changed
- nothing

### Removed
- nothing



## 2.1.8 - 2016-09-06

### Added
- nothing

### Changed
- NOTIFICATIONS: Fixing bug in notifications query where deleted and expired notifications are returned by default. Now expired notifications are only returned if the `visible` value is included in the query.
- JOBS: AH-120 Added better exception handling for `InvalidUserException` on permission assignments.
- JOBS: AH-121 Fixed bug in cli submission script caused by using `[[` with the `sh` command. On other linux distributions, `sh` maps to `bash`. On Ubuntu systems, `sh` maps to `dash` which does not support the `[[` syntax. By switching the use of `[[` to `[`, the problem is solved. 

### Removed
- nothing



## 2.1.8 - 2016-08-30

### Added
- nothing

### Changed
- SYSTEMS: Updated IRODS4 connection handling to properly pool and recover from connection failures.
- SYSTEMS: Added IRODS4 connection logging

### Removed
- nothing



## 2.1.8 - 2016-08-29

### Added
- COMMON: Added caching of tenant public signing key to JWT validation process if VERIFY_JWT_SIGNATURE is set to true in the container config.
- COMMON: Adding setting for optional JWT validation based on public keys available from the tenants api.

### Changed
- SYSTEMS: Used new checked exception RemoteDataSyntaxException and return HTTP 400 instead of 409
- FILES: More accurately report the cause of a files-move failure.
- FILES: AH-104 Avoid adding preceding slash when one already exists.
- NOTIFICATIONS: AD-686 Fixing broken notification search due to bad sql query
- METADATA: AD-706 Updating hypermedia response from metadata API to include proper `associationIds` with resolvable types, `owner`, and `permissions` fields on metadata creation, update, and listing.

### Removed
- nothing



## 2.1.8 - 2016-08-21

### Added
- MIGRATIONS: Added migrations to remove redundant unique index on primary key.
- MIGRATIONS: Added migration to add usage keys for uuid service.

### Changed
- MIGRATIONS: Fixed NPE in the NewNotificationMessageQueueListenerTest class. The test still fails on exception checks, but those are valid bugs to fix in the logic, not test bugs.

### Removed
- nothing 


## 2.1.8 - 2016-07-09

### Added
- UUID: Adding internal UUID API for resolving UUID and generating new ones on demand.
- UUID: Adding swagger doc for UUID api in project base. Available online at [https://swaggerhub.com/api/deardooley/uuid/1.0](https://swaggerhub.com/api/deardooley/uuid/1.0)
- UUID: Added migrations to support usage records on uuid api.

### Changed
- nothing

### Removed
- nothing


## 2.1.8 - 2016-07-03

### Added
- JOBS: Added CUSTOM_PBS scheduler option.

### Changed
- JOBS: AH-103 Fixing a bug causing FORK jobs to hang in perpetual `SUBMITTING` status when the chmod command failed or the exec did not succeed. The issue was a weak one-liner to fork the job. I rewrote the command with checks for both pid and pid file (in case the job legitimately ran very fast), success of chmod, and better error output in both situations.
- POSTITS: Fixed bug in posits causing listings without search options to fail.

### Removed
- Nothing change.


## 2.1.8 - 2016-07-01

### Added
- ALL: Added a third `devproxy` config for interacting with services running in debug mode from your IDE within a single tomcat instance.
- JOBS: Added CUSTOM_PBS scheduler option.

### Changed
- ALL: AH-97 Refactored the compose scripts in the main checkout to make working in a dev and lb environment a bit easier. Two proxies are available for use. `traefik` provides load balancing and auth-registration of services. `proxy` is the current apache proxy solution using static ports. The primary difference, aside from scaling and downtime, is that the `static/docker-compose.yml` config will startup with support for jmx and remote tomcat debugging allowing you to interact with the container directly, or via a proxy. 
- POSTITS: Fixed bug relaying file content when requested from firefox. This had to do with the nature of the way we're proxying content back through the client. We had been forwarding headers from the client request through to the backend API untouched, then injecting any additional headers as needed when the response came back. This was performant, but caused an issue when compression was used because both the web server running the posits api and the web server fronting the files api were compressing the response, thus double-compression occurred and corrupted the response. Fixed this by stripping the `Accept-Encoding` header from the client request and letting the postit web server handle it appropriately.
- POSTITS: Fixed a double slash bug building the proxy URL for the request.  
- POSTITS: Added rudimentary search support.  
- JOBS: AH-100 Fixed a bug that caused an extra batch scheduler directive to be written into all PBS, TORQUE, and CUSTOM_TORQUE batch job scripts.  
- JOBS: AH-101 Fixed a bug causing all jobs submitted with scheduler type CUSTOM_TORQUE, CUSTOM_GRIDENGINE, CUSTOM_SLURM, and CUSTOM_PBS to fail due to the wrong parser being used to identify the job id after submission. All custom scheduler types have added to the `RemoteJobIdParserFactory` class used to assign a job id parser.  

### Removed
- Nothing change.


## 2.1.8 - 2016-06-30

### Added
- NOTIFICATIONS: Adding notification id to the realtime channel messages sent to web socket subscribers.
- SYSTEMS: Added `CUSTOM_PBS` as an alias for `CUSTOM_TORQUE` scheduler type.

### Changed
- ALL: Flattened submodules into a single code base from which all services can be built.
- ALL: Updated compose definition to use a [traefik|http://traefik.io] proxy to front APIs.
- ALL: Moved to Compose API v2 and Docker Engine 1.12 networking.
- SYSTEMS: Fixed bug in the PBS batch script generator causing `#PBS -q normal` to be included in every set of directives regardless of the actual queue specified. Deleting this.

### Removed
- Nothing change.


## 2.1.8 - 2016-06-19

### Added
- No change.

### Changed
- FILES, JOBS, TRANSFORMS: Fixing url encoding to work around discrepancies in the url decoding of path components between different OS. This works in combination with an update to the `agaveapi/java-api-base:alpine` image.
- FILES: Fixed a UX issue where specifying recursive permissions on a file caused an error. This is now ignored and the permission is applied to the file.
- MONITORS: Fixed a bug allowing monitors to run at too frequent an interval manually.  
- MONITORS: Started explicitly setting or updating the next scheduled check time when a monitor is created or updated so users can fire the check immediately to test without violating the minimum check interval quota. This will happen under any of the following conditions: 1. creating a new monitor, 2. Updating a monitor which has not run yet, or 3. Updating an existing monitor to run more frequently whose previous next scheduled check would be further away than the current time plus the new frequency interval. Making this change prevents the service from negatively delaying scheduled checks on updates.

### Removed
- Nothing change.


## 2.1.8 - 2016-06-15 

### Added 
- No change.

### Changed
- FILES, JOBS: Further cleaning up and localizing several remote data client connections throughout both APIs to ensure connections are closed immediately after opening. This has a couple implications. First, as a plus, it reduces the time over which a connection needs to be maintained to any individual system. This is helpful in situatoins where connection throttling on the remote host is an issue. Second, as a minus, it increases the number of times that a connection needs to be established. This usually isn't a problem, but for slower systems, this will bump the time needed to get a job into queue by 2x the time to establish a connection. Not a big deal, but worth noting.
- FILES: Cleaning up the connection error responses for public file downloads so, in the event a download fails, users unfamiliar with the API will have a better idea why they didn't get the data they thought they were going to get. 

### Removed
- Nothing change.


## 2.1.8 - 2016-06-12 

### Added 
- JOBS: Added two new fields to control the strictness with which resubmission occurs. `ignoreParameterConflicts` and `ignoreInputConflicts` allow any conflicts between the previous hidden field values and the current ones to be ignored and replaced with the current values. In doing this, the job will not be reproducible, however it will be much more helpful in situations where you simply want to keep rerunning jobs regardless of changes the app.  
- JOBS: Adding submit scripts for new custom scheduler types. `CustomGridEngineSubmitScript`, `CustomTorqueSubmitScript`, and `CustomSlurmSubmitScript`. These will add the job name, output file , and error file, then resolve all macros in the customDirectives batch queue field and append it to the scheduler directive section of the resolved wrapper template. This is helpful for customizations for scheduler extensions and non-standard configurations.
- JOBS: Added support for new BatchQueue.mappedName field. If specified in the system queue definition, the mappedName will be used in the batch submit script rather than the name. Otherwise, the name will be used. The `AGAVE_JOB_BATCH_QUEUE` wrapper template variable will resolve this way as well. 
- APPS: Added history url to the apps hypermedia response. This resolves issue AH-59 
- APPS: Fixed provenance tracking in apps service. 
- APPS: Added additional history records to the source app when it's published or cloned. This resolves issue AH-60  
- APPS: Added additional records to previously published apps when a newer version is published. This resolves issue AH-60  
- APPS: Added checksum to history message when an app is published. This resolves issue AH-60  
- APPS: Added created history record when an app is published. This resolves issue AH-60  
- METADATA: Expanding hypermedia response value for `associatedIds` to return an array of expanded objects. Rather than just return a link, we now resolve each of the associatedIds to an object with a `href` to the resource, a `rel` field equal to the uuid of the resource, and a `title` field equal to the resource type (file, system, app, etc.). This avoids multiple uuid of the same resource type overwriting each other. 
- MONITORS: Added history url to the apps hypermedia response. This resolves issue AH-59 
- MONITORS: Added PUT support on a monitor to allow easy enable/disable of monitors.
- NOTIFICATIONS: Added support for specifying agave url as callback targets for notifications. When doing this, the event creator will be used as the authenticated user in the callback to the callback target. In doing this, it is possible to chain together events and orchestrate actions to create rich, distributed workflow scenarios. Let the inception begin.  
- NOTIFICATIONS: Fixing bug preventing notifications from updating properly. This resolves issues AD-465
- SYSTEMS: Added history url to the apps hypermedia response. This resolves issue AH-59 
- SYSTEMS: Fixed issue copying files/folders with spaces in the name on sftp systems. We were previously escaping the space. Now we quote the paths, so escaping is no longer needed. This resolves issue AH-53
- SYSTEMS: Added three new scheduler types, `CUSTOM_GRIDENGINE`, `CUSTOM_SLURM`, and `CUSTOM_TORQUE`. These allow for greater customization of the scheduler directives through the `customDirectives` field in a BatchQueue. Only the job name, output, and error directives will be added by agave in jobs submitted to systems with these scheduler types. Everything else is user-supplied. This is helpful if you would like to inject your own predetermined values for cpu utilization, pthread algorithm, custom directives, scheduler extensions, etc. If a system is defined with a custom scheduler type and a queue has no `customDirectives` value defined, the legacy behavior will apply. This resolves issue AD-424
- SYSTEMS: Added `mappedName` and `description` fields to BatchQueue representation to allow multiple queue definitions to map to a single physical queue on a remote system. If no `mappedName` field is provided, the `name` field is used to maintain legacy behavior. The `name` field is still checked for uniqueness within a system, however a uuid is used internally to identify them. This allows us in an upcoming release to assign user and group ACL to batch queues within a system rounding out the quota support. This resolves issue AH-424
- SYSTEMS: Adding owner field to the main execution and storage system json response. This resolves issue AH-53
- SYSTEMS: Adding new batch queue `mappedName` and `description` fields to the execution system detailed response.
- SYSTEMS: Fixed error where a global default system would remain the global default after unpublishing.  
- SYSTEMS: Added `owner` to full system description response.

### Changed
- NOTIFICATIONS: Fixed bug preventing notifications from updating. This resolves issue AD-465  
- NOTIFICATIONS: Fixed a bug where web hook calls would occasionally be called twice when successful.  
- MONITORS: Fixed bug preventing system status from updating on monitor change. This resolves issue AH-72
- METADATA: Fixing bug where owner was rewriting on metadata update by a shared user. This resolves AH-62  
- METADATA: Updating several error responses for consistent terminology between routes.
- METADATA: Fixing bug where public and world readable metadata items were not returned in search results. This resolves issue AH-68
- METADATA: Fixed bug where metadata searches did not honor pagination. This resolves issue AH-49 
- METADATA: Closing policy hole where standard users could publish metadata items and clutter things for everyone.
- METADATA: Closing policy hole where standard users could publish metadata schema items and clutter things for everyone.
- JOBS:Fixed bug in form generation resource to allow semantically correct form generation to run a specific app.  This resolves issue AD-442  
- JOBS: Fixed bug in job resubmission action that manifested when the app had a hidden field. The issue was that when the job was resubmitted, it was serialized and processed as a new job. This would include all hidden parameters and inputs as well. When the app had hidden inputs or parameters, they were validated along with all the others. Since hidden inputs or parameters are rejected as fields in a job request, every resubmission involving an app with hidden inputs or parameters would fail. While we could have just overwritten the values to fix it, doing so would violate reproducibility because we would be changing the original inputs or parameters values. So, what we needed to do was add some flexibility into the resubmission process. What we ended up doing was...  
- JOBS, FILES, TRANSFERS, APPS: Refactoring code to explicitly force close connections after they are unused. I suspect Jargon isn't effectively handling tcp timeout and keep alive in their connection management when the network is unstable. This could lead to the timeout issues we're seeing. To avoid all that we'll force close connections everywhere and prevent Jargon from doing anything related to connection or session management.
- JOBS: Fixing bug forcing all jobs to archive.
- JOBS: Fixed job permission clearing. Bad sql statement. This fixes issue AD-456 
- JOBS: Fixed a bug in the queue and job constraint processing which caused all jobs to get assigned to the system default queue.This resolves issues AH-64 AH-67 
- FILES: Fixing failure listing permissions on default system. This resolves issue AD-412
- FILES: Fixed parsing of json input on file upload to allow for null and missing values. This resolves issue AH-73
- FILES: Fixed parsing of notification on upload. 
- FILES: Fixed processing of callbackUrl and callbackURL.
- FILES: Verified HTTP redirects are followed on file imports. This closes issue AH-36
- FILES: Added notification array to hypermedia response of file import/upload. This provides the caller a handle on the notification resources created as a result of the notification bundled in the import/upload request. 
- SYSTEMS: Fixing bug not being able to unset permissions on default system. This resolves issue AD-412  
- SYSTEMS: Fixed bug looking up non public default system for user. This resolves issue AD-412  
- SYSTEMS: Fixing bug in sql query to perform recurisive permission updates. The issue was hibernate blows. This resolves issue AH-75   
- SYSTEMS: Fixed response filtering in search queries.

### Removed
- APPS: Removing use of 304 Not Modified from the apps permission responses as it violates the use of the response code in the http spec.
- TAGS: Removing use of 304 Not Modified from the tags permission responses as it violates the use of the response code in the http spec.


## 2.1.8 - 2016-05-09 

### Added 
- ALL: Added response filtering to all java APIs. Adding `filter=field1,field2,` will filter all response object to only include `field1` and `field2`. Dot notation is supported. Supported fields include all fields in the original json resources returned from each service. To get back full resource descriptions rather than just summary responses, use `filter=all`. 
- ALL: Added global parameters to limit max results. Currently set at a max of 500 across all tenants.
- TENANTS: Added `limit`, `offset`, and `naked` url query support to responses.
- FILES: Added new `files/v2/index` collection for active indexing of a file or folder. The response will perform a listing, but assign uuid to all file items and return full hypermedia responses.
- FILES: Added uuid to file events.
- FILES: Added support for propagating file events to systems and apps resources
- FILES: Added support for passing in serialized notifications on file uploads.
- FILES: Added support for importing URL from within the tenant and URLs with the `agave` schema.
- JOBS: Adding support for generic GRIDENGINE scheduler type.
- NOTIFICATIONS: Added full JSON description of all resources involved in an event to the webhook callback. This provides full context over what happened where, when, and who caused it.
- NOTIFICATIONS: Added retry policy support for user-defined retry policy on notification delivery. 
- NOTIFICATIONS: Added dead letter queues for undeliverable notifications.
- NOTIFICATIONS: Added `notifications/v2/<uuid>/failures` collection for querying and searching failed notification deliveries.
- POSTITS: Added the destination target URL added to the hypermedia response.
- POSTITS: Added `limit`, `offset`, and `naked` url query support to responses.
- REALTIME: preview release of new realtime api for receiving events via streaming and websocket connections.
- SYSTEMS: Added new `systems/v2/<id>/history` collection for tracking changes to system history. 
- SYSTEMS: Added propagation events to apps registered on an execution system when content and/or role changes occur.
- SYSTEMS: Added `lastUpdated` to system json response.
- TAGS: preview release of new Tags API.


### Changed
- ALL: URL coded hypermedia responses from all services to ensure they can be reused in requests to the api as is.
- ALL: Standardizing on the access/error log output from all services.
- APPS: Fixing ontology search queries.
- FILES: UTF-8 support on file and directory paths.
- FILES: Improved head support and performance across the board.
- FILES: Fixed bug in file upload where a missing destination folder would not throw a 404.
- FILES: Fixed a bug where the name of the new directory returned in the response to a mkdir operation was incorrect. 
- FILES: Fixed bug where listing a deleted file that currently still had a logical file threw an internal error. This can happen when a file is deleted, but currently in process of being uploaded.
- FILES: Fixed mime type resolution on file downloads. 
- FILES: Improved performance of recursive permission operations by applying bulk updates to all logical file records. Events are only thrown on the actual filleted receiving the grant.
- FILES: Improved content disposition handling on all file download apis.
- FILES: Fixed file upload notifications by adding notifications for additional events and failures during file uploads.
- FILES: Fixed a bug preventing staging tasks from propagating ownership info.
- FILES: Fixed a bug preventing agave UUID and HTTP imports from co-existing.
- FILES: Fixed a bug where file permissions were displayed with the system accessibility factored in. File permissions are independent from system permissions, thus only the file permission should be shown when requested.
- FILES: Fixed tenancy bleed in file upload tasks.
- FILES: Fixed bug where notifications were not sent on file upload.
- Fixed a bug where failed notifications were not send on file upload and import.
- Fixed a bug where download events were not sent on file download.
- FILES: Refactored recursive file permission updates to perform bulk database updates and not send events for every impacted file in the FileItem tree. Only the FileItem on which the permission grant was made will receive the event.
- FILES: Fixed bug where staging tasks were not honoring tenant exclusions set in the container config.
- FILES: Refactored LogicalFile convenient event processing methods to reduce the number of redundant events written to the history.
- FILES: Fixed bug in url path encoding that stripped the leading slash from absolute paths.
- JOBS: Added faster HEAD support for all job output download endpoints. This should be used in leu of a job output listing when sniffing content size, name, and media format.
- JOBS: Added support for processing the `AGAVE_JOB_CALLBACK_NOTIFICATION` template macro. This allows ones to send user-defined runtime data as a custom event to which they can subscribe in their job.
- JOBS: Improving exception handling on job input staging by checking for system availability.
- JOBS: Fixing bug where previous job output paths could not be used as inputs if there were spaces in the path element names.
- JOBS: JOBS: Fixing bug where hypermedia response to file outputs was not url encoded.
- JOBS: Fixed a bug where connections on file downloads would hang waiting of the connection to close.
- METADATA: Fixed bug preventing shared metadata items from showing up in the general collection listing.
- METADATA: Fixed permission performance and cleanup on queries.
- MONITORS: Added better event propagation and notificaiton of status changes when monitoring systems. 
- PROFILES: Fixed bug in hypermedia query where the profile and internal user urls were not resolved to the calling tenant.
- POSTITS: Rewrote proxying function to the target response headers are returned directly from the destination target.
- POSTITS: `force` is accepted as a query parameter to the postit url to force a download of the target content independent of whether it was originally included in the destination target url.
- POSTITS: Content-Range headers are supported and properly relayed to remote destination.
- POSTITS: 
- SYSTEMS: Improved IRODS session handling by migrating to a singleton class.
- SYSTEMS: Fixed bug preventing login on servers running the latest openssh 
- SYSTEMS: Shrunk size of large free text field in labels and description.
- SYSTEMS: Fixing batch queue details requests from responding with a 500 when the queue no longer exists.
- SYSTEMS: Dropping IRODS max buffer size to line up with Jargon internal size to reduce number of round trips when proxying data.
- SYSTEMS: Reording preferred key exchange algorithms used by ssh and sftp library. This restores compatibility with OpenSSH > 6.9p1 and OpenSSL > 1.0.2c


### Removed
- POSTITS: Removed redundant headers from response since CORS si built into parent container.



## 2.1.6 - 2016-03-11 

### Added 
- POSTITS: added support for `Content-Range` header to be relayed in PostIt redeptions.

### Changed
- ALL: Changing jargon property to set `usingSpecQueryForDataObjPermissionsForUserInGroup = true` by default on all irods3 interactions. This is a change from the previous value in that it assumes a different configuration on the target system. Rolled in for CyVerse. This closes #118

### Removed
- nothing


## 2.1.6 - 2016-03-07 

### Added 
- JOBS: added `JOB_CREATED`, `JOB_UPDATED`, `JOB_FINISHED`, `JOB_FAILED`, `JOB_PAUSED`, `JOB_QUEUED`, and `JOB_STOPPED` as events thrown to a job's `ExecutionSystem`. 
- JOBS: added `JOB_CREATED` as an event thrown to a job's `Software`. 

### Changed
- ALL: Fixed bug where services started as headless workers threw an exception due to NEP from the custom scheduler object never being initialized. 
- POSTIT: Fixed bug where URL encoded targets where decoded prior to persisting and, as a result, could not resolve properly when the postit was redeemed. 
- POSTIT: Fixed bug where the `force` url query parameter was not honored by the posits service. This resulted in the content-disposition header not being set and the content-type not being set to application/octet-stream. 
- SYSTEMS: Added programmable `jargon.properties` to the IRODS3 driver to improve TCP performance and work around a NPE bug in the jargon property parsing library. 
- SYSTEMS: Added retry when sockets are unexpectedly closed during a `IRODS.stat()` operation. This attempts to address about 80% of the IRODS 3 issues. 
- JOBS: Refactoring JobEventProcessor to send job and system/software objects in message. 
- JOBS: Added resiliancy to the json serialization of job events. 
- JOBS: Fixed bug preventing job notifications from sending to `Software` and `ExecutionSystem` events. 
- NOTIFICATIONS: Updated JCE policy files to support longer keys and more ciphers. Resolves AD-279. 
- METADATA: Fixed bug causing duplicate metadata owner permission object to be returned when querying the metadata permissions collection. Resolves AD-253. 
- METADATA: Fixed bug allowing metadata owner permissions to be saved as a separate ACL rather than defaulting on the ownership field. 
- METADATA: Fixed metadata `_links.self.href` URL to resolve to actual tenant URL. Resolves #112. 
- MONITORS: Fixed bug where message queue client would not close connections on RunTimeException.  
- TRANSFERS: Fixed bug where message queue client would not close connections on RunTimeException. 

### Removed
- NOTIFICATIONS: Removed support for calling webhook endpoints using SSLv2 or SSLv3.


## 2.1.6 - 2016-01-31

### Added
- FILES: support for IRODS v4.x
- FILES: support for ssh keys with passwords

### Changed
- FILES: Fixed bug pulling credentials from multiple myproxy servers.
- FILES: Fixed bug pipelining files via gridftp
- FILES: Fixed bug querying s3 buckets where metadata ACL do not grant read permissions.
- FILES: Changed the file size threshold for proxied copies from IRODS.
- JOBS: Fixed bug where systems notifications were not sent on job creation, submission, and completion for a given system.
- POSTITS: Fixed bug impacting the behavior of the PostIts API when the `noauth` parameter was used. Now any truthy value will set the `noauth` to true and result in an unauthenticated request to the backend system.
- POSTITS: Updated the `max_execution_time` time from 30 seconds to 6 hours to support larger file downloads via PostIt redemption.

### Removed
- nothing


## 2.1.5 - 2015-12-23

### Added
- nothing

### Changed
- SYSTEMS: Fixed bug in default and global default system lookup where users with common usernames across multiple accounts did not have tenancy honored.
- JOBS: Added cache invalidation to job permission query to avoid false negatives when first requesting a file download from a job folder. This resolves issue #99
- FILES: Fixed bug in file URL parsing causing spaces to be escaped prior to resolution. This resolves issue #105.

### Removed
- nothing


## 2.1.5 - 2015-12-15

### Added
- SYSTEMS: Added `maxRequestedTime` to batchQueue response objects.
- JOBS: Adding JOB_OWNER, JOB_TENANT, JOB_ARCHIVE, and JOB_ARCHIVE_PATH to the available job template macros.  

### Changed
- ALL: Updated mongodb in compose file to bootstrapping agaveapi/mongodb:2.6 image.
- ALL: Updating java APIs to write .war file to `$CATALINA_HOME` rather than a specific path. 
- LOGGING: Switched logging api to smaller alpine php base image. 
- USAGE: Switched usage api to smaller alpine php base image. 
- DOCS: Switched docs api to smaller alpine php base image. 
- JOBS: Fixing response from individual file in job output listing. Name and path were not resolved properly.  
- JOBS: Fixed permission value returned in job output listing. Was not updating  value after doing permission math.  
- FILES: Updating files api to use StringUtils.replace rather than String.replace so we can do static value replacement rather than regex. This is helpful when user values contain regex reserve characters. 
- SYSTEMS: Fixed server side copies of sftp systems when rootDir is not the system root. A directory check was getting called against an absolute path rather than a relative one, thus causing things to 404.

### Removed
- nothing

 
## 2.1.5 - 2015-11-16

### Added
- nothing

### Changed
- JOBS: Fixing bug where the transfer task on a job output file download was not saved prior to the logical file being saved. This caused a problem persisting new logical field and resulted in downloads to file.

### Removed
- nothing


## 2.1.5 - 2015-11-10

### Added
- NOTIFICATIONS: Adding custom headers to all notifications sent from the platform. For HTTP requests, the following headers will be added: 
> * **User-Agent**: a custom user agent value is not provided. This reflects the Agave notification client making the request on the remote URL. ex. `"User-Agent": "Agave-Hookbot/62d327e3-6db9-43d7-b52c-a6e11260016e"` 
> * **X-Agave-Delivery**: this is a unique identifier for the request. Every request will have a unique identifier which you may reference when debugging any failed calls. ex. `"X-Agave-Delivery": "4789012708851445275-b0b0b0bb0b-0001-042"`
> * **X-Agave-Notification**: this is the id of the Agave Notification resource which produced this HTTP request. ex. `"X-Agave-Notification": "1183686048393466341-b0b0b0bb0b-0001-011"`  

  The same headers will be added to email notifications, though they will not appear in the email content.  
- NOTIFICATIONS: Adding MACROs to all monitoring events to enable the full "lastCheck" object to be referenced. `TYPE`, `LAST_CHECK_ID`, and `LAST_MESSAGE` are supported in addition to all the existing MACROS.

- MONITORS: Added `type` as a url query parameter to monitor checks
- MONITORS: Adding monitor JSON into the web hook request body.
- MONITORS

### Changed
- MONITORS: Fixing HAL response on monitor checks
- MONITORS: Fixing monitor check listing endpoint and monitor check resource retrieval. Search and fetch by UUID was not working.
- MONITORS: Fixing event propagation on login failure.
- SYSTEMS: Fixing collection listing of batch queues. The limit and offset parameters were juxtaposed.
- SYSTEMS: Fixing logging for second leg of relay transfer so destination and protocols are properly logged.
- SYSTEMS: Updating worker url on relay transfer operations to point to https://worker.prod.agaveapi.co
- MONITORS: Fixed race condition resulting in `lastCheck` object in serialized monitor POSTed in a web hook request always being the check from the previous run. Now the check that just ran will always be included.
- JOBS: Fixing template MACRO replacement in a couple situations where String.replaceAll() was used swapped out for StringUtils.replace(), but the regex values were not updated to reflect the string matching vs regex matching. 

### Removed
- nothing


## 2.1.5 - 2015-11-05

### Added
- Updated build and deployment instructions in README.

### Changed
- POSTITS: Somehow the HAL and tenancy functions got rolled out of the branch. Adding them back in. This restores listing responses.

### Removed
- nothing


## 2.1.5 - 2015-10-28

### Added
- nothing

### Changed
- JOBS: Fixed an oversight in the template variable resolution where punctuation was not honored.

### Removed
- nothing



## 2.1.5 - 2015-10-26

### Added
- nothing

### Changed
- APPS: Updating app cloning action to use the atomic file permission creation on deploymentPath creation.
- APPS: Fixed a bug where app assets were not explicitly granted permissions. This could have been an issue when cloning assets onto public systems.
- APPS: Fixed a bug where the user-supplied deploymentPath was not picked up when cloning and thus failed.
- JOBS: Updating data queues to use the atomic file permission application on directory creation.
- JOBS: Fixed a bug where archivePath could be set to null and result in permissions being set incorrectly.
- JOBS: Fixed a bug where multiple input files were overwriting each other and creating an anti-race condition.
- FILES: Fixing bug where admins couldn't identify specify private systems they owned when doing file listings. Bad sql...

### Removed
- nothing



## 2.1.5 - 2015-10-23

### Added
- APPS: App JSON body is now passed in to webhook calls.
- APPS: Adding `parameters.type` value to the possible app search attributes.
- APPS: Adding hypermedia to software events. 

### Changed
- APPS: Fixing permission, asset searching, naming, system validation, etc on app cloning.
- APPS: Cleaning up exception throwing to ensure root throwable is propagated.
* JOBS: Rewrote archiving logic to account for relative paths in the agave ignore file.  This resolves Issue #93
* JOBS: Updated checks during archiving to make sure file/folder is present  
* SYSTEMS: Fixing bug in URLCopy preventing blacklist from being honored in directory copies.  
* SYSTEMS: Fixing bug in system credential serialization where default system credentials generated hypermedia links with "null" in the internalUsername position.  
 
### Removed
- APPS: Removing CLONING_FAILED and PUBLISHING_FAILED events from the app history. These are just clutter. Notifications will propagate, however.


## 2.1.5 - 2015-10-22

### Added
- NOTIFICATIONS: Added support for specifying custom event when performing a forced notificatino event.
- DOCS: Adding swagger 2.0 definition to docs. Default is still 1.2

### Changed
- FILES: Fixed bug preventing parent folders from receiving CONTENT_CHANGE events when data was uploaded or changed.
- FILES: Fixed file upload error caused by no transfer listener being present on a relay upload.
- FILES: Fixed broken null check on file ownership to non-public directories on public systems without mirror permissions. aka, the VDJ fix.
- JOBS: Fixed bug preventing data from staging due to a completed transfer being marked as cancelled.
- JOBS: Added catch to roll back job status on container SIGKILL.
- NOTIFICATIONS: Fixed bug where the `persistent` attribute in a notification request was not being parsed on application/x-www-form-urlencoded requests.
- NOTIFICATIONS: Fixed validity check on notification updates to allow updating persistent notifications.
- METADATA: Fixing bug where superadmin permission lookup could throw null pionter exception.
	
### Removed
- nothing



## 2.1.5 - 2015-10-20

### Added
- nothing

### Changed
- APPS: Fixed error responses to return a valid domain message rather than the default HTTP code message.
- APPS: Fixed searching by app id when app is public.
- APPS: Fixed search for matches within a list of values when the list is of length 1
- SYSTEMS: Fixed redundant diretory listing in S3

### Removed
- nothing


## 2.1.5 - 2015-10-09
### Added
- POSTITS: Adding `naked` url query support to responses.
- USAGE: Adding `naked` url query support to responses.
- TENANTS: Adding `naked` url query support to responses.
- TRANSFERS: Adding quartz management app to the service.
- JOBS: Fixed bug where the job `lastUpdated` timestamp was not updated when a monitoring fired, but the system was offline. This caused constant polling and failing for jobs that don't need to be checked.
- JOBS: Updated quartz management on each worker container to support a scheduler id in the url query. This allows us to track different queues as needed.

### Changed
- METADATA: Fixing pagination query in mongo listings to speed up response. Was using batchQuery, instead using limit. Reduced number of results returned and pushes pagination to server side.
- APPS: Fixed publication when specifying target execution system.
- APPS: Fixing auth check when publishing.
- SYSTEMS: Ratcheting down the connection timeout for SSH/SFTP connections from 60s to 15s.
- SYSTEMS: Adding configuration and logic to enforce parallel transfers and bump the buffer sizes used to 4MB on put/get/sync
- SYSTEMS: Fixing bug in S3 that caused multipart uploads to fail due to checksum issue. This happens when you try to set the checksum yourself on a multipart transfer rather than letting the server side handle it.
- SYSTEMS: Fixing bug in S3 listings where the folder being listed was included in the listing as a named entry. This was caused by not filtering out the named directory as we do with other protocols.
- SYSTEMS: Fixing bug in relay transfers preventing both sides of the transfer from being reported in real time. Now the parent transfer task will spawn two subtasks, one for each leg of the relay. The existing rollup will double report file size, but at least it works.
- ALL: Switched boolean fields to `tinyint(1)` in all relational mappings.
- FILES: Increased concurrency support for data processing.

### Removed
- FILES: Removing the logic in various endpoints that deletes logical files if the data does not exist on the remote system. This is unfavorable due to the chicken and egg problem that happens when uploading or transferring data to a new location.


## 2.1.5 - 2015-10-02
### Added
- SYSTEMS: Tuning SFTP transfers. Now bumping tcp buffer windows to 50mb. This allows for signifcantly faster transfers on fast networks. In practice we need to adjust this and the tcp socket buffer size to optimize transfer speeds.
- FILES: File upload now throws a new UPLOAD event for which one can subscribe.
File upload supports range queries. When uploading to a URL, the range specifies the starting point at which the uploaded content will be written. No validation is made on the back-end of the range, however content will not be cut off if the original was longer than the uploaded content + range index.
- FILES: Added pagination to file uploads
- METADATA: Added support for wildcard queries. You can now specify a `*` in any search term and the value will be evaluated as a regex.
- ALL: Added `naked` url query parameter to all endpoints. Setting `naked=true` in the url query will strip the wrapper from the response and return only the tranditional `result` value.
- NOTIFICATIONS: jobs, apps, and systems webhooks will not receive the JSON description of the affected resource in the POST body. URL templating is still supported. 

### Changed
- SYSTEMS: Fixing queries for public and private systems. Permissions query was not being properly injected into hql.
- APPS: Fixing queries for public and private apps. Permissions query was not being properly injected into hql.
- ALL: Adding error message when using an operator other than EQ or NEQ with a boolean search term.
- JOBS: Fixed bug where permissions were not set on the root job archive folder if not created at submission. Need to verify this is done to
- JOBS: Fixed sort order of the job history api so events return from oldest to newest rather than the other way around.
Fixed validation and path resolution on staging of previous job output given as an input url
- JOBS: FILES: Sped up the file history query dramatically. the parent tree as well.
- FILES: Added event propagation to the parent directory on all file manipulation events with a new "CONTENT_CHANGE" event. Subscribe to this to know when file/folder contents change for any reason.
- FILES: Fixed event order to be ascending by event id 
- FILES: Fixed bug where encoding and staging tasks could not be released after they completed or failed, thus preventing nay more from completing.
- FILES: File upload processing, messaging, and events now line up with import operations.
Fixed bug in history queries where yet-to-be-copied logical files were deleted prior to data being copied. This happened because an existence check was made on the query and when it failed, the record was deleted and resulting children cleaned up. This code is removed and a 404 is only thrown if there is not logical file and no target data. This fixed the bug where file uuid seemingly reset/disappeared. It also fixes the bug preventing notifications from being sent on some new transfers. 
- FILES: Added parameterized support for proxy to disk.
- FILES: Fixed bug in file upload when the target path overlaps what would be the public home directory of a private system if it were public.
- FILES: Fixed bug in transfers where status was not set properly if there was no file type given.
- TENANTS: Restricted results to tenants where status = 'LIVE'
- SYSTEMS: Adding "enable" and "disable" actions to the System management API
- SYSTEMS: Added ENABLED and DISABLED notification events to systems.
- SYSTEMS: Fixed bug where storage config was not cloning the containerName, publicAppsDir, or mirrorPermissions attributes.
Fixing error responses from primary collection queries.
- NOTIFICATIONS: Fixed bug in deserialization of messages and handling of messages with no context.
- ALL: Fixing pretty printing format to include newline after object and split arrays to newlines.
- ALL: XFixing pretty printing that was broken in legacy apis.


### Removed
- nothing


## 2.1.4 - 2015-09-14
### Added
- APPS: Search support has been added to the collections endpoint.
- APPS: Historical app events have been added similar to jobs and files.
- APPS: App registration,update, and delete events now trigger corresponding system events.
- APPS: Collection requests have been rewritten with significant performance speedup.
- JOBS: Staging and archiving events now have transfer ids included in the response for referencing transfer objects and their histories.
- FILES: Multipart upload is now supported via the PLUpload spec.
- FILES: HTTP Range header is now supported on file uploads. This can be SLOW depending on protocol. We're not going to promote this just yet.
- FILES: Copy actions now support an optional `append` attribute in the request allowing files to be concatenated to each other. 
- SYSTEMS: Search support has been added to the systems and batchQueues endpoints.
- SYSTEMS: Collection requests have been rewritten with significant performance speedup.

### Changed
- APPS: Previous search endpoints for name, tags, and ontology now proxy to the corresponding url query based collection query.
- JOBS: Job output discovery now checks the status history when determining where to look for data rather than just the current status. This prevents pointing at the archive location when archiving may have failed.
- JOBS: Job callbacks are broken into their own package and processed using a custom management class.
- JOBS: Changed query used to fetch job events so history shows in occurance order even when timestamps are identical. This was needed to deal with mysql precision.
- FILES: Changed query used to fetch file events so history shows in occurance order even when timestamps are identical. This was needed to deal with mysql precision.
- FILES: fixed possible bug in notifications not updating when called from different tenants.
- TENANTS: fixed bug where transactions were not closed on tenant lookups.
- SYSTEMS: fixed bug where enumerations were not being copied over on clone operations.
- SYSTEMS: fixed a bug where `software.parameters.semantics.minCardinality` and `software.parameters.semantics.minCardinality` were not being copied over on clone operations.
- SYSTEMS: agave URI schema is now case insensitive everywhere.

### Removed
- SYSTEMS: The BatchQueue endpoint had a redundant `name` field in it that has been removed.



## 2.1.4 - 2015-08-13
### Added
- JOBS: ISO 8601 support for date fields in job search queries

### Changed
- APPS: Fixed bug preventing existing apps from being updates. Bug was the object was evicted after delete, then referenced later on.

### Removed
- nothing


## 2.1.4 - 2015-08-04
### Added
- ALL: Adding configs for auto-reconnect standard and pooled jdbc connections into hibernate config to prevent contention between tomcat, dbcp, and hibernate.
- JOBS: Refactored monitoring and callback behavior to set a job to finished once it hits `CLEANING_UP` and is not archiving output. This fixes an unnecessary lag in job processing when the archiving tasks are backed up.
- SYSTEMS: Adding support for querying S3 complaint APIs.
- SYSTEMS: Added file metadata caching to speed up recursive operations within a request.

### Changed
- ALL: Cleaned up some transaction leaks.
- APPS: Fixed a bug where the original app owner was used to check permission to update an existing app on a system rather than the authenticated user.
- DOCS: Fixing typo in jobs output endpoint preventing clients from auto detecting the URL.
- JOBS: Fixed the way `*.eq`, `*.nin`, and `*.in` are handled for dates in search.
- SYSTEMS: Adding correction when hostnames are given rather than URLs in S3 system definitions to make validation friendlier.
- SYSTEMS: Stabilizing unit tests and optimizing for throughput to get all data access and permission tests working across the entire test matrix.

### Removed
- nothing


## 2.1.4 - 2015-07-28
### Added
- ALL: Added an authenticated /runtimes endpoint to legacy java apis to allow viewing and editing of configs at runtime. This is rather moot since we're running containers, but it's helpful for steering and debugging.
- JOBS: Added support for negative capacity isolation values by prefixing a system or username with an exclaimation point. For example, running a container with `-e IPLANT_DEDICATED_USER_ID=!dooley -e IPLANT_DEDICATED_TENANT_ID=tacc.prod`, will process everything for the `tacc.prod` tenant except jobs owned or associated with user `dooley`. It is ***NOT*** currently possible to mix and match inclusion and exclusion. Next up will be whitelists and blacklists for these properties.

### Changed
- ALL: Updating build system to build and publish Docker images tagged with the git commit hash, version, and update latest
- ALL: Updated build flags to handle enabling/disabling pushing and naming.
- ALL: Updated build system to publish direct to any Docker repo on deploy.
- ALL: Updating Tomcat JNDI connection settings moving to DHCP Pool in an attempt to deal with some of the timeout and dead connection issues we've been having. To address this properly, we should split the APIs out to run without a 2nd level cache while the workers use the existing DAO.

### Removed
- nothing

## 2.1.3 - 2015-07-24
### Added
- ALL: Updating formatting on HTML email templates to provide cleaner field descriptions and enforce newlines.
- FILES: Added management UI for worker processes. This is identical to the one for the jobs API.

### Changed
- ALL: Fix a bug in notifiations preventing webhooks from sending due to messages not being able to be deserialized from the queue. [Issue 74](https://bitbucket.org/agaveapi/agave/issues/74/job-notifications-are-not-sending)
- FILES: Refactored file staging processing to reflect the same producer/consumer approach taken by the jobs api. This removes contention and guarantees no-conflict in a single host environment.

### Removed
- nothing


## 2.1.3 - 2015-07-22
### Added
- ALL: HTML email notification support for all events. Email will be sent as both plain text and HTML.

### Changed
- JOBS: Fixed temporal job queries so searching by date range (ex `startTime.between=1 week ago,yesterday`, `endTime.between=2015-05-05 8:00,2015-05-05 12:00`) works as expected.
- JOBS: Refactored queue processing into two schedulers, one for producer and the other consumer. Producers have a thread for each queue. Consumers have a generic pool of 25 threads to process new jobs and triggers created by the producers. Each quartz job has a key equal to the agave job uuid, thus quartz prevents duplication within a jvm. To prevent monopolization of the thread pool, the concurrent list of job ids is still kept in the producer and no new job will be created while the queue is at length Settings.MAX_XXXX_TASKS.
- JOBS: Fixed a bug where job status queries were not refreshing quickly enough. This is an artifact of optimistic record locking used to prevent concurrency issues across distributed JVM. Each request will give a stale update at most one time, then instantly refresh with a new query to the DB.

### Removed
- nothing


## 2.1.3 - 2015-07-16
### Added
- ALL: adding in Docker Compose file to bring up all core services, dependent services, and load balancer with routing built in. If using Swarm, this is sufficient for a scalable multihost setup.
- ALL: Added support for sending email using multiple clients. This lets us use SendGrid, MailGun, SMTP, PostFix, or optional log file printing of email notifications.
- JOBS: Improved performance and reliability of monitoring processes

### Changed
- JOBS: Fixed bug preventing job status updates from occuring during monitoring  processes when remote connectivity was needed.
- JOBS: Refactored job queues to use a custom job factory. They now follow producer/consumer pattern. This implementation uses a concurrent linked list to track the active jobs so no conflict can happen within a single jvm.
- FILES: Fixed bug in pagination of file histories.

### Removed
- Old fat container build
- Deprecated `fig.xml` file
- Deprecated tomcat and depenent service configs.


## 2.1.3 - 2015-07-13
### Added
- SYSTEMS: added system batch queues as a formal resource with independent crud api /systems/<id>/queues. With the expanded format, queue descriptions will include and additional `load` object which describes the current load on a queue in terms of Agave usage.
- SYSTEMS: added `system.load` object which describes the current load on a system in terms of Agave usage.
- JOBS, TRANSFORMS: Added reaper thread to clean up zombie jobs across the platform.
_ JOBS, FILES, TRANSFORMS: Added support for transferring job output directories by specifying their URI. Agave resolves them internally, verifies access rights, and resolves them to their current system and path. For users with access to a job, but not the system, the remote system is modified to use the job work directory as they system root, thus isolating their ability to access any other data.

### Changed
- ALL: AgaveUUID class was updated to avoid collisions when accessed within 100 nanoseconds.
- JOBS: refactored worker processes so they are largely self-healing in the even the container is closed. Now any running processes will be rolled back to their previous state and resubmitted to the queue for pick up by another worker.
- SYSTEMS: Updated transfer classes to support graceful termination due to shutdown or thread interruption.
- SYSTEMS: Fixed bug in 3rd party transfers preventing total data moved from writing to the logs.
- POSTITS, LOGGING, TENANTS, USAGE: Fixed parameterization of the config files to inject the proper runtime values upon maven build.
- JOBS: lot of concurrency tests replicating production quartz behavior.
- JOBS, SYSTEMS: leveraging a new threadsafe approach to passing tasks through the API.

### Removed
- nothing


## 2.1.2 - 2015-06-29
### Added
- ALL: Added iplant.dedicated.tenant.id configuration setting to enable the restriction of a worker to a particular tenant.
- ALL: Added iplant.drain.all.queues configuration setting to tell a worker to stop accepting new tasks.  
- ALL: Added quartz workers endpoint to legacy APIs to monitor worker tasks
- ALL: Added printing of JWT JSON as well as header when the `debugjwt` url parameter is defined.
- JOBS: Added reaper thread to roll back zombie archiving jobs that have not updated in several minutes. This will grow out to handle all zombie tasks.
- SYSTEMS: Added full support for FTP storage systems. Both authenticated and anonymous FTP is supported. Use FTP for the system.storage.protocol value and ANONYMOUS or PASSWORD for the system.storage.auth.type value.
- SYSTEMS: Added system.[storage,login].auth.caCerts field to x509 auth configurations to allow the importing of a trustroot archive from a public URL. This allows users to provide self-signed credentials for their private infrastructure and still access them from Agave. Each system's auth config trustroots are sandboxed and fetched as needed. Archive can be in zip, tar, bzip2, tgz, tar.gz, tar.bz2, or jar format.
- NOTIFICATIONS: Added support for authenticated SMTP servers and HTML email.


### Changed
- ALL: Updated myproxy to support  and fall back on TLS automatically.
- APPS: Fixed bug in app registration where apps would not save due to a uniqueness constraint failure.
- APPS: Fixed bug in app update endpoint where the app would not save due to the id not being resolved properly.
- APPS: Fixed bug in permission checks of app assets where checks would fail if an absolute path was not given on public systems.
- JOBS: Fixed bug in job status worker where concurrency collisions were not being caught. This prevented Condor jobs from updating.
- JOBS: Fixed bug in job staging worker where job would fail due to a StaleObjectException if more than one input was present. This was caused by the transfer task associated with the staging job event being updated as a separate entity during the execution of the `URLCopy.copy()` method. When the method returned, the original reference to the trnasfer task was still referenced in the job event. Because we had a `Cascade={ALL,DELETE}` annotation on the association, persistence failed due to the stale transfer task. This was corrected by changing the annotation field to `Cascade={DELETE}`. Since we manage transfer tasks independently, this is completely safe.
- JOBS: Updated search query to accept comma-delimited lists of search values.
> /jobs/v2/?status.in=RUNNING,SUBMITTING,ARCHIVING
> /jobs/v2/?endtime.after=2015-01-17&endtime.before=today

- JOBS: Updated search query to allow data ranges to be preceded by a comparator such that you can specify created=(2014-12-01,today)
> /jobs/v2/?executionsystem.like=stampede&runtime.gt=86400  
> /jobs/v2/?submittime.on=yesterday&appid.like=bwa  

- FILES: Rewrote a portion of the jglobus library to support multiple truststore locations and concurrent, multiuser scenarios.
- FILES: Fixed a bug where the root of public systems could not be viewed by admins.
- FILES: Fixed bug where staging and encoding tasks could not get an optimistic lock.
- JOBS: Rewrote job queues to handle concurrency and failures a bit better. Conflicts seem to be isolated at tests up to 10 simultaneous workers.
- SYSTEMS: Updated URLCopy, TransferTask, and RemoteTransferListener to catch content updates in real time.
- SYSTEMS: Updated URLCopy to use a relay transfer rather than a proxy transfer when file size is under 6GB. This allows for speedups from striping, etc in certain situations.
- SYSTEMS: Updated URLCopy to roll back and cancel transfer task groups when a transfer is cancelled from another thread.
- SYSTEMS: Fixed bug preventing MyProxy from retrieving certs from unknown, self-signed servers.
- SYSTEMS: Fixed S3 support, optimizing uploads and downloads using chunked transfers.
- SYSTEMS: Fixed bug in HTTP imports where some url parameters were not forwarded to the download client.
- TRANSFORMS: Fixed bug where decoding tasks could not get an optimistic lock.
- TRANSFORMS: Updated queue workers to track data movement.
- TRANSFORMS: Fixed bug where tenancy was not honored on callbacks.
- POSTITS: Fixed parameterization bug preventing CD
- USAGE: Fixed parameterization bug preventing CD

### Removed
- Disabling of apps if the assets disappear temporarily.
-

## 2.1.1 - 2015-06-02
### Added
- ALL: Added support for pagination through the limit and offset url query parameters.
- FILES: Added support for forced downloads on the files download service. This will add the `Content-Disposition` header to the response whenever `force=true` is in the URL query.
- FILES: Added support for unspecified range request sizes. You can now specify `256-` as a valid range. The files services will return everything after byte 256 in that file. This is helpful whenever you need to continue a download after it previously failed.
- SYSTEMS: Added new `system.storage.auth.trustedCALocation` and `system.login.auth.trustedCALocation` fields to system definitions to allow for trustroots to be provided as tar, zip, tgz, or bzip2 archives at a public URL.
- JOBS: Added support for forced downloads on the job output download service. This will add the `Content-Disposition` header to the response whenever `force=true` is in the URL query.
- JOBS: Added support for unspecified range request sizes. You can now specify `256-` as a valid range. The job output service will return everything after byte 256 in that file. This is helpful whenever you need to continue a download after it previously failed.
- JOBS: search has been updated so you can query by any job attribute using a URL query string such as status=running. Dates such as `created`, `starttime`, and `submittime` are rounded to the day and matched accordingly. `name`, `inputs`, and `parameters` are all partial matches. All other fields are exact matches.

### Changed
- APPS: Fixed a bug where app.input.semantics.maxCardinality was not preserved when copying or publishing an app.
- APPS: Fixed a bug where app.output.value.order was not preserved when copying or publishing an app.
- JOBS: Fixed a bug where jobs could remain in a persistent active state when they failed due to parsing or unexpected errors from the file system. Now they will be set to FAILED after the max job expiration time is reached.
- JOBS: Fixed a bug where non-primary tenant jobs were not being updated when the callback came. This had to do with the JobDAO.getJobBYUUID() method not removing the tenancy filter.
- JOBS:
- SYSTEMS: Added better exception handling to prevent users from attempts to redefine an execution system to a storage system or vice versa.
- SYSTEMS: Fixed a bug in the SFTP client where port would not default properly if not given.
- SYSTEMS: Improved exception handling when validation X.509 credentials.
- JOBS: Bug causing a race condition in job submission and, indirecdtly, failed jobs under low traffic situations was fixed.

### Removed
- No change.

## 2.1.0 - 2014-11-23
### Added
- No change.

### Changed
- APPS: Fixed a bug where app.input.semantics.maxCardinality was not preserved when copying or publishing an app.
- APPS: Fixed a bug where app.output.value.order was not preserved when copying or publishing an app.
- JOBS: Fixed a bug where jobs could remain in a persistent active state when they failed due to parsing or unexpected errors from the file system. Now they will be set to FAILED after the max job expiration time is reached.

### Removed
- No change.


## 2.1.0 - 2014-11-05
### Added
- No change.

### Changed
- SYSTEMS: Rewrote the ssh tunneling code to produce more reliable tunnels through dynamic port selection and a vt100 pseudo terminal to the remote system.
- APPS: Rolling back change of app.parameter.value.enumValues attribute to app.parameter.value.enum_values for legacy compatibility.

### Removed
- No change.


## 2.1.0 - 2014-11-04
### Added
- Added Maven goals to build Docker containers out of each API.

### Changed
- Updated the fig.xml file to launch the API as a series of linked containers rather than a single fat container.
- Updated build instructions in the README.md file.

### Removed
- No change.

## 2.1.0 - 2014-11-04
### Added
- APPS: Support for array default values for app inputs, outputs, and parameters
- APPS: app.*.semantics.minCardinality and app.*.semantics.maxCardinality support for app inputs, outputs, and parameters.
- APPS: Inputs and parameters maintain default ordering and ordering values in the response from the API.
- APPS: Added app.*.value.repeatArguments support for app inputs, outputs, and parameters. This tells the job service whether to add the arguments once or in front of each job prior to injecting into the template.
- APPS: Added app.*.value.encoding support for app inputs, outputs, and parameters. This tells the job service whether to quote the value(s) prior to injecting into the template.
- JOBS: Support for array values when defining individual inputs and parameters
- SYSTEMS: Added global remote connection timeout limit of 90 seconds for command invocation on systems.
- PROFILES: Added UUID to user profiles.
- NOTIFICATIONS: Added support for notification events tied to user profiles. Currently CREATED, REVOKED, UPDATED are supported.

### Changed
- APPS: Input, output, and parameter default values are returned as JSON arrays rather than primary type values when the app.*.semantics.maxCardinality is greater than 1. All parameters, inputs, and outputs are set to 1 by default for backward compatibility.
- APPS: Increased label length on app inputs, outputs, and parameter labels from 64 to 128 characters.
- APPS: Fixed bug in SoftwareOutput.setValidator() preventing it from properly validating the value.
- JOBS: Fixed bug in job submission stemming from failed scheduler id parsing when connection to remote system times out.
- JOBS: Fixed bug in monitoring processes that would terminate a job if it finished before the first check ran.
- JOBS: Improved exception handling so the scheduler response bubbles back to the job error message when submission fails.
- JOBS: Fixed a bug in job submission when retrieving the job id and the operation had not yet completed. Switched to blocking call that consumes output as it goes. This speeds up things on average quite a bit because there is no long a forced 8 second delay for every call.
- JOBS: Updated internal representation of job values to be json based and honor the original primary value types. This fixed a bug where the json response from jobs always turned job values into strings rather than honoring the primary type.
- JOBS: Fixed event message to reflect how the job was submitted, i.e. HPC, CLI, or other.
- JOBS: Fixed bug where boolean parameters were not parsed in the job request if the request was made as pure json.d
- SYSTEMS: Fixed a bug in MyProxy credential handling that prevented certificate retrieval if SSLv3 was disabled.
- MONITORS: Fixed exception handling when a check resulted in a RuntimeException so the logs and monitor message will still present the correct message.
- PROFILES: Fixed bug in profiles notifications that prevented the notification's template variables from resolving correctly.

### Removed
- No change.

## 2.0.0 - 2014-10-24
### Added
- Dockerfile to build the APIs as a single Docker image
- fig.yml file to orchestrate the deployment of the Agave APIs and dependent services in a single step.

### Changed
- FILES: Fixed bug in notifications for file imports. notifications attribute was not accepted.
- FILES: Fixed bug in web hook processing for file imports where certain web hook URLs could cause infinite loops.

### Removed
- No change.

## 2.0.0 - 2014-09-26
### Added
- NOTIFICATIONS: Adding support for setting wildcard UUID on notifications. Only tenant admins and above can add these.
- NOTIFICATIONS: Added support for new user creation events and general profile notifications.

### Changed
- FILES: Fixed hypermedia "self" reference on individual file listings.
- FILES: Fixed bug discovering and uploading data in virtual home directory on sftp systems.

### Removed
- No change.

## 2.0.0 - 2014-09-17
### Added
- No change.

### Changed
- FILES: * Updating how file permissions are returned from irods on public systems. Now if the system is public, rather than returning all users, it will simply list the "public" user as having read access unless, of course, they do not have read access. Basically s/$username/public/g.
- FILES: Fixed bug in path resolution when passing in agave:// and tenant file url, such as when transferring data, importing, or specifying job inputs. Standard tenant urls now validate properly and throw exceptions properly when a bad system is provided.
- FILES: Fixed bug in url parsing allowing for proper handling of system root paths
- FILES: Fixed bug in file permissions preventing recursive permissions from being applied on non-irods systems.
- FILES: Updated file upload processing to reduce disk footprint, shorten response time, and increase the relay transfer to the remote system.
- FILES: Updating response from systems roles service to list the user profile under the correct hypermedia attribute

### Removed
- No change.


## 2.0.0 - 2014-09-06
### Added
- Project CHANGELOG.md file.

### Changed
- NOTIFICATIONS: Fixed bug in notifications sent from monitors service to list the proper webhook response.
- FILES: Updating response to requests to transfer data from non-agave systems. This response was different from the one given when uploading a file. Matching up for consistency.
- DOCS: Fixing bad url value in the job search method

### Removed
- No change.
