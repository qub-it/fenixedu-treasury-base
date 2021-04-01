package org.fenixedu.treasury.domain.forwardpayments.implementations;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentConfiguration;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qubit.solution.fenixedu.bennu.webservices.domain.keystore.DomainKeyStore;

@Deprecated
public class TPAInvocationUtil {

    private static final class PropInfo {
        private String t;
        private Integer s;

        public PropInfo(final String t, final Integer s) {
            this.t = t;
            this.s = s;
        }
    }

    private ForwardPayment forwardPayment;

    private static final Map<String, PropInfo> propsInfo = Maps.newHashMap();

    static {
        propsInfo.put("A001", new PropInfo("N", 9));
        propsInfo.put("A030", new PropInfo("C", 4));
        propsInfo.put("A031", new PropInfo("N", 3));
        propsInfo.put("A032", new PropInfo("N", 3));
        propsInfo.put("A037", new PropInfo("N", 14));
        propsInfo.put("A038", new PropInfo("N", 3));
        propsInfo.put("A050", new PropInfo("N", 2));
        propsInfo.put("A052", new PropInfo("N", 3));
        propsInfo.put("A053", new PropInfo("N", 10));
        propsInfo.put("A054", new PropInfo("C", 10));
        propsInfo.put("A061", new PropInfo("N", 8));
        propsInfo.put("A077", new PropInfo("C", 16));
        propsInfo.put("A078", new PropInfo("C", 16));
        propsInfo.put("A085", new PropInfo("C", 10));
        propsInfo.put("A089", new PropInfo("N", 10));
        propsInfo.put("A103", new PropInfo("N", 8));
        propsInfo.put("A105", new PropInfo("N", 4));
        propsInfo.put("A149", new PropInfo("C", 1));
        propsInfo.put("A3148", new PropInfo("N", 1));
        propsInfo.put("A7706", new PropInfo("C", 44));
        propsInfo.put("A7707", new PropInfo("N", 1));
        propsInfo.put("C003", new PropInfo("C", 16));
        propsInfo.put("C004", new PropInfo("N", 6));
        propsInfo.put("C005", new PropInfo("N", 3));
        propsInfo.put("C007", new PropInfo("N", 15));
        propsInfo.put("C012", new PropInfo("C", 128));
        propsInfo.put("C013", new PropInfo("C", 40));
        propsInfo.put("C016", new PropInfo("N", 2));
        propsInfo.put("C017", new PropInfo("N", 2));
        propsInfo.put("C025", new PropInfo("N", 7));
        propsInfo.put("C026", new PropInfo("C", 6));
        propsInfo.put("C042", new PropInfo("N", 1));
        propsInfo.put("C046", new PropInfo("C", 128));
        propsInfo.put("C108", new PropInfo("N", 3));
        propsInfo.put("XA086", new PropInfo("C", 42));
    }

    public TPAInvocationUtil(final ForwardPayment forwardPayment) {
        this.forwardPayment = forwardPayment;
    }

    public Map<String, String> mapAuthenticationRequest() {
        final TPAVirtualImplementation implementation =
                (TPAVirtualImplementation) forwardPayment.getForwardPaymentConfiguration().implementation();

        final LinkedHashMap<String, String> params = Maps.newLinkedHashMap();

        // MBNET Homologacao

        // MBNET Producao https://www.mbnet.pt

        params.put("A030", padding(TPAVirtualImplementation.AUTHENTICATION_REQUEST_MESSAGE, 4));
        params.put("A001", padding(forwardPayment.getForwardPaymentConfiguration().getVirtualTPAId(), 9));
        params.put("C007", padding(forwardPayment.getReferenceNumber(), 15));
        params.put("A105", padding(TPAVirtualImplementation.EURO_CODE, 4));
        params.put("A061", padding(forwardPayment.getDebtAccount().getFinantialInstitution().getCurrency()
                .getValueWithScale(forwardPayment.getAmount()).toString(), 8));
        params.put("C046", "");
        params.put("C012", implementation.getReturnURL(forwardPayment));

        final String c013 = hmacsha1(params);
        params.put("C013", c013);

        return params;
    }

    public Map<String, String> postAuthorizationRequest(final LinkedHashMap<String, String> requestMap) {
        final LinkedHashMap<String, String> params = Maps.newLinkedHashMap();

        params.put("A030", TPAVirtualImplementation.M001);
        params.put("A001", padding(forwardPayment.getForwardPaymentConfiguration().getVirtualTPAId(), 9));
        params.put("C007", padding(forwardPayment.getReferenceNumber(), 15));
        params.put("A061", padding(forwardPayment.getDebtAccount().getFinantialInstitution().getCurrency()
                .getValueWithScale(forwardPayment.getAmount()).toString(), 8));
        params.put("A105", padding(TPAVirtualImplementation.EURO_CODE, 4));

        final String c013 = hmacsha1(params);
        params.put("C013", c013);

        requestMap.putAll(params);

        return post(params, true);
    }

    public Map<String, String> postPaymentStatus(final LinkedHashMap<String, String> requestData) {
        final LinkedHashMap<String, String> params = Maps.newLinkedHashMap();

        params.put("A030", TPAVirtualImplementation.M020);
        params.put("A001", padding(forwardPayment.getForwardPaymentConfiguration().getVirtualTPAId(), 9));
        params.put("C007", padding(forwardPayment.getReferenceNumber(), 15));

        final String c013 = hmacsha1(params);
        params.put("C013", c013);

        requestData.putAll(params);

        return post(params, true);
    }

    public Map<String, String> postPayment(final DateTime authorizationDate, final LinkedHashMap<String, String> requestData) {
        final LinkedHashMap<String, String> params = Maps.newLinkedHashMap();

        params.put("A030", TPAVirtualImplementation.M002);
        params.put("A001", padding(forwardPayment.getForwardPaymentConfiguration().getVirtualTPAId(), 9));
        params.put("C007", padding(forwardPayment.getReferenceNumber(), 15));
        params.put("A061", padding(forwardPayment.getDebtAccount().getFinantialInstitution().getCurrency()
                .getValueWithScale(forwardPayment.getAmount()).toString(), 8));
        params.put("A105", padding(TPAVirtualImplementation.EURO_CODE, 4));
        params.put("A037", authorizationDate.toString("yyyyMMddHHmmss"));

        final String c013 = hmacsha1(params);
        params.put("C013", c013);

        requestData.putAll(params);

        return post(params, true);
    }

    private Map<String, String> post(final LinkedHashMap<String, String> params, final boolean isXml) {
        try {
            // https://teste.mbnet.pt/pvtn
            final URL url = new URL(forwardPayment.getForwardPaymentConfiguration().getVirtualTPAMOXXURL());
            
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            try {
                connection.setSSLSocketFactory(getFactory());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            
            connection.setUseCaches(false);

            connection.setDoInput(true);
            connection.setDoOutput(true);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);

            PrintWriter out = new PrintWriter(byteStream, true);

            out.print(httpsParams(params));
            out.flush();

            String lengthString = String.valueOf(byteStream.size());
            connection.setRequestProperty("Content-Length", lengthString);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Language", "pt-PT");

            byteStream.writeTo(connection.getOutputStream());

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String strResult = "";
            String strLine;
            while ((strLine = in.readLine()) != null) {
                strResult = strResult + strLine;
            }
            in.close();

            return convertStringToMap(strResult, isXml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String padding(String str, int length) {
        boolean paddingLeft = true;
        char paddingChar = '0';
        if (str == null) {
            return "";
        }

        if (str.length() > length) {
            throw new RuntimeException("length exceeded");
        }

        String strToPadding = "";
        for (int i = 0; i != length - str.length(); i++) {
            strToPadding = strToPadding + paddingChar;
        }

        if (paddingLeft) {
            return strToPadding + str;
        }

        return str + strToPadding;
    }

    private Map<String, String> convertStringToMap(final String strResult, boolean isXml) {
        final Map<String, String> result = Maps.newHashMap();
        if (isXml) {

            try {
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                final InputSource inputSource = new InputSource(new StringReader(strResult));
                final Document document = documentBuilder.parse(inputSource);

                NodeList nodeList = document.getChildNodes().item(0).getChildNodes();

                getXMLResponseNodeValues(result, nodeList);
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            String[] keyValues = strResult.split("&");
            for (final String kv : keyValues) {
                final String[] kvS = kv.split("=");
                if (kvS.length == 2) {
                    result.put(kvS[0].trim(), kvS[1].trim());
                }
            }
        }

        return result;
    }

    private static final List<String> INNER_REPONSE_NODES = Lists.newArrayList("XA086", "M120V01OC");

    private void getXMLResponseNodeValues(Map<String, String> result, NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (propsInfo.keySet().contains(node.getNodeName())) {
                result.put(node.getNodeName(), nodeList.item(i).getTextContent());
            } else if (INNER_REPONSE_NODES.contains(node.getNodeName())) {
                getXMLResponseNodeValues(result, node.getChildNodes());
            }
        }
    }

    private boolean isForms(String strResult) {
        return true;
    }

    private boolean isXml(String strResult) {
        return false;
    }

    private String httpsParams(final Map<String, String> params) {
        final String paramsStr = MapUtil.mapToString(params);

        return paramsStr;
    }

    private SSLSocketFactory getFactory() throws Exception {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        final String pKeyPassword = forwardPayment.getForwardPaymentConfiguration().getVirtualTPACertificatePassword();

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        InputStream keyInput = services.getFileStream(forwardPayment.getForwardPaymentConfiguration().getVirtualTPACertificate());
        keyStore.load(keyInput, pKeyPassword.toCharArray());
        keyInput.close();

        keyManagerFactory.init(keyStore, pKeyPassword.toCharArray());

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        return context.getSocketFactory();
    }

    public String hmacsha1(final LinkedHashMap<String, String> params) {
        String strMensagemASCII = "";
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            String strFieldResquestName = entry.getKey();
            String strFieldResquestValue = entry.getValue();

            if ("A061".equals(strFieldResquestName)) {
                strFieldResquestValue = strFieldResquestValue.replace("\\.", "");
            }

            String strFieldResquestType = propsInfo.get(strFieldResquestName).t;

            int intFieldResquestSize = propsInfo.get(strFieldResquestName).s;

            String strValueFieldResquestPadded = "";
            if ("N".equals(strFieldResquestType)) {
                strFieldResquestValue = strFieldResquestValue.replace(".", "");
                strValueFieldResquestPadded = padding(strFieldResquestValue, intFieldResquestSize);
            } else {
                strValueFieldResquestPadded = strFieldResquestValue.trim();
            }
            strMensagemASCII = strMensagemASCII + strValueFieldResquestPadded;
        }

        final HMac hmac = new HMac(new SHA1Digest());

        hmac.init(new KeyParameter(this.forwardPayment.getForwardPaymentConfiguration().getVirtualTPAMerchantId().getBytes()));
        hmac.update(strMensagemASCII.getBytes(), 0, strMensagemASCII.getBytes().length);
        byte[] resBuf = new byte[hmac.getMacSize()];
        hmac.doFinal(resBuf, 0);
        return new String(Hex.encode(resBuf));
    }

    private int[] sha1(int[] arrMessage) {
        if (arrMessage == null) {
            return null;
        }
        try {
            MessageDigest objMessageDigest = MessageDigest.getInstance("SHA1");
            byte[] arrBytes = new byte[arrMessage.length];
            for (int i = 0; i != arrMessage.length; i++) {
                arrBytes[i] = ((byte) arrMessage[i]);
            }
            objMessageDigest.update(arrBytes);

            byte[] arrBytesSHA = objMessageDigest.digest();

            int[] arrInts = new int[arrBytesSHA.length];
            for (int i = 0; i != arrBytesSHA.length; i++) {
                arrBytesSHA[i] &= 0xFF;
            }
            return arrInts;
        } catch (Exception e) {
            System.out.println("calcSHA1():: " + e.getMessage());
        }
        return null;
    }

}

class MapUtil {
    public static String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String key : map.keySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            String value = map.get(key);
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return stringBuilder.toString();
    }

    public static Map<String, String> stringToMap(String input) {
        Map<String, String> map = new HashMap<String, String>();

        String[] nameValuePairs = input.split("&");
        for (String nameValuePair : nameValuePairs) {
            String[] nameValue = nameValuePair.split("=");
            try {
                map.put(URLDecoder.decode(nameValue[0], "UTF-8"),
                        nameValue.length > 1 ? URLDecoder.decode(nameValue[1], "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return map;
    }
}
