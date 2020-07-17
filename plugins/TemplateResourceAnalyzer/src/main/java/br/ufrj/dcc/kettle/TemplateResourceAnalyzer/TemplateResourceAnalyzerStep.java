/**
*
*/
package br.ufrj.dcc.kettle.TemplateResourceAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

public class TemplateResourceAnalyzerStep extends BaseStep implements StepInterface {
	
	private TemplateResourceAnalyzerData data;
	private TemplateResourceAnalyzerMeta meta;
	
	public TemplateResourceAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateResourceAnalyzerMeta) smi;
		data = (TemplateResourceAnalyzerData) sdi;
		
		return super.init(smi, sdi);
	}
	
	public List<String> getResourceNames(Elements resources, List<String> notMappedResources){
		Map<String,Float> resourcesCompletenessPercentage = data.resourcesCompletenessPercentage;
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text();
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!notMappedResources.contains(resourceName)) {
            		Float resourcePercentage = getResourcePercentage(resourceName);
                	resourcesCompletenessPercentage.put(resourceName, resourcePercentage);
            		notMappedResources.add(resourceName);
            	}
			}
			else {
				break;
			}
		}
		
		return notMappedResources;
	}
	
	public List<String> getNotMappedResources() {
		this.logBasic("Getting the not mapped resources");
		List<String> notMappedResources = data.resources;
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
			
			notMappedResources = getResourceNames(resources, notMappedResources);
			
			if (quantity > 2000) {
				Integer timesDivided = quantity/2000;
				while (timesDivided > 0) {
					String newUrl = newPage.attr("href");
					newUrl = newUrl.replaceAll("amp;", "");
					String otherPageUrl = String.format("https://%s.wikipedia.org%s", DBpedia, newUrl);
					Document moreResourceDocs = Jsoup.connect(otherPageUrl).get();
					resources = moreResourceDocs.select("li a[href^=\"/wiki/\"]");
					newPage = moreResourceDocs.select("p ~ a[href^=\"/w/index.php?\"]").get(1);
					notMappedResources = getResourceNames(resources, notMappedResources);
					timesDivided -= 1;
				}
			}
			
			return notMappedResources;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return notMappedResources;
		}
	}
	
	public List<String> getProperties() {
		List<String> templateProperties = new ArrayList<>();
		
		try {
			String url = String.format("http://mappings.dbpedia.org/index.php/Mapping_%s:%s", meta.getDBpedia(), meta.getTemplate().replaceAll(" ", "_"));
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
	
	public Float getResourcePercentage(String resourceName) {
		List<String> templateProperties = data.templateProperties;
		List<String> resourceProperties = getResourceProperties(meta.getDBpedia(), resourceName);
		data.resourcesExistingProperties.put(resourceName, resourceProperties.size());
		List<String> notMappedProperties = getNotMappedProperties(resourceProperties, templateProperties);
		data.resourcesNotMappedProperties.put(resourceName, notMappedProperties.size());
		List<String> missingProperties = getMissingProperties(resourceProperties, templateProperties);
		data.resourcesMissingProperties.put(resourceName, missingProperties.size());
		return getPercentage(resourceProperties.size(), notMappedProperties.size(), templateProperties.size());
	}
	
	public List<String> getResources() {
		Map<String, String> mapTemplateUrl = new HashMap<String, String>();
		mapTemplateUrl.put("pt", "Predefinição");
		mapTemplateUrl.put("fr", "Modèle");
		mapTemplateUrl.put("ja", "Template");
		return getSparqlResources(mapTemplateUrl.get(meta.getDBpedia()));
	}
		
	private List<String> getSparqlResources(String templateDefinition) {
		String DBpedia = meta.getDBpedia();
		Integer limit = 500;
		
		List<String> templateResources = new ArrayList<>();
		
		String templateUrl = String.format("http://%s.dbpedia.org/resource/%s:%s", DBpedia, templateDefinition, meta.getTemplate().replace(" ", "_"));
		
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
            	String templateName = resource.getLiteral("name").getString();
        		this.logBasic(String.format("Getting the resource: %s", templateName));
            	Float resourcePercentage = getResourcePercentage(templateName);
            	data.resourcesCompletenessPercentage.put(templateName, resourcePercentage);
            	
            	templateResources.add(templateName);
            	
            	if (limit == 0) {
            		TimeUnit.MINUTES.sleep(3);
            		limit = 500;
            	}
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return templateResources;
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
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource.replace(" ", "_"));
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text();
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
	
	public Float getPercentage(Integer resourceProperties, Integer notMapped, Integer templateProperties) {
		if (templateProperties == 0)
			return (float) 0;
		
		Integer mappedProperties = resourceProperties - notMapped;
		Float percentage = (mappedProperties.floatValue()/templateProperties.floatValue()) * 100;
		
		return percentage;
	}
	
	private void getDBpediaResources() {
		if (meta.getDBpedia().equals("pt") || meta.getDBpedia().equals("fr") || meta.getDBpedia().equals("ja")) {
			this.logBasic("Getting the resources");
			data.resources = getResources();
		}
		if (meta.getNotMappedResources() == true) {
			data.resources = getNotMappedResources();
		}
		data.quantity = data.resources.size();
	}
	
	private void orderResources() {
		if (meta.getOrder().equals("Ascending")) {
			data.resourcesCompletenessPercentage = data.resourcesCompletenessPercentage.entrySet()
	                .stream()
	                .sorted(Map.Entry.comparingByValue())
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			Set<String> keys = data.resourcesCompletenessPercentage.keySet();
			List<String> resourcesInOrder = new ArrayList<String>();
			resourcesInOrder.addAll(keys);
			data.resources = resourcesInOrder;
			
		}
		if (meta.getOrder().equals("Descending")) {
			data.resourcesCompletenessPercentage = data.resourcesCompletenessPercentage.entrySet()
	                .stream()
	                .sorted((Map.Entry.<String, Float>comparingByValue().reversed()))
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			Set<String> keys = data.resourcesCompletenessPercentage.keySet();
			List<String> resourcesInOrder = new ArrayList<String>();
			resourcesInOrder.addAll(keys);
			data.resources = resourcesInOrder;
		}
	}
	
	private void initializeOutputFiles() {
		FileWriter CSVwriter;
		FileWriter writer;
		try {
			CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
			CSVUtils.writeLine(CSVwriter, Arrays.asList("Resources", "Existing Properties", "Missing Properties", "Total", "Completeness Percentage (%)"), ',');
			data.CSVwriter = CSVwriter;
			
			writer = new FileWriter(meta.getOutputFile(), true);
			data.bufferedWriter = new BufferedWriter(writer);
            data.bufferedWriter.write("The result of the analysis was:");
            data.bufferedWriter.newLine();
            data.bufferedWriter.write(String.format("There are %s resources inside %s. In some cases, there are properties that are not mapped in the template or template properties that are not in the resource.", data.resources.size(), meta.getTemplate()));
            
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	private void writeOutput(Object[] outputRow) throws KettleStepException {
		String resourceName = data.resources.remove(0);
		this.logBasic(String.format("Writting the information from the resource: %s", resourceName));
		data.percentage = data.resourcesCompletenessPercentage.get(resourceName);
		data.totalResourcesExistingProperties = data.totalResourcesExistingProperties + data.resourcesExistingProperties.get(resourceName);
		data.totalResourcesNotMappedProperties = data.totalResourcesNotMappedProperties + data.resourcesNotMappedProperties.get(resourceName);
		data.totalResourcesMissingProperties = data.totalResourcesMissingProperties + data.resourcesMissingProperties.get(resourceName);
		
        try {
        	CSVUtils.writeLine(data.CSVwriter, Arrays.asList(resourceName, data.resourcesExistingProperties.get(resourceName).toString(), data.resourcesMissingProperties.get(resourceName).toString(), String.format("%s", data.templateProperties.size()), data.percentage.toString()), ',');

        	data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("The resource %s has %s properties, in which %s template properties are not in it, and %s properties are not mapped in the template.", resourceName, data.resourcesExistingProperties.get(resourceName), data.resourcesMissingProperties.get(resourceName), data.resourcesNotMappedProperties.get(resourceName)));
			data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("As the template has %s properties, it leads to a completude percentage of %s.", data.templateProperties.size(), data.percentage));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		outputRow[data.outputResourcesIndex] = resourceName;
		outputRow[data.outputExistingPropertiesIndex] = data.resourcesExistingProperties.get(resourceName);
		outputRow[data.outputMissingPropertiesIndex] = data.resourcesMissingProperties.get(resourceName);
		outputRow[data.outputTotalIndex] = data.templateProperties.size();
		outputRow[data.outputPercentageIndex] = data.percentage;
		
		putRow(data.outputRowMeta, outputRow);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (TemplateResourceAnalyzerMeta) smi;
		data = (TemplateResourceAnalyzerData) sdi;
		
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
			
			data.outputResourcesIndex = data.outputRowMeta.indexOfValue( "Resources" );
			data.outputExistingPropertiesIndex = data.outputRowMeta.indexOfValue( "Existing Properties" );
			data.outputMissingPropertiesIndex = data.outputRowMeta.indexOfValue( "Missing Properties" );
			data.outputTotalIndex = data.outputRowMeta.indexOfValue( "Total" );
			data.outputPercentageIndex = data.outputRowMeta.indexOfValue( "Completeness Percentage (%)" );
		      
			this.logBasic("Getting the template properties");
		    data.templateProperties = getProperties();
			data.templateProperties.remove(0);
			
			getDBpediaResources();
			
			this.logBasic("Sorting the resources");
			
			orderResources();
			
			this.logBasic("Output Files are being written... ");
			
			initializeOutputFiles();
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 5 );
		
		if (data.resources.size() > 0) {
			
			writeOutput(outputRow);
			return true;
		}
		else {
			data.percentage = getPercentage(data.totalResourcesExistingProperties, data.totalResourcesNotMappedProperties, data.templateProperties.size()*data.quantity);
			outputRow[data.outputResourcesIndex] = "Total";
			outputRow[data.outputExistingPropertiesIndex] = data.totalResourcesExistingProperties;
			outputRow[data.outputMissingPropertiesIndex] = data.totalResourcesMissingProperties;
			outputRow[data.outputTotalIndex] = data.templateProperties.size() * data.quantity;
			outputRow[data.outputPercentageIndex] = data.percentage;
			this.logBasic("Transformation complete");
			try {
				CSVUtils.writeLine(data.CSVwriter, Arrays.asList("Total", data.totalResourcesExistingProperties.toString(), data.totalResourcesMissingProperties.toString(), String.format("%s", data.templateProperties.size() * data.quantity), data.percentage.toString()), ',');
				data.CSVwriter.flush();
		        data.CSVwriter.close();
		        
		        data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("To sum up, there are %s resource that has %s properties, in which %s template properties are not in it, and %s properties are not mapped in the template. The completeness percentage of the template is %s", data.quantity, data.totalResourcesExistingProperties, data.totalResourcesMissingProperties, data.totalResourcesNotMappedProperties, data.percentage));
				data.bufferedWriter.close();
				this.logBasic("Output Files were written.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateResourceAnalyzerMeta) smi;
		data = (TemplateResourceAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}