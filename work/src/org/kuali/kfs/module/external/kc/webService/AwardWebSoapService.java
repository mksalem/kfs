/*
 * Copyright 2010-2012 The Kuali Foundation.
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.external.kc.webService;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

import org.kuali.kfs.module.external.kc.KcConstants;
import org.kuali.kfs.module.external.kc.service.KfsService;
import org.kuali.kra.external.award.service.AwardWebService;

/**
 * This class was generated by Apache CXF 2.2.10
 * Thu Sep 30 15:50:58 HST 2010
 * Generated source version: 2.2.10
 *
 */


@WebServiceClient(name = KcConstants.Award.SOAP_SERVICE_NAME,
                  wsdlLocation = "http://test.kc.kuali.org/kc-trunk/remoting/awardWebSoapService?wsdl",
                  targetNamespace = KcConstants.KC_NAMESPACE_URI)
public class AwardWebSoapService extends KfsService {

    public final static QName AwardWebServicePort = new QName(KcConstants.KC_NAMESPACE_URI, KcConstants.Award.SERVICE_PORT);
    static {
        try {
           getWsdl(KcConstants.Award.SERVICE);
         } catch (MalformedURLException e) {
             LOG.warn("Can not initialize the wsdl");
         }
    }

    public AwardWebSoapService() throws MalformedURLException {
        super(getWsdl(KcConstants.Award.SERVICE), KcConstants.Award.SERVICE);
    }


    /**
     *
     * @return
     *     returns InstitutionalUnitService
     */
    @WebEndpoint(name = KcConstants.Award.SERVICE_PORT)
    public AwardWebService getAwardWebServicePort() {
        return super.getPort(AwardWebServicePort, AwardWebService.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns InstitutionalUnitService
     */
    @WebEndpoint(name = KcConstants.Award.SERVICE_PORT)
    public AwardWebService getAwardWebServicePort(WebServiceFeature... features) {
        return super.getPort(AwardWebServicePort, AwardWebService.class, features);
    }

    @Override
    public URL getWsdl() throws MalformedURLException {
        return super.getWsdl(KcConstants.Award.SERVICE);
    }

}
