package org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.io.FileUtils;

public class SOAPLoggingHandler implements SOAPHandler<SOAPMessageContext> {
    
    private String outboundMessage;
    private String inboundMessage;
    
    @Override
    public void close(MessageContext messageContext) {

    }

    @Override
    public boolean handleFault(SOAPMessageContext messageContext) {
        return false;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext messageContext) {
        boolean direction = ((Boolean) messageContext.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();
        if (direction) {
            outboundMessage = dumpMsg(messageContext);
            // FileUtils.writeByteArrayToFile(new File("/home/anilmamede/Downloads/a/c.txt"), outboundMessage.getBytes("UTF-8"));
        } else {
            inboundMessage = dumpMsg(messageContext);
        }

        return true;
    }
    
    
    
    public String dumpMsg(final SOAPMessageContext context) {
        try {
           SOAPMessage soapmsg = context.getMessage();
           return getMsgAsString(soapmsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
     }    

    public String getMsgAsString(final SOAPMessage message) {
        String msg = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            msg = baos.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return msg;
    }

    @Override
    public Set getHeaders() {
        return null;
    }
    
    public String getOutboundMessage() {
        return outboundMessage;
    }
    
    public String getInboundMessage() {
        return inboundMessage;
    }
    
    public static SOAPLoggingHandler createLoggingHandler(final BindingProvider client) {
        final SOAPLoggingHandler loggingHandler = new SOAPLoggingHandler();
        List<Handler> handlerChain = ((BindingProvider) client).getBinding().getHandlerChain();
        if(handlerChain == null) {
            handlerChain = new ArrayList<Handler>();
        }
        
        handlerChain.add(loggingHandler);
        
        ((BindingProvider) client).getBinding().setHandlerChain(handlerChain);

        return loggingHandler;
    }
    
}
