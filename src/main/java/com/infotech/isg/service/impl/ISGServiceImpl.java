package com.infotech.isg.service.impl;

import com.infotech.isg.domain.Transaction;
import com.infotech.isg.domain.ServiceActions;
import com.infotech.isg.domain.OperatorStatus;
import com.infotech.isg.validation.RequestValidator;
import com.infotech.isg.validation.TransactionValidator;
import com.infotech.isg.validation.ErrorCodes;
import com.infotech.isg.repository.TransactionRepository;
import com.infotech.isg.repository.OperatorStatusRepository;
import com.infotech.isg.service.OperatorService;
import com.infotech.isg.service.OperatorServiceResponse;
import com.infotech.isg.service.OperatorNotAvailableException;
import com.infotech.isg.service.OperatorUnknownResponseException;
import com.infotech.isg.service.AccessControl;
import com.infotech.isg.service.ISGService;
import com.infotech.isg.service.ISGServiceResponse;
import com.infotech.isg.service.ISGException;

import java.util.List;
import java.util.Date;

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
    protected OperatorStatusRepository operatorStatusRepository;
    protected OperatorService operatorService;
    protected RequestValidator requestValidator;
    protected TransactionValidator transactionValidator;
    protected int operatorId;

    @Override
    public ISGServiceResponse topup(String username, String password,
                                    String bankCode, int amount,
                                    String channel, String state,
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
            switch (errorCode) {
                case ErrorCodes.STF_RESOLVED_SUCCESSFUL:
                    // STF has resolved this transaction as successful
                    List<Transaction> transactions = transactionRepository.findByRefNumBankCodeClientId(bankReceipt, bankCode, accessControl.getClient().getId());
                    long transactionId = 0;
                    String operatorTId = null;
                    if ((transactions != null) && (transactions.size() > 0)) {
                        transactionId = transactions.get(0).getId();
                        operatorTId = transactions.get(0).getOperatorTId();
                    }
                    return new ISGServiceResponse("OK", transactionId, operatorTId);

                default:
                    return new ISGServiceResponse("ERROR", errorCode, null);
            }
        }

        // register ongoing transaction
        Transaction transaction = new Transaction();
        transaction.setProvider(operatorId);
        transaction.setAction(ServiceActions.getActionCode(action));
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

        OperatorServiceResponse operatorServiceResponse = null;
        try {
            operatorServiceResponse = operatorService.topup(consumer, amount, transaction.getId(), action);
        } catch (OperatorNotAvailableException e) {
            LOG.error("operator service not available, OPERATOR_SERVICE_ERROR returned", e);
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_ERROR, null);
        } catch (OperatorUnknownResponseException e) {
            // ambiguous status, set for STF
            transaction.setStf(1);
            transaction.setStfResult(0);
            transaction.setOperatorResponseCode(-1);
            transactionRepository.update(transaction);
            LOG.error("error in calling service provider, STF set and operator_service_error_donot_reverse code returned", e);
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_ERROR_DONOT_REVERSE, null);
        }

        if (operatorServiceResponse == null) {
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_ERROR, null);
        }

        if (!operatorServiceResponse.getCode().equalsIgnoreCase("0")) {
            // operation not successful
            transaction.setStatus(-1);
            transaction.setOperatorDateTime(new Date());
            transaction.setOperatorResponseCode(Integer.parseInt(operatorServiceResponse.getCode()));
            transaction.setOperatorResponse(operatorServiceResponse.getMessage());
            transaction.setToken(operatorServiceResponse.getToken());
            transaction.setOperatorTId(operatorServiceResponse.getTransactionId());
            transaction.setOperatorCommand(operatorServiceResponse.getStatus());
            transactionRepository.update(transaction);
            return new ISGServiceResponse("ERROR", ErrorCodes.OPERATOR_SERVICE_RESPONSE_NOK, operatorServiceResponse.getCode());
        }

        // operation successful, OK
        transaction.setStatus(1);
        transaction.setOperatorDateTime(new Date());
        transaction.setOperatorResponseCode(Integer.parseInt(operatorServiceResponse.getCode()));
        transaction.setOperatorResponse(operatorServiceResponse.getMessage());
        transaction.setToken(operatorServiceResponse.getToken());
        transaction.setOperatorTId(operatorServiceResponse.getTransactionId());
        transaction.setOperatorCommand(operatorServiceResponse.getStatus());
        transactionRepository.update(transaction);
        return new ISGServiceResponse("OK", transaction.getId(), operatorServiceResponse.getTransactionId());
    }

    @Override
    public ISGServiceResponse isOperatorAvailable() {
        OperatorStatus operatorStatus = operatorStatusRepository.findById(operatorId);

        if (operatorStatus == null) {
            throw new ISGException("operator status not available");
        }

        return new ISGServiceResponse("OK", (operatorStatus.getIsAvailable()) ? 1L : 0L, null);
    }

    @Override
    public int getOperatorId() {
        return operatorId;
    }
}
