package br.ufrj.ingrid;

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
	
	int outputResourcesIndex = -1;
	int outputExistingPropertiesIndex = -1;
	int outputMissingPropertiesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	
	List<String> resources = new ArrayList<>();
	List<String> templateProperties = new ArrayList<>();
	
	Map<String,Float> resourcesCompletenessPercentage = new HashMap<>();
	Map<String,Integer> resourcesExistingProperties = new HashMap<>();
	Map<String,Integer> resourcesNotMappedProperties = new HashMap<>();
	Map<String,Integer> resourcesMissingProperties = new HashMap<>();
	
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	Float percentage = (float) 0;
	Integer quantity = 0;
	
	Integer totalResourcesExistingProperties = 0;
	Integer totalResourcesNotMappedProperties = 0;
	Integer totalResourcesMissingProperties = 0;
	
	public TemplateResourceAnalyzerData() {
		super();
	}

}