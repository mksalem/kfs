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
function onblur_proposalDirectCostAmount( directAmountField ) {
    updateTotalAmount( directAmountField.name, findElPrefix( directAmountField.name ) + ".proposalIndirectCostAmount", "proposalTotalAmount" );
}

function onblur_proposalIndirectCostAmount( indirectAmountField ) {
    updateTotalAmount( findElPrefix( indirectAmountField.name ) + ".proposalDirectCostAmount", indirectAmountField.name, "proposalTotalAmount" );
}

function onblur_proposalStatusCode( proposalStatusCodeField ) {
    var fieldName = proposalStatusCodeField.name;
    if (valueChanged( fieldName )) {
        var code = getElementValue( fieldName );
        var rejectedName = findElPrefix( fieldName ) + ".proposalRejectedDate";
        // if status changed to rejected or withdrawn
        if (code == "R" || code == "W") {
            // then default rejected date to today
            if (getElementValue( rejectedName ) == "") {
                setRecipientValue( rejectedName, today() );
            }
        }
    }
}

function onblur_subcontractorNumber( subcontractorNumberField ) {
  //alert("subcontract "+subcontractorNumberField)
    singleKeyLookup( SubcontractorService.getByPrimaryId, subcontractorNumberField, "subcontractor", "subcontractorName" );
}
function onblur_subcontractorNumber_nonPersonnel( subcontractorNumberField , boName, fieldName) {
  //alert("subcontract "+subcontractorNumberField+" "+boName)
    singleKeyLookupDiff( SubcontractorService.getByPrimaryId, subcontractorNumberField, boName, "subcontractorName", fieldName  );
}

function onblur_agencyNumber( agencyNumberField, boName ) {
   //alert("agenc# "+agencyNumberField)
    singleKeyLookup( AgencyService.getByPrimaryId, agencyNumberField, boName, "fullName" );
}


function singleKeyLookup( dwrFunction, primaryKeyField, boName, propertyName ) {
    var primaryKeyValue = DWRUtil.getValue( primaryKeyField ).trim();
    var targetFieldName = findElPrefix( primaryKeyField ) + "." + boName + "." + propertyName;
    if (primaryKeyValue == "") {
        clearRecipients( targetFieldName );
    } else {
        dwrFunction( primaryKeyValue, makeDwrSingleReply( boName, propertyName, targetFieldName));
    }
}

function singleKeyLookupDiff( dwrFunction, primaryKeyField, boName, propertyName, fieldName ) {
// lookup with different property namd and field name
    var primaryKeyValue = DWRUtil.getValue( primaryKeyField ).trim();
    var targetFieldName
    if (boName=="") {
    	targetFieldName = findElPrefix( primaryKeyField ) + "." + fieldName;
    } else {
    	targetFieldName = findElPrefix( primaryKeyField ) + "." + boName + "." + fieldName;
    }
    if (primaryKeyValue == "") {
        clearRecipients( targetFieldName );
    } else {
        dwrFunction( primaryKeyValue, makeDwrSingleReply( boName, propertyName, targetFieldName));
    }
}

function makeDwrSingleReply( boName, propertyName, targetFieldName ) {
    var friendlyBoName = boName.replace(/([A-Z])/g, ' $1').toLowerCase();
    return {
        callback:function(data) {
            if (data != null && typeof data == 'object') {
                setRecipientValue( targetFieldName, data[propertyName] );
                if (boName=="budgetAgency") {
                    setRecipientValue( "document.budget.budgetAgency.agencyTypeCode", data["agencyTypeCode"] ); 
                    removeFpt(boName);                   
                }
                if (boName=="agency") {
                    setRecipientValue( "document.routingFormAgency.agency.agencyTypeCode", data["agencyTypeCode"] ); 
                    removeFpt(boName);                   
                }
            } else {
                setRecipientValue( targetFieldName, wrapError( friendlyBoName + " not found" ), true );
            }
        },
        errorHandler:function(errorMessage) {
            setRecipientValue( targetFieldName, wrapError( friendlyBoName + " not found" ), true );
        }
    };
}

function organizationNameLookup( anyFieldOnProposalOrganization ) {
    var elPrefix = findElPrefix( anyFieldOnProposalOrganization.name );
    var chartOfAccountsCode = DWRUtil.getValue( elPrefix + ".chartOfAccountsCode" ).toUpperCase().trim();
    var organizationCode = DWRUtil.getValue( elPrefix + ".organizationCode" ).toUpperCase().trim();
    var targetFieldName = elPrefix + ".organization.organizationName";
    if (chartOfAccountsCode == "" || organizationCode == "") {
        clearRecipients( targetFieldName );
    } else {
        var dwrReply = makeDwrSingleReply( "organization", "organizationName", targetFieldName);
        OrganizationService.getByPrimaryIdWithCaching( chartOfAccountsCode, organizationCode, dwrReply);
    }
}

function personIDLookup( userIdField ) {
   //alert("proposalDirectorIDLookup " + userIdField)
    var elPrefix = findElPrefix( userIdField );
	var userNameFieldName = elPrefix + ".personName";
	var universalIdFieldName = findElPrefix( elPrefix ) + ".personUniversalIdentifier";
	
	loadPersonInfo( userIdField, universalIdFieldName, userNameFieldName );
}

function loadPersonInfo( userIdFieldName, universalIdFieldName, userNameFieldName ) {
    var userId = DWRUtil.getValue( userIdFieldName ).trim();

    if (userId == "") {
        clearRecipients( universalIdFieldName );
        clearRecipients( userNameFieldName );
    } else {
        var dwrReply = {
            callback:function(data) {
                if ( data != null && typeof data == 'object' ) {
                    setRecipientValue( universalIdFieldName, data.personUniversalIdentifier );
                    setRecipientValue( userNameFieldName, data.personName );
                } else {
                    clearRecipients( universalIdFieldName );
                    setRecipientValue( userNameFieldName, wrapError( "person not found" ), true );
                } },
            errorHandler:function( errorMessage ) {
                clearRecipients( universalIdFieldName );
                setRecipientValue( userNameFieldName, wrapError( "person not found" ), true );
            }
        };
        ProjectDirectorService.getByPersonUserIdentifier( userId, dwrReply );
    }
}

function budgetNameLookup( documentNumberField ) {
    var elPrefix = findElPrefix( documentNumberField );
	var budgetNameFieldName = elPrefix + ".budget.budgetName";
	var budgetDocumentNumberFieldName = elPrefix  + ".budget.documentNumber";
	
	loadBudgetInfo( documentNumberField, budgetNameFieldName, budgetDocumentNumberFieldName );
}

function loadBudgetInfo( documentNumberField, budgetNameFieldName, budgetDocumentNumberFieldName ) {
    // budgetservice return a 'budget' object if it is found, but for some reason
    // the dwrreply always get data=null.  This is a temporary solution to get name only.
    var documentNumber = DWRUtil.getValue( documentNumberField ).trim();

    if (documentNumber == "") {
        //alert(" document number is empty ")
        clearRecipients( budgetDocumentNumberFieldName );
        clearRecipients( budgetNameFieldName );
    } else {
       //alert ("doc # is notempty")
        var dwrReply = {
            callback:function(data) {
                if ( data != null && data != "" ) {
                    setRecipientValue( budgetDocumentNumberFieldName, documentNumber);
                    setRecipientValue( budgetNameFieldName, data );
                } else {
                    clearRecipients( budgetDocumentNumberFieldName );
                    setRecipientValue( budgetNameFieldName, wrapError( "Budget document not found" ), true );
                } },
            errorHandler:function( errorMessage ) {
                clearRecipients( budgetDocumentNumberFieldName );
                setRecipientValue( budgetNameFieldName, wrapError( "Budget document not found" ), true );
            }
        };
        BudgetService.getByPrimaryId( documentNumber, dwrReply );
    }
}

// cfda
function cfdaLookup( cfdaField ) {
    var elPrefix = findElPrefix( cfdaField );
	var titleNameFieldName = elPrefix + ".cfdaProgramTitleName";
	var routingFormCfdaFieldName = findElPrefix( elPrefix ) + ".routingFormCatalogOfFederalDomesticAssistanceNumber";
	
	loadCfdaInfo( cfdaField, titleNameFieldName, routingFormCfdaFieldName );
}


function loadCfdaInfo( cfdaField, titleNameFieldName, routingFormCfdaFieldName ) {
    var cfdaNumber = DWRUtil.getValue( cfdaField ).trim();

    if (cfdaNumber == "") {
        clearRecipients( routingFormCfdaFieldName );
        clearRecipients( titleNameFieldName );
    } else {
        var dwrReply = {
            callback:function(data) {
                if ( data != null && typeof data == 'object' ) {
                    setRecipientValue( routingFormCfdaFieldName, data.cfdaNumber );
                    setRecipientValue( titleNameFieldName, data.cfdaProgramTitleName );
                } else {
                    clearRecipients( routingFormCfdaFieldName );
                    setRecipientValue( titleNameFieldName, wrapError( "Cfda not found" ), true );
                } },
            errorHandler:function( errorMessage ) {
                clearRecipients( routingFormCfdaFieldName );
                setRecipientValue( titleNameFieldName, wrapError( "Cfda not found" ), true );
            }
        };
        CfdaService.getByPrimaryId( cfdaNumber, dwrReply );
    }
}




function today() {
    var now = new Date();
    // Kuali's DateFormatter requires this format, regardless of Locale.
    return (1 + now.getMonth()) + "/" + now.getDate() + "/" + now.getFullYear();
}

function updateTotalAmount( directAmountFieldName, indirectAmountFieldName, totalAmountFieldName ) {
    var directAmount = getElementValue( directAmountFieldName );
    var indirectAmount = getElementValue( indirectAmountFieldName );
    var totalFieldName = findElPrefix( directAmountFieldName ) + "."+ totalAmountFieldName;
    if ( isCurrencyNumber( directAmount ) && isCurrencyNumber( indirectAmount ) ) {
        var totalValue = formatCurrency( parseCurrency( directAmount ) + parseCurrency( indirectAmount ) );
        setRecipientValue( totalFieldName, totalValue );
    }
    else {
        setRecipientValue( totalFieldName, "" );
    }
}

function isCurrencyNumber( value ) {
    return /^[($-]*\d{1,3}(,?\d{3})*(\.\d{0,2})?\)?$/.test( value.toString().trim() );
}

function parseCurrency( value ) {
    value = value.toString().trim();
    var negative = /^\(.*\)$/.test(value);
    return (negative ? -1 : 1) * parseFloat( value.replace(/[($,]/g, "") );
}

function formatCurrency( amount ) {
    var negative = amount < 0;
    var roundedParts = (Math.abs(amount) + 0.005).toString().split(".");
    var whole = roundedParts[0];
    var fraction = roundedParts.length < 2 ? "00" : (roundedParts[1] + "00").substring(0, 2);
    var groups = [];
    while (whole.length > 3) {
        groups.unshift( whole.substring(whole.length - 3) );
        whole = whole.substring(0, whole.length - 3);
    }
    if (whole.length > 0) {
        groups.unshift(whole);
    }
    // Kuali's CurrencyFormatter is not displaying the $ symbol, so this function doesn't either.
    return (negative ? "(" : "") + groups.join(",") + "." + fraction + (negative ? ")" : "");
}

function onblur_awardDirectCostAmount( directAmountField ) {
    updateTotalAmount( directAmountField.name, findElPrefix( directAmountField.name ) + ".awardIndirectCostAmount", "awardTotalAmount" );
}

function onblur_awardIndirectCostAmount( indirectAmountField ) {
    updateTotalAmount( findElPrefix( indirectAmountField.name ) + ".awardDirectCostAmount", indirectAmountField.name, "awardTotalAmount" );
}

function accountNameLookup( anyFieldOnAwardAccount ) {
    var elPrefix = findElPrefix( anyFieldOnAwardAccount );
    var chartOfAccountsCode = DWRUtil.getValue( elPrefix + ".chartOfAccountsCode" ).toUpperCase().trim();
    var accountNumber = DWRUtil.getValue( elPrefix + ".accountNumber" ).toUpperCase().trim();
    var targetFieldName = elPrefix + ".account.accountName";
    if (accountNumber=='') {
		clearRecipients(targetFieldName);
	} else if (chartOfAccountsCode=='') {
		setRecipientValue(targetFieldName, wrapError( 'chart code is empty' ), true );
    } else {
        var dwrReply = makeDwrSingleReply( "account", "accountName", targetFieldName);
        AccountService.getByPrimaryIdWithCaching( chartOfAccountsCode, accountNumber, dwrReply);
    }
}

// following methods are dynamic add/remove element for ftpagency
function addCfp(agencyNumber) {
  // add clear fpt button
  // does this work for all browsers?
  var myDiv = document.getElementById("myDiv");
    var oldid = document.getElementById("newDiv");
  var fptAgencyNumber=kualiElements[agencyNumber];
  //alert("addcfp "+oldid+" "+agencyNumber+" "+fptAgencyNumber.value)
  if (oldid!=null) {
    // has to do this because the div may have no clearfpt image
	myDiv.removeChild(oldid);
  }
  if (fptAgencyNumber!=null && fptAgencyNumber.value!="") {
	    var newdiv = document.createElement("div");
	  newdiv.setAttribute("id","newDiv");
	  newdiv.innerHTML = "<input type='image' name='methodToCall.clearFedPassthrough.anchor1' src='static/images/tinybutton-clearfptagency.jpg' class='tinybutton' alt='clear fed passthrough'>";
	  myDiv.appendChild(newdiv);
  }
}


function removeFpt(boName) {
  // does this work for all browsers?
  var i=1;
  var elementName
  var agencyNumber
  if (boName=="budgetAgency") {
  	elementName="document.budget.budgetAgency.agencyTypeCode"
  } else {
  	elementName="document.routingFormAgency.agency.agencyTypeCode"
  }
  if (boName=="budgetAgency") {
  	agencyNumber="document.budget.federalPassThroughAgencyNumber"
  } else {
  	agencyNumber="document.agencyFederalPassThroughNumber"
  }
	  var typeCode=kualiElements[elementName];
	  var fptAgencyNumber=kualiElements[agencyNumber];
	  if (typeCode!=null && getElementValue( elementName )!=null ) {
		  if (typeCode.value=="D") {
		    if (fptAgencyNumber==null || getElementValue(agencyNumber)=="") {		    
		    restoreFpt(boName);
		    }
		  } else {
			  var pDiv = document.getElementById("pDiv");
			  var cDiv = document.getElementById("cDiv");
			  if (cDiv!=null) {
			    pDiv.removeChild(cDiv);
			    var newdiv = document.createElement("div");
			  newdiv.setAttribute("id","cDiv");
			    newdiv.innerHTML="N/A"
			    pDiv.appendChild(newdiv);
			  }
		  } 
	  } 
}

function sleep50ms() {
}
function restoreFpt(boName) {

  var pDiv = document.getElementById("pDiv");
    var newdiv = document.createElement("div");
      var cDiv = document.getElementById("cDiv");
  if (cDiv!=null) {
    pDiv.removeChild(cDiv);
    }
    newdiv.setAttribute("id","cDiv");

    var addedHtml

   if (boName=="budgetAgency") {
        addedHtml="<input type=\"text\" name=\"document.budget.federalPassThroughAgencyNumber\" maxlength='5' size='5' tabindex='0' value='' onchange='' onblur=\"onblur_agencyNumber('document.budget.federalPassThroughAgencyNumber','federalPassThroughAgency');addCfp('document.budget.federalPassThroughAgencyNumber')\" id=\"document.budget.federalPassThroughAgencyNumber\" style='' class=''> "+
                 "<input type='image' tabindex='5110' name='methodToCall.performLookup.(!!org.kuali.module.cg.bo.Agency!!).(((agencyNumber:document.budget.federalPassThroughAgencyNumber,fullName:document.budget.federalPassThroughAgency.fullName))).((##)).((<>)).(([])).((**)).((^^)).((&&)).((//)).((~~)).anchorGeneral' "+
                 "src='kr/static/images/searchicon.gif' border='0' class='tinybutton' valign='middle' alt='Search ' title='Search ' /> "+
	              "<div id='document.budget.federalPassThroughAgency.fullName.div' > </div> <div id='myDiv'></div>"
    } else {
        addedHtml="<input type=\"text\" name=\"document.agencyFederalPassThroughNumber\" maxlength='5' size='7' tabindex='0' value='' onchange='' onblur=\"onblur_agencyNumber('document.agencyFederalPassThroughNumber','federalPassThroughAgency');addCfp('document.agencyFederalPassThroughNumber');\" id='document.agencyFederalPassThroughNumber' style='' class=''> "+
            "<input type=\"image\" tabindex=\"1000009\" name=\"methodToCall.performLookup.(!!org.kuali.module.cg.bo.Agency!!).(((agencyNumber:document.agencyFederalPassThroughNumber,fullName:document.federalPassThroughAgency.fullName))).((##)).((<>)).(([static/images/buttonsmall_namelater.gif])).((**)).((^^)).((&&)).((//)).((~~)).anchor1\" "+
            "src=\"kr/static/images/searchicon.gif\" border=\"0\" class=\"tinybutton\" valign=\"middle\" alt=\"Search \" title=\"Search \" /> "+
			    		"<div id=\"document.federalPassThroughAgency.fullName.div\" > </div> <div id='myDiv'></div>"
    }
    
    newdiv.innerHTML=addedHtml;
	pDiv.appendChild(newdiv);
	             
}