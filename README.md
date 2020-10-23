![](img/logo_big2.png)

ETL4Profiling is a Kettle extension that gets the profile of the database inputed in the plugins, specially DBpedia, its main case of study. 

## How to use the project

To use the project, the only need is to [download the newest version of the plugins](https://github.com/ingridpacheco/ETL4Profiling/releases) and extract the file ``.tar.gz`` in the folder ``plugins/`` of your Kettle.

### Plugins

ETL4Profiling is currently providing the following plugins:

* [DBpediaTriplification](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/DBpediaTriplification)
* [GetDBpediaData](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/GetDBpediaData)
* [InnerProfiling](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/InnerProfiling)
* [MergeProfiling](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/MergeProfiling)
* [PropertyAnalyzer](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/PropertyAnalyzer)
* [ResourceInputAnalyzer](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/ResourceInputAnalyzer)
* [ResourcePropertiesAnalyzer](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/ResourcePropertiesAnalyzer)
* [TemplatePropertyAnalyzer](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/TemplatePropertyAnalyzer)
* [TemplateResourceAnalyzer](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/TemplateResourceAnalyzer)
* [TemplateResourceInputAnalyzer](https://github.com/ingridpacheco/ETL4Profiling/tree/master/plugins/TemplateResourceInputAnalyzer)

## Development

### Requirements

* [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) to development.
* [Maven](https://maven.apache.org/) to manage dependencies.
* [Kettle 8.2](https://sourceforge.net/projects/pentaho/) to test and deploy.

This project was developed using Eclipse IDE, but it can be used with any other IDE of your preference.

### Installation

1. Download and install Kettle, Pentaho Data Integration (pdi-ce-8.2.0.0-342 or latest version).
2. Download, install and settup Maven.
3. Download the [newest version of the plugins](https://github.com/ingridpacheco/ETL4Profiling/releases) and change the variable ``pdi.home`` in the pom of your root project ``plugins`` to the place where your Kettle is installed, and then, run ``mvn clean install`` in your ``plugins`` folder.

### Creating a new plugin

To create a new plugin, a specific documentation is available in [HOW TO CREATE A KETTLE PLUGIN](https://github.com/johncurcio/ETL4LODPlus/blob/master/docs/PLUGINS.md) by John Curcio.
To create a plugin for ETL4Profiling the only modification is in the mvn creation. Instead of ``br.ufrj.ppgi.greco.kettle`` in the groupId, it should be ``br.ufrj.dcc.kettle``.

```
cd ETL4Profiling/plugins
$ mvn archetype:generate -DgroupId=br.ufrj.dcc.kettle.NomeDoPlugin -DartifactId=NomeDoPlugin -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

## License

This project uses the MIT license. For more details, read [LICENSE.md](LICENSE).

## Inspired by

* [ETL4LODPlus](https://github.com/johncurcio/ETL4LODPlus)
* [ETL4DBpedia](https://github.com/JeanGabrielNguemaN/ETL4DBpedia)