
/**
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

package xc.mst.action.repository;

import org.apache.log4j.Logger;

import xc.mst.bo.provider.Provider;
import xc.mst.constants.Constants;
import xc.mst.manager.repository.DefaultProviderService;
import xc.mst.manager.repository.ProviderService;

import com.opensymphony.xwork2.ActionSupport;

/**
 *This class is used to delete a repository from the database
 *
 * @author Tejaswi Haramurali
 */
public class DeleteRepository extends ActionSupport
{
        
    /** Generated by eclipse	 */
	private static final long serialVersionUID = 4498437059514909755L;

	/** A reference to the logger for this class */
    static Logger log = Logger.getLogger(Constants.LOGGER_GENERAL);

    /** The ID of the repository to be deleted */
    private int repositoryId;
    
	/** Error type */
	private String errorType; 
	
	/** Message explaining why the repository cannot be deleted */
	private String message;

	/** Provider service */
    private ProviderService providerService = new DefaultProviderService();
    
    /** Determines whether repository is deleted */
	private boolean deleted;

    /**
     * Overrides default implementation to delete a repository.
     * @return {@link #SUCCESS}
     */
    public String execute()
    {
        try
        {
            log.debug("DeleteRepository:execute():Repository Id to be deleted : " + repositoryId);
            Provider provider = providerService.getProviderById(repositoryId);
            
            // Delete provider only if it is not harvested.
            if (provider.getLastHarvestEndTime() != null) {
                message = "Repository has harvested data and cannot be deleted.";
                deleted = false;
            } else {
    	    	providerService.deleteProvider(provider);
            	deleted = true;
            }
            return SUCCESS;
        }
        catch(Exception e)
        {
            log.debug(e, e.fillInStackTrace());
            this.addFieldError("viewRepositoryError", "Repository cannot be deleted");
            errorType = "error";
            return INPUT;
        }
    }
    
	/**
     * Returns error type
     * 
     * @return error type
     */
	public String getErrorType() {
		return errorType;
	}

    /**
     * Sets error type
     * 
     * @param errorType error type
     */
	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	/**
	 * Returns the error message
	 * 
	 * @return error message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns true if repository deleted, else false
	 * 
	 * @return Returns true if repository deleted, else false
	 */
	public boolean isDeleted() {
		return deleted;
	}

    /**
     * Set the ID of the repository to be deleted
     * 
     * @param repositoryId The ID of the repository to be deleted
     */
    public void setRepositoryId(int repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    /**
     * Gets the ID of the repository to be deleted
     * 
     * @return The ID of the repository to be deleted
     */
    public int getRepositoryId()
    {
        return repositoryId;
    }

}
