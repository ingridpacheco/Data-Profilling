/**
*
*/
package br.ufrj.ingrid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
* @author IngridPacheco
*
*/
public class ResourceDataCompletudeDialog extends BaseStepDialog implements StepDialogInterface {
	private ResourceDataCompletudeMeta resourceDataCompletudeMeta;
	private ComboVar wDBpedia;
	private ComboVar wTemplate;
	private ComboVar wResource;
	
	private String[] DBpediaValues = {"fr", "ja", "pt"};
	  
	private String[] TemplateValues;
	
	public ResourceDataCompletudeDialog(Shell parent, Object in, TransMeta tr, String sname) {
		super(parent, (BaseStepMeta) in, tr, sname);
		resourceDataCompletudeMeta = (ResourceDataCompletudeMeta) in;
	}
	
	public String[] getTemplateValues(String DBpedia){
		try {
			String url = String.format("http://mappings.dbpedia.org/index.php/Mapping_%s", DBpedia.toLowerCase());
			Document doc = Jsoup.connect(url).get();
			Elements mappings = doc.select(String.format("a[href^=\"/index.php/Mapping_%s:\"]", DBpedia.toLowerCase()));
			
			TemplateValues = new String[mappings.size()];
			for (int i = 0; i < mappings.size(); i++) {
				String templateMapping = mappings.get(i).text();
				TemplateValues[i] = templateMapping.split(":")[1];
			}
			
		  	return TemplateValues;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  	TemplateValues = new String[1];
		  	TemplateValues[0] = "";
		  	return TemplateValues;
		}
	}
	
	public String[] getResourceValues(String DBpedia, String Template) throws IOException{
		
		List<String> templateResources = new ArrayList<>();
		
		Template = Template.replace(" ", "_");
		
		Map<String, String> mapTemplateUrl = new HashMap<String, String>();
		mapTemplateUrl.put("pt", "Predefinição");
		mapTemplateUrl.put("fr", "Modèle");
		mapTemplateUrl.put("ja", "Template");
		
		String templateUrl = String.format("http://%s.dbpedia.org/resource/%s:%s", DBpedia, mapTemplateUrl.get(DBpedia) ,Template);
		
		String queryStr =
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX dbp: ?dbpUrl \n"
				+ "SELECT DISTINCT ?uri ?name WHERE {\n"
				+ "?uri dbp:wikiPageUsesTemplate ?templateUrl. \n"
				+ "?uri rdfs:label ?label.filter langMatches(lang(?label), ?language). \n"
				+ "BIND(STR(?label)AS ?name).} \n";
		
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setCommandText(queryStr);
		pss.setIri("dbpUrl", String.format("http://%s.dbpedia.org/property/", DBpedia));
		pss.setIri("templateUrl", templateUrl);
		pss.setLiteral("language", DBpedia);
		
		String sparqlUrl = String.format("http://%s.dbpedia.org/sparql", DBpedia);
				
        Query query = QueryFactory.create(pss.asQuery());

        try ( QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlUrl, query) ) {
            ((QueryEngineHTTP)qexec).addParam("timeout", "10000") ;

            ResultSet rs = qexec.execSelect();
            
            while (rs.hasNext()) {
            	QuerySolution resource = rs.next();
            	String templateName = resource.getLiteral("name").getString();
            	templateResources.add(templateName);
            }
            ResultSetFormatter.out(System.out, rs, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return templateResources.toArray(new String[0]);
	}
	
	@Override
	public String open() {
		
		Shell parent = getParent();
		Display display = parent.getDisplay();
		
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		props.setLook(shell);
		setShellImage(shell, resourceDataCompletudeMeta);
		
		ModifyListener lsMod = new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				resourceDataCompletudeMeta.setChanged();
			}
		};
	
		changed = resourceDataCompletudeMeta.hasChanged();
		
		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;
		
		shell.setLayout(formLayout);
		shell.setText("Resource Data Completude");// Messages.getString(“KafkaTopicPartitionConsumerDialog.Shell.Title”));
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;
		
		// Step name
		wlStepname = new Label(shell, SWT.RIGHT);
		wlStepname.setText("Step Name");// Messages.getString(“KafkaTopicPartitionConsumerDialog.StepName.Label”));
		props.setLook(wlStepname);
		fdlStepname = new FormData();
		fdlStepname.left = new FormAttachment(0, 0);
		fdlStepname.right = new FormAttachment(middle, -margin);
		fdlStepname.top = new FormAttachment(0, margin);
		wlStepname.setLayoutData(fdlStepname);
		wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wStepname);
		wStepname.addModifyListener(lsMod);
		fdStepname = new FormData();
		fdStepname.left = new FormAttachment(middle, 0);
		fdStepname.top = new FormAttachment(0, margin);
		fdStepname.right = new FormAttachment(100, 0);
		wStepname.setLayoutData(fdStepname);
		Control lastWidget = wStepname;
		
		Label wlDBpedia = new Label(shell, SWT.RIGHT);
		wlDBpedia.setText("DBpedia Field");// Messages.getString(“KafkaTopicPartitionConsumerDialog.TopicName.Label”));
		props.setLook(wlDBpedia);
		FormData fdlDBpedia = new FormData();
		fdlDBpedia.top = new FormAttachment(lastWidget, margin);
		fdlDBpedia.left = new FormAttachment(0, 0);
		fdlDBpedia.right = new FormAttachment(middle, -margin);
		wlDBpedia.setLayoutData(fdlDBpedia);
		wDBpedia = new ComboVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wDBpedia);
		wDBpedia.addModifyListener(lsMod);
		wDBpedia.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				wDBpedia.setItems(DBpediaValues);
				shell.setCursor(null);
				busy.dispose();
			}
		});
		FormData fdDBpedia = new FormData();
		fdDBpedia.top = new FormAttachment(lastWidget, margin);
		fdDBpedia.left = new FormAttachment(middle, 0);
		fdDBpedia.right = new FormAttachment(100, 0);
		wDBpedia.setLayoutData(fdDBpedia);
		lastWidget = wDBpedia;
		
		Label wlTemplate = new Label(shell, SWT.RIGHT);
		wlTemplate.setText("Template Field");// Messages.getString(“KafkaTopicPartitionConsumerDialog.TopicName.Label”));
		props.setLook(wlTemplate);
		FormData fdlTemplate = new FormData();
		fdlTemplate.top = new FormAttachment(lastWidget, margin);
		fdlTemplate.left = new FormAttachment(0, 0);
		fdlTemplate.right = new FormAttachment(middle, -margin);
		wlTemplate.setLayoutData(fdlTemplate);
		wTemplate = new ComboVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wTemplate);
		wTemplate.addModifyListener(lsMod);
		wTemplate.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wTemplate.setItems(getTemplateValues(wDBpedia.getText()));
				busy.dispose();
			}
		});
		FormData fdTemplate = new FormData();
		fdTemplate.top = new FormAttachment(lastWidget, margin);
		fdTemplate.left = new FormAttachment(middle, 0);
		fdTemplate.right = new FormAttachment(100, 0);
		wTemplate.setLayoutData(fdTemplate);
		lastWidget = wTemplate;
		
		Label wlResource = new Label(shell, SWT.RIGHT);
		wlResource.setText("Resource Field");// Messages.getString(“KafkaTopicPartitionConsumerDialog.TopicName.Label”));
		props.setLook(wlResource);
		FormData fdlResource = new FormData();
		fdlResource.top = new FormAttachment(lastWidget, margin);
		fdlResource.left = new FormAttachment(0, 0);
		fdlResource.right = new FormAttachment(middle, -margin);
		wlResource.setLayoutData(fdlResource);
		wResource = new ComboVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wResource);
		wResource.addModifyListener(lsMod);
		wResource.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				try {
					wResource.setItems(getResourceValues(wDBpedia.getText(),wTemplate.getText()));
				} catch (IOException e1) {
					String[] resources = new String[0];
					// TODO Auto-generated catch block
					wResource.setItems(resources);
				}
				busy.dispose();
			}
		});
		FormData fdResource = new FormData();
		fdResource.top = new FormAttachment(lastWidget, margin);
		fdResource.left = new FormAttachment(middle, 0);
		fdResource.right = new FormAttachment(100, 0);
		wResource.setLayoutData(fdResource);
		lastWidget = wResource;
		
		// Buttons
		wOK = new Button(shell, SWT.PUSH);
		wOK.setText(BaseMessages.getString("System.Button.OK")); //$NON-NLS-1$
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString("System.Button.Cancel")); //$NON-NLS-1$
		
		setButtonPositions(new Button[] { wOK, wCancel }, margin, null);
		
		lsCancel = new Listener() {
			public void handleEvent(Event e) {
				cancel();
			}
		};
		lsOK = new Listener() {
			public void handleEvent(Event e) {
				ok();
			}
		};
		wCancel.addListener(SWT.Selection, lsCancel);
		wOK.addListener(SWT.Selection, lsOK);
		
		lsDef = new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				ok();
			}
		};
		
		// Set the shell size, based upon previous time…
		setSize(shell, 200, 150, true);
		getData(resourceDataCompletudeMeta, true);
		// consumerMeta.setChanged(changed);
		
		// setTableFieldCombo();
		
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		return stepname;
	}
	
	@SuppressWarnings("deprecation")
	private void ok() {
		if (Const.isEmpty(wStepname.getText())) {
			return;
		}
		setData(resourceDataCompletudeMeta);
		dispose();
	}
	
	private void cancel() {
		stepname = null;
		resourceDataCompletudeMeta.setChanged(changed);
		dispose();
	}
	
	/**
	* Copy information from the meta-data input to the dialog fields.
	*/
	/**
	* @param consumerMeta
	* @param copyStepname
	*/
	private void getData(ResourceDataCompletudeMeta resourceCompletudeMeta, boolean copyStepname) {
		if (copyStepname) {
			wStepname.setText(stepname);
			if (resourceCompletudeMeta.getDBpedia() != null)
				wDBpedia.setText(resourceCompletudeMeta.getDBpedia());
			if (resourceCompletudeMeta.getTemplate() != null)
				wTemplate.setText(resourceCompletudeMeta.getTemplate());
			if (resourceCompletudeMeta.getTemplate() != null)
				wTemplate.setText(resourceCompletudeMeta.getTemplate());
		}
	}
	
	/**
	* Copy information from the dialog fields to the meta-data input
	*/
	private void setData(ResourceDataCompletudeMeta resourceCompletudeMeta) {
		stepname = wStepname.getText();
//		resourceDataCompletudeMeta.setNewField(wDBpedia.getText());
		resourceCompletudeMeta.setDBpedia(wDBpedia.getText());
		resourceCompletudeMeta.setTemplate(wTemplate.getText());
		resourceCompletudeMeta.setResource(wResource.getText());
		resourceCompletudeMeta.setChanged();
	}
}