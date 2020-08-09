package br.ufrj.dcc.kettle.MergeProfiling;

import java.util.HashSet;
import java.util.Hashtable;

public class Utils {
	
	public Utils(MergeProfilingMeta meta, MergeProfilingData data){
	}

	/**
	 * Trata o valor passado como parametro, retirando os caracteres <, > e "
	 * 
	 * @param value
	 * @return
	 */
	
	public String removeSignals(String value) {
		if (value != null) {
			return value.replaceAll("<", "").replaceAll(">", "").replaceAll("\"", "").trim();
		} else {
			return "";
		}
	}
	
	public void compareValues(Float value, String subject, String predicate, MergeProfilingData data) {
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
	
	public void checkSubject(MergeProfilingData data, String subject) {
		if (data.inputSubjectsPredicate.get(subject) != null) {
			data.subjects.add(subject);
		}
	}
	
	public void addValueToHashTable(String subject, String predicate, Float value, MergeProfilingData data) {
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
	
	public HashSet<String> addValueList(Hashtable<String,HashSet<String>> predicates, String key, String predicate) {
		HashSet<String> subjectPredicates = predicates.getOrDefault(key, new HashSet<String>());
		subjectPredicates.add(predicate);
		return subjectPredicates;
	}
}