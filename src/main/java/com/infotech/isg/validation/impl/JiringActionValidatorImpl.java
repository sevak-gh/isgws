package com.infotech.isg.validation.impl;

import com.infotech.isg.validation.ErrorCodes;
import com.infotech.isg.validation.ActionValidator;
import com.infotech.isg.domain.ServiceActions;

import org.springframework.stereotype.Component;

/**
 * validator for Jiring service actions
 *
 * @author Sevak Gharibian
 */
@Component("JiringActionValidator")
public class JiringActionValidatorImpl implements ActionValidator {

    @Override
    public int validate(String action) {
        if (!ServiceActions.isActionExist(action)) {
            return ErrorCodes.INVALID_OPERATOR_ACTION;
        }

        if (!((ServiceActions.getActionCode(action) == ServiceActions.TOP_UP)
              || (ServiceActions.getActionCode(action) == ServiceActions.PAY_BILL)
              || (ServiceActions.getActionCode(action) == ServiceActions.WALLET))) {
            return ErrorCodes.INVALID_OPERATOR_ACTION;
        }

        return ErrorCodes.OK;
    }
}
