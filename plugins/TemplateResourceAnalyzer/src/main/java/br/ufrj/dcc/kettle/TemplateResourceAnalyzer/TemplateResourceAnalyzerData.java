package br.ufrj.dcc.kettle.TemplateResourceAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class TemplateResourceAnalyzerData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputDBpediaIndex = -1;
	int outputResourcesIndex = -1;
	int outputExistingPropertiesIndex = -1;
	int outputMissingPropertiesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	
	String resource;
	
	List<String> resources = new ArrayList<>();
	List<String> templateProperties = new ArrayList<>();
	
	Map<String,Float> resourcesCompletenessPercentage = new HashMap<>();
	Map<String,Integer> resourcesExistingProperties = new HashMap<>();
	Map<String,Integer> resourcesNotMappedProperties = new HashMap<>();
	Map<String,Integer> resourcesMissingProperties = new HashMap<>();
	List<String> totalExistingProperties = new ArrayList<String>();
	
	String DBpediaVersion = "";
	String template = "";
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	Integer quantity = 0;
	
	Integer totalResourcesExistingProperties = 0;
	Integer totalResourcesNotMappedProperties = 0;
	Integer totalResourcesMissingProperties = 0;
	
	public TemplateResourceAnalyzerData() {
		super();
	}

}