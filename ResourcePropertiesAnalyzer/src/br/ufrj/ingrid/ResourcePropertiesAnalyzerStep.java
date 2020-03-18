/**
*
*/
package br.ufrj.ingrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
	
	public List<String> getProperties() {
		String dbpedia = meta.getDBpedia();
		String template = meta.getTemplate();
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
	
	public List<String> getResourceProperties() {
		List<String> resourceProperties = new ArrayList<>();
		String DBpedia = meta.getDBpedia();
		String resource = meta.getResource();
		
		resource = resource.replace(" ", "_");
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text();
				this.logBasic(String.format("Getting the property: %s", resourceProperty.split(":")[1]));
				if (!resourceProperties.contains(resourceProperty.split(":")[1])) {
					resourceProperties.add(resourceProperty.split(":")[1]);
				}
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
	
	public void getAllProperties() {
		List<String> templateProperties = data.templateProperties;
		List<String> resourceProperties = data.resourceProperties;
		
		Set<String> set = new LinkedHashSet<>(resourceProperties);
		set.addAll(templateProperties);
		
		List<String> allProperties = new ArrayList<>(set);
		data.allProperties = allProperties;
		
		List<String> missingProperties = new ArrayList<>(templateProperties);
		missingProperties.removeAll(resourceProperties);
		data.missingProperties = missingProperties;
		
		List<String> notMapped = new ArrayList<>(resourceProperties);
		notMapped.removeAll(templateProperties);
		data.notMappedProperties = notMapped;
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
			
			data.outputExistingPropertiesIndex = data.outputRowMeta.indexOfValue("Property");
			data.outputNotMapedPropertiesIndex = data.outputRowMeta.indexOfValue( "Is property in template?" );
			data.outputMissingPropertiesIndex = data.outputRowMeta.indexOfValue( "Is property in resource?" );
			   
			this.logBasic("Getting the template properties");
		    data.templateProperties = getProperties();
			data.templateProperties.remove(0);
			
			this.logBasic("Getting the resources' properties");
			data.resourceProperties = getResourceProperties();
			getAllProperties();
			
			this.logBasic("Output Files are being written... ");
			
			FileWriter CSVwriter;
			FileWriter writer;
			try {
				CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
				CSVUtils.writeLine(CSVwriter, Arrays.asList("Property", "Is property in template?", "Is property in resource?"), ',');
				data.CSVwriter = CSVwriter;
				
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("There are %s properties joining the template's properties and the resource's properties, in which %s are the %s's properties and %s are the %s's properties.", data.allProperties.size(), data.resourceProperties.size(), meta.getResource(), data.templateProperties.size(), meta.getTemplate()));
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("Some of this properties, %s, are not in DBpedia. Also, some of them, %s, are not in the resource.", data.notMappedProperties.size(), data.missingProperties.size()));
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 3 );
		
		if (data.allProperties.size() > 0) {
			
			
			String property = data.allProperties.remove(0);
			
			this.logBasic(String.format("Writting the information from the property: %s", property));
			
			String isMapped = "Yes";
			String isInResource = "Yes";
			
			if (data.notMappedProperties.contains(property)) {
				isMapped = "No";
			}
			
			if (data.missingProperties.contains(property)) {
				isInResource = "No";
			}
			
			if ((isInResource.equals("Yes") && isMapped.equals("Yes") && meta.getWhichProperty().equals("Properties on Resource and Template")) || (isInResource.equals("Yes") && isMapped.equals("No") && meta.getWhichProperty().equals("Properties only on Resource")) || isInResource.equals("No") && isMapped.equals("Yes") && meta.getWhichProperty().equals("Properties only on Template") || meta.getWhichProperty().equals("All")) {
				try {
					CSVUtils.writeLine(data.CSVwriter, Arrays.asList(property, isMapped.toString(), isInResource.toString()), ',');
					
					data.bufferedWriter.newLine();
					data.bufferedWriter.write(String.format("The property %s: Is in %s? %s; Is in %s? %s", property, meta.getTemplate(), isMapped, meta.getResource(), isInResource));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				outputRow[data.outputExistingPropertiesIndex] = property;
				outputRow[data.outputNotMapedPropertiesIndex] = isMapped;
				outputRow[data.outputMissingPropertiesIndex] = isInResource;
				
				putRow(data.outputRowMeta, outputRow);
			}
			
			return true;
		}
		else {
			
			this.logBasic("Transformation complete");
			
			try {
				data.CSVwriter.flush();
		        data.CSVwriter.close();
		        
				data.bufferedWriter.close();
				this.logBasic("Output Files were written... ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return false;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ResourcePropertiesAnalyzerMeta) smi;
		data = (ResourcePropertiesAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}