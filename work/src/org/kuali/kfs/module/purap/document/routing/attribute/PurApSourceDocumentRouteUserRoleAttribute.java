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
package org.kuali.kfs.module.purap.document.routing.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kuali.kfs.module.purap.document.PurchasingAccountsPayableDocument;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kew.engine.RouteContext;
import org.kuali.rice.kew.exception.KEWUserNotFoundException;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kew.identity.Id;
import org.kuali.rice.kew.rule.ResolvedQualifiedRole;
import org.kuali.rice.kew.rule.Role;
import org.kuali.rice.kew.rule.UnqualifiedRoleAttribute;
import org.kuali.rice.kew.user.AuthenticationUserId;
import org.kuali.rice.kns.service.DataDictionaryService;
import org.kuali.rice.kns.service.DocumentService;
import org.kuali.rice.kns.util.ObjectUtils;

/**
 * TODO delyea - documentation
 */
public class PurApSourceDocumentRouteUserRoleAttribute extends UnqualifiedRoleAttribute {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PurApSourceDocumentRouteUserRoleAttribute.class);

    private static final String SOURCE_DOC_ROUTED_BY_USER_ROLE_KEY = "SOURCE_DOC_ROUTED_BY_USER";
    private static final String SOURCE_DOC_ROUTED_BY_USER_ROLE_LABEL = "User who Routed Source Document";

    private static final Role ROLE = new Role(PurApSourceDocumentRouteUserRoleAttribute.class, SOURCE_DOC_ROUTED_BY_USER_ROLE_KEY, SOURCE_DOC_ROUTED_BY_USER_ROLE_LABEL);
    private static final List<Role> ROLES;
    static {
        ArrayList<Role> roles = new ArrayList<Role>(1);
        roles.add(ROLE);
        ROLES = Collections.unmodifiableList(roles);
    }

    /**
     * @see org.kuali.rice.kew.rule.UnqualifiedRoleAttribute#getRoleNames()
     */
    @Override
    public List<Role> getRoleNames() {
        return ROLES;
    }

    private void assertDocumentNotNull(PurchasingAccountsPayableDocument document) {
        if (ObjectUtils.isNull(document)) {
            String errorMessage = "Document with doc id '" + document.getDocumentNumber() + "' and class '" + document.getClass() + "' does not exist in system";
            LOG.error("resolveRole() " + errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * TODO delyea - documentation
     * 
     * @param routeContext the RouteContext
     * @param roleName the role name
     * @return a ResolvedQualifiedRole
     */
    @Override
    public ResolvedQualifiedRole resolveRole(RouteContext routeContext, String roleName) throws KEWUserNotFoundException {
        String documentNumber = null;
        try {
            documentNumber = routeContext.getDocument().getRouteHeaderId().toString();
            PurchasingAccountsPayableDocument document = (PurchasingAccountsPayableDocument) SpringContext.getBean(DocumentService.class).getByDocumentHeaderId(documentNumber);
            assertDocumentNotNull(document);
            document.refreshNonUpdateableReferences();
            PurchasingAccountsPayableDocument sourceDocument = document.getPurApSourceDocumentIfPossible();
            // method getSourceDocumentIfPossible() could return null but for using this instance we should get something back
            assertDocumentNotNull(sourceDocument);
            // return the user who routed the source document
            DataDictionaryService dataDictionaryService = SpringContext.getBean(DataDictionaryService.class);
            String label = "User Who Routed " + document.getPurApSourceDocumentLabelIfPossible() + " " + sourceDocument.getPurapDocumentIdentifier();
            return new ResolvedQualifiedRole(label, Arrays.asList(new Id[] { new AuthenticationUserId(sourceDocument.getDocumentHeader().getWorkflowDocument().getRoutedByUserNetworkId()) }));
        }
        catch (WorkflowException e) {
            String errorMessage = "Workflow problem while trying to get document using doc id '" + documentNumber + "'";
            LOG.error("resolveRole() " + errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
