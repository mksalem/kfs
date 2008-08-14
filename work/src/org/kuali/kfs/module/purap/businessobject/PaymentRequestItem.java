/*
 * Copyright 2006-2007 The Kuali Foundation.
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

package org.kuali.kfs.module.purap.businessobject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.kuali.kfs.module.purap.PurapConstants;
import org.kuali.kfs.module.purap.PurapPropertyConstants;
import org.kuali.kfs.module.purap.document.PaymentRequestDocument;
import org.kuali.kfs.module.purap.document.PurchaseOrderDocument;
import org.kuali.kfs.module.purap.document.service.AccountsPayableService;
import org.kuali.kfs.module.purap.document.service.PurapService;
import org.kuali.kfs.module.purap.exception.PurError;
import org.kuali.kfs.module.purap.util.ExpiredOrClosedAccountEntry;
import org.kuali.kfs.module.purap.util.PurApItemUtils;
import org.kuali.kfs.module.purap.util.PurApObjectUtils;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kns.util.KualiDecimal;
import org.kuali.rice.kns.util.ObjectUtils;

/**
 * Payment Request Item Business Object.
 */
public class PaymentRequestItem extends AccountsPayableItemBase {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PaymentRequestItem.class);

    private BigDecimal purchaseOrderItemUnitPrice;
    private KualiDecimal itemOutstandingInvoiceQuantity;
    private KualiDecimal itemOutstandingInvoiceAmount;


    /**
     * Default constructor.
     */
    public PaymentRequestItem() {

    }

    /**
     * preq item constructor - Delegate
     * 
     * @param poi - purchase order item
     * @param preq - payment request document
     */
    public PaymentRequestItem(PurchaseOrderItem poi, PaymentRequestDocument preq) {
        this(poi, preq, new HashMap<String, ExpiredOrClosedAccountEntry>());
    }

    /**
     * Constructs a new payment request item, but also merges expired accounts.
     * 
     * @param poi - purchase order item
     * @param preq - payment request document
     * @param expiredOrClosedAccountList - list of expired or closed accounts to merge
     */
    public PaymentRequestItem(PurchaseOrderItem poi, PaymentRequestDocument preq, HashMap<String, ExpiredOrClosedAccountEntry> expiredOrClosedAccountList) {

        // copy base attributes w/ extra array of fields not to be copied
        PurApObjectUtils.populateFromBaseClass(PurApItemBase.class, poi, this, PurapConstants.PREQ_ITEM_UNCOPYABLE_FIELDS);

        // set up accounts
        List accounts = new ArrayList();
        for (PurApAccountingLine account : poi.getSourceAccountingLines()) {
            PurchaseOrderAccount poa = (PurchaseOrderAccount) account;

            // check if this account is expired/closed and replace as needed
            SpringContext.getBean(AccountsPayableService.class).processExpiredOrClosedAccount(poa, expiredOrClosedAccountList);

            accounts.add(new PaymentRequestAccount(this, poa));
        }
        this.setSourceAccountingLines(accounts);

        // clear amount and desc on below the line - we probably don't need that null
        // itemType check but it's there just in case remove if it causes problems
        // also do this if of type service, kulpurap - 1242
        if ((ObjectUtils.isNotNull(this.getItemType()) && !this.getItemType().isQuantityBasedGeneralLedgerIndicator())) {
            // setting unit price to be null to be more consistent with other below the line
            this.setItemUnitPrice(null);

            // if below the line item
            if (!this.getItemType().isItemTypeAboveTheLineIndicator()) {
                this.setItemDescription("");
            }
        }
        
        // copy custom
        this.purchaseOrderItemUnitPrice = poi.getItemUnitPrice();
//        this.purchaseOrderCommodityCode = poi.getPurchaseOrderCommodityCd();
                
        // set doc fields
        this.setPurapDocumentIdentifier(preq.getPurapDocumentIdentifier());
        this.setPurapDocument(preq);
    }

    /**
     * Retreives a purchase order item by inspecting the item type to see if its above the line or below the line and returns the
     * appropriate type.
     * 
     * @return - purchase order item
     */
    public PurchaseOrderItem getPurchaseOrderItem() {
        if (ObjectUtils.isNotNull(this.getPurapDocumentIdentifier())) {
            if (ObjectUtils.isNull(this.getPaymentRequest())) {
                this.refreshReferenceObject(PurapPropertyConstants.PURAP_DOC);
            }
        }
        // ideally we should do this a different way - maybe move it all into the service or save this info somehow (make sure and
        // update though)
        if (getPaymentRequest() != null) {
            PurchaseOrderDocument po = getPaymentRequest().getPurchaseOrderDocument();
            PurchaseOrderItem poi = null;
            if (this.getItemType().isItemTypeAboveTheLineIndicator()) {
                poi = (PurchaseOrderItem) po.getItem(this.getItemLineNumber().intValue() - 1);
                // throw error if line numbers don't match
            }
            else {
                poi = (PurchaseOrderItem) SpringContext.getBean(PurapService.class).getBelowTheLineByType(po, this.getItemType());
            }
            if (poi != null) {
                return poi;
            }
            else {
                LOG.debug("getPurchaseOrderItem() Returning null because PurchaseOrderItem object for line number" + getItemLineNumber() + "or itemType " + getItemTypeCode() + " is null");
                return null;
            }
        }
        else {

            LOG.error("getPurchaseOrderItem() Returning null because paymentRequest object is null");
            throw new PurError("Payment Request Object in Purchase Order item line number " + getItemLineNumber() + "or itemType " + getItemTypeCode() + " is null");
        }
    }

    public KualiDecimal getPoOutstandingAmount() {
        PurchaseOrderItem poi = getPurchaseOrderItem();
        if(ObjectUtils.isNull(this.getPurchaseOrderItemUnitPrice()) || KualiDecimal.ZERO.equals(this.getPurchaseOrderItemUnitPrice())){
            return null;
        }else{
            return this.getPoOutstandingAmount(poi);
        }
    }

    private KualiDecimal getPoOutstandingAmount(PurchaseOrderItem poi) {
        if (poi == null) {
            return KualiDecimal.ZERO;
        }
        else {
            return poi.getItemOutstandingEncumberedAmount();
        }
    }

    public KualiDecimal getPoOriginalAmount() {
        PurchaseOrderItem poi = getPurchaseOrderItem();
        if (poi == null) {
            return null;
        }
        else {
            return poi.getExtendedPrice();
        }
    }

    /**
     * Exists due to a setter requirement by the htmlControlAttribute
     * @deprecated
     * @param amount - po outstanding amount
     */
    public void setPoOutstandingAmount(KualiDecimal amount) {
        // do nothing
    }


    public KualiDecimal getPoOutstandingQuantity() {
        PurchaseOrderItem poi = getPurchaseOrderItem();
        if (poi == null) {
            return null;
        }
        else {
            if(PurapConstants.ItemTypeCodes.ITEM_TYPE_SERVICE_CODE.equals(this.getItemTypeCode())){
                return null;
            }else{
                return poi.getOutstandingQuantity();
            }
        }
    }

    /**
     * Exists due to a setter requirement by the htmlControlAttribute
     * @deprecated
     * @param amount - po outstanding quantity
     */
    public void setPoOutstandingQuantity(KualiDecimal qty) {
        // do nothing
    }

    public BigDecimal getPurchaseOrderItemUnitPrice() {
        return purchaseOrderItemUnitPrice;
    }
    
    public BigDecimal getOriginalAmountfromPO() {
        return purchaseOrderItemUnitPrice;
    }
    
    public void setOriginalAmountfromPO(BigDecimal purchaseOrderItemUnitPrice) {
        // Do nothing
    }

    public void setPurchaseOrderItemUnitPrice(BigDecimal purchaseOrderItemUnitPrice) {
        this.purchaseOrderItemUnitPrice = purchaseOrderItemUnitPrice;
    }

    public KualiDecimal getItemOutstandingInvoiceAmount() {
        return itemOutstandingInvoiceAmount;
    }

    public void setItemOutstandingInvoiceAmount(KualiDecimal itemOutstandingInvoiceAmount) {
        this.itemOutstandingInvoiceAmount = itemOutstandingInvoiceAmount;
    }

    public KualiDecimal getItemOutstandingInvoiceQuantity() {
        return itemOutstandingInvoiceQuantity;
    }

    public void setItemOutstandingInvoiceQuantity(KualiDecimal itemOutstandingInvoiceQuantity) {
        this.itemOutstandingInvoiceQuantity = itemOutstandingInvoiceQuantity;
    }

    public PaymentRequestDocument getPaymentRequest() {
        return super.getPurapDocument();
    }

    public void setPaymentRequest(PaymentRequestDocument paymentRequest) {
        this.setPurapDocument(paymentRequest);
    }

    public void generateAccountListFromPoItemAccounts(List<PurApAccountingLine> accounts) {
        for (PurApAccountingLine line : accounts) {
            PurchaseOrderAccount poa = (PurchaseOrderAccount) line;
            if (!line.isEmpty()) {
                getSourceAccountingLines().add(new PaymentRequestAccount(this, poa));
            }
        }
    }

    /**
     * @see org.kuali.kfs.module.purap.businessobject.PurApItem#getAccountingLineClass()
     */
    public Class getAccountingLineClass() {
        return PaymentRequestAccount.class;
    }

    public boolean isDisplayOnPreq() {
        PurchaseOrderItem poi = getPurchaseOrderItem();
        if (ObjectUtils.isNull(poi)) {
            LOG.debug("poi was null");
            return false;
        }

        // if the po item is not active... skip it
        if (!poi.isItemActiveIndicator()) {
            LOG.debug("poi was not active: " + poi.toString());
            return false;
        }

        ItemType poiType = poi.getItemType();

        if (poiType.isQuantityBasedGeneralLedgerIndicator()) {
            if (poi.getItemQuantity().isGreaterThan(poi.getItemInvoicedTotalQuantity())) {
                return true;
            }
            else {
                if (ObjectUtils.isNotNull(this.getItemQuantity()) && this.getItemQuantity().isGreaterThan(KualiDecimal.ZERO)) {
                    return true;
                }
            }

            return false;
        }
        else { // not quantity based
            if (poi.getItemOutstandingEncumberedAmount().isGreaterThan(KualiDecimal.ZERO)) {
                return true;
            }
            else {
                if (PurApItemUtils.isNonZeroExtended(this)) {
                    return true;
                }
                return false;
            }

        }
    }

    /**
     * sets account line percentage to zero.
     * 
     * @see org.kuali.kfs.module.purap.businessobject.PurApItem#resetAccount()
     */
    @Override
    public void resetAccount() {
        super.resetAccount();
        this.getNewSourceLine().setAccountLinePercent(new BigDecimal(0));
    }

    /**
     * Refreshes payment request object.
     * 
     * @see org.kuali.rice.kns.bo.PersistableBusinessObjectBase#afterLookup(org.apache.ojb.broker.PersistenceBroker)
     */
    @Override
    public void afterLookup(PersistenceBroker persistenceBroker) throws PersistenceBrokerException {
        super.afterLookup(persistenceBroker);
        if (ObjectUtils.isNotNull(this.getPurapDocumentIdentifier())) {
            if (ObjectUtils.isNull(this.getPaymentRequest())) {
                this.refreshReferenceObject(PurapPropertyConstants.PURAP_DOC);
            }
        }
    }

}
