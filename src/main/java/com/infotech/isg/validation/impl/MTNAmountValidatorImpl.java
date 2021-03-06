package com.infotech.isg.validation.impl;

import com.infotech.isg.validation.AmountValidator;
import com.infotech.isg.validation.ErrorCodes;

import org.springframework.stereotype.Component;

/**
 * MTN validator for amount
 *
 * @author Sevak Gharibian
 */
@Component("MTNAmountValidator")
public class MTNAmountValidatorImpl implements AmountValidator {

    @Override
    public int validate(int amount, int action) {
        return (amount >= 1000) ? ErrorCodes.OK : ErrorCodes.INVALID_AMOUNT;
    }
}
