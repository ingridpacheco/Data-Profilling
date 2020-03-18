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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

public class TemplateResourceInputAnalyzerStep extends BaseStep implements StepInterface {
	
	private TemplateResourceInputAnalyzerData data;
	private TemplateResourceInputAnalyzerMeta meta;
	
	public TemplateResourceInputAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateResourceInputAnalyzerMeta) smi;
		data = (TemplateResourceInputAnalyzerData) sdi;
		
		return super.init(smi, sdi);
	}
	
	public List<String> getResourceNames(Elements resources, List<String> notMappedResources){
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text().split("\\(")[0];
            	if (resourceName.charAt(resourceName.length() - 1) == ' '){
            		resourceName = resourceName.substring(0, resourceName.length() - 1);
            	}
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!notMappedResources.contains(StringUtils.stripAccents(resourceName.replaceAll("-* *", "").toLowerCase()))) {
            		notMappedResources.add(StringUtils.stripAccents(resourceName.replaceAll("-* *", "").toLowerCase()));
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
	
	public List<String> getResources() {
		String DBpedia = meta.getDBpedia();
		String Template = meta.getTemplate();
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
            	String templateName = resource.getLiteral("name").getString().split("\\(")[0];
            	
            	if (templateName.charAt(templateName.length() - 1) == ' '){
            	      templateName = templateName.substring(0, templateName.length() - 1);
            	}
            	
            	templateName = templateName.replaceAll("-* *", "");
            	
            	templateResources.add(StringUtils.stripAccents(templateName.toLowerCase()));
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return templateResources;
	}
	
	public String checkPropertyInResource(String DBpedia, String resource, String property) {
		
		resource = resource.replace(" ", "_");
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property/%s\"]", DBpedia, property));
	
			if (properties.size() == 0) {
				return "No";
			}
			else {
				return "Yes";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "No";
		}
	}
	
	public Boolean checkResource(String resource) {
		List<String> DBpediaResources = data.resources;
		
		this.logBasic(String.format("Check resource %s", resource));
		
		if (DBpediaResources.contains(resource)) {
			return true;
		}
		return false;
	}
	
	public String checkPercentage(Integer insideResources, Integer total) {
		Float percentage = (insideResources.floatValue()/total.floatValue()) * 100;
		
		return percentage.toString().concat("%");
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (TemplateResourceInputAnalyzerMeta) smi;
		data = (TemplateResourceInputAnalyzerData) sdi;
		
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
			
			data.outputResourceNameIndex = data.outputRowMeta.indexOfValue( "Resource" );
			data.outputResourcesOnDBpediaIndex = data.outputRowMeta.indexOfValue( "Resource is on DBpedia?" );
			
			if (meta.getDBpedia().equals("pt") || meta.getDBpedia().equals("fr") || meta.getDBpedia().equals("ja")) {
				this.logBasic("Getting the mapped resources");
				data.resources = getResources();
			}
			
			if (meta.getNotMappedResources() == true) {
				data.resources = getNotMappedResources();
			}
			
			BufferedReader csvReader;
			try {				
				csvReader = new BufferedReader(new FileReader(meta.getBrowseFilename()));
				String row;
				while ((row = csvReader.readLine()) != null) {
					String resourceName = row.split(",")[0];
					data.csvResources.add(resourceName);
					if (checkResource(StringUtils.stripAccents(resourceName.replaceAll("-* *", "").toLowerCase()))) {
						data.resourcesFound.add(resourceName);
					}
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
			
			data.quantity = data.csvResources.size();
			
			FileWriter CSVwriter;
			FileWriter writer;
			try {
				CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
				CSVUtils.writeLine(CSVwriter, Arrays.asList("Resource", "Resource is on DBpedia?"), ',');
				data.CSVwriter = CSVwriter;
				
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
				 
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("There are %s resources inside the CSV file, some of which are not in DBpedia.", data.csvResources.size()));
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
		
		if (data.csvResources.size() > 0) {
			
			String resourceName = data.csvResources.remove(0);
			
			this.logBasic(String.format("Writting the information from the resource: %s", resourceName));
			
			String isOnDBpedia = "";
			
			if (data.resourcesFound.contains(resourceName)) {
				isOnDBpedia = "Yes";
			}
			else {
				isOnDBpedia = "No";
			}
			
			if ((isOnDBpedia.equals("Yes") && meta.getResource().equals("Resources on DBpedia")) || (isOnDBpedia.equals("No") && meta.getResource().equals("Resources missing in DBpedia")) || meta.getResource().equals("All")) {
				
				try {
					CSVUtils.writeLine(data.CSVwriter, Arrays.asList(resourceName, isOnDBpedia), ',');
					
					data.bufferedWriter.newLine();
					data.bufferedWriter.write(String.format("The resource %s is on DBpedia? %s", resourceName, isOnDBpedia));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				outputRow[data.outputResourceNameIndex] = resourceName;
				outputRow[data.outputResourcesOnDBpediaIndex] = isOnDBpedia;
							
				putRow(data.outputRowMeta, outputRow);
			
			}
			
			return true;
		
		}
		else {		
			
			this.logBasic("Transformation complete");
			
			try {
				data.CSVwriter.flush();
		        data.CSVwriter.close();
		        
		        data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("The completeness percentage is %s", checkPercentage(data.resourcesFound.size(), data.quantity)));
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
		meta = (TemplateResourceInputAnalyzerMeta) smi;
		data = (TemplateResourceInputAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}