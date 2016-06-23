#!/bin/bash
#
# mongos3backup.sh
#
# Simple backup script to dump the agave mongodb, compress, tag, and push it to
# a daily backup folder on s3.
#
# Based on https://gist.github.com/2206527

# Basic variables
mongouser=""
mongopass=""
bucket="s3://agavedailybackups"

# Timestamp (sortable AND readable)
stamp=`date -u +%FT%TZ`
datestamp=`date +"%Y-%m-%d"`

# Feedback
echo -e "Dumping to \e[1;32m$bucket/$stamp/\e[00m"

# Define our filenames
filename="$stamp-mongo.tgz"
tmpdir="/tmp/$stamp-mongo"
object="$bucket/$datestamp/${filename}"

# Feedback
echo -e "\e[1;34m$db\e[00m"

# Dump and zip
echo -e "  dumping \e[0;35m$tmpfile\e[00m"
mongodump --port 9000 --out "${tmpdir}"

echo -e "  creating \e[0;35m$tmpfile\e[00m"
tar czf "/tmp/${filename}" "${tmpdir}"

# Upload
echo -e "  uploading..."
s3cmd put "/tmp/${filename}" "${object}"

# Delete
rm -rf "${filename}" "${tmpdir}"

# Jobs a goodun
echo -e "\e[1;32mMongoDB backups a goodun\e[00m"