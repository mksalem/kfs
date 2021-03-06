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
package org.kuali.kfs.pdp.businessobject.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kuali.kfs.pdp.PdpConstants;
import org.kuali.kfs.pdp.PdpKeyConstants;
import org.kuali.kfs.pdp.PdpParameterConstants;
import org.kuali.kfs.pdp.PdpPropertyConstants;
import org.kuali.kfs.pdp.businessobject.FormatProcess;
import org.kuali.kfs.pdp.businessobject.PaymentProcess;
import org.kuali.kfs.pdp.service.PdpAuthorizationService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.rice.core.api.config.property.ConfigurationService;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kns.lookup.HtmlData;
import org.kuali.rice.kns.lookup.HtmlData.AnchorHtmlData;
import org.kuali.rice.kns.lookup.KualiLookupableHelperServiceImpl;
import org.kuali.rice.krad.bo.BusinessObject;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.ObjectUtils;
import org.kuali.rice.krad.util.UrlFactory;

/**
 * This class allows custom handling of FormatProcesses within the lookup framework.
 */
public class FormatProcessLookupableHelperService extends KualiLookupableHelperServiceImpl {

    private ConfigurationService configurationService;
    private PdpAuthorizationService pdpAuthorizationService;

    /**
     * @see org.kuali.rice.kns.lookup.KualiLookupableHelperServiceImpl#getSearchResults(java.util.Map)
     */
    @Override
    public List<? extends BusinessObject> getSearchResults(Map<String, String> fieldValues) {
        return super.getSearchResults(fieldValues);
    }

    /**
     * @see org.kuali.rice.kns.lookup.AbstractLookupableHelperServiceImpl#getCustomActionUrls(org.kuali.rice.krad.bo.BusinessObject, java.util.List)
     */
    @Override
    public List<HtmlData> getCustomActionUrls(BusinessObject businessObject, List pkNames) {
        List<HtmlData> anchorHtmlDataList = new ArrayList<HtmlData>();
        if (businessObject instanceof FormatProcess) {
            Person person = GlobalVariables.getUserSession().getPerson();
            FormatProcess formatProcess = (FormatProcess) businessObject;
            int processId = formatProcess.getPaymentProcIdentifier();

            Map primaryKeys = new HashMap();
            primaryKeys.put(PdpPropertyConstants.PaymentProcess.PAYMENT_PROCESS_ID, processId);
            PaymentProcess paymentProcess = (PaymentProcess) getBusinessObjectService().findByPrimaryKey(PaymentProcess.class, primaryKeys);

            String linkText = KFSConstants.EMPTY_STRING;
            String url = KFSConstants.EMPTY_STRING;
            String basePath = configurationService.getPropertyValueAsString(KFSConstants.APPLICATION_URL_KEY) + "/" + PdpConstants.Actions.FORMAT_PROCESS_ACTION;

            if (pdpAuthorizationService.hasRemoveFormatLockPermission(person.getPrincipalId()) && ObjectUtils.isNotNull(paymentProcess) && !paymentProcess.isFormattedIndicator()) {
                Properties params = new Properties();
                params.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, PdpConstants.ActionMethods.CLEAR_FORMAT_PROCESS_ACTION);
                params.put(PdpParameterConstants.FormatProcess.PROCESS_ID_PARAM, UrlFactory.encode(String.valueOf(processId)));
                url = UrlFactory.parameterizeUrl(basePath, params);

                linkText = configurationService.getPropertyValueAsString(PdpKeyConstants.FormatProcess.CLEAR_UNFINISHED_FORMAT_PROCESS);

                AnchorHtmlData anchorHtmlData = new AnchorHtmlData(url, PdpConstants.ActionMethods.CONFIRM_CANCEL_ACTION, linkText);
                anchorHtmlDataList.add(anchorHtmlData);
            }
            else {
                AnchorHtmlData anchorHtmlData = new AnchorHtmlData("&nbsp;", "", "");
                anchorHtmlDataList.add(anchorHtmlData);
            }

        }
        return anchorHtmlDataList;
    }

    /**
     * This method gets the configurationService.
     * 
     * @return configurationService
     */
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    /**
     * This method sets the configurationService.
     * @param configurationService
     */
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * This method sets the pdpAuthorizationService.
     * @param pdpAuthorizationService The pdpAuthorizationService to be set.
     */
    public void setPdpAuthorizationService(PdpAuthorizationService pdpAuthorizationService) {
        this.pdpAuthorizationService = pdpAuthorizationService;
    }
}
