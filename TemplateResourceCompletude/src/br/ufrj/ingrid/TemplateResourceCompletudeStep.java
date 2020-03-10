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

public class TemplateResourceCompletudeStep extends BaseStep implements StepInterface {
	
	private TemplateResourceCompletudeData data;
	private TemplateResourceCompletudeMeta meta;
	
	public TemplateResourceCompletudeStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateResourceCompletudeMeta) smi;
		data = (TemplateResourceCompletudeData) sdi;
		
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
		meta = (TemplateResourceCompletudeMeta) smi;
		data = (TemplateResourceCompletudeData) sdi;
		
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
			
			data.outputResourceInCSVIndex = data.outputRowMeta.indexOfValue( "Quantity of Resources in CSV" );
			data.outputResourcesOnDBpediaIndex = data.outputRowMeta.indexOfValue( "Resources found on DBpedia" );
			data.outputResourcesMissingIndex = data.outputRowMeta.indexOfValue( "Resources missing on DBpedia" );
			data.outputCompletudePercentageIndex = data.outputRowMeta.indexOfValue( "Completude Percentage" );
			
			data.resources = getResources(meta.getDBpedia(), meta.getTemplate());
			
			BufferedReader csvReader;
			try {
				csvReader = new BufferedReader(new FileReader(meta.getBrowseFilename()));
				String row;
				while ((row = csvReader.readLine()) != null) {
					data.csvResources.add(row.split(",")[0]);
					if (checkResource(row.split(",")[0].toLowerCase(), data.resources) == true) {
						data.resourcesFound.add(row.split(",")[0]);
					}
					else {
						data.missingResources.add(row.split(",")[0]);
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
			
			data.percentage = checkPercentage(data.resourcesFound.size(), data.csvResources.size());
			
			FileWriter writer;
			try {
				writer = new FileWriter(meta.getOutputFile(), true);
				BufferedWriter bufferedWriter = new BufferedWriter(writer);
				 
	            bufferedWriter.write("The result of the analysis was:");
	            bufferedWriter.newLine();
	            bufferedWriter.write(String.format("There are %s resources in the CSV input, in which %s were found on DBpedia.", data.csvResources.size(), data.resourcesFound.size()));
	            bufferedWriter.newLine();
	            bufferedWriter.write(String.format("Also, %s resources are missing on DBpedia, which leds to a completude percentage of %s.", data.missingResources.size(), data.percentage));
	 
	            bufferedWriter.close();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			this.logBasic("Output File was written... ");
			
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
		
		outputRow[data.outputResourceInCSVIndex] = data.csvResources.size();
		outputRow[data.outputResourcesOnDBpediaIndex] = data.resourcesFound.size();
		outputRow[data.outputResourcesMissingIndex] = data.missingResources.size();
		outputRow[data.outputCompletudePercentageIndex] = data.percentage;
					
		putRow(data.outputRowMeta, outputRow);
		
		return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (TemplateResourceCompletudeMeta) smi;
		data = (TemplateResourceCompletudeData) sdi;
		
		super.dispose(smi, sdi);
	}

}