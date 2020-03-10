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
            	String templateName = resource.getLiteral("name").getString().toLowerCase();
            	templateResources.add(templateName);
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
	
	public Boolean checkResource(String resource, List<String> DBpediaResources) {
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
			
			data.outputResourceNameIndex = data.outputRowMeta.indexOfValue( "Resource" );
			data.outputResourcesOnDBpediaIndex = data.outputRowMeta.indexOfValue( "Resource is on DBpedia?" );
			
			data.resources = getResources(meta.getDBpedia(), meta.getTemplate());
			
			BufferedReader csvReader;
			try {
				csvReader = new BufferedReader(new FileReader(meta.getBrowseFilename()));
				String row;
				while ((row = csvReader.readLine()) != null) {
					data.csvResources.add(row.split(",")[0]);
					if (checkResource(row.split(",")[0].toLowerCase(), data.resources) == true) {
						data.resourcesFound.add(row.split(",")[0].toLowerCase());
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
			
			FileWriter writer;
			try {
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
				 
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("There are %s resources inside the CSV file, some of which are not in DBpedia.", data.csvResources.size()));
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			this.logBasic("Output File is being written... ");
			
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
		
		if (data.csvResources.size() > 0) {
			
			String resourceName = data.csvResources.remove(0);
			
			String isOnDBpedia = "";
			
			if (data.resourcesFound.contains(resourceName.toLowerCase())) {
				isOnDBpedia = "Yes";
			}
			else {
				isOnDBpedia = "No";
			}
			
			if ((isOnDBpedia.equals("Yes") && meta.getResource().equals("Resources on DBpedia")) || (isOnDBpedia.equals("No") && meta.getResource().equals("Resources missing in DBpedia")) || meta.getResource().equals("All")) {
				
				try {
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
			try {
				data.bufferedWriter.close();
				this.logBasic("Output File was written... ");
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