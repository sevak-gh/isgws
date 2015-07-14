package com.infotech.isg.ws;

import com.infotech.isg.service.ISGService;
import com.infotech.isg.service.ISGServiceResponse;
import com.infotech.isg.validation.ErrorCodes;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.jws.soap.SOAPBinding.ParameterStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * publishes ISG service endpoint through XML Web service.
 *
 * @author Sevak Gharibian
 */
@WebService(name = "ISGWS", targetNamespace = "urn:TopUpWSDL")
@SOAPBinding(style = Style.DOCUMENT, use = Use.LITERAL)
@Service("ISGWS")
public class ISGWS {

    private static final Logger LOG = LoggerFactory.getLogger(ISGWS.class);

    @Resource
    private WebServiceContext context;

    private final ISGService mtnService;
    private final ISGService mciService;
    private final ISGService jiringService;
    private final ISGService rightelService;

    /**
    * gets client remote IP through web service context
    */
    private String getClientIp() {
        MessageContext mc = context.getMessageContext();
        HttpServletRequest request = (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
        return request.getRemoteAddr();
    }

    @Autowired
    public ISGWS(@Qualifier("MTNService") ISGService mtnService,
                 @Qualifier("MCIService") ISGService mciService,
                 @Qualifier("JiringService") ISGService jiringService,
                 @Qualifier("RightelService") ISGService rightelService) {
        this.mtnService = mtnService;
        this.mciService = mciService;
        this.jiringService = jiringService;
        this.rightelService = rightelService;
    }

    /**
     * MCI topup service
     *
     */
    @WebMethod(operationName = "MCI", action = "urn:TopUpWSDL/MCI")
    @WebResult(name = "MCIResponse")
    public ISGServiceResponse mci(@WebParam(name = "username") String username,
                                  @WebParam(name = "password") String password,
                                  @WebParam(name = "bankcode") String bankCode,
                                  @WebParam(name = "amount") int amount,
                                  @WebParam(name = "channel") String channel,
                                  @WebParam(name = "state") String state,
                                  @WebParam(name = "bankreceipt") String bankReceipt,
                                  @WebParam(name = "orderid") String orderId,
                                  @WebParam(name = "consumer") String consumer,
                                  @WebParam(name = "customerip") String customerIp) {

        ISGServiceResponse response = mciService.topup(username, password, bankCode, amount, channel,
                                      state, bankReceipt, orderId, consumer, customerIp,
                                      getClientIp(), "top-up", "noname");

        return response;
    }

    /**
     * MTN topup service
     *
     */
    @WebMethod(operationName = "MTN", action = "urn:TopUpWSDL/MTN")
    @WebResult(name = "MTNResponse")
    public ISGServiceResponse mtn(@WebParam(name = "username") String username,
                                  @WebParam(name = "password") String password,
                                  @WebParam(name = "action") String action,
                                  @WebParam(name = "bankcode") String bankCode,
                                  @WebParam(name = "amount") int amount,
                                  @WebParam(name = "channel") String channel,
                                  @WebParam(name = "state") String state,
                                  @WebParam(name = "bankreceipt") String bankReceipt,
                                  @WebParam(name = "orderid") String orderId,
                                  @WebParam(name = "consumer") String consumer,
                                  @WebParam(name = "customerip") String customerIp,
                                  @WebParam(name = "customerName") String customerName) {

        ISGServiceResponse response = mtnService.topup(username, password, bankCode, amount, channel,
                                      state, bankReceipt, orderId, consumer, customerIp,
                                      getClientIp(), action, customerName);

        return response;
    }

    /**
     * represents Jiring service
     *
     */
    @WebMethod(operationName = "Jiring", action = "urn:TopUpWSDL/Jiring")
    @WebResult(name = "JiringResponse")
    public ISGServiceResponse jiring(@WebParam(name = "username") String username,
                                     @WebParam(name = "password") String password,
                                     @WebParam(name = "bankcode") String bankCode,
                                     @WebParam(name = "amount") int amount,
                                     @WebParam(name = "channel") String channel,
                                     @WebParam(name = "state") String state,
                                     @WebParam(name = "bankreceipt") String bankReceipt,
                                     @WebParam(name = "orderid") String orderId,
                                     @WebParam(name = "consumer") String consumer,
                                     @WebParam(name = "customerip") String customerIp,
                                     @WebParam(name = "action") String action) {

        ISGServiceResponse response = jiringService.topup(username, password, bankCode, amount, channel,
                                      state, bankReceipt, orderId, consumer, customerIp,
                                      getClientIp(),
                                      ((action == null) || action.isEmpty()) ? "top-up" : action, "noname");    // top-up default action

        return response;
    }

    /**
     * Rightel topup service
     *
     */
    @WebMethod(operationName = "Rightel", action = "urn:TopUpWSDL/Rightel")
    @WebResult(name = "RightelResponse")
    public ISGServiceResponse rightel(@WebParam(name = "username") String username,
                                      @WebParam(name = "password") String password,
                                      @WebParam(name = "action") String action,
                                      @WebParam(name = "bankcode") String bankCode,
                                      @WebParam(name = "amount") int amount,
                                      @WebParam(name = "channel") String channel,
                                      @WebParam(name = "state") String state,
                                      @WebParam(name = "bankreceipt") String bankReceipt,
                                      @WebParam(name = "orderid") String orderId,
                                      @WebParam(name = "consumer") String consumer,
                                      @WebParam(name = "customerip") String customerIp) {

        ISGServiceResponse response = rightelService.topup(username, password, bankCode, amount, channel,
                                      state, bankReceipt, orderId, consumer, customerIp,
                                      getClientIp(), action, "noname");

        return response;
    }


    /**
     * MCI get bill amount
     *
     */
    @WebMethod(operationName = "getMCIBill", action = "urn:TopUpWSDL/getMCIBill")
    @WebResult(name = "getMCIBillResponse")
    public ISGServiceResponse getMCIBill(@WebParam(name = "consumer") String consumer) {

        ISGServiceResponse response = jiringService.getBill(consumer);

        return response;
    }

    /**
     * returns true/false for MCI service availability
     *
     */
    @WebMethod(operationName = "isMCIAvailable", action = "urn:TopUpWSDL/isMCIAvailable")
    @WebResult(name = "isMCIAvailableResponse")
    public ISGServiceResponse isMCIAvailable() {
        ISGServiceResponse response =  mciService.isOperatorAvailable();
        return response;
    }

    /**
     * returns true/false for MTN service availability
     *
     */
    @WebMethod(operationName = "isMTNAvailable", action = "urn:TopUpWSDL/isMTNAvailable")
    @WebResult(name = "isMTNAvailableResponse")
    public ISGServiceResponse isMTNAvailable() {
        ISGServiceResponse response = mtnService.isOperatorAvailable();
        return response;
    }

    /**
     * returns true/false for Jiring service availability
     *
     */
    @WebMethod(operationName = "isJiringAvailable", action = "urn:TopUpWSDL/isJiringAvailable")
    @WebResult(name = "isJiringAvailableResponse")
    public ISGServiceResponse isJiringAvailable() {
        ISGServiceResponse response = jiringService.isOperatorAvailable();
        return response;
    }

    /**
     * verifies MCI transaction
     *
     */
    @WebMethod(operationName = "verifyMCI", action = "urn:TopUpWSDL/verifyMCI")
    @WebResult(name = "verifyMCIResponse")
    public ISGServiceResponse verifyMCI(@WebParam(name = "consumer") String consumer,
                                        @WebParam(name = "transactionId") String transactionId) {
        return mciService.verifyTransaction(consumer, transactionId);
    }

    /**
     * verifies MTN transaction
     *
     */
    @WebMethod(operationName = "verifyMTN", action = "urn:TopUpWSDL/verifyMTN")
    @WebResult(name = "verifyMTNResponse")
    public ISGServiceResponse verifyMTN(@WebParam(name = "consumer") String consumer,
                                        @WebParam(name = "transactionId") String transactionId) {
        return mtnService.verifyTransaction(consumer, transactionId);
    }

    /**
     * verifies Jiring transaction
     *
     */
    @WebMethod(operationName = "verifyJiring", action = "urn:TopUpWSDL/verifyJiring")
    @WebResult(name = "verifyJiringResponse")
    public ISGServiceResponse verifyJiring(@WebParam(name = "consumer") String consumer,
                                           @WebParam(name = "transactionId") String transactionId) {
        return jiringService.verifyTransaction(consumer, transactionId);
    }
}
