package br.ufrj.dcc.kettle.GetDBpediaData;

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

public class GetDBpediaDataData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputPropertyIndex = -1;
	int outputTemplateIndex = -1;
	int outputDBpediaVersion = -1;
	int outputResourceIndex = -1;
	int outputValueIndex = -1;
	int outputTypeIndex = -1;
	
	List<String> dataFound = new ArrayList<>();
	String resourceName = "";
	Map<String, Integer> properties = new Hashtable<String, Integer>();
	List<String> resourceProperties = new ArrayList<>();
	List<String> propertyValues = new ArrayList<>();
	List<String> propertyTypes = new ArrayList<>();
	Map<String, List<String>> propertyParameters = new Hashtable<String, List<String>>();
	Map<String, Map<String, List<String>>> cacheResourceProperties = new Hashtable<String, Map<String, List<String>>>();
	
	FileWriter CSVwriter;
	FileWriter CSVOutput;
	BufferedWriter bufferedWriter;
	Boolean isCached = false;
	
	public GetDBpediaDataData() {
		super();
	}

}