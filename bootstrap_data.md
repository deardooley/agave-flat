#!/bin/bash

### Init infrastructure

```
echo "Starting third-party containers"
cd config/testbed
docker-compose -f third-party.yml up -d pushpin beanstalkd redis mongodb mysql sftp
docker-compose -f data.yml up -d sftp irods4
docker-compose -f data.yml up -d ssh slurm htcondor  
```   

### Init cli tenant

```
# Add default tenant `agave.dev`
auth-switch -t agave.dev -u testuser -d http://api.example.com -S
```  

### Register systems
  
```
SYSTEMS_FOLDER=agave/agave-systems/systems-core/src/test/resources/systems

############### STORAGE - SFTP ######################
systems-addupdate -F $SYSTEMS_FOLDER/storage/storage.example.com.json

# set as the default storage system
systems-setdefault storage.example.com

############### STORAGE - IRODS ######################
systems-addupdate -F $SYSTEMS_FOLDER/storage/irods4.example.com.json

############### EXECUTION - CLI ######################
systems-addupdate -F $SYSTEMS_FOLDER/execution/execute.example.com.json

# set as the default execution system
systems-publish -d ssh.example.com

############### EXECUTION - HPC ######################
systems-addupdate -F $SYSTEMS_FOLDER/execution/slurm.example.com.json

############### EXECUTION - CONDOR ######################
systems-addupdate -F $SYSTEMS_FOLDER/execution/condor.example.com.json  
```  

### Move data  

```
#################### Upload data to IRODS ########################
git clone https://bitbucket.org/agaveapi/science-api-samples.git

files-mkdir -N testuser -S irods4-password.example.com .
files-upload -v -F science-api-samples -S irods4-password.example.com testuser

#################### Create persistent notifications on the upload folder ########################
# We need the uuid of the uploaded folder, so we'll make a request and pull it out of the metadata link in the hypermedia response
UPLOAD_FOLDER_UUID=$(files-list -v -l 1 --filter=_links.metadata -S irods4-password.example.com testuser/science-api-samples | jq -r '.[0]._links.metadata.href' | sed 's/^.*?q=//g' | urldecode | jq -r '.associationIds')

# Create persistent notifications on the UUID of the upload folder we just retrieved.
# these notifications will always fail and get persisted in the failed attempt queue
notifications-addupdate  -U 'http://httpbin.org/status/500' -E "*" -P  -I 0 -R NONE -D 0 -L 0  -S  -A  $UPLOAD_FOLDER_UUID
 
#################### Transfer data between system ########################
# First, make a destination folder to keep things clean for later if/when we publish the system
files-mkdir -N transferred_data -S storage.example.com testuser 

# create a webhook URL just in case we want to watch while things run
IMPORT_REQUESTBIN=$(requestbin-create)
echo "Webhooks for the file transfer will post to $IMPORT_REQUESTBIN"

# transfer the data from the irods system to the compute system
files-import -U 'agave://irods4-password.example.com/testuser/science-api-samples' -W "$IMPORT_REQUESTBIN?path=${PATH}&event=${EVENT}" -S storage.example.com testuser/transferred_data
```


### Register Apps

```

# Local path to the JSON app descriptions
APPS_FOLDER=testuser/agave/agave-apps/apps-core/src/test/resources/software

# create parent folder for all app assets
files-mkdir -N agave/apps -S storage.example.com testuser

################# fork-1.0.0 #################
# upload the app assets and register the app
files-upload -F $APPS_FOLDER/fork-1.0.0 -S storage.example.com testuser/agave/apps
apps-addupdate -F $APPS_FOLDER/fork-1.0.0/app.json

# create some history for the app
apps-addupdate -F $APPS_FOLDER/fork-1.0.0/app.json fork-1.0.0
apps-disable fork-1.0.0
apps-enable fork-1.0.0

# share the app with someone
apps-pems-update -p READ_EXECUTE -u testshareuser fork-1.0.0
apps-pems-list fork-1.0.0

################# fork-clone-1.0.0 #################
# create a copy of this app with a new id
CLONE_APP=$(apps-clone -n fork-clone -s storage.example.com -v fork-1.0.0)

# update it for a history record
echo "${CLONE_APP}" | apps-addupdate -F - fork-clone-1.0.0

# enable and disable
apps-disable fork-clone-1.0.0
apps-enable fork-clone-1.0.0


################# head-hpc-1.0.0 #################
# upload the app assets and register the app
files-upload -F $APPS_FOLDER/head-hpc-1.0.0 -S storage.example.com testuser/agave/apps
apps-addupdate -F $APPS_FOLDER/head-hpc-1.0.0/app.json


################# wc-condor-1.0.0 #################
# upload the app assets and register the app
files-upload -F $APPS_FOLDER/wc-condor-1.0.0 -S storage.example.com testuser/agave/apps
apps-addupdate -F $APPS_FOLDER/wc-condor-1.0.0/app.json

``` 

### Publishing Apps and Systems

```

################# publishing systems #################
systems-publish storage.example.com
systems-publish execute.example.com


################# publishing fork-1.0.0 #################
# The resulting id will be fork-1.0.0u1
apps-publish fork-1.0.0

# The resulting id will be fork-1.0.0u2
apps-publish fork-1.0.0


################# publishing fork-clone-1.0.0 #################
# The resulting id will be fork-1.0.0u3. 
apps-publish -n fork fork-clone-1.0.0

# The resulting id will be fork-1.0.0u4
apps-publish fork-1.0.0

################# disable older public apps #################
apps-disable fork-1.0.0u1
apps-disable fork-1.0.0u2
apps-disable fork-1.0.0u3

```  

### Create metadata 

```
################# system comments #################
STORAGE_SYSTEM_UUID=$(systems-list -v --filter=uuid storage.example.com)
EXECUTION_SYSTEM_UUID=$(systems-list -v --filter=uuid execute.example.com)
echo '{"name": "comment", "value":{"owner":"testuser","body": "This might be useful."}, "associatedIds": ["'$EXECUTION_SYSTEM_UUID'","'$STORAGE_SYSTEM_UUID'"]}' | metadata-addupdate -F -

################# files comments #################
echo '{"name": "comment", "value":{"owner":"testuser","body": "Created as part of file upload and transfer tests."}, "associatedIds": ["'$UPLOAD_FOLDER_UUID'"]}' | metadata-addupdate -F -

################# files comments #################
APP_FORK_1_0_0_UUID=$(apps-list -v --filter=uuid fork-1.0.0)
APP_FORK_1_0_0_UUID=$(apps-list -v --filter=uuid fork-1.0.0u3)
echo '{"name": "comment", "value":{"owner":"testuser","body": "Accidentally overwritten by the cloned fork app. Disregard this for real usage."}}, "associatedIds": ["'$APP_FORK_1_0_0_UUID'"]}' | metadata-addupdate -F -


METADATA_PROJECT_ID=$(echo '{"name": "project", "value":{"owner":"testuser","description": "This is my first project."}}' | metadata-addupdate -v -F - | jq -r '.id')
echo '{"name": "project_vote", "value":{"voter":"ryan","is_in_favor": true}, "associatedIds": ["'$METADATA_PROJECT_ID'"]}' | metadata-addupdate -v -F -
echo '{"name": "project_vote", "value":{"voter":"rion","is_in_favor": true}, "associatedIds": ["'$METADATA_PROJECT_ID'"]}' | metadata-addupdate -v -F -
echo '{"name": "project_vote", "value":{"voter":"rian","is_in_favor": false}, "associatedIds": ["'$METADATA_PROJECT_ID'"]}' | metadata-addupdate -v -F -
echo '{"name": "project_vote", "value":{"voter":"ryon","is_in_favor": true}, "associatedIds": ["'$METADATA_PROJECT_ID'"]}' | metadata-addupdate -v -F -

```  

### Run Jobs

```  

# create a webhook URL just in case we want to watch while jobs run
JOBS_REQUESTBIN=$(requestbin-create)
echo "Webhooks for the job submissions will post to $JOBS_REQUESTBIN"

################# generate job request templates #################
# generate the job submission templates from the app descriptions and default values
JOB_TEMPLATE_FORK_1_0_0=$(jobs-template -A -C fork-1.0.0)
JOB_TEMPLATE_FORK_1_0_0_u_4=$(jobs-template -A -C fork-1.0.0u4)
JOB_TEMPLATE_HEAD_HPC_1_0_0=$(jobs-template -v -A -C head-hpc-1.0.0 | jq --arg rqbin "$JOBS_REQUESTBIN?appId=\${APP_ID}&job_id=\${JOB_ID}&event=\${EVENT}" '. |= .notifications = [{"url": $rqbin, "event":"*", "persistent":true, "policy":{"retryStrategy":"IMMEDIATE","retryLimit":3,"retryRate":5,"retryDelay":5,"saveOnFailure":true}}]')
JOB_TEMPLATE_WC_CONDOR_1_0_0=$(jobs-template -v -A -C wc-condor-1.0.0 | jq --arg rqbin "$JOBS_REQUESTBIN?appId=\${APP_ID}&job_id=\${JOB_ID}&event=\${EVENT}" '. |= .notifications = [{"url": $rqbin, "event":"*", "persistent":true, "policy":{"retryStrategy":"IMMEDIATE","retryLimit":3,"retryRate":5,"retryDelay":5,"saveOnFailure":true}}]')

################# run unarchived jobs #################
# vanilla no archive private app job
JOB_ID_FORK_1_0_0=$(echo "$JOB_TEMPLATE_FORK_1_0_0" | jq '. |= .archive = false' | jobs-submit -v -F - | jq -r '.id')

# vanilla no archive public app job
JOB_ID_FORK_1_0_0_u_4=$(echo "$JOB_TEMPLATE_FORK_1_0_0_u_4" | jq '. |= .archive = false' | jobs-submit -F - | jq -r '.id')

# throw in a few more
for i in {1..9}; do
	jobs-resubmit $JOB_ID_FORK_1_0_0
	jobs-resubmit $JOB_ID_FORK_1_0_0_u_4
done

################# run unarchived jobs #################
# archive app to IRODS with default path
JOB_ID_FORK_1_0_0=$(echo "$JOB_TEMPLATE_FORK_1_0_0" | jq '. |= .archiveSystem = irods-password.example.com' | jobs-submit -v -F - | jq -r '.id')

# archive app to IRODS with custom path
JOB_ID_FORK_1_0_0=$(echo "$JOB_TEMPLATE_FORK_1_0_0" | jq '. |= .archiveSystem = irods-password.example.com |= .archivePath = "testuser/customarchivepath"' | jobs-submit -v -F - | jq -r '.id')



################# run a batch of jobs across all apps and systems #################

shuffle_jobs() {
   local i tmp size max rand

   # $RANDOM % (i+1) is biased because of the limited range of $RANDOM
   # Compensate by using a range which is a multiple of the array size.
   size=${#jobs_ids[*]}
   max=$(( 32768 / size * size ))

   for ((i=size-1; i>0; i--)); do
      while (( (rand=$RANDOM) >= max )); do :; done
      rand=$(( rand % (i+1) ))
      tmp=${jobs_ids[i]} jobs_ids[i]=${jobs_ids[rand]} jobs_ids[rand]=$tmp
   done
}
 
# vanilla archived private app job
JOB_ID_FORK_1_0_0_ARCHIVE=$(echo "$JOB_TEMPLATE_FORK_1_0_0" | jobs-submit -v -F - | jq -r '.id')
JOB_ID_FORK_1_0_0_u_4_ARCHIVE=$(echo "$JOB_TEMPLATE_FORK_1_0_0_u_4" | jobs-submit -v -F - | jq -r '.id')
JOB_ID_HEAD_HPC_1_0_0_ARCHIVE=$(echo "$JOB_TEMPLATE_HEAD_HPC_1_0_0" | jobs-submit -v -F - | jq -r '.id')
JOB_ID_WC_CONDOR_1_0_0_ARCHIVE=$(echo "$JOB_TEMPLATE_WC_CONDOR_1_0_0" | jobs-submit -v -F - | jq -r '.id')


jobs_ids=()

for i in {1..10}; do
	jobs_ids+=($JOB_ID_FORK_1_0_0_ARCHIVE)
	jobs_ids+=($JOB_ID_FORK_1_0_0_u_4_ARCHIVE)
	jobs_ids+=($JOB_ID_HEAD_HPC_1_0_0_ARCHIVE)
	jobs_ids+=($JOB_ID_WC_CONDOR_1_0_0_ARCHIVE)
done

shuffle

for i in "${jobs_ids[@]}"; do 
	jobs-resubmit $i
done

```
