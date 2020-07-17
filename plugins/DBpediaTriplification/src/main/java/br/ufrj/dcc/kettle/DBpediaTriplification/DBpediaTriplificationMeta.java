/**
*
*/
package br.ufrj.dcc.kettle.DBpediaTriplification;

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
public class DBpediaTriplificationMeta extends BaseStepMeta implements StepMetaInterface {

	private String DBpedia;
	private String template;
	public String browseOutputCSVFilename;
	private Boolean notMappedResources;
	
	public DBpediaTriplificationMeta() {
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
	
	public String getOutputCSVFile() {
		return browseOutputCSVFilename;
	}

	public void setOutputCSVFile(String browseOutputCSVFilename) {
		this.browseOutputCSVFilename = browseOutputCSVFilename;
	}
	
	public boolean getNotMappedResources() {
		return notMappedResources;
	}
	
	public void setNotMappedResources(Boolean notMappedResources) {
		this.notMappedResources = notMappedResources;
	}
	
	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
	Trans trans) {
		return new DBpediaTriplificationStep(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}
	
	@Override
	public StepDataInterface getStepData() {
		return new DBpediaTriplificationData();
	}
	
	@Override
	public void setDefault() {
		notMappedResources = false;
	// TODO Auto-generated method stub
	}
	
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
		try {
			DBpedia = XMLHandler.getTagValue(stepnode,"DBPEDIA");
			template = XMLHandler.getTagValue(stepnode,"TEMPLATE");
			browseOutputCSVFilename = XMLHandler.getTagValue(stepnode,"BROWSEOUTPUTCSVFILENAME");
			notMappedResources = "Y".equals(XMLHandler.getTagValue(stepnode, "NOTMAPPEDRESOURCES"));
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
		if (browseOutputCSVFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEOUTPUTCSVFILENAME", browseOutputCSVFilename));
		}
		retVal.append("    ").append(XMLHandler.addTagValue("NOTMAPPEDRESOURCES", notMappedResources));
		return retVal.toString();
	}
	
	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {
		try {
			DBpedia = rep.getStepAttributeString(stepId, "DBPEDIA");
			template = rep.getStepAttributeString(stepId, "TEMPLATE");
			browseOutputCSVFilename = rep.getStepAttributeString(stepId, "BROWSEOUTPUTCSVFILENAME");
			notMappedResources = rep.getStepAttributeBoolean(stepId, "NOTMAPPEDRESOURCES");
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
			if (browseOutputCSVFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEOUTPUTCSVFILENAME", browseOutputCSVFilename);
			}
			rep.saveStepAttribute(transformationId, stepId,
	                "NOTMAPPEDRESOURCES", notMappedResources);
		} catch (Exception e) {
			throw new KettleException("Unexpected error saving step Sample Plug-In from the repository", e);
		}
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException {
		ValueMetaInterface NTriples = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		NTriples.setName("N-Triples");
		NTriples.setOrigin(origin);
		rowMeta.addValueMeta(NTriples);
	}
	
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {
		CheckResult cr;
		if (prev == null || prev.size() == 0) {
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Not receiving any fields from previous steps!", stepMeta);
			remarks.add(cr);
		}
	}
}