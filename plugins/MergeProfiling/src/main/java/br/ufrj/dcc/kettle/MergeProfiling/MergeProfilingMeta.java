/**
*
*/
package br.ufrj.dcc.kettle.MergeProfiling;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

/**
* @author IngridPacheco
*
*/

public class MergeProfilingMeta extends BaseStepMeta implements StepMetaInterface {

	private String nTripleFieldName;
	private String subject;
	private String predicate;
	private Boolean isTriplified;
	public String browseInputCSVFilename;
	public String browseOutputFilename;
	public String browseOutputCSVFilename;
	private String inputChoice;
	
	public MergeProfilingMeta() {
		super(); // allocate BaseStepInfo
	}
	
	public String getInputChoice() {
		return inputChoice;
	}
	
	public void setInputChoice(String inputChoiceValue) {
		this.inputChoice = inputChoiceValue;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subjectValue) {
		this.subject = subjectValue;
	}
	
	public String getPredicate() {
		return predicate;
	}
	
	public void setPredicate(String predicateValue) {
		this.predicate = predicateValue;
	}
	
	public String getOutputFile() {
		return browseOutputFilename;
	}

	public void setOutputFile(String browseOutputFilename) {
		this.browseOutputFilename = browseOutputFilename;
	}
	
	public String getOutputCSVFile() {
		return browseOutputCSVFilename;
	}

	public void setOutputCSVFile(String browseOutputCSVFilename) {
		this.browseOutputCSVFilename = browseOutputCSVFilename;
	}
	
	public String getInputCSVFile() {
		return browseInputCSVFilename;
	}

	public void setInputCSVFile(String browseInputCSVFilename) {
		this.browseInputCSVFilename = browseInputCSVFilename;
	}
	
	public Boolean getIsTriplified() {
		return isTriplified;
	}

	public void setIsTriplified(Boolean isTriplified) {
		this.isTriplified = isTriplified;
	}
	
	public String getNTripleFieldName() {
		return nTripleFieldName;
	}
	
	public void setNTripleFieldName(String nTripleFieldNameValue) {
		this.nTripleFieldName = nTripleFieldNameValue;
	}
	
	public StepDialogInterface getDialog( Shell shell, StepMetaInterface meta, TransMeta transMeta, String name ) {
	    return new MergeProfilingDialog( shell, meta, transMeta, name );
	  }
	
	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
	Trans trans) {
		return new MergeProfilingStep(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}
	
	@Override
	public StepDataInterface getStepData() {
		return new MergeProfilingData();
	}
	
	@Override
	public void setDefault() {
		setInputChoice("N-Triple");
		setIsTriplified(false);
		
	// TODO Auto-generated method stub
	}
	
	public Object clone() {
	    Object retval = super.clone();
	    return retval;
	}
	
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
		try {
			nTripleFieldName = XMLHandler.getTagValue(stepnode,"NTRIPLEFIELDNAME");
			subject = XMLHandler.getTagValue(stepnode,"SUBJECT");
			predicate = XMLHandler.getTagValue(stepnode,"PREDICATE");
			isTriplified = "Y".equals(XMLHandler.getTagValue(stepnode, "ISTRIPLIFIED"));
			browseInputCSVFilename = XMLHandler.getTagValue(stepnode,"BROWSEINPUTCSVFILENAME");
			browseOutputFilename = XMLHandler.getTagValue(stepnode,"BROWSEOUTPUTFILENAME");
			browseOutputCSVFilename = XMLHandler.getTagValue(stepnode,"BROWSEOUTPUTCSVFILENAME");
			inputChoice = XMLHandler.getTagValue(stepnode, "INPUTCHOICE");
		} catch (Exception e) {
			throw new KettleXMLException("Load XML: Excption ", e);// Messages.getString(“KafkaTopicPartitionConsumerMeta.Exception.loadXml”),
		// e);
		}
	}
	
	public String getXML() throws KettleException {
		StringBuilder retVal = new StringBuilder();
		if (nTripleFieldName != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("NTRIPLEFIELDNAME", nTripleFieldName));
		}
		if (subject != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("SUBJECT", subject));
		}
		if (predicate != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("PREDICATE", predicate));
		}
		if (browseInputCSVFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEINPUTCSVFILENAME", browseInputCSVFilename));
		}
		if (browseOutputFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEOUTPUTFILENAME", browseOutputFilename));
		}
		if (browseOutputCSVFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEOUTPUTCSVFILENAME", browseOutputCSVFilename));
		}
		retVal.append("    ").append(XMLHandler.addTagValue("ISTRIPLIFIED", isTriplified));
		retVal.append("    ").append(XMLHandler.addTagValue("INPUTCHOICE", inputChoice));
		return retVal.toString();
	}
	
	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {
		try {
			nTripleFieldName = rep.getStepAttributeString(stepId, "NTRIPLEFIELDNAME");
			subject = rep.getStepAttributeString(stepId, "SUBJECT");
			predicate = rep.getStepAttributeString(stepId, "PREDICATE");
			browseOutputFilename = rep.getStepAttributeString(stepId, "BROWSEOUTPUTFILENAME");
			browseOutputCSVFilename = rep.getStepAttributeString(stepId, "BROWSEOUTPUTCSVFILENAME");
			browseInputCSVFilename = rep.getStepAttributeString(stepId, "BROWSEINPUTCSVFILENAME");
			inputChoice = rep.getStepAttributeString(stepId, "INPUTCHOICE");
			isTriplified = rep.getStepAttributeBoolean(stepId, "ISTRIPLIFIED");
		} catch (Exception e) {
			throw new KettleException("Unexpected error reading step Sample Plug-In from the repository", e);
		}
	}
	
	public void saveRep(Repository rep, ObjectId transformationId, ObjectId stepId) throws KettleException {
		try {
			if (nTripleFieldName != null) {
				rep.saveStepAttribute(transformationId, stepId, "NTRIPLEFIELDNAME", nTripleFieldName);
			}
			if (subject != null) {
				rep.saveStepAttribute(transformationId, stepId, "SUBJECT", subject);
			}
			if (predicate != null) {
				rep.saveStepAttribute(transformationId, stepId, "PREDICATE", predicate);
			}
			if (browseOutputFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEOUTPUTFILENAME", browseOutputFilename);
			}
			if (browseOutputCSVFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEOUTPUTCSVFILENAME", browseOutputCSVFilename);
			}
			if (browseInputCSVFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEINPUTCSVFILENAME", browseInputCSVFilename);
			}
			rep.saveStepAttribute(transformationId, stepId,
	                "ISTRIPLIFIED", isTriplified);
			rep.saveStepAttribute(transformationId, stepId,
	                "INPUTCHOICE", inputChoice);
		} catch (Exception e) {
			throw new KettleException("Unexpected error saving step Sample Plug-In from the repository", e);
		}
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException {
		ValueMetaInterface SubjectMeta = new ValueMetaString("Subject");
		SubjectMeta.setOrigin(origin);
		rowMeta.addValueMeta(SubjectMeta);
		
		ValueMetaInterface PredicatesMeta = new ValueMetaString("Predicates");
		PredicatesMeta.setOrigin(origin);
		rowMeta.addValueMeta(PredicatesMeta);
		
		ValueMetaInterface IsCSVMeta = new ValueMetaString("IsSubjectInCSV");
		IsCSVMeta.setOrigin(origin);
		rowMeta.addValueMeta(IsCSVMeta);
	}
	
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {
		CheckResult cr;
		if (prev == null || prev.size() == 0) {
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Not receiving any fields from previous steps!", stepMeta);
			remarks.add(cr);
		}
	}
}