folder="${BASE_DIR}/old_logs/$(date +%Y%m%d_%H%M%S)"
mkdir -p ${folder}

for file in $(ls -1 $MST_INSTANCE/logs/*.txt*) 
do
	cp ${file} ${folder}
done

for file in $(find $TOMCAT_HOME/logs/*)
do
	cp ${file} ${folder}
done
