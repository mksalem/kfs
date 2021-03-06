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
package org.kuali.kfs.module.ar.businessobject.lookup;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAward;
import org.kuali.kfs.integration.cg.ContractsAndGrantsModuleBillingService;
import org.kuali.kfs.module.ar.ArConstants;
import org.kuali.kfs.module.ar.businessobject.ContractsGrantsInvoiceLookupResult;
import org.kuali.kfs.module.ar.report.service.ContractsGrantsInvoiceReportService;
import org.kuali.kfs.module.ar.report.service.ContractsGrantsReportHelperService;
import org.kuali.kfs.module.ar.web.ui.ContractsGrantsLookupResultRow;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.core.web.format.Formatter;
import org.kuali.rice.kim.api.KimConstants;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kim.api.services.IdentityManagementService;
import org.kuali.rice.kns.document.authorization.BusinessObjectRestrictions;
import org.kuali.rice.kns.lookup.HtmlData;
import org.kuali.rice.kns.web.comparator.CellComparatorHelper;
import org.kuali.rice.kns.web.struts.form.LookupForm;
import org.kuali.rice.kns.web.ui.Column;
import org.kuali.rice.kns.web.ui.ResultRow;
import org.kuali.rice.krad.bo.BusinessObject;
import org.kuali.rice.krad.bo.PersistableBusinessObjectBase;
import org.kuali.rice.krad.lookup.CollectionIncomplete;
import org.kuali.rice.krad.util.BeanPropertyComparator;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.KRADConstants;
import org.kuali.rice.krad.util.ObjectUtils;

/**
 * Defines a lookupable helper service class for Contracts & Grants Invoices.
 */
public class ContractsGrantsInvoiceLookupableHelperServiceImpl extends AccountsReceivableLookupableHelperServiceImplBase {

    private org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ContractsGrantsInvoiceLookupableHelperServiceImpl.class);
    protected ContractsAndGrantsModuleBillingService contractsAndGrantsModuleBillingService;
    protected ContractsGrantsReportHelperService contractsGrantsReportHelperService;
    protected ContractsGrantsInvoiceReportService contractsGrantsInvoiceReportService;

    /**
     * This method performs the lookup and returns a collection of lookup items
     *
     * @param lookupForm
     * @param kualiLookupable
     * @param resultTable
     * @param bounded
     * @return
     */
    @Override
    public Collection performLookup(LookupForm lookupForm, Collection resultTable, boolean bounded) {
        Collection<BusinessObject> displayList;

        // Call search method to get results - always use unbounded to get the entire set of results.

        displayList = (Collection<BusinessObject>) getSearchResultsUnbounded(lookupForm.getFieldsForLookup());

        List pkNames = getBusinessObjectMetaDataService().listPrimaryKeyFieldNames(getBusinessObjectClass());
        List returnKeys = getReturnKeys();
        Person user = GlobalVariables.getUserSession().getPerson();

        // Iterate through result list and wrap rows with return url and action urls
        for (BusinessObject element : displayList) {
            LOG.debug("Doing lookup for " + element.getClass());

            ContractsGrantsInvoiceLookupResult result = ((ContractsGrantsInvoiceLookupResult) element);
            List<String> awardAttributesForDisplay = result.getAwardAttributesForDisplay();

            BusinessObjectRestrictions businessObjectRestrictions = getBusinessObjectAuthorizationService().getLookupResultRestrictions(result, user);
            // add list of awards to sub Result rows
            List<ResultRow> subResultRows = new ArrayList<ResultRow>();
            for (ContractsAndGrantsBillingAward award : result.getAwards()) {

                List<Column> subResultColumns = new ArrayList<Column>();

                for (String propertyName : awardAttributesForDisplay) {
                    subResultColumns.add(setupResultsColumn(award, propertyName, businessObjectRestrictions));
                }

                ResultRow subResultRow = new ResultRow(subResultColumns, "", "");
                subResultRow.setObjectId(((PersistableBusinessObjectBase) award).getObjectId());
                subResultRows.add(subResultRow);
            }

            // Create main customer header row
            Collection<Column> columns = getColumns(element, businessObjectRestrictions);
            HtmlData returnUrl = getReturnUrl(element, lookupForm, returnKeys, businessObjectRestrictions);
            ContractsGrantsLookupResultRow row = new ContractsGrantsLookupResultRow((List<Column>) columns, subResultRows, returnUrl.constructCompleteHtmlTag(), getActionUrls(element, pkNames, businessObjectRestrictions));
            resultTable.add(row);
        }

        return displayList;
    }

    /**
     * overriding this method to convert the list of awards to a list of ContratcsGrantsInvoiceLookupResult
     *
     * @see org.kuali.core.lookup.Lookupable#getSearchResults(java.util.Map)
     */
    @Override
    public List<? extends BusinessObject> getSearchResultsUnbounded(Map<String, String> fieldValues) {
        Collection searchResultsCollection;
        // Get the list of awards
        searchResultsCollection = getSearchResultsHelper(org.kuali.rice.krad.lookup.LookupUtils.forceUppercase(getBusinessObjectClass(), fieldValues), true);
        // Convert to suitable list
        searchResultsCollection = getContractsGrantsInvoiceReportService().getPopulatedContractsGrantsInvoiceLookupResults(searchResultsCollection);
        filterSearchResults(searchResultsCollection);
        return this.buildSearchResultList(searchResultsCollection, new Long(searchResultsCollection.size()));
    }

    /**
     * build the search result list from the given collection and the number of all qualified search results
     *
     * @param searchResultsCollection the given search results, which may be a subset of the qualified search results
     * @param actualSize the number of all qualified search results
     * @return the search result list with the given results and actual size
     */
    protected List buildSearchResultList(Collection searchResultsCollection, Long actualSize) {
        CollectionIncomplete results = new CollectionIncomplete(searchResultsCollection, actualSize);

        // Sort list if default sort column given
        List searchResults = results;
        List defaultSortColumns = getDefaultSortColumns();
        if (defaultSortColumns.size() > 0) {
            Collections.sort(results, new BeanPropertyComparator(defaultSortColumns, true));
        }
        return searchResults;
    }

    /**
     * @see org.kuali.rice.kns.lookup.KualiLookupableHelperServiceImpl#getSearchResultsHelper(java.util.Map, boolean)
     */
    @Override
    protected List<? extends BusinessObject> getSearchResultsHelper(Map<String, String> fieldValues, boolean unbounded) {
        return contractsAndGrantsModuleBillingService.lookupAwards(fieldValues, unbounded);
    }

    /**
     * Filter out awards the user is not authorized to initiate CINV docs for (not a fund manager for the award) from the search results.
     *
     * @param searchResultsCollection
     */
    protected void filterSearchResults(Collection<ContractsGrantsInvoiceLookupResult> searchResultsCollection) {
        Map<String, String> permissionDetails = new HashMap<String, String>();
        permissionDetails.put(KimConstants.AttributeConstants.DOCUMENT_TYPE_NAME, ArConstants.ArDocumentTypeCodes.CONTRACTS_GRANTS_INVOICE);
        Map<String, String> qualificationDetails = new HashMap<String, String>();

        for (Iterator<ContractsGrantsInvoiceLookupResult> searchResultsIterator = searchResultsCollection.iterator(); searchResultsIterator.hasNext();) {
            ContractsGrantsInvoiceLookupResult contractsGrantsInvoiceLookupResult = searchResultsIterator.next();
            for (Iterator<ContractsAndGrantsBillingAward> awardIterator = contractsGrantsInvoiceLookupResult.getAwards().iterator(); awardIterator.hasNext();) {
                ContractsAndGrantsBillingAward award = awardIterator.next();
                qualificationDetails.put(KFSPropertyConstants.PROPOSAL_NUMBER, award.getProposalNumber().toString());

                if (!SpringContext.getBean(IdentityManagementService.class).isAuthorizedByTemplateName(GlobalVariables.getUserSession().getPrincipalId(), KRADConstants.KUALI_RICE_SYSTEM_NAMESPACE, KimConstants.PermissionTemplateNames.INITIATE_DOCUMENT, permissionDetails, qualificationDetails)) {
                    awardIterator.remove();
                }
            }
            if (contractsGrantsInvoiceLookupResult.getAwards().size() == 0) {
                searchResultsIterator.remove();
            }
        }
    }

    /**
     * @param element
     * @param attributeName
     * @return Column
     */
    protected Column setupResultsColumn(BusinessObject element, String attributeName, BusinessObjectRestrictions businessObjectRestrictions) {
        Column col = new Column();

        col.setPropertyName(attributeName);


        String columnTitle = getDataDictionaryService().getAttributeLabel(element.getClass(), attributeName);
        if (StringUtils.isBlank(columnTitle)) {
            columnTitle = getDataDictionaryService().getCollectionLabel(element.getClass(), attributeName);
        }
        col.setColumnTitle(columnTitle);
        col.setMaxLength(getDataDictionaryService().getAttributeMaxLength(element.getClass(), attributeName));

        try {
            Class formatterClass = getDataDictionaryService().getAttributeFormatter(element.getClass(), attributeName);
            Formatter formatter = null;
            if (formatterClass != null) {
                formatter = (Formatter) formatterClass.newInstance();
                col.setFormatter(formatter);
            }

            // Pick off result column from result list, do formatting
            String propValue = KFSConstants.EMPTY_STRING;
            Object prop = ObjectUtils.getPropertyValue(element, attributeName);

            // Set comparator and formatter based on property type
            Class propClass = null;

            PropertyDescriptor propDescriptor = PropertyUtils.getPropertyDescriptor(element, col.getPropertyName());
            if (propDescriptor != null) {
                propClass = propDescriptor.getPropertyType();
            }

            // Formatters
            if (prop != null) {
                propValue = getContractsGrantsReportHelperService().formatByType(prop, formatter);
            }

            // Comparator
            col.setComparator(CellComparatorHelper.getAppropriateComparatorForPropertyClass(propClass));
            col.setValueComparator(CellComparatorHelper.getAppropriateValueComparatorForPropertyClass(propClass));
            propValue = super.maskValueIfNecessary(element.getClass(), col.getPropertyName(), propValue, businessObjectRestrictions);
            col.setPropertyValue(propValue);

            if (StringUtils.isNotBlank(propValue)) {
                col.setColumnAnchor(getInquiryUrl(element, col.getPropertyName()));
            }
        }
        catch (InstantiationException ie) {
            throw new RuntimeException("Unable to get new instance of formatter class for property " + col.getPropertyName(), ie);
        }
        catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException("Cannot access PropertyType for property " + "'" + col.getPropertyName() + "' " + " on an instance of '" + element.getClass().getName() + "'.", ex);
        }
        return col;
    }


    /**
     * Constructs the list of columns for the search results. All properties for the column objects come from the DataDictionary.
     *
     * @param bo
     * @return Collection<Column>
     */
    protected Collection<Column> getColumns(BusinessObject bo, BusinessObjectRestrictions businessObjectRestrictions) {
        Collection<Column> columns = new ArrayList<Column>();

        for (String attributeName : getBusinessObjectDictionaryService().getLookupResultFieldNames(bo.getClass())) {
            columns.add(setupResultsColumn(bo, attributeName, businessObjectRestrictions));
        }
        return columns;
    }

    public ContractsAndGrantsModuleBillingService getContractsAndGrantsModuleBillingService() {
        return contractsAndGrantsModuleBillingService;
    }

    public void setContractsAndGrantsModuleBillingService(ContractsAndGrantsModuleBillingService contractsAndGrantsModuleBillingService) {
        this.contractsAndGrantsModuleBillingService = contractsAndGrantsModuleBillingService;
    }

    public ContractsGrantsReportHelperService getContractsGrantsReportHelperService() {
        return contractsGrantsReportHelperService;
    }

    public void setContractsGrantsReportHelperService(ContractsGrantsReportHelperService contractsGrantsReportHelperService) {
        this.contractsGrantsReportHelperService = contractsGrantsReportHelperService;
    }

    public ContractsGrantsInvoiceReportService getContractsGrantsInvoiceReportService() {
        return contractsGrantsInvoiceReportService;
    }

    public void setContractsGrantsInvoiceReportService(ContractsGrantsInvoiceReportService contractsGrantsInvoiceReportService) {
        this.contractsGrantsInvoiceReportService = contractsGrantsInvoiceReportService;
    }
}
