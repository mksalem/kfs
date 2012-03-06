/*
 * Copyright 2010 The Kuali Foundation.
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
package org.kuali.kfs.module.tem.document;

import static org.kuali.kfs.module.tem.TemConstants.PARAM_NAMESPACE;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_AWARD;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_ENT_MANAGER;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_FISCAL;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_ORG;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_SPCL;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_SUB;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.AWAIT_TAX_MANAGER;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_AWARD;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_ENT_MANAGER;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_FISCAL;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_ORG;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_SPCL;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_SUB;
import static org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys.DAPRVD_TAX_MANAGER;
import static org.kuali.kfs.module.tem.TemConstants.TravelEntertainmentParameters.ENTERTAINMENT_DOCUMENT_LOCATION;
import static org.kuali.kfs.module.tem.TemConstants.TravelEntertainmentParameters.PARAM_DTL_TYPE;
import static org.kuali.kfs.module.tem.util.BufferedLogger.debug;

import java.beans.PropertyChangeEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.kuali.kfs.fp.document.DisbursementVoucherDocument;
import org.kuali.kfs.integration.ar.AccountsReceivableCustomer;
import org.kuali.kfs.module.purap.document.RequisitionDocument;
import org.kuali.kfs.module.tem.TemConstants;
import org.kuali.kfs.module.tem.TemConstants.EntertainmentStatusCodeKeys;
import org.kuali.kfs.module.tem.TemConstants.TravelRelocationParameters;
import org.kuali.kfs.module.tem.TemPropertyConstants;
import org.kuali.kfs.module.tem.TemPropertyConstants.TEMProfileProperties;
import org.kuali.kfs.module.tem.TemWorkflowConstants;
import org.kuali.kfs.module.tem.businessobject.ActualExpense;
import org.kuali.kfs.module.tem.businessobject.Attendee;
import org.kuali.kfs.module.tem.businessobject.PerDiemExpense;
import org.kuali.kfs.module.tem.businessobject.Purpose;
import org.kuali.kfs.module.tem.businessobject.TEMProfile;
import org.kuali.kfs.module.tem.businessobject.TravelerDetail;
import org.kuali.kfs.module.tem.businessobject.TravelerType;
import org.kuali.kfs.module.tem.document.service.TravelDocumentService;
import org.kuali.kfs.module.tem.document.service.TravelEntertainmentDocumentService;
import org.kuali.kfs.module.tem.service.TravelService;
import org.kuali.kfs.module.tem.service.TravelerService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.businessobject.AccountingLine;
import org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntry;
import org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntrySourceDetail;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.UniversityDateService;
import org.kuali.rice.kew.dto.DocumentRouteStatusChangeDTO;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kew.util.KEWConstants;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kim.service.PersonService;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.KualiDecimal;
import org.kuali.rice.kns.util.ObjectUtils;
import org.kuali.rice.kns.workflow.service.WorkflowDocumentService;

@Entity
@Table(name = "TEM_ENT_DOC_T")
public class TravelEntertainmentDocument extends TEMReimbursementDocument {

    private Integer hostProfileId;
    private String hostName;
    private String eventTitle;
    private Boolean hostCertified;
    private Boolean nonEmployeeCertified;
    protected Boolean spouseIncluded;
    private String description;
    private String purposeCode;
    private Purpose purpose;
    private String paymentMethod;

    private Boolean attendeeListAttached;
    private Integer numberOfAttendees;
    private TravelerDetail host;
    private TEMProfile hostProfile;
    private TravelerDetail attendeeDetail;

    private List<Attendee> attendee = new ArrayList<Attendee>();

    public TravelEntertainmentDocument() {
    }

    @Override
    public boolean answerSplitNodeQuestion(String nodeName) throws UnsupportedOperationException {
        if (nodeName.equals(TemWorkflowConstants.SPECIAL_REQUEST))
            return requiresSpecialRequestReviewRouting();
        if (nodeName.equals(TemWorkflowConstants.TAX_MANAGER))
            return requiresTaxManagerApprovalRouting();
        if (nodeName.equals(TemWorkflowConstants.ENTERTAINMENT_MANAGER))
            return requiresEntertainmentManagerRouting();
        throw new UnsupportedOperationException("Cannot answer split question for this node you call \"" + nodeName + "\"");
    }

    private boolean requiresEntertainmentManagerRouting() {
        return true;
    }

    @Override
    protected boolean requiresSpecialRequestReviewRouting() {
        if (super.requiresSpecialRequestReviewRouting()) {
            return true;
        }
        
        if (getPurpose() != null) {
            String purposeCode = getPurpose().getPurposeCode();
            if (getPurpose().isReviewRequiredIndicator() != null && getPurpose().isReviewRequiredIndicator()) {
                return true;
            }
        }

        if ((ObjectUtils.isNotNull(getSpouseIncluded()) && getSpouseIncluded())) {
            return true;
        }
        
        return false;
    }

    public void populateDisbursementVoucherFields(DisbursementVoucherDocument disbursementVoucherDocument) {
        super.populateDisbursementVoucherFields(disbursementVoucherDocument);
        
        disbursementVoucherDocument.setDisbVchrCheckStubText(this.getTravelDocumentIdentifier() + " " + (this.getEventTitle() != null ? this.getEventTitle() : "") + this.getTripBegin());              
        disbursementVoucherDocument.getDocumentHeader().setDocumentDescription("Generated for ENT doc: " + this.getDocumentTitle() != null ? this.getDocumentTitle() : this.getTravelDocumentIdentifier());
        if (disbursementVoucherDocument.getDocumentHeader().getDocumentDescription().length() >= 40) {
            String truncatedDocumentDescription = disbursementVoucherDocument.getDocumentHeader().getDocumentDescription().substring(0, 39);
            disbursementVoucherDocument.getDocumentHeader().setDocumentDescription(truncatedDocumentDescription);
        }

        try {
            disbursementVoucherDocument.getDocumentHeader().getWorkflowDocument().setTitle(this.getDocumentHeader().getDocumentDescription());
        }
        catch (WorkflowException ex) {
            ex.printStackTrace();
        }
        
        disbursementVoucherDocument.setDisbursementVoucherDocumentationLocationCode(getParameterService().getParameterValue(PARAM_NAMESPACE, PARAM_DTL_TYPE, ENTERTAINMENT_DOCUMENT_LOCATION));        
        String paymentReasonCode = getParameterService().getParameterValue(PARAM_NAMESPACE, PARAM_DTL_TYPE, TemConstants.TravelEntertainmentParameters.ENT_REIMBURSEMENT_DV_REASON_CODE);
        disbursementVoucherDocument.getDvPayeeDetail().setDisbVchrPaymentReasonCode(paymentReasonCode);
        disbursementVoucherDocument.setDisbVchrPaymentMethodCode(this.getPaymentMethod());
    }

    public void populateRequisitionFields(RequisitionDocument reqsDoc, TravelDocument document) {
        super.populateRequisitionFields(reqsDoc, document);
        TravelEntertainmentDocument entDocument = (TravelEntertainmentDocument) document;
        reqsDoc.getDocumentHeader().setDocumentDescription("Generated for ENT doc: " + (entDocument.getEventTitle() == null ? "" : entDocument.getEventTitle()));
        reqsDoc.getDocumentHeader().setOrganizationDocumentNumber(entDocument.getTravelDocumentIdentifier());
        Calendar calendar = getDateTimeService().getCurrentCalendar();
        calendar.setTime(entDocument.getTripBegin());
        reqsDoc.setPostingYear(calendar.get(calendar.YEAR));
    }

    public void doRouteStatusChange(DocumentRouteStatusChangeDTO statusChangeEvent) {
        super.doRouteStatusChange(statusChangeEvent);

        debug("Handling route status change");
        debug("route status is ", statusChangeEvent.getNewRouteStatus());
        String currStatus = getDocumentHeader().getWorkflowDocument().getRouteHeader().getAppDocStatus();
        if (ObjectUtils.isNotNull(currStatus)) {
            updateAppDocStatus(currStatus);
        }
        
        if (KEWConstants.ROUTE_HEADER_DISAPPROVED_CD.equals(statusChangeEvent.getNewRouteStatus())) {
            // first we need to see where we were so we can change the app doc status
            String currAppDocStatus = getAppDocStatus();
            if (currAppDocStatus.equals(AWAIT_FISCAL)) {
                updateAppDocStatus(DAPRVD_FISCAL);
            }
            if (currAppDocStatus.equals(AWAIT_ORG)) {
                updateAppDocStatus(DAPRVD_ORG);
            }
            if (currAppDocStatus.equals(AWAIT_SUB)) {
                updateAppDocStatus(DAPRVD_SUB);
            }
            if (currAppDocStatus.equals(AWAIT_AWARD)) {
                updateAppDocStatus(DAPRVD_AWARD);
            }
            if (currAppDocStatus.equals(AWAIT_SPCL)) {
                updateAppDocStatus(DAPRVD_SPCL);
            }
            if (currAppDocStatus.equals(AWAIT_TAX_MANAGER)) {
                updateAppDocStatus(DAPRVD_TAX_MANAGER);
            }
            if (currAppDocStatus.equals(AWAIT_ENT_MANAGER)) {
                updateAppDocStatus(DAPRVD_ENT_MANAGER);
            }
        }
        
        String s = statusChangeEvent.getNewRouteStatus();
        if (KEWConstants.ROUTE_HEADER_FINAL_CD.equals(statusChangeEvent.getNewRouteStatus()) || KEWConstants.ROUTE_HEADER_PROCESSED_CD.equals(statusChangeEvent.getNewRouteStatus())) {
            debug("New route status is ", statusChangeEvent.getNewRouteStatus());
            // for some reason when it goes to final it never updates to the last status
            updateAppDocStatus(EntertainmentStatusCodeKeys.ENT_MANAGER_APPROVED);
            if (getDocumentGrandTotal() != null && getDocumentGrandTotal().isGreaterThan(KualiDecimal.ZERO)) {
                getEntertainmentDocumentService().createDVReimbursementDocument(this);
            }

            // If the hold new fiscal year encumbrance indicator is true and the trip end date
            // is after the current fiscal year end date then mark all the gl pending entries
            // as 'H' (Hold) otherwise mark all the gl pending entries as 'A' (approved)
            if (getGeneralLedgerPendingEntries() != null && !getGeneralLedgerPendingEntries().isEmpty()) {
                if (getParameterService().getIndicatorParameter(TemConstants.PARAM_NAMESPACE, TemConstants.TravelAuthorizationParameters.PARAM_DTL_TYPE, TemConstants.TravelAuthorizationParameters.HOLD_NEW_FY_ENCUMBRANCES_IND)) {
                    UniversityDateService universityDateService = SpringContext.getBean(UniversityDateService.class);
                    java.util.Date endDate = universityDateService.getLastDateOfFiscalYear(universityDateService.getCurrentFiscalYear());
                    if (ObjectUtils.isNotNull(getTripEnd()) && getTripEnd().after(endDate)) {
                        for (GeneralLedgerPendingEntry glpe : getGeneralLedgerPendingEntries()) {
                            glpe.setFinancialDocumentApprovedCode(KFSConstants.PENDING_ENTRY_APPROVED_STATUS_CODE.HOLD);
                        }
                    }
                }
                else {
                    for (GeneralLedgerPendingEntry glpe : getGeneralLedgerPendingEntries()) {
                        glpe.setFinancialDocumentApprovedCode(KFSConstants.DocumentStatusCodes.APPROVED);
                    }
                }
                SpringContext.getBean(BusinessObjectService.class).save(getGeneralLedgerPendingEntries());
            }
        }
    }

    public void updateAppDocStatus(String newStatus) {
        debug("new status is: " + newStatus);

        // get current workflow status and compare to status change
        String currStatus = getAppDocStatus();
        if (ObjectUtils.isNull(currStatus) || !newStatus.equalsIgnoreCase(currStatus)) {
            // update
            setAppDocStatus(newStatus);
        }
        if ((this.getDocumentHeader().getWorkflowDocument().stateIsFinal() || getDocumentHeader().getWorkflowDocument().stateIsProcessed())) {
            WorkflowDocumentService workflowDocumentService = SpringContext.getBean(WorkflowDocumentService.class);
            try {
                workflowDocumentService.save(this.getDocumentHeader().getWorkflowDocument(), null);
            }
            catch (WorkflowException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
    }

    protected TravelEntertainmentDocumentService getEntertainmentDocumentService() {
        return SpringContext.getBean(TravelEntertainmentDocumentService.class);
    }

    public void initiateDocument() {
        updateAppDocStatus(TemConstants.TravelReimbursementStatusCodeKeys.IN_PROCESS);
        setActualExpenses(new ArrayList<ActualExpense>());
        setPerDiemExpenses(new ArrayList<PerDiemExpense>());

        getDocumentHeader().setDocumentDescription(TemConstants.PRE_FILLED_DESCRIPTION);
        if (this.getTraveler() == null) {
            this.setTraveler(new TravelerDetail());
            this.getTraveler().setTravelerTypeCode(TemConstants.EMP_TRAVELER_TYP_CD);
        }
        
        Calendar calendar = getDateTimeService().getCurrentCalendar();
        if (this.getTripBegin() == null) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            setTripBegin(new Timestamp(calendar.getTimeInMillis()));

        }
        if (this.getTripEnd() == null) {
            calendar.add(Calendar.DAY_OF_MONTH, 2);
            setTripEnd(new Timestamp(calendar.getTimeInMillis()));
        }

        Person currentUser = GlobalVariables.getUserSession().getPerson();
        if (!getTravelDocumentService().isTravelArranger(currentUser, null)) {
            TEMProfile temProfile = getTravelService().findTemProfileByPrincipalId(currentUser.getPrincipalId());
            if (temProfile != null) {
                setTemProfile(temProfile);
            }
        }
    }

    /**
     * Given the <code>financialObjectCode</code>, determine the total of the {@link SourceAccountingLine} instances with that
     * <code>financialObjectCode</code>
     * 
     * @param financialObjectCode to search for total on
     * @return @{link KualiDecimal} with total value for {@link AccountingLines} with <code>finanncialObjectCode</code>
     */
    public KualiDecimal getTotalFor(final String financialObjectCode) {
        KualiDecimal retval = KualiDecimal.ZERO;

        debug("Getting total for ", financialObjectCode);

        for (final AccountingLine line : (List<AccountingLine>) getSourceAccountingLines()) {
            debug("Comparing ", financialObjectCode, " to ", line.getObjectCode().getCode());
            if (line.getObjectCode().getCode().equals(financialObjectCode)) {
                retval = retval.add(line.getAmount());
            }
        }

        return retval;
    }

    public boolean canShowHostCertification() {
        return (getHostProfile() != null && getTemProfile() != null && !getHostProfile().getProfileId().equals(getTemProfile().getProfileId()) && !getDocumentHeader().getWorkflowDocument().stateIsInitiated());
    }

    public boolean canDisplayNonEmployeeCheckbox() {
        return ((getHostProfile() != null && getHostProfile().getTravelerTypeCode().equals(TemConstants.NONEMP_TRAVELER_TYP_CD)) || (getTemProfile() != null && getTemProfile().getTravelerTypeCode().equals(TemConstants.NONEMP_TRAVELER_TYP_CD)));
    }

    protected String generateDescription() {
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        PersonService<Person> ps = SpringContext.getBean(PersonService.class);

        Person person = ps.getPerson(getTraveler().getPrincipalId());

        this.getTraveler().refreshReferenceObject(TemPropertyConstants.CUSTOMER);

        AccountsReceivableCustomer customer = getTraveler().getCustomer();
        if (person != null) {
            sb.append(person.getLastName() + ", " + person.getFirstName() + " " + person.getMiddleName() + " ");
        }
        else if (customer != null) {
            sb.append(customer.getCustomerName() + " ");
        }
        else {
            sb.append(getTraveler().getFirstName() + " " + getTraveler().getLastName() + " ");
        }

        if (this.getTripBegin() != null) {
            sb.append(format.format(this.getTripBegin()) + " ");
        }
        if (eventTitle != null)
            sb.append(this.eventTitle);
        String tempStr = sb.toString();

        if (tempStr.length() > 40) {
            tempStr = tempStr.substring(0, 39);
        }

        return tempStr;
    }

    @Transient
    public void addAttendee(final Attendee line) {
        final String sequenceName = line.getSequenceName();
        final Long sequenceNumber = getSequenceAccessorService().getNextAvailableSequenceNumber(sequenceName, Attendee.class);
        line.setId(sequenceNumber.intValue());
        line.setDocumentNumber(this.documentNumber);
        notifyChangeListeners(new PropertyChangeEvent(this, "attendee", null, line));
        getAttendee().add(line);
    }

    @Transient
    public void removeAttendee(final Integer index) {
        final Attendee line = getAttendee().remove((int) index);
        notifyChangeListeners(new PropertyChangeEvent(this, "attendee", line, null));
    }

    @Column(name = "HOST_TEM_PROFILE_ID", length = 50, nullable = true)
    public Integer getHostProfileId() {
        return hostProfileId;
    }

    public void setHostProfileId(Integer hostProfileId) {
        this.hostProfileId = hostProfileId;
        BusinessObjectService service = (BusinessObjectService) SpringContext.getService("businessObjectService");
        Map<String, Object> primaryKeys = new HashMap<String, Object>();
        primaryKeys.put(TemPropertyConstants.TEMProfileProperties.PROFILE_ID, hostProfileId);
        setHostProfile((TEMProfile) service.findByPrimaryKey(TEMProfile.class, primaryKeys));
    }

    @Column(name = "TITLE", length = 100, nullable = true)
    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    @Column(name = "HOST_CERTIFIED", length = 1, nullable = true)
    public Boolean getHostCertified() {
        return hostCertified;
    }

    public void setHostCertified(Boolean hostCertified) {
        this.hostCertified = hostCertified;
    }

    @Column(name = "NON_EMPLOYEE_CERTIFIED", length = 1, nullable = true)
    public Boolean getNonEmployeeCertified() {
        return nonEmployeeCertified;
    }

    public void setNonEmployeeCertified(Boolean nonEmployeeCertified) {
        this.nonEmployeeCertified = nonEmployeeCertified;
    }

    @Column(name = "SPOUSE_INCLUDED", nullable = true, length = 1)
    public Boolean getSpouseIncluded() {
        return spouseIncluded;
    }

    public Boolean getSpouseIncludedForSearching() {
        return spouseIncluded;
    }

    public Boolean isSpouseIncludedForSearching() {
        return spouseIncluded;
    }

    public void setSpouseIncluded(Boolean spouseIncluded) {
        this.spouseIncluded = spouseIncluded;
    }

    @Column(name = "DESCRIPTION", nullable = true, length = 255)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "PURPOSE_CODE", nullable = true, length = 4)
    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurpose(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    @JoinColumn(name = "PURPOSE_CODE", nullable = true)
    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    @Override
    public boolean isDebit(GeneralLedgerPendingEntrySourceDetail postable) {
        // TODO Auto-generated method stub
        return false;
    }

    protected TravelService getTravelService() {
        return SpringContext.getBean(TravelService.class);
    }

    protected TravelDocumentService getTravelDocumentService() {
        return SpringContext.getBean(TravelDocumentService.class);
    }

    @Column(name = "ATTENDEE_LIST_ATTACHED", nullable = true, length = 1)
    public Boolean getAttendeeListAttached() {
        return attendeeListAttached;
    }

    public void setAttendeeListAttached(Boolean attendeeListAttached) {
        this.attendeeListAttached = attendeeListAttached;
    }

    @Column(name = "NUMBER_ATTENDEES", nullable = true, length = 50)
    public Integer getNumberOfAttendees() {
        return numberOfAttendees;
    }

    public void setNumberOfAttendees(Integer numberOfAttendees) {
        this.numberOfAttendees = numberOfAttendees;
    }

    public List<Attendee> getAttendee() {
        return attendee;
    }

    public void setAttendee(List<Attendee> attendee) {
        this.attendee = attendee;
    }

    @Column(name = "PAYMENT_METHOD", nullable = true, length = 15)
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Column(name = "HOST_NAME", nullable = true, length = 40)
    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public TravelerDetail getHost() {
        return host;
    }

    public void setHost(TravelerDetail host) {
        this.host = host;
    }

    public TravelerDetail getAttendeeDetail() {
        return attendeeDetail;
    }

    public void setAttendeeDetail(TravelerDetail attendeeDetail) {
        this.attendeeDetail = attendeeDetail;
    }

    public TEMProfile getHostProfile() {
        return hostProfile;
    }

    public void setHostProfile(TEMProfile hostProfile) {
        this.hostProfile = hostProfile;
        if (hostProfile != null) {
            TravelerService service = (TravelerService) SpringContext.getService("travelerService");
            service.populateTEMProfile(hostProfile);
            if (hostProfile.getTravelerType() == null) {
                BusinessObjectService boService = (BusinessObjectService) SpringContext.getService("businessObjectService");
                Map<String, Object> fieldValues = new HashMap<String, Object>();
                fieldValues.put("code", hostProfile.getTravelerTypeCode());
                List<TravelerType> types = (List<TravelerType>) boService.findMatching(TravelerType.class, fieldValues);
                hostProfile.setTravelerType(types.get(0));
            }
        }
    }
    
    @Override
    public String getReportPurpose() {
        if (purpose != null) {
            return purpose.getPurposeName();
        }
        
        return null;
    }

    @Override
    public void populateVendorPayment(DisbursementVoucherDocument disbursementVoucherDocument) {
        super.populateVendorPayment(disbursementVoucherDocument);
        
        disbursementVoucherDocument.setDisbVchrPaymentMethodCode(TemConstants.DisbursementVoucherPaymentMethods.CHECK_ACH_PAYMENT_METHOD_CODE);
        String locationCode = getParameterService().getParameterValue(PARAM_NAMESPACE, TravelRelocationParameters.PARAM_DTL_TYPE, TravelRelocationParameters.RELOCATION_DOCUMENTATION_LOCATION_CODE);
        String checkStubText = this.getTravelDocumentIdentifier() + ", " + this.getEventTitle();
        disbursementVoucherDocument.setDisbVchrPaymentMethodCode(TemConstants.DisbursementVoucherPaymentMethods.CHECK_ACH_PAYMENT_METHOD_CODE);
        
        disbursementVoucherDocument.setDisbursementVoucherDocumentationLocationCode(locationCode);
        disbursementVoucherDocument.setDisbVchrCheckStubText(checkStubText);
        
    }

    @Override
    public KualiDecimal getPerDiemAdjustment() {
        // Never Used
        return null;
    }

    @Override
    public void setPerDiemAdjustment(KualiDecimal perDiemAdjustment) {
        // Never Used
        
    }
}
