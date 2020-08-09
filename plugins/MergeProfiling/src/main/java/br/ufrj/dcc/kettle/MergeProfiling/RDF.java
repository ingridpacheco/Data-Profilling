package br.ufrj.dcc.kettle.MergeProfiling;

import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMetaInterface;

public class RDF {
	
	public String subject;
	public String predicate;
	public String value;
	public String inputChoice;
	public String tripleName;
	public Utils UtilsFunctions;
	public String metaSubject;
	public String metaPredicate;
	public String metaValue;

	public RDF(MergeProfilingMeta meta, MergeProfilingData data){
		this.UtilsFunctions = new Utils(meta, data);
		this.inputChoice = meta.getInputChoice();
		this.tripleName = meta.getNTripleFieldName();
		this.metaSubject = meta.getSubject();
		this.metaPredicate = meta.getPredicate();
		this.metaValue = meta.getValue();
	}

	
	public void getRdfInformation(Object[] inputRow, RowMetaInterface inputRowMeta, MergeProfilingData data) throws KettleValueException {
		Boolean save = true;
		String value;
		if (this.inputChoice.equals("N-Triple")) {
			String[] tripleParameters = inputRowMeta.getString(inputRow, this.tripleName, "").split(" ");
			
			//Add predicate in subjects list
			subject = this.UtilsFunctions.removeSignals((tripleParameters[0]));
			predicate = this.UtilsFunctions.removeSignals((tripleParameters[1]));
			if (tripleParameters.length > 2) {
				value = tripleParameters[2];
				if ((value.matches("(.*)double(.*)") || value.matches("(.*)integer(.*)")) && data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {						
					this.UtilsFunctions.compareValues(Float.valueOf(value.split("\"")[1]), subject, predicate, data);
				}
			}
			else {
				save = false;
			}
		}
		else {
			subject = this.UtilsFunctions.removeSignals((inputRowMeta.getString(inputRow, this.metaSubject, "")));
			predicate = this.UtilsFunctions.removeSignals((inputRowMeta.getString(inputRow, this.metaPredicate, "")));
			value = inputRowMeta.getString(inputRow, this.metaValue, "");
			
			if (value != null) {
				if (value.matches("^\\d*(.\\d+)?$") && data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {
					this.UtilsFunctions.compareValues(Float.valueOf(value), subject, predicate, data);
				}
			}
			else {
				save = false;
			}
		}
		
		if (save) {
			data.predicates.add(predicate);
			this.UtilsFunctions.checkSubject(data, subject);
		}
	}

}