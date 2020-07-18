/**
*
*/
package br.ufrj.dcc.kettle.InnerProfiling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;


import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
* @author IngridPacheco
*
*/

public class InnerProfilingStep extends BaseStep implements StepInterface {
	
	private InnerProfilingData data;
	private InnerProfilingMeta meta;
	private String subject;
	private String predicate;
	
	public InnerProfilingStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (InnerProfilingMeta) smi;
		data = (InnerProfilingData) sdi;
		
		if ( !super.init( meta, data ) ) {
		      return false;
		}
		
		return true;
	}
	
	private void readCSVInformation() {
		BufferedReader csvReader;
		try {				
			csvReader = new BufferedReader(new FileReader(meta.getInputCSVBrowse()));
			String row;
			subject = "";
			while ((row = csvReader.readLine()) != null) {
				if (subject.equals("")) {
					row = csvReader.readLine();
				}
				if (meta.getInputChoice().equals("N-Triple")) {
					String[] triples = (row.split(" "));
					subject = removeSignals(triples[0]);
					predicate = removeSignals(triples[1]);
				}
				else {
					subject = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[0]) : removeSignals(row.split(",")[0]);
					predicate = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[1]) : removeSignals(row.split(",")[1]);
				}
				
				getRDFpredicates();
			}
			csvReader.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (InnerProfilingMeta) smi;
		data = (InnerProfilingData) sdi;
		
		Object[] inputRow = getRow(); // get row, blocks when needed!
		
		if (inputRow == null) // no more input to be expected…
		{
			this.logBasic("Transformation complete");
			this.logBasic("No more rows to be processed... ");
			
			try {
				this.logBasic("Writing the information in the file");
				
				//Write analysis results in CSV and txt files
				writeCSV();
				data.CSVwriter.flush();
		        data.CSVwriter.close();
				
            	data.bufferedWriter.close();
				this.logBasic("Output Files were written... ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			setOutputDone();
			return false;
		}
		
		if (first) {
			first = false;
			data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
			
			data.outputSubjectIndex = data.outputRowMeta.indexOfValue( "Subject" );
			data.outputPredicatesIndex = data.outputRowMeta.indexOfValue( "Predicates" );
			
			//Initialize writing in CSV and txt files
			FileWriter CSVwriter;
			FileWriter writer;
			try {
				CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
				CSVUtils.writeLine(CSVwriter, Arrays.asList("Subject", "Missing Predicates", "Completeness Percentage"), ',');
				data.CSVwriter = CSVwriter;
				
				writer = new FileWriter(meta.getOutputFile(), true);
				data.bufferedWriter = new BufferedWriter(writer);
				 
	            data.bufferedWriter.write("The result of the analysis was:");
	            data.bufferedWriter.newLine();
	            data.bufferedWriter.write("There may exist some subjects that could become more complete with predicates that belongs to another subjects. Here is the list of the missing predicates for each subject.");
	            
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			if (meta.getIsInputCSV()) {
				readCSVInformation();
			}
		}
		if (!meta.getIsInputCSV()) {
			//Get RDF parameters
			getRdfInformation(inputRow);	
			
			Object[] outputRow = inputRow;
			outputRow = RowDataUtil.resizeArray(outputRow, 3);
			outputRow[data.outputSubjectIndex] = subject;
			outputRow[data.outputPredicatesIndex] = predicate;
			putRow(data.outputRowMeta, outputRow);
		}
		
		return true;
	}
	
	public void getRdfInformation(Object[] inputRow) throws KettleValueException {
		
		if (meta.getInputChoice().equals("N-Triple")) {
			String[] tripleParameters = getInputRowMeta().getString(inputRow, meta.getNTripleFieldName(), "").split(" ");;
			
			//Add predicate in subjects list
			subject = removeSignals(tripleParameters[0]);
			predicate = removeSignals(tripleParameters[1]);
		}
		else {
			subject = removeSignals(getInputRowMeta().getString(inputRow, meta.getSubject(), ""));
			predicate = removeSignals(getInputRowMeta().getString(inputRow, meta.getPredicate(), ""));
		}
		
		getRDFpredicates();
	}
		
	public void getRDFpredicates() {
		data.subjectsPredicates.put(subject, addValueList(data.subjectsPredicates, subject, predicate));
		
		//Remove predicate if it is in the missingPredicates' list of the subject
		if (data.missingPredicates.getOrDefault(subject, new HashSet<String>()).contains(predicate)) {
            data.missingPredicates.put(subject, removeValue(data.missingPredicates, subject, predicate));
        }
		
		//Check for missing predicates
		for (String missingPredicate : data.predicates) {
            if (!data.subjectsPredicates.getOrDefault(subject, new HashSet<String>()).contains(missingPredicate)) {
            	data.missingPredicates.put(subject, addValueList(data.missingPredicates, subject, missingPredicate));
            }
        }
		
		//Add predicate to predicate list
		data.predicates.add(predicate);
		
		//Add predicate to the list of missing predicates from other subjects
		for (String listSubject : data.subjectsPredicates.keySet()) {
			if (!subject.equals(listSubject) && !data.subjectsPredicates.get(listSubject).contains(predicate)) {
				data.missingPredicates.put(listSubject, addValueList(data.missingPredicates, listSubject, predicate));
			}
		}
	}
	
	private void writeCSV() throws IOException {
		for (String subject : data.missingPredicates.keySet()) {
			HashSet<String> subjectPredicates = data.missingPredicates.get(subject);
			String predicateString = createPredicateString(subjectPredicates);
			
			//Calculates the completeness percentage of the subject
			Integer completenessPercentage = getCompletenessPercentage(data.subjectsPredicates.get(subject));
			CSVUtils.writeLine(data.CSVwriter, Arrays.asList(subject, predicateString, completenessPercentage.toString()), ',');
			data.bufferedWriter.newLine();
			if (subjectPredicates.size() > 0) {
				data.bufferedWriter.write(String.format("The subject %s could become more complete with this predicates: %s.", subject, predicateString));
			}
			else {
				data.bufferedWriter.write(String.format("The subject %s is totally complete", subject));
			}
			data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("It currently has a completeness percentage of %d because it has %d predicates out of %d", completenessPercentage, data.subjectsPredicates.get(subject).size(), data.predicates.size()));
		}
	}
	
	private Integer getCompletenessPercentage(HashSet<String> subjectPredicates) {
		return (100 * subjectPredicates.size()) / data.predicates.size();
	}
	
	private String createPredicateString(HashSet<String> predicates) {
		return String.join(";", predicates); 
	}
	
	private HashSet<String> removeValue(Hashtable<String,HashSet<String>> predicates, String key, String predicate) {
		HashSet<String> subjectPredicates = predicates.get(key);
		
		subjectPredicates.remove(predicate);
		return subjectPredicates;
	}
	
	private HashSet<String> addValueList(Hashtable<String,HashSet<String>> predicates, String key, String predicate) {
		HashSet<String> subjectPredicates = predicates.getOrDefault(key, new HashSet<String>());
		subjectPredicates.add(predicate);
		return subjectPredicates;
	}
	
	/**
	 * Trata o valor passado como parametro, retirando os caracteres <, > e "
	 * 
	 * @param value
	 * @return
	 */
	private static String removeSignals(String value) {
		if (value != null) {
			return value.replaceAll("<", "").replaceAll(">", "").replaceAll("\"", "").trim();
		} else {
			return "";
		}
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		InnerProfilingMeta meta = (InnerProfilingMeta) smi;
		InnerProfilingData data = (InnerProfilingData) sdi;
		
		super.dispose(meta, data);
	}
}