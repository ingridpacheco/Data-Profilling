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

public class TemplateResourceCompletudeData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputResourceInCSVIndex = -1;
	int outputResourcesOnDBpediaIndex = -1;
	int outputCompletudePercentageIndex = -1;
	int outputResourcesMissingIndex = -1;
	
	List<String> csvResources = new ArrayList<>();
	List<String> resources = new ArrayList<>();
	String percentage = "";
	
	List<String> missingResources = new ArrayList<>();
	List<String> resourcesFound = new ArrayList<>();
	
	public TemplateResourceCompletudeData() {
		super();
	}

}