/**
*
*/
package br.ufrj.dcc.kettle.MergeProfiling;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
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
	private CSV CSVfile;
	private RDF RDFfile;
	
	public MergeProfilingStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (MergeProfilingMeta) smi;
		data = (MergeProfilingData) sdi;
		CSVfile = new CSV(meta, data);
		RDFfile = new RDF(meta, data);
		
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
				Object[] outputRow = RowDataUtil.resizeArray(inputRow, 6);
				//Write analysis results in CSV and txt files
				writeOutput(outputRow);
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
			
			this.CSVfile.readCSVInformation(meta.getInputSecondCSV(), data);
			
			this.logBasic("Initializing output files");
			
			initializeOutputFiles();
			
			if (meta.getIsInputCSV()) {
				
				this.logBasic("Getting fields from first input file");
				this.CSVfile.readFirstCSVInformation(meta.getInputFirstCSV(), data);
				
			}
		}
		
		if (!meta.getIsInputCSV()) {
			//Get RDF parameters
			RowMetaInterface inputRowMeta = (RowMetaInterface) getInputRowMeta();
			this.RDFfile.getRdfInformation(inputRow, inputRowMeta, data);
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
	
	private void writeOutput(Object[] outputRow) throws IOException, KettleStepException {
		for (String subject : data.inputSubjectsPredicate.keySet()) {
			this.logBasic(subject);
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

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		MergeProfilingMeta meta = (MergeProfilingMeta) smi;
		MergeProfilingData data = (MergeProfilingData) sdi;
		
		super.dispose(meta, data);
	}
}