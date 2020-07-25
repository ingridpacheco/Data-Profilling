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
import org.pentaho.di.core.exception.KettleStepException;
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
		Object[] outputRow = inputRow;
		outputRow = RowDataUtil.resizeArray(outputRow, 6);
		
		if (inputRow == null) // no more input to be expected…
		{
			this.logBasic("Transformation complete");
			this.logBasic("No more rows to be processed... ");
			
			try {
				this.logBasic("Writing the information in the file");
				
				//Write analysis results in CSV and txt files
				writeCSV(outputRow);
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
			data.outputPredicatesIndex = data.outputRowMeta.indexOfValue( "Qtd. Predicates" );
			data.outputIsInInputIndex = data.outputRowMeta.indexOfValue( "Is Subject In First Input" );
			data.outputMissingPredicatesIndex = data.outputRowMeta.indexOfValue( "Missing Predicates" );
			data.outputQuantityMissingPredicatesIndex = data.outputRowMeta.indexOfValue( "Qtd. Missing Predicates" );
			data.outputCompletenessPercentageIndex = data.outputRowMeta.indexOfValue( "Completeness Percentage" );
			
			this.logBasic("Getting fields from second input file");
			
			readCSVInformation();
			
			this.logBasic("Initializing output files");
			
			initializeOutputFiles();
			
			if (meta.getIsInputCSV()) {
				
				this.logBasic("Getting fields from first input file");
				readFirstCSVInformation();
				
			}
		}
		
		if (!meta.getIsInputCSV()) {
			//Get RDF parameters
			getRdfInformation(inputRow);
		}
		
		return true;
	}
	
	private void initializeOutputFiles() {
		FileWriter CSVwriter;
		FileWriter writer;
		try {
			CSVwriter = new FileWriter(meta.getOutputCSVFile(), true);
			CSVUtils.writeLine(CSVwriter, Arrays.asList("Subject", "Missing Predicates", "Qtd. Predicates", "Qtd. Missing Predicates", "Completeness Percentage"), ',');
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
			csvReader = new BufferedReader(new FileReader(meta.getInputSecondCSV()));
			String row;
			String inputSubject;
			String inputPredicate;
			String inputValue;
			while ((row = csvReader.readLine()) != null) {
				if (data.inputSubjectsPredicate.size() == 0) {
					row = csvReader.readLine();
				}
				if (meta.getIsTriplified()) {
					String[] triples = (row.split(" "));
					inputSubject = removeSignals(triples[0]);
					inputPredicate = removeSignals(triples[1]);
					inputValue = triples[2];
					if (inputValue.matches("(.*)double(.*)") || inputValue.matches("(.*)integer(.*)")) {
						addValueToHashTable(inputSubject, inputPredicate, Float.valueOf(inputValue.split("\"")[1]));
					}
				}
				else {
					inputSubject = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[0]) : removeSignals(row.split(",")[0]);
					inputPredicate = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[1]) : removeSignals(row.split(",")[1]);
					inputValue = (row.matches("\\S+[;]\\S+")) ? (row.split(";")[2]) : (row.split(",")[2]);
					if (inputValue.matches("^-?\\d*(\\.\\d+)?$")) {
						addValueToHashTable(inputSubject, inputPredicate, Float.valueOf(inputValue));
						
					}
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
	
	private void addValueToHashTable(String subject, String predicate, Float value) {
		@SuppressWarnings("serial")
		Hashtable<String, Float> subjectValue = new Hashtable<String, Float>(){};
		if (data.inputSubjectPredicateValue.containsKey(subject)) {
			subjectValue = data.inputSubjectPredicateValue.get(subject);
			subjectValue.put(predicate, value);
		}
		else {
			subjectValue.put(predicate, value);
		}
		data.inputSubjectPredicateValue.put(subject, subjectValue);
	}
	
	private void compareValues(Float value) {
		@SuppressWarnings("serial")
		Hashtable<String, Float> subjectValue = new Hashtable<String, Float>(){};
		Float firstInputValue = data.inputSubjectPredicateValue.get(subject).get(predicate);
		Float result = (100 * value) / firstInputValue;
		if (data.inputSubjectValuePercentage.containsKey(subject)) {
			subjectValue = data.inputSubjectValuePercentage.get(subject);
			subjectValue.put(predicate, result);
		}
		else {
			subjectValue.put(predicate, result);
		}
		data.inputSubjectValuePercentage.put(subject, subjectValue);
	}
	
	private void readFirstCSVInformation() {
		BufferedReader csvReader;
		try {				
			csvReader = new BufferedReader(new FileReader(meta.getInputFirstCSV()));
			String row;
			subject = "";
			String value = "";
			while ((row = csvReader.readLine()) != null) {
				if (subject.equals("")) {
					row = csvReader.readLine();
				}
				if (meta.getInputChoice().equals("N-Triple")) {
					String[] triples = (row.split(" "));
					subject = removeSignals(triples[0]);
					predicate = removeSignals(triples[1]);
					value = triples[2];
					if ((value.matches("(.*)double(.*)") || value.matches("(.*)integer(.*)")) && data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {
						compareValues(Float.valueOf(value.split("\"")[1]));
					}
				}
				else {
					subject = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[0]) : removeSignals(row.split(",")[0]);
					predicate = (row.matches("\\S+[;]\\S+")) ? removeSignals(row.split(";")[1]) : removeSignals(row.split(",")[1]);
					value = (row.matches("\\S+[;]\\S+")) ? (row.split(";")[2]) : (row.split(",")[2]);
					if (value.matches("^-?\\d*(\\.\\d+)?$") && data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {
						compareValues(Float.valueOf(value));
					}
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
			String[] tripleParameters = getInputRowMeta().getString(inputRow, meta.getNTripleFieldName(), "").split(" ");
			
			//Add predicate in subjects list
			subject = removeSignals(tripleParameters[0]);
			predicate = removeSignals(tripleParameters[1]);
			String value = tripleParameters[2];
			if ((value.matches("(.*)double(.*)") || value.matches("(.*)integer(.*)")) && data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {						
				compareValues(Float.valueOf(value.split("\"")[1]));
			}
		}
		else {
			subject = removeSignals(getInputRowMeta().getString(inputRow, meta.getSubject(), ""));
			predicate = removeSignals(getInputRowMeta().getString(inputRow, meta.getPredicate(), ""));
			String value = getInputRowMeta().getString(inputRow, meta.getValue(), "");
			if (value.matches("^-?\\d*(\\.\\d+)?$") && data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {
				compareValues(Float.valueOf(value));
			}
		}
		
		data.predicates.add(predicate);
		
		checkSubject();
	}
	
	private void checkSubject() {
		if (data.inputSubjectsPredicate.get(subject) != null) {
			data.subjects.add(subject);
		}
	}
	
	private void writeCSV(Object[] outputRow) throws IOException, KettleStepException {
		for (String subject : data.inputSubjectsPredicate.keySet()) {
			data.bufferedWriter.newLine();
			if (!data.subjects.contains(subject)) {
				data.bufferedWriter.write(String.format("The subject %s was not in the first database.", subject));
				outputRow[data.outputIsInInputIndex] = false;
			}
			else {
				data.bufferedWriter.write(String.format("The subject %s was in the first database.", subject));
				outputRow[data.outputIsInInputIndex] = true;
			}
			HashSet<String> missingPredicates = getComplementPredicates(data.predicates, data.inputSubjectsPredicate.get(subject));
			String predicateString = createPredicateString(missingPredicates);
			HashSet<String> onlyCSVPredicates = getComplementPredicates(data.inputSubjectsPredicate.get(subject), data.predicates);
			String CSVPredicatesString = createPredicateString(onlyCSVPredicates);
			Integer subjectHas = data.predicates.size() - missingPredicates.size();
			
			//Calculates the completeness percentage of the subject
			Integer completenessPercentage = getCompletenessPercentage(subjectHas);
			CSVUtils.writeLine(data.CSVwriter, Arrays.asList(subject, predicateString, String.valueOf(subjectHas), String.valueOf(missingPredicates.size()) ,completenessPercentage.toString()), ',');
			if (missingPredicates.size() > 0) {
				data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("The subject has the following missing predicates: %s", predicateString));
			}
			if (data.inputSubjectValuePercentage.containsKey(subject)) {
				data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("The subject had some common properties with first input"));
				for (String property :  data.inputSubjectValuePercentage.get(subject).keySet()) {
					data.bufferedWriter.newLine();
					data.bufferedWriter.write(String.format("The predicate: %s", property));
					data.bufferedWriter.newLine();
					data.bufferedWriter.write(String.format("The percentage of the second inputted value in comparison to the first is of %.2f %s", data.inputSubjectValuePercentage.get(subject).get(property), "(%)"));
				}
			}
			data.bufferedWriter.newLine();
			data.bufferedWriter.write(String.format("It currently has a completeness percentage of %d because it has %d predicates out of %d", completenessPercentage, subjectHas, data.predicates.size()));
			if (onlyCSVPredicates.size() > 0) {
				data.bufferedWriter.newLine();
				data.bufferedWriter.write(String.format("It also has %d predicates that are not mapped in the database. These predicates are: %s", onlyCSVPredicates.size(), CSVPredicatesString));
			}
			
			outputRow[data.outputSubjectIndex] = subject;
			outputRow[data.outputMissingPredicatesIndex] = predicateString;
			outputRow[data.outputPredicatesIndex] = subjectHas;
			outputRow[data.outputQuantityMissingPredicatesIndex] = missingPredicates.size();
			outputRow[data.outputCompletenessPercentageIndex] = completenessPercentage;
			putRow(data.outputRowMeta, outputRow);
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