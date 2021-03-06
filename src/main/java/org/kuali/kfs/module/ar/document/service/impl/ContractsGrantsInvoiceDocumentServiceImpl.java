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
package org.kuali.kfs.module.ar.document.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.coa.businessobject.Account;
import org.kuali.kfs.coa.businessobject.ObjectCodeCurrent;
import org.kuali.kfs.coa.service.AccountService;
import org.kuali.kfs.coa.service.ObjectCodeService;
import org.kuali.kfs.gl.businessobject.Balance;
import org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAward;
import org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAwardAccount;
import org.kuali.kfs.integration.cg.ContractsAndGrantsModuleBillingService;
import org.kuali.kfs.module.ar.ArAuthorizationConstants;
import org.kuali.kfs.module.ar.ArConstants;
import org.kuali.kfs.module.ar.ArKeyConstants;
import org.kuali.kfs.module.ar.ArPropertyConstants;
import org.kuali.kfs.module.ar.batch.ContractsGrantsInvoiceDocumentBatchStep;
import org.kuali.kfs.module.ar.businessobject.AwardAccountObjectCodeTotalBilled;
import org.kuali.kfs.module.ar.businessobject.Bill;
import org.kuali.kfs.module.ar.businessobject.ContractsGrantsInvoiceDetail;
import org.kuali.kfs.module.ar.businessobject.CostCategory;
import org.kuali.kfs.module.ar.businessobject.CostCategoryObjectCode;
import org.kuali.kfs.module.ar.businessobject.CostCategoryObjectConsolidation;
import org.kuali.kfs.module.ar.businessobject.CostCategoryObjectLevel;
import org.kuali.kfs.module.ar.businessobject.CustomerInvoiceDetail;
import org.kuali.kfs.module.ar.businessobject.InvoiceAccountDetail;
import org.kuali.kfs.module.ar.businessobject.InvoiceAddressDetail;
import org.kuali.kfs.module.ar.businessobject.InvoiceBill;
import org.kuali.kfs.module.ar.businessobject.InvoiceDetailAccountObjectCode;
import org.kuali.kfs.module.ar.businessobject.InvoiceMilestone;
import org.kuali.kfs.module.ar.businessobject.InvoiceSuspensionCategory;
import org.kuali.kfs.module.ar.businessobject.InvoiceTemplate;
import org.kuali.kfs.module.ar.businessobject.Milestone;
import org.kuali.kfs.module.ar.businessobject.OrganizationAccountingDefault;
import org.kuali.kfs.module.ar.businessobject.OrganizationOptions;
import org.kuali.kfs.module.ar.businessobject.SystemInformation;
import org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument;
import org.kuali.kfs.module.ar.document.dataaccess.ContractsGrantsInvoiceDocumentDao;
import org.kuali.kfs.module.ar.document.service.AccountsReceivablePendingEntryService;
import org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService;
import org.kuali.kfs.module.ar.document.service.CustomerInvoiceDocumentService;
import org.kuali.kfs.module.ar.document.validation.SuspensionCategory;
import org.kuali.kfs.module.ar.identity.ArKimAttributes;
import org.kuali.kfs.module.ar.report.PdfFormattingMap;
import org.kuali.kfs.module.ar.service.ContractsGrantsBillingUtilityService;
import org.kuali.kfs.module.ar.service.CostCategoryService;
import org.kuali.kfs.sys.FinancialSystemModuleConfiguration;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.PdfFormFillerUtil;
import org.kuali.kfs.sys.batch.Job;
import org.kuali.kfs.sys.businessobject.FinancialSystemDocumentHeader;
import org.kuali.kfs.sys.businessobject.SystemOptions;
import org.kuali.kfs.sys.document.service.FinancialSystemDocumentService;
import org.kuali.kfs.sys.document.validation.event.AttributedRouteDocumentEvent;
import org.kuali.kfs.sys.service.OptionsService;
import org.kuali.kfs.sys.service.UniversityDateService;
import org.kuali.kfs.sys.util.FallbackMap;
import org.kuali.kfs.sys.util.ReflectionMap;
import org.kuali.rice.core.api.config.property.ConfigurationService;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.core.api.search.SearchOperator;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;
import org.kuali.rice.kew.api.document.DocumentStatus;
import org.kuali.rice.kew.api.exception.WorkflowException;
import org.kuali.rice.kim.api.identity.IdentityService;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kim.api.identity.PersonService;
import org.kuali.rice.kim.api.identity.principal.Principal;
import org.kuali.rice.kim.api.permission.PermissionService;
import org.kuali.rice.krad.UserSession;
import org.kuali.rice.krad.bo.Attachment;
import org.kuali.rice.krad.bo.DocumentHeader;
import org.kuali.rice.krad.bo.ModuleConfiguration;
import org.kuali.rice.krad.bo.Note;
import org.kuali.rice.krad.service.AttachmentService;
import org.kuali.rice.krad.service.BusinessObjectService;
import org.kuali.rice.krad.service.DocumentService;
import org.kuali.rice.krad.service.KualiModuleService;
import org.kuali.rice.krad.service.KualiRuleService;
import org.kuali.rice.krad.service.NoteService;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.DocumentException;

/**
 * This class implements the services required for Contracts & Grants Invoice Document.
 */
@Transactional
public class ContractsGrantsInvoiceDocumentServiceImpl implements ContractsGrantsInvoiceDocumentService {
    protected static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ContractsGrantsInvoiceDocumentServiceImpl.class);

    protected AccountsReceivablePendingEntryService accountsReceivablePendingEntryService;
    protected AccountService accountService;
    protected AttachmentService attachmentService;
    protected BusinessObjectService businessObjectService;
    protected ConfigurationService configurationService;
    protected ContractsGrantsBillingUtilityService contractsGrantsBillingUtilityService;
    protected ContractsGrantsInvoiceDocumentDao contractsGrantsInvoiceDocumentDao;
    protected ContractsAndGrantsModuleBillingService contractsAndGrantsModuleBillingService;
    protected CostCategoryService costCategoryService;
    protected CustomerInvoiceDocumentService customerInvoiceDocumentService;
    protected DateTimeService dateTimeService;
    protected DocumentService documentService;
    protected FinancialSystemDocumentService financialSystemDocumentService;
    protected IdentityService identityService;
    protected KualiModuleService kualiModuleService;
    protected KualiRuleService kualiRuleService;
    protected NoteService noteService;
    protected ObjectCodeService objectCodeService;
    protected ParameterService parameterService;
    protected PermissionService permissionService;
    protected PersonService personService;
    protected UniversityDateService universityDateService;
    protected OptionsService optionsService;

    private List<SuspensionCategory> suspensionCategories;

    public static final String REPORT_LINE_DIVIDER = "--------------------------------------------------------------------------------------------------------------";

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#createSourceAccountingLines(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public void createSourceAccountingLines(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument, List<ContractsAndGrantsBillingAwardAccount> awardAccounts) {
        // To check if the Source accounting lines are existing. If they are do nothing
        if (CollectionUtils.isEmpty(contractsGrantsInvoiceDocument.getSourceAccountingLines())) {
            ContractsAndGrantsBillingAward award = contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getAward();

            // To retrieve the financial object code from the Organization Accounting Default.
            final OrganizationAccountingDefault organizationAccountingDefault = retrieveBillingOrganizationAccountingDefault(contractsGrantsInvoiceDocument.getBillByChartOfAccountCode(), contractsGrantsInvoiceDocument.getBilledByOrganizationCode());
            if (ObjectUtils.isNotNull(award) && ObjectUtils.isNotNull(organizationAccountingDefault)) {
                if (StringUtils.equalsIgnoreCase(award.getInvoicingOptionCode(), ArConstants.INV_ACCOUNT)) {
                    // If its bill by Account , irrespective of it is by contract control account, there would be a single source accounting line with award account specified by the user.
                    CustomerInvoiceDetail cide = createSourceAccountingLine(contractsGrantsInvoiceDocument.getDocumentNumber(), awardAccounts.get(0).getChartOfAccountsCode(), awardAccounts.get(0).getAccountNumber(), organizationAccountingDefault.getDefaultInvoiceFinancialObjectCode(), getAccountingLineAmountForDocument(contractsGrantsInvoiceDocument), new Integer(1));
                    contractsGrantsInvoiceDocument.getSourceAccountingLines().add(cide);
                }
                else if (StringUtils.equalsIgnoreCase(award.getInvoicingOptionCode(), ArConstants.INV_CONTRACT_CONTROL_ACCOUNT)) {
                    // by control account
                    // If its bill by Contract Control Account there would be a single source accounting line.
                    CustomerInvoiceDetail cide = createSourceAccountingLinesByContractControlAccount(contractsGrantsInvoiceDocument, organizationAccountingDefault);
                    contractsGrantsInvoiceDocument.getSourceAccountingLines().add(cide);
                }
                else {
                    // by award
                    List<CustomerInvoiceDetail> awardAccountingLines = createSourceAccountingLinesByAward(contractsGrantsInvoiceDocument, organizationAccountingDefault);
                    contractsGrantsInvoiceDocument.getSourceAccountingLines().addAll(awardAccountingLines);
                }
            }
        }
    }

    /**
     * Determines the total amount for the given document, based on billing frequency of the award
     * @param invoice the contracts & grants invoice to find a total amount for
     * @return the total amount of the document
     */
    protected KualiDecimal getAccountingLineAmountForDocument(ContractsGrantsInvoiceDocument invoice) {
        if (StringUtils.equals(invoice.getInvoiceGeneralDetail().getBillingFrequencyCode(), ArConstants.PREDETERMINED_BILLING_SCHEDULE_CODE)) {
            return getBillAmountTotal(invoice);
        } else if (StringUtils.equals(invoice.getInvoiceGeneralDetail().getBillingFrequencyCode(), ArConstants.MILESTONE_BILLING_SCHEDULE_CODE)) {
            return getInvoiceMilestoneTotal(invoice);
        } else {
            return invoice.getTotalCostInvoiceDetail().getInvoiceAmount();
        }
    }

    /**
     * Generates the source accounting lines for a Contracts & Grants Invoice from the award accounts associated with an award (in the form of the pre-generated invoice account details)
     * @param contractsGrantsInvoiceDocument the Contracts & Grants Invoice to create invoice details for
     * @param organizationAccountingDefault the
     * @return a List of generated accounting lines
     */
    protected List<CustomerInvoiceDetail> createSourceAccountingLinesByAward(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument, final OrganizationAccountingDefault organizationAccountingDefault) {
        List<CustomerInvoiceDetail> awardAccountingLines = new ArrayList<>();
        if (!CollectionUtils.isEmpty(contractsGrantsInvoiceDocument.getAccountDetails())) {
            final Map<String, KualiDecimal> accountExpenditureAmounts = getCategoryExpenditureAmountsForInvoiceAccountDetail(contractsGrantsInvoiceDocument);
            for (InvoiceAccountDetail invAcctD : contractsGrantsInvoiceDocument.getAccountDetails()) {
                String accountNumber = invAcctD.getAccountNumber();
                String coaCode = invAcctD.getChartOfAccountsCode();
                String objectCode = organizationAccountingDefault.getDefaultInvoiceFinancialObjectCode();
                Integer sequenceNumber = contractsGrantsInvoiceDocument.getAccountDetails().indexOf(invAcctD) + 1;// To set a sequence number for the Accounting Lines
                // To calculate totalAmount based on the billing Frequency. Assuming that there would be only one
                // account if its Milestone/Predetermined Schedule.
                KualiDecimal totalAmount = KualiDecimal.ZERO;
                if (StringUtils.equalsIgnoreCase(contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getBillingFrequencyCode(), ArConstants.MILESTONE_BILLING_SCHEDULE_CODE)) {
                    final KualiDecimal totalMilestoneAmount = getInvoiceMilestoneTotal(contractsGrantsInvoiceDocument);
                    if (totalMilestoneAmount != KualiDecimal.ZERO) {
                        totalAmount = totalMilestoneAmount;
                    }
                }
                else if (StringUtils.equalsIgnoreCase(contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getBillingFrequencyCode(), ArConstants.PREDETERMINED_BILLING_SCHEDULE_CODE)) {
                    final KualiDecimal totalBillAmount = getBillAmountTotal(contractsGrantsInvoiceDocument);
                    if (totalBillAmount != KualiDecimal.ZERO) {
                        totalAmount = totalBillAmount;
                    }
                }
                else {
                    final String accountKey = StringUtils.join(new String[] { invAcctD.getChartOfAccountsCode(), invAcctD.getAccountNumber() }, "-");
                    totalAmount = accountExpenditureAmounts.containsKey(accountKey)
                            ? accountExpenditureAmounts.get(accountKey)
                            : KualiDecimal.ZERO;
                }

                CustomerInvoiceDetail cide = createSourceAccountingLine(contractsGrantsInvoiceDocument.getDocumentNumber(), coaCode, accountNumber, objectCode, totalAmount, sequenceNumber);
                awardAccountingLines.add(cide);
            }
        }
        return awardAccountingLines;
    }

    /**
     * Totals the current expenditure amounts of any invoice detail account object codes on the document which have set categories by account numbers
     * @param contractsGrantsInvoiceDocument the document holding invoice detail account object codes
     * @return a Map where the key is the concatenation of chartOfAccountsCode-accountNumber and the value is the expenditure amount on that account
     */
    protected Map<String, KualiDecimal> getCategoryExpenditureAmountsForInvoiceAccountDetail(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        Map<String, KualiDecimal> expenditureAmounts = new HashMap<>();
        for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : contractsGrantsInvoiceDocument.getInvoiceDetailAccountObjectCodes()) {
            final String accountKey = StringUtils.join(new String[] { invoiceDetailAccountObjectCode.getChartOfAccountsCode(), invoiceDetailAccountObjectCode.getAccountNumber() }, "-");
            if (!StringUtils.isBlank(invoiceDetailAccountObjectCode.getCategoryCode())) {
                KualiDecimal total = expenditureAmounts.get(accountKey);
                if (ObjectUtils.isNull(total)) {
                    total = KualiDecimal.ZERO;
                }
                expenditureAmounts.put(accountKey, total.add(invoiceDetailAccountObjectCode.getCurrentExpenditures()));
            }
        }
        return expenditureAmounts;
    }

    /**
     * Creates source accounting lines using the contract control account to populate
     * @param contractsGrantsInvoiceDocument the Contracts & Grants Invoice to add lines to
     * @param organizationAccountingDefault the organization accounting default associated with the invoice
     */
    protected CustomerInvoiceDetail createSourceAccountingLinesByContractControlAccount(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument, final OrganizationAccountingDefault organizationAccountingDefault) {
        // To get the account number and coa code for contract control account.
        String accountNumber = null;
        // Use the first account to get the contract control account number, as every account would have the same contract control account number.
        List<InvoiceAccountDetail> accountDetails = contractsGrantsInvoiceDocument.getAccountDetails();
        if (CollectionUtils.isNotEmpty(accountDetails) && StringUtils.isNotEmpty(accountDetails.get(0).getContractControlAccountNumber())) {
            accountNumber = accountDetails.get(0).getContractControlAccountNumber();
        }

        String coaCode = contractsGrantsInvoiceDocument.getBillByChartOfAccountCode();
        String objectCode = organizationAccountingDefault.getDefaultInvoiceFinancialObjectCode();

        CustomerInvoiceDetail cide = createSourceAccountingLine(contractsGrantsInvoiceDocument.getDocumentNumber(), coaCode, accountNumber, objectCode, getAccountingLineAmountForDocument(contractsGrantsInvoiceDocument), new Integer(1));
        return cide;
    }

    /**
     * Calculates the total of bill amounts if the given CINV doc is a pre-determined billing kind of CINV doc
     * @param contractsGrantsInvoiceDocument the document to total bills on
     * @return the total from the bills, or 0 if no bills exist or the CINV is not a pre-determined billing kind of CINV
     */
    protected KualiDecimal getBillAmountTotal(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        KualiDecimal totalBillAmount = KualiDecimal.ZERO;
        // To calculate the total bill amount.
        if (contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getBillingFrequencyCode().equalsIgnoreCase(ArConstants.PREDETERMINED_BILLING_SCHEDULE_CODE) && !CollectionUtils.isEmpty(contractsGrantsInvoiceDocument.getInvoiceBills())) {
            for (InvoiceBill bill : contractsGrantsInvoiceDocument.getInvoiceBills()) {
                if (bill.getEstimatedAmount() != null) {
                    totalBillAmount = totalBillAmount.add(bill.getEstimatedAmount());
                }
            }
        }
        return totalBillAmount;
    }

    /**
     * Calculates the total of milestones on the given CINV, assuming the CINV is a milestone kind of CINV
     * @param contractsGrantsInvoiceDocument the document to find total milestones on
     * @return the total of the milestones, or 0 if no milestones exist or if the CINV is not a milestone schedule CINV
     */
    protected KualiDecimal getInvoiceMilestoneTotal(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        KualiDecimal totalMilestoneAmount = KualiDecimal.ZERO;
        // To calculate the total milestone amount.
        if (contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getBillingFrequencyCode().equalsIgnoreCase(ArConstants.MILESTONE_BILLING_SCHEDULE_CODE) && !CollectionUtils.isEmpty(contractsGrantsInvoiceDocument.getInvoiceMilestones())) {
            for (InvoiceMilestone milestone : contractsGrantsInvoiceDocument.getInvoiceMilestones()) {
                if (milestone.getMilestoneAmount() != null) {
                    totalMilestoneAmount = totalMilestoneAmount.add(milestone.getMilestoneAmount());
                }
            }
        }
        return totalMilestoneAmount;
    }

    /**
     * Retrieves the OrganizationAccountingDefault record for the given billing organization
     * @param billByChartOfAccountsCode the chart of the billing organization
     * @param billByOrganizationCode the organization code of the billing organization
     * @return the OrganizationAccountingDefault for the given chart and organization code
     */
    protected OrganizationAccountingDefault retrieveBillingOrganizationAccountingDefault(final String billByChartOfAccountsCode, final String billByOrganizationCode) {
        Map<String, Object> criteria = new HashMap<String, Object>();
        Integer currentYear = universityDateService.getCurrentFiscalYear();
        criteria.put(KFSPropertyConstants.UNIVERSITY_FISCAL_YEAR, currentYear);
        criteria.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, billByChartOfAccountsCode);
        criteria.put(KFSPropertyConstants.ORGANIZATION_CODE, billByOrganizationCode);
        // Need to avoid hitting database in the loop. option would be to set the financial object code when the form loads and save it somewhere.
        OrganizationAccountingDefault organizationAccountingDefault = businessObjectService.findByPrimaryKey(OrganizationAccountingDefault.class, criteria);
        return organizationAccountingDefault;
    }

    /**
     * @param docNum
     * @param coaCode
     * @param acctNum
     * @param obCode
     * @param totalAmount
     * @param seqNum
     * @return
     */
    protected CustomerInvoiceDetail createSourceAccountingLine(String docNum, String coaCode, String acctNum, String obCode, KualiDecimal totalAmount, Integer seqNum) {
        CustomerInvoiceDetail cid = new CustomerInvoiceDetail();
        cid.setDocumentNumber(docNum);

        cid.setAccountNumber(acctNum);
        cid.setChartOfAccountsCode(coaCode);
        cid.setFinancialObjectCode(obCode);

        cid.setSequenceNumber(seqNum);
        cid.setInvoiceItemQuantity(BigDecimal.ONE);
        cid.setInvoiceItemUnitOfMeasureCode(ArConstants.CUSTOMER_INVOICE_DETAIL_UOM_DEFAULT);

        cid.setInvoiceItemUnitPrice(totalAmount);
        cid.setAmount(totalAmount);
        if (totalAmount.isNegative()) {
            cid.setInvoiceItemDiscountLineNumber(seqNum);
        }
        // To get AR Object codes for the GLPEs .... as it is not being called implicitly..

        cid.setAccountsReceivableObjectCode(getAccountsReceivablePendingEntryService().getAccountsReceivableObjectCode(cid));
        return cid;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#recalculateNewTotalBilled(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public void recalculateTotalAmountBilledToDate(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        ContractsGrantsInvoiceDetail totalCostInvoiceDetail = contractsGrantsInvoiceDocument.getTotalCostInvoiceDetail();

        // To verify the expenditure amounts have been changed and
        // update the invoiceDetailObjectCode
        boolean expenditureValueChanged = adjustObjectCodeAmountsIfChanged(contractsGrantsInvoiceDocument);

        if (expenditureValueChanged) {
            // update Total Direct Cost in the Invoice Detail Tab
            KualiDecimal totalDirectCostExpenditures = getInvoiceDetailExpenditureSum(contractsGrantsInvoiceDocument.getDirectCostInvoiceDetails());

            // Set expenditures to Direct Cost invoice Details
            ContractsGrantsInvoiceDetail totalDirectCostInvoiceDetail = contractsGrantsInvoiceDocument.getTotalDirectCostInvoiceDetail();
            if (ObjectUtils.isNotNull(totalDirectCostInvoiceDetail)){
                totalDirectCostInvoiceDetail.setInvoiceAmount(totalDirectCostExpenditures);
            }

            // update Total Indirect Cost in the Invoice Detail Tab
            KualiDecimal totalInDirectCostExpenditures = getInvoiceDetailExpenditureSum(contractsGrantsInvoiceDocument.getIndirectCostInvoiceDetails());

            // Set expenditures to Indirect Cost invoice Details
            ContractsGrantsInvoiceDetail totalInDirectCostInvoiceDetail = contractsGrantsInvoiceDocument.getTotalIndirectCostInvoiceDetail();
            if (ObjectUtils.isNotNull(totalInDirectCostInvoiceDetail)){
                totalInDirectCostInvoiceDetail.setInvoiceAmount(totalInDirectCostExpenditures);
            }

            // Set the total for Total Cost Invoice Details section.
            if(ObjectUtils.isNotNull(totalCostInvoiceDetail)) {
                totalCostInvoiceDetail.setInvoiceAmount(totalDirectCostInvoiceDetail.getInvoiceAmount().add(totalInDirectCostExpenditures));
            }
            recalculateAccountDetails(contractsGrantsInvoiceDocument.getAccountDetails(), contractsGrantsInvoiceDocument.getInvoiceDetailAccountObjectCodes());

            // update source accounting lines
            updateInvoiceSourceAccountingLines(contractsGrantsInvoiceDocument.getAccountDetails(), contractsGrantsInvoiceDocument.getSourceAccountingLines());


        }

        contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().setTotalPreviouslyBilled(getAwardBilledToDateAmountByProposalNumber(contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getProposalNumber()));

        KualiDecimal newTotalBilled = totalCostInvoiceDetail.getInvoiceAmount().add(contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getTotalPreviouslyBilled());
        newTotalBilled = newTotalBilled.add(getOtherTotalBilledForAwardPeriod(contractsGrantsInvoiceDocument));

        // set the General Detail Total to be billed - there would be only one value for Total Cost invoice Details.
        contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().setTotalAmountBilledToDate(newTotalBilled);
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getOtherNewTotalBilledForAwardPeriod(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public KualiDecimal getOtherTotalBilledForAwardPeriod(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        KualiDecimal newTotalBilled = KualiDecimal.ZERO;

        Map<String, String> fieldValuesForInvoice = new HashMap<>();
        fieldValuesForInvoice.put(ArPropertyConstants.ContractsGrantsInvoiceDocumentFields.PROPOSAL_NUMBER, contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getProposalNumber().toString());
        fieldValuesForInvoice.put(ArPropertyConstants.INVOICE_GENERAL_DETAIL+"."+ArPropertyConstants.BILLING_PERIOD, contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getBillingPeriod());
        String docNumberCriteriaString = SearchOperator.NOT + contractsGrantsInvoiceDocument.getDocumentNumber();
        if (ObjectUtils.isNotNull(contractsGrantsInvoiceDocument.getFinancialSystemDocumentHeader()) && StringUtils.isNotBlank(contractsGrantsInvoiceDocument.getFinancialSystemDocumentHeader().getFinancialDocumentInErrorNumber())) {
            docNumberCriteriaString += SearchOperator.NOT + contractsGrantsInvoiceDocument.getFinancialSystemDocumentHeader().getFinancialDocumentInErrorNumber();
        }
        fieldValuesForInvoice.put(KFSPropertyConstants.DOCUMENT_NUMBER, docNumberCriteriaString);
        fieldValuesForInvoice.put(ArPropertyConstants.DOCUMENT_STATUS_CODE, SearchOperator.NOT + KFSConstants.DocumentStatusCodes.PROCESSED + SearchOperator.NOT + KFSConstants.DocumentStatusCodes.APPROVED);

        Collection<ContractsGrantsInvoiceDocument> cgInvoiceDocuments = retrieveAllCGInvoicesByCriteria(fieldValuesForInvoice);
        for (ContractsGrantsInvoiceDocument cgInvoiceDocument: cgInvoiceDocuments) {
            for (InvoiceAccountDetail invAcctD : cgInvoiceDocument.getAccountDetails()) {
                newTotalBilled = newTotalBilled.add(invAcctD.getInvoiceAmount());
            }
        }

        return newTotalBilled;
    }

    /**
     * @param invoiceDetails
     * @return
     */
    public KualiDecimal getInvoiceDetailExpenditureSum(List<ContractsGrantsInvoiceDetail> invoiceDetails) {
        KualiDecimal totalExpenditures = KualiDecimal.ZERO;
        for (ContractsGrantsInvoiceDetail invoiceDetail : invoiceDetails) {
            totalExpenditures = totalExpenditures.add(invoiceDetail.getInvoiceAmount());
        }
        return totalExpenditures;
    }

    /**
     * @param invoiceAccountDetails
     * @param sourceAccountingLines
     */
    protected void updateInvoiceSourceAccountingLines(List<InvoiceAccountDetail> invoiceAccountDetails, List sourceAccountingLines) {

        if (sourceAccountingLines.size() > 1) {// Invoice By Award
            for (CustomerInvoiceDetail cide : (List<CustomerInvoiceDetail>) sourceAccountingLines) {
                for (InvoiceAccountDetail invoiceAccountDetail : invoiceAccountDetails) {
                    if (cide.getAccountNumber().equals(invoiceAccountDetail.getAccountNumber())) {
                        cide.setInvoiceItemUnitPrice(invoiceAccountDetail.getInvoiceAmount());
                        cide.setAmount(invoiceAccountDetail.getInvoiceAmount());
                    }
                }
            }
        }
        // This would be a case where the invoice is generated by Contract Control Account or Invoice By Account.
        else if (sourceAccountingLines.size() == 1) {
            KualiDecimal totalExpenditureAmount = KualiDecimal.ZERO;
            if (invoiceAccountDetails.size() == 1) {// Invoice By Account
                // update source accounting lines
                CustomerInvoiceDetail cide = (CustomerInvoiceDetail) sourceAccountingLines.get(0);
                cide.setInvoiceItemUnitPrice(invoiceAccountDetails.get(0).getInvoiceAmount());
                cide.setAmount(invoiceAccountDetails.get(0).getInvoiceAmount());
            }
            else {// Invoice By Contract Control Account
                for (InvoiceAccountDetail invoiceAccountDetail : invoiceAccountDetails) {
                    totalExpenditureAmount = totalExpenditureAmount.add(invoiceAccountDetail.getInvoiceAmount());
                }
                // update source accounting lines
                CustomerInvoiceDetail cide = (CustomerInvoiceDetail) sourceAccountingLines.get(0);
                cide.setInvoiceItemUnitPrice(totalExpenditureAmount);
                cide.setAmount(totalExpenditureAmount);
            }
        }

    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#prorateBill(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public void prorateBill(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) throws WorkflowException {
        KualiDecimal totalCost = new KualiDecimal(0); // Amount to be billed on this invoice
        // must iterate through the invoice details because the user might have manually changed the value
        for (ContractsGrantsInvoiceDetail invD : contractsGrantsInvoiceDocument.getInvoiceDetails()) {
            totalCost = totalCost.add(invD.getInvoiceAmount());
        }
        KualiDecimal billedTotalCost = contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getTotalPreviouslyBilled(); // Total Billed so far
        KualiDecimal accountAwardTotal = contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getAwardTotal(); // AwardTotal

        if (accountAwardTotal.subtract(billedTotalCost).isGreaterEqual(new KualiDecimal(0))) {
            KualiDecimal amountEligibleForBilling = accountAwardTotal.subtract(billedTotalCost);
            // only recalculate if the current invoice is over what's billable.

            if (totalCost.isGreaterThan(amountEligibleForBilling)) {
                // use BigDecimal because percentage should not have only a scale of 2, we need more for accuracy
                BigDecimal percentage = amountEligibleForBilling.bigDecimalValue().divide(totalCost.bigDecimalValue(), 10, BigDecimal.ROUND_HALF_DOWN);
                KualiDecimal amountToBill = new KualiDecimal(0); // use to check if rounding has left a few cents off

                ContractsGrantsInvoiceDetail largestCostCategory = null;
                BigDecimal largestAmount = BigDecimal.ZERO;
                for (ContractsGrantsInvoiceDetail invD : contractsGrantsInvoiceDocument.getInvoiceDetails()) {
                    BigDecimal newValue = invD.getInvoiceAmount().bigDecimalValue().multiply(percentage);
                    KualiDecimal newKualiDecimalValue = new KualiDecimal(newValue.setScale(2, BigDecimal.ROUND_DOWN));
                    invD.setInvoiceAmount(newKualiDecimalValue);
                    amountToBill = amountToBill.add(newKualiDecimalValue);
                    if (newValue.compareTo(largestAmount) > 0) {
                        largestAmount = newKualiDecimalValue.bigDecimalValue();
                        largestCostCategory = invD;
                    }
                }
                if (!amountToBill.equals(amountEligibleForBilling)) {
                    KualiDecimal remaining = amountEligibleForBilling.subtract(amountToBill);
                    if (ObjectUtils.isNull(largestCostCategory) && CollectionUtils.isNotEmpty(contractsGrantsInvoiceDocument.getInvoiceDetails())) {
                        largestCostCategory = contractsGrantsInvoiceDocument.getInvoiceDetails().get(0);
                    }
                    if (ObjectUtils.isNotNull(largestCostCategory)) {
                        largestCostCategory.setInvoiceAmount(largestCostCategory.getInvoiceAmount().add(remaining));
                    }
                }
                recalculateTotalAmountBilledToDate(contractsGrantsInvoiceDocument);
            }
        }
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#addToAccountObjectCodeBilledTotal(java.util.List)
     */
    @Override
    public void addToAccountObjectCodeBilledTotal(List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodes) {
        for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : invoiceDetailAccountObjectCodes) {
            Map<String, Object> totalBilledKeys = new HashMap<String, Object>();
            totalBilledKeys.put(KFSPropertyConstants.PROPOSAL_NUMBER, invoiceDetailAccountObjectCode.getProposalNumber());
            totalBilledKeys.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, invoiceDetailAccountObjectCode.getChartOfAccountsCode());
            totalBilledKeys.put(KFSPropertyConstants.ACCOUNT_NUMBER, invoiceDetailAccountObjectCode.getAccountNumber());
            totalBilledKeys.put(KFSPropertyConstants.FINANCIAL_OBJECT_CODE, invoiceDetailAccountObjectCode.getFinancialObjectCode());

            List<AwardAccountObjectCodeTotalBilled> awardAccountObjectCodeTotalBilledList = (List<AwardAccountObjectCodeTotalBilled>) businessObjectService.findMatching(AwardAccountObjectCodeTotalBilled.class, totalBilledKeys);
            AwardAccountObjectCodeTotalBilled awardAccountObjectCodeTotalBilled = new AwardAccountObjectCodeTotalBilled();
            if (awardAccountObjectCodeTotalBilledList != null && !awardAccountObjectCodeTotalBilledList.isEmpty()) {
                awardAccountObjectCodeTotalBilled = awardAccountObjectCodeTotalBilledList.get(0);
                awardAccountObjectCodeTotalBilled.setTotalBilled(awardAccountObjectCodeTotalBilled.getTotalBilled().add(invoiceDetailAccountObjectCode.getCurrentExpenditures()));
            }
            else {
                awardAccountObjectCodeTotalBilled.setProposalNumber(invoiceDetailAccountObjectCode.getProposalNumber());
                awardAccountObjectCodeTotalBilled.setChartOfAccountsCode(invoiceDetailAccountObjectCode.getChartOfAccountsCode());
                awardAccountObjectCodeTotalBilled.setAccountNumber(invoiceDetailAccountObjectCode.getAccountNumber());
                awardAccountObjectCodeTotalBilled.setFinancialObjectCode(invoiceDetailAccountObjectCode.getFinancialObjectCode());
                awardAccountObjectCodeTotalBilled.setTotalBilled(invoiceDetailAccountObjectCode.getCurrentExpenditures());
            }
            getBusinessObjectService().save(awardAccountObjectCodeTotalBilled);
        }
    }

    /**
     * If any of the current expenditures for the cost categories on the Contracts & Grants Invoice Document have changed,
     * recalculate the Object Code amounts.
     *
     * @param contractsGrantsInvoiceDocument document containing cost categories to review
     * @return true if expenditure value changed, false otherwise
     */
    protected boolean adjustObjectCodeAmountsIfChanged(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        boolean isExpenditureValueChanged = false;

        // put the invoiceDetailAccountObjectCode into a map based on category
        List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodes = contractsGrantsInvoiceDocument.getInvoiceDetailAccountObjectCodes();
        Map<String, List<InvoiceDetailAccountObjectCode>> invoiceDetailAccountObjectCodeMap = new HashMap<>();
        for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : invoiceDetailAccountObjectCodes) {
            String categoryCode = invoiceDetailAccountObjectCode.getCategoryCode();
            List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodeList = invoiceDetailAccountObjectCodeMap.get(categoryCode);
            // if new category, create new list to put into map
            if (invoiceDetailAccountObjectCodeList == null) {
                List<InvoiceDetailAccountObjectCode> newInvoiceDetailAccountObjectCodeList = new ArrayList<InvoiceDetailAccountObjectCode>();
                newInvoiceDetailAccountObjectCodeList.add(invoiceDetailAccountObjectCode);
                invoiceDetailAccountObjectCodeMap.put(categoryCode, newInvoiceDetailAccountObjectCodeList);
            }
            // else, if list is found, add it to existing list
            else {
                invoiceDetailAccountObjectCodeMap.get(categoryCode).add(invoiceDetailAccountObjectCode);
            }
        }

        // figure out if any of the current expenditures for the category has been changed. If yes, then update the invoiceDetailObjectCode and update account details
        for (ContractsGrantsInvoiceDetail invoiceDetail : contractsGrantsInvoiceDocument.getInvoiceDetails()) {
            KualiDecimal total = getSumOfExpendituresOfCategory(invoiceDetailAccountObjectCodeMap.get(invoiceDetail.getCategoryCode()));
            // To set expenditures to zero if its blank - to avoid exceptions.
            if (ObjectUtils.isNull(invoiceDetail.getInvoiceAmount())) {
                invoiceDetail.setInvoiceAmount(KualiDecimal.ZERO);
            }

            if (invoiceDetail.getInvoiceAmount().compareTo(total) != 0) {
                recalculateObjectCodeByCategory(contractsGrantsInvoiceDocument, invoiceDetail, total, invoiceDetailAccountObjectCodeMap.get(invoiceDetail.getCategoryCode()));
                isExpenditureValueChanged = true;
            }
        }
        return isExpenditureValueChanged;
    }

    /**
     * @param invoiceDetailAccountObjectCodes
     * @return
     */
    protected KualiDecimal getSumOfExpendituresOfCategory(List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodes) {
        KualiDecimal total = KualiDecimal.ZERO;
        // null can occur if this category has no invoice detail objectcode amounts
        if (!ObjectUtils.isNull(invoiceDetailAccountObjectCodes)) {
            for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : invoiceDetailAccountObjectCodes) {
                total = total.add(invoiceDetailAccountObjectCode.getCurrentExpenditures());
            }
        }
        return total;
    }

    /**
     * This method recalculates the invoiceDetailAccountObjectCode in one category that sits behind the scenes of the invoice document.
     * @param contractsGrantsInvoiceDocument
     * @param invoiceDetail
     * @param total is the sum of the current expenditures from all the object codes in that category
     * @param invoiceDetailAccountObjectCodes
     */
    protected void recalculateObjectCodeByCategory(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument, ContractsGrantsInvoiceDetail invoiceDetail, KualiDecimal total, List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodes) {
        KualiDecimal currentExpenditure = invoiceDetail.getInvoiceAmount();
        KualiDecimal newTotalAmount = KualiDecimal.ZERO;

        // if the sum of the object codes is 0, then distribute the expenditure change evenly to all object codes in the category
        if (total.compareTo(KualiDecimal.ZERO) == 0) {
            if (invoiceDetailAccountObjectCodes != null) {
                int numberOfObjectCodes = invoiceDetailAccountObjectCodes.size();
                if (numberOfObjectCodes != 0) {
                    KualiDecimal newAmount = new KualiDecimal(currentExpenditure.bigDecimalValue().divide(new BigDecimal(numberOfObjectCodes), 10, BigDecimal.ROUND_HALF_DOWN));
                    for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : invoiceDetailAccountObjectCodes) {
                        invoiceDetailAccountObjectCode.setCurrentExpenditures(newAmount);
                        newTotalAmount = newTotalAmount.add(newAmount);
                    }
                }
            }
            else { // if the list is null, then there are no account/object code in the gl_balance_t. So assign the amount to the first object code in the category
                assignCurrentExpenditureToNonExistingAccountObjectCode(contractsGrantsInvoiceDocument, invoiceDetail);
            }
        }
        else {

            for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : invoiceDetailAccountObjectCodes) {
                // this may rarely happen
                // if the initial total is 0, that means none of the object codes in this category is set to bill. If this amount is changed, then just divide evenly among all object codes.
                KualiDecimal newAmount = (new KualiDecimal(invoiceDetailAccountObjectCode.getCurrentExpenditures().bigDecimalValue().divide(total.bigDecimalValue(), 10, BigDecimal.ROUND_HALF_DOWN).multiply(currentExpenditure.bigDecimalValue())));
                invoiceDetailAccountObjectCode.setCurrentExpenditures(newAmount);
                newTotalAmount = newTotalAmount.add(newAmount);
            }

            int remainderFromRounding = currentExpenditure.subtract(newTotalAmount).multiply(new KualiDecimal(100)).intValue();

            // add remainder from rounding
            KualiDecimal addAmount = new KualiDecimal(0.01);
            if (remainderFromRounding < 0) {
                addAmount = new KualiDecimal(-0.01);
                remainderFromRounding = Math.abs(remainderFromRounding);
            }

            for (int i = 0, j = 0; i < remainderFromRounding; i++, j++) {
                // reset j if its more than size of list
                if (j >= invoiceDetailAccountObjectCodes.size()) {
                    j = 0;
                }
                invoiceDetailAccountObjectCodes.get(j).setCurrentExpenditures(invoiceDetailAccountObjectCodes.get(j).getCurrentExpenditures().add(addAmount));
            }
        }
    }

    /**
     * @param contractsGrantsInvoiceDocument
     * @param invoiceDetail
     */
    protected void assignCurrentExpenditureToNonExistingAccountObjectCode(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument, ContractsGrantsInvoiceDetail invoiceDetail) {
        String categoryCode = invoiceDetail.getCategoryCode();
        if (StringUtils.isBlank(categoryCode)) {
            throw new IllegalArgumentException("Category Code can not be null during recalculation of account object code for Contracts & Grants Invoice Document.");
        }
        // get the category that matches this category code.
        final CostCategory category = businessObjectService.findBySinglePrimaryKey(CostCategory.class, categoryCode);

        // got the category now.
        if (!ObjectUtils.isNull(category)) {
            final KualiDecimal oneCent = new KualiDecimal(0.01);

            int size = contractsGrantsInvoiceDocument.getAccountDetails().size();
            KualiDecimal amount = new KualiDecimal(invoiceDetail.getInvoiceAmount().bigDecimalValue().divide(new BigDecimal(size), 2, RoundingMode.HALF_UP));
            KualiDecimal remainder = invoiceDetail.getInvoiceAmount().subtract(amount.multiply(new KualiDecimal(size)));

            for (InvoiceAccountDetail invoiceAccountDetail : contractsGrantsInvoiceDocument.getAccountDetails()) {
                InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode = new InvoiceDetailAccountObjectCode();
                invoiceDetailAccountObjectCode.setDocumentNumber(contractsGrantsInvoiceDocument.getDocumentNumber());
                invoiceDetailAccountObjectCode.setProposalNumber(contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getProposalNumber());
                invoiceDetailAccountObjectCode.setCategoryCode(categoryCode);
                invoiceDetailAccountObjectCode.setAccountNumber(invoiceAccountDetail.getAccountNumber());
                invoiceDetailAccountObjectCode.setChartOfAccountsCode(invoiceAccountDetail.getChartOfAccountsCode());
                invoiceDetailAccountObjectCode.setCumulativeExpenditures(KualiDecimal.ZERO); // it's 0.00 that's why we are in this section to begin with.
                invoiceDetailAccountObjectCode.setTotalBilled(KualiDecimal.ZERO); // this is also 0.00 because it has never been billed before
                final ObjectCodeCurrent objectCode = getCostCategoryService().findObjectCodeForChartAndCategory(invoiceAccountDetail.getChartOfAccountsCode(), categoryCode);
                if (!ObjectUtils.isNull(objectCode)) {
                    invoiceDetailAccountObjectCode.setFinancialObjectCode(objectCode.getFinancialObjectCode());
                }

                // tack on or remove one penny until the remainder is 0 - take a penny, leave a penny!
                if (remainder.isGreaterThan(KualiDecimal.ZERO)) {
                    amount = amount.add(oneCent);
                    remainder = remainder.subtract(oneCent);
                }
                else if (remainder.isLessThan(KualiDecimal.ZERO)) {
                    amount = amount.subtract(oneCent);
                    remainder = remainder.add(oneCent);
                }
                invoiceDetailAccountObjectCode.setCurrentExpenditures(amount);

                List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodes = contractsGrantsInvoiceDocument.getInvoiceDetailAccountObjectCodes();
                if (invoiceDetailAccountObjectCodes.contains(invoiceDetailAccountObjectCode)) {
                    // update existing code
                    InvoiceDetailAccountObjectCode original = invoiceDetailAccountObjectCodes.get(invoiceDetailAccountObjectCodes.indexOf(invoiceDetailAccountObjectCode));
                    original.setCurrentExpenditures(amount);
                    original.setCategoryCode(categoryCode);
                } else {
                    // add this single account object code item to the list in the Map
                    contractsGrantsInvoiceDocument.getInvoiceDetailAccountObjectCodes().add(invoiceDetailAccountObjectCode);
                }
            }
        }
        else {
            LOG.error("Category Code cannot be found from the category list during recalculation of account object code for Contracts & Grants Invoice Document.");
        }
    }

    /**
     * @param invoiceAccountDetails
     * @param invoiceDetailAccountObjectCodes
     */
    public void recalculateAccountDetails(List<InvoiceAccountDetail> invoiceAccountDetails, List<InvoiceDetailAccountObjectCode> invoiceDetailAccountObjectCodes) {
        Map<String, KualiDecimal> currentExpenditureByAccountNumberMap = new HashMap<String, KualiDecimal>();
        for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : invoiceDetailAccountObjectCodes) {
            String accountNumber = invoiceDetailAccountObjectCode.getAccountNumber();
            KualiDecimal expenditureSum = currentExpenditureByAccountNumberMap.get(accountNumber);
            // if account number not found in map, then create new total, 0
            if (expenditureSum == null) {
                expenditureSum = KualiDecimal.ZERO;
            }
            expenditureSum = expenditureSum.add(invoiceDetailAccountObjectCode.getCurrentExpenditures());
            currentExpenditureByAccountNumberMap.put(accountNumber, expenditureSum);
        }

        for (InvoiceAccountDetail invoiceAccountDetail : invoiceAccountDetails) {
            final KualiDecimal expenditureAmount = ObjectUtils.isNull(currentExpenditureByAccountNumberMap.get(invoiceAccountDetail.getAccountNumber()))
                    ? KualiDecimal.ZERO
                    : currentExpenditureByAccountNumberMap.get(invoiceAccountDetail.getAccountNumber());
            invoiceAccountDetail.setInvoiceAmount(expenditureAmount);
        }
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getAwardBilledToDateAmountByProposalNumber(java.lang.Long)
     */
    @Override
    public KualiDecimal getAwardBilledToDateAmountByProposalNumber(Long proposalNumber) {
        Map<String, Object> keys = new HashMap<String, Object>();
        keys.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);

        List<AwardAccountObjectCodeTotalBilled> accountObjectCodeTotalBilledList = (List<AwardAccountObjectCodeTotalBilled>) businessObjectService.findMatching(AwardAccountObjectCodeTotalBilled.class, keys);
        KualiDecimal billedToDateAmount = KualiDecimal.ZERO;
        for (AwardAccountObjectCodeTotalBilled awardAccountObjectCodeTotalBilled : accountObjectCodeTotalBilledList) {
            billedToDateAmount = billedToDateAmount.add(awardAccountObjectCodeTotalBilled.getTotalBilled());
        }
        return billedToDateAmount;
    }

    /**
     * This method retrieves all CG invoice document that match the given field values
     *
     * @param fieldValues
     * @return
     */
    @Override
    public Collection<ContractsGrantsInvoiceDocument> retrieveAllCGInvoicesByCriteria(Map fieldValues) {
        return contractsGrantsInvoiceDocumentDao.getMatchingInvoicesByCollection(fieldValues);
    }

    /**
     * This method calculates the Budget and cumulative amount for Award Account
     *
     * @param awardAccount
     * @return
     */
    @Override
    public KualiDecimal getBudgetAndActualsForAwardAccount(ContractsAndGrantsBillingAwardAccount awardAccount, String balanceTypeCode, Date awardBeginningDate) {
        List<Balance> glBalances = new ArrayList<Balance>();
        KualiDecimal balanceAmount = KualiDecimal.ZERO;
        KualiDecimal balAmt = KualiDecimal.ZERO;
        Integer currentYear = universityDateService.getCurrentFiscalYear();
        final SystemOptions systemOption = optionsService.getCurrentYearOptions();
        List<Integer> fiscalYears = new ArrayList<Integer>();
        Calendar c = Calendar.getInstance();

        if (ObjectUtils.isNotNull(awardBeginningDate)) {
            Integer fiscalYear = universityDateService.getFiscalYear(awardBeginningDate);

            if (ObjectUtils.isNotNull(fiscalYear)) {
                for (Integer i = fiscalYear; i <= currentYear; i++) {
                    fiscalYears.add(i);
                }
                for (Integer eachFiscalYr : fiscalYears) {

                    Map<String, Object> balanceKeys = new HashMap<String, Object>();
                    balanceKeys.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, awardAccount.getChartOfAccountsCode());
                    balanceKeys.put(KFSPropertyConstants.ACCOUNT_NUMBER, awardAccount.getAccountNumber());
                    balanceKeys.put(KFSPropertyConstants.UNIVERSITY_FISCAL_YEAR, eachFiscalYr);
                    balanceKeys.put(KFSPropertyConstants.BALANCE_TYPE_CODE, balanceTypeCode);
                    balanceKeys.put(KFSPropertyConstants.OBJECT_TYPE_CODE, systemOption.getFinObjTypeExpenditureexp().getCode());
                    glBalances.addAll(businessObjectService.findMatching(Balance.class, balanceKeys));
                }
                for (Balance bal : glBalances) {
                    if (ObjectUtils.isNull(bal.getSubAccount()) || ObjectUtils.isNull(bal.getSubAccount().getA21SubAccount()) || !StringUtils.equalsIgnoreCase(bal.getSubAccount().getA21SubAccount().getSubAccountTypeCode(), KFSConstants.SubAccountType.COST_SHARE)) {
                        balAmt = bal.getContractsGrantsBeginningBalanceAmount().add(bal.getAccountLineAnnualBalanceAmount());
                        balanceAmount = balanceAmount.add(balAmt);
                    }
                }
            }
        }
        return balanceAmount;
    }

    /**
     * This method serves as a create and update. When it is first called, the List<InvoiceSuspensionCategory> is empty. This list
     * then gets populated with invoiceSuspensionCategories where the test fails. Each time the document goes through validation,
     * and this method gets called, it will update the list by adding or remvoing the suspension categories
     *
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#updateSuspensionCategoriesOnDocument(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public void updateSuspensionCategoriesOnDocument(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        if (!contractsGrantsInvoiceDocument.isCorrectionDocument()) {
            ContractsAndGrantsBillingAward award = contractsGrantsInvoiceDocument.getInvoiceGeneralDetail().getAward();
            String documentNumber = contractsGrantsInvoiceDocument.getDocumentNumber();

            if (ObjectUtils.isNotNull(suspensionCategories)) {
                for (SuspensionCategory suspensionCategory : suspensionCategories) {
                    InvoiceSuspensionCategory invoiceSuspensionCategory = new InvoiceSuspensionCategory(documentNumber, suspensionCategory.getCode());
                    if (suspensionCategory.shouldSuspend(contractsGrantsInvoiceDocument)) {
                        if (!contractsGrantsInvoiceDocument.getInvoiceSuspensionCategories().contains(invoiceSuspensionCategory)) {
                            contractsGrantsInvoiceDocument.getInvoiceSuspensionCategories().add(invoiceSuspensionCategory);
                        }
                    } else if (contractsGrantsInvoiceDocument.getInvoiceSuspensionCategories().contains(invoiceSuspensionCategory)) {
                        contractsGrantsInvoiceDocument.getInvoiceSuspensionCategories().remove(invoiceSuspensionCategory);
                    }
                }
            }
        }
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#calculateTotalPaymentsToDateByAward(org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAward)
     */
    @Override
    public KualiDecimal calculateTotalPaymentsToDateByAward(ContractsAndGrantsBillingAward award) {
        KualiDecimal totalPayments = KualiDecimal.ZERO;

        Map<String, Object> criteria = new HashMap<String, Object>();
        criteria.put(ArPropertyConstants.ContractsGrantsInvoiceDocumentFields.PROPOSAL_NUMBER, award.getProposalNumber());
        Collection<ContractsGrantsInvoiceDocument> cgInvoiceDocs = businessObjectService.findMatching(ContractsGrantsInvoiceDocument.class, criteria);

        for (ContractsGrantsInvoiceDocument cgInvoiceDoc : cgInvoiceDocs) {
            totalPayments = totalPayments.add(getCustomerInvoiceDocumentService().calculateAppliedPaymentAmount(cgInvoiceDoc));
        }
        return totalPayments;
    }

    /**
     * This method calculates the Cumulative Disbursement amount for an awardAccount
     *
     * @param awardAccount
     * @return
     */
    protected KualiDecimal getCumulativeCashDisbursement(ContractsAndGrantsBillingAwardAccount awardAccount, java.sql.Date awardBeginningDate) {
        Integer currentYear = universityDateService.getCurrentFiscalYear();
        final SystemOptions systemOption = optionsService.getCurrentYearOptions();
        KualiDecimal cumAmt = KualiDecimal.ZERO;
        KualiDecimal balAmt = KualiDecimal.ZERO;
        List<Balance> glBalances = new ArrayList<Balance>();

        List<Integer> fiscalYears = new ArrayList<Integer>();
        Calendar c = Calendar.getInstance();


        Integer fiscalYear = universityDateService.getFiscalYear(awardBeginningDate);

        for (Integer i = fiscalYear; i <= currentYear; i++) {
            fiscalYears.add(i);
        }
        for (Integer eachFiscalYr : fiscalYears) {
            Map<String, Object> balanceKeys = new HashMap<String, Object>();
            balanceKeys.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, awardAccount.getChartOfAccountsCode());
            balanceKeys.put(KFSPropertyConstants.ACCOUNT_NUMBER, awardAccount.getAccountNumber());
            balanceKeys.put(KFSPropertyConstants.UNIVERSITY_FISCAL_YEAR, eachFiscalYr);
            balanceKeys.put(KFSPropertyConstants.BALANCE_TYPE_CODE, systemOption.getActualFinancialBalanceTypeCd());
            balanceKeys.put(KFSPropertyConstants.OBJECT_TYPE_CODE, systemOption.getFinObjTypeExpenditureexp().getCode());
            glBalances.addAll(businessObjectService.findMatching(Balance.class, balanceKeys));
        }
        for (Balance bal : glBalances) {
            if (ObjectUtils.isNull(bal.getSubAccount()) || ObjectUtils.isNull(bal.getSubAccount().getA21SubAccount()) || !StringUtils.equalsIgnoreCase(bal.getSubAccount().getA21SubAccount().getSubAccountTypeCode(), KFSConstants.SubAccountType.COST_SHARE)) {
                balAmt = bal.getContractsGrantsBeginningBalanceAmount().add(bal.getAccountLineAnnualBalanceAmount());
                cumAmt = cumAmt.add(balAmt);
            }
        }
        return cumAmt;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getMilestonesBilledToDateAmount(java.lang.Long)
     */
    @Override
    public KualiDecimal getMilestonesBilledToDateAmount(Long proposalNumber) {
        Map<String, Object> totalBilledKeys = new HashMap<String, Object>();
        totalBilledKeys.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);
        KualiDecimal billedToDateAmount = KualiDecimal.ZERO;

        List<Milestone> milestones = (List<Milestone>) businessObjectService.findMatching(Milestone.class, totalBilledKeys);
        if (CollectionUtils.isNotEmpty(milestones)) {
            Iterator<Milestone> iterator = milestones.iterator();
            while (iterator.hasNext()) {
                Milestone milestone = iterator.next();
                if (milestone.isBilled()) {
                    billedToDateAmount = billedToDateAmount.add(milestone.getMilestoneAmount());
                }
            }
        }
        return billedToDateAmount;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getPredeterminedBillingBilledToDateAmount(java.lang.Long)
     */
    @Override
    public KualiDecimal getPredeterminedBillingBilledToDateAmount(Long proposalNumber) {
        Map<String, Object> totalBilledKeys = new HashMap<String, Object>();
        totalBilledKeys.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);
        KualiDecimal billedToDateAmount = KualiDecimal.ZERO;

        List<Bill> bills = (List<Bill>) businessObjectService.findMatching(Bill.class, totalBilledKeys);
        if (CollectionUtils.isNotEmpty(bills)) {
            Iterator<Bill> iterator = bills.iterator();
            while (iterator.hasNext()) {
                Bill bill = iterator.next();
                if (bill.isBilled()) {
                    billedToDateAmount = billedToDateAmount.add(bill.getEstimatedAmount());
                }
            }
        }
        return billedToDateAmount;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getExpiredAccountsOfAward(org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAward)
     *      Retrive all the expired accounts of an award
     */
    @Override
    public Collection<Account> getExpiredAccountsOfAward(ContractsAndGrantsBillingAward award) {

        Collection<ContractsAndGrantsBillingAwardAccount> awardAccounts = award.getActiveAwardAccounts();
        Collection<Account> expiredAwardAccounts = new ArrayList<Account>();

        if (awardAccounts != null && !awardAccounts.isEmpty()) {

            Date today = dateTimeService.getCurrentSqlDateMidnight();

            for (ContractsAndGrantsBillingAwardAccount awardAccount : awardAccounts) {
                Account account = awardAccount.getAccount();

                if (account != null) {
                    Date expDt = account.getAccountExpirationDate();

                    if (expDt != null && expDt.before(today)) {
                        expiredAwardAccounts.add(account);
                    }
                }

            }

            return expiredAwardAccounts;
        }

        return null;
    }


    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getContractControlAccounts(org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAward)
     */
    @Override
    public List<Account> getContractControlAccounts(ContractsAndGrantsBillingAward award) {

        if (!CollectionUtils.isEmpty(award.getActiveAwardAccounts())) {
            List<Account> controlAccounts = new ArrayList<Account>();
            for (ContractsAndGrantsBillingAwardAccount awardAccount : award.getActiveAwardAccounts()) {
                if (ObjectUtils.isNotNull(awardAccount.getAccount().getContractControlAccount())) {
                    controlAccounts.add(awardAccount.getAccount().getContractControlAccount());
                }
            }
            if (CollectionUtils.isNotEmpty(controlAccounts)) {
                return controlAccounts;
            }
        }
        return null;
    }

    /**
     * To retrieve processing chart code and org code from the billing chart code and org code
     *
     * @param cgInvoiceDocument
     * @param billingChartOfAccountsCode
     * @param billingOrganizationCode
     * @return
     */
    @Override
    public List<String> getProcessingFromBillingCodes(String billingChartCode, String billingOrgCode) {

        List<String> procCodes = new ArrayList<String>();
        // To access Organization Options to find the billing values based on procesing codes
        Map<String, String> criteria = new HashMap<String, String>();
        criteria.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, billingChartCode);
        criteria.put(KFSPropertyConstants.ORGANIZATION_CODE, billingOrgCode);
        OrganizationOptions organizationOptions = businessObjectService.findByPrimaryKey(OrganizationOptions.class, criteria);

        if (ObjectUtils.isNotNull(organizationOptions)) {
            procCodes.add(0, organizationOptions.getProcessingChartOfAccountCode());
            procCodes.add(1, organizationOptions.getProcessingOrganizationCode());
        }

        return procCodes;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#canViewInvoice(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument, java.lang.String)
     */
    @Override
    public boolean canViewInvoice(ContractsGrantsInvoiceDocument invoice, String collectorPrincipalId) {
        Map<String, String> qualification = new HashMap<String, String>(3);
        qualification.put(ArKimAttributes.BILLING_CHART_OF_ACCOUNTS_CODE, invoice.getBillByChartOfAccountCode());
        qualification.put(ArKimAttributes.BILLING_ORGANIZATION_CODE, invoice.getBilledByOrganizationCode());
        qualification.put(ArKimAttributes.PROCESSING_CHART_OF_ACCOUNTS_CODE, invoice.getAccountsReceivableDocumentHeader().getProcessingChartOfAccountCode());
        qualification.put(ArKimAttributes.PROCESSING_ORGANIZATION_CODE, invoice.getAccountsReceivableDocumentHeader().getProcessingOrganizationCode());

        String customerName = invoice.getCustomerName();
        if (!StringUtils.isBlank(customerName)) {
            qualification.put(ArKimAttributes.CUSTOMER_NAME, customerName);
        }

        return getPermissionService().isAuthorized(collectorPrincipalId, ArConstants.AR_NAMESPACE_CODE, ArAuthorizationConstants.VIEW_CONTRACTS_GRANTS_INVOICE_IN_BILLING_REPORTS_PERMISSION, qualification);
    }

    /**
     * This method generates the attached invoices for the agency addresses in the Contracts & Grants Invoice Document.
     */
    @Override
    public void generateInvoicesForInvoiceAddresses(ContractsGrantsInvoiceDocument document) {
        InvoiceTemplate invoiceTemplate = null;
        Iterator<InvoiceAddressDetail> iterator = document.getInvoiceAddressDetails().iterator();
        while (iterator.hasNext()) {
            InvoiceAddressDetail invoiceAddressDetail = iterator.next();
            byte[] reportStream;
            byte[] copyReportStream;
            // validating the invoice template
            if (ObjectUtils.isNotNull(invoiceAddressDetail.getCustomerInvoiceTemplateCode())) {
                invoiceTemplate = businessObjectService.findBySinglePrimaryKey(InvoiceTemplate.class, invoiceAddressDetail.getCustomerInvoiceTemplateCode());

                // generate invoices from templates.
                if (ObjectUtils.isNotNull(invoiceTemplate) && invoiceTemplate.isActive() && StringUtils.isNotBlank(invoiceTemplate.getFilename())) {
                    ModuleConfiguration systemConfiguration = kualiModuleService.getModuleServiceByNamespaceCode(KFSConstants.OptionalModuleNamespaces.ACCOUNTS_RECEIVABLE).getModuleConfiguration();
                    String templateFolderPath = ((FinancialSystemModuleConfiguration) systemConfiguration).getTemplateFileDirectories().get(KFSConstants.TEMPLATES_DIRECTORY_KEY);
                    String templateFilePath = templateFolderPath + File.separator + invoiceTemplate.getFilename();
                    File templateFile = new File(templateFilePath);
                    File outputDirectory = null;
                    String outputFileName;
                    try {
                        // generating original invoice
                        outputFileName = document.getDocumentNumber() + "_" + invoiceAddressDetail.getCustomerAddressName() + getDateTimeService().toDateStringForFilename(getDateTimeService().getCurrentDate()) + ArConstants.TemplateUploadSystem.EXTENSION;
                        Map<String, String> replacementList = getTemplateParameterList(document);
                        replacementList.put(ArPropertyConstants.CustomerInvoiceDocumentFields.CUSTOMER+"."+ArPropertyConstants.FULL_ADDRESS, contractsGrantsBillingUtilityService.buildFullAddress(invoiceAddressDetail.getCustomerAddress()));
                        reportStream = PdfFormFillerUtil.populateTemplate(templateFile, replacementList);
                        // creating and saving the original note with an attachment
                        if (ObjectUtils.isNotNull(document.getInvoiceGeneralDetail()) && document.getInvoiceGeneralDetail().isFinalBillIndicator()) {
                            reportStream = PdfFormFillerUtil.createFinalmarkOnFile(reportStream, getConfigurationService().getPropertyValueAsString(ArKeyConstants.INVOICE_ADDRESS_PDF_WATERMARK_FINAL));
                        }
                        Note note = new Note();
                        note.setNotePostedTimestampToCurrent();
                        final String finalNotePattern = getConfigurationService().getPropertyValueAsString(ArKeyConstants.INVOICE_ADDRESS_PDF_FINAL_NOTE);
                        note.setNoteText(MessageFormat.format(finalNotePattern, document.getDocumentNumber(), invoiceAddressDetail.getCustomerAddressName()));
                        note.setNoteTypeCode(KFSConstants.NoteTypeEnum.BUSINESS_OBJECT_NOTE_TYPE.getCode());
                        Person systemUser = personService.getPersonByPrincipalName(KFSConstants.SYSTEM_USER);
                        note = noteService.createNote(note, document.getNoteTarget(), systemUser.getPrincipalId());
                        Attachment attachment = attachmentService.createAttachment(note, outputFileName, ArConstants.TemplateUploadSystem.TEMPLATE_MIME_TYPE, reportStream.length, new ByteArrayInputStream(reportStream), KFSConstants.EMPTY_STRING);
                        // adding attachment to the note
                        note.setAttachment(attachment);
                        noteService.save(note);
                        attachment.setNoteIdentifier(note.getNoteIdentifier());
                        businessObjectService.save(attachment);
                        document.addNote(note);

                        // generating Copy invoice
                        outputFileName = document.getDocumentNumber() + "_" + invoiceAddressDetail.getCustomerAddressName() + getDateTimeService().toDateStringForFilename(getDateTimeService().getCurrentDate()) + getConfigurationService().getPropertyValueAsString(ArKeyConstants.INVOICE_ADDRESS_PDF_COPY_FILENAME_SUFFIX) + ArConstants.TemplateUploadSystem.EXTENSION;
                        copyReportStream = PdfFormFillerUtil.createWatermarkOnFile(reportStream, getConfigurationService().getPropertyValueAsString(ArKeyConstants.INVOICE_ADDRESS_PDF_WATERMARK_COPY));
                        // creating and saving the copy note with an attachment
                        Note copyNote = new Note();
                        copyNote.setNotePostedTimestampToCurrent();
                        final String copyNotePattern = getConfigurationService().getPropertyValueAsString(ArKeyConstants.INVOICE_ADDRESS_PDF_COPY_NOTE);
                        copyNote.setNoteText(MessageFormat.format(copyNotePattern, document.getDocumentNumber(), invoiceAddressDetail.getCustomerAddressName()));
                        copyNote.setNoteTypeCode(KFSConstants.NoteTypeEnum.BUSINESS_OBJECT_NOTE_TYPE.getCode());
                        copyNote = noteService.createNote(copyNote, document.getNoteTarget(), systemUser.getPrincipalId());
                        Attachment copyAttachment = attachmentService.createAttachment(copyNote, outputFileName, ArConstants.TemplateUploadSystem.TEMPLATE_MIME_TYPE, copyReportStream.length, new ByteArrayInputStream(copyReportStream), KFSConstants.EMPTY_STRING);
                        // adding attachment to the note
                        copyNote.setAttachment(copyAttachment);
                        noteService.save(copyNote);
                        copyAttachment.setNoteIdentifier(copyNote.getNoteIdentifier());
                        businessObjectService.save(copyAttachment);
                        document.addNote(copyNote);
                        invoiceAddressDetail.setNoteId(note.getNoteIdentifier());
                        // saving the note to the document header
                        documentService.updateDocument(document);
                    } catch (IOException | DocumentException ex) {
                        addNoteForInvoiceReportFail(document);
                    }
                } else {
                    addNoteForInvoiceReportFail(document);
                }
            } else {
                addNoteForInvoiceReportFail(document);
            }

        }
    }

    /**
     * This method generated the template parameter list to populate the pdf invoices that are attached to the Document.
     * The evident goal of this method was to return practically any possible property from the given document into a Map which could be stamped on any invoice PDF.  Given that, we've done some strange
     * tricks with Map implementations.  First, we wrap the document in a ReflectionMap, which means that all nested properties from the document can be read via Map notation.  We still have a number of properties
     * we want to add, though, for instance for the payee and the award.  So we wrap ReflectionMap in a FallbackMap, which will treat a regular HashMap and the ReflectionMap as if they were one Map for the sake of
     * getting at least.  Finally, we wrap the map of all properties into a PdfFormattingMap, which formats any values to be returned through get() into properly formatted Strings.
     * @param document the ContractsGrantsInvoiceDocument to convert into a Map form
     * @return a Map.  With everything.
     */
    protected Map<String, String> getTemplateParameterList(ContractsGrantsInvoiceDocument document) {
        ContractsAndGrantsBillingAward award = document.getInvoiceGeneralDetail().getAward();

        Map cinvDocMap = new ReflectionMap(document);
        Map<String, Object> parameterMap = new FallbackMap<String, Object>(cinvDocMap);

        Map<String, Object> primaryKeys = new HashMap<>();
        primaryKeys.put(KFSPropertyConstants.UNIVERSITY_FISCAL_YEAR, document.getAccountingPeriod().getUniversityFiscalYear());
        primaryKeys.put(KFSPropertyConstants.PROCESSING_CHART_OF_ACCT_CD, document.getAccountsReceivableDocumentHeader().getProcessingChartOfAccountCode());
        primaryKeys.put(KFSPropertyConstants.PROCESSING_ORGANIZATION_CODE, document.getAccountsReceivableDocumentHeader().getProcessingOrganizationCode());
        SystemInformation sysInfo = businessObjectService.findByPrimaryKey(SystemInformation.class, primaryKeys);
        parameterMap.put(KFSPropertyConstants.DOCUMENT_NUMBER, document.getDocumentNumber());
        if (ObjectUtils.isNotNull(document.getDocumentHeader().getWorkflowDocument().getDateCreated())) {
            parameterMap.put(KFSPropertyConstants.DATE, getDateTimeService().toDateString(document.getDocumentHeader().getWorkflowDocument().getDateCreated().toDate()));
        }
        if (ObjectUtils.isNotNull(document.getDocumentHeader().getWorkflowDocument().getDateFinalized())) {
            parameterMap.put(ArPropertyConstants.FINAL_STATUS_DATE, getDateTimeService().toDateString(document.getDocumentHeader().getWorkflowDocument().getDateFinalized().toDate()));
        }
        parameterMap.put(KFSPropertyConstants.PROPOSAL_NUMBER, document.getInvoiceGeneralDetail().getProposalNumber());
        parameterMap.put(KFSPropertyConstants.PAYEE+"."+KFSPropertyConstants.NAME, document.getBillingAddressName());
        parameterMap.put(KFSPropertyConstants.PAYEE+"."+KFSPropertyConstants.ADDRESS_LINE1, document.getBillingLine1StreetAddress());
        parameterMap.put(KFSPropertyConstants.PAYEE+"."+KFSPropertyConstants.ADDRESS_LINE2, document.getBillingLine2StreetAddress());
        parameterMap.put(KFSPropertyConstants.PAYEE+"."+KFSPropertyConstants.CITY, document.getBillingCityName());
        parameterMap.put(KFSPropertyConstants.PAYEE+"."+KFSPropertyConstants.STATE, document.getBillingStateCode());
        parameterMap.put(KFSPropertyConstants.PAYEE+"."+KFSPropertyConstants.ZIPCODE, document.getBillingZipCode());
        parameterMap.put(ArPropertyConstants.ADVANCE_FLAG, ArConstants.PREDETERMINED_BILLING_SCHEDULE_CODE.equals(document.getInvoiceGeneralDetail().getBillingFrequencyCode()));
        parameterMap.put(ArPropertyConstants.REIMBURSEMENT_FLAG, !ArConstants.PREDETERMINED_BILLING_SCHEDULE_CODE.equals(document.getInvoiceGeneralDetail().getBillingFrequencyCode()));
        parameterMap.put(ArPropertyConstants.ACCOUNT_DETAILS+"."+KFSPropertyConstants.CONTRACT_CONTROL_ACCOUNT_NUMBER, getRecipientAccountNumber(document.getAccountDetails()));
        if (ObjectUtils.isNotNull(sysInfo)) {
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+ArPropertyConstants.SystemInformationFields.FEIN_NUMBER, sysInfo.getUniversityFederalEmployerIdentificationNumber());
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+KFSPropertyConstants.NAME, sysInfo.getOrganizationRemitToAddressName());
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+KFSPropertyConstants.ADDRESS_LINE1, sysInfo.getOrganizationRemitToLine1StreetAddress());
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+KFSPropertyConstants.ADDRESS_LINE2, sysInfo.getOrganizationRemitToLine2StreetAddress());
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+KFSPropertyConstants.CITY, sysInfo.getOrganizationRemitToCityName());
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+KFSPropertyConstants.STATE, sysInfo.getOrganizationRemitToStateCode());
            parameterMap.put(ArPropertyConstants.SYSTEM_INFORMATION+"."+KFSPropertyConstants.ZIPCODE, sysInfo.getOrganizationRemitToZipCode());
        }
        if (CollectionUtils.isNotEmpty(document.getDirectCostInvoiceDetails())) {
            ContractsGrantsInvoiceDetail firstInvoiceDetail = document.getDirectCostInvoiceDetails().get(0);

            for (int i = 0; i < document.getDirectCostInvoiceDetails().size(); i++) {
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.INVOICE_DETAIL_IDENTIFIER, document.getDirectCostInvoiceDetails().get(i).getInvoiceDetailIdentifier());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+KFSPropertyConstants.DOCUMENT_NUMBER, document.getDirectCostInvoiceDetails().get(i).getDocumentNumber());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.CATEGORY, document.getDirectCostInvoiceDetails().get(i).getCostCategory().getCategoryName());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.TOTAL_BUDGET, document.getDirectCostInvoiceDetails().get(i).getTotalBudget());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.INVOICE_AMOUNT, document.getDirectCostInvoiceDetails().get(i).getInvoiceAmount());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.CUMULATIVE_EXPENDITURES, document.getDirectCostInvoiceDetails().get(i).getCumulativeExpenditures());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.BUDGET_REMAINING, document.getDirectCostInvoiceDetails().get(i).getBudgetRemaining());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.TOTAL_PREVIOUSLY_BILLED, document.getDirectCostInvoiceDetails().get(i).getTotalPreviouslyBilled());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.TOTAL_AMOUNT_BILLED_TO_DATE, document.getDirectCostInvoiceDetails().get(i).getTotalAmountBilledToDate());
                parameterMap.put(ArPropertyConstants.INVOICE_DETAIL+"[" + i + "]."+ArPropertyConstants.AMOUNT_REMAINING_TO_BILL, firstInvoiceDetail.getAmountRemainingToBill());
            }
        }
        ContractsGrantsInvoiceDetail totalDirectCostInvoiceDetail = document.getTotalDirectCostInvoiceDetail();
        if (ObjectUtils.isNotNull(totalDirectCostInvoiceDetail)) {
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.INVOICE_DETAIL_IDENTIFIER, totalDirectCostInvoiceDetail.getInvoiceDetailIdentifier());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+KFSPropertyConstants.DOCUMENT_NUMBER, totalDirectCostInvoiceDetail.getDocumentNumber());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.CATEGORY, getConfigurationService().getPropertyValueAsString(ArKeyConstants.CONTRACTS_GRANTS_INVOICE_DETAILS_DIRECT_SUBTOTAL_LABEL));
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_BUDGET, totalDirectCostInvoiceDetail.getTotalBudget());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.INVOICE_AMOUNT, totalDirectCostInvoiceDetail.getInvoiceAmount());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.CUMULATIVE_EXPENDITURES, totalDirectCostInvoiceDetail.getCumulativeExpenditures());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.BUDGET_REMAINING, totalDirectCostInvoiceDetail.getBudgetRemaining());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_PREVIOUSLY_BILLED, totalDirectCostInvoiceDetail.getTotalPreviouslyBilled());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_AMOUNT_BILLED_TO_DATE, totalDirectCostInvoiceDetail.getTotalAmountBilledToDate());
            parameterMap.put(ArPropertyConstants.DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.AMOUNT_REMAINING_TO_BILL, totalDirectCostInvoiceDetail.getAmountRemainingToBill());
        }
        ContractsGrantsInvoiceDetail totalInDirectCostInvoiceDetail = document.getTotalIndirectCostInvoiceDetail();
        if (ObjectUtils.isNotNull(totalInDirectCostInvoiceDetail)) {
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.INVOICE_DETAIL_IDENTIFIER, totalInDirectCostInvoiceDetail.getInvoiceDetailIdentifier());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+KFSPropertyConstants.DOCUMENT_NUMBER, totalInDirectCostInvoiceDetail.getDocumentNumber());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.CATEGORIES, getConfigurationService().getPropertyValueAsString(ArKeyConstants.CONTRACTS_GRANTS_INVOICE_DETAILS_INDIRECT_SUBTOTAL_LABEL));
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_BUDGET, totalInDirectCostInvoiceDetail.getTotalBudget());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.INVOICE_AMOUNT, totalInDirectCostInvoiceDetail.getInvoiceAmount());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.CUMULATIVE_EXPENDITURES, totalInDirectCostInvoiceDetail.getCumulativeExpenditures());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.BUDGET_REMAINING, totalInDirectCostInvoiceDetail.getBudgetRemaining());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_PREVIOUSLY_BILLED, totalInDirectCostInvoiceDetail.getTotalPreviouslyBilled());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_AMOUNT_BILLED_TO_DATE, totalInDirectCostInvoiceDetail.getTotalAmountBilledToDate());
            parameterMap.put(ArPropertyConstants.IN_DIRECT_COST_INVOICE_DETAIL+"."+ArPropertyConstants.AMOUNT_REMAINING_TO_BILL, totalInDirectCostInvoiceDetail.getAmountRemainingToBill());
        }
        ContractsGrantsInvoiceDetail totalCostInvoiceDetail = document.getTotalCostInvoiceDetail();
        if (ObjectUtils.isNotNull(totalCostInvoiceDetail)) {
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.INVOICE_DETAIL_IDENTIFIER, totalCostInvoiceDetail.getInvoiceDetailIdentifier());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+KFSPropertyConstants.DOCUMENT_NUMBER, totalCostInvoiceDetail.getDocumentNumber());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.CATEGORIES, getConfigurationService().getPropertyValueAsString(ArKeyConstants.CONTRACTS_GRANTS_INVOICE_DETAILS_TOTAL_LABEL));
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_BUDGET, totalCostInvoiceDetail.getTotalBudget());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.INVOICE_AMOUNT, totalCostInvoiceDetail.getInvoiceAmount());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.CUMULATIVE_EXPENDITURES, totalCostInvoiceDetail.getCumulativeExpenditures());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.BUDGET_REMAINING, totalCostInvoiceDetail.getBudgetRemaining());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_PREVIOUSLY_BILLED, totalCostInvoiceDetail.getTotalPreviouslyBilled());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.ESTIMATED_COST, totalCostInvoiceDetail.getTotalPreviouslyBilled().add(totalCostInvoiceDetail.getInvoiceAmount()));
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.TOTAL_AMOUNT_BILLED_TO_DATE, totalCostInvoiceDetail.getTotalAmountBilledToDate());
            parameterMap.put(ArPropertyConstants.TOTAL_INVOICE_DETAIL+"."+ArPropertyConstants.AMOUNT_REMAINING_TO_BILL, totalCostInvoiceDetail.getAmountRemainingToBill());
        }

        if (ObjectUtils.isNotNull(award)) {
            KualiDecimal billing = getAwardBilledToDateAmountByProposalNumber(award.getProposalNumber());
            KualiDecimal payments = calculateTotalPaymentsToDateByAward(award);
            KualiDecimal receivable = billing.subtract(payments);
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.BILLINGS, billing);
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.PAYMENTS, payments);
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.RECEIVABLES, receivable);
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.PROPOSAL_NUMBER, award.getProposalNumber());
            if (ObjectUtils.isNotNull(award.getAwardBeginningDate())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_BEGINNING_DATE, getDateTimeService().toDateString(award.getAwardBeginningDate()));
            }
            if (ObjectUtils.isNotNull(award.getAwardEndingDate())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_ENDING_DATE, getDateTimeService().toDateString(award.getAwardEndingDate()));
            }
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_TOTAL_AMOUNT, award.getAwardTotalAmount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_ADDENDUM_NUMBER, award.getAwardAddendumNumber());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_ALLOCATED_UNIVERSITY_COMPUTING_SERVICES_AMOUNT, award.getAwardAllocatedUniversityComputingServicesAmount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.FEDERAL_PASS_THROUGH_FUNDED_AMOUNT, award.getFederalPassThroughFundedAmount());
            if (ObjectUtils.isNotNull(award.getAwardEntryDate())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_ENTRY_DATE, getDateTimeService().toDateString(award.getAwardEntryDate()));
            }
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AGENCY_FUTURE_1_AMOUNT, award.getAgencyFuture1Amount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AGENCY_FUTURE_2_AMOUNT, award.getAgencyFuture2Amount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AGENCY_FUTURE_3_AMOUNT, award.getAgencyFuture3Amount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_DOCUMENT_NUMBER, award.getAwardDocumentNumber());
            if (ObjectUtils.isNotNull(award.getAwardLastUpdateDate())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_LAST_UPDATE_DATE, getDateTimeService().toDateString(award.getAwardLastUpdateDate()));
            }
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.FEDERAL_PASS_THROUGH_INDICATOR, award.getFederalPassThroughIndicator());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.OLD_PROPOSAL_NUMBER, award.getOldProposalNumber());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_DIRECT_COST_AMOUNT, award.getAwardDirectCostAmount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_INDIRECT_COST_AMOUNT, award.getAwardIndirectCostAmount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.FEDERAL_FUNDED_AMOUNT, award.getFederalFundedAmount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_CREATE_TIMESTAMP, award.getAwardCreateTimestamp());
            if (ObjectUtils.isNotNull(award.getAwardClosingDate())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_CLOSING_DATE, getDateTimeService().toDateString(award.getAwardClosingDate()));
            }
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.PROPOSAL_AWARD_TYPE_CODE, award.getProposalAwardTypeCode());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_STATUS_CODE, award.getAwardStatusCode());
            if (ObjectUtils.isNotNull(award.getLetterOfCreditFund())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.LETTER_OF_CREDIT_FUND_GROUP_CODE, award.getLetterOfCreditFund().getLetterOfCreditFundGroupCode());
            }
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.LETTER_OF_CREDIT_FUND_CODE, award.getLetterOfCreditFundCode());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.GRANT_DESCRIPTION_CODE, award.getGrantDescriptionCode());
            if (ObjectUtils.isNotNull(award.getProposal())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.GRANT_NUMBER, award.getProposal().getGrantNumber());
            }
            parameterMap.put(KFSPropertyConstants.AGENCY_NUMBER, award.getAgencyNumber());
            parameterMap.put(KFSPropertyConstants.AGENCY+"."+KFSPropertyConstants.FULL_NAME, award.getAgency().getFullName());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.FEDERAL_PASS_THROUGH_AGENCY_NUMBER, award.getFederalPassThroughAgencyNumber());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AGENCY_ANALYST_NAME, award.getAgencyAnalystName());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.ANALYST_TELEPHONE_NUMBER, award.getAnalystTelephoneNumber());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.BILLING_FREQUENCY_CODE, award.getBillingFrequencyCode());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.AWARD_PROJECT_TITLE, award.getAwardProjectTitle());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AWARD_PURPOSE_CODE, award.getAwardPurposeCode());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.ACTIVE, award.isActive());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.KIM_GROUP_NAMES, award.getKimGroupNames());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.ROUTING_ORG, award.getRoutingOrg());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.ROUTING_CHART, award.getRoutingChart());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.EXCLUDED_FROM_INVOICING, award.isExcludedFromInvoicing());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.ADDITIONAL_FORMS_REQUIRED, award.isAdditionalFormsRequiredIndicator());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.ADDITIONAL_FORMS_DESCRIPTION, award.getAdditionalFormsDescription());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.INSTRUMENT_TYPE_CODE, award.getInstrumentTypeCode());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.MIN_INVOICE_AMOUNT, award.getMinInvoiceAmount());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.AUTO_APPROVE, award.getAutoApproveIndicator());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.LOOKUP_PERSON_UNIVERSAL_IDENTIFIER, award.getLookupPersonUniversalIdentifier());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.LOOKUP_PERSON, award.getLookupPerson().getPrincipalName());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.USER_LOOKUP_ROLE_NAMESPACE_CODE, award.getUserLookupRoleNamespaceCode());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.USER_LOOKUP_ROLE_NAME, award.getUserLookupRoleName());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.FUNDING_EXPIRATION_DATE, award.getFundingExpirationDate());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.STOP_WORK_INDICATOR, award.isStopWorkIndicator());
            parameterMap.put(KFSPropertyConstants.AWARD+"."+KFSPropertyConstants.STOP_WORK_REASON, award.getStopWorkReason());
            if (ObjectUtils.isNotNull(award.getAwardPrimaryProjectDirector())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArConstants.AWARD_PROJECT_DIRECTOR+"."+KFSPropertyConstants.NAME, award.getAwardPrimaryProjectDirector().getProjectDirector().getName());
            }
            parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.LETTER_OF_CREDIT_FUND_CODE, award.getLetterOfCreditFundCode());
            if (ObjectUtils.isNotNull(award.getAwardPrimaryFundManager())) {
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.PRIMARY_FUND_MANAGER+"."+KFSPropertyConstants.NAME, award.getAwardPrimaryFundManager().getFundManager().getName());
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.PRIMARY_FUND_MANAGER+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.EMAIL, award.getAwardPrimaryFundManager().getFundManager().getEmailAddress());
                parameterMap.put(KFSPropertyConstants.AWARD+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.PRIMARY_FUND_MANAGER+"."+ArPropertyConstants.ContractsAndGrantsBillingAwardFields.PHONE, award.getAwardPrimaryFundManager().getFundManager().getPhoneNumber());
            }
            if (ObjectUtils.isNotNull(document.getInvoiceGeneralDetail())) {
                parameterMap.put(ArPropertyConstants.TOTAL_AMOUNT_DUE, receivable.add(document.getInvoiceGeneralDetail().getTotalAmountBilledToDate()));
            }
        }
        return new PdfFormattingMap(parameterMap);
    }

    /**
     * returns proper contract control Account Number.
     *
     * @return
     */
    protected String getRecipientAccountNumber(List<InvoiceAccountDetail> accountDetails) {
        if (CollectionUtils.isNotEmpty(accountDetails)) {
            if (ObjectUtils.isNull(accountDetails.get(0).getContractControlAccountNumber())) {
                return accountDetails.get(0).getAccountNumber();
            }
            return accountDetails.get(0).getContractControlAccountNumber();
        }
        return null;
    }

    /**
     * This method sets the last billed date to Award and Award Account objects based on the status of the invoice. Final or
     * Corrected.
     *
     * @param document
     */
    @Override
    public void updateLastBilledDate(ContractsGrantsInvoiceDocument document) {
        boolean isFinalBill = document.getInvoiceGeneralDetail().isFinalBillIndicator();

        // To calculate and update Last Billed Date based on the status of the invoice. Final or Corrected.
        // 1. Set last Billed Date to Award Accounts

        Iterator<InvoiceAccountDetail> iterator = document.getAccountDetails().iterator();
        while (iterator.hasNext()) {
            InvoiceAccountDetail id = iterator.next();
            if (isFinalBill) {
                setAwardAccountFinalBilledValueAndLastBilledDate(id, true, document.getInvoiceGeneralDetail().getProposalNumber(), document.isInvoiceReversal(), document.getInvoiceGeneralDetail().getLastBilledDate());
            } else {
                calculateAwardAccountLastBilledDate(id, document.isInvoiceReversal(), document.getInvoiceGeneralDetail().getLastBilledDate(), document.getInvoiceGeneralDetail().getProposalNumber());
            }
        }

        // 2. Set last Billed to Award = least of last billed date of award account.
        Long proposalNumber = document.getInvoiceGeneralDetail().getProposalNumber();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);
        ContractsAndGrantsBillingAward award = kualiModuleService.getResponsibleModuleService(ContractsAndGrantsBillingAward.class).getExternalizableBusinessObject(ContractsAndGrantsBillingAward.class, map);

        if (CollectionUtils.isNotEmpty(award.getActiveAwardAccounts())) {
            // To set last billed Date to award.
            contractsAndGrantsModuleBillingService.setLastBilledDateToAward(proposalNumber, getLastBilledDate(award));
        }

    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getLastBilledDate(java.lang.Long)
     */
    @Override
    public java.sql.Date getLastBilledDate(ContractsAndGrantsBillingAward award) {
        java.sql.Date awdLastBilledDate = null;

        if (ObjectUtils.isNotNull(award)) {
            // last Billed of Award = least of last billed date of award account.
            ContractsAndGrantsBillingAwardAccount awardAccount;

            if (CollectionUtils.isNotEmpty(award.getActiveAwardAccounts())) {
                ContractsAndGrantsBillingAwardAccount firstActiveawardAccount = award.getActiveAwardAccounts().get(0);

                awardAccount = firstActiveawardAccount;
                awdLastBilledDate = firstActiveawardAccount.getCurrentLastBilledDate();

                for (int i = 0; i < award.getActiveAwardAccounts().size(); i++) {
                    if (ObjectUtils.isNull(awdLastBilledDate) || ObjectUtils.isNull(award.getActiveAwardAccounts().get(i).getCurrentLastBilledDate())) {
                        // The dates would be null if the user is correcting the first invoice created for the award.
                        // Then the award last billed date should also be null.
                        awdLastBilledDate = null;
                    }
                    else if (ObjectUtils.isNotNull(awdLastBilledDate) && ObjectUtils.isNotNull(award.getActiveAwardAccounts().get(i).getCurrentLastBilledDate())) {
                        if (awdLastBilledDate.after(award.getActiveAwardAccounts().get(i).getCurrentLastBilledDate())) {
                            awdLastBilledDate = award.getActiveAwardAccounts().get(i).getCurrentLastBilledDate();
                        }
                    }
                }
            }
        }

        return awdLastBilledDate;
    }

    /**
     * This method updates the AwardAccount object's last billed Variable with the value provided
     *
     * @param id
     * @param invoiceStatus
     * @param lastBilledDate
     * @param proposalNumber
     */
    protected void calculateAwardAccountLastBilledDate(InvoiceAccountDetail id, boolean invoiceReversal, java.sql.Date lastBilledDate, Long proposalNumber) {
        Map<String, Object> mapKey = new HashMap<String, Object>();
        mapKey.put(KFSPropertyConstants.ACCOUNT_NUMBER, id.getAccountNumber());
        mapKey.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, id.getChartOfAccountsCode());
        mapKey.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);
        // To set previous and current last Billed Date for award account .
        contractsAndGrantsModuleBillingService.setLastBilledDateToAwardAccount(mapKey, invoiceReversal, lastBilledDate);

    }

    /**
     * This method updates the Bills and Milestone objects billed Field.
     *
     * @param billed
     */
    @Override
    public void updateBillsAndMilestones(boolean billed, List<InvoiceMilestone> invoiceMilestones, List<InvoiceBill> invoiceBills) {
        updateMilestonesBilledIndicator(billed, invoiceMilestones);
        updateBillsBilledIndicator(billed, invoiceBills);
    }

    /**
     * Update Milestone objects billed value.
     * @param billed
     * @param invoiceMilestones
     */
    @Override
    public void updateMilestonesBilledIndicator(boolean billed, List<InvoiceMilestone> invoiceMilestones) {
        if (CollectionUtils.isNotEmpty(invoiceMilestones)) {
            List<Long> milestoneIds = new ArrayList<Long>();
            for (InvoiceMilestone invoiceMilestone : invoiceMilestones) {
                milestoneIds.add(invoiceMilestone.getMilestoneIdentifier());
            }

            if (CollectionUtils.isNotEmpty(milestoneIds)) {
                setMilestonesBilled(milestoneIds, billed);
            }
        }
    }

    /**
     * This method updates value of billed in Milestone BO to the value of the billed parameter
     *
     * @param proposalNumber
     * @param milestoneIds
     * @param billed
     */
    protected void setMilestonesBilled(List<Long> milestoneIds, boolean billed) {
        List<Milestone> milestones = null;
        Map<String, Object> fieldValues = new HashMap<String, Object>();
        fieldValues.put(ArPropertyConstants.MilestoneFields.MILESTONE_IDENTIFIER, milestoneIds);
        milestones = (List<Milestone>)getBusinessObjectService().findMatching(Milestone.class, fieldValues);

        if (ObjectUtils.isNotNull(milestones)) {
            for (Milestone milestone : milestones) {
                milestone.setBilled(billed);
            }
            getBusinessObjectService().save(milestones);
        }
    }

    /**
     * Update Bill objects billed value.
     * @param billed
     * @param invoiceBills
     */
    @Override
    public void updateBillsBilledIndicator(boolean billed, List<InvoiceBill> invoiceBills) {
        if (CollectionUtils.isNotEmpty(invoiceBills)) {
            List<Long> billIds = new ArrayList<Long>();
            for (InvoiceBill invoiceBill : invoiceBills) {
                billIds.add(invoiceBill.getBillIdentifier());
            }

            if (CollectionUtils.isNotEmpty(invoiceBills)) {
                setBillsBilled(billIds, billed);
            }
        }
    }

    /**
     * This method updates value of billed in Bill BO to billed
     *
     * @param fieldValuesList
     * @param billed
     */
    protected void setBillsBilled(List<Long> billIds, boolean billed) {
        List<Bill> bills = null;
        Map<String, Object> fieldValues = new HashMap<String, Object>();
        fieldValues.put(ArPropertyConstants.BillFields.BILL_IDENTIFIER, billIds);
        bills = (List<Bill>)getBusinessObjectService().findMatching(Bill.class, fieldValues);

        if (ObjectUtils.isNotNull(bills)) {
            for (Bill bill : bills) {
                bill.setBilled(billed);
            }
            getBusinessObjectService().save(bills);
        }
    }


    /**
     * This method updates the ContractsAndGrantsBillingAwardAccount object's FinalBilled Variable with the value provided
     *
     * @param id
     * @param value
     * @param proposalNumber
     */
    protected void setAwardAccountFinalBilledValue(InvoiceAccountDetail id, boolean value, Long proposalNumber) {
        Map<String, Object> mapKey = new HashMap<String, Object>();
        mapKey.put(KFSPropertyConstants.ACCOUNT_NUMBER, id.getAccountNumber());
        mapKey.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, id.getChartOfAccountsCode());
        mapKey.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);

        // To set final Billed to award Account
        contractsAndGrantsModuleBillingService.setFinalBilledToAwardAccount(mapKey, value);
    }

    /**
     * This method updates the ContractsAndGrantsBillingAwardAccount object's FinalBilled Variable with the value provided
     * and also sets the last billed date and invoice status.
     *
     * @param id
     * @param finalBilled
     * @param proposalNumber
     * @param invoiceStatus
     * @param lastBilledDate
     */
    protected void setAwardAccountFinalBilledValueAndLastBilledDate(InvoiceAccountDetail id, boolean finalBilled, Long proposalNumber, boolean invoiceReversal, java.sql.Date lastBilledDate) {
        Map<String, Object> mapKey = new HashMap<String, Object>();
        mapKey.put(KFSPropertyConstants.ACCOUNT_NUMBER, id.getAccountNumber());
        mapKey.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, id.getChartOfAccountsCode());
        mapKey.put(KFSPropertyConstants.PROPOSAL_NUMBER, proposalNumber);

        // To set previous and current last Billed Date for award account .
        contractsAndGrantsModuleBillingService.setFinalBilledAndLastBilledDateToAwardAccount(mapKey, finalBilled, invoiceReversal, lastBilledDate);
    }

    /**
     * This method updates AwardAccounts
     */
    @Override
    public void updateUnfinalizationToAwardAccount(List<InvoiceAccountDetail> accountDetails, Long proposalNumber) {
        Iterator iterator = accountDetails.iterator();
        while (iterator.hasNext()) {
            InvoiceAccountDetail id = (InvoiceAccountDetail) iterator.next();
            setAwardAccountFinalBilledValue(id, false, proposalNumber);
        }
    }

    /**
     * Corrects the Contracts & Grants Invoice Document.
     *
     * @throws WorkflowException
     */
    @Override
    public void correctContractsGrantsInvoiceDocument(ContractsGrantsInvoiceDocument document) throws WorkflowException {
        // correct Direct Cost Invoice Details.
        for (ContractsGrantsInvoiceDetail id : document.getDirectCostInvoiceDetails()) {
            correctInvoiceDetail(id);
        }

        // correct Indirect Cost Invoice Details.
        for (ContractsGrantsInvoiceDetail id : document.getIndirectCostInvoiceDetails()) {
            correctInvoiceDetail(id);
        }

        // update correction to the InvoiceAccountDetail objects
        for (InvoiceAccountDetail id : document.getAccountDetails()) {
            correctInvoiceAccountDetail(id);
        }

        // correct invoiceDetailAccountObjectCode.
        for (InvoiceDetailAccountObjectCode invoiceDetailAccountObjectCode : document.getInvoiceDetailAccountObjectCodes()) {
            invoiceDetailAccountObjectCode.correctInvoiceDetailAccountObjectCodeExpenditureAmount();
        }

        // correct Bills
        KualiDecimal totalBillingAmount = KualiDecimal.ZERO;
        for (InvoiceBill bill : document.getInvoiceBills()) {
            bill.setEstimatedAmount(bill.getEstimatedAmount().negated());
            totalBillingAmount = totalBillingAmount.add(bill.getEstimatedAmount());
        }

        // correct Milestones
        KualiDecimal totalMilestonesAmount = KualiDecimal.ZERO;
        for (InvoiceMilestone milestone : document.getInvoiceMilestones()) {
            milestone.setMilestoneAmount(milestone.getMilestoneAmount().negated());
            totalMilestonesAmount = totalMilestonesAmount.add(milestone.getMilestoneAmount());
        }

        // set the billed to Date Field
        if (document.getInvoiceGeneralDetail().getBillingFrequencyCode().equalsIgnoreCase(ArConstants.MILESTONE_BILLING_SCHEDULE_CODE) && CollectionUtils.isNotEmpty(document.getInvoiceMilestones())) {
            // check if award has milestones
            document.getInvoiceGeneralDetail().setTotalPreviouslyBilled(getMilestonesBilledToDateAmount(document.getInvoiceGeneralDetail().getProposalNumber()));
            // update the new total billed for the invoice.
            document.getInvoiceGeneralDetail().setTotalAmountBilledToDate(document.getInvoiceGeneralDetail().getTotalAmountBilledToDate().add(totalMilestonesAmount));
        }
        else if (document.getInvoiceGeneralDetail().getBillingFrequencyCode().equalsIgnoreCase(ArConstants.PREDETERMINED_BILLING_SCHEDULE_CODE) && CollectionUtils.isNotEmpty(document.getInvoiceBills())) {
            // check if award has bills
            document.getInvoiceGeneralDetail().setTotalPreviouslyBilled(getPredeterminedBillingBilledToDateAmount(document.getInvoiceGeneralDetail().getProposalNumber()));
            // update the new total billed for the invoice.
            document.getInvoiceGeneralDetail().setTotalAmountBilledToDate(document.getInvoiceGeneralDetail().getTotalAmountBilledToDate().add(totalBillingAmount));
        }
        else {
            document.getInvoiceGeneralDetail().setTotalPreviouslyBilled(getAwardBilledToDateAmountByProposalNumber(document.getInvoiceGeneralDetail().getProposalNumber()));
            KualiDecimal newTotalBilled = document.getTotalCostInvoiceDetail().getInvoiceAmount().add(document.getInvoiceGeneralDetail().getTotalPreviouslyBilled());
            newTotalBilled = newTotalBilled.add(getOtherTotalBilledForAwardPeriod(document));
            document.getInvoiceGeneralDetail().setTotalAmountBilledToDate(newTotalBilled);
        }

        for (InvoiceAddressDetail invoiceAddressDetail: document.getInvoiceAddressDetails()) {
            invoiceAddressDetail.setInitialTransmissionDate(null);
        }
    }

    /**
     * Error corrects an invoice detail
     * @param invoiceDetail the invoice detail to error correct
     */
    protected void correctInvoiceDetail(ContractsGrantsInvoiceDetail invoiceDetail) {
        invoiceDetail.setTotalPreviouslyBilled(invoiceDetail.getTotalPreviouslyBilled().add(invoiceDetail.getInvoiceAmount()));
        invoiceDetail.setCumulativeExpenditures(invoiceDetail.getCumulativeExpenditures().subtract(invoiceDetail.getInvoiceAmount()));
        invoiceDetail.setInvoiceAmount(invoiceDetail.getInvoiceAmount().negated());
        invoiceDetail.setInvoiceDocument(null);
    }

    /**
     * Error corrects an invoice account detail
     * @param invoiceAccountDetail the invoice account detail to error correct
     */
    protected void correctInvoiceAccountDetail(InvoiceAccountDetail invoiceAccountDetail) {
        invoiceAccountDetail.setTotalPreviouslyBilled(invoiceAccountDetail.getTotalPreviouslyBilled().add(invoiceAccountDetail.getInvoiceAmount()));
        invoiceAccountDetail.setCumulativeExpenditures(invoiceAccountDetail.getCumulativeExpenditures().subtract(invoiceAccountDetail.getInvoiceAmount()));
        invoiceAccountDetail.setInvoiceAmount(invoiceAccountDetail.getInvoiceAmount().negated());
        invoiceAccountDetail.setInvoiceDocument(null);
    }

    /**
     * This method returns a list of character strings that represent base 36 integers from start(non-inclusive) to limit
     * (inclusive).
     *
     * @param start the starting point of the list. This value is not included in the list.
     * @param limit the ending point of the list. This value is included in the list
     * @return the list of strings
     * @throws IllegalArgumentException if start is not less than limit
     */
    protected List<String> incrementAlphaNumericString(String stringToIncrement, String stringLimit) throws IllegalArgumentException {
        int startInt = Integer.parseInt(stringToIncrement, 36);
        int limitInt = Integer.parseInt(stringLimit, 36);
        if (startInt >= limitInt) {
            throw new IllegalArgumentException("Starting code must be less than limit code.");
        }
        List<String> retval = new ArrayList<String>();
        for (int i = startInt + 1; i <= limitInt; i++) {
            // format below forces the string back to 4 characters and replace makes the extra
            // characters '0'
            retval.add(String.format("%4s", Integer.toString(i, 36)).replace('\u0020', '0'));
        }
        return retval;
    }

    protected void addNoteForInvoiceReportFail(ContractsGrantsInvoiceDocument document) {
        Note note = new Note();
        note.setNotePostedTimestampToCurrent();
        note.setNoteText(configurationService.getPropertyValueAsString(ArKeyConstants.ERROR_FILE_UPLOAD_NO_PDF_FILE_SELECTED_FOR_SAVE));
        note.setNoteTypeCode(KFSConstants.NoteTypeEnum.BUSINESS_OBJECT_NOTE_TYPE.getCode());
        Person systemUser = personService.getPersonByPrincipalName(KFSConstants.SYSTEM_USER);
        note = noteService.createNote(note, document.getNoteTarget(), systemUser.getPrincipalId());
        noteService.save(note);
        document.addNote(note);
    }

    @Override
    public List<String> checkAwardContractControlAccounts(ContractsAndGrantsBillingAward award) {
        List<String> errorString = new ArrayList<String>();
        boolean isValid = true;
        int accountNum = award.getActiveAwardAccounts().size();
        // To check if invoicing options exist on the award
        if (ObjectUtils.isNotNull(award.getInvoicingOptionCode())) {

            // To check if the award account is associated with a contract control account.
            for (ContractsAndGrantsBillingAwardAccount awardAccount : award.getActiveAwardAccounts()) {
                if (ObjectUtils.isNull(awardAccount.getAccount().getContractControlAccount())) {
                    isValid = false;
                    break;
                }
            }

            // if the Invoicing option is "By Contract Control Account" and there are no contract control accounts for one / all
            // award accounts, then throw error.
            if (award.getInvoicingOptionCode().equalsIgnoreCase(ArConstants.INV_CONTRACT_CONTROL_ACCOUNT)) {
                if (!isValid) {
                    errorString.add(ArKeyConstants.AwardConstants.ERROR_NO_CTRL_ACCT);
                    errorString.add(award.getInvoicingOptionDescription());
                }
            }

            // if the Invoicing option is "By Award" and there are no contract control accounts for one / all award accounts, then
            // throw error.
            else if (award.getInvoicingOptionCode().equalsIgnoreCase(ArConstants.INV_AWARD)) {
                if (!isValid) {
                    errorString.add(ArKeyConstants.AwardConstants.ERROR_NO_CTRL_ACCT);
                    errorString.add(award.getInvoicingOptionDescription());
                }
                else {
                    if (accountNum != 1) {
                        Account tmpAcct1, tmpAcct2;

                        Object[] awardAccounts = award.getActiveAwardAccounts().toArray();
                        for (int i = 0; i < awardAccounts.length - 1; i++) {
                            tmpAcct1 = ((ContractsAndGrantsBillingAwardAccount) awardAccounts[i]).getAccount().getContractControlAccount();
                            tmpAcct2 = ((ContractsAndGrantsBillingAwardAccount) awardAccounts[i + 1]).getAccount().getContractControlAccount();
                            // if the Invoicing option is "By Award" and there are more than one contract control account assigned
                            // for the award, then throw error.
                            if (ObjectUtils.isNull(tmpAcct1) || !tmpAcct1.equals(tmpAcct2)) {
                                errorString.add(ArKeyConstants.AwardConstants.ERROR_MULTIPLE_CTRL_ACCT);
                                errorString.add(award.getInvoicingOptionDescription());
                            }
                        }
                    }
                }
            }
        }
        return errorString;
    }

    /**
     * Determines whether the given ContractsGrantsInvoiceDocument is "effective" or not: if it is disapproved, cancelled, or error corrected then it is NOT effective,
     * and in all other cases, it is effective
     * @param invoiceDocument the invoice document to check
     * @return true if the document is "effective" given the rules above, false otherwise
     */
    @Override
    public boolean isInvoiceDocumentEffective(String documentNumber) {
        final FinancialSystemDocumentHeader invoiceDocHeader = getBusinessObjectService().findBySinglePrimaryKey(FinancialSystemDocumentHeader.class, documentNumber);
        final String documentStatus = invoiceDocHeader.getWorkflowDocumentStatusCode();
        if (StringUtils.isBlank(invoiceDocHeader.getFinancialDocumentInErrorNumber()) && !StringUtils.equals(documentStatus, DocumentStatus.CANCELED.getCode()) && !StringUtils.equals(documentStatus, DocumentStatus.DISAPPROVED.getCode())) { // skip error correcting CINVs, as they should be taken care of by the error correcting code
            final DocumentHeader correctingDocumentHeader = getFinancialSystemDocumentService().getCorrectingDocumentHeader(documentNumber);
            if (ObjectUtils.isNull(correctingDocumentHeader) || isCorrectedInvoiceDocumentEffective(correctingDocumentHeader.getDocumentNumber())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if an error correction is "effective" - ie, currently locking resources like milestones and pre-determined billing from the original CINV
     * @param errorCorrectionDocumentNumber the document number to check for the effectiveness of
     * @return true if the document is effectively locking resources, false otherwise
     */
    protected boolean isCorrectedInvoiceDocumentEffective(String errorCorrectionDocumentNumber) {
        final FinancialSystemDocumentHeader invoiceDocHeader = getBusinessObjectService().findBySinglePrimaryKey(FinancialSystemDocumentHeader.class, errorCorrectionDocumentNumber);
        final String documentStatus = invoiceDocHeader.getWorkflowDocumentStatusCode();
        if (getFinancialSystemDocumentService().getPendingDocumentStatuses().contains(documentStatus)) {
            return true; // the error correction document is currently pending, then it has not yet freed the milestones and pre-billings on the original CINV, so it's effective
        }
        final DocumentHeader correctingDocumentHeader = getFinancialSystemDocumentService().getCorrectingDocumentHeader(errorCorrectionDocumentNumber);
        if (!ObjectUtils.isNull(correctingDocumentHeader) && isCorrectedInvoiceDocumentEffective(correctingDocumentHeader.getDocumentNumber())) {
            return true; // is the error correction currently undergoing error correction itself?  Then recheck the rules on the newer error corrector to see if this document is effective or not
        }
        return false; // the error correction document is not effective and has freed resources
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#isTemplateValidForContractsGrantsInvoiceDocument(org.kuali.kfs.module.ar.businessobject.InvoiceTemplate, org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public boolean isTemplateValidForContractsGrantsInvoiceDocument(InvoiceTemplate invoiceTemplate, ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        if (ObjectUtils.isNotNull(contractsGrantsInvoiceDocument)) {
            return StringUtils.equals(invoiceTemplate.getBillByChartOfAccountCode(), contractsGrantsInvoiceDocument.getBillByChartOfAccountCode()) && StringUtils.equals(invoiceTemplate.getBilledByOrganizationCode(),contractsGrantsInvoiceDocument.getBilledByOrganizationCode());
        }
        return true;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#doesCostCategoryContainObjectCode(org.kuali.kfs.module.ar.businessobject.CostCategory, java.lang.String, java.lang.String)
     */
    @Override
    public boolean doesCostCategoryContainObjectCode(CostCategory category, String chartOfAccountsCode, String objectCode) {
        if (!CollectionUtils.isEmpty(category.getObjectCodes())) {
            for (CostCategoryObjectCode categoryObjectCode : category.getObjectCodes()) {
                if (StringUtils.equals(categoryObjectCode.getChartOfAccountsCode(), chartOfAccountsCode) && StringUtils.equals(categoryObjectCode.getFinancialObjectCode(), objectCode)) {
                    return true;
                }
            }
        }

        if (!CollectionUtils.isEmpty(category.getObjectLevels())) {
            for (CostCategoryObjectLevel categoryObjectLevel : category.getObjectLevels()) {
                if (getObjectCodeService().doesObjectLevelContainObjectCode(categoryObjectLevel.getChartOfAccountsCode(), categoryObjectLevel.getFinancialObjectLevelCode(), chartOfAccountsCode, objectCode)) {
                    return true;
                }
            }
        }

        if (!CollectionUtils.isEmpty(category.getObjectConsolidations())) {
            for (CostCategoryObjectConsolidation categoryObjectConsolidation : category.getObjectConsolidations()) {
                if (getObjectCodeService().doesObjectConsolidationContainObjectCode(categoryObjectConsolidation.getChartOfAccountsCode(), categoryObjectConsolidation.getFinConsolidationObjectCode(), chartOfAccountsCode, objectCode)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#getInvoicesByAward(java.util.Collection)
     */
    @Override
    public Map<Long, List<ContractsGrantsInvoiceDocument>> getInvoicesByAward(Collection<ContractsGrantsInvoiceDocument> invoices) {
        Map<Long, List<ContractsGrantsInvoiceDocument>> invoicesByAward = new HashMap<>();
        for (ContractsGrantsInvoiceDocument invoice : invoices) {
            Long proposalNumber = invoice.getInvoiceGeneralDetail().getProposalNumber();
            if (invoicesByAward.containsKey(proposalNumber)) {
                invoicesByAward.get(proposalNumber).add(invoice);
            }
            else {
                List<ContractsGrantsInvoiceDocument> invoicesByProposalNumber = new ArrayList<ContractsGrantsInvoiceDocument>();
                invoicesByProposalNumber.add(invoice);
                invoicesByAward.put(proposalNumber, invoicesByProposalNumber);
            }
        }
        return invoicesByAward;
    }

    /**
     * If the document has only one CustomerInvoiceDetail, update the amount with the total expenditures from the cost categories; otherwise,
     * split the amounts by account
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#recalculateSourceAccountingLineTotals(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public void recalculateSourceAccountingLineTotals(ContractsGrantsInvoiceDocument contractsGrantsInvoiceDocument) {
        if (!CollectionUtils.isEmpty(contractsGrantsInvoiceDocument.getSourceAccountingLines())) {
            if (contractsGrantsInvoiceDocument.getSourceAccountingLines().size() == 1) {
                final CustomerInvoiceDetail customerInvoiceDetail = (CustomerInvoiceDetail)contractsGrantsInvoiceDocument.getSourceAccountingLine(0);
                customerInvoiceDetail.setAmount(getAccountingLineAmountForDocument(contractsGrantsInvoiceDocument));
            } else {
                final Map<String, KualiDecimal> accountExpenditureAmounts = getCategoryExpenditureAmountsForInvoiceAccountDetail(contractsGrantsInvoiceDocument);
                for (Object al : contractsGrantsInvoiceDocument.getSourceAccountingLines()) {
                    final CustomerInvoiceDetail customerInvoiceDetail = (CustomerInvoiceDetail)al;
                    final String accountKey = StringUtils.join(new String[] { customerInvoiceDetail.getChartOfAccountsCode(), customerInvoiceDetail.getAccountNumber() }, "-");
                    customerInvoiceDetail.setAmount(accountExpenditureAmounts.get(accountKey));
                }
            }
        }
    }

    /**
     * Checks the KFS-AR / ContractsGrantsInvoiceDocumentBatchStep / User parameter, or, if no value is found for that parameter, the KFS system user to see if they are the
     * initiator of the document.  If the KFS-AR / ContractsGrantsInvoiceDocumentBatchStep / User parameter is set to a value of a principal who creates CINVs outside of batch
     * - well, okay, that's an interesting business process, that also means that the results of this method may be incorrect.  But you can always override, right?
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#isDocumentBatchCreated(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public boolean isDocumentBatchCreated(ContractsGrantsInvoiceDocument document) {
        if (document.getInvoiceGeneralDetail().getAward().getAutoApproveIndicator()) {
            final Principal batchJobInitiatorPrincipal = getContractsGrantsInvoiceBatchCreationUserPrincipal();
            return StringUtils.equalsIgnoreCase(document.getFinancialSystemDocumentHeader().getInitiatorPrincipalId(), batchJobInitiatorPrincipal.getPrincipalId());
        }
        return false;
    }

    /**
     * @see org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService#doesInvoicePassValidation(org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument)
     */
    @Override
    public boolean doesInvoicePassValidation(final ContractsGrantsInvoiceDocument document) {
        try {
            final Boolean result = GlobalVariables.doInNewGlobalVariables(new UserSession(getContractsGrantsInvoiceBatchCreationUserPrincipal().getPrincipalName()), new Callable<Boolean>() {
                /**
                 * Checks if the given document passes rule validation with no errors
                 * @see java.util.concurrent.Callable#call()
                 */
                @Override
                public Boolean call() throws Exception {
                    final AttributedRouteDocumentEvent routeEvent = new AttributedRouteDocumentEvent(document);
                    getKualiRuleService().applyRules(routeEvent);
                    return Boolean.valueOf(!GlobalVariables.getMessageMap().hasErrors());
                }

            });
            return result.booleanValue();
        } catch (Exception e) {
            // validation actually caused an exception?  Um, let's log that, and maybe we'll say we didn't pass validation here, okay?
            LOG.error("Running validation on Contracts & Grants Invoice "+document.getDocumentNumber()+" caused an exception", e);
            return false;
        }
    }

    /**
     * Determines the user who creates CINV documents via the batch job
     * @return the principal for the user who creates CINV documents via the batch job
     */
    protected Principal getContractsGrantsInvoiceBatchCreationUserPrincipal() {
        final String batchJobInitiatorPrincipalName = getParameterService().getParameterValueAsString(ContractsGrantsInvoiceDocumentBatchStep.class, Job.STEP_USER_PARM_NM, KFSConstants.SYSTEM_USER);
        final Principal batchJobInitiatorPrincipal = getIdentityService().getPrincipalByPrincipalName(batchJobInitiatorPrincipalName);
        return ObjectUtils.isNull(batchJobInitiatorPrincipal)
                ? getIdentityService().getPrincipalByPrincipalName(KFSConstants.SYSTEM_USER)
                : batchJobInitiatorPrincipal;
    }

    public ContractsAndGrantsModuleBillingService getContractsAndGrantsModuleBillingService() {
        return contractsAndGrantsModuleBillingService;
    }

    public void setContractsAndGrantsModuleBillingService(ContractsAndGrantsModuleBillingService contractsAndGrantsModuleBillingService) {
        this.contractsAndGrantsModuleBillingService = contractsAndGrantsModuleBillingService;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    /**
     * Sets the accountService attribute value.
     *
     * @param accountService The accountService to set.
     */
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Gets the objectCodeService attribute.
     * @return Returns the objectCodeService.
     */
    public ObjectCodeService getObjectCodeService() {
        return objectCodeService;
    }

    /**
     * Sets the objectCodeService attribute value.
     * @param objectCodeService The objectCodeService to set.
     */
    public void setObjectCodeService(ObjectCodeService objectCodeService) {
        this.objectCodeService = objectCodeService;
    }

    /**
     * Gets the attachmentService attribute.
     *
     * @return Returns the attachmentService.
     */
    public AttachmentService getAttachmentService() {
        return attachmentService;
    }

    /**
     * Sets the attachmentService attribute value.
     *
     * @param attachmentService The attachmentService to set.
     */
    public void setAttachmentService(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    public NoteService getNoteService() {
        return noteService;
    }

    public void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }

    public KualiModuleService getKualiModuleService() {
        return kualiModuleService;
    }

    /**
     * Sets the kualiModuleService attribute value.
     *
     * @param kualiModuleService The kualiModuleService to set.
     */
    public void setKualiModuleService(KualiModuleService kualiModuleService) {
        this.kualiModuleService = kualiModuleService;
    }

    /**
     * @return
     */
    public ContractsGrantsInvoiceDocumentDao getContractsGrantsInvoiceDocumentDao() {
        return contractsGrantsInvoiceDocumentDao;
    }

    /**
     * @param contractsGrantsInvoiceDocumentDao
     */
    public void setContractsGrantsInvoiceDocumentDao(ContractsGrantsInvoiceDocumentDao contractsGrantsInvoiceDocumentDao) {
        this.contractsGrantsInvoiceDocumentDao = contractsGrantsInvoiceDocumentDao;
    }

    public ContractsGrantsBillingUtilityService getContractsGrantsBillingUtilityService() {
        return contractsGrantsBillingUtilityService;
    }

    public void setContractsGrantsBillingUtilityService(ContractsGrantsBillingUtilityService contractsGrantsBillingUtilityService) {
        this.contractsGrantsBillingUtilityService = contractsGrantsBillingUtilityService;
    }

    public FinancialSystemDocumentService getFinancialSystemDocumentService() {
        return financialSystemDocumentService;
    }

    public void setFinancialSystemDocumentService(FinancialSystemDocumentService financialSystemDocumentService) {
        this.financialSystemDocumentService = financialSystemDocumentService;
    }

    public List<SuspensionCategory> getSuspensionCategories() {
        return suspensionCategories;
    }

    public void setSuspensionCategories(List<SuspensionCategory> suspensionCategories) {
        this.suspensionCategories = suspensionCategories;
    }

    public BusinessObjectService getBusinessObjectService() {
        return businessObjectService;
    }

    public void setBusinessObjectService(BusinessObjectService businessObjectService) {
        this.businessObjectService = businessObjectService;
    }

    public ParameterService getParameterService() {
        return parameterService;
    }

    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public UniversityDateService getUniversityDateService() {
        return universityDateService;
    }

    public void setUniversityDateService(UniversityDateService universityDateService) {
        this.universityDateService = universityDateService;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public DateTimeService getDateTimeService() {
        return dateTimeService;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public PersonService getPersonService() {
        return personService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public AccountsReceivablePendingEntryService getAccountsReceivablePendingEntryService() {
        return accountsReceivablePendingEntryService;
    }

    public void setAccountsReceivablePendingEntryService(AccountsReceivablePendingEntryService accountsReceivablePendingEntryService) {
        this.accountsReceivablePendingEntryService = accountsReceivablePendingEntryService;
    }

    public OptionsService getOptionsService() {
        return optionsService;
    }

    public void setOptionsService(OptionsService optionsService) {
        this.optionsService = optionsService;
    }

    public KualiRuleService getKualiRuleService() {
        return kualiRuleService;
    }

    public void setKualiRuleService(KualiRuleService kualiRuleService) {
        this.kualiRuleService = kualiRuleService;
    }

    public IdentityService getIdentityService() {
        return identityService;
    }

    public void setIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public CostCategoryService getCostCategoryService() {
        return costCategoryService;
    }

    public void setCostCategoryService(CostCategoryService costCategoryService) {
        this.costCategoryService = costCategoryService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public CustomerInvoiceDocumentService getCustomerInvoiceDocumentService() {
        return customerInvoiceDocumentService;
    }

    public void setCustomerInvoiceDocumentService(CustomerInvoiceDocumentService customerInvoiceDocumentService) {
        this.customerInvoiceDocumentService = customerInvoiceDocumentService;
    }
}
