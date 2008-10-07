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
package org.kuali.kfs.module.cab.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.coa.businessobject.ObjectCode;
import org.kuali.kfs.fp.businessobject.CapitalAssetInformation;
import org.kuali.kfs.integration.cab.CapitalAssetBuilderAssetTransactionType;
import org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService;
import org.kuali.kfs.integration.cam.CapitalAssetManagementAsset;
import org.kuali.kfs.integration.purap.CapitalAssetLocation;
import org.kuali.kfs.integration.purap.CapitalAssetSystem;
import org.kuali.kfs.integration.purap.ItemCapitalAsset;
import org.kuali.kfs.module.cab.CabConstants;
import org.kuali.kfs.module.cab.CabKeyConstants;
import org.kuali.kfs.module.cab.CabParameterConstants;
import org.kuali.kfs.module.cab.CabPropertyConstants;
import org.kuali.kfs.module.cab.businessobject.AssetTransactionType;
import org.kuali.kfs.module.cab.businessobject.GeneralLedgerEntry;
import org.kuali.kfs.module.cab.businessobject.GeneralLedgerEntryAsset;
import org.kuali.kfs.module.cab.businessobject.PurchasingAccountsPayableDocument;
import org.kuali.kfs.module.cab.businessobject.PurchasingAccountsPayableItemAsset;
import org.kuali.kfs.module.cab.businessobject.PurchasingAccountsPayableLineAssetAccount;
import org.kuali.kfs.module.cam.CamsConstants;
import org.kuali.kfs.module.cam.CamsKeyConstants;
import org.kuali.kfs.module.cam.CamsPropertyConstants;
import org.kuali.kfs.module.cam.businessobject.Asset;
import org.kuali.kfs.module.cam.businessobject.AssetGlobal;
import org.kuali.kfs.module.cam.businessobject.AssetType;
import org.kuali.kfs.module.cam.document.service.AssetService;
import org.kuali.kfs.module.purap.PurapConstants;
import org.kuali.kfs.module.purap.PurapKeyConstants;
import org.kuali.kfs.module.purap.PurapPropertyConstants;
import org.kuali.kfs.module.purap.businessobject.AvailabilityMatrix;
import org.kuali.kfs.module.purap.businessobject.CapitalAssetTransactionTypeRule;
import org.kuali.kfs.module.purap.businessobject.PurApAccountingLine;
import org.kuali.kfs.module.purap.businessobject.PurApItem;
import org.kuali.kfs.module.purap.businessobject.PurchasingCapitalAssetItem;
import org.kuali.kfs.module.purap.businessobject.PurchasingCapitalAssetItemBase;
import org.kuali.kfs.module.purap.businessobject.PurchasingItem;
import org.kuali.kfs.module.purap.businessobject.PurchasingItemBase;
import org.kuali.kfs.module.purap.businessobject.PurchasingItemCapitalAssetBase;
import org.kuali.kfs.module.purap.businessobject.RecurringPaymentType;
import org.kuali.kfs.module.purap.document.service.PurapService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.Building;
import org.kuali.kfs.sys.businessobject.Room;
import org.kuali.kfs.sys.businessobject.SourceAccountingLine;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.ParameterService;
import org.kuali.kfs.sys.service.impl.ParameterConstants;
import org.kuali.rice.kns.bo.Campus;
import org.kuali.rice.kns.bo.Parameter;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.DataDictionaryService;
import org.kuali.rice.kns.service.DictionaryValidationService;
import org.kuali.rice.kns.service.KualiConfigurationService;
import org.kuali.rice.kns.service.KualiModuleService;
import org.kuali.rice.kns.service.LookupService;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.KualiDecimal;
import org.kuali.rice.kns.util.ObjectUtils;


public class CapitalAssetBuilderModuleServiceImpl implements CapitalAssetBuilderModuleService {

    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#validateIndividualCapitalAssetSystemFromPurchasing(java.lang.String,
     *      java.util.List, java.lang.String, java.lang.String)
     */
    public boolean validateIndividualCapitalAssetSystemFromPurchasing(String systemState, List<PurchasingCapitalAssetItem> capitalAssetItems, String chartCode, String documentType) {
        // For Individual Asset system type, the List of CapitalAssetSystems in the input parameter for
        // validateAllFieldRequirementsByChart
        // should be null. So we'll pass in a null here.
        boolean valid = validateAllFieldRequirementsByChart(systemState, null, capitalAssetItems, chartCode, documentType, PurapConstants.CapitalAssetSystemTypes.INDIVIDUAL);
        valid &= validateQuantityOnLocationsEqualsQuantityOnItem(capitalAssetItems, PurapConstants.CapitalAssetSystemTypes.INDIVIDUAL, systemState);
        // TODO : add all the other cams validations according to the specs in here whenever applicable, or, according to the jira :
        // potential validation '
        // against CAMS data (for example, asset # exist in CAMS)
        valid &= validateIndividualSystemPurchasingTransactionTypesAllowingAssetNumbers(capitalAssetItems);
        valid &= validateNonQuantityDrivenAllowedIndicator(capitalAssetItems);
        return valid;
    }

    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#validateOneSystemCapitalAssetSystemFromPurchasing(java.lang.String,
     *      java.util.List, java.util.List, java.lang.String, java.lang.String)
     */
    public boolean validateOneSystemCapitalAssetSystemFromPurchasing(String systemState, List<CapitalAssetSystem> capitalAssetSystems, List<PurchasingCapitalAssetItem> capitalAssetItems, String chartCode, String documentType) {
        boolean valid = validateAllFieldRequirementsByChart(systemState, capitalAssetSystems, capitalAssetItems, chartCode, documentType, PurapConstants.CapitalAssetSystemTypes.ONE_SYSTEM);
        // TODO : add all the other cams validations according to the specs in here whenever applicable, or, according to the jira :
        // potential validation '
        // against CAMS data (for example, asset # exist in CAMS)
        String capitalAssetTransactionType = capitalAssetItems.get(0).getCapitalAssetTransactionTypeCode();
        String prefix = "document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[0].";
        valid &= validatePurchasingTransactionTypesAllowingAssetNumbers(capitalAssetSystems.get(0), capitalAssetTransactionType, prefix);
        valid &= validateNonQuantityDrivenAllowedIndicator(capitalAssetItems);
        return valid;
    }

    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#validateMultipleSystemsCapitalAssetSystemFromPurchasing(java.lang.String,
     *      java.util.List, java.util.List, java.lang.String, java.lang.String)
     */
    public boolean validateMultipleSystemsCapitalAssetSystemFromPurchasing(String systemState, List<CapitalAssetSystem> capitalAssetSystems, List<PurchasingCapitalAssetItem> capitalAssetItems, String chartCode, String documentType) {
        boolean valid = validateAllFieldRequirementsByChart(systemState, capitalAssetSystems, capitalAssetItems, chartCode, documentType, PurapConstants.CapitalAssetSystemTypes.MULTIPLE);
        // TODO : add all the other cams validations according to the specs in here whenever applicable, or, according to the jira :
        // potential validation '
        // against CAMS data (for example, asset # exist in CAMS)
        String capitalAssetTransactionType = capitalAssetItems.get(0).getCapitalAssetTransactionTypeCode();
        String prefix = "document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[0].";
        valid &= validatePurchasingTransactionTypesAllowingAssetNumbers(capitalAssetSystems.get(0), capitalAssetTransactionType, prefix);
        valid &= validateNonQuantityDrivenAllowedIndicator(capitalAssetItems);
        return valid;
    }

    /**
     * @see org.kuali.kfs.integration.service.CapitalAssetBuilderModuleService#doesDocumentExceedThreshold(org.kuali.rice.kns.util.KualiDecimal)
     */
    public boolean doesDocumentExceedThreshold(KualiDecimal docTotal) {
        String parameterThreshold = this.getParameterService().getParameterValue(AssetGlobal.class, CamsConstants.Parameters.CAPITALIZATION_LIMIT_AMOUNT);
        if (docTotal.compareTo(new KualiDecimal(parameterThreshold)) > 0) {
            return true;
        }
        return false;
    }

    /**
     * @see org.kuali.kfs.integration.service.CapitalAssetBuilderModuleService#validateAccounts(java.util.List, java.lang.String)
     */
    public boolean validateAccounts(List<SourceAccountingLine> accountingLines, String transactionType) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Validates all the field requirements by chart. It obtains a List of parameters where the parameter names are like
     * "CHARTS_REQUIRING%" then loop through these parameters. If the system type is individual then invoke the
     * validateFieldRequirementByChartForIndividualSystemType for further validation, otherwise invoke the
     * validateFieldRequirementByChartForOneOrMultipleSystemType for further validation.
     * 
     * @param systemState
     * @param capitalAssetSystems
     * @param capitalAssetItems
     * @param chartCode
     * @param documentType
     * @param systemType
     * @return
     */
    private boolean validateAllFieldRequirementsByChart(String systemState, List<CapitalAssetSystem> capitalAssetSystems, List<PurchasingCapitalAssetItem> capitalAssetItems, String chartCode, String documentType, String systemType) {
        boolean valid = true;
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(CabPropertyConstants.Parameter.PARAMETER_NAMESPACE_CODE, CabConstants.Parameters.NAMESPACE);
        fieldValues.put(CabPropertyConstants.Parameter.PARAMETER_DETAIL_TYPE_CODE, CabConstants.Parameters.DETAIL_TYPE_DOCUMENT);
        String name = "CHARTS_REQUIRING%" + documentType;
        fieldValues.put(CabPropertyConstants.Parameter.PARAMETER_NAME, name);
        // TODO: KULPURAP-2837, Find a more permanent home for the codes for handling "LIKE" criterias. This works fine for now
        // to serve its purpose, although this is probably not the permanent location (ask Chris).
        List<Parameter> results = SpringContext.getBean(PurapService.class).getParametersGivenLikeCriteria(fieldValues);

        for (Parameter parameter : results) {
            if (systemType.equals(PurapConstants.CapitalAssetSystemTypes.INDIVIDUAL)) {
                valid &= validateFieldRequirementByChartForIndividualSystemType(systemState, capitalAssetItems, chartCode, parameter.getParameterName(), parameter.getParameterValue());
            }
            else {
                valid &= validateFieldRequirementByChartForOneOrMultipleSystemType(systemType, systemState, capitalAssetSystems, capitalAssetItems, chartCode, parameter.getParameterName(), parameter.getParameterValue());
            }
        }
        return valid;
    }

    /**
     * Validates field requirement by chart for one or multiple system types.
     * 
     * @param systemType
     * @param systemState
     * @param capitalAssetSystems
     * @param capitalAssetItems
     * @param chartCode
     * @param parameterName
     * @param parameterValueString
     * @return
     */
    private boolean validateFieldRequirementByChartForOneOrMultipleSystemType(String systemType, String systemState, List<CapitalAssetSystem> capitalAssetSystems, List<PurchasingCapitalAssetItem> capitalAssetItems, String chartCode, String parameterName, String parameterValueString) {
        boolean valid = true;
        boolean needValidation = (this.getParameterService().getParameterEvaluator(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, parameterName, chartCode).evaluationSucceeds());

        if (needValidation) {
            if (parameterName.startsWith("CHARTS_REQUIRING_LOCATIONS_ADDRESS")) {
                return validateCapitalAssetLocationAddressFieldsOneOrMultipleSystemType(capitalAssetSystems);
            }
            String mappedName = PurapConstants.CAMS_REQUIREDNESS_FIELDS.REQUIREDNESS_FIELDS_BY_PARAMETER_NAMES.get(parameterName);
            if (mappedName != null) {
                // Check the availability matrix here, if this field doesn't exist according to the avail. matrix, then no need
                // to validate any further.
                String availableValue = getValueFromAvailabilityMatrix(mappedName, systemType, systemState);
                if (availableValue.equals(PurapConstants.CapitalAssetAvailability.NONE)) {
                    return true;
                }

                // capitalAssetTransactionType field is off the item
                if (mappedName.equals(PurapPropertyConstants.CAPITAL_ASSET_TRANSACTION_TYPE_CODE)) {
                    String[] mappedNames = { PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS, mappedName };

                    for (PurchasingCapitalAssetItem item : capitalAssetItems) {
                        StringBuffer keyBuffer = new StringBuffer("document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[" + new Integer(item.getPurchasingItem().getItemLineNumber().intValue() - 1) + "].");
                        valid &= validateFieldRequirementByChartHelper(item, ArrayUtils.subarray(mappedNames, 1, mappedNames.length), keyBuffer, item.getPurchasingItem().getItemLineNumber());
                    }
                }
                // all the other fields are off the system.
                else {
                    List<String> mappedNamesList = new ArrayList<String>();
                    mappedNamesList.add(PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_SYSTEMS);
                    if (mappedName.indexOf(".") < 0) {
                        mappedNamesList.add(mappedName);
                    }
                    else {
                        mappedNamesList.addAll(mappedNameSplitter(mappedName));
                    }
                    // For One system type, we would only have 1 CapitalAssetSystem, however, for multiple we may have more than
                    // one systems in the future. Either way, this for loop should allow both the one system and multiple system
                    // types to work fine.
                    int count = 0;
                    for (CapitalAssetSystem system : capitalAssetSystems) {
                        StringBuffer keyBuffer = new StringBuffer("document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_SYSTEMS + "[" + new Integer(count) + "].");
                        valid &= validateFieldRequirementByChartHelper(system, ArrayUtils.subarray(mappedNamesList.toArray(), 1, mappedNamesList.size()), keyBuffer, null);
                        count++;
                    }
                }
            }
        }
        return valid;
    }

    /**
     * Validates the field requirement by chart for individual system type.
     * 
     * @param systemState
     * @param capitalAssetItems
     * @param chartCode
     * @param parameterName
     * @param parameterValueString
     * @return
     */
    private boolean validateFieldRequirementByChartForIndividualSystemType(String systemState, List<PurchasingCapitalAssetItem> capitalAssetItems, String chartCode, String parameterName, String parameterValueString) {
        boolean valid = true;
        boolean needValidation = (this.getParameterService().getParameterEvaluator(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, parameterName, chartCode).evaluationSucceeds());

        if (needValidation) {
            if (parameterName.startsWith("CHARTS_REQUIRING_LOCATIONS_ADDRESS")) {
                return validateCapitalAssetLocationAddressFieldsForIndividualSystemType(capitalAssetItems);
            }
            String mappedName = PurapConstants.CAMS_REQUIREDNESS_FIELDS.REQUIREDNESS_FIELDS_BY_PARAMETER_NAMES.get(parameterName);
            if (mappedName != null) {
                // Check the availability matrix here, if this field doesn't exist according to the avail. matrix, then no need
                // to validate any further.
                String availableValue = getValueFromAvailabilityMatrix(mappedName, PurapConstants.CapitalAssetSystemTypes.INDIVIDUAL, systemState);
                if (availableValue.equals(PurapConstants.CapitalAssetAvailability.NONE)) {
                    return true;
                }

                // capitalAssetTransactionType field is off the item
                List<String> mappedNamesList = new ArrayList<String>();

                if (mappedName.equals(PurapPropertyConstants.CAPITAL_ASSET_TRANSACTION_TYPE_CODE)) {
                    mappedNamesList.add(PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS);
                    mappedNamesList.add(mappedName);

                }
                // all the other fields are off the system which is off the item
                else {
                    mappedNamesList.add(PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS);
                    mappedNamesList.add(PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_SYSTEM);
                    if (mappedName.indexOf(".") < 0) {
                        mappedNamesList.add(mappedName);
                    }
                    else {
                        mappedNamesList.addAll(mappedNameSplitter(mappedName));
                    }
                }
                // For Individual system type, we'll always iterate through the item, then if the field is off the system, we'll get
                // it through
                // the purchasingCapitalAssetSystem of the item.
                for (PurchasingCapitalAssetItem item : capitalAssetItems) {
                    StringBuffer keyBuffer = new StringBuffer("document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[" + new Integer(item.getPurchasingItem().getItemLineNumber().intValue() - 1) + "].");
                    valid &= validateFieldRequirementByChartHelper(item, ArrayUtils.subarray(mappedNamesList.toArray(), 1, mappedNamesList.size()), keyBuffer, item.getPurchasingItem().getItemLineNumber());
                }
            }
        }
        return valid;
    }

    /**
     * Utility method to split a long String using the "." as the delimiter then add each of the array element into a List of String
     * and return the List of String.
     * 
     * @param mappedName The String to be splitted.
     * @return The List of String after being splitted, with the "." as delimiter.
     */
    private List<String> mappedNameSplitter(String mappedName) {
        List<String> result = new ArrayList<String>();
        String[] mappedNamesArray = mappedName.split("\\.");
        for (int i = 0; i < mappedNamesArray.length; i++) {
            result.add(mappedNamesArray[i]);
        }
        return result;
    }

    /**
     * Validates the field requirement by chart recursively and give error messages when it returns false.
     * 
     * @param bean The object to be used to obtain the property value
     * @param mappedNames The array of Strings which when combined together, they form the field property
     * @param errorKey The error key to be used for adding error messages to the error map.
     * @param itemNumber The Integer that represents the item number that we're currently iterating.
     * @return true if it passes the validation.
     */
    private boolean validateFieldRequirementByChartHelper(Object bean, Object[] mappedNames, StringBuffer errorKey, Integer itemNumber) {
        boolean valid = true;
        Object value = ObjectUtils.getPropertyValue(bean, (String) mappedNames[0]);
        if (ObjectUtils.isNull(value)) {
            errorKey.append(mappedNames[0]);
            String fieldName = SpringContext.getBean(DataDictionaryService.class).getAttributeErrorLabel(bean.getClass(), (String) mappedNames[0]);
            if (itemNumber != null) {
                fieldName = fieldName + " in Item " + itemNumber;
            }
            GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, fieldName);
            return false;
        }
        else if (value instanceof Collection) {
            if (((Collection) value).isEmpty()) {
                // if this collection doesn't contain anything, when it's supposed to contain some strings with values, return
                // false.
                errorKey.append(mappedNames[0]);
                GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, (String) mappedNames[0]);
                return false;
            }
            int count = 0;
            for (Iterator iter = ((Collection) value).iterator(); iter.hasNext();) {
                errorKey.append(mappedNames[0] + "[" + count + "].");
                count++;
                valid &= validateFieldRequirementByChartHelper(iter.next(), ArrayUtils.subarray(mappedNames, 1, mappedNames.length), errorKey, itemNumber);
            }
            return valid;
        }
        else if (StringUtils.isBlank(value.toString())) {
            errorKey.append(mappedNames[0]);
            GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, (String) mappedNames[0]);
            return false;
        }
        else if (mappedNames.length > 1) {
            // this means we have not reached the end of a single field to be traversed yet, so continue with the recursion.
            errorKey.append(mappedNames[0]).append(".");
            valid &= validateFieldRequirementByChartHelper(value, ArrayUtils.subarray(mappedNames, 1, mappedNames.length), errorKey, itemNumber);
            return valid;
        }
        else {
            return true;
        }
    }

    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#getAllAssetTransactionTypes()
     */
    public List<CapitalAssetBuilderAssetTransactionType> getAllAssetTransactionTypes() {
        Class<? extends CapitalAssetBuilderAssetTransactionType> assetTransactionTypeClass = this.getKualiModuleService().getResponsibleModuleService(CapitalAssetBuilderAssetTransactionType.class).getExternalizableBusinessObjectImplementation(CapitalAssetBuilderAssetTransactionType.class);

        return (List<CapitalAssetBuilderAssetTransactionType>) this.getBusinessObjectService().findAll(assetTransactionTypeClass);
    }

    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#getValueFromAvailabilityMatrix(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public String getValueFromAvailabilityMatrix(String fieldName, String systemType, String systemState) {
        for (AvailabilityMatrix am : PurapConstants.CAMS_AVAILABILITY_MATRIX.MATRIX_LIST) {
            if (am.fieldName.equals(fieldName) && am.systemState.equals(systemState) && am.systemType.equals(systemType)) {
                return am.availableValue;
            }
        }
        // if we can't find any matching from availability matrix, return null for now.
        return null;
    }

    /**
     * Validates that the total quantity on all locations equals to the quantity on the line item. This is only used for IND system
     * type.
     * 
     * @param capitalAssetItems
     * @return true if the total quantity on all locations equals to the quantity on the line item.
     */
    private boolean validateQuantityOnLocationsEqualsQuantityOnItem(List<PurchasingCapitalAssetItem> capitalAssetItems, String systemType, String systemState) {
        boolean valid = true;
        String availableValue = getValueFromAvailabilityMatrix(PurapPropertyConstants.CAPITAL_ASSET_LOCATIONS + "." + PurapPropertyConstants.QUANTITY, systemType, systemState);
        if (availableValue.equals(PurapConstants.CapitalAssetAvailability.NONE)) {
            // If the location quantity isn't available on the document given the system type and system state, we don't need to
            // validate this, just return true.
            return true;
        }
        int count = 0;
        for (PurchasingCapitalAssetItem item : capitalAssetItems) {
            if (item.getPurchasingItem() != null && !item.getPurchasingItem().getItemTypeCode().equals(PurapConstants.ItemTypeCodes.ITEM_TYPE_SERVICE_CODE)) {
                KualiDecimal total = new KualiDecimal(0);
                for (CapitalAssetLocation location : item.getPurchasingCapitalAssetSystem().getCapitalAssetLocations()) {
                    total = total.add(location.getItemQuantity());
                }
                if (!item.getPurchasingItem().getItemQuantity().equals(total)) {
                    valid = false;
                    String errorKey = PurapKeyConstants.ERROR_CAPITAL_ASSET_LOCATIONS_QUANTITY_MUST_EQUAL_ITEM_QUANTITY;
                    String propertyName = "document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[" + count + "]." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_SYSTEM + ".newPurchasingCapitalAssetLocationLine." + PurapPropertyConstants.QUANTITY;
                    GlobalVariables.getErrorMap().putError(propertyName, errorKey, Integer.toString(count + 1));
                }
            }
            count++;
        }

        return valid;
    }

    /**
     * Validates for the individual system type that for each of the items, the capitalAssetTransactionTypeCode matches the system
     * parameter PURCHASING_ASSET_TRANSACTION_TYPES_ALLOWING_ASSET_NUMBERS, the method will return true but if it doesn't match the
     * system parameter, then loop through each of the itemCapitalAssets. If there is any non-null capitalAssetNumber then return
     * false.
     * 
     * @param capitalAssetItems the List of PurchasingCapitalAssetItems on the document to be validated
     * @return false if the capital asset transaction type does not match the system parameter that allows asset numbers but the
     *         itemCapitalAsset contains at least one asset numbers.
     */
    private boolean validateIndividualSystemPurchasingTransactionTypesAllowingAssetNumbers(List<PurchasingCapitalAssetItem> capitalAssetItems) {
        boolean valid = true;
        int count = 0;
        for (PurchasingCapitalAssetItem capitalAssetItem : capitalAssetItems) {
            String prefix = "document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[" + count + "].";
            valid &= validatePurchasingTransactionTypesAllowingAssetNumbers(capitalAssetItem.getPurchasingCapitalAssetSystem(), capitalAssetItem.getCapitalAssetTransactionTypeCode(), prefix);
            count++;
        }
        return valid;
    }

    /**
     * Generic validation that if the capitalAssetTransactionTypeCode does not match the system parameter
     * PURCHASING_ASSET_TRANSACTION_TYPES_ALLOWING_ASSET_NUMBERS and at least one of the itemCapitalAssets contain a non-null
     * capitalAssetNumber then return false. This method is used by one system and multiple system types as well as being used as a
     * helper method for validateIndividualSystemPurchasingTransactionTypesAllowingAssetNumbers method.
     * 
     * @param capitalAssetSystem the capitalAssetSystem containing a List of itemCapitalAssets to be validated.
     * @param capitalAssetTransactionType the capitalAssetTransactionTypeCode containing asset numbers to be validated.
     * @return false if the capital asset transaction type does not match the system parameter that allows asset numbers but the
     *         itemCapitalAsset contains at least one asset numbers.
     */
    private boolean validatePurchasingTransactionTypesAllowingAssetNumbers(CapitalAssetSystem capitalAssetSystem, String capitalAssetTransactionType, String prefix) {
        String parameterName = CabParameterConstants.CapitalAsset.PURCHASING_ASSET_TRANSACTION_TYPES_ALLOWING_ASSET_NUMBERS;
        boolean allowedAssetNumbers = (this.getParameterService().getParameterEvaluator(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, parameterName, capitalAssetTransactionType).evaluationSucceeds());
        if (allowedAssetNumbers) {
            // If this is a transaction type that allows asset numbers, we don't need to validate anymore, just return true here.
            return true;
        }
        else {
            for (ItemCapitalAsset asset : capitalAssetSystem.getItemCapitalAssets()) {
                if (asset.getCapitalAssetNumber() != null) {
                    String propertyName = prefix + PurapPropertyConstants.CAPITAL_ASSET_TRANSACTION_TYPE_CODE;
                    GlobalVariables.getErrorMap().putError(propertyName, PurapKeyConstants.ERROR_CAPITAL_ASSET_ASSET_NUMBERS_NOT_ALLOWED_TRANS_TYPE, capitalAssetTransactionType);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validates that if the non quantity drive allowed indicator on the capital asset transaction type is false and the quantity on
     * any of the line items is either null or 0 then return false.
     * 
     * @param capitalAssetItems The List of PurchasingCapitalAssetItem to be validated.
     * @return false if the indicator is false and there is at least one non quantity items on the list.
     */
    private boolean validateNonQuantityDrivenAllowedIndicator(List<PurchasingCapitalAssetItem> capitalAssetItems) {
        boolean valid = true;
        int count = 0;
        for (PurchasingCapitalAssetItem capitalAssetItem : capitalAssetItems) {
            if (StringUtils.isNotBlank(capitalAssetItem.getCapitalAssetTransactionTypeCode())) {

                ((PurchasingCapitalAssetItemBase) capitalAssetItem).refreshReferenceObject(PurapPropertyConstants.CAPITAL_ASSET_TRANSACTION_TYPE);

                if (!capitalAssetItem.getCapitalAssetTransactionType().getCapitalAssetNonquantityDrivenAllowIndicator()) {
                    if (capitalAssetItem.getPurchasingItem().getItemTypeCode().equals(PurapConstants.ItemTypeCodes.ITEM_TYPE_SERVICE_CODE)) {
                        String prefix = "document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[" + count + "].";
                        String propertyName = prefix + PurapPropertyConstants.CAPITAL_ASSET_TRANSACTION_TYPE_CODE;
                        GlobalVariables.getErrorMap().putError(propertyName, PurapKeyConstants.ERROR_CAPITAL_ASSET_TRANS_TYPE_NOT_ALLOWING_NON_QUANTITY_ITEMS, capitalAssetItem.getCapitalAssetTransactionTypeCode());
                        valid &= false;
                    }
                }
            }
            count++;
        }
        return valid;
    }

    // -------- KULPURAP 2795 methods start here.

    /**
     * Wrapper to do Capital Asset validations, generating errors instead of warnings. Makes sure that the given item's data
     * relevant to its later possible classification as a Capital Asset is internally consistent, by marshaling and calling the
     * methods marked as Capital Asset validations. This implementation assumes that all object codes are valid (real) object codes.
     * 
     * @param recurringPaymentType The item's document's RecurringPaymentType
     * @param item A PurchasingItemBase object
     * @param apoCheck True if this check is for APO purposes
     * @return True if the item passes all Capital Asset validations
     */
    public boolean validateItemCapitalAssetWithErrors(RecurringPaymentType recurringPaymentType, PurApItem item, boolean apoCheck) {
        PurchasingItemBase purchasingItem = (PurchasingItemBase) item;
        List<String> previousErrorPath = GlobalVariables.getErrorMap().getErrorPath();
        GlobalVariables.getErrorMap().clearErrorPath();
        GlobalVariables.getErrorMap().addToErrorPath(PurapConstants.CAPITAL_ASSET_TAB_ERRORS);
        boolean result = validateItemCapitalAsset(recurringPaymentType, purchasingItem, false);

        // Now that we're done with cams related validations, reset the error path to what it was previously.
        GlobalVariables.getErrorMap().clearErrorPath();
        for (String path : previousErrorPath) {
            GlobalVariables.getErrorMap().addToErrorPath(path);
        }
        return result;
    }

    /**
     * Wrapper to do Capital Asset validations, generating warnings instead of errors. Makes sure that the given item's data
     * relevant to its later possible classification as a Capital Asset is internally consistent, by marshaling and calling the
     * methods marked as Capital Asset validations. This implementation assumes that all object codes are valid (real) object codes.
     * 
     * @param recurringPaymentType The item's document's RecurringPaymentType
     * @param item A PurchasingItemBase object
     * @return True if the item passes all Capital Asset validations
     */
    public boolean validateItemCapitalAssetWithWarnings(RecurringPaymentType recurringPaymentType, PurApItem item) {
        PurchasingItemBase purchasingItem = (PurchasingItemBase) item;
        return validateItemCapitalAsset(recurringPaymentType, purchasingItem, true);
    }

    /**
     * Makes sure that the given item's data relevant to its later possible classification as a Capital Asset is internally
     * consistent, by marshaling and calling the methods marked as Capital Asset validations. This implementation assumes that all
     * object codes are valid (real) object codes.
     * 
     * @param recurringPaymentType The item's document's RecurringPaymentType
     * @param item A PurchasingItemBase object
     * @param warn A boolean which should be set to true if warnings are to be set on the calling document for most of the
     *        validations, rather than errors.
     * @return True if the item passes all Capital Asset validations
     */
    protected boolean validateItemCapitalAsset(RecurringPaymentType recurringPaymentType, PurchasingItemBase item, boolean warn) {
        boolean valid = true;
        String itemIdentifier = item.getItemIdentifierString();
        boolean quantityBased = item.getItemType().isQuantityBasedGeneralLedgerIndicator();
        KualiDecimal itemQuantity = item.getItemQuantity();
        HashSet<String> capitalOrExpenseSet = new HashSet<String>(); // For the first validation on every accounting line.

        String capitalAssetTransactionTypeCode = "";
        AssetTransactionType capitalAssetTransactionType = null;

        if (item.getPurchasingCapitalAssetItem() != null) {
            capitalAssetTransactionTypeCode = item.getPurchasingCapitalAssetItem().getCapitalAssetTransactionTypeCode();
            ((PurchasingCapitalAssetItemBase) item.getPurchasingCapitalAssetItem()).refreshReferenceObject(PurapPropertyConstants.CAPITAL_ASSET_TRANSACTION_TYPE);
            capitalAssetTransactionType = (AssetTransactionType) item.getPurchasingCapitalAssetItem().getCapitalAssetTransactionType();
        }

        // Do the checks that depend on Accounting Line information.
        for (PurApAccountingLine accountingLine : item.getSourceAccountingLines()) {
            // Because of ObjectCodeCurrent, we had to refresh this.
            accountingLine.refreshReferenceObject(KFSPropertyConstants.OBJECT_CODE);
            ObjectCode objectCode = accountingLine.getObjectCode();
            String capitalOrExpense = objectCodeCapitalOrExpense(objectCode);
            capitalOrExpenseSet.add(capitalOrExpense); // HashSets accumulate distinct values (and nulls) only.

            valid &= validateAccountingLinesNotCapitalAndExpense(capitalOrExpenseSet, warn, itemIdentifier, objectCode);
            if (warn) {
                valid &= validateLevelCapitalAssetIndication(itemQuantity, item.getExtendedPrice(), objectCode, itemIdentifier);
            }
            else {
                validateLevelCapitalAssetIndication(itemQuantity, item.getExtendedPrice(), objectCode, itemIdentifier);
            }

            // Do the checks involving capital asset transaction type.
            if (!StringUtils.isEmpty(capitalAssetTransactionTypeCode)) {
                valid &= validateObjectCodeVersusTransactionType(objectCode, capitalAssetTransactionType, warn, itemIdentifier, quantityBased);
            }
        }
        // These checks do not depend on Accounting Line information, but do depend on transaction type.
        if (!StringUtils.isEmpty(capitalAssetTransactionTypeCode)) {
            valid &= validateCapitalAssetTransactionTypeVersusRecurrence(capitalAssetTransactionType, recurringPaymentType, warn, itemIdentifier);
        }
        return valid;
    }

    /**
     * Capital Asset validation: An item cannot have among its associated accounting lines both object codes that indicate it is a
     * Capital Asset, and object codes that indicate that the item is not a Capital Asset. Whether an object code indicates that the
     * item is a Capital Asset is determined by whether its level is among a specific set of levels that are deemed acceptable for
     * such items.
     * 
     * @param capitalOrExpenseSet A HashSet containing the distinct values of either "Capital" or "Expense" that have been added to
     *        it.
     * @param warn A boolean which should be set to true if warnings are to be set on the calling document
     * @param itemIdentifier A String identifying the item for error display
     * @param objectCode An ObjectCode, for error display
     * @return True if the given HashSet contains at most one of either "Capital" or "Expense"
     */
    public boolean validateAccountingLinesNotCapitalAndExpense(HashSet<String> capitalOrExpenseSet, boolean warn, String itemIdentifier, ObjectCode objectCode) {
        boolean valid = true;
        // If the set contains more than one distinct string, fail.
        if (capitalOrExpenseSet.size() > 1) {
            if (warn) {
                String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.ERROR_ITEM_CAPITAL_AND_EXPENSE);
                warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                warning = StringUtils.replace(warning, "{1}", objectCode.getFinancialObjectCodeName());
                GlobalVariables.getMessageList().add(warning);
            }
            else {
                GlobalVariables.getErrorMap().putError(KFSConstants.FINANCIAL_OBJECT_LEVEL_CODE_PROPERTY_NAME, CabKeyConstants.ERROR_ITEM_CAPITAL_AND_EXPENSE, itemIdentifier, objectCode.getFinancialObjectCodeName());
            }
            valid &= false;
        }
        return valid;
    }

    /**
     * Gets the distinct object code sub-types that exist in the AssetTransactionTypeRule table.
     * 
     * @return A HashSet containing the distinct Object Code Sub-types
     */
    // TODO: - delete this method, I think it's unused.
    // private HashSet<String> getAssetTransactionTypeDistinctObjectCodeSubtypes() {
    // HashSet<String> objectCodeSubtypesInTable = new HashSet<String>();
    // HashMap<String, String> dummyMap = new HashMap<String, String>();
    // List<CapitalAssetTransactionTypeRule> allRelations = (List<CapitalAssetTransactionTypeRule>)
    // SpringContext.getBean(LookupService.class).findCollectionBySearch(CapitalAssetTransactionTypeRule.class, dummyMap);
    // for (CapitalAssetTransactionTypeRule relation : allRelations) {
    // // Add sub-type codes if not already there.
    // objectCodeSubtypesInTable.add(relation.getFinancialObjectSubTypeCode());
    // }
    //
    // return objectCodeSubtypesInTable;
    // }
    /**
     * Capital Asset validation: If the item has a quantity, and has an extended price greater than or equal to the threshold for
     * becoming a Capital Asset, and the object code has one of a list of levels related to capital assets, and indicating the
     * possibility of the item being a capital asset, rather than an actual Capital Asset level, a warning should be given that a
     * Capital Asset level should be used. Failure of this validation gives a warning both at the Requisition stage and at the
     * Purchase Order stage.
     * 
     * @param itemQuantity The quantity as a KualiDecimal
     * @param extendedPrice The extended price as a KualiDecimal
     * @param objectCode The ObjectCode
     * @param itemIdentifier A String identifying the item
     * @return False if the validation fails.
     */
    public boolean validateLevelCapitalAssetIndication(KualiDecimal itemQuantity, KualiDecimal extendedPrice, ObjectCode objectCode, String itemIdentifier) {
        boolean valid = true;
        if ((itemQuantity != null) && (itemQuantity.isGreaterThan(KualiDecimal.ZERO))) {
            String capitalAssetPriceThreshold = this.getParameterService().getParameterValue(AssetGlobal.class, CabParameterConstants.CapitalAsset.CAPITAL_ASSET_PRICE_THRESHOLD);
            if ((extendedPrice != null) && (StringUtils.isNotEmpty(capitalAssetPriceThreshold)) && (extendedPrice.isGreaterEqual(new KualiDecimal(capitalAssetPriceThreshold)))) {

                String possiblyCapitalAssetObjectCodeLevels = "";
                try {
                    possiblyCapitalAssetObjectCodeLevels = this.getParameterService().getParameterValue(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, CabParameterConstants.CapitalAsset.POSSIBLE_CAPITAL_ASSET_OBJECT_LEVELS);
                    if (StringUtils.contains(possiblyCapitalAssetObjectCodeLevels, objectCode.getFinancialObjectLevel().getFinancialObjectLevelCode())) {
                        valid &= false;
                    }
                }
                catch (NullPointerException npe) {
                    valid &= false;
                }
                if (!valid) {
                    String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.WARNING_ABOVE_THRESHOLD_SUGESTS_CAPITAL_ASSET_LEVEL);
                    warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                    warning = StringUtils.replace(warning, "{1}", capitalAssetPriceThreshold);
                    GlobalVariables.getMessageList().add(warning);
                }
            }
        }

        return valid;
    }

    /**
     * Capital Asset validation: If the item has a transaction type, check that the transaction type is acceptable for the object
     * code sub-types of all the object codes on the associated accounting lines.
     * 
     * @param objectCode
     * @param capitalAssetTransactionType
     * @param warn A boolean which should be set to true if warnings are to be set on the calling document
     * @param itemIdentifier
     * @return
     */
    // TODO: - delete commented code below
    public boolean validateObjectCodeVersusTransactionType(ObjectCode objectCode, CapitalAssetBuilderAssetTransactionType capitalAssetTransactionType, boolean warn, String itemIdentifier, boolean quantityBasedItem) {
        boolean valid = true;
        // HashMap<String, String> tranTypeMap = new HashMap<String, String>();
        // tranTypeMap.put(PurapPropertyConstants.ITEM_CAPITAL_ASSET_TRANSACTION_TYPE_CODE,
        // capitalAssetTransactionType.getCapitalAssetTransactionTypeCode());
        // List<CapitalAssetTransactionTypeRule> relevantRelations = (List<CapitalAssetTransactionTypeRule>)
        // SpringContext.getBean(LookupService.class).findCollectionBySearch(CapitalAssetTransactionTypeRule.class, tranTypeMap);
        String[] objectCodeSubTypes = {};


        if (quantityBasedItem) {
            objectCodeSubTypes = StringUtils.split(capitalAssetTransactionType.getCapitalAssetQuantitySubtypeRequiredText(), ";");
        }
        else {
            objectCodeSubTypes = StringUtils.split(capitalAssetTransactionType.getCapitalAssetNonquantitySubtypeRequiredText(), ";");
        }

        boolean found = false;
        for (String subType : objectCodeSubTypes) {
            if (StringUtils.equals(subType, objectCode.getFinancialObjectSubTypeCode())) {
                found = true;
                break;
            }
        }


        // for (CapitalAssetTransactionTypeRule relation : relevantRelations) {
        // if (StringUtils.equals(relation.getFinancialObjectSubTypeCode(), objectCode.getFinancialObjectSubTypeCode())) {
        // found = true;
        // break;
        // }
        // }
        if (!found) {
            if (warn) {
                String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.ERROR_ITEM_TRAN_TYPE_OBJECT_CODE_SUBTYPE);
                warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                warning = StringUtils.replace(warning, "{1}", capitalAssetTransactionType.getCapitalAssetTransactionTypeDescription());
                warning = StringUtils.replace(warning, "{2}", objectCode.getFinancialObjectCodeName());
                GlobalVariables.getMessageList().add(warning);
            }
            else {
                GlobalVariables.getErrorMap().putError(PurapPropertyConstants.ITEM_CAPITAL_ASSET_TRANSACTION_TYPE, CabKeyConstants.ERROR_ITEM_TRAN_TYPE_OBJECT_CODE_SUBTYPE, itemIdentifier, capitalAssetTransactionType.getCapitalAssetTransactionTypeDescription(), objectCode.getFinancialObjectCodeName(), objectCode.getFinancialObjectSubType().getFinancialObjectSubTypeName());
            }
            valid &= false;
        }
        return valid;
    }

    /**
     * Capital Asset validation: If the item has a transaction type, check that if the document specifies that recurring payments
     * are to be made, that the transaction type is one that is appropriate for this situation, and that if the document does not
     * specify that recurring payments are to be made, that the transaction type is one that is appropriate for that situation.
     * 
     * @param capitalAssetTransactionType
     * @param recurringPaymentType
     * @param warn A boolean which should be set to true if warnings are to be set on the calling document
     * @param itemIdentifier
     * @return
     */
    public boolean validateCapitalAssetTransactionTypeVersusRecurrence(CapitalAssetBuilderAssetTransactionType capitalAssetTransactionType, RecurringPaymentType recurringPaymentType, boolean warn, String itemIdentifier) {
        boolean valid = true;

        // If there is a tran type ...
        if ((capitalAssetTransactionType != null) && (capitalAssetTransactionType.getCapitalAssetTransactionTypeCode() != null)) {
            String recurringTransactionTypeCodes = this.getParameterService().getParameterValue(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, CabParameterConstants.CapitalAsset.RECURRING_CAMS_TRAN_TYPES);


            if (recurringPaymentType != null) { // If there is a recurring payment type ...
                if (!StringUtils.contains(recurringTransactionTypeCodes, capitalAssetTransactionType.getCapitalAssetTransactionTypeCode())) {
                    // There should be a recurring tran type code.
                    if (warn) {
                        String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.ERROR_ITEM_WRONG_TRAN_TYPE);
                        warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                        warning = StringUtils.replace(warning, "{1}", capitalAssetTransactionType.getCapitalAssetTransactionTypeDescription());
                        warning = StringUtils.replace(warning, "{2}", CabConstants.ValidationStrings.RECURRING);
                        GlobalVariables.getMessageList().add(warning);
                    }
                    else {
                        GlobalVariables.getErrorMap().putError(PurapPropertyConstants.ITEM_CAPITAL_ASSET_TRANSACTION_TYPE, CabKeyConstants.ERROR_ITEM_WRONG_TRAN_TYPE, itemIdentifier, capitalAssetTransactionType.getCapitalAssetTransactionTypeDescription(), CabConstants.ValidationStrings.RECURRING);
                    }
                    valid &= false;
                }
            }
            else { // If the payment type is not recurring ...
                // There should not be a recurring transaction type code.
                if (StringUtils.contains(recurringTransactionTypeCodes, capitalAssetTransactionType.getCapitalAssetTransactionTypeCode())) {
                    if (warn) {
                        String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.ERROR_ITEM_WRONG_TRAN_TYPE);
                        warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                        warning = StringUtils.replace(warning, "{1}", capitalAssetTransactionType.getCapitalAssetTransactionTypeDescription());
                        warning = StringUtils.replace(warning, "{2}", CabConstants.ValidationStrings.NON_RECURRING);
                        GlobalVariables.getMessageList().add(warning);
                    }
                    else {
                        GlobalVariables.getErrorMap().putError(PurapPropertyConstants.ITEM_CAPITAL_ASSET_TRANSACTION_TYPE, CabKeyConstants.ERROR_ITEM_WRONG_TRAN_TYPE, itemIdentifier, capitalAssetTransactionType.getCapitalAssetTransactionTypeDescription(), CabConstants.ValidationStrings.NON_RECURRING);
                    }
                    valid &= false;
                }
            }
        }
        else { // If there is no transaction type ...
            if (recurringPaymentType != null) { // If there is a recurring payment type ...
                if (warn) {
                    String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.ERROR_ITEM_NO_TRAN_TYPE);
                    warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                    warning = StringUtils.replace(warning, "{1}", CabConstants.ValidationStrings.RECURRING);
                    GlobalVariables.getMessageList().add(warning);
                }
                else {
                    GlobalVariables.getErrorMap().putError(PurapPropertyConstants.ITEM_CAPITAL_ASSET_TRANSACTION_TYPE, CabKeyConstants.ERROR_ITEM_NO_TRAN_TYPE, itemIdentifier, CabConstants.ValidationStrings.RECURRING);
                }
                valid &= false;
            }
            else { // If the payment type is not recurring ...
                if (warn) {
                    String warning = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(CabKeyConstants.ERROR_ITEM_NO_TRAN_TYPE);
                    warning = StringUtils.replace(warning, "{0}", itemIdentifier);
                    warning = StringUtils.replace(warning, "{1}", CabConstants.ValidationStrings.NON_RECURRING);
                    GlobalVariables.getMessageList().add(warning);
                }
                else {
                    GlobalVariables.getErrorMap().putError(PurapPropertyConstants.ITEM_CAPITAL_ASSET_TRANSACTION_TYPE, CabKeyConstants.ERROR_ITEM_NO_TRAN_TYPE, itemIdentifier, CabConstants.ValidationStrings.NON_RECURRING);
                }
                valid &= false;
            }
        }

        return valid;
    }

    /**
     * Utility wrapping isCapitalAssetObjectCode for the use of processItemCapitalAssetValidation.
     * 
     * @param oc An ObjectCode
     * @return A String indicating that the given object code is either Capital or Expense
     */
    private String objectCodeCapitalOrExpense(ObjectCode oc) {
        String capital = CabConstants.ValidationStrings.CAPITAL;
        String expense = CabConstants.ValidationStrings.EXPENSE;
        return (isCapitalAssetObjectCode(oc) ? capital : expense);
    }

    /**
     * Predicate to determine whether the given object code is of a specifically capital asset level.
     * 
     * @param oc An ObjectCode
     * @return True if the ObjectCode's level is the one designated as specifically for capital assets.
     */
    public boolean isCapitalAssetObjectCode(ObjectCode oc) {
        String capitalAssetObjectCodeLevels = this.getParameterService().getParameterValue(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, CabParameterConstants.CapitalAsset.CAPITAL_ASSET_OBJECT_LEVELS);
        return (StringUtils.containsIgnoreCase(capitalAssetObjectCodeLevels, oc.getFinancialObjectLevelCode()) ? true : false);
    }

    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#validateFinancialProcessingData(java.util.List,
     *      org.kuali.kfs.fp.businessobject.CapitalAssetInformation)
     */
    public boolean validateFinancialProcessingData(List<SourceAccountingLine> accountingLines, CapitalAssetInformation capitalAssetInformation) {
        boolean valid = true;

        // Check if we need to collect cams data
        boolean dataEntryExpected = false;
        if (this.getParameterService().parameterExists(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, CabParameterConstants.CapitalAsset.FINANCIAL_PROCESSING_CAPITAL_OBJECT_SUB_TYPES)) {
            List<String> financialProcessingCapitalObjectSubTypes = this.getParameterService().getParameterValues(ParameterConstants.CAPITAL_ASSET_BUILDER_DOCUMENT.class, CabParameterConstants.CapitalAsset.FINANCIAL_PROCESSING_CAPITAL_OBJECT_SUB_TYPES);
            for (SourceAccountingLine sourceAccountingLine : accountingLines) {
                if (financialProcessingCapitalObjectSubTypes.contains(sourceAccountingLine.getObjectCode().getFinancialObjectSubTypeCode())) {
                    dataEntryExpected = true;
                    break;
                }
            }
        } // else leave dataEntryExpected on false

        CapitalAssetManagementAsset capitalAssetManagementAsset = capitalAssetInformation.getCapitalAssetManagementAsset();
        if (!dataEntryExpected) {
            if ((capitalAssetManagementAsset != null) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetTagNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetTypeCode()) || ObjectUtils.isNotNull(capitalAssetInformation.getVendorNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetQuantity()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetManufacturerName()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetManufacturerModelNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetSerialNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetDescription()) || ObjectUtils.isNotNull(capitalAssetInformation.getCampusCode()) || ObjectUtils.isNotNull(capitalAssetInformation.getBuildingCode()) || ObjectUtils.isNotNull(capitalAssetInformation.getBuildingRoomNumber())
                    || ObjectUtils.isNotNull(capitalAssetInformation.getBuildingSubRoomNumber())) {
                // If no parameter was found or determined that data shouldn't be collected, give error if data was entered
                GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, CabKeyConstants.CapitalAssetManagementAsset.ERROR_ASSET_DO_NOT_ENTER_ANY_DATA);
                return false;
            }
            else {
                // No data to be collected and no data entered. Hence no error
                return true;
            }
        }

        if (capitalAssetManagementAsset != null) {
            // Update Asset
            if (ObjectUtils.isNotNull(capitalAssetManagementAsset.getCapitalAssetNumber())) {

                // error out if data exists on adding asset
                if (ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetTypeCode()) || ObjectUtils.isNotNull(capitalAssetInformation.getVendorNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetQuantity()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetManufacturerName()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetManufacturerModelNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetSerialNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getCapitalAssetDescription()) || ObjectUtils.isNotNull(capitalAssetInformation.getCampusCode()) || ObjectUtils.isNotNull(capitalAssetInformation.getBuildingCode()) || ObjectUtils.isNotNull(capitalAssetInformation.getBuildingRoomNumber()) || ObjectUtils.isNotNull(capitalAssetInformation.getBuildingSubRoomNumber())) {
                    valid = false;
                    GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, CabKeyConstants.CapitalAssetManagementAsset.ERROR_ASSET_NEW_OR_UPDATE_ONLY);
                }

                valid = validateCapitalAssetManagementAsset(capitalAssetManagementAsset);
            }
        }
        else {
            // New Asset
            valid = checkCapitalAssetFeildsExist(capitalAssetInformation);
            if (valid) {
                valid = validateCapitalAssetFields(capitalAssetInformation);
            }
        }
        return valid;
    }

    /**
     * To validate if the asset is active
     * 
     * @param capitalAssetManagementAsset the asset to be validated
     * @return boolean false if the asset is not active
     */
    private boolean validateCapitalAssetManagementAsset(CapitalAssetManagementAsset capitalAssetManagementAsset) {
        boolean valid = true;

        Map<String, String> params = new HashMap<String, String>();
        params.put(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, capitalAssetManagementAsset.getCapitalAssetNumber().toString());
        Asset asset = (Asset) this.getBusinessObjectService().findByPrimaryKey(Asset.class, params);

        if (ObjectUtils.isNull(asset)) {
            valid = false;
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, KFSKeyConstants.ERROR_EXISTENCE, label);
        }
        else if (!(CamsConstants.InventoryStatusCode.CAPITAL_ASSET_ACTIVE_IDENTIFIABLE.equals(asset.getInventoryStatusCode()) || CamsConstants.InventoryStatusCode.CAPITAL_ASSET_ACTIVE_NON_ACCESSIBLE.equals(asset.getCapitalAssetTypeCode()) || CamsConstants.InventoryStatusCode.CAPITAL_ASSET_UNDER_CONSTRUCTION.equals(asset.getCapitalAssetTypeCode()) || CamsConstants.InventoryStatusCode.CAPITAL_ASSET_SURPLUS_EQUIPEMENT.equals(asset.getCapitalAssetTypeCode()))) {
            valid = false;
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, CabKeyConstants.CapitalAssetManagementAsset.ERROR_ASSET_ACTIVE_CAPITAL_ASSET_REQUIRED);
        }
        return valid;
    }

    /**
     * Check if all required fields exist
     * 
     * @param capitalAssetInformation the fields of add asset to be checked
     * @return boolean false if a required field is not active
     */
    private boolean checkCapitalAssetFeildsExist(CapitalAssetInformation capitalAssetInformation) {
        boolean valid = true;

        if (capitalAssetInformation.getCapitalAssetQuantity() == null || capitalAssetInformation.getCapitalAssetQuantity() <= 0) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.QUANTITY);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.QUANTITY, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        if (StringUtils.isBlank(capitalAssetInformation.getVendorNumber())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.VENDOR_NAME);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.VENDOR_NAME, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        if (StringUtils.isBlank(capitalAssetInformation.getCapitalAssetTypeCode())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.CAPITAL_ASSET_TYPE_CODE);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAPITAL_ASSET_TYPE_CODE, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        if (StringUtils.isBlank(capitalAssetInformation.getCapitalAssetManufacturerName())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.MANUFACTURER_NAME);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.MANUFACTURER_NAME, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        if (StringUtils.isBlank(capitalAssetInformation.getCapitalAssetDescription())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.CAPITAL_ASSET_DESCRIPTION);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAPITAL_ASSET_DESCRIPTION, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        /*
         * Tag number is not required on Add Asset if (StringUtils.isBlank(capitalAssetInformation.getCapitalAssetTagNumber())) {
         * String label = this.getDataDictionaryService().getAttributeLabel(Asset.class,
         * CamsPropertyConstants.Asset.CAMPUS_TAG_NUMBER);
         * GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAMPUS_TAG_NUMBER, KFSKeyConstants.ERROR_REQUIRED,
         * label); valid = false; }
         */
        if (StringUtils.isBlank(capitalAssetInformation.getCampusCode())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.CAMPUS_CODE);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.CAMPUS_CODE, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        if (StringUtils.isBlank(capitalAssetInformation.getBuildingCode())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.BUILDING_CODE);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.BUILDING_CODE, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        if (StringUtils.isBlank(capitalAssetInformation.getBuildingRoomNumber())) {
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.BUILDING_ROOM_NUMBER);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.Asset.BUILDING_ROOM_NUMBER, KFSKeyConstants.ERROR_REQUIRED, label);
            valid = false;
        }

        return valid;
    }

    /**
     * To check if the data is valid
     * 
     * @param capitalAssetInformation the information of add asset to be validated
     * @return boolean false if data is incorrect
     */
    private boolean validateCapitalAssetFields(CapitalAssetInformation capitalAssetInformation) {
        boolean valid = true;

        Map<String, String> params = new HashMap<String, String>();
        params.put(CamsPropertyConstants.AssetType.CAPITAL_ASSET_TYPE_CODE, capitalAssetInformation.getCapitalAssetTypeCode().toString());
        AssetType assetType = (AssetType) this.getBusinessObjectService().findByPrimaryKey(AssetType.class, params);

        if (ObjectUtils.isNull(assetType)) {
            valid = false;
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, CamsPropertyConstants.Asset.CAPITAL_ASSET_TYPE_CODE);
            GlobalVariables.getErrorMap().putError(CamsPropertyConstants.AssetType.CAPITAL_ASSET_TYPE_CODE, KFSKeyConstants.ERROR_EXISTENCE, label);
        }

        if (StringUtils.isNotBlank(capitalAssetInformation.getCapitalAssetTagNumber())) {
            if (!this.getAssetService().findActiveAssetsMatchingTagNumber(capitalAssetInformation.getCapitalAssetTagNumber()).isEmpty()) {
                GlobalVariables.getErrorMap().putError(CamsPropertyConstants.AssetGlobalDetail.CAMPUS_TAG_NUMBER, CamsKeyConstants.AssetGlobal.ERROR_CAMPUS_TAG_NUMBER_DUPLICATE, capitalAssetInformation.getCapitalAssetTagNumber());
                valid = false;
            }
        }

        params = new HashMap<String, String>();
        params.put(KFSPropertyConstants.CAMPUS_CODE, capitalAssetInformation.getCampusCode());
        Campus campus = (Campus) this.getBusinessObjectService().findByPrimaryKey(Campus.class, params);
        if (ObjectUtils.isNull(campus)) {
            valid = false;
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, KFSPropertyConstants.CAMPUS_CODE);
            GlobalVariables.getErrorMap().putError(KFSPropertyConstants.CAMPUS_CODE, KFSKeyConstants.ERROR_EXISTENCE, label);
        }


        params = new HashMap<String, String>();
        params.put(KFSPropertyConstants.CAMPUS_CODE, capitalAssetInformation.getCampusCode());
        params.put(KFSPropertyConstants.BUILDING_CODE, capitalAssetInformation.getBuildingCode());
        Building building = (Building) this.getBusinessObjectService().findByPrimaryKey(Building.class, params);
        if (ObjectUtils.isNull(building)) {
            valid = false;
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, KFSPropertyConstants.BUILDING_CODE);
            GlobalVariables.getErrorMap().putError(KFSPropertyConstants.BUILDING_CODE, KFSKeyConstants.ERROR_EXISTENCE, label);
        }

        params = new HashMap<String, String>();
        params.put(KFSPropertyConstants.CAMPUS_CODE, capitalAssetInformation.getCampusCode());
        params.put(KFSPropertyConstants.BUILDING_CODE, capitalAssetInformation.getBuildingCode());
        params.put(KFSPropertyConstants.BUILDING_ROOM_NUMBER, capitalAssetInformation.getBuildingRoomNumber());
        Room room = (Room) this.getBusinessObjectService().findByPrimaryKey(Room.class, params);
        if (ObjectUtils.isNull(room)) {
            valid = false;
            String label = this.getDataDictionaryService().getAttributeLabel(Asset.class, KFSPropertyConstants.BUILDING_ROOM_NUMBER);
            GlobalVariables.getErrorMap().putError(KFSPropertyConstants.BUILDING_ROOM_NUMBER, KFSKeyConstants.ERROR_EXISTENCE, label);
        }

        return valid;
    }


    /**
     * @see org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService#notifyRouteStatusChange(java.lang.String,
     *      java.lang.String)
     */
    public void notifyRouteStatusChange(String documentNumber, String financialStatusCode) {
        if (KFSConstants.DocumentStatusCodes.DISAPPROVED.equals(financialStatusCode) || KFSConstants.DocumentStatusCodes.CANCELLED.equals(financialStatusCode)) {
            // release CAB line items
            activateCabGlLines(documentNumber);
            activateCabPOLines(documentNumber);
        }
        if (KFSConstants.DocumentStatusCodes.APPROVED.equals(financialStatusCode)) {
            // report asset numbers to PO
            // TODO
        }
    }

    /**
     * Activates CAB GL Lines
     * 
     * @param documentNumber String
     */
    protected void activateCabGlLines(String documentNumber) {
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put("capitalAssetManagementDocumentNumber", documentNumber);
        Collection<GeneralLedgerEntryAsset> matchingGlAssets = this.getBusinessObjectService().findMatching(GeneralLedgerEntryAsset.class, fieldValues);
        if (matchingGlAssets != null && !matchingGlAssets.isEmpty()) {
            for (GeneralLedgerEntryAsset generalLedgerEntryAsset : matchingGlAssets) {
                GeneralLedgerEntry generalLedgerEntry = generalLedgerEntryAsset.getGeneralLedgerEntry();
                generalLedgerEntry.setTransactionLedgerSubmitAmount(KualiDecimal.ZERO);
                generalLedgerEntry.setActive(true);
                this.getBusinessObjectService().save(generalLedgerEntry);
                this.getBusinessObjectService().delete(generalLedgerEntryAsset);
            }
        }
    }

    /**
     * Activates PO Lines
     * 
     * @param documentNumber String
     */
    protected void activateCabPOLines(String documentNumber) {
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put("capitalAssetManagementDocumentNumber", documentNumber);
        Collection<PurchasingAccountsPayableItemAsset> matchingPoAssets = this.getBusinessObjectService().findMatching(PurchasingAccountsPayableItemAsset.class, fieldValues);

        if (matchingPoAssets != null && !matchingPoAssets.isEmpty()) {
            for (PurchasingAccountsPayableItemAsset iemAsset : matchingPoAssets) {
                PurchasingAccountsPayableDocument purapDocument = iemAsset.getPurchasingAccountsPayableDocument();
                purapDocument.setActive(true);
                this.getBusinessObjectService().save(purapDocument);
                iemAsset.setActive(true);
                this.getBusinessObjectService().save(iemAsset);
                List<PurchasingAccountsPayableLineAssetAccount> lineAssetAccounts = iemAsset.getPurchasingAccountsPayableLineAssetAccounts();
                for (PurchasingAccountsPayableLineAssetAccount assetAccount : lineAssetAccounts) {
                    assetAccount.setActive(true);
                    this.getBusinessObjectService().save(assetAccount);
                    GeneralLedgerEntry generalLedgerEntry = assetAccount.getGeneralLedgerEntry();
                    // TODO, payment amount is negative or positive depending on the credit/debit code and document type
                    KualiDecimal submitAmount = generalLedgerEntry.getTransactionLedgerSubmitAmount();
                    if (submitAmount == null) {
                        submitAmount = KualiDecimal.ZERO;
                    }
                    if (CabConstants.PREQ.equals(generalLedgerEntry.getFinancialDocumentTypeCode())) {
                        if (KFSConstants.GL_CREDIT_CODE.equals(generalLedgerEntry.getTransactionDebitCreditCode())) {
                            submitAmount = submitAmount.add(assetAccount.getItemAccountTotalAmount());
                        }
                        else {
                            submitAmount = submitAmount.subtract(assetAccount.getItemAccountTotalAmount());
                        }
                    }
                    else if (CabConstants.CM.equals(generalLedgerEntry.getFinancialDocumentTypeCode())) {
                        if (KFSConstants.GL_CREDIT_CODE.equals(generalLedgerEntry.getTransactionDebitCreditCode())) {
                            submitAmount = submitAmount.subtract(assetAccount.getItemAccountTotalAmount());
                        }
                        else {
                            submitAmount = submitAmount.add(assetAccount.getItemAccountTotalAmount());
                        }
                    }
                    generalLedgerEntry.setTransactionLedgerSubmitAmount(submitAmount);
                    generalLedgerEntry.setActive(true);
                    this.getBusinessObjectService().save(generalLedgerEntry);
                }
            }
        }
    }

    public boolean validateUpdateCAMSView(List<PurApItem> purapItems) {
        boolean valid = true;
        for (PurApItem purapItem : purapItems) {
            if (purapItem.getItemType().isItemTypeAboveTheLineIndicator()) {
                if (!doesItemNeedCapitalAsset(purapItem)) {
                    PurchasingCapitalAssetItem camsItem = ((PurchasingItem) purapItem).getPurchasingCapitalAssetItem();
                    if (camsItem != null && !camsItem.isEmpty()) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemCapitalAssetLine", PurapKeyConstants.ERROR_CAPITAL_ASSET_ITEM_NOT_CAMS_ELIGIBLE, "in line item # " + purapItem.getItemLineNumber());
                    }
                }
            }
        }
        return valid;
    }

    public boolean doesItemNeedCapitalAsset(PurApItem item) {
        for (PurApAccountingLine accountingLine : item.getSourceAccountingLines()) {
            accountingLine.refreshReferenceObject(KFSPropertyConstants.OBJECT_CODE);
            if (isCapitalAssetObjectCode(accountingLine.getObjectCode())) {
                return true;
            }
        }

        return false;
    }

    public boolean validateAddItemCapitalAssetBusinessRules(ItemCapitalAsset asset) {
        boolean valid = true;
        if (asset.getCapitalAssetNumber() == null) {
            valid = false;
        }
        else {
            valid = SpringContext.getBean(DictionaryValidationService.class).isBusinessObjectValid((PurchasingItemCapitalAssetBase) asset);
        }
        if (!valid) {
            String propertyName = "newPurchasingItemCapitalAssetLine." + PurapPropertyConstants.CAPITAL_ASSET_NUMBER;
            String errorKey = PurapKeyConstants.ERROR_CAPITAL_ASSET_ASSET_NUMBER_MUST_BE_LONG_NOT_NULL;
            GlobalVariables.getErrorMap().putError(propertyName, errorKey);
        }

        return valid;
    }

    public boolean validateCapitalAssetsForAutomaticPurchaseOrderRule(List<PurApItem> itemList) {
        for (PurApItem item : itemList) {
            if (doesItemNeedCapitalAsset(item)) {
                // If the item needs capital asset, we cannnot have an APO, so return false.
                return false;
            }
        }
        return true;
    }

    public boolean validateCapitalAssetLocationAddressFieldsOneOrMultipleSystemType(List<CapitalAssetSystem> capitalAssetSystems) {
        boolean valid = true;
        int systemCount = 0;
        for (CapitalAssetSystem system : capitalAssetSystems) {
            List<CapitalAssetLocation> capitalAssetLocations = system.getCapitalAssetLocations();
            StringBuffer errorKey = new StringBuffer("document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_SYSTEMS + "[" + new Integer(systemCount++) + "].");
            errorKey.append("capitalAssetLocations");
            int locationCount = 0;
            for (CapitalAssetLocation location : capitalAssetLocations) {
                errorKey.append("[" + locationCount++ + "].");
                valid &= validateCapitalAssetLocationAddressFields(location, errorKey);
            }
        }
        return valid;
    }

    public boolean validateCapitalAssetLocationAddressFieldsForIndividualSystemType(List<PurchasingCapitalAssetItem> capitalAssetItems) {
        boolean valid = true;
        for (PurchasingCapitalAssetItem item : capitalAssetItems) {
            CapitalAssetSystem system = item.getPurchasingCapitalAssetSystem();
            List<CapitalAssetLocation> capitalAssetLocations = system.getCapitalAssetLocations();
            StringBuffer errorKey = new StringBuffer("document." + PurapPropertyConstants.PURCHASING_CAPITAL_ASSET_ITEMS + "[" + new Integer(item.getPurchasingItem().getItemLineNumber().intValue() - 1) + "].");
            errorKey.append("purchasingCapitalAssetSystem.capitalAssetLocations");
            int i = 0;
            for (CapitalAssetLocation location : capitalAssetLocations) {
                errorKey.append("[" + i++ + "].");
                valid &= validateCapitalAssetLocationAddressFields(location, errorKey);
            }
        }
        return valid;
    }

    private boolean validateCapitalAssetLocationAddressFields(CapitalAssetLocation location, StringBuffer errorKey) {
        boolean valid = true;
        if (StringUtils.isBlank(location.getCapitalAssetLine1Address())) {
            GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_ADDRESS_LINE1);
            valid &= false;
        }
        if (StringUtils.isBlank(location.getCapitalAssetCityName())) {
            GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_CITY);
            valid &= false;
        }
        if (StringUtils.isBlank(location.getCapitalAssetCountryCode())) {
            GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_COUNTRY);
            valid &= false;
        }
        else if (location.getCapitalAssetCountryCode().equals(KFSConstants.COUNTRY_CODE_UNITED_STATES)) {
            if (StringUtils.isBlank(location.getCapitalAssetStateCode())) {
                GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_STATE);
                valid &= false;
            }
            if (StringUtils.isBlank(location.getCapitalAssetPostalCode())) {
                GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_POSTAL_CODE);
                valid &= false;
            }
        }
        if (!location.isOffCampusIndicator()) {
            if (StringUtils.isBlank(location.getCampusCode())) {
                GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_CAMPUS);
                valid &= false;
            }
            if (StringUtils.isBlank(location.getBuildingCode())) {
                GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_BUILDING);
                valid &= false;
            }
            if (StringUtils.isBlank(location.getBuildingRoomNumber())) {
                GlobalVariables.getErrorMap().putError(errorKey.toString(), KFSKeyConstants.ERROR_REQUIRED, PurapPropertyConstants.CAPITAL_ASSET_LOCATION_ROOM);
                valid &= false;
            }
        }
        return valid;
    }

    /**
     * Gets the parameterService attribute.
     * 
     * @return Returns the parameterService.
     */
    public ParameterService getParameterService() {
        return SpringContext.getBean(ParameterService.class);
    }

    /**
     * Gets the businessObjectService attribute.
     * 
     * @return Returns the businessObjectService.
     */
    public BusinessObjectService getBusinessObjectService() {
        return SpringContext.getBean(BusinessObjectService.class);
    }

    /**
     * Gets the dataDictionaryService attribute.
     * 
     * @return Returns the dataDictionaryService.
     */
    public DataDictionaryService getDataDictionaryService() {
        return SpringContext.getBean(DataDictionaryService.class);
    }

    /**
     * Gets the assetService attribute.
     * 
     * @return Returns the assetService.
     */
    public AssetService getAssetService() {
        return SpringContext.getBean(AssetService.class);
    }

    /**
     * Gets the kualiModuleService attribute.
     * 
     * @return Returns the kualiModuleService.
     */
    public KualiModuleService getKualiModuleService() {
        return SpringContext.getBean(KualiModuleService.class);
    }
}
