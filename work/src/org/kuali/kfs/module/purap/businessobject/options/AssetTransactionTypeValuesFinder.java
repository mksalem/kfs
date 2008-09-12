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
package org.kuali.kfs.module.purap.businessobject.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kuali.kfs.integration.cab.CapitalAssetBuilderAssetTransactionType;
import org.kuali.kfs.integration.cab.CapitalAssetBuilderModuleService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kns.lookup.keyvalues.KeyValuesBase;
import org.kuali.rice.kns.service.KeyValuesService;
import org.kuali.rice.kns.web.ui.KeyLabelPair;

/**
 * Values finder for CapitalAssetBuilderAssetTransactionTypes
 */
public class AssetTransactionTypeValuesFinder extends KeyValuesBase {

    /**
     * Returns code/description pairs of all CapitalAssetBuilderAssetTransactionTypes.
     * 
     * @see org.kuali.rice.kns.lookup.keyvalues.KeyValuesFinder#getKeyValues()
     */
    public List getKeyValues() {
        CapitalAssetBuilderModuleService cabModuleService = SpringContext.getBean(CapitalAssetBuilderModuleService.class);
        List<CapitalAssetBuilderAssetTransactionType> types = cabModuleService.getAllAssetTransactionTypes();
        List labels = new ArrayList<KeyLabelPair>();
        labels.add(new KeyLabelPair("",""));
        for (Object type : types) {
            CapitalAssetBuilderAssetTransactionType camsType = (CapitalAssetBuilderAssetTransactionType)type;           
            labels.add(new KeyLabelPair(camsType.getCapitalAssetTransactionTypeCode(), camsType.getCapitalAssetTransactionTypeDescription()));
        }

        return labels;
    }

}
