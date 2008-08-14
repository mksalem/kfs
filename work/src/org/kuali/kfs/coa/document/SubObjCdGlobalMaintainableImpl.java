/*
 * Copyright 2007 The Kuali Foundation.
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
package org.kuali.kfs.coa.document;

import java.util.ArrayList;
import java.util.List;

import org.kuali.kfs.coa.businessobject.AccountGlobalDetail;
import org.kuali.kfs.coa.businessobject.SubObjCd;
import org.kuali.kfs.coa.businessobject.SubObjCdGlobal;
import org.kuali.kfs.coa.businessobject.SubObjCdGlobalDetail;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.rice.kns.document.MaintenanceLock;
import org.kuali.rice.kns.maintenance.KualiGlobalMaintainableImpl;

/**
 * This class provides some specific functionality for the {@link SubObjCdGlobal} maintenance document generateMaintenanceLocks -
 * generates maintenance locks on {@link SubObjCd}
 */
public class SubObjCdGlobalMaintainableImpl extends KualiGlobalMaintainableImpl {

    /**
     * This generates maintenance locks on {@link SubObjCd}
     * 
     * @see org.kuali.rice.kns.maintenance.Maintainable#generateMaintenanceLocks()
     */
    @Override
    public List<MaintenanceLock> generateMaintenanceLocks() {
        // create locking rep for each combination of account and object code
        List<MaintenanceLock> maintenanceLocks = new ArrayList();
        SubObjCdGlobal subObjCdGlobal = (SubObjCdGlobal) getBusinessObject();

        for (AccountGlobalDetail accountGlobalDetail : subObjCdGlobal.getAccountGlobalDetails()) {
            for (SubObjCdGlobalDetail subObjCdGlobalDetail : subObjCdGlobal.getSubObjCdGlobalDetails()) {
                MaintenanceLock maintenanceLock = new MaintenanceLock();
                maintenanceLock.setDocumentNumber(subObjCdGlobal.getDocumentNumber());

                StringBuffer lockrep = new StringBuffer();
                lockrep.append(SubObjCd.class.getName() + KFSConstants.Maintenance.AFTER_CLASS_DELIM);
                lockrep.append("fiscalYear" + KFSConstants.Maintenance.AFTER_FIELDNAME_DELIM);
                lockrep.append(subObjCdGlobalDetail.getUniversityFiscalYear() + KFSConstants.Maintenance.AFTER_VALUE_DELIM);
                lockrep.append("chartOfAccountsCode" + KFSConstants.Maintenance.AFTER_FIELDNAME_DELIM);
                lockrep.append(accountGlobalDetail.getChartOfAccountsCode() + KFSConstants.Maintenance.AFTER_VALUE_DELIM);
                lockrep.append("accountNumber" + KFSConstants.Maintenance.AFTER_FIELDNAME_DELIM);
                lockrep.append(accountGlobalDetail.getAccountNumber() + KFSConstants.Maintenance.AFTER_VALUE_DELIM);
                lockrep.append("financialObjectCode" + KFSConstants.Maintenance.AFTER_FIELDNAME_DELIM);
                lockrep.append(subObjCdGlobalDetail.getFinancialObjectCode() + KFSConstants.Maintenance.AFTER_VALUE_DELIM);
                lockrep.append("financialSubObjectCode" + KFSConstants.Maintenance.AFTER_FIELDNAME_DELIM);
                lockrep.append(subObjCdGlobal.getFinancialSubObjectCode());

                maintenanceLock.setLockingRepresentation(lockrep.toString());
                maintenanceLocks.add(maintenanceLock);
            }
        }
        return maintenanceLocks;
    }
}
