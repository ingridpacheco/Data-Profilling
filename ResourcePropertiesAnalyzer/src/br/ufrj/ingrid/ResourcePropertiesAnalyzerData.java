package br.ufrj.ingrid;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class ResourcePropertiesAnalyzerData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputExistingPropertiesIndex = -1;
	int outputMissingPropertiesIndex = -1;
	int outputNotMapedPropertiesIndex = -1;
	
	int quantity = 0;
	
	List<String> templateProperties = new ArrayList<>();
	List<String> missingProperties = new ArrayList<>();
	List<String> resourceProperties = new ArrayList<>();
	List<String> notMappedProperties = new ArrayList<>();
	List<String> allProperties = new ArrayList<>();
	
	BufferedWriter bufferedWriter;
	
	public ResourcePropertiesAnalyzerData() {
		super();
	}

}