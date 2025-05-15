package org.fenixedu.treasury.services.payments.sibspay;

import org.fenixedu.treasury.services.payments.sibspay.webhook.SibsPayWebhookLogic;
import org.junit.jupiter.api.Test;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.junit.jupiter.api.Assertions.*;

class SibsPayWebhookControllerTest {

    @Test
    void testCipherExists() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        assertDoesNotThrow(SibsPayWebhookLogic::getCipherInstance);
        assertNotNull(SibsPayWebhookLogic.getCipherInstance());
    }
}