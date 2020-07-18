![](img/logo_big2.png)

ETL4Profiling is a Kettle extension that gets the profile of the database inputed in the plugins, specially DBpedia, its main case of study. 

## How to use the project

To use the project, the only need is to [download the newest version of the plugins](https://github.com/ingridpacheco/ETL4Profiling/releases) and extract the file ``.tar.gz`` in the folder ``plugins/`` of your Kettle.

### Plugins

ETL4Profiling is currently providing the following plugins:

* [InnerProfiling](https://maven.apache.org/)
* [MergeProfiling](https://sourceforge.net/projects/pentaho/)
* [DBpediaTriplification](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [PropertyAnalyzer](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [ResourceInputAnalyzer](https://maven.apache.org/)
* [ResourcePropertiesAnalyzer](https://sourceforge.net/projects/pentaho/)
* [TemplatePropertyAnalyzer](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [TemplateResourceAnalyzer](https://maven.apache.org/)
* [TemplateResourceInputAnalyzer](https://sourceforge.net/projects/pentaho/)

## Development

### Requirements

* [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) to development.
* [Maven](https://maven.apache.org/) to manage dependencies.
* [Kettle 8.2](https://sourceforge.net/projects/pentaho/) to test and deploy.

This project was developed using Eclipse IDE, but it can be used with any other IDE of your preference.

### Installation

1. Download and install Kettle, Pentaho Data Integration (pdi-ce-8.2.0.0-342 or latest version).
2. Download, install and settup Maven.
3. Download the [newest version of the plugins](https://github.com/ingridpacheco/ETL4Profiling/releases) and change the variable ``pdi.home`` in the pom of your root project ``plugins`` to the place where your Kettle is installed, and then, run ``mvn clean install`` in your project's root.

## License

This project uses the MIT license. For more details, read [LICENSE.md](LICENSE).

## Inspired in

* [ETL4LODPlus](https://github.com/johncurcio/ETL4LODPlus) and [ETL4DBpedia](https://github.com/JeanGabrielNguemaN/ETL4DBpedia)