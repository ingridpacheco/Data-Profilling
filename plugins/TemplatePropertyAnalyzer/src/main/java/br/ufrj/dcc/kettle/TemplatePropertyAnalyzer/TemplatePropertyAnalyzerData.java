package br.ufrj.dcc.kettle.TemplatePropertyAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Hashtable;
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
	
	int outputPropertyIndex = -1;
	int outputInsideResourcesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	
	Map<String, Integer> properties = new Hashtable<String, Integer>();
	Map<String, Float> propertiesPercentage = new Hashtable<String, Float>();
	List<String> templateProperties = new ArrayList<>();
	
	Integer totalQuantityOfResourcesThatHasTheProperty = 0;
	Integer quantityTotal = 0;
	Integer quantityOfResources = 0;
	
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	
	Float percentage = (float) 0;
	
	public TemplatePropertyAnalyzerData() {
		super();
	}

}