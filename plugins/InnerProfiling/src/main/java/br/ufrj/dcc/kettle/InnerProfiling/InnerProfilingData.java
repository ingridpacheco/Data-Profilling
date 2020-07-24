package br.ufrj.dcc.kettle.InnerProfiling;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Hashtable;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class InnerProfilingData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	int outputSubjectIndex = -1;
	int outputPredicatesIndex = -1;
	
	Set<String> predicates = new HashSet<>();
	Hashtable<String, HashSet<String>> subjectsPredicates = new Hashtable<String,HashSet<String>>();
	Hashtable<String, HashSet<String>> missingPredicates = new Hashtable<String,HashSet<String>>();
	
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	
	public InnerProfilingData() {
		super();
	}

}