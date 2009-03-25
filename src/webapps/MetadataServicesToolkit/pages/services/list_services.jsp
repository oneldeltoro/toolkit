<!--
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the  
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/. 
  *
  -->

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:import url="/inc/doctype-frag.jsp"/>

<LINK href="page-resources/css/header.css" rel="stylesheet" type="text/css">


<html>
    <head>
        <title>List Services</title>
        <c:import url="/inc/meta-frag.jsp"/>

        <LINK href="page-resources/yui/reset-fonts-grids/reset-fonts-grids.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/base-mst.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/yui/menu/assets/skins/sam/menu.css"  rel="stylesheet" type="text/css" >

        <LINK href="page-resources/css/global.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/main_menu.css" rel="stylesheet" type="text/css" >
        <LINK href="page-resources/css/tables.css" rel="stylesheet" type="text/css" >
		<LINK href="page-resources/css/header.css" rel="stylesheet" type="text/css">
		<LINK href="page-resources/css/bodylayout.css" rel="stylesheet" type="text/css">
        <LINK HREF="page-resources/css/bodylayout.css" REL="stylesheet" TYPE="text/css">

        <SCRIPT LANGUAGE="JavaScript" src="page-resources/yui/yahoo-dom-event/yahoo-dom-event.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" src="page-resources/yui/connection/connection-min.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" src="page-resources/yui/container/container_core-min.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" SRC="page-resources/yui/menu/menu-min.js"></SCRIPT>
        <SCRIPT LANGUAGE="JavaScript" SRC="page-resources/js/main_menu.js"></SCRIPT>
     
    </head>


 <body class="yui-skin-sam">

        <!--  yahoo doc 2 template creates a page 950 pixles wide -->
        <div id="doc2">

            <!-- page header - this uses the yahoo page styling -->
            <div id="hd">

                <!--  this is the header of the page -->
                <c:import url="/inc/header.jsp"/>

                <!--  this is the header of the page -->
                <c:import url="/inc/menu.jsp"/>
                <jsp:include page="/inc/breadcrumb.jsp">

                    <jsp:param name="bread" value="Services , List Services" />

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

                    <div class="viewTable">
                        <table width="100%">
                            <thead>
                                <tr>
                                    <td class="sortcolumn"><a href="listServices.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=service_name">Service Name</a></td>
                                    <td><a href="listServices.action?isAscendingOrder=${!isAscendingOrder}&columnSorted=port">Associated Repository URL</a></td>
                                    <td>Status</td>
                                    <td>View Log</td>
                                    <td>Delete</td>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="n" items="${services}" varStatus="a">
                                    <tr>
                                        <td><c:out value="${n.name}"/></td>
                                        <c:set var="baseURL" value="${baseURL}"/>
                                        <c:set var = "url" value="${fn:replace(baseURL,'8080',n.port)}" />
                                        <td><c:out value="${url}"/></td>
                                        <td>Status should be here</td>
                                        <td>
                                            <button class="xc_button" type="button" name="Service">Service</button> &nbsp;&nbsp;&nbsp;
                                            <button class="xc_button" type="button" name="HarvestOut">Harvest Out</button>
                                        </td>
                                        <td><button class="xc_button" type="button" name="delete">Delete</button></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>

            </div>
        </div>
</body>
</html>
