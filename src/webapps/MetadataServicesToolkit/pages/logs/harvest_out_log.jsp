<!--
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the  
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/. 
  *
  -->

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt"%>

<!--  document type -->
<c:import url="/inc/doctype-frag.jsp"/>

<LINK href="page-resources/css/header.css" rel="stylesheet" type="text/css">


<html>
    <head>
        <title>Harvest-Out Logs</title>
        <c:import url="/inc/meta-frag.jsp"/>

        <LINK href="page-resources/yui/reset-fonts-grids/reset-fonts-grids.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/base-mst.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/yui/menu/assets/skins/sam/menu.css"  rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/global.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/main_menu.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/tables.css" rel="stylesheet" type="text/css" >
		<LINK href="page-resources/css/header.css" rel="stylesheet" type="text/css">
		<LINK href="page-resources/css/bodylayout.css" rel="stylesheet" type="text/css">

        <SCRIPT LANGUAGE="JavaScript" src="page-resources/yui/yahoo-dom-event/yahoo-dom-event.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" src="page-resources/yui/connection/connection-min.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" src="page-resources/yui/container/container_core-min.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" SRC="page-resources/yui/menu/menu-min.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" SRC="page-resources/js/main_menu.js"></SCRIPT>
        <script language="javascript" src="page-resources/js/log_harvestout.js"></script>
        
    </head>

    <body class="yui-skin-sam">
        <%@ taglib prefix="s" uri="/struts-tags" %>
        <!--  yahoo doc 2 template creates a page 950 pixles wide -->
        <div id="doc2">

		<!-- page header - this uses the yahoo page styling -->
		<div id="hd">

            <!--  this is the header of the page -->
            <c:import url="/inc/header.jsp"/>

            <!--  this is the header of the page -->
            <c:import url="/inc/menu.jsp"/>
             <jsp:include page="/inc/breadcrumb.jsp">

                    <jsp:param name="bread" value="Logs,Harvest-Out" />

             </jsp:include>
 		</div>
		<!--  end header -->

		<!-- body -->
		<div id="bd">
       
             <!-- Display of error message -->
                <c:if test="${errorType != null}">
                    <div class="${errorType}">
                        <img  src="${pageContext.request.contextPath}/page-resources/img/${errorType}.jpg">
                        <s:fielderror cssClass="errorMessage"/>
                    </div>
                 </c:if>

              <div align="right" style="margin-bottom:10px;">
                <button class="xc_button" type="button" onclick="javascript:YAHOO.xc.mst.log.resetAll();" name="next">Reset All *</button>
              </div>
                <div class="viewTable">
                    <table width="100%">
                        <thead>
                            <tr>
                                <td><a href="harvestOutLog.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=harvest_out_log_file_name">Service</a></td>
                                <td><a href="harvestOutLog.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=harvest_out_records_available">#Records available for harvest</a></td>
                                <td><a href="harvestOutLog.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=harvest_out_records_harvested">#Records harvested</a></td>
                                <td><a href="harvestOutLog.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=harvest_out_warnings">#Warnings</a></td>
                                <td><a href="harvestOutLog.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=harvest_out_errors">#Errors</a></td>
                                <td>Reset</td>
                                <td><a href="harvestOutLog.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=harvest_out_last_log_reset">Last Reset Date</a></td>
                            </tr>
                        </thead>
                        <tbody>
                              <c:forEach var="n" items="${serviceList}" varStatus="a">
                                  <tr>
                                      <td><a style="cursor:pointer;" onclick ="javascript:YAHOO.xc.mst.log.downloadFile('${n.id}')"><U>${n.name}</U></a></td>
                                      <td>${n.harvestOutRecordsAvailable}</td>
                                      <td>${n.harvestOutRecordsHarvested}</td>
                                      <td>${n.harvestOutWarnings}</td>
                                      <td>${n.harvestOutErrors}</td>
                                      <td><button class="xc_button" type="button" name="reset" onclick="javascript:YAHOO.xc.mst.log.resetFunction('${n.harvestOutLogFileName}','${n.id}')">Reset</button></td>
                                      <td>${n.harvestOutLastLogReset}</td>
                                  </tr>
                              </c:forEach>
                        </tbody>
                    </table>
                    * Reset will reset the statistic to 0 and move the log file to the archives directory
                     <div align="right" style="margin-top:10px;margin-bottom:10px;">
                        <button class="xc_button" type="button" onclick="javascript:YAHOO.xc.mst.log.resetAll();" name="next">Reset All</button>
                    </div>
                    <form name="harvestOutReset" method="post">
                        <input type="hidden" name="harvestOutLogFileName" id="harvestOutLogFileName">
                        <input type="hidden" name="serviceId" id="serviceId">
                    </form>
			</div>


 		</div>
		<!--  end body -->

        </div>
        <!-- end doc -->
    </body>
</html>
