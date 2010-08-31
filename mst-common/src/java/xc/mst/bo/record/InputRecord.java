package xc.mst.bo.record;

import java.util.List;

/**
 * @author Benjamin Anderson
 *
 * @see RecordIfc
 * @see OutputRecord
 * @see xc.mst.services.impl.GenericMetadataService#process(InputRecord)
 */
public interface InputRecord extends RecordIfc {
	
	/**
	 * @return If this is not the first time this service has processed
	 * this record and it previously produced OutputRecords, then they
	 * will be attached as successors.  <b>note: this OutputRecords will
	 * not contain the actual payload xml</b>
	 * 
	 * @see xc.mst.services.impl.GenericMetadataService#process(InputRecord)
	 */
	public List<OutputRecord> getSuccessors();

}
