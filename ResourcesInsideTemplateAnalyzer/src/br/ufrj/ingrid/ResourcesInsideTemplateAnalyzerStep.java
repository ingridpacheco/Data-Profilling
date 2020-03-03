/**
*
*/
package br.ufrj.ingrid;

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

public class ResourcesInsideTemplateAnalyzerStep extends BaseStep implements StepInterface {
	
	private ResourcesInsideTemplateAnalyzerData data;
	private ResourcesInsideTemplateAnalyzerMeta meta;
	
	public ResourcesInsideTemplateAnalyzerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ResourcesInsideTemplateAnalyzerMeta) smi;
		data = (ResourcesInsideTemplateAnalyzerData) sdi;
		
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
		resource = resource.replace(" ", "_");
		
		try {
			String url = String.format("http://%s.dbpedia.org/resource/%s", DBpedia, resource);
			Document doc = Jsoup.connect(url).get();
			Elements properties = doc.select(String.format("a[href^=\"http://%s.dbpedia.org/property\"]", DBpedia));
	
			for (int i = 0; i < properties.size(); i++) {
				String resourceProperty = properties.get(i).text();
				resourceProperties.add(resourceProperty.split(":")[1]);
			}
			
		  	return resourceProperties;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  	return resourceProperties;
		}
	}
	
	public String getPercentage(Integer resourceProperties, Integer notMapped, Integer templateProperties) {
		if (templateProperties == 0)
			return "0";
		
		Integer mappedProperties = resourceProperties - notMapped;
		Float percentage = (mappedProperties.floatValue()/templateProperties.floatValue()) * 100;
		
		return percentage.toString().concat("%");
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (ResourcesInsideTemplateAnalyzerMeta) smi;
		data = (ResourcesInsideTemplateAnalyzerData) sdi;
		
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
			data.outputPercentageIndex = data.outputRowMeta.indexOfValue( "Completude Percentage" );
		      
		    data.templateProperties = getProperties(meta.getDBpedia(), meta.getTemplate());
			data.templateProperties.remove(0);
			data.resources = getResources(meta.getDBpedia(), meta.getTemplate());
		}
		
		Object[] outputRow = RowDataUtil.resizeArray( inputRow, 5 );
		
		if (data.resources.size() > 0) {
			String resourceName = data.resources.get(0);
			List<String> resourceProperties = getResourceProperties(meta.getDBpedia(), data.resources.remove(0));
			List<String> missingProperties = getMissingProperties(resourceProperties, data.templateProperties);
			List<String> notMappedProperties = getNotMappedProperties(resourceProperties, data.templateProperties);
			
			outputRow[data.outputResourcesIndex] = resourceName;
			outputRow[data.outputExistingPropertiesIndex] = resourceProperties.size();
			outputRow[data.outputMissingPropertiesIndex] = missingProperties.size();
			outputRow[data.outputTotalIndex] = data.templateProperties.size();
			outputRow[data.outputPercentageIndex] = getPercentage(resourceProperties.size(), notMappedProperties.size(), data.templateProperties.size());
			
			putRow(data.outputRowMeta, outputRow);
			
			return true;
		}
		else {
			return false;
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ResourcesInsideTemplateAnalyzerMeta) smi;
		data = (ResourcesInsideTemplateAnalyzerData) sdi;
		
		super.dispose(smi, sdi);
	}

}