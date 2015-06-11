<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
<spring:url var="datatablesUrl"
    value="/javaScript/dataTables/media/js/jquery.dataTables.latest.min.js" />
<spring:url var="datatablesBootstrapJsUrl"
    value="/javaScript/dataTables/media/js/jquery.dataTables.bootstrap.min.js"></spring:url>
<script type="text/javascript" src="${datatablesUrl}"></script>
<script type="text/javascript" src="${datatablesBootstrapJsUrl}"></script>
<spring:url var="datatablesCssUrl"
    value="/CSS/dataTables/dataTables.bootstrap.min.css" />
<link rel="stylesheet" href="${datatablesCssUrl}" />
<spring:url var="datatablesI18NUrl"
    value="/javaScript/dataTables/media/i18n/${portal.locale.language}.json" />

<link rel="stylesheet" type="text/css"
    href="${pageContext.request.contextPath}/CSS/dataTables/dataTables.bootstrap.min.css" />

<link
    href="${pageContext.request.contextPath}/static/treasury/css/dataTables.responsive.css"
    rel="stylesheet" />
<script
    src="${pageContext.request.contextPath}/static/treasury/js/dataTables.responsive.js"></script>
<link
    href="${pageContext.request.contextPath}/webjars/datatables-tools/2.2.4/css/dataTables.tableTools.css"
    rel="stylesheet" />
<script
    src="${pageContext.request.contextPath}/webjars/datatables-tools/2.2.4/js/dataTables.tableTools.js"></script>
<link
    href="${pageContext.request.contextPath}/webjars/select2/4.0.0-rc.2/dist/css/select2.min.css"
    rel="stylesheet" />
<script
    src="${pageContext.request.contextPath}/webjars/select2/4.0.0-rc.2/dist/js/select2.min.js"></script>
<script type="text/javascript"
    src="${pageContext.request.contextPath}/webjars/bootbox/4.4.0/bootbox.js"></script>
<script
    src="${pageContext.request.contextPath}/static/treasury/js/omnis.js"></script>

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
<%--${portal.angularToolkit()} --%>
${portal.toolkit()}

<%-- TITLE --%>
<div class="page-header">
    <h1>
        <spring:message
            code="label.administration.base.manageVat.updateVat" />
        <small></small>
    </h1>
</div>

<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
    <span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>&nbsp;<a
        class=""
        href="${pageContext.request.contextPath}/treasury/administration/base/managevat/vat/read/${vat.externalId}"><spring:message
            code="label.event.back" /></a> &nbsp;|&nbsp;
</div>
<c:if test="${not empty infoMessages}">
    <div class="alert alert-info" role="alert">

        <c:forEach items="${infoMessages}" var="message">
            <p>
                <span class="glyphicon glyphicon glyphicon-ok-sign"
                    aria-hidden="true">&nbsp;</span> ${message}
            </p>
        </c:forEach>
    </div>
</c:if>
<c:if test="${not empty warningMessages}">
    <div class="alert alert-warning" role="alert">

        <c:forEach items="${warningMessages}" var="message">
            <p>
                <span class="glyphicon glyphicon-exclamation-sign"
                    aria-hidden="true">&nbsp;</span> ${message}
            </p>
        </c:forEach>
    </div>
</c:if>
<c:if test="${not empty errorMessages}">
    <div class="alert alert-danger" role="alert">

        <c:forEach items="${errorMessages}" var="message">
            <p>
                <span class="glyphicon glyphicon-exclamation-sign"
                    aria-hidden="true">&nbsp;</span> ${message}
            </p>
        </c:forEach>
    </div>
</c:if>

<form method="post" class="form-horizontal">
    <div class="panel panel-default">
        <div class="panel-body">
            <div class="form-group row">
                <div class="col-sm-2 control-label">
                    <spring:message code="label.Vat.vatType" />
                </div>

                <div class="col-sm-4">
                    <div class="form-control">
                        <c:out value="${vat.vatType.name.content }" />
                    </div>
                </div>
            </div>
            <div class="form-group row">
                <div class="col-sm-2 control-label">
                    <spring:message
                        code="label.Vat.finantialInstitution" />
                </div>

                <div class="col-sm-10 ">
                    <div class="form-control">
                        <c:out value="${vat.finantialInstitution.name }" />
                    </div>
                </div>
            </div>
            <div class="form-group row">
                <div class="col-sm-2 control-label">
                    <spring:message code="label.Vat.taxRate" />
                </div>

                <div class="col-sm-4">
                    <input id="vat_taxRate" class="form-control"
                        type="text"
                        pattern="[0-9]+(\.[0-9][0-9]?[0-9]?)?"
                        name="taxrate"
                        value='<c:out value='${not empty param.taxrate ? param.taxrate : vat.taxRate }'/>'
                        required />
                </div>
            </div>
            <div class="form-group row">
                <div class="col-sm-2 control-label">
                    <spring:message code="label.Vat.beginDate" />
                </div>

                <div class="col-sm-4">
                    <input id="vat_beginDate" class="form-control"
                        type="text" name="begindate" bennu-date
                        value='<c:out value='${not empty param.begindate ? param.begindate : vat.beginDate.toString("yyyy-MM-dd") }'/>'
                        required />
                </div>
            </div>
            <div class="form-group row">
                <div class="col-sm-2 control-label">
                    <spring:message code="label.Vat.endDate" />
                </div>

                <div class="col-sm-4">
                    <input id="vat_endDate" class="form-control"
                        type="text" name="enddate" bennu-date
                        value='<c:out value='${not empty param.enddate ? param.enddate : vat.endDate.toString("yyyy-MM-dd") }'/>'
                        required />
                </div>
            </div>
        </div>
        <div class="panel-footer">
            <input type="submit" class="btn btn-default" role="button"
                value="<spring:message code="label.submit" />" />
        </div>
    </div>
</form>

<script>
$(document).ready(function() {
	<%-- Block for providing administrativeOffice options --%>
	<%-- CHANGE_ME --%> <%-- INSERT YOUR FORMAT FOR element --%>
// 	vatType_options = [
// 		<c:forEach items="${vatTypeList}" var="element"> 
// 			{
// 				text : "<c:out value='${element.name.content}'/>",  
// 				id : "<c:out value='${element.externalId}'/>"
// 			},
// 		</c:forEach>
// 	];
	
// 	$("#vat_vatType").select2(
// 		{
// 			data : vatType_options,
// 		}	  
//     );
	
// 	$("#vat_vatType").select2().select2('val','${param.vatType}');
	
// 	finantialInstitution_options = [
// 		<c:forEach items="${finantialInstitutionList}" var="element"> 
// 			{
// 				text : "<c:out value='${element.name}'/>",  
// 				id : "<c:out value='${element.externalId}'/>"
// 			},
// 		</c:forEach>
// 	];
	
// 	$("#vat_finantialInstitution").select2(
// 		{
// 			data : finantialInstitution_options,
// 		}	  
//     );
	
// 	$("#vat_finantialInstitution").select2().select2('val','${param.finantialInstitution}');
	
	vatExemptionReason_options = [
	                        		<c:forEach items="${vatExemptionReasonList}" var="element"> 
	                        			{
	                        				text : "<c:out value='${element.name.content}'/>",  
	                        				id : "<c:out value='${element.externalId}'/>"
	                        			},
	                        		</c:forEach>
	                        	];
	                        	
	                        	$("#vat_vatExemptionReason").select2(
	                        		{
	                        			data : vatExemptionReason_options,
	                        		}	  
	                            );
	                        	
	                        	$("#vat_vatExemptionReason").select2().select2('val','${param.vatExemptionReason}');
	
	});
</script>
