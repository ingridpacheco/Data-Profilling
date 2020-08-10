package br.ufrj.dcc.kettle.TemplatePropertyAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class TemplatePropertyAnalyzerData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputDBpediaIndex = -1;
	int outputPropertyIndex = -1;
	int outputInsideResourcesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	int outputMissingPropertiesIndex = -1;
	int quantity = 0;
	
	String property;
	
	HashSet<String> resources = new HashSet<String>();
	List<String> properties = new ArrayList<String>();
	List<String> templateResources = new ArrayList<>();
	
	Map<String,Float> propertiesCompletenessPercentage = new HashMap<>();
	Map<String,Integer> propertiesExistingResources = new HashMap<>();
	Map<String,Integer> propertiesMissingResources = new HashMap<>();
	List<String> totalExistingResources = new ArrayList<String>();
	
	Integer totalQuantityOfResourcesThatHasTheProperty = 0;
	Integer totalOfResources = 0;
	
	Integer totalPropertiesExistingProperties = 0;
	Integer totalPropertiesMissingProperties = 0;
	
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	
	public TemplatePropertyAnalyzerData() {
		super();
	}

}