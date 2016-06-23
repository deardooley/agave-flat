IFILE=${query1}
PLL=${printLongestLine}

OUTPUT_FILE=wc_out.txt

ARGS=" -clw"

if [[ -n $PLL ]]; then
	ARGS="${ARGS}m"
fi

set -x

LFILE=$(basename ${IFILE})

wc ${ARGS} ${IFILE} > ${OUTPUT_FILE}

set +x

tar czvf output.tar.gz ${OUTPUT_FILE}