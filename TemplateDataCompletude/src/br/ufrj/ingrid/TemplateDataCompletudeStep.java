/**
*
*/
package br.ufrj.ingrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
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

public class TemplateDataCompletudeStep extends BaseStep implements StepInterface {
	
	private TemplateDataCompletudeData data;
	private TemplateDataCompletudeMeta meta;
	
	public TemplateDataCompletudeStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateDataCompletudeMeta) smi;
		data = (TemplateDataCompletudeData) sdi;
		
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
	
	public List<String> getResources(String DBpedia, String Template) {
		List<String> templateResources = new ArrayList<>();
		
		Template = Template.replace(" ", "_");
		
		Map<String, String> mapTemplateUrl = new HashMap<String, String>();
		mapTemplateUrl.put("pt", "Predefinição");
		mapTemplateUrl.put("fr", "Modèle");
		mapTemplateUrl.put("ja", "Template");
		
		String templateUrl = String.format("http://%s.dbpedia.org/resource/%s:%s", DBpedia, mapTemplateUrl.get(DBpedia) ,Template);
		
		String queryStr =
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX dbp: ?dbpUrl \n"
				+ "SELECT DISTINCT ?uri ?name WHERE {\n"
				+ "?uri dbp:wikiPageUsesTemplate ?templateUrl. \n"
				+ "?uri rdfs:label ?label.filter langMatches(lang(?label), ?language). \n"
				+ "BIND(STR(?label)AS ?name).} \n";
		
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setCommandText(queryStr);
		pss.setIri("dbpUrl", String.format("http://%s.dbpedia.org/property/", DBpedia));
		pss.setIri("templateUrl", templateUrl);
		pss.setLiteral("language", DBpedia);
		
		String sparqlUrl = String.format("http://%s.dbpedia.org/sparql", DBpedia);
				
        Query query = QueryFactory.create(pss.asQuery());

        try ( QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlUrl, query) ) {
            ((QueryEngineHTTP)qexec).addParam("timeout", "10000") ;

            ResultSet rs = qexec.execSelect();
            
            while (rs.hasNext()) {
            	QuerySolution resource = rs.next();
            	String templateName = resource.getLiteral("name").getString();
            	templateResources.add(templateName);
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return templateResources;
	}
	
	public List<String> getMissingProperties(List<String> resourceProperties, List<String> templateProperties, Map<String, List<Integer>> propertiesData, List<String> propertiesNames) {
		List<String> missingProperties = new ArrayList<>();
		for (int i = 0; i < templateProperties.size(); i++) {			
			if (!resourceProperties.contains(templateProperties.get(i)) && !missingProperties.contains(templateProperties.get(i))) {
				missingProperties.add(templateProperties.get(i));
				
				if (propertiesData.containsKey(templateProperties.get(i))) {
					List<Integer> propertiesValues = propertiesData.get(templateProperties.get(i));
					propertiesValues.set(1, propertiesValues.get(1) + 1);
					propertiesData.put(templateProperties.get(i), propertiesValues);
				}
				else {
					List<Integer> propertiesValues = new ArrayList<Integer>();
					propertiesValues.add(0);
					propertiesValues.add(1);
					propertiesData.put(templateProperties.get(i), propertiesValues);
					propertiesNames.add(templateProperties.get(i));
				}
			}
		}
		
		return missingProperties;
	}
	
	public void checkPropertiesInResource(String DBpedia, String resource, List<String> templateProperties, Map<String, Integer> allProperties) {
		
		resource = resource.replace(" ", "_");
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text();
				if (templateProperties.contains(resourceProperty.split(":")[1])) {
					if (allProperties.containsKey(resourceProperty.split(":")[1])) {
						Integer value = allProperties.get(resourceProperty.split(":")[1]);
						allProperties.put(resourceProperty.split(":")[1], value + 1);
					}
					else {
						allProperties.put(resourceProperty.split(":")[1], 1);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<String, Integer> countMissingProperties(Map<String, Integer> missingPropertiesCount, List<String> missingProperties, List<String> allMissingProperties) {
		for (int i = 0; i < missingProperties.size(); i++) {
			if (missingPropertiesCount.containsKey(missingProperties.get(i))){
				Integer oldValue = missingPropertiesCount.get(missingProperties.get(i));
				missingPropertiesCount.put(missingProperties.get(i), oldValue + 1);
			}
			else {
				allMissingProperties.add(missingProperties.get(i));
				missingPropertiesCount.put(missingProperties.get(i), 1);
			}
		}
		return missingPropertiesCount;
	}
	
	public String getPercentage(Integer insideResources, Integer total) {
		Float percentage = (insideResources.floatValue()/total.floatValue()) * 100;
		
		return percentage.toString().concat("%");
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (TemplateDataCompletudeMeta) smi;
		data = (TemplateDataCompletudeData) sdi;
		
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
			
			data.outputPropertyIndex = data.outputRowMeta.indexOfValue( "Property" );
			data.outputInsideResourcesIndex = data.outputRowMeta.indexOfValue( "Inside Resources" );
			data.outputTotalIndex = data.outputRowMeta.indexOfValue( "Total" );
			data.outputPercentageIndex = data.outputRowMeta.indexOfValue( "Completude Percentage" );
		      
		    data.templateProperties = getProperties(meta.getDBpedia(), meta.getTemplate());
			data.templateProperties.remove(0);
			List<String> resources = getResources(meta.getDBpedia(), meta.getTemplate());
			
			for(int i = 0; i < resources.size(); i++) {
				checkPropertiesInResource(meta.getDBpedia(), resources.get(i), data.templateProperties, data.properties);
			}
			data.quantityOfResources = resources.size();
			
			FileWriter writer;
			try {
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
				 
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("There are %s properties in %s, some of which are not in all resources from this template.", data.templateProperties.size(), meta.getTemplate()));
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			this.logBasic("Output File is being written... ");
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
			    
		if (data.templateProperties.size() > 0) {
			String templateProperty = data.templateProperties.remove(0);
			
			Integer quantityOfResourcesThatHasTheProperty;
			if (data.properties.containsKey(templateProperty)) {
				quantityOfResourcesThatHasTheProperty = data.properties.get(templateProperty);
			}
			else {
				quantityOfResourcesThatHasTheProperty = 0;
			}
			
			data.totalQuantityOfResourcesThatHasTheProperty += quantityOfResourcesThatHasTheProperty;
			data.quantityTotal += data.quantityOfResources;
			
			data.percentage = getPercentage(quantityOfResourcesThatHasTheProperty, data.quantityOfResources);
			
            try {
            	data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("The property %s is in %s resources from a total of %s resources, which leads to a completude percentage of %s.", templateProperty, quantityOfResourcesThatHasTheProperty, data.quantityOfResources, data.percentage));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			outputRow[data.outputPropertyIndex] = templateProperty;
			outputRow[data.outputInsideResourcesIndex] = quantityOfResourcesThatHasTheProperty;
			outputRow[data.outputTotalIndex] = data.quantityOfResources;
			outputRow[data.outputPercentageIndex] = data.percentage;
			
			putRow(data.outputRowMeta, outputRow);
			
			return true;
		}
		else {
			
			data.percentage = getPercentage(data.totalQuantityOfResourcesThatHasTheProperty, data.quantityTotal);
			
			try {
            	data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("To sum up, there are %s properties from this template that are inside resources (the amount of all property inside resources), from a total os %s properties that should be inside resources (considering that all the properties should be in all resources). The completude percentage of this template is %s.", data.totalQuantityOfResourcesThatHasTheProperty, data.quantityTotal, data.percentage));
				data.bufferedWriter.close();
				this.logBasic("Output File was written... ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			outputRow[data.outputPropertyIndex] = "Total";
			outputRow[data.outputInsideResourcesIndex] = data.totalQuantityOfResourcesThatHasTheProperty;
			outputRow[data.outputTotalIndex] = data.quantityTotal;
			outputRow[data.outputPercentageIndex] = data.percentage;
			
			putRow(data.outputRowMeta, outputRow);
			
			return false;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateDataCompletudeMeta) smi;
		data = (TemplateDataCompletudeData) sdi;
		
		super.dispose(smi, sdi);
	}

}