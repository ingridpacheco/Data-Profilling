/**
*
*/
package br.ufrj.ingrid;

import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

/**
* @author IngridPacheco
*
*/

@SuppressWarnings("deprecation")
public class TemplateResourceCompletudeMeta extends BaseStepMeta implements StepMetaInterface {

	private String DBpedia;
	private String template;
	private String resource;
	public String browseFilename;
	public String browseOutputFilename;
	
	public TemplateResourceCompletudeMeta() {
		super(); // allocate BaseStepInfo
	}
	
	public String getDBpedia() {
		return DBpedia;
	}
	
	public void setDBpedia(String DBpediaValue) {
		this.DBpedia = DBpediaValue;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public void setTemplate(String templateValue) {
		this.template = templateValue;
	}
	
	public String getBrowseFilename() {
		return browseFilename;
	}

	public void setBrowseFilename(String browseFilename) {
		this.browseFilename = browseFilename;
	}
	
	public String getOutputFile() {
		return browseOutputFilename;
	}

	public void setOutputFile(String browseOutputFilename) {
		this.browseOutputFilename = browseOutputFilename;
	}
	
	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
	Trans trans) {
		return new TemplateResourceCompletudeStep(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}
	
	@Override
	public StepDataInterface getStepData() {
		return new TemplateResourceCompletudeData();
	}
	
	@Override
	public void setDefault() {
	// TODO Auto-generated method stub
	}
	
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
		try {
			DBpedia = XMLHandler.getTagValue(stepnode,"DBPEDIA");
			template = XMLHandler.getTagValue(stepnode,"TEMPLATE");
			resource = XMLHandler.getTagValue(stepnode,"RESOURCE");
			browseFilename = XMLHandler.getTagValue(stepnode,"BROWSEFILENAME");
			browseOutputFilename = XMLHandler.getTagValue(stepnode,"BROWSEOUTPUTFILENAME");
		} catch (Exception e) {
			throw new KettleXMLException("Load XML: Excption ", e);// Messages.getString(“KafkaTopicPartitionConsumerMeta.Exception.loadXml”),
		// e);
		}
	}
	
	public String getXML() throws KettleException {
		StringBuilder retVal = new StringBuilder();
		if (DBpedia != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("DBPEDIA", DBpedia));
		}
		if (template != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("TEMPLATE", template));
		}
		if (resource != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("RESOURCE", resource));
		}
		
		if (browseFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEFILENAME", browseFilename));
		}
		if (browseOutputFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEOUTPUTFILENAME", browseOutputFilename));
		}
		return retVal.toString();
	}
	
	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {
		try {
			DBpedia = rep.getStepAttributeString(stepId, "DBPEDIA");
			template = rep.getStepAttributeString(stepId, "TEMPLATE");
			resource = rep.getStepAttributeString(stepId, "RESOURCE");
			browseFilename = rep.getStepAttributeString(stepId, "BROWSEFILENAME");
			browseOutputFilename = rep.getStepAttributeString(stepId, "BROWSEOUTPUTFILENAME");
		} catch (Exception e) {
			throw new KettleException("Unexpected error reading step Sample Plug-In from the repository", e);
		}
	}
	
	public void saveRep(Repository rep, ObjectId transformationId, ObjectId stepId) throws KettleException {
		try {
			if (DBpedia != null) {
				rep.saveStepAttribute(transformationId, stepId, "DBPEDIA", DBpedia);
			}
			if (template != null) {
				rep.saveStepAttribute(transformationId, stepId, "TEMPLATE", template);
			}
			if (resource != null) {
				rep.saveStepAttribute(transformationId, stepId, "RESOURCE", resource);
			}
			if (browseFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEFILENAME", browseFilename);
			}
			if (browseOutputFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEOUTPUTFILENAME", browseOutputFilename);
			}
		} catch (Exception e) {
			throw new KettleException("Unexpected error saving step Sample Plug-In from the repository", e);
		}
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException {
		ValueMetaInterface ResourcesInCSV = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		ResourcesInCSV.setName("Quantity of Resources in CSV");
		ResourcesInCSV.setOrigin(origin);
		rowMeta.addValueMeta(ResourcesInCSV);
		
		ValueMetaInterface ResourcesOnDBpedia = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		ResourcesOnDBpedia.setName("Resources found on DBpedia");
		ResourcesOnDBpedia.setOrigin(origin);
		rowMeta.addValueMeta(ResourcesOnDBpedia);
		
		ValueMetaInterface ResourcesMissing = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		ResourcesMissing.setName("Resources missing on DBpedia");
		ResourcesMissing.setOrigin(origin);
		rowMeta.addValueMeta(ResourcesMissing);
		
		ValueMetaInterface Percentage = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		Percentage.setName("Completude Percentage");
		Percentage.setOrigin(origin);
		rowMeta.addValueMeta(Percentage);
	}
	
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {
		CheckResult cr;
		if (prev == null || prev.size() == 0) {
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Not receiving any fields from previous steps!", stepMeta);
			remarks.add(cr);
		}
		
		CheckResultInterface ok = new CheckResult(CheckResult.TYPE_RESULT_OK, "", stepMeta);
		if (browseFilename == null || browseFilename.length() == 0) {
			ok = new CheckResult(CheckResult.TYPE_RESULT_ERROR, "No files can be found to read.", stepMeta);
			remarks.add(ok);
		} else {
			ok = new CheckResult(CheckResult.TYPE_RESULT_OK, "Both shape file and the DBF file are defined.", stepMeta);
			remarks.add(ok);
		}
	}
}