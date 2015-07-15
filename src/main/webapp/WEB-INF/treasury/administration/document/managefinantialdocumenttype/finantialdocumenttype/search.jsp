<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<spring:url var="datatablesUrl" value="/javaScript/dataTables/media/js/jquery.dataTables.latest.min.js"/>
<spring:url var="datatablesBootstrapJsUrl" value="/javaScript/dataTables/media/js/jquery.dataTables.bootstrap.min.js"></spring:url>
<script type="text/javascript" src="${datatablesUrl}"></script>
<script type="text/javascript" src="${datatablesBootstrapJsUrl}"></script>
<spring:url var="datatablesCssUrl" value="/CSS/dataTables/dataTables.bootstrap.min.css"/>
<link rel="stylesheet" href="${datatablesCssUrl}"/>
<spring:url var="datatablesI18NUrl" value="/javaScript/dataTables/media/i18n/${portal.locale.language}.json"/>

<link rel="stylesheet" type="text/css"
	href="${pageContext.request.contextPath}/CSS/dataTables/dataTables.bootstrap.min.css"/>

<link href="${pageContext.request.contextPath}/static/treasury/css/dataTables.responsive.css" rel="stylesheet"/>
<script src="${pageContext.request.contextPath}/static/treasury/js/dataTables.responsive.js"></script>
<link href="${pageContext.request.contextPath}/webjars/datatables-tools/2.2.4/css/dataTables.tableTools.css" rel="stylesheet"/>
<script src="${pageContext.request.contextPath}/webjars/datatables-tools/2.2.4/js/dataTables.tableTools.js"></script>
<link href="${pageContext.request.contextPath}/webjars/select2/4.0.0-rc.2/dist/css/select2.min.css" rel="stylesheet" />
<script src="${pageContext.request.contextPath}/webjars/select2/4.0.0-rc.2/dist/js/select2.min.js"></script>						
<script type="text/javascript" src="${pageContext.request.contextPath}/webjars/bootbox/4.4.0/bootbox.js" ></script>
<script src="${pageContext.request.contextPath}/static/treasury/js/omnis.js"></script>

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
<%--${portal.angularToolkit()} --%>
${portal.toolkit()}

<%-- TITLE --%>
<div class="page-header">
	<h1><spring:message code="label.administration.document.manageFinantialDocumentType.searchFinantialDocumentType" />
		<small></small>
	</h1>
</div>
<%-- NAVIGATION --%>
<div class="well well-sm" style="display:inline-block">
	<span class="glyphicon glyphicon-plus-sign" aria-hidden="true"></span>&nbsp;<a class="" href="${pageContext.request.contextPath}/treasury/administration/document/managefinantialdocumenttype/finantialdocumenttype/create"   ><spring:message code="label.event.create" /></a>
&nbsp;
</div>
<c:if test="${not empty infoMessages}">
	<div class="alert alert-info" role="alert">

		<c:forEach items="${infoMessages}" var="message">
			<p> <span class="glyphicon glyphicon glyphicon-ok-sign" aria-hidden="true">&nbsp;</span>
  				${message}
  			</p>
		</c:forEach>
	</div>
</c:if>
<c:if test="${not empty warningMessages}">
	<div class="alert alert-warning" role="alert">

		<c:forEach items="${warningMessages}" var="message">
			<p> <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true">&nbsp;</span>
  				${message}
  			</p>
		</c:forEach>
	</div>
</c:if>
<c:if test="${not empty errorMessages}">
	<div class="alert alert-danger" role="alert">

		<c:forEach items="${errorMessages}" var="message">
			<p> <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true">&nbsp;</span>
  				${message}
  			</p>
		</c:forEach>
	</div>
</c:if>

<%-- 
<div class="panel panel-default">
<form method="get" class="form-horizontal">
<div class="panel-body">
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.FinantialDocumentType.type"/></div> 

<div class="col-sm-4">
	<select id="finantialDocumentType_type" class="form-control" name="type">
		<option value="">&nbsp;</option> 
		<c:forEach items="${typeValues}" var="field">
			<option value='<c:out value='${field}'/>'><c:out value='${field}'/></option>
		</c:forEach>
	</select>
	<script>
		$("#finantialDocumentType_type").val('<c:out value='${not empty param.type ? param.type : finantialDocumentType.type }'/>');
	</script>	
</div>
</div>		
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.FinantialDocumentType.code"/></div> 

<div class="col-sm-10">
	<input id="finantialDocumentType_code" class="form-control" type="text" name="code"  value='<c:out value='${not empty param.code ? param.code : finantialDocumentType.code }'/>' />
</div>	
</div>		
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.FinantialDocumentType.name"/></div> 

<div class="col-sm-10">
<input id="finantialDocumentType_name" class="form-control" type="text" name="name"  bennu-localized-string value='${not empty param.name ? param.name : "{}" } '/> 
</div>
</div>		
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.FinantialDocumentType.documentNumberSeriesPrefix"/></div> 

<div class="col-sm-10">
	<input id="finantialDocumentType_documentNumberSeriesPrefix" class="form-control" type="text" name="documentnumberseriesprefix"  value='<c:out value='${not empty param.documentnumberseriesprefix ? param.documentnumberseriesprefix : finantialDocumentType.documentNumberSeriesPrefix }'/>' />
</div>	
</div>		
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.FinantialDocumentType.invoice"/></div> 

<div class="col-sm-2">
<select id="finantialDocumentType_invoice" name="invoice" class="form-control">
<option value="false"><spring:message code="label.no"/></option>
<option value="true"><spring:message code="label.yes"/></option>				
</select>
	<script>
		$("#finantialDocumentType_invoice").val('<c:out value='${not empty param.invoice ? param.invoice : finantialDocumentType.invoice }'/>');
	</script>	
</div>
</div>		
</div>
<div class="panel-footer">
	<input type="submit" class="btn btn-default" role="button" value="<spring:message code="label.search" />"/>
</div>
</form>
</div>

 --%>
<c:choose>
	<c:when test="${not empty searchfinantialdocumenttypeResultsDataSet}">
		<table id="searchfinantialdocumenttypeTable" class="table responsive table-bordered table-hover" width="100%">
			<thead>
				<tr>
					<%--!!!  Field names here --%>
<th><spring:message code="label.FinantialDocumentType.type"/></th>
<th><spring:message code="label.FinantialDocumentType.code"/></th>
<th><spring:message code="label.FinantialDocumentType.name"/></th>
<th><spring:message code="label.FinantialDocumentType.documentNumberSeriesPrefix"/></th>
<%-- <th><spring:message code="label.FinantialDocumentType.invoice"/></th> --%>
<%-- Operations Column --%>
					<th></th>
				</tr>
			</thead>
			<tbody>
				
			</tbody>
		</table>
	</c:when>
	<c:otherwise>
				<div class="alert alert-warning" role="alert">
					
					<p> <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true">&nbsp;</span>			<spring:message code="label.noResultsFound" /></p>
					
				</div>	
		
	</c:otherwise>
</c:choose>

<script>
	var searchfinantialdocumenttypeDataSet = [
			<c:forEach items="${searchfinantialdocumenttypeResultsDataSet}" var="searchResult">
				<%-- Field access / formatting  here CHANGE_ME --%>
				{
				"DT_RowId" : '<c:out value='${searchResult.externalId}'/>',
"type" : "<c:out value='${searchResult.type.descriptionI18N.content}'/>",
"code" : "<c:out value='${searchResult.code}'/>",
"name" : "<c:out value='${searchResult.name.content}'/>",
"documentnumberseriesprefix" : "<c:out value='${searchResult.documentNumberSeriesPrefix}'/>",
// "invoice" : "<c:if test="${searchResult.invoice}"><spring:message code="label.true" /></c:if><c:if test="${not searchResult.invoice}"><spring:message code="label.false" /></c:if>",
"actions" :
" <a  class=\"btn btn-default btn-xs\" href=\"${pageContext.request.contextPath}/treasury/administration/document/managefinantialdocumenttype/finantialdocumenttype/search/view/${searchResult.externalId}\"><spring:message code='label.view'/></a>" +
                "" },
            </c:forEach>
    ];
	
	$(document).ready(function() {

	


		var table = $('#searchfinantialdocumenttypeTable').DataTable({language : {
			url : "${datatablesI18NUrl}",			
		},
		"columns": [
			{ data: 'type' },
			{ data: 'code' },
			{ data: 'name' },
			{ data: 'documentnumberseriesprefix' },
// 			{ data: 'invoice' },
			{ data: 'actions',className:"all" }
			
		],
		//CHANGE_ME adjust the actions column width if needed
		"columnDefs": [
		//54
		               { "width": "54px", "targets": 4 } 
		             ],
		"data" : searchfinantialdocumenttypeDataSet,
		//Documentation: https://datatables.net/reference/option/dom
//"dom": '<"col-sm-6"l><"col-sm-3"f><"col-sm-3"T>rtip', //FilterBox = YES && ExportOptions = YES
//"dom": 'T<"clear">lrtip', //FilterBox = NO && ExportOptions = YES
"dom": '<"col-sm-6"l><"col-sm-6"f>rtip', //FilterBox = YES && ExportOptions = NO
//"dom": '<"col-sm-6"l>rtip', // FilterBox = NO && ExportOptions = NO
        "tableTools": {
            "sSwfPath": "${pageContext.request.contextPath}/static/treasury/swf/copy_csv_xls_pdf.swf"
        }
		});
		table.columns.adjust().draw();
		
		  $('#searchfinantialdocumenttypeTable tbody').on( 'click', 'tr', function () {
		        $(this).toggleClass('selected');
		    } );
		  
	}); 
</script>

