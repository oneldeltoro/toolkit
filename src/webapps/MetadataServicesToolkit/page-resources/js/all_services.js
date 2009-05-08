 /*
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

YAHOO.namespace("xc.mst.services.listServices");

YAHOO.xc.mst.services.listServices = {

 downloadFile: function(type,id)
            {
                window.location = "pages/logs/downloadLogFile.jsp?logType="+type+"&id="+id;
            },

	/**
	 *  Dialog to confirm service delete
	 */
	createDeleteServiceDialog : function()
	{
	
	
		// Define various event handlers for Dialog
		var handleSubmit = function() {
		    YAHOO.util.Connect.setForm('deleteService');
		    
		    //delete the service
		    var cObj = YAHOO.util.Connect.asyncRequest('post',
			        'deleteService.action', callback);
		};
		
			
		// handle a cancel of deleting service dialog
		var handleCancel = function() {
		    YAHOO.xc.mst.services.deleteServiceDialog.hide();
		};
		
		// handle a Yes of deleting service dialog
		var handleYes = function() {
			document.deleteServiceRecords.submit();
		    YAHOO.xc.mst.services.deleteServiceOkDialog.hide();
		};

		// handle a No of deleting service dialog
		var handleNo = function() {
		    YAHOO.xc.mst.services.deleteServiceOkDialog.hide();
		};		
		
		var handleSuccess = function(o) {
		
		    //get the response from adding a service
		    var response = eval("("+o.responseText+")");
	
		    
		    //if the service was not deleted then show the service the error message.
		    // received from the server
		    if( response.serviceDeleted == "false" )
		    {
		    	YAHOO.xc.mst.services.deleteServiceDialog.hide();
		        var deleteServiceError = document.getElementById('deleteServiceError');
			             deleteServiceError.innerHTML = '<p id="newDeleteServiceError">' 
 			            + response.message + '</p>';
 		            YAHOO.xc.mst.services.deleteServiceOkDialog.showDialog();
 		            
		    }
		    else
		    {
		        // we can clear the form if the services were deleted
		        YAHOO.xc.mst.services.deleteServiceDialog.hide();
		        window.location = 'listServices.action?isAscendingOrder=true&amp;columnSorted=ServiceName';
		    }
		};
		
		// handle form submission failure
		var handleFailure = function(o) {
		    alert('service submission failed ' + o.status);
		};
		
		
	
		// Instantiate the Dialog
		// make it modal - 
		// it should not start out as visible - it should not be shown until 
		// new service button is clicked.
		YAHOO.xc.mst.services.deleteServiceDialog = new YAHOO.widget.Dialog('deleteServiceDialog', 
	        { width : "400px",
			  visible : false, 
			  modal : true,
			  buttons : [ { text:'Yes', handler:handleSubmit },
						  { text:'No', handler:handleCancel, isDefault:true } ]
			} );
		
			
	       // Show the dialog
	       YAHOO.xc.mst.services.deleteServiceDialog.showDialog = function()
	       {
	           YAHOO.xc.mst.services.deleteServiceDialog.show();
	           YAHOO.xc.mst.services.deleteServiceDialog.center();
	       }

	    	   
		// Wire up the success and failure handlers
		var callback = { success: handleSuccess,  failure: handleFailure };


		// Render the Dialog
		YAHOO.xc.mst.services.deleteServiceDialog.render();

		
		YAHOO.xc.mst.services.deleteServiceOkDialog = new YAHOO.widget.Dialog('deleteServiceOkDialog', 
		{ width : "400px",
			  visible : false, 
			  modal : true,
			  buttons : [ { text:'Yes', handler:handleYes},
			  				 { text:'No', handler:handleNo, isDefault:true }]
		} );	

	       // Show the dialog with error message
	       YAHOO.xc.mst.services.deleteServiceOkDialog.showDialog = function()
	       {
	        	YAHOO.xc.mst.services.deleteServiceOkDialog.show();
	        	YAHOO.xc.mst.services.deleteServiceOkDialog.center();
	       }

		// Render the Dialog
		YAHOO.xc.mst.services.deleteServiceOkDialog.render();

	},

	deleteService : function(serviceId) {

					document.getElementById('service_id').value = serviceId;
			document.getElementById('service_delete_id').value = serviceId;
			
			YAHOO.xc.mst.services.deleteServiceDialog.showDialog();
		
	},

	/** 
	 * initialize the page 
	 */ 
	init : function() 
	{
	    YAHOO.xc.mst.services.createDeleteServiceDialog();
	}

}

// initialize the code once the dom is ready
YAHOO.util.Event.onDOMReady(YAHOO.xc.mst.services.init);
