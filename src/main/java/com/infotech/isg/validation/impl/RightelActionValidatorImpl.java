package com.infotech.isg.validation.impl;

import com.infotech.isg.validation.ErrorCodes;
import com.infotech.isg.validation.ActionValidator;
import com.infotech.isg.domain.ServiceActions;

import org.springframework.stereotype.Component;

/**
 * validator for Rightel service actions
 *
 * @author Sevak Gharibian
 */
@Component("RightelActionValidator")
public class RightelActionValidatorImpl implements ActionValidator {

    @Override
    public int validate(String action) {

        if (!ServiceActions.isActionExist(action)) {
            return ErrorCodes.INVALID_OPERATOR_ACTION;
        }

        if ((ServiceActions.getActionCode(action) != ServiceActions.TOP_UP)
            && (ServiceActions.getActionCode(action) != ServiceActions.WOW)) {
            return ErrorCodes.INVALID_OPERATOR_ACTION;
        }

        return ErrorCodes.OK;
    }
}
