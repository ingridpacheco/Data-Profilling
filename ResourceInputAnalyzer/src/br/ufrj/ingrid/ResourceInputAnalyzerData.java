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

public class ResourceInputAnalyzerData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputPropertyNameIndex = -1;
	
	List<String> csvProperties = new ArrayList<>();
	List<String> properties = new ArrayList<>();
	
	List<String> notOnCSVProperties = new ArrayList<>();
	
	FileWriter CSVwriter;
	BufferedWriter bufferedWriter;
	
	public ResourceInputAnalyzerData() {
		super();
	}

}