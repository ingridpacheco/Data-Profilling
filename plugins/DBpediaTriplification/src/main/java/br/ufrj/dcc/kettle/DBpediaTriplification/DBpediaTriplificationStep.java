package br.ufrj.dcc.kettle.DBpediaTriplification;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

public class DBpediaTriplificationStep extends BaseStep implements StepInterface {
	
	private DBpediaTriplificationData data;
	private DBpediaTriplificationMeta meta;
	private String subject;
	private HashSet<String> DBpediaresources = new HashSet<String>();
	
	public DBpediaTriplificationStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (DBpediaTriplificationMeta) smi;
		data = (DBpediaTriplificationData) sdi;
		
		return super.init(smi, sdi);
	}
	
	public void getDBpediaNotMappedResources() {
		this.logBasic("Getting not mapped resources");
		HashSet<String> notMappedResources = DBpediaresources;
		String DBpedia = meta.getDBpedia();
		String template = meta.getTemplate();
		
		try {
			String url = String.format("https://tools.wmflabs.org/templatecount/index.php?lang=%s&namespace=10&name=%s#bottom", DBpedia, template);
			Document doc = Jsoup.connect(url).get();
			Integer quantity = Integer.parseInt(doc.select("form + h3 + p").text().split(" ")[0]);
			
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
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public HashSet<String> getResourceNames(Elements resources, HashSet<String> notMappedResources){
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text();
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!notMappedResources.contains(resourceName)) {
            		subject = resourceName;
            		DBpediaresources.add(subject);
            		getResourceProperties();
            		notMappedResources.add(resourceName);
            	}
			}
			else {
				break;
			}
		}
		
		return notMappedResources;
	}
	
	public void getDBpediaTriples() {
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
            	subject = resource.getLiteral("name").getString();
            	this.logBasic(String.format("Resource: %s", subject));
            	DBpediaresources.add(subject);
            	getResourceProperties();
            	
            	TimeUnit.SECONDS.sleep(1);
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private String getMapValues(String DBpedia) {
		Map<String, String> mapValueExp = new HashMap<String, String>();
		mapValueExp.put("pt", "label[class=\"c1\"]:has(a[href^=\"http://pt.dbpedia.org/property\"]) + div[class^=\"c2 value\"]");
		mapValueExp.put("ja", "span[prefix=\"prop-ja: \"], a[prefix=\"prop-ja: \"]");
		
		return mapValueExp.get(DBpedia);
	}
	
	private void getResourceProperties() {
		String DBpedia = meta.getDBpedia();
		String resource = subject.replace(" ", "_");
		String regexpValue = getMapValues(DBpedia);
		
		Set<String> resources = new HashSet<String>();
		Integer counter = 0;
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
			Elements values = doc.select(regexpValue);
	
			
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty;
				if (DBpedia.equals("ja") && values.get(i).toString().matches("^(a).*$")) {
					String actualProperty = values.get(i).attributes().get("rel").split("prop-ja:")[1];
					if (resources.contains(actualProperty)) {
						resourceProperty = actualProperty;
					}
					else {
						resourceProperty = properties.get(counter).text().split(":")[1];
						resources.add(resourceProperty);
						counter += 1;
					}
				}
				else {
					resourceProperty = properties.get(counter).text().split(":")[1];
					resources.add(resourceProperty);
					counter += 1;
				}
				
				String propertyUrl = String.format("http://%s.dbpedia.org/property/%s", DBpedia, resourceProperty);
				String propertyValue = values.get(i).text();
				
				// Get resource triple
				getTriple(url, propertyUrl, propertyValue);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getTriple(String resourceUrl, String propertyUrl, String value) {
		if (meta.getDBpedia() == "pt") {
			if (value.matches("^(dbr\\W).*$") && value.contains(" ")) {
				String[] objects = value.split(" ");
				for (String obj : objects) {
					data.triples.add((String.format("<%s> <%s> %s", resourceUrl, propertyUrl, formatValue(obj))));
				}
			}
			else {
				data.triples.add((String.format("<%s> <%s> %s", resourceUrl, propertyUrl, formatValue(value))));
			}
		}
		else {
			data.triples.add((String.format("<%s> <%s> %s", resourceUrl, propertyUrl, formatJapaneseValue(value))));
		}
	}
	
	private String formatJapaneseValue(String value) {
		if (value.matches("^-?\\d*(\\.\\d+)?$")) {
			String type = value.matches("^-?\\d*$") ? "integer" : "float"; 
			return String.format("\"%s\"^^<http://www.w3.org/2001/XMLSchema#%s> .", value, type);
		}
		if (value.matches("^(dbpedia-ja\\W).*$") || value.matches("^(template-ja\\W).*$")) {
			String expression = value.matches("^(dbpedia-ja\\W).*$") ? "resource/" : "resource/Template:";
			String newValue = value.matches("^(dbpedia-ja\\W).*$") ? value.split("dbpedia-ja:")[1] : value.split("template-ja:")[1];
			String resourceUrl = String.format("http://%s.dbpedia.org/%s%s", meta.getDBpedia(), expression, newValue);
			return String.format("<%s> .", resourceUrl);
		}
		return String.format("\"%s\"@%s .", value, meta.getDBpedia());
	}
	
	private String formatValue(String value) {
		if (value.matches("^(xsd\\W).*$")) {
			String[] literalValue = value.split("xsd\\W")[1].split(" ");
			return String.format("\"%s\"^^<http://www.w3.org/2001/XMLSchema#%s> .", literalValue[1], literalValue[0]);
		}
		if (Pattern.matches("^(dbr\\W).*$", value) || value.matches("(.*)resource(.*)")) {
			String expression = Pattern.matches("^(dbr\\W).*$", value) ? "dbr\\W" : "resource/";
			String resourceUrl = String.format("http://%s.dbpedia.org/resource/%s", meta.getDBpedia(), value.split(expression)[1]);
			return String.format("<%s> .", resourceUrl);
		}
		return String.format("\"%s\"@%s .", value, meta.getDBpedia());
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (DBpediaTriplificationMeta) smi;
		data = (DBpediaTriplificationData) sdi;
		
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
			
			data.outputNTriplesIndex = data.outputRowMeta.indexOfValue( "N-Triples" );
			
			this.logBasic("Output Files are being created... ");
			
			FileWriter CSVwriter;
			try {
				CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
				CSVUtils.writeLine(CSVwriter, Arrays.asList("N-Triples"), ',');
				data.CSVwriter = CSVwriter;
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			this.logBasic("Getting the tripples");
			
			if (meta.getSpecifyResource()) {
				subject = meta.getResource();
				getResourceProperties();
			}
			else {
				getDBpediaTriples();
			    
			    if (meta.getNotMappedResources() == true) {
			    	getDBpediaNotMappedResources();
				}
			}
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 1 );
		
		this.logBasic(String.format("Writting the information of the triples"));
		
		if (data.triples.size() > 0) {
			String triple = data.triples.remove(0);
			this.logBasic(String.format("Writting the triple: %s", triple));
			
			try {
            	CSVUtils.writeLine(data.CSVwriter, Arrays.asList(triple), ',');        	
  			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			outputRow[data.outputNTriplesIndex] = triple;
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

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (DBpediaTriplificationMeta) smi;
		data = (DBpediaTriplificationData) sdi;
		
		super.dispose(smi, sdi);
	}

}