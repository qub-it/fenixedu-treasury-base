/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.services.payments.meowallet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.FenixFramework;

@Path("/forwardPayment")
public class ForwadPaymentController {

    private static final String SUCCESS = "/returnpayment";
    private static final long SLEEP_TIME_TO_PROCESS_WEBHOOK = 3000; // 3 seconds

    @GET
    @Path(SUCCESS)
    public void success(@Context HttpServletRequest httpRequest, @Context HttpServletResponse response) throws IOException {

        // Delay this request in order for webhook to process the notification
        try {
            Thread.sleep(SLEEP_TIME_TO_PROCESS_WEBHOOK);
        } catch (InterruptedException e) {
        }

        ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();

        String forwardPaymentId = httpRequest.getParameter("forwardPaymentId");
        Class screenClass = null;
        try {
            String screenName = httpRequest.getParameter("screenName");
            screenClass = Class.forName(screenName);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        String url = "";
        try {
            ForwardPaymentRequest forwardPayment = FenixFramework.getDomainObject(forwardPaymentId);
            PostProcessPaymentStatusBean result = forwardPayment.getDigitalPaymentPlatform().castToForwardPaymentPlatformService()
                    .processForwardPayment(forwardPayment);

            if (result != null && result.getForwardPaymentStatusBean().isInPayedState()) {
                url = implementation.getForwardPaymentURL(httpRequest.getContextPath(), screenClass, true, forwardPaymentId,
                        false);

            } else {
                url = implementation.getForwardPaymentURL(httpRequest.getContextPath(), screenClass, false, forwardPaymentId,
                        false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            url = implementation.getForwardPaymentURL(httpRequest.getContextPath(), screenClass, false, forwardPaymentId, true);
        } finally {
            response.sendRedirect(url);
        }
    }
}
