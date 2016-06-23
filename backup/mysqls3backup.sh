#!/bin/bash
#
# mysqls3backup.sh
#
# Simple backup script to dump the agave mysql db, compress, tag, and push it to
# a daily backup folder on s3.
#
# Based on https://gist.github.com/2206527

# Basic variables
mysqluser="YOUR_DBUSER"
mysqlpass="YOUR_DBUSER_PASSPHRASE"
bucket="s3://agavedailybackups"

# Timestamp (sortable AND readable)
stamp=`date -u +%FT%TZ`
datestamp=`date +"%Y-%m-%d"`

# List all the databases
databases=`mysql -u $mysqluser -p$mysqlpass -e "SHOW DATABASES;" | tr -d "| " | grep -v "\(Database\|information_schema\|performance_schema\|mysql\|test\)"`

# Feedback
echo -e "Dumping to \e[1;32m$bucket/$stamp/\e[00m"

# Loop the databases
for db in $databases; do

  # Define our filenames
  basefilename="$stamp-$db"
  tmpdir="/tmp/$basefilename"
  object="$bucket/$datestamp/${basefilename}.tgz"

  # Feedback
  echo -e "\e[1;34m$db\e[00m"

  # Dump and zip
  echo -e "  creating \e[0;35m${tmpdir}\e[00m"
  mkdir -p "$tmpdir"
  
  if [ "$db" == "iplant-api" ] || [ "$db" == "agave-api" ]; then
    # ignore Usage table as it shrinks db size by > 90%
  	echo -e "  dumping structure \e[0;35m$tmpdir/${basefilename}.tables.sql\e[00m"
  	mysqldump -u $mysqluser -p$mysqlpass --force --opt --databases "$db" --no-data > "$tmpdir/${basefilename}.tables.sql"
  	echo -e "  dumping structure \e[0;35m$tmpdir/${basefilename}.data.sql\e[00m"
  	mysqldump -u $mysqluser -p$mysqlpass --force --opt --databases "$db" --ignore-table="$db.Usage" > "$tmpdir/${basefilename}.data.sql"
  else
  	echo -e "  dumping structure \e[0;35m$tmpdir/${basefilename}.tables.sql\e[00m"
  	mysqldump -u $mysqluser -p$mysqlpass --force --opt --databases "$db" --no-data > "$tmpdir/${basefilename}.tables.sql"
  	echo -e "  dumping structure \e[0;35m$tmpdir/${basefilename}.data.sql\e[00m"
  	mysqldump -u $mysqluser -p$mysqlpass --force --opt --databases "$db" > "$tmpdir/${basefilename}.data.sql"
  fi
  
  tar -czf  "${tmpdir}.tgz" "${tmpdir}"
  
  # Upload
  echo -e "  uploading..."
  s3cmd put "${tmpdir}.tgz" "$object"

  # Delete
  echo -e "  cleaning up $tmpdir..."
  rm -rf "${tmpdir}" "${tmpdir}.tgz"

done;

# Jobs a goodun
echo -e "\e[1;32mJobs a goodun\e[00m"
