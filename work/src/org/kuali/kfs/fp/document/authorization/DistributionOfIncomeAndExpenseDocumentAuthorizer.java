/*
 * Copyright 2008 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.fp.document.authorization;

import java.util.List;
import java.util.Map;

import org.kuali.kfs.fp.document.DistributionOfIncomeAndExpenseDocument;
import org.kuali.kfs.sys.KfsAuthorizationConstants;
import org.kuali.kfs.sys.businessobject.ElectronicPaymentClaim;
import org.kuali.kfs.sys.document.authorization.AccountingDocumentAuthorizerBase;
import org.kuali.rice.kns.bo.user.UniversalUser;
import org.kuali.rice.kns.document.Document;

/**
 * This class...
 */
public class DistributionOfIncomeAndExpenseDocumentAuthorizer extends AccountingDocumentAuthorizerBase {

    /**
     * @see org.kuali.rice.kns.authorization.DocumentAuthorizer#getEditMode(org.kuali.rice.kns.document.Document,
     *      org.kuali.rice.kns.bo.user.KualiUser)
     */
    @Override
    public Map getEditMode(Document document, UniversalUser user, List sourceLines, List targetLines) {
        DistributionOfIncomeAndExpenseDocument diDoc = (DistributionOfIncomeAndExpenseDocument) document;
        
        Map editModeMap = super.getEditMode(document, user, sourceLines, targetLines);
        
        List<ElectronicPaymentClaim> epcs = diDoc.getElectronicPaymentClaims();

        if(epcs==null) {
            diDoc.refreshReferenceObject("electronicPaymentClaims");
            epcs = diDoc.getElectronicPaymentClaims();
        }
        
        if(epcs!=null && epcs.size()>0) {
            editModeMap.put(KfsAuthorizationConstants.DistributionOfIncomeAndExpenseEditMode.SOURCE_LINE_READ_ONLY_MODE, "TRUE");
        }
        
        return editModeMap;
    }
}
