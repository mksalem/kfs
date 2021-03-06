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
package org.kuali.kfs.module.bc.businessobject;

import java.math.BigDecimal;

/**
 * Budget Construction Organization Account Funding Detail Report Business Object.
 */
public class BudgetConstructionOrgReasonSummaryReport {

    // Header parts
    private String fiscalYear;

    private String orgChartOfAccountsCode;
    private String orgChartOfAccountDescription;
    private String organizationCode;
    private String organizationName;
    private String consHdr;
    private String reqFy;
    private String objectCodes;
    private String numberAndNameForAccountSubAccount;
    private String thresholdOrReason;


    // Groups
    private String emplid;
    private Integer personSortCode;

    // Body parts

    private String chartOfAccountsCode;
    private String accountNumber;
    private String subAccountNumber;
    private String financialObjectCode;
    private String financialSubObjectCode;
    private String deleteBox;
    private String name;

    // from BudgetConstructionIntendedIncumbent
    private String iuClassificationLevel;
    // from PendingBudgetConstructionAppointmentFunding
    // from BudgetConstructionAdministrativePost
    private String administrativePost;
    // from BudgetConstructionPosition
    private String positionNumber;
    private String normalWorkMonthsAndiuPayMonths;
    private String positionSalaryPlanDefault;
    private String positionGradeDefault;
    // from BudgetConstructionCalculatedSalaryFoundationTracker
    private BigDecimal csfTimePercent;
    private Integer csfAmount;
    // from PendingBudgetConstructionAppointmentFunding
    private String appointmentFundingDurationCode;
    private Integer appointmentTotalIntendedAmount;
    private BigDecimal appointmentTotalIntendedFteQuantity;
    private Integer salaryMonths;
    private Integer salaryAmount;
    private BigDecimal percentAmount;
    private String tiFlag;


    private Integer amountChange;
    private BigDecimal percentChange;
    
    private Integer appointmentFundingReasonAmount;
    private String appointmentFundingReasonDescription;

    // not sure where it is from
    private BigDecimal positionFte;

    // Total parts
    // person
    private String personPositionNumber;
    private String personFiscalYearTag;
    private String personNormalMonthsAndPayMonths;

    private Integer personCsfAmount;
    private BigDecimal personCsfPercent;
    private Integer personSalaryNormalMonths;
    private Integer personSalaryAmount;
    private BigDecimal personSalaryPercent;
    private BigDecimal personSalaryFte;
    private String personTiFlag;
    private Integer personAmountChange;
    private BigDecimal personPercentChange;

    // org
    private BigDecimal newFte;
    private Integer newTotalAmount;
    private Integer newAverageAmount;
    private BigDecimal conFte;
    private Integer conTotalBaseAmount;
    private Integer conTotalRequestAmount;
    private Integer conAverageBaseAmount;
    private Integer conAverageRequestAmount;
    private Integer conAveragechange;
    private BigDecimal conPercentChange;


    /**
     * Gets the consHdr
     * 
     * @return Returns the consHdr.
     */
    public String getConsHdr() {
        return consHdr;
    }

    /**
     * Sets the consHdr
     * 
     * @param consHdr The consHdr to set.
     */
    public void setConsHdr(String consHdr) {
        this.consHdr = consHdr;
    }

    /**
     * Gets the fiscalYear
     * 
     * @return Returns the fiscalYear.
     */
    public String getFiscalYear() {
        return fiscalYear;
    }

    /**
     * Sets the fiscalYear
     * 
     * @param fiscalYear The fiscalYear to set.
     */
    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    /**
     * Gets the organizationCode
     * 
     * @return Returns the organizationCode.
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * Sets the organizationCode
     * 
     * @param organizationCode The organizationCode to set.
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * Gets the organizationName
     * 
     * @return Returns the organizationName.
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Sets the organizationName
     * 
     * @param organizationName The organizationName to set.
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    /**
     * Gets the reqFy
     * 
     * @return Returns the reqFy.
     */
    public String getReqFy() {
        return reqFy;
    }

    /**
     * Sets the reqFy
     * 
     * @param reqFy The reqFy to set.
     */
    public void setReqFy(String reqFy) {
        this.reqFy = reqFy;
    }

    /**
     * Gets the orgChartOfAccountDescription
     * 
     * @return Returns the orgChartOfAccountDescription.
     */
    public String getOrgChartOfAccountDescription() {
        return orgChartOfAccountDescription;
    }

    /**
     * Sets the orgChartOfAccountDescription
     * 
     * @param orgChartOfAccountDescription The orgChartOfAccountDescription to set.
     */
    public void setOrgChartOfAccountDescription(String orgChartOfAccountDescription) {
        this.orgChartOfAccountDescription = orgChartOfAccountDescription;
    }

    /**
     * Gets the orgChartOfAccountsCode
     * 
     * @return Returns the orgChartOfAccountsCode.
     */
    public String getOrgChartOfAccountsCode() {
        return orgChartOfAccountsCode;
    }

    /**
     * Sets the orgChartOfAccountsCode
     * 
     * @param orgChartOfAccountsCode The orgChartOfAccountsCode to set.
     */
    public void setOrgChartOfAccountsCode(String orgChartOfAccountsCode) {
        this.orgChartOfAccountsCode = orgChartOfAccountsCode;
    }


    public String getFinancialObjectCode() {
        return financialObjectCode;
    }

    public void setFinancialObjectCode(String financialObjectCode) {
        this.financialObjectCode = financialObjectCode;
    }

    public String getAdministrativePost() {
        return administrativePost;
    }

    public void setAdministrativePost(String administrativePost) {
        this.administrativePost = administrativePost;
    }

    public String getAppointmentFundingDurationCode() {
        return appointmentFundingDurationCode;
    }

    public void setAppointmentFundingDurationCode(String appointmentFundingDurationCode) {
        this.appointmentFundingDurationCode = appointmentFundingDurationCode;
    }


    public Integer getAppointmentTotalIntendedAmount() {
        return appointmentTotalIntendedAmount;
    }

    public void setAppointmentTotalIntendedAmount(Integer appointmentTotalIntendedAmount) {
        this.appointmentTotalIntendedAmount = appointmentTotalIntendedAmount;
    }

    public BigDecimal getAppointmentTotalIntendedFteQuantity() {
        return appointmentTotalIntendedFteQuantity;
    }

    public void setAppointmentTotalIntendedFteQuantity(BigDecimal appointmentTotalIntendedFteQuantity) {
        this.appointmentTotalIntendedFteQuantity = appointmentTotalIntendedFteQuantity;
    }

    public Integer getCsfAmount() {
        return csfAmount;
    }

    public void setCsfAmount(Integer csfAmount) {
        this.csfAmount = csfAmount;
    }

    public BigDecimal getCsfTimePercent() {
        return csfTimePercent;
    }

    public void setCsfTimePercent(BigDecimal csfTimePercent) {
        this.csfTimePercent = csfTimePercent;
    }

    public String getFinancialSubObjectCode() {
        return financialSubObjectCode;
    }

    public void setFinancialSubObjectCode(String financialSubObjectCode) {
        this.financialSubObjectCode = financialSubObjectCode;
    }

    public String getNormalWorkMonthsAndiuPayMonths() {
        return normalWorkMonthsAndiuPayMonths;
    }

    public void setNormalWorkMonthsAndiuPayMonths(String normalWorkMonthsAndiuPayMonths) {
        this.normalWorkMonthsAndiuPayMonths = normalWorkMonthsAndiuPayMonths;
    }

    public String getPositionGradeDefault() {
        return positionGradeDefault;
    }

    public void setPositionGradeDefault(String positionGradeDefault) {
        this.positionGradeDefault = positionGradeDefault;
    }

    public String getPositionNumber() {
        return positionNumber;
    }

    public void setPositionNumber(String positionNumber) {
        this.positionNumber = positionNumber;
    }

    public String getPositionSalaryPlanDefault() {
        return positionSalaryPlanDefault;
    }

    public void setPositionSalaryPlanDefault(String positionSalaryPlanDefault) {
        this.positionSalaryPlanDefault = positionSalaryPlanDefault;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAmountChange() {
        return amountChange;
    }

    public void setAmountChange(Integer amountChange) {
        this.amountChange = amountChange;
    }

    public BigDecimal getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(BigDecimal percentChange) {
        this.percentChange = percentChange;
    }

    /**
     * Gets the chartOfAccountsCode attribute. 
     * @return Returns the chartOfAccountsCode.
     */
    public String getChartOfAccountsCode() {
        return chartOfAccountsCode;
    }

    /**
     * Sets the chartOfAccountsCode attribute value.
     * @param chartOfAccountsCode The chartOfAccountsCode to set.
     */
    public void setChartOfAccountsCode(String chartOfAccountsCode) {
        this.chartOfAccountsCode = chartOfAccountsCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getEmplid() {
        return emplid;
    }

    public void setEmplid(String emplid) {
        this.emplid = emplid;
    }

    public BigDecimal getPositionFte() {
        return positionFte;
    }

    public void setPositionFte(BigDecimal positionFte) {
        this.positionFte = positionFte;
    }

    public String getSubAccountNumber() {
        return subAccountNumber;
    }

    public void setSubAccountNumber(String subAccountNumber) {
        this.subAccountNumber = subAccountNumber;
    }

    public String getDeleteBox() {
        return deleteBox;
    }

    public void setDeleteBox(String deleteBox) {
        this.deleteBox = deleteBox;
    }

    public String getObjectCodes() {
        return objectCodes;
    }

    public void setObjectCodes(String objectCodes) {
        this.objectCodes = objectCodes;
    }

    public String getNumberAndNameForAccountSubAccount() {
        return numberAndNameForAccountSubAccount;
    }

    public void setNumberAndNameForAccountSubAccount(String numberAndNameForAccountSubAccount) {
        this.numberAndNameForAccountSubAccount = numberAndNameForAccountSubAccount;
    }

    public Integer getPersonSortCode() {
        return personSortCode;
    }

    public void setPersonSortCode(Integer personSortCode) {
        this.personSortCode = personSortCode;
    }

    public String getIuClassificationLevel() {
        return iuClassificationLevel;
    }

    public void setIuClassificationLevel(String iuClassificationLevel) {
        this.iuClassificationLevel = iuClassificationLevel;
    }

    public BigDecimal getPercentAmount() {
        return percentAmount;
    }

    public void setPercentAmount(BigDecimal percentAmount) {
        this.percentAmount = percentAmount;
    }

    public Integer getSalaryAmount() {
        return salaryAmount;
    }

    public void setSalaryAmount(Integer salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    public Integer getSalaryMonths() {
        return salaryMonths;
    }

    public void setSalaryMonths(Integer salaryMonths) {
        this.salaryMonths = salaryMonths;
    }

    public String getTiFlag() {
        return tiFlag;
    }

    public void setTiFlag(String tiFlag) {
        this.tiFlag = tiFlag;
    }

    public Integer getConAverageBaseAmount() {
        return conAverageBaseAmount;
    }

    public void setConAverageBaseAmount(Integer conAverageBaseAmount) {
        this.conAverageBaseAmount = conAverageBaseAmount;
    }

    public Integer getConAveragechange() {
        return conAveragechange;
    }

    public void setConAveragechange(Integer conAveragechange) {
        this.conAveragechange = conAveragechange;
    }

    public Integer getConAverageRequestAmount() {
        return conAverageRequestAmount;
    }

    public void setConAverageRequestAmount(Integer conAverageRequestAmount) {
        this.conAverageRequestAmount = conAverageRequestAmount;
    }

    public BigDecimal getConFte() {
        return conFte;
    }

    public void setConFte(BigDecimal conFte) {
        this.conFte = conFte;
    }

    public BigDecimal getConPercentChange() {
        return conPercentChange;
    }

    public void setConPercentChange(BigDecimal conPercentChange) {
        this.conPercentChange = conPercentChange;
    }

    public Integer getConTotalBaseAmount() {
        return conTotalBaseAmount;
    }

    public void setConTotalBaseAmount(Integer conTotalBaseAmount) {
        this.conTotalBaseAmount = conTotalBaseAmount;
    }

    public Integer getConTotalRequestAmount() {
        return conTotalRequestAmount;
    }

    public void setConTotalRequestAmount(Integer conTotalRequestAmount) {
        this.conTotalRequestAmount = conTotalRequestAmount;
    }

    public Integer getNewAverageAmount() {
        return newAverageAmount;
    }

    public void setNewAverageAmount(Integer newAverageAmount) {
        this.newAverageAmount = newAverageAmount;
    }

    public BigDecimal getNewFte() {
        return newFte;
    }

    public void setNewFte(BigDecimal newFte) {
        this.newFte = newFte;
    }

    public Integer getNewTotalAmount() {
        return newTotalAmount;
    }

    public void setNewTotalAmount(Integer newTotalAmount) {
        this.newTotalAmount = newTotalAmount;
    }

    public Integer getPersonAmountChange() {
        return personAmountChange;
    }

    public void setPersonAmountChange(Integer personAmountChange) {
        this.personAmountChange = personAmountChange;
    }

    public Integer getPersonCsfAmount() {
        return personCsfAmount;
    }

    public void setPersonCsfAmount(Integer personCsfAmount) {
        this.personCsfAmount = personCsfAmount;
    }

    public String getPersonNormalMonthsAndPayMonths() {
        return personNormalMonthsAndPayMonths;
    }

    public void setPersonNormalMonthsAndPayMonths(String personNormalMonthsAndPayMonths) {
        this.personNormalMonthsAndPayMonths = personNormalMonthsAndPayMonths;
    }

    public BigDecimal getPersonCsfPercent() {
        return personCsfPercent;
    }

    public void setPersonCsfPercent(BigDecimal personCsfPercent) {
        this.personCsfPercent = personCsfPercent;
    }

    public String getPersonFiscalYearTag() {
        return personFiscalYearTag;
    }

    public void setPersonFiscalYearTag(String personFiscalYearTag) {
        this.personFiscalYearTag = personFiscalYearTag;
    }

    public BigDecimal getPersonPercentChange() {
        return personPercentChange;
    }

    public void setPersonPercentChange(BigDecimal personPercentChange) {
        this.personPercentChange = personPercentChange;
    }

    public String getPersonPositionNumber() {
        return personPositionNumber;
    }

    public void setPersonPositionNumber(String personPositionNumber) {
        this.personPositionNumber = personPositionNumber;
    }

    public Integer getPersonSalaryAmount() {
        return personSalaryAmount;
    }

    public void setPersonSalaryAmount(Integer personSalaryAmount) {
        this.personSalaryAmount = personSalaryAmount;
    }

    public BigDecimal getPersonSalaryFte() {
        return personSalaryFte;
    }

    public void setPersonSalaryFte(BigDecimal personSalaryFte) {
        this.personSalaryFte = personSalaryFte;
    }

    public Integer getPersonSalaryNormalMonths() {
        return personSalaryNormalMonths;
    }

    public void setPersonSalaryNormalMonths(Integer personSalaryNormalMonths) {
        this.personSalaryNormalMonths = personSalaryNormalMonths;
    }

    public BigDecimal getPersonSalaryPercent() {
        return personSalaryPercent;
    }

    public void setPersonSalaryPercent(BigDecimal personSalaryPercent) {
        this.personSalaryPercent = personSalaryPercent;
    }

    public String getPersonTiFlag() {
        return personTiFlag;
    }

    public void setPersonTiFlag(String personTiFlag) {
        this.personTiFlag = personTiFlag;
    }

    public String getThresholdOrReason() {
        return thresholdOrReason;
    }

    public void setThresholdOrReason(String thresholdOrReason) {
        this.thresholdOrReason = thresholdOrReason;
    }

    public Integer getAppointmentFundingReasonAmount() {
        return appointmentFundingReasonAmount;
    }

    public void setAppointmentFundingReasonAmount(Integer appointmentFundingReasonAmount) {
        this.appointmentFundingReasonAmount = appointmentFundingReasonAmount;
    }

    public String getAppointmentFundingReasonDescription() {
        return appointmentFundingReasonDescription;
    }

    public void setAppointmentFundingReasonDescription(String appointmentFundingReasonDescription) {
        this.appointmentFundingReasonDescription = appointmentFundingReasonDescription;
    }

}

