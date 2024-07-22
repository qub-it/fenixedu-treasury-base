package org.fenixedu.treasury.services.payments.sibspay;

import org.junit.jupiter.api.Test;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.junit.jupiter.api.Assertions.*;

class SibsPayWebhookControllerTest {

    @Test
    void testCipherExists() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        assertDoesNotThrow(SibsPayWebhookController::getCipherInstance);
        assertNotNull(SibsPayWebhookController.getCipherInstance());
    }
}