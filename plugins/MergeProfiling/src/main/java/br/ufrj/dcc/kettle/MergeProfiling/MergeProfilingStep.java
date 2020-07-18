/**
*
*/
package br.ufrj.dcc.kettle.MergeProfiling;

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

public class MergeProfilingStep extends BaseStep implements StepInterface {
	
	private MergeProfilingData data;
	private MergeProfilingMeta meta;
	private String subject;
	private String predicate;
	
	public MergeProfilingStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (MergeProfilingMeta) smi;
		data = (MergeProfilingData) sdi;
		
		if ( !super.init( meta, data ) ) {
		      return false;
		}
		
		return true;
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (MergeProfilingMeta) smi;
		data = (MergeProfilingData) sdi;
		
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
			data.outputIsInCSVIndex = data.outputRowMeta.indexOfValue( "IsSubjectInCSV" );
			
			readCSVInformation();
			
			//Initialize writing in CSV and TXT files
			initializeOutputFiles();
			
			if (meta.getIsInputCSV()) {
				readFirstCSVInformation();
			}
		}
		
		if (!meta.getIsInputCSV()) {
			//Get RDF parameters
			getRdfInformation(inputRow);	
			
			Object[] outputRow = inputRow;
			outputRow[data.outputSubjectIndex] = subject;
			outputRow[data.outputPredicatesIndex] = predicate;
			String IsInCSV = (data.inputSubjectsPredicate.get(subject) == null) ? "false" : "true";
			outputRow[data.outputIsInCSVIndex] = IsInCSV;
			putRow(data.outputRowMeta, outputRow);
		}
		
		return true;
	}
	
	private void initializeOutputFiles() {
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
            data.bufferedWriter.write("An evaluation was done in the CSV subjects and there may have some resources that could be more complete with other predicates.");
            
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	private void readCSVInformation() {
		BufferedReader csvReader;
		try {				
			csvReader = new BufferedReader(new FileReader(meta.getInputCSVFile()));
			String row;
			String inputSubject;
			String inputPredicate;
			while ((row = csvReader.readLine()) != null) {
				if (data.inputSubjectsPredicate.size() == 0) {
					row = csvReader.readLine();
				}
				if (meta.getIsTriplified()) {
					String[] triples = (row.split(" "));
					inputSubject = removeSignals(triples[0]);
					inputPredicate = removeSignals(triples[1]);
				}
				else {
					inputSubject = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[0]) : removeSignals(row.split(",")[0]);
					inputPredicate = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[1]) : removeSignals(row.split(",")[1]);
				}
				data.inputSubjectsPredicate.put(inputSubject, addValueList(data.inputSubjectsPredicate, inputSubject, inputPredicate));
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
	
	private void readFirstCSVInformation() {
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
				
				data.predicates.add(predicate);
				
				checkSubject();
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
		
		data.predicates.add(predicate);
		
		checkSubject();
	}
	
	private void checkSubject() {
		if (data.inputSubjectsPredicate.get(subject) != null) {
			data.subjects.add(subject);
		}
	}
	
	private void writeCSV() throws IOException {
		for (String subject : data.inputSubjectsPredicate.keySet()) {
			data.bufferedWriter.newLine();
			if (!data.subjects.contains(subject)) {
				data.bufferedWriter.write(String.format("The subject %s was not in the first database.", subject));
			}
			else {
				data.bufferedWriter.write(String.format("The subject %s was in the first database.", subject));
			}
			HashSet<String> missingPredicates = getComplementPredicates(data.predicates, data.inputSubjectsPredicate.get(subject));
			String predicateString = createPredicateString(missingPredicates);
			HashSet<String> onlyCSVPredicates = getComplementPredicates(data.inputSubjectsPredicate.get(subject), data.predicates);
			String CSVPredicatesString = createPredicateString(onlyCSVPredicates);
			Integer subjectHas = data.predicates.size() - missingPredicates.size();
			
			//Calculates the completeness percentage of the subject
			Integer completenessPercentage = getCompletenessPercentage(subjectHas);
			CSVUtils.writeLine(data.CSVwriter, Arrays.asList(subject, predicateString, completenessPercentage.toString()), ',');
			if (missingPredicates.size() > 0) {
				data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("The subject has the following missing predicates: %s", predicateString));
			}
			data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("It currently has a completeness percentage of %d because it has %d predicates out of %d", completenessPercentage, subjectHas, data.predicates.size()));
			data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("It also has %d predicates that are not mapped in the database. These predicates are: %s", onlyCSVPredicates.size(), CSVPredicatesString));
		}
	}
	
	private HashSet<String> getComplementPredicates(HashSet<String> firstInput, HashSet<String> secondInput) {
		HashSet<String> rdfPredicates = new HashSet<String>(firstInput);
		rdfPredicates.removeAll(secondInput);
		return rdfPredicates;
	}
	
	private Integer getCompletenessPercentage(Integer subjectHas) {
		return (data.predicates.size() > 0) ? ((100 * subjectHas) / data.predicates.size()) : 0;
	}
	
	private String createPredicateString(HashSet<String> predicates) {
		return String.join(";", predicates); 
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
		MergeProfilingMeta meta = (MergeProfilingMeta) smi;
		MergeProfilingData data = (MergeProfilingData) sdi;
		
		super.dispose(meta, data);
	}
}