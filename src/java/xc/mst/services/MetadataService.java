/**
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

package xc.mst.services;


import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import xc.mst.bo.processing.Job;
import xc.mst.bo.processing.ProcessingDirective;
import xc.mst.bo.provider.Format;
import xc.mst.bo.provider.Set;
import xc.mst.bo.record.Record;
import xc.mst.bo.service.Service;
import xc.mst.bo.user.User;
import xc.mst.constants.Constants;
import xc.mst.dao.DataException;
import xc.mst.dao.DatabaseConfigException;
import xc.mst.dao.processing.DefaultProcessingDirectiveDAO;
import xc.mst.dao.processing.ProcessingDirectiveDAO;
import xc.mst.dao.provider.DefaultFormatDAO;
import xc.mst.dao.provider.DefaultSetDAO;
import xc.mst.dao.provider.FormatDAO;
import xc.mst.dao.provider.SetDAO;
import xc.mst.dao.record.DefaultXcIdentifierForFrbrElementDAO;
import xc.mst.dao.record.XcIdentifierForFrbrElementDAO;
import xc.mst.dao.service.DefaultOaiIdentiferForServiceDAO;
import xc.mst.dao.service.DefaultServiceDAO;
import xc.mst.dao.service.OaiIdentifierForServiceDAO;
import xc.mst.dao.service.ServiceDAO;
import xc.mst.dao.user.DefaultGroupDAO;
import xc.mst.dao.user.DefaultUserGroupUtilDAO;
import xc.mst.dao.user.GroupDAO;
import xc.mst.dao.user.UserGroupUtilDAO;
import xc.mst.email.Emailer;
import xc.mst.manager.IndexException;
import xc.mst.manager.processingDirective.DefaultJobService;
import xc.mst.manager.processingDirective.JobService;
import xc.mst.manager.record.DefaultRecordService;
import xc.mst.manager.record.RecordService;
import xc.mst.utils.LogWriter;
import xc.mst.utils.MSTConfiguration;
import xc.mst.utils.index.RecordList;
import xc.mst.utils.index.Records;
import xc.mst.utils.index.SolrIndexManager;

/**
 * A copy of the MST is designed to interface with one or more Metadata Services depending on how it's configured.
 * There are several Metadata Services which may be used, each one of which extends the MetadataService
 * class.  The MetadataService class provides a common interface through which the MST can invoke functionality on
 * a Metadata Service.
 *
 * @author Eric Osisek
 */
public abstract class MetadataService
{
	/**
	 * The logger object
	 */
	protected static Logger log = Logger.getLogger(Constants.LOGGER_PROCESSING);

	/**
	 * The service representing this service in the database
	 */
	protected Service service = null;

	/**
	 * The name of this service
	 */
	private String serviceName = null;

	/**
	 * The number of warnings in running the current service
	 */
	private int warningCount = 0;

	/**
	 * The number of errors in running the current service
	 */
	private int errorCount = 0;
	
	/**
	 * The number of errors in running the current service per commit
	 */
	private int errorCountPerCommit = 0;

	/**
	 * Data access object for getting services
	 */
	private static ServiceDAO serviceDao = new DefaultServiceDAO();

	/**
	 * Data access object for getting sets
	 */
	private static SetDAO setDao = new DefaultSetDAO();

	/**
	 * Data access object for getting formats
	 */
	private static FormatDAO formatDao = new DefaultFormatDAO();

	/**
	 * Data access object for getting OAI IDs
	 */
	private static OaiIdentifierForServiceDAO oaiIdDao = new DefaultOaiIdentiferForServiceDAO();

	/**
	 * Data access object for getting FRBR level IDs
	 */
	private static XcIdentifierForFrbrElementDAO frbrLevelIdDao = new DefaultXcIdentifierForFrbrElementDAO();

	/**
	 * Manager for getting, inserting and updating records
	 */
	private static RecordService recordService = new DefaultRecordService();
	
	/**
	 * Manager for getting, inserting and updating jobs
	 */
	private static JobService jobService = new DefaultJobService();

	/**
	 * The processing directives for this service
	 */
	private List<ProcessingDirective> processingDirectives = null;

	/**
	 * A list of services to run after this service's processing completes
	 * The keys are the service IDs and the values are the IDs of the sets
	 * that service's records should get added to
	 */
	private HashMap<Integer, Integer> servicesToRun = new HashMap<Integer, Integer>();

	/**
	 * The flag for indicating cancel service operation.
	 */
	private boolean isCanceled;

	/**
	 * The flag for indicating service operation pause.
	 */
	private boolean isPaused;



	/**
	 * The number of records processed by the service so far
	 */
	protected int processedRecordCount = 0;
	
	/**
	 * The number of records the service needs to process
	 */
	private int totalRecordCount = 0;

	/**
	 * The number of input records 
	 */
	protected int inputRecordCount = 0;
	
	/**
	 * The output set id 
	 */
	protected Set outputSet;

	/**
	 * Used to send email reports
	 */
	private Emailer mailer = new Emailer();

	/**
	 * Data access object for getting user info from groups
	 */
	UserGroupUtilDAO userGroupUtilDAO = new DefaultUserGroupUtilDAO();
	
	/**
	 * Data access object for getting processing directives
	 */
	private static ProcessingDirectiveDAO processingDirectiveDao = new DefaultProcessingDirectiveDAO();
	
	/** Stores the unprocessed record identifiers */
	protected ArrayList<String> unprocessedErrorRecordIdentifiers = new ArrayList<String>();
	
	/**
	 * Data access object for getting groups
	 */
	GroupDAO groupDAO = new DefaultGroupDAO();
	
	public final boolean runService(int serviceId, int outputSetId) {
		
		try {
			// Set the service's ID and name
			setServiceId(serviceId);
			setServiceName(service.getName());
	
			// Load the service's configuration
			loadConfiguration(service.getServiceConfig());
	
			// Create the list of ProcessingDirectives which could be run on records processed from this service
			setProcessingDirectives(processingDirectiveDao.getBySourceServiceId(serviceId));
	
			if(log.isDebugEnabled())
				log.debug("Constructed the MetadataService Object, running its processRecords() method.");
	
			LogWriter.addInfo(service.getServicesLogFileName(), "Starting the " + service.getName() + " Service.");
	
			if(log.isDebugEnabled())
				log.debug("Validating the Metadata Service with ID " + serviceId + ".");
	
			checkService(Constants.STATUS_SERVICE_RUNNING, true);
	
			if(log.isDebugEnabled())
				log.debug("Running the Metadata Service with ID " + serviceId + ".");
	
			setOutputSet(setDao.getById(outputSetId));
			// Run the service's processRecords method
			boolean success = processRecords();
	
			LogWriter.addInfo(service.getServicesLogFileName(), "The " + service.getName() + " Service finished running.  " + processedRecordCount + " records processed." + (processedRecordCount - errorCount) + " records were processed successfully. " + errorCount + " were not processed due to error.");
	
			// Update database with status of service
			if(!isCanceled && success)
				setStatus(Constants.STATUS_SERVICE_NOT_RUNNING);
	
			sendReportEmail(null);
			return success;
		} catch (DatabaseConfigException dce) {
				log.error("Exception occurred while invoking the service's processRecords method.", dce);

				// Update database with status of service
				service.setStatus(Constants.STATUS_SERVICE_ERROR);
				sendReportEmail("Exception occurred while invoking the service's processRecords method.");
				
				LogWriter.addError(service.getServicesLogFileName(), "An internal error occurred while trying to start the " + service.getName() + " Service.");

				// Load the provider again in case it was updated during the harvest
				try
				{
					service = serviceDao.getById(service.getId());
				}
				catch (DatabaseConfigException e1)
				{
					log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e1);

					return false;
				}

				// Increase the warning and error counts as appropriate, then update the provider
				service.setServicesErrors(service.getServicesErrors() + 1);

				try
				{
					serviceDao.update(service);
				}
				catch (DataException e2)
				{
					log.warn("Unable to update the service's warning and error counts due to a Data Exception.", e2);
				}

				return false;

		}
	}
		


	/**
	 * This method gets called to give the service the service specific configuration
	 * which was defined for it in its configuration file.
	 *
	 * @param config The service specific configuration defined in the service's configuration file
	 */
	public abstract void loadConfiguration(String config);



	/**
	 * The MST calls this method to signal the Metadata Service to process the records.  Depending on the
	 * service, this method might look at all records in the database or it might just look at the
	 * unprocessed ones.  The type of processing that occurs will also be service specific.
	 *
	 * This method will process as many records as possible, creating a new list of records which contains
	 * the records which resulted from processing the existing ones.  Each record in the Lucene index will
	 * store a list of the record(s) it was processed from.  A record may be processed from multiple
	 * records, and more than one record may be processed from a single record.
	 *
	 * This method will return true if all processing worked perfectly and false if there were errors.  If
	 * it returns false, it will still have performed as much processing as possible.
	 *
	 * @param outputSetId The set to which processed records should be added, or -1 if they should not be added to an additional set
	 * @return true if all processing worked perfectly, false if there were errors.
	 */
	public boolean processRecords()
	{
		try
		{
			// Get the list of record inputs for this service
			Records records = recordService.getInputForServiceToProcess(service.getId());
			totalRecordCount = records.size();
			log.info("Number of records to be processed by service = " + totalRecordCount);
			
			long startTime = new Date().getTime();
			long endTime = 0;
			long timeDiff = 0;
			
			
			// Iterate over the list of input records and process each.
			// Then run the processing directives on the results of each and add
			// the appropriate record inputs for services to be run on the records
			// resulting from the processing.  Also maintain a list of services to
			// be invoked after this service is finished.  Finally, add the records
			// resulting from this service.
			for(Record processMe : records)
			{
				// If the service is not canceled and not paused then continue
				if(!isCanceled && !isPaused)
				{
					processRecord(processMe);
					if(processedRecordCount % 100000 == 0)
					{
						SolrIndexManager.getInstance().commitIndex();
						
						updateServiceStatistics();
						
						// Updates the database with latest record id and OAI identifier used.
						// So that in case of server down, the service will resume correctly.  
						updateOAIRecordIds();
						
						endTime = new Date().getTime();
						timeDiff = endTime - startTime;
						
						LogWriter.addInfo(service.getServicesLogFileName(), "Processed " + processedRecordCount + " records so far. Time taken = " + (timeDiff / (1000*60*60)) + "hrs  " + ((timeDiff % (1000*60*60)) / (1000*60)) + "mins  " + (((timeDiff % (1000*60*60)) % (1000*60)) / 1000) + "sec  " + (((timeDiff % (1000*60*60)) % (1000*60)) % 1000) + "ms  ");
						
						startTime = new Date().getTime();
					}
				}
				else
					{
						// If canceled the stop processing records
						if(isCanceled)
							{
								LogWriter.addInfo(service.getServicesLogFileName(), "Cancelled Service " + serviceName);
								LogWriter.addInfo(service.getServicesLogFileName(), "Processed " + processedRecordCount + " records so far.");
								// Update database with status of service
								setStatus(Constants.STATUS_SERVICE_CANCELED);
								break;
							}
						// If paused then wait
						else if(isPaused)
							{
								LogWriter.addInfo(service.getServicesLogFileName(), "Paused Service " + serviceName);
								// Update database with status of service
								setStatus(Constants.STATUS_SERVICE_PAUSED);

								while(isPaused && !isCanceled)
									{
										LogWriter.addInfo(service.getServicesLogFileName(), "Service Waiting to resume" );
										Thread.sleep(3000);
									}
								// If the service is canceled after it is paused, then exit
								if(isCanceled)
								{
									LogWriter.addInfo(service.getServicesLogFileName(), " Cancelled Service " + serviceName);
									// Update database with status of service
									setStatus(Constants.STATUS_SERVICE_CANCELED);
									break;

								}
								// If the service is resumed after it is paused, then continue
								else
								{
									LogWriter.addInfo(service.getServicesLogFileName(), "Resumed Service " + serviceName);
									// Update database with status of service
									setStatus(Constants.STATUS_SERVICE_RUNNING);

								}

							}

					}
			} // end loop over records to process

			// Reopen the reader so it can see the changes made by running the service
			SolrIndexManager.getInstance().commitIndex();

			// Get the results of any final processing the service needs to perform
			finishProcessing();
			
			// Reopen the reader so it can see the changes made by running the service
			SolrIndexManager.getInstance().commitIndex();
			
			endTime = new Date().getTime();
			timeDiff = endTime - startTime;
			LogWriter.addInfo(service.getServicesLogFileName(), "Processed " + processedRecordCount + " records so far. Time taken = " + (timeDiff / (1000*60*60)) + "hrs  " + ((timeDiff % (1000*60*60)) / (1000*60)) + "mins  " + (((timeDiff % (1000*60*60)) % (1000*60)) / 1000) + "sec  " + (((timeDiff % (1000*60*60)) % (1000*60)) % 1000) + "ms  ");
			
			
			return true;
		} // end try(process the records)
		catch(Exception e)
		{
			log.error("An error occurred while running the service with ID " + service.getId() + ".", e);
			
			try {
				// Commit processed records to index
				SolrIndexManager.getInstance().commitIndex();
			} catch (IndexException ie) {
				log.error("Exception occured when commiting " + service.getName() + " records to index", e);
			}
			
			// Update database with status of service
			setStatus(Constants.STATUS_SERVICE_ERROR);
			
			return false;
		} // end catch(Exception)
		finally // Update the error and warning count for the service
		{
			
			if (!updateServiceStatistics()) {
				return false;
			}
			
			// Updates the database with latest record id and OAI identifier used.
			updateOAIRecordIds();


		} // end finally(write the next IDs to the database)
	} // end method processRecords(int)

	/*
	 * Updates the database with latest record id and OAI identifier used.
	 * So that in case of server down, the service will resume correctly.  
	 */
	private void updateOAIRecordIds() {
		// Update the next OAI ID for this service in the database
		oaiIdDao.writeNextOaiId(service.getId());

		// Update the next XC ID for all elements in the database
		frbrLevelIdDao.writeNextXcId(XcIdentifierForFrbrElementDAO.ELEMENT_ID_WORK);
		frbrLevelIdDao.writeNextXcId(XcIdentifierForFrbrElementDAO.ELEMENT_ID_EXPRESSION);
		frbrLevelIdDao.writeNextXcId(XcIdentifierForFrbrElementDAO.ELEMENT_ID_MANIFESTATION);
		frbrLevelIdDao.writeNextXcId(XcIdentifierForFrbrElementDAO.ELEMENT_ID_HOLDINGS);
		frbrLevelIdDao.writeNextXcId(XcIdentifierForFrbrElementDAO.ELEMENT_ID_ITEM);
		frbrLevelIdDao.writeNextXcId(XcIdentifierForFrbrElementDAO.ELEMENT_ID_RECORD);
	}
	
	/*
	 * Update number of warnings, errors, records available, output & input records count
	 */
	private boolean updateServiceStatistics() {
		// Load the provider again in case it was updated during the harvest
		Service service = null;
		try
		{
			service = serviceDao.getById(this.service.getId());
		}
		catch (DatabaseConfigException e1)
		{
			log.error("DatabaseConfig exception occured when getting service from database to update error, warning count.", e1);

			return false;
		}

		// Increase the warning and error counts as appropriate, then update the provider
		service.setServicesWarnings(service.getServicesWarnings() + warningCount);
		service.setServicesErrors(service.getServicesErrors() + errorCountPerCommit);
		service.setInputRecordCount(service.getInputRecordCount() + inputRecordCount);
		try {
			long harvestOutRecordsAvailable = recordService.getCount(null, null, null, -1, service.getId());
			service.setHarvestOutRecordsAvailable(harvestOutRecordsAvailable);
			service.setOutputRecordCount((int) harvestOutRecordsAvailable);

			/* In case of Normalization Service update, the deleted records are updated.
			 * So input record count will be zero, so count of output records is assigned to input count
			 * Since for normalization it is 1:1 i/p record to o/p.
			 */
			if (service.getInputRecordCount() == 0) {
				service.setInputRecordCount((int) harvestOutRecordsAvailable);
			}

		} catch (IndexException ie) {
			log.error("Index exception occured while querying Solr for number of output records in service " + service.getName() + ".", ie);
			return false;
		}
		
		try
		{
			serviceDao.update(service);
		}
		catch (DataException e)
		{
			log.error("Unable to update the service's warning and error counts due to a Data Exception.", e);
			return false;
		}
		
		warningCount = 0;
		errorCountPerCommit = 0;
		inputRecordCount = 0;

		return true;

	}
	


	/**
	 * Gets the cancel status of the service.
	 */
	public boolean isCanceled() {
		return isCanceled;
	}

	/**
	 * Gets the pause status of the service.
	 */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * Sets the pause status of the service.
	 */
	public void setPaused(boolean isPaused) {
		this.isPaused = isPaused;
	}

	/**
	 * Sets the cancel status of the service.
	 * @param isCanceled Flag indicating the cancel status of the service
	 */
	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	/**
	 * Gets the name for this service
	 *
	 * @return This service's name
	 */
	public String getServiceName()
	{
		return serviceName;
	} // end method getServiceName()

	/**
	 * Gets the status of the job
	 */
	public String getServiceStatus(){

		if(isCanceled)
			return Constants.STATUS_SERVICE_CANCELED;
		else if(isPaused)
			return Constants.STATUS_SERVICE_PAUSED;
		else 
			return Constants.STATUS_SERVICE_RUNNING;
	}


	/**
	 * @return the processedRecordCount
	 */
	public int getProcessedRecordCount() {
		return processedRecordCount;
	}

	/**
	 * @return the totalRecordCount
	 */
	public int getTotalRecordCount() {
		return totalRecordCount;
	}

	/**
	 * This method validates that the service is able to be run.
	 *
	 * @throws ServiceValidationException When the service is invalid
	 */
	protected abstract void validateService() throws ServiceValidationException;

	/**
	 * This method processes a single record.
	 *
	 * @param record The record to process
	 * @return A list of outgoing records that should be added, modified, or deleted
	 *         as a result of processing the incoming record
	 */
	protected abstract void processRecord(Record record) throws Exception ;

	/**
	 * This method gets called after all new records are processed.  If the service
	 * needs to do any additional processing after it processed all the input records,
	 * it should be done in this method.
	 */
	protected abstract void finishProcessing();

	/**
	 * Refreshes the index so all records are searchable.
	 */
	protected void refreshIndex()
	{
		try
		{
			SolrIndexManager.getInstance().waitForJobCompletion(5000);
			SolrIndexManager.getInstance().commitIndex();
		}
		catch (IndexException e)
		{
			log.error("An error occurred while commiting new records to the Solr index.", e);
		}
	}

	/**
	 * Updates a record in the index
	 */
	protected void updateRecord(Record record)
	{
		try
		{
			// TODO Method used only in aggregation service. Should the record's updatedAt(new Date()) be set?
			recordService.update(record);
		}
		catch (IndexException e)
		{
			log.error("An error occurred while updating a record in the Solr index.", e);
		}
		catch (DataException e)
		{
			log.error("An error occurred while updating a record in the Solr index.", e);
		}
	}
	
	/**
	 * Gets the next OAI identifier for the service
	 *
	 * @return The next OAI identifier for the service
	 */
	public String getNextOaiId()
	{
		return "oai:" + MSTConfiguration.getProperty(Constants.CONFIG_DOMAIN_NAME_IDENTIFIER) + ":" + MSTConfiguration.getInstanceName() + "/" + service.getIdentifier().replace(" ", "_") + "/" + oaiIdDao.getNextOaiIdForService(service.getId());
	}

	/**
	 * Gets a Format by name.  Subclasses of MetadataService can call this method to
	 * get a Format from the database.
	 *
	 * @param name The name of the target Format
	 * @return The Format with the passed name
	 * @throws DatabaseConfigException
	 */
	protected Format getFormatByName(String name) throws DatabaseConfigException
	{
		return formatDao.getByName(name);
	}
	
	/**
	 * Gets all records that contain the passed trait
	 *
	 * @param trait The trait of the records we're getting
	 * @return A list of records that have the passed trait
	 * @throws IndexException
	 */
	protected RecordList getByTrait(String trait) throws IndexException
	{
		return recordService.getByTrait(trait);
	}
	
	/**
	 * Gets the output record for the service with the passed OAI identifier
	 * 
	 * @param oaiId The OAI identifier of the record to get
	 * @return The output record for the service with the passed OAI identifier
	 * @throws IndexException 
	 * @throws DatabaseConfigException 
	 */
	protected Record getOutputByOaiId(String oaiId) throws IndexException, DatabaseConfigException
	{
		return recordService.getByOaiIdentifierAndService(oaiId, service.getId());
	}
	
	/**
	 * Gets the input record for the service with the passed OAI identifier
	 * 
	 * @param oaiId The OAI identifier of the record to get
	 * @return The input record for the service with the passed OAI identifier
	 * @throws IndexException 
	 * @throws DatabaseConfigException 
	 */
	protected Record getInputByOaiId(String oaiId) throws IndexException, DatabaseConfigException
	{
		return recordService.getInputForServiceByOaiIdentifier(oaiId, service.getId());
	}
	
	/**
	 * Adds a new set to the database
	 *
	 * @param setSpec The setSpec of the new set
	 * @param setName The display name of the new set
	 * @param setDescription A description of the new set
	 * @throws DataException If an error occurred while adding the set
	 */
	protected Set addSet(String setSpec, String setName, String setDescription) throws DataException
	{
		Set set = new Set();
		set.setSetSpec(setSpec);
		set.setDescription(setDescription);
		set.setDisplayName(setName);
		set.setIsRecordSet(true);
		set.setIsProviderSet(false);
		setDao.insert(set);
		return set;
	}

	/**
	 * Gets the set from the database with the passed setSpec
	 *
	 * @param setSpec The setSpec of the target set
	 * @return The set with the passed setSpec
	 * @throws DatabaseConfigException
	 */
	protected Set getSet(String setSpec) throws DatabaseConfigException
	{
		return setDao.getBySetSpec(setSpec);
	}

	/**
	 * Logs an info message in the service's log file
	 *
	 * @param message The message to log
	 */
	protected void logInfo(String message)
	{
		LogWriter.addInfo(service.getServicesLogFileName(), message);
	}

	/**
	 * Logs a warning message in the service's log file
	 *
	 * @param message The message to log
	 */
	protected void logWarning(String message)
	{
		LogWriter.addWarning(service.getServicesLogFileName(), message);
		warningCount++;
	}

	/**
	 * Logs an error message in the service's log file
	 *
	 * @param message The message to log
	 */
	protected void logError(String message)
	{
		LogWriter.addError(service.getServicesLogFileName(), message);
		errorCount++;
		errorCountPerCommit++;
	}

	/**
	 * Gets the organization code for the record
	 * @return
	 */
	protected abstract String getOrganizationCode();
	
	/**
	 * Inserts a record in the Lucene index and sets up RecordInput values
	 * for any processing directives the record matched so the appropriate
	 * services process the record
	 *
	 * @param record The record to insert
	 */
	protected void insertNewRecord(Record record) throws DataException, IndexException
	{
		try
		{
			record.setService(service);

			// Run the processing directives against the record we're inserting
			checkProcessingDirectives(record);

			if(!recordService.insert(record))
				log.error("Failed to insert the new record with the OAI Identifier " + record.getOaiIdentifier() + ".");
		} // end try(insert the record)
		catch (DataException e)
		{
			log.error("An exception occurred while inserting the record into the Lucene index.", e);
			throw e;
		} // end catch(DataException)
		catch (IndexException ie) {
			log.error("An exception occurred while inserting the record into the index.", ie);
			throw ie;
		}
	} // end method insertNewRecord(Record)

	/**
	 * Updates a record in the Lucene index and sets up RecordInput values
	 * for any processing directives the record matched so the appropriate
	 * services reprocess the record after the update
	 *
	 * @param newRecord The record as it should look after the update (the record ID is not set)
	 * @param oldRecord The record in the Lucene index which needs to be updated
	 */
	protected void updateExistingRecord(Record newRecord, Record oldRecord) throws DataException, IndexException
	{
		try
		{
			// Set the new record's ID to the old record's ID so when we call update()
			// on the new record it will update the correct record in the Lucene index
			newRecord.setId(oldRecord.getId());
			newRecord.setUpdatedAt(new Date());
			newRecord.setService(service);

			// Update the record.  If the update was successful,
			// run the processing directives against the updated record
			if(recordService.update(newRecord))
				checkProcessingDirectives(newRecord);
			else
				log.error("The update failed for the record with ID " + newRecord.getId() + ".");
			
		} // end try(update the record)
		catch (DataException e)
		{
			log.error("An exception occurred while updating the record into the index.", e);
			throw e;
		} // end catch(DataException)
		catch (IndexException ie) {
			log.error("An exception occurred while updating the record into the index.", ie);
			throw ie;
		}
	} // end method updateExistingRecord(Record, Record)

	/**
	 * Logs the status of the service to the database
	 * @throws DataException
	 */
	public void setStatus(String status)
	{

		// Load the provider again in case it was updated during the harvest
		Service service = null;
		try
		{
			service = serviceDao.getById(this.service.getId());
			LogWriter.addInfo(service.getServicesLogFileName(), "Setting the status of the service " +service.getName() +" as:" +status);
			service.setStatus(status);
			serviceDao.update(service);
		}
		catch (DatabaseConfigException e1)
		{
			log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e1);

		} catch(DataException e)
		{
			log.error("An error occurred while updating service status to database for service with ID" + service.getId() + ".", e);
		}
	}

	/**
	 * Sets the service ID for this service
	 *
	 * @param serviceId This service's ID
	 * @throws DatabaseConfigException
	 */
	private void setServiceId(int serviceId) throws DatabaseConfigException
	{
		this.service = serviceDao.getById(serviceId);
	} // end method setServiceId(int)

	/**
	 * Sets the name for this service
	 *
	 * @param serviceName This service's name
	 */
	private void setServiceName(String serviceName)
	{
		this.serviceName = serviceName;
	} // end method setServiceName(int)

	/**
	 * Sets the list of processing directives for this service
	 *
	 * @param processingDirectives The list of processing directives which should be run on records processed by this service
	 */
	private void setProcessingDirectives(List<ProcessingDirective> processingDirectives)
	{
		this.processingDirectives = processingDirectives;
	} // end method setProcessingDirectives(List<ProcessingDirective>)

	/**
	 * Runs the processing directives for this service against the record.  For all matching
	 * processing directives, adds the appropriate recordInput objects to the Lucene index.
	 * Also adds the service ID for all matched processing directives to the list of services
	 * to run when this service finishes.
	 *
	 * @param record The record to match against the processing directives
	 */
	private void checkProcessingDirectives(Record record)
	{
		// Don't check processing directives for subclasses of Record
		if(!record.getClass().getName().equals("xc.mst.bo.record.Record"))
			return;

		// Maintain a list of processing directives which were matched
		ArrayList<ProcessingDirective> matchedProcessingDirectives = new ArrayList<ProcessingDirective>();

		boolean matchedFormat = false;
		boolean matchedSet = false;

		// Loop over the processing directives and check if any of them match the record
		for(ProcessingDirective processingDirective : processingDirectives)
		{
			matchedFormat = false;
			matchedSet = false;
			
			// Check if the record matches any of the metadata formats for the current processing directive
			if(processingDirective.getTriggeringFormats().contains(record.getFormat())) {
				matchedFormat = true;
			}

			// check if the record is in any of the sets for the current processing directive
			if(processingDirective.getTriggeringSets() != null && processingDirective.getTriggeringSets().size() > 0)  {
				for(Set set : record.getSets())
				{
					if(processingDirective.getTriggeringSets().contains(set))
					{
						matchedSet = true;
						break;
					} 
				}
			} else {
				matchedSet = true;
			}
			
			if (matchedFormat && matchedSet) {
				matchedProcessingDirectives.add(processingDirective);
			}
		} // end loop over processing directives

		// Loop over the matched processing directives.  Add the appropriate record inputs and add the
		// correct services to the list of services to run after the harvest completes
		for(ProcessingDirective matchedProcessingDirective : matchedProcessingDirectives)
		{
			record.addInputForService(matchedProcessingDirective.getService());
			record.removeProcessedByService(matchedProcessingDirective.getService());

			Integer serviceId = new Integer(matchedProcessingDirective.getService().getId());

			if(!servicesToRun.containsKey(serviceId)) {
				
				int outputSetId = new Integer(matchedProcessingDirective.getOutputSet() == null ? 0 : matchedProcessingDirective.getOutputSet().getId());
				servicesToRun.put(serviceId, outputSetId);

				// Add jobs to database
				try {
					Job job = new Job(matchedProcessingDirective.getService(), outputSetId, Constants.THREAD_SERVICE);
					job.setOrder(jobService.getMaxOrder() + 1); 
					jobService.insertJob(job);
				} catch (DatabaseConfigException dce) {
					log.error("DatabaseConfig exception occured when ading jobs to database", dce);
				}

			}
		} // end loop over matched processing directives
	} // end method checkProcessingDirectives(Record)

	/**
	 * Reprocesses the passed record by all records that had processed it in the past
	 *
	 * @param record The record to reprocess
	 */
	public void reprocessRecord(Record record)
	{
		for(Service processingService : record.getProcessedByServices())
		{
			record.addInputForService(processingService);

			Integer serviceId = processingService.getId();

			if(!servicesToRun.containsKey(serviceId)) {
				servicesToRun.put(serviceId, 0);
				
				// Add jobs to database
				try {
					Job job = new Job(processingService, 0, Constants.THREAD_SERVICE);
					job.setOrder(jobService.getMaxOrder() + 1); 
					jobService.insertJob(job);
				} catch (DatabaseConfigException dce) {
					log.error("DatabaseConfig exception occured when ading jobs to database", dce);
				}
			}

		}
	}

	/**
	 * Checks whether or not the service is able to be run.  If the service is
	 * not runnable, logs the reason as an error in the service's log file and
	 * sets the service's status to "error".  Otherwise sets the service's status to
	 * the passed status.
	 *
	 * @param statusForSuccess The status of the service if it is runnable
	 * @param testSolr True to verify access to the Solr index, false otherwise
	 * @return True iff the service is runnable
	 */
	private boolean checkService(String statusForSuccess, boolean testSolr)
	{
		if(testSolr)
		{
			// Check that we can access the Solr index
			try
			{
				RecordList test = recordService.getInputForService(service.getId());
				if(test == null)
				{
					LogWriter.addError(service.getServicesLogFileName(), "Cannot run the service because we cannot access the Solr index.");
					service.setServicesErrors(service.getServicesErrors()+1);

					try
					{
						serviceDao.update(service);
					}
					catch(DataException e)
					{
						log.error("An error occurred while updating the service's error count.", e);
					}

					setStatus(Constants.STATUS_SERVICE_ERROR);

					return false;
				}
			}
			catch(Exception e)
			{
				LogWriter.addError(service.getServicesLogFileName(), "Cannot run the service because we cannot access the Solr index.");
				service.setServicesErrors(service.getServicesErrors()+1);

				try
				{
					serviceDao.update(service);
				}
				catch(DataException e1)
				{
					log.error("An error occurred while updating the service's error count.", e1);
				}

				setStatus(Constants.STATUS_SERVICE_ERROR);

				return false;
			}
		}

		try
		{
			validateService();
		}
		catch(ServiceValidationException e)
		{
			LogWriter.addError(service.getServicesLogFileName(), "Cannot run the service for the following reason: " + e.getMessage() + ".");
			service.setServicesErrors(service.getServicesErrors()+1);

			try
			{
				serviceDao.update(service);
			}
			catch(DataException e1)
			{
				log.error("An error occurred while updating the service's error count.", e1);
			}

			setStatus(Constants.STATUS_SERVICE_ERROR);

			return false;
		}

		if (statusForSuccess != null && statusForSuccess.length() > 0) {
			setStatus(statusForSuccess);
		}

		return true;
	}
	
	/**
	 * Validates the service with the passed ID
	 *
	 * @param serviceId The ID of the MetadataService to run
	 * @param successStatus The status of the MetadataService is the validation was successful
	 * @param testSolr True to test the connection to the index, false otherwise
	 */
	public static void checkService(int serviceId, String successStatus, boolean testSolr)
	{
		if(log.isDebugEnabled())
			log.debug("Entering MetadataService.checkService for the service with ID " + serviceId + ".");

		// Get the service
		Service serviceObj = null;
		try
		{
			serviceObj = serviceDao.getById(serviceId);
		}
		catch (DatabaseConfigException e3)
		{
			log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e3);

			return; // We can't connect to the database so we can't write the status
		}

		MetadataService serviceToTest = null;

		// The name of the class for the service specified in the configuration file.
		String targetClassName = serviceObj.getClassName();

		// Get the class for the service specified in the configuration file
		try
		{
			if(log.isDebugEnabled())
				log.debug("Trying to get the MetadataService class named " + targetClassName);

			// Get the class specified in the configuration file
			// The class loader for the MetadataService class
			ClassLoader serviceLoader = MetadataService.class.getClassLoader();

			// Load the class from the .jar file
			// TODO: Don't reload the class file each time.  Instead, load it into
			//       Tomcat once when the MST is started or the service is added/updated.
			//       This requires more research into Tomcat's class loaders
			URLClassLoader loader = new URLClassLoader(new URL[] { new File(serviceObj.getServiceJar()).toURI().toURL() }, serviceLoader);
			Class<?> clazz = loader.loadClass(targetClassName);

			serviceToTest = (MetadataService)clazz.newInstance();

			if(log.isDebugEnabled())
				log.debug("Found the MetadataService class named " + targetClassName + ", getting its constructor.");

			serviceToTest.service = serviceObj;

			// Set the service's ID and name
			serviceToTest.setServiceId(serviceId);
			serviceToTest.setServiceName(serviceObj.getName());

			// Load the service's configuration
			serviceToTest.loadConfiguration(serviceObj.getServiceConfig());

			// Create the list of ProcessingDirectives which could be run on records processed from this service
			serviceToTest.setProcessingDirectives(processingDirectiveDao.getBySourceServiceId(serviceId));

			if(log.isDebugEnabled())
				log.debug("Constructed the MetadataService Object, running its processRecords() method.");

			LogWriter.addInfo(serviceObj.getServicesLogFileName(), "Validated the " + serviceObj.getName() + " Service.");

			if(log.isDebugEnabled())
				log.debug("Validating the Metadata Service with ID " + serviceId + ".");

			serviceToTest.checkService(successStatus, testSolr);
		} // end try(run the service through reflection)
		catch(ClassNotFoundException e)
		{
			log.error("Could not find class " + targetClassName, e);

			LogWriter.addError(serviceObj.getServicesLogFileName(), "Tried to validate the " + serviceObj.getName() + " Service, but the java class " + targetClassName + " could not be found.");

			// Increase the warning and error counts as appropriate, then update the provider
			serviceObj.setServicesErrors(serviceObj.getServicesErrors() + 1);

			try
			{
				serviceDao.update(serviceObj);
			}
			catch (DataException e2)
			{
				log.warn("Unable to update the service's warning and error counts due to a Data Exception.", e2);
			}

			// Update database with status of service
			try
			{
				serviceObj.setStatus(Constants.STATUS_SERVICE_ERROR);
				serviceDao.update(serviceObj);
			}
			catch(DataException e1)
			{
				log.error("An error occurred while updating service status to database for service with ID" + serviceObj.getId() + ".", e1);
			}
		} // end if(service is not user defined)
		catch(NoClassDefFoundError e)
		{
			log.error("Could not find class " + targetClassName, e);

			LogWriter.addError(serviceObj.getServicesLogFileName(), "Tried to validate the " + serviceObj.getName() + " Service, but the java class " + targetClassName + " could not be found.");

			// Load the provider again in case it was updated during the harvest
			try
			{
				serviceObj = serviceDao.getById(serviceObj.getId());
			}
			catch (DatabaseConfigException e3)
			{
				log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e3);

				return;
			}

			// Increase the warning and error counts as appropriate, then update the provider
			serviceObj.setServicesErrors(serviceObj.getServicesErrors() + 1);

			try
			{
				serviceDao.update(serviceObj);
			}
			catch (DataException e2)
			{
				log.warn("Unable to update the service's warning and error counts due to a Data Exception.", e2);
			}

			// Update database with status of service
			try
			{
				serviceObj.setStatus(Constants.STATUS_SERVICE_ERROR);
				serviceDao.update(serviceObj);
			}
			catch(DataException e1)
			{
				log.error("An error occurred while updating service status to database for service with ID" + serviceObj.getId() + ".", e1);
			}
		} // end catch(NoClassDefFoundError)
		catch(IllegalAccessException e)
		{
			log.error("IllegalAccessException occurred while invoking the service's checkRecords method.", e);

			LogWriter.addError(serviceObj.getServicesLogFileName(), "Tried to validate the " + serviceObj.getName() + " Service, but the java class " + targetClassName + "'s processRecords method could not be accessed.");

			// Load the provider again in case it was updated during the harvest
			try
			{
				serviceObj = serviceDao.getById(serviceObj.getId());
			}
			catch (DatabaseConfigException e3)
			{
				log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e3);
			}

			// Increase the warning and error counts as appropriate, then update the provider
			serviceObj.setServicesErrors(serviceObj.getServicesErrors() + 1);

			try
			{
				serviceDao.update(serviceObj);
			}
			catch (DataException e2)
			{
				log.warn("Unable to update the service's warning and error counts due to a Data Exception.", e2);
			}

			// Update database with status of service
			try
			{
				serviceObj.setStatus(Constants.STATUS_SERVICE_ERROR);
				serviceDao.update(serviceObj);
			}
			catch(DataException e1)
			{
				log.error("An error occurred while updating service status to database for service with ID" + serviceObj.getId() + ".", e1);
			}
		} // end catch(IllegalAccessException)
		catch(Exception e)
		{
			log.error("Exception occurred while invoking the service's checkRecords method.", e);

			LogWriter.addError(serviceObj.getServicesLogFileName(), "An internal error occurred while trying to validate the " + serviceObj.getName() + " Service.");

			// Load the provider again in case it was updated during the harvest
			try
			{
				serviceObj = serviceDao.getById(serviceObj.getId());
			}
			catch (DatabaseConfigException e3)
			{
				log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e3);
			}

			// Increase the warning and error counts as appropriate, then update the provider
			serviceObj.setServicesErrors(serviceObj.getServicesErrors() + 1);

			try
			{
				serviceDao.update(serviceObj);
			}
			catch (DataException e2)
			{
				log.warn("Unable to update the service's warning and error counts due to a Data Exception.", e2);
			}

			// Update database with status of service
			try
			{
				serviceObj.setStatus(Constants.STATUS_SERVICE_ERROR);
				serviceDao.update(serviceObj);
			}
			catch(DataException e1)
			{
				log.error("An error occurred while updating service status to database for service with ID" + serviceObj.getId() + ".", e1);
			}
		} // end catch(Exception)
	} // end method checkService(int)

	
	/**
	 * Builds and sends an email report about the harvest to the schedule's notify email address.
	 *
	 * @param problem The problem which prevented the harvest from finishing, or null if the harvest was successful
	 */
	public boolean sendReportEmail(String problem)
	{
		try {
			
			if (mailer.isConfigured()) {
				
				// The email's subject
				InetAddress addr = null;
				addr = InetAddress.getLocalHost();

				String subject = "Results of processing " + getServiceName() +" by MST Server on " + addr.getHostName();
		
				// The email's body
				StringBuilder body = new StringBuilder();
		
				// First report any problems which prevented the harvest from finishing
				if(problem != null)
					body.append("The service failed for the following reason: ").append(problem).append("\n\n");

				// Report on the number of records inserted successfully and the number of failed inserts
				body.append("Total number of records to process = " + totalRecordCount);
				body.append("\nNumber of records processed successfully = " + (processedRecordCount - errorCount));
				
				if(errorCount > 0) {
					body.append("\nNumber of records not processed due to error = " + errorCount);
					body.append("\nPlease login into MST and goto Menu -> Logs -> Services to see the list of failed records and the reason for failure.");
				}
				
				// Send email to every admin user
				for (User user : userGroupUtilDAO.getUsersForGroup(groupDAO.getByName(Constants.ADMINSTRATOR_GROUP).getId())) {
					mailer.sendEmail(user.getEmail(), subject, body.toString());
				}
				
				return true;
			} 
			else {
				return false;
			}

		}
		catch (UnknownHostException exp) {
			log.error("Host name query failed. Error sending notification email.",exp);
			return false;
		}
		catch (DatabaseConfigException e) {
			log.error("Database connection exception. Error sending notification email.");
			return false;
		}
		catch (Exception e) {
			log.error("Error sending notification email.");
			return false;
		}
	} // end method sendReportEmail

	public ArrayList<String> getUnprocessedErrorRecordIdentifiers() {
		return unprocessedErrorRecordIdentifiers;
	}

	public void setUnprocessedErrorRecordIdentifiers(
			ArrayList<String> unprocessedErrorRecordIdentifiers) {
		this.unprocessedErrorRecordIdentifiers = unprocessedErrorRecordIdentifiers;
	}

	public Set getOutputSet() {
		return outputSet;
	}

	public void setOutputSet(Set outputSet) {
		this.outputSet = outputSet;
	}

	public abstract void setInputRecordCount(int inputRecordCount);
	
	

}