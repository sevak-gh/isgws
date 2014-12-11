package com.infotech.isg.proxy.mci;

import com.infotech.isg.util.HashGenerator;
import com.infotech.isg.util.SOAPHelper;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPException;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* implementation for MCI proxy.
*
* @author Sevak Gharibian
*/
@Component("MCIProxy")
public class MCIProxyImpl implements MCIProxy {

    private static final Logger LOG = LoggerFactory.getLogger(MCIProxyImpl.class);

    @Value("${mci.url}")
    private String url;

    @Value("${mci.username}")
    private String username;

    @Value("${mci.password}")
    private String password;

    @Value("${mci.namespace}")
    private String namespace;

    private static final String SOAPACTION_GETTOKEN = "GetToken";
    private static final String SOAPACTION_RECHARGE = "Recharge";

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public MCIProxyGetTokenResponse getToken() {

        // create empty soap request
        SOAPMessage request = SOAPHelper.createSOAPRequest(namespace, namespace + SOAPACTION_GETTOKEN);

        // send message and get response
        SOAPMessage response = SOAPHelper.callSOAP(request, url);

        // process response
        MCIProxyGetTokenResponse getTokenResponse = SOAPHelper.parseResponse(response, namespace, "GetTokenResponse", MCIProxyGetTokenResponse.class);

        return getTokenResponse;
    }

    @Override
    public MCIProxyRechargeResponse recharge(String token, String consumer,
            int amount, long trId) {

        // create empty soap request
        SOAPMessage request = SOAPHelper.createSOAPRequest(namespace, namespace + SOAPACTION_RECHARGE);

        // add request body/header
        try {
            SOAPHeader header = request.getSOAPHeader();
            SOAPHeaderElement headerElement = header.addHeaderElement(new QName(namespace, "AuthHeader", "ns"));
            SOAPElement usernameElement = headerElement.addChildElement(new QName(namespace, "UserName", "ns"));
            usernameElement.setValue(username);
            SOAPElement passwordElement = headerElement.addChildElement(new QName(namespace, "Password", "ns"));
            String combination = username.toUpperCase() + "|" + password + "|" + token;
            passwordElement.setValue(HashGenerator.getMD5(combination));
            SOAPBody body = request.getSOAPBody();
            SOAPBodyElement bodyElement = body.addBodyElement(new QName(namespace, "Recharge", "ns"));
            SOAPElement element = bodyElement.addChildElement(new QName(namespace, "BrokerID", "ns"));
            element.addTextNode(username);
            element = bodyElement.addChildElement(new QName(namespace, "MobileNumber", "ns"));
            element.addTextNode(consumer);
            element = bodyElement.addChildElement(new QName(namespace, "CardAmount", "ns"));
            element.addTextNode(Integer.toString(amount));
            element = bodyElement.addChildElement(new QName(namespace, "TransactionID", "ns"));
            element.addTextNode("MCI" + Long.toString(trId));
            request.saveChanges();
        } catch (SOAPException e) {
            throw new RuntimeException("soap extended request creation error", e);
        }

        // send message and get response
        SOAPMessage response = SOAPHelper.callSOAP(request, url);

        // process response
        MCIProxyRechargeResponse rechargeResponse = SOAPHelper.parseResponse(response, namespace, "RechargeResponse", MCIProxyRechargeResponse.class);

        return rechargeResponse;
    }
}
