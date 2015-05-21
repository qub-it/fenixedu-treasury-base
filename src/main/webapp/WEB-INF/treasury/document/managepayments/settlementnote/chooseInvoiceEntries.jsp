<%@page import="org.fenixedu.treasury.ui.accounting.managecustomer.DebtAccountController"%>
<%@page import="org.fenixedu.treasury.ui.document.managepayments.SettlementNoteController"%>
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

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
<!--  ${portal.toolkit()} -->
${portal.angularToolkit()}


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

<script
    src="${pageContext.request.contextPath}/webjars/angular-sanitize/1.3.11/angular-sanitize.js"></script>
<link rel="stylesheet" type="text/css"
    href="${pageContext.request.contextPath}/webjars/angular-ui-select/0.11.2/select.min.css" />
<script
    src="${pageContext.request.contextPath}/webjars/angular-ui-select/0.11.2/select.min.js"></script>


<%-- TITLE --%>
<div class="page-header">
    <h1>
        <spring:message
            code="label.administration.manageCustomer.createSettlementNote.chooseInvoiceEntries" />
        <small></small>
    </h1>
    <div>
        <p>
            ${ settlementNoteBean.debtAccount.customer.name } (NIF ${ settlementNoteBean.debtAccount.customer.fiscalNumber })
        </p>
    </div>
</div>

<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
    <span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>&nbsp;<a
        class="" href="${pageContext.request.contextPath}/<%= DebtAccountController.READ_URL %>${settlementNoteBean.debtAccount.externalId}"><spring:message
            code="label.event.back" /></a> |&nbsp;&nbsp;
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

<script>
   angular.isUndefinedOrNull = function(val) {
	    return angular.isUndefined(val) || val === null };
   angular
        .module('angularAppSettlementNote', [ 'ngSanitize', 'ui.select' ])
		.controller(
				  'SettlementNoteController',
				  [
				    '$scope',
					function($scope) {
					   $scope.object = angular.fromJson('${settlementNoteBeanJson}');
					} 
   				  ]
	     );
</script>


<div>
    <p>
        <b>1. <spring:message code="label.administration.manageCustomer.createSettlementNote.chooseInvoiceEntries" /></b>
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        2. <spring:message code="label.administration.manageCustomer.createSettlementNote.calculateInterest" />
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        3. <spring:message code="label.administration.manageCustomer.createSettlementNote.createDebitNote" />
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        4. <spring:message code="label.administration.manageCustomer.createSettlementNote.insertpayment" />
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        5. <spring:message code="label.administration.manageCustomer.createSettlementNote.summary" />
    </p>
</div>

<form name='form' method="post" class="form-horizontal"
    ng-app="angularAppSettlementNote"
    ng-controller="SettlementNoteController"
    action='${pageContext.request.contextPath}<%= SettlementNoteController.CHOOSE_INVOICE_ENTRIES_URL %>'>

    <input name="bean" type="hidden" value="{{ object }}" />

    <div class="panel panel-primary">    
        <div class="panel-heading">
            <h3 class="panel-title">
                <spring:message code="label.DebitEntry" />
            </h3>
            <p>
                <spring:message code="label.DebitEntry.choose" />
            </p>
        </div>
        <div class="panel-body">
            <table id="debitEntriesTable"
                class="table responsive table-bordered table-hover">
                <thead>
                    <tr>
                        <%-- Check Column --%>
                        <th></th>
                        <th><spring:message code="label.DebitEntry.documentNumber" /></th>
                        <th><spring:message code="label.DebitEntry.description" /></th>
                        <th><spring:message code="label.DebitEntry.date" /></th>
                        <th><spring:message code="label.DebitEntry.totalAmount" /></th>
                        <th><spring:message code="label.DebitEntry.openAmount" /></th>
                        <th><spring:message code="label.DebitEntry.vat" /></th>
                        <th><spring:message code="label.DebitEntry.paymentAmount" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${ settlementNoteBean.debitEntries}" var="debitEntryBean" varStatus="loop">
                        <c:if test="${ debitEntryBean.notValid }">
                            <tr class="alert alert-danger">
                        </c:if>
                        <c:if test="${ not debitEntryBean.notValid }">
                            <tr>
                        </c:if>
                        
                            <td>
                                <span class="glyphicon glyphicon-remove-circle" ng-show="object.debitEntries[${ loop.index }].isNotValid"></span>
                                <input class="form-control" ng-model="object.debitEntries[${ loop.index }].isIncluded" type="checkbox" />
                            </td>
                            <td>
                                <c:out value="${ debitEntryBean.debitEntry.finantialDocument.documentNumber }"/>
                            </td>
                            <td>
                                <c:out value="${ debitEntryBean.debitEntry.description }"/>
                            </td>
                            <td>
                                <c:out value="${ debitEntryBean.documentDate.toString('yyyy-MM-dd')}"/>
                            </td>
                            <td>
                                <c:out value="${ settlementNoteBean.debtAccount.finantialInstitution.currency.symbol }" />
                                <c:out value="${ debitEntryBean.debitEntry.amountWithVat }"/>
                            </td>
                            <td>
                                <c:out value="${ settlementNoteBean.debtAccount.finantialInstitution.currency.symbol }" />
                                <c:out value="${ debitEntryBean.debitEntry.openAmountWithVat }"/>
                            </td>
                            <td>
                                <c:out value="${ debitEntryBean.debitEntry.vat.taxRate }"/>
                            </td>
                            <td>
                                <input class="form-control" name="paymentAmount${ loop.index }" ng-model="object.debitEntries[${ loop.index }].paymentAmount" type="text" ng-disabled="!object.debitEntries[${ loop.index }].isIncluded" ng-pattern="/^[0-9]+(\.[0-9]{1,2})?$/" value='0.00'/>
                                <p class="alert alert-danger" ng-show="form.paymentAmount${ loop.index }.$error.pattern && object.debitEntries[${ loop.index }].isIncluded"><spring:message code="error.invalid.format.input" /></p>                    
                            </td>
                            
                        </tr>
                    </c:forEach> 
                </tbody>
            </table>
        </div>
    </div>
    
    <div class="panel panel-primary">    
        <div class="panel-heading">
            <h3 class="panel-title">
                <spring:message code="label.CreditEntry" />
            </h3>
            <p>
                <spring:message code="label.CreditEntry.choose" />
            </p>
        </div>
        <div class="panel-body">            
            <table id="creditEntriesTable"
                class="table responsive table-bordered table-hover">
                <thead>
                    <tr>
                        <%-- Check Column --%>
                        <th></th>
                        <th><spring:message code="label.CreditEntry.documentNumber" /></th>
                        <th><spring:message code="label.CreditEntry.motive" /></th>
                        <th><spring:message code="label.CreditEntry.date" /></th>
                        <th><spring:message code="label.DebitEntry.vat" /></th>
                        <th><spring:message code="label.CreditEntry.totalAmount" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${ settlementNoteBean.creditEntries}" var="creditEntryBean" varStatus="loop">
                        <tr>
                            <td>
                                <input class="form-control" ng-model="object.creditEntries[${ loop.index }].isIncluded" type="checkbox" />
                            </td>
                            <td>
                                <c:out value="${ creditEntryBean.creditEntry.finantialDocument.documentNumber }"/>
                            </td>
                            <td>
                                <c:out value="${ creditEntryBean.creditEntry.description }"/>
                            </td>
                            <td>
                                <c:out value="${ creditEntryBean.documentDate.toString('yyyy-MM-dd')}"/>
                            </td>
                            <td>
                                <c:out value="${ creditEntryBean.creditEntry.vat.taxRate }"/>
                            </td>
                            <td>
                                <c:out value="${ settlementNoteBean.debtAccount.finantialInstitution.currency.symbol }" />
                                <c:out value="${ creditEntryBean.creditEntry.openAmountWithVat }"/>
                            </td>
                        </tr>
                    </c:forEach> 
                </tbody>
            </table>
        </div>
    </div>
    <div class="form-group row">
        <div class="col-sm-2 control-label">
            <spring:message code="label.date"/>
        </div> 
        <div class="col-sm-4">
            <input  class="form-control" type="text" ng-model="object.date" />
        </div>
    </div>
    <div class="panel-footer">
        <input type="submit" class="btn btn-default" role="button" value="<spring:message code="label.continue" />"/>
    </div>
</form>

<script>
	$(document).ready(function() {

		// Put here the initializing code for page
	});
</script>