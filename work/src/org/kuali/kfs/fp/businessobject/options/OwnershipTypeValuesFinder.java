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
package org.kuali.kfs.fp.businessobject.options;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kuali.kfs.fp.businessobject.OwnershipTypeCode;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kns.lookup.keyvalues.KeyValuesBase;
import org.kuali.rice.kns.service.KeyValuesService;
import org.kuali.rice.kns.web.ui.KeyLabelPair;

/**
 * This class returns list of ownership type value pairs.
 */
public class OwnershipTypeValuesFinder extends KeyValuesBase {

    /*
     * @see org.kuali.keyvalues.KeyValuesFinder#getKeyValues()
     */
    public List getKeyValues() {
        List boList = (List) SpringContext.getBean(KeyValuesService.class).findAll(OwnershipTypeCode.class);
        List keyValues = new ArrayList();
        keyValues.add(new KeyLabelPair("", ""));
        for (Iterator iter = boList.iterator(); iter.hasNext();) {
            OwnershipTypeCode element = (OwnershipTypeCode) iter.next();
            keyValues.add(new KeyLabelPair(element.getCode(), element.getName()));
        }

        return keyValues;
    }

}
