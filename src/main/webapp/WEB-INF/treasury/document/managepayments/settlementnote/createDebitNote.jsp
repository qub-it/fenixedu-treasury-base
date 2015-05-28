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
<%-- ${portal.toolkit()} --%>
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
       		<div class="well well-sm">
			<p><strong><spring:message code="label.DebtAccount.finantialInstitution" />: </strong>${settlementNoteBean.debtAccount.finantialInstitution.name}</p>
			<p><strong><spring:message code="label.DebtAccount.customer" />: </strong>${settlementNoteBean.debtAccount.customer.code} - ${settlementNoteBean.debtAccount.customer.name}</p>
			<p><strong><spring:message code="label.Customer.fiscalNumber" />: </strong>${ settlementNoteBean.debtAccount.customer.fiscalNumber }</p>
		</div>

    </div>
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

<div>
    <p>
        1. <spring:message code="label.administration.manageCustomer.createSettlementNote.chooseInvoiceEntries" />
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        2. <spring:message code="label.administration.manageCustomer.createSettlementNote.calculateInterest" />
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        <b>3. <spring:message code="label.administration.manageCustomer.createSettlementNote.createDebitNote" /></b>
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        4. <spring:message code="label.administration.manageCustomer.createSettlementNote.insertpayment" />
        <span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span>
        5. <spring:message code="label.administration.manageCustomer.createSettlementNote.summary" />
    </p>
</div>

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

<script type="text/javascript">
    function processSubmit(url) {
        console.log(url);
        $("#createDebitNoteForm").attr("action", url);
        $("#createDebitNoteForm").submit();
    }
</script>

<form id='createDebitNoteForm' name='form' method="post" class="form-horizontal"
    ng-app="angularAppSettlementNote"
    ng-controller="SettlementNoteController"
    action='${pageContext.request.contextPath}<%= SettlementNoteController.CREATE_DEBIT_NOTE_URL %>'>

    <input name="bean" type="hidden" value="{{ object }}" />

    <div class="panel panel-primary">    
        <div class="panel-heading">
            <h3 class="panel-title">
                <spring:message code="label.DebitNote" />
            </h3>
            <p>
                <spring:message code="label.DebitNote.create" />
            </p>
        </div>
        <div class="panel-body">
            <table class="table">
                <tbody>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.DebitEntry.dueDate" /></th>
                        <td><c:out
                                value='${settlementNoteBean.debitNoteDate.toString("yyyy-MM-dd")}' />
                        </td>
                    </tr>
                </tbody>
            </table>
            <table id="debitNoteTable"
                class="table responsive table-bordered table-hover">
                <thead>
                    <tr>
                        <th><spring:message code="label.DebitEntry.description" /></th>
                        <th><spring:message code="label.DebitEntry.vat" /></th>
                        <th><spring:message code="label.DebitEntry.amountWithVat" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${ settlementNoteBean.debitEntries }" var="debitEntryBean">
                        <c:if test="${ debitEntryBean.included && empty debitEntryBean.debitEntry.finantialDocument  }">
                            <tr>
                                <td>
                                    <c:out value="${ debitEntryBean.debitEntry.description }" />
                                </td>
                                <td>
                                    <c:out value="${ settlementNoteBean.debtAccount.finantialInstitution.currency.getValueWithScale(  debitEntryBean.debitEntry.vat.taxRate ) }"/>
                                </td>
                                <td>
                                    <c:out value="${ settlementNoteBean.debtAccount.finantialInstitution.currency.getValueFor( debitEntryBean.paymentAmountWithVat ) }" />
                                </td>
                            </tr>
                        </c:if>                    
                    </c:forEach>
                    <c:forEach items="${ settlementNoteBean.interestEntries }" var="interestEntryBean">
                        <c:if test="${ interestEntryBean.included  }">
                            <tr>
                                <td>
                                    <c:out value="${ interestEntryBean.debitEntry.description }" />
                                </td>
                                <td>
                                    0.00
                                </td>
                                <td>
                                    <c:out value="${ settlementNoteBean.debtAccount.finantialInstitution.currency.getValueFor( interestEntryBean.interest.interestAmount ) }" />
                                </td>
                            </tr>
                        </c:if>                    
                    </c:forEach>                 
                </tbody>
            </table>
        </div>
        <div class="panel-footer">
            <button type="button" class="btn btn-default" onClick="javascript:processSubmit('${pageContext.request.contextPath}<%= SettlementNoteController.CHOOSE_INVOICE_ENTRIES_URL %>')"><spring:message code="label.event.back" /></button>
            <button type="button" class="btn btn-primary" onClick="javascript:processSubmit('${pageContext.request.contextPath}<%= SettlementNoteController.CREATE_DEBIT_NOTE_URL %>')"><spring:message code="label.continue" /></button>
        </div>
    </div>    
</form>

<script>
	$(document).ready(function() {
		// Put here the initializing code for page
	});
</script>
