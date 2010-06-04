/**
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

package xc.mst.scheduling;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;

import xc.mst.bo.harvest.HarvestSchedule;
import xc.mst.bo.processing.Job;
import xc.mst.bo.processing.ProcessingDirective;
import xc.mst.constants.Constants;
import xc.mst.dao.DatabaseConfigException;
import xc.mst.manager.BaseManager;
import xc.mst.utils.MSTConfiguration;
import xc.mst.utils.TimingLogger;

/**
 * A Thread which runs in the background and checks every minute to see
 * if any harvests are scheduled to run.  If they are, the Harvester
 * will be invoked for all steps of the harvest schedule which was due to be invoked
 * This class also runs MetadataServices when the Harvester or another Service
 * matches a processing directive requiring the service to be run.
 *
 * This Thread maintains a queue of jobs to be run.  A job may be
 * either a harvest or a service.
 *
 * @author Eric Osisek
 */
public class Scheduler extends BaseManager implements Runnable
{
	/**
	 * A queue of WorkerThreads that are waiting to run harvests/services
	 */
	private static Queue<WorkerThread> waitingJobs = new LinkedList<WorkerThread>();

	/**
	 * The WorkerThread that is currently running harvests/services
	 */
	private static WorkerThread runningJob;
	protected Job previousJob = null;

	/**
	 * Gets the currently running job
	 */
	public static WorkerThread getRunningJob() {
		return runningJob;
	}

	/**
	 * Whether or not the scheduler has been killed
	 */
	boolean killed = false;

	static
	{
		// Abort if we could not find the configuration file.
		String logConfigFileLocation = MSTConfiguration.getInstance().getProperty(Constants.CONFIG_LOGGER_CONFIG_FILE_LOCATION);
		if(logConfigFileLocation == null)
		{
			System.err.println("The configuration file was invalid or did not exist.");
			System.exit(1);
		} // end else
	} // end static initializer

	/**
	 * A reference to the logger for this class
	 */
	static Logger log = Logger.getLogger(Constants.LOGGER_GENERAL);

	/**
	 * The Thread's run method.  This checks the database every minute
	 * for harvests which are scheduled to be run, and invokes the
	 * harvester on all steps for that schedule.  Steps invoked in this
	 * manner are queued such that we don't have too many simultaneously
	 * running harvests or services.
	 *
	 * This method also runs MetadataServices when the Harvester or another Service
	 * matches a processing directive requiring the service to be run.
	 */
	public void run()
	{
		
		Map<Integer, Long> lastRunDate = new HashMap<Integer, Long>();
		
		while(!killed)
		{
			// Get the current time
			Calendar now = Calendar.getInstance();

			// Get a list of harvest schedules which need to be run now
			List<HarvestSchedule> schedulesToRun = null;

			// Get the schedules to run
			try
			{
				schedulesToRun = getHarvestScheduleDAO().getSchedulesToRun(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.DAY_OF_WEEK), now.get(Calendar.MINUTE));
			}
			catch (DatabaseConfigException e1)
			{
				log.error("Cannot connect to the database with the parameters supplied in the configuration file.", e1);

				schedulesToRun = new ArrayList<HarvestSchedule>();
			}

			// Run each scheduled harvest
			for(HarvestSchedule scheduleToRun : schedulesToRun)
			{
				boolean alreadyRanThisMinute = false;
				if (lastRunDate.containsKey(scheduleToRun.getId())) {
					if (System.currentTimeMillis() - lastRunDate.get(scheduleToRun.getId()) < 60000) {
						alreadyRanThisMinute = true;
					}
				}
				// BDA: The Scheduler was tied to looping every 60 seconds.  I like to test faster
				//      than that so I changed it to loop every 3 seconds.  This added check is necessary
				//      because the 60 second loop assured that a job would not be started twice.  Instead
				//      I'll keep track of the last start time for each job.
				if(!alreadyRanThisMinute &&
						!scheduleToRun.getStatus().equals(Constants.STATUS_SERVICE_RUNNING) && 
						!scheduleToRun.getStatus().equals(Constants.STATUS_SERVICE_PAUSED))
				{
					if(log.isDebugEnabled())
						log.debug("Creating a Thread to run HarvestSchedule with id " + scheduleToRun.getId());
	
					// Add job to database queue
					try {
						Job job = new Job(scheduleToRun, Constants.THREAD_REPOSITORY);
						job.setOrder(getJobService().getMaxOrder() + 1); 
						jobService.insertJob(job);
						lastRunDate.put(scheduleToRun.getId(), System.currentTimeMillis());
					} catch (DatabaseConfigException dce) {
						log.error("DatabaseConfig exception occured when ading jobs to database", dce);
					}
				}
			} // end loop over schedules to be run

			if (runningJob == null || !runningJob.isAlive())
			{
				try {
					// Get next job to run
					if (previousJob != null) {
						TimingLogger.log("finished job: "+previousJob.getJobType());
						TimingLogger.log("runningJob: "+runningJob);
						TimingLogger.reset();
					}
					Job jobToStart = jobService.getNextJobToExecute();
					previousJob = jobToStart;

					// If there was a service job in the waiting queue, start it.  Otherwise break from the loop
					if(jobToStart != null)
					{
						TimingLogger.reset();
						TimingLogger.log("starting job: "+jobToStart.getJobType());
						
						// Start a new Thread to run the Harvester component for the schedule
						// BDA_TODO what's next after this job starts?  Stick that in the jobs table.
						
						if (jobToStart.getJobType().equalsIgnoreCase(Constants.THREAD_REPOSITORY)) {
							HarvesterWorkerThread harvestThread = new HarvesterWorkerThread();
							harvestThread.setHarvestScheduleId(jobToStart.getHarvestSchedule().getId());
							harvestThread.start();
							runningJob = harvestThread;
							
							List<ProcessingDirective> processingDirectives = getProcessingDirectiveDAO().getBySourceProviderId(
									jobToStart.getHarvestSchedule().getProvider().getId());
							try {
								for (ProcessingDirective pd : processingDirectives) {
									Job job = new Job(pd.getService(), pd.getOutputSet().getId(), Constants.THREAD_SERVICE);
									job.setOrder(jobService.getMaxOrder() + 1); 
									jobService.insertJob(job);	
								}
							} catch (DatabaseConfigException dce) {
								log.error("DatabaseConfig exception occured when ading jobs to database", dce);
							}
						} else if (jobToStart.getJobType().equalsIgnoreCase(Constants.THREAD_SERVICE)) {
							TimingLogger.log("service : "+jobToStart.getService().getClassName());
							ServiceWorkerThread serviceThread = new ServiceWorkerThread();
							serviceThread.setServiceId(jobToStart.getService().getId());
							serviceThread.setOutputSetId(jobToStart.getOutputSetId());
							serviceThread.start();
							runningJob = serviceThread;
							
							List<ProcessingDirective> processingDirectives = getProcessingDirectiveDAO().getBySourceServiceId(
									jobToStart.getService().getId());
							try {
								for (ProcessingDirective pd : processingDirectives) {
									Job job = new Job(pd.getService(), pd.getOutputSet().getId(), Constants.THREAD_SERVICE);
									job.setOrder(jobService.getMaxOrder() + 1); 
									jobService.insertJob(job);	
								}
							} catch (DatabaseConfigException dce) {
								log.error("DatabaseConfig exception occured when ading jobs to database", dce);
							}
						} else if (jobToStart.getJobType().equalsIgnoreCase(Constants.THREAD_SERVICE_REPROCESS)) {
							ServiceReprocessWorkerThread serviceReprocessWorkerThread = new ServiceReprocessWorkerThread();
							serviceReprocessWorkerThread.setServiceId(jobToStart.getService().getId());
							serviceReprocessWorkerThread.start();
							runningJob = serviceReprocessWorkerThread;
						} else if (jobToStart.getJobType().equalsIgnoreCase(Constants.THREAD_DELETE_SERVICE)) {
							DeleteServiceWorkerThread deleteServiceWorkerThread = new DeleteServiceWorkerThread();
							deleteServiceWorkerThread.setServiceId(jobToStart.getService().getId());
							deleteServiceWorkerThread.start();
							runningJob = deleteServiceWorkerThread;
						}

						// Delete the job from database once its scheduled to run
						jobService.deleteJob(jobToStart);
					} // end if(the service job queue was empty)
				} catch(DatabaseConfigException dce) {
					log.error("DatabaseConfigException occured when getting job from database", dce);
				}
			}

			// Sleep until the next hour begins
			try
			{
				if(log.isDebugEnabled())
					log.debug("Scheduler Thread sleeping for 1 minute.");
				Thread.sleep(3 * 1000);
			} // end try(sleep for 1 minute)
			catch(InterruptedException e)
			{
				if(log.isDebugEnabled())
					log.debug("Caught InteruptedException while sleeping in Scheduler Thread.");
			} // end catch(InterruptedException)
			catch(Throwable t)
			{
				log.error("", t);
			}
		} // end main loop
	} // end method run()

	/**
	 * Adds a WorkerThread to the queue of Threads to be run.
	 *
	 * @param scheduleMe The Thread to be run.
	 */
	public static void scheduleThread(WorkerThread scheduleMe)
	{
		waitingJobs.add(scheduleMe);
	} // end method scheduleThread(WorkerThread)
	
	/**
	 * Kills the Scheduling Thread
	 */
	public void kill()
	{
		killed = true;
	} // end method kill()

	/**
	 * Cancels the currently running service / harvest
	 */
	public static void cancelRunningJob(){

		runningJob.cancel();
	}

	/**
	 * Pauses the currently running service / harvest
	 */
	public static void pauseRunningJob(){

		runningJob.pause();
	}

	/**
	 * Resumes the currently running service / harvest
	 */
	public static void resumePausedJob(){

		runningJob.proceed();
	}

	/**
	 * Sets the currentJob reference to null after completion of the job.
	 */
	public static void setJobCompletion(){
		TimingLogger.log("setJobCompletion()");
		runningJob = null;
	}

} // end class Scheduler
