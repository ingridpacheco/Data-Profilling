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

public class ResourceDataCompletudeData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputExistingPropertiesIndex = -1;
	int outputMissingPropertiesIndex = -1;
	int outputNotMapedPropertiesIndex = -1;
	int outputTotalIndex = -1;
	int outputPercentageIndex = -1;
	
	List<String> templateProperties = new ArrayList<>();
	List<String> missingProperties = new ArrayList<>();
	List<String> resourceProperties = new ArrayList<>();
	
	String percentage = "";
	
	public ResourceDataCompletudeData() {
		super();
	}

}