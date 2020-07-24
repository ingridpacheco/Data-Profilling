package br.ufrj.dcc.kettle.GetDBpediaData;

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

public class GetDBpediaDataData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputPropertyIndex = -1;
	int outputTemplateIndex = -1;
	int outputDBpediaVersion = -1;
	int outputResourceIndex = -1;
	
	String resourceName = "";
	
	Map<String, Integer> properties = new Hashtable<String, Integer>();
	List<String> resourceProperties = new ArrayList<>();
	List<String> dataFound = new ArrayList<>();
	
	FileWriter CSVwriter;
	
	public GetDBpediaDataData() {
		super();
	}

}