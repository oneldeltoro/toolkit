./scripts/clear_logs.sh
./scripts/remove_index.sh

#./scripts/run_sql.sh ../../sql/records.sql
#./scripts/run_sql.sh delete_all_jobs.sql
#./scripts/run_sql.sh delete_all_harvests.sql
#./scripts/run_sql.sh delete_records.sql
#./scripts/run_sql.sh copy_service.sql
cat ./scripts/sql/mst_dump.sql | sed "s/==provider_name==/${provider_name}/g" | sed "s/==provider_url==/${provider_url}/g" | sed "s/==service_id==/${service_id}/g" > ./scripts/sql/temp.sql
./scripts/run_sql.sh temp.sql