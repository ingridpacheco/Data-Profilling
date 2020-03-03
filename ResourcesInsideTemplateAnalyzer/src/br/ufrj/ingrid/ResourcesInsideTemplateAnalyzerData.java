package br.ufrj.ingrid;

import java.util.ArrayList;
import java.util.List;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class ResourcesInsideTemplateAnalyzerData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputResourcesIndex = -1;
	int outputExistingPropertiesIndex = -1;
	int outputMissingPropertiesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	
	List<String> resources = new ArrayList<>();
	List<String> templateProperties = new ArrayList<>();
	
	public ResourcesInsideTemplateAnalyzerData() {
		super();
	}

}