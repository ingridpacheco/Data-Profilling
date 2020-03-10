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

public class TemplateResourceAnalyzerData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputResourceNameIndex = -1;
	int outputResourcesOnDBpediaIndex = -1;
	
	List<String> csvResources = new ArrayList<>();
	List<String> resources = new ArrayList<>();
	
	List<String> resourcesFound = new ArrayList<>();
	
	BufferedWriter bufferedWriter;
	
	public TemplateResourceAnalyzerData() {
		super();
	}

}