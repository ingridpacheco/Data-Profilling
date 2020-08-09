package br.ufrj.dcc.kettle.MergeProfiling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CSV {
	
	public String subject;
	public String predicate;
	public String value;
	public Utils UtilsFunctions;
	public String getInputChoice;
	public Boolean isTriplified;

	public CSV(MergeProfilingMeta meta, MergeProfilingData data){
		this.UtilsFunctions = new Utils(meta, data);
		this.getInputChoice = meta.getInputChoice();
		this.isTriplified = meta.getIsTriplified();
	}
	
	public void readFirstCSVInformation(String fileName, MergeProfilingData data) {
		BufferedReader csvReader;
		try {				
			csvReader = new BufferedReader(new FileReader(fileName));
			String row;
			subject = "";
			Boolean save = true;
			while ((row = csvReader.readLine()) != null) {
				if (subject.equals("")) {
					row = csvReader.readLine();
				}
				if (this.getInputChoice.equals("N-Triple")) {
					save = getTriplifiedValue(row, data, true);
				}
				else {
					save = getFieldsValue(row, data, true);
				}
				
				if (save) {
					data.predicates.add(predicate);
					this.UtilsFunctions.checkSubject(data, subject);
				}
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
	
	public void readCSVInformation(String fileName, MergeProfilingData data) {
		BufferedReader csvReader;
		try {				
			csvReader = new BufferedReader(new FileReader(fileName));
			String row;
			Boolean save = true;
			while ((row = csvReader.readLine()) != null) {
				if (data.inputSubjectsPredicate.size() == 0) {
					row = csvReader.readLine();
				}
				if (this.isTriplified) {
					save = getTriplifiedValue(row, data, false);
				}
				else {
					save = getFieldsValue(row, data, false);
				}
				if (save) {
					data.inputSubjectsPredicate.put(subject, this.UtilsFunctions.addValueList(data.inputSubjectsPredicate, subject, predicate));
				}
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

	private Boolean getFieldsValue(String row, MergeProfilingData data, Boolean first) {
		Boolean save = true;
		String[] parameters;
		if (row.matches("(.*)[;].*$")) {
			parameters = row.split(";");
		}
		else {
			parameters = row.split(",");
		}
		subject = this.UtilsFunctions.removeSignals((parameters[0]));
		predicate = this.UtilsFunctions.removeSignals((parameters[1]));
		if (parameters.length > 2) {
			value = parameters[2];
			if (value.matches("^\\d*(.\\d+)?$")) {
				if (data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate)) {
					this.UtilsFunctions.compareValues(Float.valueOf(value), subject, predicate, data);
				}
				if (!first) {
					this.UtilsFunctions.addValueToHashTable(subject, predicate, Float.valueOf(value), data);
				}
			}
		}
		else {
			save = false;
		}
		return save;
	}
	
	private Boolean getTriplifiedValue(String row, MergeProfilingData data, Boolean first) {
		Boolean save = true;
		String[] triples = (row.split(" "));
		subject = this.UtilsFunctions.removeSignals(triples[0]);
		predicate = this.UtilsFunctions.removeSignals(triples[1]);
		if (triples.length > 2) {
			value = triples[2];
			if (value.matches("(.*)double(.*)") || value.matches("(.*)integer(.*)")) {
				if (data.inputSubjectPredicateValue.containsKey(subject) && data.inputSubjectPredicateValue.get(subject).containsKey(predicate) && first) {
					this.UtilsFunctions.compareValues(Float.valueOf(value.split("\"")[1]), subject, predicate, data);
				}
				if (!first) {
					this.UtilsFunctions.addValueToHashTable(subject, predicate, Float.valueOf(value.split("\"")[1]), data);
				}
			}
		}
		else {
			save = false;
		}
		
		return save;
	}

}