package com.infotech.isg.service;

import com.infotech.isg.domain.Operator;
import com.infotech.isg.domain.PaymentChannel;
import com.infotech.isg.domain.Transaction;
import com.infotech.isg.validation.IRequestValidator;
import com.infotech.isg.validation.TransactionValidator;
import com.infotech.isg.validation.ErrorCodes;
import com.infotech.isg.repository.TransactionRepository;
import com.infotech.isg.proxy.ServiceProvider;
import com.infotech.isg.proxy.ServiceProviderResponse;
import com.infotech.isg.service.ISGService;
import com.infotech.isg.service.ISGServiceResponse;
import com.infotech.isg.service.ISGException;

import java.util.List;
import java.util.Date;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* abstract implementation for ISG service
*
* @author Sevak Gharibian
*/
public abstract class ISGServiceImpl implements ISGService {

    private static final Logger LOG = LoggerFactory.getLogger(MCIServiceImpl.class);

    protected AccessControl accessControl;
    protected TransactionRepository transactionRepository;
    protected ServiceProvider serviceProvider;
    protected IRequestValidator requestValidator;
    protected TransactionValidator transactionValidator;
    protected int operatorId;
    protected int actionCode;

    @Override
    public ISGServiceResponse topup(String username, String password,
                                    String bankCode, int amount,
                                    int channel, String state,
                                    String bankReceipt, String orderId,
                                    String consumer, String customerIp,
                                    String remoteIp, String action) {

        int errorCode = ErrorCodes.OK;
        errorCode = requestValidator.validate(username, password, bankCode,
                                              amount, channel, state,
                                              bankReceipt, orderId, consumer,
                                              customerIp, remoteIp, action, operatorId);
        if (errorCode != ErrorCodes.OK) {
            return new ISGServiceResponse("ERROR", errorCode, null);
        }

        // authenticate client
        errorCode = accessControl.authenticate(username, password, remoteIp);
        if (errorCode != ErrorCodes.OK) {
            return new ISGServiceResponse("ERROR", errorCode, null);
        }

        // validate if transaction is duplicate
        errorCode = transactionValidator.validate(bankReceipt, bankCode, accessControl.getClient().getId(),
                    orderId, operatorId, amount, channel, consumer, customerIp);
        if (errorCode != ErrorCodes.OK) {
            // TODO: may need more review
            List<Transaction> transactions = transactionRepository.findByRefNumBankCodeClientId(bankReceipt, bankCode, accessControl.getClient().getId());
            switch (errorCode) {
                case ErrorCodes.STF_RESOLVED_SUCCESSFUL:
                    // STF has resolved this transaction as successful
                    long transactionId = 0;
                    String operatorResponse = null;
                    if ((transactions != null) && (transactions.size() > 0)) {
                        transactionId = transactions.get(0).getId();
                        operatorResponse = transactions.get(0).getOperatorResponse();
                    }
                    return new ISGServiceResponse("OK", transactionId, operatorResponse);

                case ErrorCodes.STF_RESOLVED_FAILED:
                    // STF has resolved this transaction as failed
                    return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_RESPONSE_NOK, null);

                case ErrorCodes.STF_ERROR:
                    // invalid STF status, set for STF to try again
                    if ((transactions != null) && (transactions.size() > 0)) {
                        transactions.get(0).setStf(1);
                        transactions.get(0).setStfResult(0);
                        transactions.get(0).setOperatorResponseCode(2);
                        transactionRepository.update(transactions.get(0));
                    }
                    return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_ERROR_DONOT_REVERSE, null);

                default:
                    return new ISGServiceResponse("ERROR", errorCode, null);
            }
        }

        // register ongoing transaction
        Transaction transaction = new Transaction();
        transaction.setProvider(operatorId);
        transaction.setAction(actionCode);
        transaction.setState(state);
        transaction.setResNum(orderId);
        transaction.setRefNum(bankReceipt);
        transaction.setRemoteIp(remoteIp);
        transaction.setTrDateTime(new Date());
        transaction.setAmount(amount);
        transaction.setChannel(channel);
        transaction.setConsumer(consumer);
        transaction.setBankCode(bankCode);
        transaction.setClientId(accessControl.getClient().getId());
        transaction.setCustomerIp(customerIp);
        transaction.setStatus(-1);
        transaction.setBankVerify(amount);
        transaction.setVerifyDateTime(new Date());
        transactionRepository.create(transaction);

        ServiceProviderResponse serviceProviderResponse = null;
        try {
            serviceProviderResponse = serviceProvider.topup(consumer, amount, transaction.getId());
        } catch (ISGException e) {
            // ambiguous status, set for STF
            transaction.setStf(1);
            transaction.setStfResult(0);
            transaction.setOperatorResponseCode(2);
            transactionRepository.update(transaction);
            LOG.error("error in calling service provider, STF set and operator_service_error_donot_reverse code returned", e);
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_ERROR_DONOT_REVERSE, null);
        }

        if (serviceProviderResponse == null) {
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_ERROR, null);
        }

        if (!serviceProviderResponse.getCode().equalsIgnoreCase("0")) {
            // operation not successful
            transaction.setStatus(-1);
            transaction.setOperatorDateTime(new Date());
            transaction.setOperatorResponseCode(Integer.parseInt(serviceProviderResponse.getCode()));
            transaction.setOperatorResponse(serviceProviderResponse.getMessage());
            transaction.setToken(serviceProviderResponse.getTransactionId());
            transaction.setOperatorTId(serviceProviderResponse.getTransactionId());
            transaction.setOperatorCommand(serviceProviderResponse.getStatus());
            transactionRepository.update(transaction);
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_RESPONSE_NOK, null);
        }

        // operation successful, OK
        transaction.setStatus(1);
        transaction.setOperatorDateTime(new Date());
        transaction.setOperatorResponseCode(Integer.parseInt(serviceProviderResponse.getCode()));
        transaction.setOperatorResponse(serviceProviderResponse.getMessage());
        transaction.setToken(serviceProviderResponse.getTransactionId());
        transaction.setOperatorTId(serviceProviderResponse.getTransactionId());
        transaction.setOperatorCommand(serviceProviderResponse.getStatus());
        transactionRepository.update(transaction);
        return new ISGServiceResponse("OK", transaction.getId(), serviceProviderResponse.getMessage());
    }
}