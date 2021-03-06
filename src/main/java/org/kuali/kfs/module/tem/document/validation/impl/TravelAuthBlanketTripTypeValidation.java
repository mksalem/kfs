/*
 * The Kuali Financial System, a comprehensive financial management system for higher education.
 * 
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kuali.kfs.module.tem.document.validation.impl;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.tem.TemKeyConstants;
import org.kuali.kfs.module.tem.TemPropertyConstants;
import org.kuali.kfs.module.tem.document.TravelAuthorizationDocument;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.document.validation.GenericValidation;
import org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.ObjectUtils;

public class TravelAuthBlanketTripTypeValidation extends GenericValidation {

    //@Override
    @Override
    public boolean validate(AttributedDocumentEvent event) {
        boolean rulePassed = true;
        TravelAuthorizationDocument taDocument = (TravelAuthorizationDocument)event.getDocument();
        if (!ObjectUtils.isNull(taDocument.getTripType())) {
            if (taDocument.isBlanketTravel()) {
             // If the user selects Blanket Trip Type, airfare amount and the Trip Detail Estimate should not be completed. (Note:
                // Blanket Travel implies in-state travel)
                if (!ObjectUtils.isNull(taDocument.getPerDiemExpenses()) && !taDocument.getPerDiemExpenses().isEmpty()) {
                    GlobalVariables.getMessageMap().putError(TemPropertyConstants.PER_DIEM_EXPENSES, TemKeyConstants.ERROR_TA_BLANKET_TYPE_NO_ESTIMATE);
                    taDocument.logErrors();
                    rulePassed = false;
                }
                if (!ObjectUtils.isNull(taDocument.getActualExpenses()) && !taDocument.getActualExpenses().isEmpty()) {
                    GlobalVariables.getMessageMap().putError(TemPropertyConstants.NEW_ACTUAL_EXPENSE_LINE, TemKeyConstants.ERROR_TA_BLANKET_TYPE_NO_EXPENSES);
                    taDocument.logErrors();
                    rulePassed = false;
                }
                if (!taDocument.getTripType().isBlanketTravel() ) {
                    rulePassed = false;
                    GlobalVariables.getMessageMap().putError(KFSConstants.DOCUMENT_PROPERTY_NAME + "." +TemPropertyConstants.TRIP_TYPE_CODE, TemKeyConstants.ERROR_TA_TRIP_TYPE_BLANKET_NOT_ALLOWED);
                }
            }
            if ((taDocument.isBlanketTravel() || isNonEncumbranceTrip(taDocument)) && (StringUtils.isBlank(taDocument.getTemProfile().getDefaultChartCode()) || StringUtils.isBlank(taDocument.getTemProfile().getDefaultAccount()))) {
                rulePassed = false;
                final String basePropertyName = (taDocument.isBlanketTravel()) ? TemPropertyConstants.BLANKET_IND : TemPropertyConstants.TRIP_TYPE_CODE;
                GlobalVariables.getMessageMap().putError(basePropertyName, TemKeyConstants.ERROR_TA_PROFILE_NOT_COMPLETE_FOR_BLANKET_TRAVEL);
            }
        }

        return rulePassed;
    }

    /**
     * Determines if the trip type does not need to generate encumbrances and has no accounting line
     * @param travelAuth the travel authorization to verify
     * @return true if the authorization is not planning to generate encumbrances, false otherwise
     */
    protected boolean isNonEncumbranceTrip(TravelAuthorizationDocument travelAuth) {
        return (!travelAuth.getTripType().isGenerateEncumbrance() || travelAuth.hasOnlyPrepaidExpenses()) && (travelAuth.getSourceAccountingLines() == null || travelAuth.getSourceAccountingLines().isEmpty());
    }
}
