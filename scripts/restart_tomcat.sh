ant prepareWebApp

scripts/remote_1.sh

ssh ${REMOTE_USER}@${REMOTE_BOX_NAME} "cd ${REMOTE_BASE_DIR}; scripts/go.sh ${1} stop_tomcat"
scripts/push_code.sh
ssh ${REMOTE_USER}@${REMOTE_BOX_NAME} "cd ${REMOTE_BASE_DIR}; scripts/go.sh ${1} clear_stuff"
ssh ${REMOTE_USER}@${REMOTE_BOX_NAME} "cd ${REMOTE_BASE_DIR}; scripts/go.sh ${1} start_tomcat"