package org.fenixedu.treasury.domain.forwardpayments.implementations;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.reflect.MethodUtils;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;

public interface IForwardPaymentController {

    public static final String CONTROLLER_URL = "/treasury/document/forwardpayments/forwardpayment";
    public static final String PROCESS_FORWARD_PAYMENT_URI = "/processforwardpayment";
    public static final String PROCESS_FORWARD_PAYMENT_URL = CONTROLLER_URL + PROCESS_FORWARD_PAYMENT_URI;

    public static Map<Class<? extends IForwardPaymentPlatformService>, Class<? extends IForwardPaymentController>> CONTROLLER_MAP =
            new HashMap<>();

    public static void registerForwardPaymentController(Class<? extends IForwardPaymentPlatformService> implementationClass,
            Class<? extends IForwardPaymentController> controllerClass) {
        CONTROLLER_MAP.put(implementationClass, controllerClass);
    }

    public static IForwardPaymentController getForwardPaymentController(final PaymentRequest request) {

        try {
            final Class<?> implementationClass = (Class<?>) request.getDigitalPaymentPlatform().getClass();
            Object result = MethodUtils.invokeStaticMethod(CONTROLLER_MAP.get(implementationClass), "getForwardPaymentController",
                    request);
            return (IForwardPaymentController) result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public String processforwardpayment(ForwardPaymentRequest forwardPayment, Object model, HttpServletResponse response,
            HttpSession session);
}
