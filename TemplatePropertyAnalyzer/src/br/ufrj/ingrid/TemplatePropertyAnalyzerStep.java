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
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (TemplatePropertyAnalyzerMeta) smi;
		data = (TemplatePropertyAnalyzerData) sdi;
		
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
			
			data.outputResourceIndex = data.outputRowMeta.indexOfValue( "Resource" );
			data.outputInsideResourcesIndex = data.outputRowMeta.indexOfValue( "Has the property?" );
		      
		    data.property = meta.getProperty();
			data.resources = getResources(meta.getDBpedia(), meta.getTemplate());
			
			FileWriter writer;
			try {
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
				 
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write(String.format("There are %s resources in %s. The property %s is not in some of them.", data.resources.size(), meta.getTemplate(), meta.getProperty()));
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			this.logBasic("Output File is being written... ");
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 4 );
			    
		if (data.resources.size() > 0) {
			
			String resourceName = data.resources.remove(0);
			String hasProperty = checkPropertyInResource(meta.getDBpedia(), resourceName, meta.getProperty());
			
			if ((hasProperty.equals("Yes") && meta.getResource().equals("Has property")) || (hasProperty.equals("No") && meta.getResource().equals("Doesn't have property")) || meta.getResource().equals("All")) {
				
				try {
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
		meta = (TemplatePropertyAnalyzerMeta) smi;
		data = (TemplatePropertyAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}