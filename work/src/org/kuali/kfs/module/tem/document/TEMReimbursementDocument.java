/*
 * Copyright 2012 The Kuali Foundation.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.fp.document.DisbursementVoucherDocument;
import org.kuali.kfs.module.purap.businessobject.PaymentRequestView;
import org.kuali.kfs.module.purap.document.PaymentRequestDocument;
import org.kuali.kfs.module.purap.document.RequisitionDocument;
import org.kuali.kfs.module.purap.util.PurApRelatedViews;
import org.kuali.kfs.module.tem.TemConstants;
import org.kuali.kfs.module.tem.businessobject.TEMExpense;
import org.kuali.kfs.module.tem.document.service.TravelDocumentService;
import org.kuali.kfs.module.tem.service.TEMExpenseService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.service.DocumentService;
import org.kuali.rice.kns.util.KualiDecimal;

public abstract class TEMReimbursementDocument extends TravelDocumentBase {
    
    public KualiDecimal getReimbursableTotal() {
        KualiDecimal eligible = getEligibleAmount();
        final KualiDecimal expenseLimit = getExpenseLimit();
        
        if (expenseLimit != null && expenseLimit.doubleValue() > 0) {
            return eligible.isGreaterThan(expenseLimit) ? expenseLimit : eligible;
        }
        
        return eligible;
    }
    
    public KualiDecimal getEligibleAmount(){
        return getApprovedAmount().subtract(getCTSTotal()).subtract(getCorporateCardTotal());
    }
    
    public KualiDecimal getTotalPaidAmountToVendor() {
        KualiDecimal totalPaidAmountToVendor = KualiDecimal.ZERO;
        try {
            Map<String, List<Document>> relateddocs = SpringContext.getBean(TravelDocumentService.class).getDocumentsRelatedTo(this);
            List<Document> relatedDVs = relateddocs.get("DV");
            if (relatedDVs != null && relatedDVs.size() > 0) {
                for (Document document : relatedDVs) {
                    if (document instanceof DisbursementVoucherDocument) {
                        DisbursementVoucherDocument dv = (DisbursementVoucherDocument) document;
                        if (dv.getDocumentHeader().getWorkflowDocument().stateIsFinal()) {
                            totalPaidAmountToVendor = totalPaidAmountToVendor.add(dv.getDisbVchrCheckTotalAmount());
                        }
                    }
                }
            }
        }
        catch (WorkflowException ex) {
            ex.printStackTrace();
        }
        return totalPaidAmountToVendor;
    }

    public KualiDecimal getTotalPaidAmountToRequests() {
        KualiDecimal totalPaidAmountToRequests = KualiDecimal.ZERO;
        try {
            Map<String, List<Document>> relateddocs = SpringContext.getBean(TravelDocumentService.class).getDocumentsRelatedTo(this);
            List<Document> relatedDVs = relateddocs.get("REQS");
            if (relatedDVs != null && relatedDVs.size() > 0) {
                for (Document document : relatedDVs) {
                    if (document instanceof RequisitionDocument) {
                        RequisitionDocument reqs = (RequisitionDocument) document;
                        PurApRelatedViews relatedviews = reqs.getRelatedViews();
                        if (relatedviews != null && relatedviews.getRelatedPaymentRequestViews() != null && relatedviews.getRelatedPaymentRequestViews().size() > 0) {
                            List<PaymentRequestView> preqViews = relatedviews.getRelatedPaymentRequestViews();
                            for (PaymentRequestView preqView : preqViews) {
                                PaymentRequestDocument preqDocument = (PaymentRequestDocument) SpringContext.getBean(DocumentService.class).getByDocumentHeaderId(preqView.getDocumentNumber());
                                if (preqDocument.getDocumentHeader().getWorkflowDocument().stateIsFinal()) {
                                    totalPaidAmountToRequests = totalPaidAmountToRequests.add(preqDocument.getVendorInvoiceAmount());
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (WorkflowException ex) {
            ex.printStackTrace();
        }
        return totalPaidAmountToRequests;
    }

    public KualiDecimal getReimbursableGrandTotal() {
        KualiDecimal grandTotal = KualiDecimal.ZERO;
        grandTotal = getApprovedAmount().add(getTotalPaidAmountToVendor()).add(getTotalPaidAmountToRequests());
               
        if (KualiDecimal.ZERO.isGreaterThan(grandTotal)) {
            return KualiDecimal.ZERO;
        }
        
        return grandTotal;
    }   
}
