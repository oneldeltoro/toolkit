/**
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

package xc.mst.action.services;

import xc.mst.action.*;
import xc.mst.action.processingDirective.*;
import com.opensymphony.xwork2.ActionSupport;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import xc.mst.bo.service.Service;
import xc.mst.constants.Constants;
import xc.mst.manager.processingDirective.DefaultServicesService;
import xc.mst.manager.processingDirective.ServicesService;

/**
 * Edits the details of a service
 *
 * @author Tejaswi Haramurali
 */
public class EditService extends ActionSupport
{
    /** Service object to interact with the services in the MST */
    private ServicesService servicesService = new DefaultServicesService();

    /** Denotes the type of error */
    private String errorType;

    /** List of XCCFG config files which were present in the directory (location of this directory can be obtained from the manual/documentation) */
    private List<String> serviceFiles = new ArrayList<String>();

    /** The XCCFG file selected by the user */
    private String selectedLocation;

    /** The ID of the service whose details are being edited */
    private int serviceId;

    /** The temporary service object which is used to populate data in the JSP page*/
    private Service temporaryService;

     /** A reference to the logger for this class */
    static Logger log = Logger.getLogger(Constants.LOGGER_GENERAL);

     /**
     * Overrides default implementation to view the edit service page.
     *
     * @return {@link #SUCCESS}
     */
    @Override
    public String execute()
    {
        try
        {
            temporaryService = servicesService.getServiceById(serviceId);
            setTemporaryService(temporaryService);
            File dir = new File("serviceConfig");
            
            FileFilter fileFilter =  new XCCGFileFilter();

            File[] fileList = dir.listFiles(fileFilter);
            for(int i=0;i<fileList.length;i++)
            {
                serviceFiles.add(fileList[i].getName());
            }
            setServiceFiles(serviceFiles);
            return SUCCESS;
        }
        catch(Exception e)
        {
            log.error("The edit-service page could not be loaded correctly",e);
            this.addFieldError("viewEditServiceError", "The edit-service page could not be loaded correctly");
            return INPUT;
        }
    }

    /**
     * The method that does the actual task of editing the details of a service
     *
     * @return {@link #SUCCESS}
     */
    public String editService()
    {
        try
        {
            String location = "serviceConfig/" + getSelectedLocation();
            File file = new File(location);
            servicesService.updateService(file, servicesService.getServiceById(serviceId));
            return SUCCESS;
        }
        catch(Exception e)
        {
            log.error("Error editing the service",e);
            errorType = "error";
            this.addFieldError("editServiceError",e.getMessage());
            File dir = new File("serviceConfig");
            FileFilter fileFilter =  new XCCGFileFilter();

            File[] fileList = dir.listFiles(fileFilter);
            for(int i=0;i<fileList.length;i++)
            {
                serviceFiles.add(fileList[i].getName());
            }
            setServiceFiles(serviceFiles);
            return INPUT;
        }
    }

     /**
     * Sets the temporary service object
     *
     * @param temporaryService service object
     */
    public void setTemporaryService(Service temporaryService)
    {
        this.temporaryService = temporaryService;
    }

    /**
     * Returns the temporary Service object
     *
     * @return service object
     */
    public Service getTemporaryService()
    {
        return this.temporaryService;
    }

    /**
     * Sets the service ID of the service whose details are to be edited
     *
     * @param serviceId service ID
     */
    public void setServiceId(int serviceId)
    {
        this.serviceId = serviceId;
    }

    /**
     * Returns the ID of the service whose details are to be edited
     *
     * @return service ID
     */
    public int getServiceId()
    {
        return this.serviceId;
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
     * Returns the list of XCCFG config files at the hard-coded location (location can be found in documentation/manual)
     *
     * @return list of config files
     */
    public List<String> getServiceFiles()
    {
        return this.serviceFiles;
    }

    /**
     * Sets the list of XCCFG config files which were found at the hard-coded location (location can be found in documentation/manual)
     *
     * @param serviceFileList list of config files
     */
    public void setServiceFiles(List<String> serviceFiles)
    {
        this.serviceFiles = serviceFiles;
    }

    /**
     * The XCCFG file that is selected by the user
     *
     * @param selectedLocation config file
     */
    public void setSelectedLocation(String selectedLocation)
    {
        this.selectedLocation = selectedLocation;
    }

    /**
     * Returns the XCCFG file selected by the user
     *
     * @return config file
     */
    public String getSelectedLocation()
    {
        return this.selectedLocation;
    }
}
