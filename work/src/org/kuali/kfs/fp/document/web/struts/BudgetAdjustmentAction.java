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
package org.kuali.kfs.fp.document.web.struts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.kuali.kfs.fp.document.BudgetAdjustmentDocument;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.web.struts.KualiAccountingDocumentActionBase;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase;

/**
 * This class handles specific Actions requests for the BudgetAdjustment.
 */
public class BudgetAdjustmentAction extends KualiAccountingDocumentActionBase {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(BudgetAdjustmentAction.class);

    /**
     * Do initialization for a new budget adjustment
     * 
     * @see org.kuali.kfs.sys.web.struts.KualiAccountingDocumentActionBase#createDocument(org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase)
     */
    @Override
    protected void createDocument(KualiDocumentFormBase kualiDocumentFormBase) throws WorkflowException {
        super.createDocument(kualiDocumentFormBase);
        ((BudgetAdjustmentDocument) kualiDocumentFormBase.getDocument()).initiateDocument();

    }

    /**
     * Add warning message about copying a document with generated labor benefits.
     * 
     * @see org.kuali.kfs.sys.web.struts.KualiAccountingDocumentActionBase#copy(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward copy(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GlobalVariables.getMessageList().add(KFSKeyConstants.WARNING_DOCUMENT_BA_COPY_LABOR_BENEFITS);
        return super.copy(mapping, form, request, response);
    }

}
