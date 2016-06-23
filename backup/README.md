Agave Backup Scripts
====================
These scripts backup the Agave databases to Amazon S3 using the s3cmd tools. 

## Requirements

* s3cmd command line tools
* valid s3 account with IdM account configured for the bucket
* valid s3 bucket to catch the backups

## Installation

1. Add your account key and secret to the ~/.s3cfg file. A sample is included in this directory
2. Install the s3cmd command line tools

	```
	$ sudo su -
	$ yum install -y s3cmd
	```

3. Make sure the scripts are executable

	```
	$ chmod +x /root/mongodbs3backup.sh
	$ chmod +x /root/mysqls3backup.sh
	```

4. Manually check to make sure they are working. If successful, the backup files will be present in your s3 bucket in a daily backup folder.

	```
	$ /root/mongodbs3backup.sh
	$ /root/mysqls3backup.sh
	$ s3cmd ls s3://agavedailybackups/$(date +"%Y-%m-%d")
	```
	
5. Add the scripts to your crontab to run nightly

	```
	$ crontab -e
	```
	
	The following entries will snapshot and backup the databases at 2:30am daily.
	
	```
	30 2 * * * /root/mysqls3backup.sh > /var/log/mysqlbackup.log
	30 2 * * * /root/mysqls3backup.sh > /var/log/mongodbs3backup.sh
	```

## FYI

* This script does not back up the Usage table of the database. The usage records account for over 90% of the size of the db and by not backing them up, you drastically cut down on storage requirements. You should be harvesting your usage stats daily in a separate process anyway.

