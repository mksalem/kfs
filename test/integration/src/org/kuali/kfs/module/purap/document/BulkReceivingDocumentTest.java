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
package org.kuali.kfs.module.purap.document;

import static org.kuali.kfs.sys.fixture.UserNameFixture.parke;

import org.kuali.kfs.module.purap.fixture.BulkReceivingDocumentFixture;
import org.kuali.kfs.module.purap.fixture.PurchaseOrderDocumentFixture;
import org.kuali.kfs.sys.ConfigureContext;
import org.kuali.kfs.sys.context.KualiTestBase;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.document.AccountingDocumentTestUtils;
import org.kuali.kfs.sys.document.workflow.WorkflowTestUtils;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kew.util.KEWConstants;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.exception.ValidationException;
import org.kuali.rice.kns.service.DocumentService;
import org.kuali.rice.kns.util.GlobalVariables;

public class BulkReceivingDocumentTest extends KualiTestBase {
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    @ConfigureContext(session = parke,shouldCommitTransactions=true)
    public final void testRouteDocument() 
    throws Exception {
        BulkReceivingDocument doc = BulkReceivingDocumentFixture.SIMPLE_DOCUMENT.createBulkReceivingDocument();
        doc.prepareForSave();
        DocumentService documentService = SpringContext.getBean(DocumentService.class);
        routeDocument(doc, "routing bulk receiving document", documentService);
        WorkflowTestUtils.waitForStatusChange(doc.getDocumentHeader().getWorkflowDocument(), KEWConstants.ROUTE_HEADER_FINAL_CD);        
        Document document = documentService.getByDocumentHeaderId(doc.getDocumentNumber());
        assertTrue("Document should now be final.", doc.getDocumentHeader().getWorkflowDocument().stateIsFinal());
    }
    
    @ConfigureContext(session = parke, shouldCommitTransactions=true)
    public final void testRouteDocumentWithPO() 
    throws Exception {
        PurchaseOrderDocument po = PurchaseOrderDocumentFixture.PO_ONLY_REQUIRED_FIELDS.createPurchaseOrderDocument();
        DocumentService documentService = SpringContext.getBean(DocumentService.class);
        po.prepareForSave();       
        AccountingDocumentTestUtils.routeDocument(po, "saving copy source document", null, documentService);
        WorkflowTestUtils.waitForStatusChange(po.getDocumentHeader().getWorkflowDocument(), "F");        
        PurchaseOrderDocument poResult = (PurchaseOrderDocument) documentService.getByDocumentHeaderId(po.getDocumentNumber());
        
        BulkReceivingDocument doc = BulkReceivingDocumentFixture.SIMPLE_DOCUMENT_FOR_PO.createBulkReceivingDocument(po);
        doc.prepareForSave();
        routeDocument(doc, "routing bulk receiving document", documentService);
        WorkflowTestUtils.waitForStatusChange(doc.getDocumentHeader().getWorkflowDocument(), KEWConstants.ROUTE_HEADER_FINAL_CD);        
        Document document = documentService.getByDocumentHeaderId(doc.getDocumentNumber());
        assertTrue("Document should now be final.", doc.getDocumentHeader().getWorkflowDocument().stateIsFinal());
        
    }

    private void routeDocument(Document document, String annotation, DocumentService documentService) throws WorkflowException {
        try {
            documentService.routeDocument(document, annotation, null);
        }
        catch (ValidationException e) {
            e.printStackTrace();
            // If the business rule evaluation fails then give us more info for debugging this test.
            fail(e.getMessage() + ", " + GlobalVariables.getErrorMap());
        }
    }
}
