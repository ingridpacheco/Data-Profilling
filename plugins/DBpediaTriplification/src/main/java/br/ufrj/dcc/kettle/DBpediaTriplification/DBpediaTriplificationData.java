package br.ufrj.dcc.kettle.DBpediaTriplification;

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

public class DBpediaTriplificationData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputNTriplesIndex = -1;
	
	List<String> triples = new ArrayList<>();
	
	FileWriter CSVwriter;
	
	public DBpediaTriplificationData() {
		super();
	}

}