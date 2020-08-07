package br.ufrj.dcc.kettle.GetDBpediaData;

public class Url {
	
	public String notMapedResourcesUrl;
	public String resourcesUrl;
	public String templatePropertiesUrl;
	public String DBpedia;
	public String template;
	public String dbpUrl;
	public String sparqlUrl;

	public Url(GetDBpediaDataMeta meta){
		this.DBpedia = meta.getDBpedia();
		this.template = meta.getTemplate();

		this.notMapedResourcesUrl = String.format("https://tools.wmflabs.org/templatecount/index.php?lang=%s&namespace=10&name=%s#bottom", this.DBpedia, this.template);
		this.resourcesUrl = String.format("https://%s.wikipedia.org/wiki/Special:WhatLinksHere/Template:%s?limit=2000&namespace=0", this.DBpedia, this.template);
		this.templatePropertiesUrl =  String.format("http://mappings.dbpedia.org/index.php/Mapping_%s:%s", this.DBpedia, meta.getTemplate().replaceAll(" ", "_"));
		this.dbpUrl = String.format("http://%s.dbpedia.org/property/", this.DBpedia);
		this.sparqlUrl = String.format("http://%s.dbpedia.org/sparql", this.DBpedia);
	}

	
	public String getNextResourceUrl(String nextResource){
		return String.format("https://%s.wikipedia.org%s", this.DBpedia, nextResource);
	}

	public String getResourceUrl(String resource){
		return String.format("http://%s.dbpedia.org/resource/%s", this.DBpedia, resource.replace(" ", "_"));
	}

	public String getTemplateUrl(String templateDefinition){
		return String.format("http://%s.dbpedia.org/resource/%s:%s", this.DBpedia, templateDefinition, this.template.replaceAll(" ", "_"));
	}

}