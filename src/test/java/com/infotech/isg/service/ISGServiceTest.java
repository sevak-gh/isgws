package com.infotech.isg.service;

import java.util.Arrays;

import com.infotech.isg.domain.Client;
import com.infotech.isg.service.ErrorCodes;
import com.infotech.isg.service.ISGService;
import com.infotech.isg.service.ISGServiceImpl;
import com.infotech.isg.service.ISGServiceResponse;
import com.infotech.isg.service.RequestValidator;
import com.infotech.isg.repository.TransactionRepository;
import com.infotech.isg.repository.OperatorRepository;
import com.infotech.isg.repository.PaymentChannelRepository;
import com.infotech.isg.proxy.mci.MCIProxy;
import com.infotech.isg.proxy.mci.MCIProxyGetTokenResponse;
import com.infotech.isg.proxy.mci.MCIProxyRechargeResponse;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyObject;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
* test cases for ISG service.
*
* @author Sevak Gharibian
*/
public class ISGServiceTest {

    private ISGService isgService;

    @Mock
    private MCIProxy mciProxy;

    @Mock
    private AccessControl accessControl;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PaymentChannelRepository paymentChannelRepository;

    @Mock
    private RequestValidator mciValidator;

    @Mock
    private RequestValidator mtnValidator;

    @Mock
    private RequestValidator jiringValidator;


    @BeforeMethod(alwaysRun = true)
    public void setup() {
        MockitoAnnotations.initMocks(this);
        isgService = new ISGServiceImpl(accessControl, operatorRepository, paymentChannelRepository,
                                        transactionRepository, mciProxy, mciValidator, mtnValidator, jiringValidator);
    }

    @Test
    public void shouldMCIReturnOK() {
        //arrange
        // bypass all validators
        when(mciValidator.validateRequiredParams(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validateAmount(anyInt())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validateAction(anyString())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validateCellNumber(anyString())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validateBankCode(anyString())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validateOperator(anyObject())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validatePaymentChannel(anyObject())).thenReturn(ErrorCodes.OK);
        when(mciValidator.validateTransaction(anyObject(), anyString(),
                                              anyInt(), anyInt(), anyInt(),
                                              anyString(), anyString())).thenReturn(ErrorCodes.OK);
        // bypass authentication
        when(accessControl.authenticate(anyString(), anyString(), anyString())).thenReturn(ErrorCodes.OK);
        when(accessControl.getClient()).thenReturn(new Client() {{setId(1);}});
        // bypass proxy
        when(mciProxy.getToken()).thenReturn(new MCIProxyGetTokenResponse() {{setToken("");}});
        when(mciProxy.recharge(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(new MCIProxyRechargeResponse() {{setResponse(Arrays.asList("0", "OK"));}});

        // act
        ISGServiceResponse response = isgService.mci("", "", "", 1, 1, "", "", "", "", "", "");
        int result = (int)response.getISGDoc();

        // assert
        assertThat(result, is(ErrorCodes.OK));
    }
}
