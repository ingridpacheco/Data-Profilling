/**
*
*/
package br.ufrj.dcc.kettle.GetDBpediaData;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

public class GetDBpediaDataStep extends BaseStep implements StepInterface {
	
	private GetDBpediaDataData data;
	private GetDBpediaDataMeta meta;
	private String type;
	private String propertyValue;
	
	public GetDBpediaDataStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (GetDBpediaDataMeta) smi;
		data = (GetDBpediaDataData) sdi;
		
		return super.init(smi, sdi);
	}
	
	public void getResourceNames(Elements resources) throws KettleStepException{
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text();
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!data.dataFound.contains(resourceName)) {
            		data.dataFound.add(resourceName);
            	}
			}
			else {
				break;
			}
		}
	}
	
	public void getNotMappedResources() throws KettleStepException {
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
	
	public void getResourceProperties(String resource) {
		String DBpedia = meta.getDBpedia();
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource.replace(" ", "_"));
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
			Elements values = doc.select(String.format("label[class=\"c1\"]:has(a[href^=\"http://%s.dbpedia.org/property\"]) + div[class^=\"c2 value\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text().split(":")[1];
				if (!data.resourceProperties.contains(resourceProperty)) {
					String propertyValue = values.get(i).text();
					getFormatedValue(resource, resourceProperty, propertyValue);				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getFormatedValue(String resource, String property, String value) {
		if (value.matches("^(dbr\\W).*$") && value.contains(" ")) {
			String[] objects = value.split(" ");
			for (String obj : objects) {
				formatValue(obj);
				data.resourceProperties.add(property);
				data.propertyValues.add(propertyValue);
				data.propertyTypes.add(type);
			}
		}
		else {
			formatValue(value);
			data.resourceProperties.add(property);
			data.propertyValues.add(propertyValue);
			data.propertyTypes.add(type);
		}
	}
	
	private void formatValue(String value) {
		if (value.matches("^(xsd\\W).*$")) {
			String[] literalValue = value.split("xsd\\W")[1].split(" ");
			propertyValue = literalValue[1];
			type = literalValue[0];
		}
		else {
			if (Pattern.matches("^(dbr\\W).*$", value) || value.matches("(.*)resource(.*)")) {
				String expression = Pattern.matches("^(dbr\\W).*$", value) ? "dbr\\W" : "resource/";
				propertyValue = value.split(expression)[1];
				type = "object";
			}
			else {
				propertyValue = value;
				type = "string";
			}	
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
            	data.dataFound.add(resourceName);
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
	
	private void initializeOutputFiled() {
		FileWriter CSVwriter;
		try {
			CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
			if (meta.getOption().equals("Template properties")) {
				CSVUtils.writeLine(CSVwriter, Arrays.asList("DBpedia Version", "Template", "Property"), ',');
			}
			else {
				if (meta.getOption().equals("Template resources")) {
					CSVUtils.writeLine(CSVwriter, Arrays.asList("DBpedia Version", "Template", "Resource"), ',');
				}
				else {
					CSVUtils.writeLine(CSVwriter, Arrays.asList("DBpedia Version", "Template", "Property", "Resource", "Value", "Type"), ',');
				}
			}
			data.CSVwriter = CSVwriter;
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	private boolean writeResourceProperty(Object[] outputRow) throws KettleStepException {
			if (data.dataFound.size() > 0 || data.resourceProperties.size() > 0) {
				if (data.resourceProperties.size() == 0) {
					data.resourceName = data.dataFound.remove(0);
					getResourceProperties(data.resourceName);
				}
				String property = data.resourceProperties.remove(0);
				String value = data.propertyValues.remove(0);
				String propertyType = data.propertyTypes.remove(0);
				this.logBasic(String.format("Writting the information from: %s", data.resourceName));
				
				try {
	            	CSVUtils.writeLine(data.CSVwriter, Arrays.asList(meta.getDBpedia(), meta.getTemplate(), property, data.resourceName, value, propertyType), ',');
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				outputRow[data.outputPropertyIndex] = property;
				outputRow[data.outputTemplateIndex] = meta.getTemplate();
				outputRow[data.outputDBpediaVersion] = meta.getDBpedia();
				outputRow[data.outputResourceIndex] = data.resourceName;
				outputRow[data.outputValueIndex] = value;
				outputRow[data.outputTypeIndex] = propertyType;
				
				putRow(data.outputRowMeta, outputRow);
				return true;
			}
			else {
				this.logBasic("Transformation complete");
				
				try {
					data.CSVwriter.flush();
					data.CSVwriter.close();
					this.logBasic("Output Files were written... ");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			setOutputDone();
			return false;
		}
	}
	
	private boolean writeOutput(Object[] outputRow) throws KettleStepException {
		if (data.dataFound.size() > 0) {
			String dataName = data.dataFound.remove(0);
		
			this.logBasic(String.format("Writting the information from: %s", dataName));
			
			try {
            	CSVUtils.writeLine(data.CSVwriter, Arrays.asList(meta.getDBpedia(), meta.getTemplate(), dataName), ',');
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (meta.getOption().equals("Template properties")) {
				outputRow[data.outputPropertyIndex] = dataName;
			}
			else {
				outputRow[data.outputResourceIndex] = dataName;
			}
			
			outputRow[data.outputTemplateIndex] = meta.getTemplate();
			outputRow[data.outputDBpediaVersion] = meta.getDBpedia();
			putRow(data.outputRowMeta, outputRow);
			
			return true;
		}
		else {
			this.logBasic("Transformation complete");
			try {
				data.CSVwriter.flush();
				data.CSVwriter.close();
				this.logBasic("Output Files were written... ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setOutputDone();
			return false;
		}
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (GetDBpediaDataMeta) smi;
		data = (GetDBpediaDataData) sdi;
		
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
			data.outputTemplateIndex = data.outputRowMeta.indexOfValue( "Template" );
			data.outputDBpediaVersion = data.outputRowMeta.indexOfValue( "DBpedia Version" );
			data.outputResourceIndex = data.outputRowMeta.indexOfValue( "Resource" );
			if (meta.getOption().equals("Template resources properties") || meta.getOption().equals("Resource properties")) {
				data.outputValueIndex = data.outputRowMeta.indexOfValue( "Value" );
				data.outputTypeIndex = data.outputRowMeta.indexOfValue( "Type" );
			}
		      
			this.logBasic("Getting the template properties' informations");
			
			this.logBasic(meta.getOption());
			if (meta.getOption().equals("Template properties")) {
				this.logBasic("Getting properties");
				data.dataFound = getProperties();
			}
			else {
				if (meta.getOption().equals("Resource properties")) {
					data.dataFound.add(meta.getResource());
				}
				else {
					if (meta.getDBpedia().equals("pt") || meta.getDBpedia().equals("fr") || meta.getDBpedia().equals("ja")) {
				    	this.logBasic("Getting the resources");
				    	getResources();
				    }
				    if (meta.getNotMappedResources() == true) {
				    	this.logBasic("Getting not mapped resources");
				    	getNotMappedResources();
					}
				}
			}
			
			this.logBasic("Initializing output fields");
			
			initializeOutputFiled();
		}
		this.logBasic("Transformation complete");
		
		Object[] outputRow;
		
		if (meta.getOption().equals("Template properties")) {
			outputRow = RowDataUtil.resizeArray( inputRow, 3 );
			return writeOutput(outputRow);
		}
		else {
			if (meta.getOption().equals("Template resources")) {
				outputRow = RowDataUtil.resizeArray( inputRow, 3 );
				return writeOutput(outputRow);
			}
			else {
				outputRow = RowDataUtil.resizeArray( inputRow, 6 );
				return writeResourceProperty(outputRow);
			}
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (GetDBpediaDataMeta) smi;
		data = (GetDBpediaDataData) sdi;
		
		super.dispose(smi, sdi);
	}

}