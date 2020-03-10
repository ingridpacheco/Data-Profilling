package br.ufrj.ingrid;

import java.io.BufferedWriter;
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

public class TemplateDataCompletudeData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputPropertyIndex = -1;
	int outputInsideResourcesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	
	Map<String, Integer> properties = new Hashtable<String, Integer>();
	List<String> templateProperties = new ArrayList<>();
	
	Integer totalQuantityOfResourcesThatHasTheProperty = 0;
	Integer quantityTotal = 0;
	Integer quantityOfResources = 0;
	
	BufferedWriter bufferedWriter;
	
	String percentage = "";
	
	public TemplateDataCompletudeData() {
		super();
	}

}