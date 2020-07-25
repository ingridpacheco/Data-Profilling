/**
*
*/
package br.ufrj.dcc.kettle.TemplatePropertyAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
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

public class TemplatePropertyAnalyzerStep extends BaseStep implements StepInterface {
	
	private TemplatePropertyAnalyzerData data;
	private TemplatePropertyAnalyzerMeta meta;
	
	public TemplatePropertyAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplatePropertyAnalyzerMeta) smi;
		data = (TemplatePropertyAnalyzerData) sdi;
		
		return super.init(smi, sdi);
	}
	
	public void getResourceNames(Elements resources){
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text();
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!data.resources.contains(resourceName)) {
            		getResourceProperties(meta.getDBpedia(), resourceName);
            		data.resources.add(resourceName);
            	}
			}
			else {
				break;
			}
		}
	}
	
	public void getNotMappedResources() {
		this.logBasic("Getting the not mapped resources");
		String DBpedia = meta.getDBpedia();
		String template = meta.getTemplate();
		
		try {
			String url = String.format("https://tools.wmflabs.org/templatecount/index.php?lang=%s&namespace=10&name=%s#bottom", DBpedia, template);
			this.logBasic(String.format("Url: %s", url));
			Document doc = Jsoup.connect(url).get();
			Integer quantity = Integer.parseInt(doc.select("form + h3 + p").text().split(" ")[0]);
			this.logBasic(String.format("Quantity %s", quantity));
			
			String resourcesUrl = String.format("https://%s.wikipedia.org/wiki/Special:WhatLinksHere/Template:%s?limit=2000&namespace=0", DBpedia, template);
			Document resourcesDoc = Jsoup.connect(resourcesUrl).get();
			Elements resources = resourcesDoc.select("li a[href^=\"/wiki/\"]");
			Element newPage = resourcesDoc.select("p ~ a[href^=\"/w/index.php?\"]").first();
			
			this.logBasic(String.format("Not mapped resources: %s", resources.size()));
			
			getResourceNames(resources);
			
			if (quantity > 2000) {
				Integer timesDivided = quantity/2000;
				while (timesDivided > 0) {
					String newUrl = newPage.attr("href");
					newUrl = newUrl.replaceAll("amp;", "");
					String otherPageUrl = String.format("https://%s.wikipedia.org%s", DBpedia, newUrl);
					Document moreResourceDocs = Jsoup.connect(otherPageUrl).get();
					resources = moreResourceDocs.select("li a[href^=\"/wiki/\"]");
					newPage = moreResourceDocs.select("p ~ a[href^=\"/w/index.php?\"]").get(1);
					getResourceNames(resources);
					timesDivided -= 1;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<String> getProperties() {
		List<String> templateProperties = new ArrayList<>();
		
		try {
			String url = String.format("http://mappings.dbpedia.org/index.php/Mapping_%s:%s", meta.getDBpedia(), meta.getTemplate().replaceAll(" ", "_"));
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select("td[width=\"400px\"]");
			
			for (int i = 1; i < properties.size(); i++) {
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
	
	public void getResourceProperties(String DBpedia, String resource) {
		List<String> resourceProperties = new ArrayList<>();
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource.replace(" ", "_"));
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text().split(":")[1];
				if (!resourceProperties.contains(resourceProperty)) {
					resourceProperties.add(resourceProperty);
					if (!data.properties.containsKey(resourceProperty)) {
						data.properties.put(resourceProperty, 1);
					}
					else {
						Integer quantity = data.properties.get(resourceProperty);
						data.properties.put(resourceProperty, quantity + 1);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getResources() {
		Map<String, String> mapTemplateUrl = new HashMap<String, String>();
		mapTemplateUrl.put("pt", "Predefinição");
		mapTemplateUrl.put("fr", "Modèle");
		mapTemplateUrl.put("ja", "Template");
		
		getSparqlResources(mapTemplateUrl.get(meta.getDBpedia()));
	}
	
	private void getSparqlResources(String templateDefinition) {
		String DBpedia = meta.getDBpedia();
		String template = meta.getTemplate().replace(" ", "_");
		String templateUrl = String.format("http://%s.dbpedia.org/resource/%s:%s", DBpedia, templateDefinition, template);
		Integer limit = 500;
		
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
            	limit = limit - 1;
            	
            	QuerySolution resource = rs.next();
            	String resourceName = resource.getLiteral("name").getString();
            	this.logBasic(String.format("Resource: %s", resourceName));
            	getResourceProperties(DBpedia, resourceName);
            	data.resources.add(resourceName);
            	
            	if (limit == 0) {
            		TimeUnit.MINUTES.sleep(3);
            		limit = 500;
            	}
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
	
	public void checkPropertiesInResource(String resource) {
		String DBpedia = meta.getDBpedia();
		List<String> templateProperties = data.templateProperties;
		Map<String, Integer> allProperties = data.properties;
		
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
	
	public Float getPercentage(Integer insideResources, Integer total) {
		Float percentage = (insideResources.floatValue()/total.floatValue()) * 100;
		return percentage;
	}
	
	private void getResourceQuantity() {
		if (meta.getDBpedia().equals("pt") || meta.getDBpedia().equals("fr") || meta.getDBpedia().equals("ja")) {
	    	this.logBasic("Getting the resources");
	    	getResources();
	    }
	    if (meta.getNotMappedResources() == true) {
			getNotMappedResources();
		}
	}
	
	private void getPropertiesInformation() {
		data.templateProperties = getProperties();
	    getResourceQuantity();
		
		for(int i = 0; i < data.templateProperties.size(); i++) {
			String property = data.templateProperties.get(i);
			if (!data.properties.containsKey(property)) {
				data.properties.put(property, 0);
			}
			Float percentage = getPercentage(data.properties.get(property), data.resources.size());
			data.propertiesPercentage.put(property, percentage);
		}
	}
	
	private void sortProperties() {
		if (meta.getOrder().equals("Ascending")) {
			data.propertiesPercentage = data.propertiesPercentage.entrySet()
	                .stream()
	                .sorted(Map.Entry.comparingByValue())
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			Set<String> keys = data.propertiesPercentage.keySet();
			List<String> propertiesInOrder = new ArrayList<String>();
			propertiesInOrder.addAll(keys);
			data.templateProperties = propertiesInOrder;
			
		}
		if (meta.getOrder().equals("Descending")) {
			data.propertiesPercentage = data.propertiesPercentage.entrySet()
	                .stream()
	                .sorted((Map.Entry.<String, Float>comparingByValue().reversed()))
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			Set<String> keys = data.propertiesPercentage.keySet();
			List<String> propertiesInOrder = new ArrayList<String>();
			propertiesInOrder.addAll(keys);
			data.templateProperties = propertiesInOrder;
		}
	}
	
	private void initializeOutputFiles() {
		FileWriter CSVwriter;
		FileWriter writer;
		try {
			CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
			CSVUtils.writeLine(CSVwriter, Arrays.asList("Property", "Inside Resources", "Total", "Completeness Percentage (%)"), ',');
			data.CSVwriter = CSVwriter;
			
			writer = new FileWriter(meta.getOutputFile(), true);
			data.bufferedWriter = new BufferedWriter(writer);
			 
            data.bufferedWriter.write("The result of the analysis was:");
            data.bufferedWriter.newLine();
            data.bufferedWriter.write(String.format("There are %s properties, some of which are not in all resources from this template.", data.templateProperties.size()));
            
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	private boolean writeFinalOutput(Object[] inputRow) throws KettleStepException {
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
		
		if (data.templateProperties.size() > 0) {
			writeOutput(outputRow, data.templateProperties.remove(0));
			return true;
		}
		else {
			Float percentage = getPercentage(data.totalQuantityOfResourcesThatHasTheProperty, data.totalOfResources);
			outputRow[data.outputPropertyIndex] = "Total";
			outputRow[data.outputInsideResourcesIndex] = data.totalQuantityOfResourcesThatHasTheProperty;
			outputRow[data.outputTotalIndex] = data.totalOfResources;
			outputRow[data.outputPercentageIndex] = percentage;
			this.logBasic("Transformation complete");
			try {
				CSVUtils.writeLine(data.CSVwriter, Arrays.asList("Total", data.totalQuantityOfResourcesThatHasTheProperty.toString(), String.valueOf(data.totalOfResources), percentage.toString()), ',');
				
				data.CSVwriter.flush();
		        data.CSVwriter.close();
            	data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("To sum up, there are %s properties from this template that are inside resources (the amount of all property inside resources), from a total os %s properties that should be inside resources (considering that all the properties should be in all resources). The completude percentage of this template is %s.", data.totalQuantityOfResourcesThatHasTheProperty, data.totalOfResources, percentage));
				data.bufferedWriter.close();
				this.logBasic("Output Files were written... ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			putRow(data.outputRowMeta, outputRow);
			setOutputDone();
			return false;
		}
	}
	
	private void writeOutput(Object[] outputRow, String templateProperty) throws KettleStepException {    
		Float percentage = getPercentage(data.properties.get(templateProperty), data.resources.size());
		
		this.logBasic(String.format("Writting the information from the property: %s", templateProperty));
					
		data.totalQuantityOfResourcesThatHasTheProperty += data.properties.get(templateProperty);
		
        try {
        	CSVUtils.writeLine(data.CSVwriter, Arrays.asList(templateProperty, data.properties.get(templateProperty).toString(), String.valueOf(data.resources.size()), percentage.toString()), ',');
        	
        	data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("The property %s is in %s resources from a total of %s resources, which leads to a completude percentage of %s.", templateProperty, data.properties.get(templateProperty), data.resources.size(), percentage));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		outputRow[data.outputPropertyIndex] = templateProperty;
		outputRow[data.outputInsideResourcesIndex] = data.properties.get(templateProperty);
		outputRow[data.outputTotalIndex] = data.resources.size();
		outputRow[data.outputPercentageIndex] = percentage;
		
		putRow(data.outputRowMeta, outputRow);
	}
	
	private void removeDuplicatedProperties() {
		data.templateProperties = new ArrayList<>(
			      new HashSet<>(data.templateProperties));
	}
	
	private void getPropertiesCompleteness(Object[] inputRow) throws KettleValueException {
		String[] templateProperties = getInputRowMeta().getString(inputRow, meta.getTemplateProperties(), "").split(", ");
		data.templateProperties = new ArrayList<String>(Arrays.asList(templateProperties));
		removeDuplicatedProperties();
		
		if (data.propertiesPercentage.size() == 0 && data.properties.size() == 0) {
			initializePropertiesPercentage();
		}
		String resource = getInputRowMeta().getString(inputRow, meta.getResource(), "");
		data.resources.add(resource);
		String resourceProperty = getInputRowMeta().getString(inputRow, meta.getResourceProperties(), "");
		if (data.templateProperties.contains(resourceProperty) && ((data.resourcesProperties.containsKey(resource) && !data.resourcesProperties.get(resource).contains(resourceProperty)) || !data.resourcesProperties.containsKey(resource))) {
			HashSet<String> newResourceProperties = new HashSet<String>();
			
			if (data.resourcesProperties.containsKey(resource)) {
				newResourceProperties = data.resourcesProperties.get(resource);
			}
			
			newResourceProperties.add(resourceProperty);
			data.resourcesProperties.put(resource, newResourceProperties);
			
			if (!data.properties.containsKey(resourceProperty)) {
				data.properties.put(resourceProperty, 1);
			}
			else {
				Integer quantity = data.properties.get(resourceProperty);
				data.properties.put(resourceProperty, quantity + 1);
			}
		}
	}
	
	public void initializePropertiesPercentage() {
		for (String template : data.templateProperties) {
			data.propertiesPercentage.put(template, (float) 0);
			data.properties.put(template, 0);
		}
	}
	
	public void insert(String template){
		Map<String, Float> sortedList = new HashMap<>();
		String order = meta.getOrder();
		
		if (order.equals("Ascending")) {
			sortedList = data.propertiesPercentage.entrySet().stream()
				       .sorted(Map.Entry.comparingByValue())
				       .collect(Collectors.toMap(
				          Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		}
		else {
			sortedList = data.propertiesPercentage.entrySet().stream()
				       .sorted((Map.Entry.<String, Float>comparingByValue().reversed()))
				       .collect(Collectors.toMap(
				          Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		}
		data.propertiesPercentage = sortedList;
		data.templateProperties = new ArrayList<>(sortedList.keySet());
	}
	
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (TemplatePropertyAnalyzerMeta) smi;
		data = (TemplatePropertyAnalyzerData) sdi;
		
		Object[] inputRow = getRow(); // get row, blocks when needed!
		
		if (inputRow == null) // no more input to be expected…
		{
			if (meta.getChooseInput().equals("Previous fields input")) {
				
				Object[] outputRow = new Object[4];
				
				initializeOutputFiles();
				
				try {
					data.bufferedWriter.write(String.format("There are %s resources. In some cases, there are properties that are not mapped in the template or template properties that are not in the resource.", data.resources.size()));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				for (String template: data.templateProperties) {
					Float completeness = getPercentage(data.properties.get(template), data.resources.size());
					data.propertiesPercentage.put(template, completeness);
					insert(template);
				}
				
				data.totalOfResources = data.resources.size() * data.templateProperties.size();
				
				for (String template : data.templateProperties) {
					this.logBasic(template);
					writeOutput(outputRow, template);
				}
				
				Float percentage = getPercentage(data.totalQuantityOfResourcesThatHasTheProperty, data.totalOfResources);
				outputRow[data.outputPropertyIndex] = "Total";
				outputRow[data.outputInsideResourcesIndex] = data.totalQuantityOfResourcesThatHasTheProperty;
				outputRow[data.outputTotalIndex] = data.totalOfResources;
				outputRow[data.outputPercentageIndex] = percentage;
				this.logBasic("Transformation complete");
				try {
					CSVUtils.writeLine(data.CSVwriter, Arrays.asList("Total", data.totalQuantityOfResourcesThatHasTheProperty.toString(), String.valueOf(data.totalOfResources), percentage.toString()), ',');
					
					data.CSVwriter.flush();
			        data.CSVwriter.close();
	            	data.bufferedWriter.newLine();
					data.bufferedWriter.write(String.format("To sum up, there are %s properties from this template that are inside resources (the amount of all property inside resources), from a total os %s properties that should be inside resources (considering that all the properties should be in all resources). The completude percentage of this template is %s.", data.totalQuantityOfResourcesThatHasTheProperty, data.totalOfResources, percentage));
					data.bufferedWriter.close();
					this.logBasic("Output Files were written... ");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				putRow(data.outputRowMeta, outputRow);
			}
			
			setOutputDone();
			return false;
		}
		
		if (first) {
			first = false;
			
			data.outputRowMeta = meta.getChooseInput().equals("Previous fields input") ? new RowMeta() : (RowMetaInterface) getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
			
			data.outputPropertyIndex = data.outputRowMeta.indexOfValue( "Property" );
			data.outputInsideResourcesIndex = data.outputRowMeta.indexOfValue( "Inside Resources" );
			data.outputTotalIndex = data.outputRowMeta.indexOfValue( "Total" );
			data.outputPercentageIndex = data.outputRowMeta.indexOfValue( "Completeness Percentage (%s)" );
		      
			this.logBasic("Getting the template properties' informations");
			
			if (!meta.getChooseInput().equals("Previous fields input")) {
				
				getPropertiesInformation();
				
				this.logBasic("Sorting the properties");
				
				sortProperties();
				
				this.logBasic("Output Files are being written... ");
				
				initializeOutputFiles();
				
				data.totalOfResources = data.resources.size() * data.templateProperties.size();
				
				try {
					data.bufferedWriter.write(String.format("There are %s properties inside %s. In some cases, there are resources that doesn't have them.", data.templateProperties.size(), meta.getTemplate()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		if (!meta.getChooseInput().equals("Previous fields input")) {
			return writeFinalOutput(inputRow);
		}
		else {
			getPropertiesCompleteness(inputRow);
			return true;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplatePropertyAnalyzerMeta) smi;
		data = (TemplatePropertyAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}