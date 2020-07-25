package br.ufrj.dcc.kettle.MergeProfiling;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Hashtable;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
* @author IngridPacheco
*
*/

public class MergeProfilingData extends BaseStepData implements StepDataInterface {

public RowMetaInterface outputRowMeta;
	
	int outputSubjectIndex = -1;
	int outputPredicatesIndex = -1;
	int outputIsInInputIndex = -1;
	int outputMissingPredicatesIndex = -1;
	int outputQuantityMissingPredicatesIndex = -1;
	int outputCompletenessPercentageIndex = -1;
	
	HashSet<String> predicates = new HashSet<>();
	HashSet<String> subjects = new HashSet<>();
	Hashtable<String, HashSet<String>> inputSubjectsPredicate = new Hashtable<String,HashSet<String>>();
	Hashtable<String, Hashtable<String, Float>> inputSubjectPredicateValue = new Hashtable<String, Hashtable<String, Float>>();
	Hashtable<String, Hashtable<String, Float>> inputSubjectValuePercentage = new Hashtable<String, Hashtable<String, Float>>();
	
	
	BufferedWriter bufferedWriter;
	FileWriter CSVwriter;
	
	public MergeProfilingData() {
		super();
	}

}