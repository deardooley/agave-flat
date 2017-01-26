# This is a generic wrapper script for checking the runtime environment
# of a job and verifying the runtime template variable values
set +x

date

# print out the agave runtiem variables
echo "##################################################"
echo "# Agave Job Runtime Variables "
echo "##################################################"
echo "\n"
echo 'AGAVE_JOB_NAME="${AGAVE_JOB_NAME}"'
echo 'AGAVE_JOB_ID="${AGAVE_JOB_ID}"'
echo 'AGAVE_JOB_APP_ID="${AGAVE_JOB_APP_ID}"'
echo 'AGAVE_JOB_EXECUTION_SYSTEM="${AGAVE_JOB_EXECUTION_SYSTEM}"'
echo 'AGAVE_JOB_BATCH_QUEUE="${AGAVE_JOB_BATCH_QUEUE}"'
echo 'AGAVE_JOB_SUBMIT_TIME="${AGAVE_JOB_SUBMIT_TIME}"'
echo 'AGAVE_JOB_ARCHIVE_SYSTEM="${AGAVE_JOB_ARCHIVE_SYSTEM}"'
echo 'AGAVE_JOB_ARCHIVE_PATH="${AGAVE_JOB_ARCHIVE_PATH}"'
echo 'AGAVE_JOB_NODE_COUNT="${AGAVE_JOB_NODE_COUNT}"'
echo 'AGAVE_JOB_PROCESSORS_PER_NODE="${AGAVE_JOB_PROCESSORS_PER_NODE}"'
echo 'AGAVE_JOB_MEMORY_PER_NODE="${AGAVE_JOB_MEMORY_PER_NODE}"'
echo 'AGAVE_JOB_ARCHIVE_URL="${AGAVE_JOB_ARCHIVE_URL}"'
echo 'AGAVE_JOB_OWNER="${AGAVE_JOB_OWNER}"'
echo 'AGAVE_JOB_TENANT="${AGAVE_JOB_TENANT}"'
echo 'AGAVE_JOB_ARCHIVE="${AGAVE_JOB_ARCHIVE}"'
echo 'AGAVE_JOB_MAX_RUNTIME="${AGAVE_JOB_MAX_RUNTIME}"'
echo 'AGAVE_JOB_MAX_RUNTIME_SECONDS="${AGAVE_JOB_MAX_RUNTIME_SECONDS}"'
echo 'AGAVE_JOB_MAX_RUNTIME_MILLISECONDS="${AGAVE_JOB_MAX_RUNTIME_MILLISECONDS}"'
echo 'AGAVE_BASE_URL="${AGAVE_BASE_URL}"'
echo 'AGAVE_JOB_ARCHIVE="${AGAVE_JOB_ARCHIVE}"'
echo 'AGAVE_CACHE_DIR="${AGAVE_CACHE_DIR}"'
echo 'AGAVE_JOB_ACCESS_TOKEN="${AGAVE_JOB_ACCESS_TOKEN}"'
echo 'AGAVE_JOB_REFRESH_TOKEN="${AGAVE_JOB_REFRESH_TOKEN}"'
echo 'AGAVE_JOB_PACKAGE_OUTPUT="${AGAVE_JOB_PACKAGE_OUTPUT}"'
echo 'AGAVE_JOB_COMPRESS_OUTPUT="${AGAVE_JOB_COMPRESS_OUTPUT}"'

echo "##################################################"
echo "# Job Runtime Environment "
echo "##################################################"
echo "\n"
# print environment
env

# copy to file for usage later on
env > ./environment.out

CALLBACK=$(${command})

${AGAVE_JOB_CALLBACK_NOTIFICATION|CALLBACK}

sleep 3
