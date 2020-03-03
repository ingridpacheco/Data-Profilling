/**
*
*/
package br.ufrj.ingrid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
* @author IngridPacheco
*
*/

public class ResourcePropertiesAnalyzerStep extends BaseStep implements StepInterface {
	
	private ResourcePropertiesAnalyzerData data;
	private ResourcePropertiesAnalyzerMeta meta;
	
	public ResourcePropertiesAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ResourcePropertiesAnalyzerMeta) smi;
		data = (ResourcePropertiesAnalyzerData) sdi;
		
		return super.init(smi, sdi);
	}
	
	public List<String> getProperties(String dbpedia, String template) {
		List<String> templateProperties = new ArrayList<>();
		template = template.replaceAll(" ", "_");
		try {
			String url = String.format("http://mappings.dbpedia.org/index.php/Mapping_%s:%s", dbpedia, template);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select("td[width=\"400px\"]");
			
			for (int i = 0; i < properties.size(); i++) {
				String templateProperty = properties.get(i).text();
				String[] propertyName = templateProperty.split("\\s|_|-");
	
			    Integer size = propertyName.length - 1;
			    Integer counter = 1;
	
			    String newString = propertyName[0];

			    while(size > 0){
			        String newPropertyName = propertyName[counter].substring(0,1).toUpperCase().concat(propertyName[counter].substring(1));
			        System.out.println(newPropertyName);
			        newString = newString.concat(newPropertyName);
			        System.out.println(newString);
			        counter = counter + 1;
			        size = size - 1;
			    }

				templateProperties.add(newString);
			}
			
		  	return templateProperties;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		  	return templateProperties;
		}
	}
	
	public List<String> getMissingProperties(List<String> resourceProperties, List<String> templateProperties) {
		List<String> missingProperties = new ArrayList<>();

		for (int i = 0; i < templateProperties.size(); i++) {			
			if (!resourceProperties.contains(templateProperties.get(i)) && !missingProperties.contains(templateProperties.get(i))) {
				missingProperties.add(templateProperties.get(i));
			}
		}
		
		return missingProperties;
	}
	
	public List<String> getNotMappedProperties(List<String> resourceProperties, List<String> templateProperties) {
		List<String> notMappedProperties = new ArrayList<>();

		for (int i = 0; i < resourceProperties.size(); i++) {			
			if (!templateProperties.contains(resourceProperties.get(i)) && !notMappedProperties.contains(resourceProperties.get(i))) {
				notMappedProperties.add(resourceProperties.get(i));
			}
		}
		
		return notMappedProperties;
	}
	
	public List<String> getResourceProperties(String DBpedia, String resource) {
		List<String> resourceProperties = new ArrayList<>();
		resource = resource.replace(" ", "_");
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text();
				resourceProperties.add(resourceProperty.split(":")[1]);
			}
			
		  	return resourceProperties;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  	return resourceProperties;
		}
	}
	
	public String getPercentage(Integer quantity, Integer nopMapped, Integer size) {
		if (size == 0)
			return "0";
		
		Integer mappedProperties = quantity - nopMapped;
		Float percentage = (mappedProperties.floatValue()/size.floatValue()) * 100;
		
		return percentage.toString().concat("%");
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (ResourcePropertiesAnalyzerMeta) smi;
		data = (ResourcePropertiesAnalyzerData) sdi;
		
		Object[] inputRow = getRow(); // get row, blocks when needed!
		
		if (inputRow == null) // no more input to be expected…
		{
			setOutputDone();
			return false;
		}
		
		if (first) {
			first = false;
			data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
			
			data.outputExistingPropertiesIndex = data.outputRowMeta.indexOfValue("Existing Properties");
			data.outputMissingPropertiesIndex = data.outputRowMeta.indexOfValue( "Missing Properties" );
			data.outputNotMapedPropertiesIndex = data.outputRowMeta.indexOfValue( "Not Maped Properties" );
		      
		    data.templateProperties = getProperties(meta.getDBpedia(), meta.getTemplate());
			data.templateProperties.remove(0);
			data.resourceProperties = getResourceProperties(meta.getDBpedia(), meta.getResource());
			data.missingProperties = getMissingProperties(data.resourceProperties, data.templateProperties);
			data.notMappedProperties = getNotMappedProperties(data.resourceProperties, data.templateProperties);
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 3 );
		
		if (data.resourceProperties.size() > 0) {
			outputRow[data.outputExistingPropertiesIndex] = data.resourceProperties.remove(0);
		}
		else {
			outputRow[data.outputExistingPropertiesIndex] = "";
		}
		
		if (data.notMappedProperties.size() > 0) {
			outputRow[data.outputNotMapedPropertiesIndex] = data.notMappedProperties.remove(0);
		}
		else {
			outputRow[data.outputNotMapedPropertiesIndex] = "";
		}
		
		if (data.missingProperties.size() > 0) {
			outputRow[data.outputMissingPropertiesIndex] = data.missingProperties.remove(0);
		}
		else {
			outputRow[data.outputMissingPropertiesIndex] = "";
		}
		
		putRow(data.outputRowMeta, outputRow);
		
		if (data.missingProperties.size() == 0 && data.resourceProperties.size() == 0) {
			return false;
		}
		else {
			return true;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ResourcePropertiesAnalyzerMeta) smi;
		data = (ResourcePropertiesAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}