Installation of the DB using docker:
1. Please ensure that a .env file exists within the sql directory with MYSQL_ROOT_PASSWORD, MYSQL_DATABASE, MYSQL_USER & MYSQL_PASSWORD
2. Navigate to the directory with the docker-compose.yml file
3. Run: docker-compose up -d
4. Check if the sql scripts ran properly by checking the logs: docker logs project_manager_mysql

If the DDL scripts are ever updated, the persisted volume must be deleted 1st before re-running the yaml file:
1. Navigate to the directory with the docker-compose.yml file
2. Run the command to delete the volume: docker-compose down -v
3. Re-run: docker-compose up -d

*NOTE: Running "docker-compose down -v" will DELETE ANY EXISTING  DATA
