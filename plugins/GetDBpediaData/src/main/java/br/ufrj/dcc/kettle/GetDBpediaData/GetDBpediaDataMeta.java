/**
*
*/
package br.ufrj.dcc.kettle.GetDBpediaData;

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
public class GetDBpediaDataMeta extends BaseStepMeta implements StepMetaInterface {

	private String DBpedia;
	private String template;
	public String browseOutputCSVFilename;
	private Boolean notMappedResources;
	private String option;
	private String resource;
	private String whichResource;
	
	public GetDBpediaDataMeta() {
		super(); // allocate BaseStepInfo
	}
	
	public String getwhichResource() {
		return whichResource;
	}
	
	public void setWhichResource(String whichResource) {
		this.whichResource = whichResource;
	}
	
	public String getOption() {
		return option;
	}
	
	public void setOption(String option) {
		this.option = option;
	}
	
	public String getResource() {
		return resource;
	}
	
	public void setResource(String resource) {
		this.resource = resource;
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
		return new GetDBpediaDataStep(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}
	
	@Override
	public StepDataInterface getStepData() {
		return new GetDBpediaDataData();
	}
	
	@Override
	public void setDefault() {
		notMappedResources = false;
		option = "Template properties";
		whichResource = "Previous Fields";
	// TODO Auto-generated method stub
	}
	
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
		try {
			DBpedia = XMLHandler.getTagValue(stepnode,"DBPEDIA");
			template = XMLHandler.getTagValue(stepnode,"TEMPLATE");
			whichResource = XMLHandler.getTagValue(stepnode,"WHICHRESOURCE");
			option = XMLHandler.getTagValue(stepnode,"OPTION");
			browseOutputCSVFilename = XMLHandler.getTagValue(stepnode,"BROWSEOUTPUTCSVFILENAME");
			resource = XMLHandler.getTagValue(stepnode,"RESOURCE");
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
		if (whichResource != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("WHICHRESOURCE", whichResource));
		}
		if (option != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("OPTION", option));
		}
		if (browseOutputCSVFilename != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("BROWSEOUTPUTCSVFILENAME", browseOutputCSVFilename));
		}
		if (resource != null) {
			retVal.append("    ").append(XMLHandler.addTagValue("RESOURCE", resource));
		}
		retVal.append("    ").append(XMLHandler.addTagValue("NOTMAPPEDRESOURCES", notMappedResources));
		return retVal.toString();
	}
	
	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {
		try {
			DBpedia = rep.getStepAttributeString(stepId, "DBPEDIA");
			whichResource = rep.getStepAttributeString(stepId, "WHICHRESOURCE");
			template = rep.getStepAttributeString(stepId, "TEMPLATE");
			option = rep.getStepAttributeString(stepId, "OPTION");
			browseOutputCSVFilename = rep.getStepAttributeString(stepId, "BROWSEOUTPUTCSVFILENAME");
			resource = rep.getStepAttributeString(stepId, "RESOURCE");
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
			if (whichResource != null) {
				rep.saveStepAttribute(transformationId, stepId, "WHICHRESOURCE", whichResource);
			}
			if (template != null) {
				rep.saveStepAttribute(transformationId, stepId, "TEMPLATE", template);
			}
			if (option != null) {
				rep.saveStepAttribute(transformationId, stepId, "OPTION", option);
			}
			if (browseOutputCSVFilename != null) {
				rep.saveStepAttribute(transformationId, stepId, "BROWSEOUTPUTCSVFILENAME", browseOutputCSVFilename);
			}
			if (resource != null) {
				rep.saveStepAttribute(transformationId, stepId, "RESOURCE", resource);
			}
			rep.saveStepAttribute(transformationId, stepId,
	                "NOTMAPPEDRESOURCES", notMappedResources);
		} catch (Exception e) {
			throw new KettleException("Unexpected error saving step Sample Plug-In from the repository", e);
		}
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException {
		rowMeta.clear();
		
		ValueMetaInterface TemplateMeta = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		TemplateMeta.setName("Template");
		TemplateMeta.setOrigin(origin);
		rowMeta.addValueMeta(TemplateMeta);
		
		ValueMetaInterface VersionMeta = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
		VersionMeta.setName("DBpedia Version");
		VersionMeta.setOrigin(origin);
		rowMeta.addValueMeta(VersionMeta);
		
		if (!option.equals("Template resources")) {
			ValueMetaInterface PropertyMeta = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
			PropertyMeta.setName("Property");
			PropertyMeta.setOrigin(origin);
			rowMeta.addValueMeta(PropertyMeta);
		}
		
		if (!option.equals("Template properties")) {
			ValueMetaInterface ResourceMeta = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
			ResourceMeta.setName("Resource");
			ResourceMeta.setOrigin(origin);
			rowMeta.addValueMeta(ResourceMeta);
		}
		
		if (!option.equals("Template properties") && !option.equals("Template resources")) {
			ValueMetaInterface ValueMeta = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
			ValueMeta.setName("Value");
			ValueMeta.setOrigin(origin);
			rowMeta.addValueMeta(ValueMeta);
			
			ValueMetaInterface TypeMeta = new ValueMeta("", ValueMetaInterface.TYPE_STRING);
			TypeMeta.setName("Type");
			TypeMeta.setOrigin(origin);
			rowMeta.addValueMeta(TypeMeta);
		}
	}
	
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {
		CheckResult cr;
		if (prev == null || prev.size() == 0) {
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Not receiving any fields from previous steps!", stepMeta);
			remarks.add(cr);
		}
	}
}