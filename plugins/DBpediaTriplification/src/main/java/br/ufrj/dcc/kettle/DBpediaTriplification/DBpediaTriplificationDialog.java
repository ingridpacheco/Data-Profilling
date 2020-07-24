package br.ufrj.dcc.kettle.DBpediaTriplification;

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
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import br.ufrj.ppgi.greco.kettle.plugin.tools.swthelper.SwtHelper;

/**
* @author IngridPacheco
*
*/
public class DBpediaTriplificationDialog extends BaseStepDialog implements StepDialogInterface {
	
	private static Class<?> PKG = DBpediaTriplificationMeta.class;
	
	private DBpediaTriplificationMeta DBpediaTriplification;
	private SwtHelper swthlp;
	private String dialogTitle;
	
	private Group wInputGroup;
	private ComboVar wDBpedia;
	private ComboVar wTemplate;
	private ComboVar wResourceField;
	private Button wspecifyResource;
	private Button wGetNotMappedResources;
	
	private Group wOutputGroup;
	private TextVar wOutputCSVBrowse;
	
	private String[] DBpediaValues = { "pt", "fr", "ja"};
	private String[] TemplateValues;
	
	
	public DBpediaTriplificationDialog(Shell parent, Object in, TransMeta tr, String sname) {
		super(parent, (BaseStepMeta) in, tr, sname);
		DBpediaTriplification = (DBpediaTriplificationMeta) in;
		swthlp = new SwtHelper(tr, this.props);
		
		dialogTitle = BaseMessages.getString(PKG, "DBpediaTriplificationStep.Title");
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
	
	public List<String> getResourceNames(Elements resources, List<String> notMappedResources){
		this.logBasic("Getting the not mapped resources names");
		for (int i = 0; i < resources.size(); i++) {
			if (!resources.get(i).hasAttr("accesskey")) {
				String resourceName = resources.get(i).text();
            	
            	this.logBasic(String.format("Not Mapped Resource: %s",resourceName));
            	if (!notMappedResources.contains(resourceName)) {
            		notMappedResources.add(resourceName);
            	}
			}
			else {
				break;
			}
		}
		
		return notMappedResources;
	}
	
	public List<String> getNotMappedResources(String DBpedia, String template, List<String> mappedResources) {
		this.logBasic("Getting the not mapped resources");
		List<String> notMappedResources = mappedResources;
		
		try {
			String url = String.format("https://tools.wmflabs.org/templatecount/index.php?lang=%s&namespace=10&name=%s#bottom", DBpedia, template);
			this.logBasic(String.format("Url: %s", url));
			Document doc = Jsoup.connect(url).get();
			Integer quantity = Integer.parseInt(doc.select("form + h3 + p").text().split(" ")[0]);
			this.logBasic(String.format("Quantity %s", quantity));
			
			String resourcesUrl = String.format("https://%s.wikipedia.org/wiki/Special:WhatLinksHere/Template:%s?limit=2000&namespace=0", DBpedia, template);
			Document resourcesDoc = Jsoup.connect(resourcesUrl).get();
			Elements resources = resourcesDoc.select("li a[href^=\"/wiki/\"]");
			Element newPage = resourcesDoc.select("p ~ a[href^=\"/w/index.php?\"]").first();
			
			this.logBasic(String.format("Not mapped resources: %s", resources.size()));
			
			notMappedResources = getResourceNames(resources, notMappedResources);
			
			if (quantity > 2000) {
				Integer timesDivided = quantity/2000;
				while (timesDivided > 0) {
					String newUrl = newPage.attr("href");
					newUrl = newUrl.replaceAll("amp;", "");
					String otherPageUrl = String.format("https://%s.wikipedia.org%s", DBpedia, newUrl);
					Document moreResourceDocs = Jsoup.connect(otherPageUrl).get();
					resources = moreResourceDocs.select("li a[href^=\"/wiki/\"]");
					newPage = moreResourceDocs.select("p ~ a[href^=\"/w/index.php?\"]").get(1);
					notMappedResources = getResourceNames(resources, notMappedResources);
					timesDivided -= 1;
				}
			}
			
			return notMappedResources;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return notMappedResources;
		}
	}
	
	public List<String> getMappedResources(String DBpedia, String Template, List<String> resources) {
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
            	resources.add(templateName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resources;
	}
	
	public String[] getResourceValues(String DBpedia, String Template, Boolean hasNotMapped) throws IOException{
		
		List<String> templateResources = new ArrayList<>();
		
		Template = Template.replace(" ", "_");
		
		if (DBpedia.equals("pt") || DBpedia.equals("fr") || DBpedia.equals("ja")) {
			templateResources = getMappedResources(DBpedia, Template, templateResources);
		}
		
		if (hasNotMapped) {
			templateResources = getNotMappedResources(DBpedia, Template, templateResources);
		}
        
        return templateResources.toArray(new String[0]);
	}
	
	private ComboVar appendComboVar(Control lastControl, ModifyListener defModListener, Composite parent,
			String label) {
		ComboVar combo = swthlp.appendComboVarRow(parent, lastControl, label, defModListener);
		BaseStepDialog.getFieldsFromPrevious(combo, transMeta, stepMeta);
		return combo;
	}

	private TextVar textVarWithButton(Composite parent, Control lastControl, String label, ModifyListener lsMod,
			String btnLabel, SelectionListener listener) {
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;
		Label wLabel = new Label(parent, SWT.RIGHT);
		wLabel.setText(label);
		props.setLook(wLabel);
		FormData fdLabel = new FormData();
		fdLabel.left = new FormAttachment(0, 0);
		fdLabel.top = new FormAttachment(lastControl, margin);
		fdLabel.right = new FormAttachment(middle, -margin);
		wLabel.setLayoutData(fdLabel);

		Button button = new Button(parent, SWT.PUSH | SWT.CENTER);
		props.setLook(button);
		button.setText(btnLabel);
		FormData fdButton = new FormData();
		fdButton.right = new FormAttachment(100, 0);
		fdButton.top = new FormAttachment(lastControl, margin);
		button.setLayoutData(fdButton);

		TextVar text = new TextVar(transMeta, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(text);
		text.addModifyListener(lsMod);
		FormData fdText = new FormData();
		fdText.left = new FormAttachment(middle, 0);
		fdText.right = new FormAttachment(button, -margin);
		fdText.top = new FormAttachment(lastControl, margin);
		text.setLayoutData(fdText);

		button.addSelectionListener(listener);
		return text;
	}
	
	private void fileDialogFunction(int type, String[] fileExtensions, TextVar receptor, String[] filterNames) {
		FileDialog dialog = new FileDialog(shell, type);
		dialog.setFilterExtensions(fileExtensions);
		if (receptor.getText() != null) {
			dialog.setFileName(receptor.getText());
		}

		dialog.setFilterNames(filterNames);

		if (dialog.open() != null) {
			String str = dialog.getFilterPath() + System.getProperty("file.separator") + dialog.getFileName();
			receptor.setText(str);
		}
	}
	
	private Control buildContents(Control lastControl, ModifyListener defModListener) {
		String inputGroupLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.InputFields.Label");
		wInputGroup = swthlp.appendGroup(shell, lastControl, inputGroupLabel);
		String DBpediaLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.DBpediaField.Label");
		wDBpedia = appendComboVar(wInputGroup, defModListener, wInputGroup, DBpediaLabel);
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
		String templateLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.TemplateField.Label");
		wTemplate = appendComboVar(wDBpedia, defModListener, wInputGroup, templateLabel);
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
		String specifyResourceLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.SpecifyResource.Label");
		wspecifyResource = swthlp.appendCheckboxRow(wInputGroup, wTemplate, specifyResourceLabel,
				new SelectionListener() {
	            @Override
	            public void widgetSelected(SelectionEvent arg0)
	            {
	            	shouldSpecifyResource(wspecifyResource.getSelection());
	            	DBpediaTriplification.setChanged();
	            }
	
	            @Override
	            public void widgetDefaultSelected(SelectionEvent arg0)
	            {
	            	shouldSpecifyResource(wspecifyResource.getSelection());
	            	DBpediaTriplification.setChanged();
	            }
		});
		String getNotMappedFieldLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.GetNotMappedFields.Label");
		wGetNotMappedResources = swthlp.appendCheckboxRow(wInputGroup, wspecifyResource, getNotMappedFieldLabel,
				new SelectionListener() {
	            @Override
	            public void widgetSelected(SelectionEvent arg0)
	            {
	            	DBpediaTriplification.setChanged();
	            }
	
	            @Override
	            public void widgetDefaultSelected(SelectionEvent arg0)
	            {
	            	DBpediaTriplification.setChanged();
	            }
        	});
		String resourceLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.ResourceLabel.Label");
		wResourceField = appendComboVar(wGetNotMappedResources, defModListener, wInputGroup, resourceLabel);
		wResourceField.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				try {
					wResourceField.setItems(getResourceValues(wDBpedia.getText(),wTemplate.getText(), wGetNotMappedResources.getSelection()));
				} catch (IOException e1) {
					String[] resources = new String[0];
					// TODO Auto-generated catch block
					wResourceField.setItems(resources);
				}
				busy.dispose();
			}
		});

		String outputFieldsLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.OutputFields.Label");
		wOutputGroup = swthlp.appendGroup(shell, wInputGroup, outputFieldsLabel);
		String outputCSVBrowseLabel = BaseMessages.getString(PKG, "DBpediaTriplificationStep.OutputCSVBrowse.Label");
		wOutputCSVBrowse = textVarWithButton(wOutputGroup, wOutputGroup, outputCSVBrowseLabel,
				defModListener, BaseMessages.getString(PKG, "DBpediaTriplificationStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.csv; *.CSV" },
								wOutputCSVBrowse, new String[] { ".(csv) files" });
					}
				});

		return wOutputGroup;
	}
	
	private void shouldSpecifyResource(boolean choice) {
		wResourceField.setEnabled(choice);
	}
	
	@Override
	public String open() {
		Shell parent = getParent();
		Display display = parent.getDisplay();
		
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		props.setLook(shell);
		setShellImage(shell, DBpediaTriplification);
		
		ModifyListener lsMod = new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				DBpediaTriplification.setChanged();
			}
		};
	
		changed = DBpediaTriplification.hasChanged();
		
		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;
		
		shell.setLayout(formLayout);
		shell.setText(dialogTitle);// Messages.getString(“KafkaTopicPartitionConsumerDialog.Shell.Title”));
		
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
		
		lastWidget = buildContents(lastWidget, lsMod);
		
		// Buttons
		wOK = new Button(shell, SWT.PUSH);
		wOK.setText(BaseMessages.getString(PKG, "DBpediaTriplificationStep.Btn.OK")); //$NON-NLS-1$
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "DBpediaTriplificationStep.Btn.Cancel")); //$NON-NLS-1$
		
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
		setSize();
		
		getData(DBpediaTriplification, true);
		
		// Widen the shell size
		Rectangle shellBounds = shell.getBounds();
		shellBounds.height += 35;
		shell.setBounds(shellBounds);
		
		DBpediaTriplification.setChanged(changed);
		
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
		setData(DBpediaTriplification);
		dispose();
	}
	
	private void cancel() {
		stepname = null;
		DBpediaTriplification.setChanged(changed);
		dispose();
	}
	
	/**
	* Copy information from the meta-data input to the dialog fields.
	*/
	/**
	* @param consumerMeta
	* @param copyStepname
	*/
	private void getData(DBpediaTriplificationMeta DBpediaTriplification, boolean copyStepname) {
		if (copyStepname) {
			wStepname.setText(stepname);
			if (DBpediaTriplification.getDBpedia() != null)
				wDBpedia.setText(DBpediaTriplification.getDBpedia());
			if (DBpediaTriplification.getTemplate() != null)
				wTemplate.setText(DBpediaTriplification.getTemplate());
			if (DBpediaTriplification.getOutputCSVFile() != null)
				wOutputCSVBrowse.setText(DBpediaTriplification.getOutputCSVFile());
			if (DBpediaTriplification.getResource() != null)
				wResourceField.setText(DBpediaTriplification.getResource());
			wspecifyResource.setSelection(DBpediaTriplification.getSpecifyResource());
			wGetNotMappedResources.setSelection(DBpediaTriplification.getNotMappedResources());
			shouldSpecifyResource(wspecifyResource.getSelection());
		}
	}
	
	/**
	* Copy information from the dialog fields to the meta-data input
	*/
	private void setData(DBpediaTriplificationMeta DBpediaTriplification) {
		stepname = wStepname.getText();
		DBpediaTriplification.setDBpedia(wDBpedia.getText());
		DBpediaTriplification.setTemplate(wTemplate.getText());
		DBpediaTriplification.setOutputCSVFile(wOutputCSVBrowse.getText());
		DBpediaTriplification.setNotMappedResources(wGetNotMappedResources.getSelection());
		DBpediaTriplification.setResource(wResourceField.getText());
		DBpediaTriplification.setSpecifyResource(wspecifyResource.getSelection());
		DBpediaTriplification.setChanged();
	}
}