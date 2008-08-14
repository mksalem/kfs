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
package org.kuali.kfs.coa.businessobject;

import java.util.LinkedHashMap;
import java.util.List;

import org.kuali.kfs.sys.businessobject.Options;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.UniversityDateService;
import org.kuali.rice.kns.bo.PersistableBusinessObjectBase;
import org.kuali.rice.kns.util.TypedArrayList;

public class IndirectCostRecoveryRate extends PersistableBusinessObjectBase {
    
    private Integer universityFiscalYear;
    private String financialIcrSeriesIdentifier;
    private boolean active;
    private List indirectCostRecoveryRateDetails;
    
    private Options universityFiscal;
    
    public IndirectCostRecoveryRate() {
        universityFiscalYear = SpringContext.getBean(UniversityDateService.class).getCurrentFiscalYear();
        indirectCostRecoveryRateDetails = new TypedArrayList(IndirectCostRecoveryRateDetail.class);
    }
    
    @Override
    protected LinkedHashMap toStringMapper() {

        LinkedHashMap m = new LinkedHashMap();
        m.put("universityFiscalYear", this.universityFiscalYear);
        m.put("seriesId", this.financialIcrSeriesIdentifier);

        return m;
    }

    public Integer getUniversityFiscalYear() {
        return universityFiscalYear;
    }

    public void setUniversityFiscalYear(Integer universityFiscalYear) {
        this.universityFiscalYear = universityFiscalYear;
    }

    public Options getUniversityFiscal() {
        return universityFiscal;
    }

    public void setUniversityFiscal(Options universityFiscal) {
        this.universityFiscal = universityFiscal;
    }

    public List getIndirectCostRecoveryRateDetails() {
        return indirectCostRecoveryRateDetails;
    }

    public void setIndirectCostRecoveryRateDetails(List indirectCostRecoveryRateDetails) {
        this.indirectCostRecoveryRateDetails = indirectCostRecoveryRateDetails;
    }

    public String getFinancialIcrSeriesIdentifier() {
        return financialIcrSeriesIdentifier;
    }

    public void setFinancialIcrSeriesIdentifier(String financialIcrSeriesIdentifier) {
        this.financialIcrSeriesIdentifier = financialIcrSeriesIdentifier;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
}
