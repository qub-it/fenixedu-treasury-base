<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
<%@ taglib prefix="datatables"
	uri="http://github.com/dandelion/datatables"%>

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
			code="label.administration.base.manageProduct.readProduct" />
		<small></small>
	</h1>
</div>
<div class="modal fade" id="deleteModal">
	<div class="modal-dialog">
		<div class="modal-content">
			<form id="deleteForm"
				action="${pageContext.request.contextPath}/treasury/administration/base/manageproduct/product/delete/${product.externalId}"
				method="POST">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title">
						<spring:message code="label.confirmation" />
					</h4>
				</div>
				<div class="modal-body">
					<p>
						<spring:message
							code="label.administration.base.manageProduct.readProduct.confirmDelete" />
					</p>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-default" data-dismiss="modal">
						<spring:message code="label.close" />
					</button>
					<button id="deleteButton" class="btn btn-danger" type="submit">
						<spring:message code="label.delete" />
					</button>
				</div>
			</form>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->
<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
	<span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>&nbsp;<a
		class=""
		href="${pageContext.request.contextPath}/treasury/administration/base/manageproduct/product/"><spring:message
			code="label.event.back" /></a> |&nbsp;&nbsp; <span
		class="glyphicon glyphicon-trash" aria-hidden="true"></span>&nbsp;<a
		class="" href="#" data-toggle="modal" data-target="#deleteModal"><spring:message
			code="label.event.delete" /></a> |&nbsp;&nbsp; <span
		class="glyphicon glyphicon-pencil" aria-hidden="true"></span>&nbsp;<a
		class=""
		href="${pageContext.request.contextPath}/treasury/administration/base/manageproduct/product/update/${product.externalId}"><spring:message
			code="label.event.update" /></a> |&nbsp;&nbsp;
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

<div class="panel panel-primary">
	<div class="panel-heading">
		<h3 class="panel-title">
			<spring:message code="label.details" />
		</h3>
	</div>
	<div class="panel-body">
		<form method="post" class="form-horizontal">
			<table class="table">
				<tbody>
					<tr>
						<th scope="row" class="col-xs-3"><spring:message
								code="label.Product.code" /></th>
						<td><c:out value='${product.code}' /></td>
					</tr>
					<tr>
						<th scope="row" class="col-xs-3"><spring:message
								code="label.Product.name" /></th>
						<td><c:out value='${product.name.content}' /></td>
					</tr>
					<tr>
						<th scope="row" class="col-xs-3"><spring:message
								code="label.Product.unitOfMeasure" /></th>
						<td><c:out value='${product.unitOfMeasure.content}' /></td>
					</tr>
					<tr>
						<th scope="row" class="col-xs-3"><spring:message
								code="label.Product.active" /></th>
						<td><c:if test="${product.active}">
								<spring:message code="label.true" />
							</c:if>
							<c:if test="${not product.active}">
								<spring:message code="label.false" />
							</c:if></td>
					</tr>
				</tbody>
			</table>
		</form>
	</div>
</div>

<h2>
	<spring:message code="label.Product.Tariffs" />
</h2>

<c:choose>
	<c:when test="${not empty product.finantialInstitutionsSet}">
		<c:forEach items="${ product.finantialInstitutionsSet}"
			var="finantialInstitution">
<h3><c:out value="${ finantialInstitution.name}" /></h3>

<div class="well well-sm" style="display: inline-block">
		<span
		class="glyphicon glyphicon-plus-sign" aria-hidden="true"></span>&nbsp;<a
		class=""
		href="${pageContext.request.contextPath}/treasury/administration/base/managefixedtariff/fixedtariff/create?finantialInstitution=${finantialInstitution.externalId}&product=${product.externalId}"><spring:message
			code="label.event.create" /></a> 
</div>

			<c:choose>
				<c:when
					test="${not empty product.activeTariffsSet}">
					<datatables:table id="activeTariffs" row="tariff"
						data="${product.activeTariffsSet(finantialInstitution)}"
						cssClass="table responsive table-bordered table-hover" cdn="false"
						cellspacing="2">
						<datatables:column>
							<datatables:columnHead>
								<spring:message code="label.Tariff.amount" />
							</datatables:columnHead>
							<c:out value="${tariff.amount}" />
						</datatables:column>
						<datatables:column>
							<c:out value="${tariff}" />
							<span class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
								class=""
								href="${pageContext.request.contextPath}/treasury/document/manageinvoice/debitnote/read/${debitNote.externalId}/updateentry/"
								${debitEntry.externalId}><spring:message
									code="label.event.document.manageInvoice.updateEntry" /></a>	|&nbsp;&nbsp;
	<span class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
								class=""
								href="${pageContext.request.contextPath}/treasury/document/manageinvoice/debitnote/read/${debitNote.externalId}/deleteentry/"
								${debitEntry.externalId}><spring:message
									code="label.event.document.manageInvoice.deleteEntry" /></a>
						</datatables:column>
					</datatables:table>
					<script>
						createDataTables('activeTariffs', false, false, true,
								"${pageContext.request.contextPath}",
								"${datatablesI18NUrl}");
					</script>
				</c:when>
				<c:otherwise>
					<div class="alert alert-warning" role="alert">
						<p>
							<span class="glyphicon glyphicon-exclamation-sign"
								aria-hidden="true">&nbsp;</span>
							<spring:message code="label.noResultsFound" />
						</p>
					</div>

				</c:otherwise>
			</c:choose>
		</c:forEach>
	</c:when>
	<c:otherwise>
		<div class="alert alert-warning" role="alert">
			<p>
				<span class="glyphicon glyphicon-exclamation-sign"
					aria-hidden="true">&nbsp;</span>
				<spring:message code="label.noResultsFound" />
			</p>
		</div>

	</c:otherwise>
</c:choose>


<script>
	$(document).ready(function() {

	});
</script>
