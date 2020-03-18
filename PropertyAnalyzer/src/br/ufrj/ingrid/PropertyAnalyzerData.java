package br.ufrj.ingrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class PropertyAnalyzerData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputResourceIndex = -1;
	int outputInsideResourcesIndex = -1;
	
	List<String> resources = new ArrayList<>();
	String property = "";
	
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	
	public PropertyAnalyzerData() {
		super();
	}

}