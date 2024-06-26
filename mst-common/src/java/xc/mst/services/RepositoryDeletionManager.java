/**
 * Copyright (c) 2009 eXtensible Catalog Organization
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
 * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
 * website http://www.extensiblecatalog.org/.
 *
 */
package xc.mst.services;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import xc.mst.bo.harvest.HarvestSchedule;
import xc.mst.bo.provider.Set;
import xc.mst.bo.record.Record;
import xc.mst.bo.record.RecordCounts;
import xc.mst.constants.Status;
import xc.mst.dao.DataException;
import xc.mst.manager.harvest.ScheduleService;
import xc.mst.repo.Repository;
import xc.mst.scheduling.WorkerThread;
import xc.mst.utils.MSTConfiguration;
import xc.mst.utils.TimingLogger;

/**
 * @author JohnB
 * 
 */
public class RepositoryDeletionManager extends WorkerThread {

    private final static Logger LOG = Logger.getLogger(RepositoryDeletionManager.class);
    private HarvestSchedule m_harvestSchedule = null;

    protected int m_processedRecordCount = 0;
    protected Repository m_incomingRepository = null;

    public Repository getIncomingRepository() {
        return m_incomingRepository;
    }

    public void setIncomingRepository(Repository incomingRepository) {
        m_incomingRepository = incomingRepository;
    }

    public void setHarvestSchedule(HarvestSchedule s) {
        m_harvestSchedule = s;
    }

    public HarvestSchedule getHarvestSchedule() {
        return m_harvestSchedule;
    }

    @Override
    public void setup() {
        this.incomingRecordCounts = new RecordCounts(new Date(), RecordCounts.INCOMING);
    }

    @Override
    public String getName() {
        return "RepositoryDeletionManager-repos=" + m_incomingRepository;
    }

    private List<Record> getMoreRecords(Repository r, long id) {
        return r.getRecords(new Date(0), new Date(), id, null, null);
    }

    public boolean doSomeWork() {
        LOG.debug("**** RepositoryDeletionManager.doSomeWork() begin method " + getName());
        if (m_incomingRepository != null) {
            long id = 0;
            long numRecordsProcess = 0;
            List<Record> records = getMoreRecords(m_incomingRepository, id);
            while (records != null && records.size() > 0 && this.status != Status.CANCELED) {
                for (Record r : records) {
                    if (r.getSets() != null) {
                        for (Set s : r.getSets()) {
                            this.incomingRecordCounts.incr(type, Record.DELETED, r.getStatus());
                        }
                    }
                    this.incomingRecordCounts.incr(null, Record.DELETED, r.getStatus());
                    r.setStatus(Record.DELETED);
                    r.setUpdatedAt(null);
                    m_incomingRepository.addRecord(r);
                    m_incomingRepository.commitIfNecessary(false, numRecordsProcess++, this.incomingRecordCounts, null);
                    m_processedRecordCount++;
                    id = r.getId();
                }
                // based on the local test result that the records come in batches of 1000, if this does not remain true
                // may not ever get TimingLogger data
                if (id % 5000 == 0) {
                    TimingLogger.reset();
                }
                records = getMoreRecords(m_incomingRepository, id);

            }
            m_incomingRepository.commitIfNecessary(true, 0, this.incomingRecordCounts, null);

            // and now, since harvest schedule doesn't seem to get auto-deleted, proceed to delete it...
            deleteHarvestSchedule();

            LOG.debug("RepositoryDeletionManager.doSomeWork() end of method processed " + m_processedRecordCount + " records. lastID=" + id);
        } else {
            LOG.debug("RepositoryDeletionManager.doSomeWork() end of method - no records to process, incomingRepository = NULL!");
        }
        return false;
    }

    private void deleteHarvestSchedule() {
        try {
            if (m_harvestSchedule != null) {
                ScheduleService service = (ScheduleService) MSTConfiguration.getInstance().getBean("ScheduleService");
                if (service != null) {
                    service.deleteSchedule(m_harvestSchedule);
                    LOG.debug("RepositoryDeletionManager.doSomeWork() deleted harvestSchedule");
                } else {
                    LOG.error("NOTICE: Unable to delete harvestSchedule (NULL ScheduleService)!");
                }
            } else {
                LOG.debug("NOTICE: harvestSchedule NULL!");
            }
        } catch (DataException e) {
            LOG.error("ERROR: RepositoryDeletionManager.doSomeWork() exception deleting harvestSchedule", e);
        }
    }

    @Override
    public String getDetailedStatus() {
        return null;
    }

    @Override
    public long getRecordsProcessedThisRun() {
        return m_processedRecordCount;
    }

    @Override
    public long getRecords2ProcessThisRun() {
        return m_incomingRepository.getNumRecords();
    }

}
