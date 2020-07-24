/**
*
*/
package br.ufrj.dcc.kettle.PropertyAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class PropertyAnalyzerStep extends BaseStep implements StepInterface {
	
	private PropertyAnalyzerData data;
	private PropertyAnalyzerMeta meta;
	
	public PropertyAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (PropertyAnalyzerMeta) smi;
		data = (PropertyAnalyzerData) sdi;
		
		if ( !super.init( meta, data ) ) {
		      return false;
		}
		
		return true;
	}
	
	public List<String> getResourceNames(Elements resources, List<String> notMappedResources){
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text();
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!notMappedResources.contains(resourceName)) {
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
	
	public List<String> getResources() {
		Map<String, String> mapTemplateUrl = new HashMap<String, String>();
		mapTemplateUrl.put("pt", "Predefinição");
		mapTemplateUrl.put("fr", "Modèle");
		mapTemplateUrl.put("ja", "Template");
		
		return getSparqlResources(mapTemplateUrl.get(meta.getDBpedia()));
	}
		
	private List<String> getSparqlResources(String templateDefinition) {
		List<String> templateResources = new ArrayList<String>();
		String DBpedia = meta.getDBpedia();
		String template = meta.getTemplate().replace(" ", "_");
		String templateUrl = String.format("http://%s.dbpedia.org/resource/%s:%s", DBpedia, templateDefinition, template);
		
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
            	this.logBasic(String.format("Resource: %s", templateName));
            	templateResources.add(templateName);
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return templateResources;
	}
	
	public String checkPropertyInResource(String resource) {
		String DBpedia = meta.getDBpedia();
		resource = resource.replace(" ", "_");
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property/%s\"]", DBpedia, meta.getProperty()));
	
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
	
	private void initializeOutputFiled() {
		FileWriter CSVwriter;
		FileWriter writer;
		try {
			CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
			CSVUtils.writeLine(CSVwriter, Arrays.asList("Resource", "Has the property?"), ',');
			data.CSVwriter = CSVwriter;
			
			writer = new FileWriter(meta.getOutputFile(), true);
			data.bufferedWriter = new BufferedWriter(writer);
			 
            data.bufferedWriter.write("The result of the analysis was:");
            data.bufferedWriter.newLine();
            data.bufferedWriter.write(String.format("There are %s resources in %s. The property %s is not in some of them.", data.resources.size(), meta.getTemplate(), meta.getProperty()));
            
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (PropertyAnalyzerMeta) smi;
		data = (PropertyAnalyzerData) sdi;
		
		Object[] inputRow = getRow(); // get row, blocks when needed!
		
		if (inputRow == null) // no more input to be expected…
		{
			this.logBasic("No more rows to be processed... ");
			setOutputDone();
			return false;
		}
		
	
		if (first) {
			first = false;
			data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
			
			data.outputResourceIndex = data.outputRowMeta.indexOfValue( "Resource" );
			data.outputInsideResourcesIndex = data.outputRowMeta.indexOfValue( "Has the property?" );
		    
		    if (meta.getDBpedia().equals("pt") || meta.getDBpedia().equals("fr") || meta.getDBpedia().equals("ja")) {
			    this.logBasic("Getting the resources");
				data.resources = getResources();
		    }
		    
		    if (meta.getNotMappedResources() == true) {
				data.resources = getNotMappedResources();
			}
			
			this.logBasic("Output Files are being written... ");
			initializeOutputFiled();
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
			    
		if (data.resources.size() > 0) {
			
			String resourceName = data.resources.remove(0);
			
			this.logBasic(String.format("Writting the information from the resource: %s", resourceName));
			
			String hasProperty = checkPropertyInResource(resourceName);
			
			if ((hasProperty.equals("Yes") && meta.getResource().equals("Has property")) || (hasProperty.equals("No") && meta.getResource().equals("Doesn't have property")) || meta.getResource().equals("All")) {
				
				try {
					CSVUtils.writeLine(data.CSVwriter, Arrays.asList(resourceName, hasProperty), ',');
					
	            	data.bufferedWriter.newLine();
					data.bufferedWriter.write(String.format("The resource %s has the property? %s.", resourceName, hasProperty));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				outputRow[data.outputResourceIndex] = resourceName;
				outputRow[data.outputInsideResourcesIndex] = hasProperty;
					
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
			
			setOutputDone();
			return false;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		PropertyAnalyzerMeta meta = (PropertyAnalyzerMeta) smi;
		PropertyAnalyzerData data = (PropertyAnalyzerData) sdi;
		
		super.dispose(meta, data);
	}

}