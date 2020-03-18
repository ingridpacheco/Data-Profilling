/**
*
*/
package br.ufrj.ingrid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ResourceInputAnalyzerStep extends BaseStep implements StepInterface {
	
	private ResourceInputAnalyzerData data;
	private ResourceInputAnalyzerMeta meta;
	
	public ResourceInputAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ResourceInputAnalyzerMeta) smi;
		data = (ResourceInputAnalyzerData) sdi;
		
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
			        newString = newString.concat(newPropertyName);
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
	
	public void getNotMappedProperties() {
		List<String> templateProperties = data.properties;
		List<String> csvProperties = data.csvProperties;
		
		List<String> notOnCSVProperties = new ArrayList<>(templateProperties);
		notOnCSVProperties.removeAll(csvProperties);
		data.notOnCSVProperties = notOnCSVProperties;
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (ResourceInputAnalyzerMeta) smi;
		data = (ResourceInputAnalyzerData) sdi;
		
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
			
			data.outputPropertyNameIndex = data.outputRowMeta.indexOfValue( "Property" );
			
			this.logBasic("Getting the properties");
			data.properties = getProperties();
			data.properties.remove(0);
			
			
			this.logBasic("Reading properties from CSV file");
			BufferedReader csvReader;
			try {				
				csvReader = new BufferedReader(new FileReader(meta.getBrowseFilename()));
				String row;
				while ((row = csvReader.readLine()) != null) {
					String property = row.split(",")[0];
					String[] propertyName = property.split("\\s|_|-");
		
				    Integer size = propertyName.length - 1;
				    Integer counter = 1;
		
				    String newString = propertyName[0];

				    while(size > 0){
				        String newPropertyName = propertyName[counter].substring(0,1).toUpperCase().concat(propertyName[counter].substring(1));
				        newString = newString.concat(newPropertyName);
				        counter = counter + 1;
				        size = size - 1;
				    }

					data.csvProperties.add(newString);
				}
				csvReader.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.logBasic("All CSV data read");
			
			getNotMappedProperties();
			
			FileWriter CSVwriter;
			FileWriter writer;
			try {
				CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
				CSVUtils.writeLine(CSVwriter, Arrays.asList("Property"), ',');
				data.CSVwriter = CSVwriter;
				
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
				 
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("There are %s properties inside the CSV file. Some of the template's properties are not in the CSV. These are the properties that are missing:", data.csvProperties.size()));
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
		
		if (data.notOnCSVProperties.size() > 0) {
			
			String propertyName = data.notOnCSVProperties.remove(0);
			
			this.logBasic(String.format("Writting the information from the property: %s", propertyName));
			
			try {
				CSVUtils.writeLine(data.CSVwriter, Arrays.asList(propertyName), ',');
				
				data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("The property %s is missing in the CSV", propertyName));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			outputRow[data.outputPropertyNameIndex] = propertyName;
						
			putRow(data.outputRowMeta, outputRow);
			
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
		meta = (ResourceInputAnalyzerMeta) smi;
		data = (ResourceInputAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}