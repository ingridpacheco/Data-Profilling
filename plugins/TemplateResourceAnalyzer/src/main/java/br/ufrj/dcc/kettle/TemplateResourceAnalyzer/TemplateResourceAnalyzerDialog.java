/**
*
*/
package br.ufrj.dcc.kettle.TemplateResourceAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
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
import org.jsoup.select.Elements;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
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
public class TemplateResourceAnalyzerDialog extends BaseStepDialog implements StepDialogInterface {
	
	private static Class<?> PKG = TemplateResourceAnalyzerMeta.class;
	
	private TemplateResourceAnalyzerMeta templateResourceAnalyzer;
	private SwtHelper swthlp;
	private String dialogTitle;
	
	private Group wFirstInputGroup;
	private ComboVar wChooseInput;
	private Group wSecondInputGroup;
	private ComboVar wResource;
	private ComboVar wTemplateProperties;
	private ComboVar wResourceProperties;
	private ComboVar wDBpedia;
	private ComboVar wTemplate;
	private Button wGetNotMappedResources;
	
	private Group wOutputGroup;
	private ComboVar wOrder;
	private TextVar wOutputBrowse;
	private TextVar wOutputCSVBrowse;
	
	private String[] DBpediaValues = {
			"pt", "en", "ja",
			"ar", "az", "be",
			"bg", "bn", "ca",
			"ceb", "Commons", "cs",
			"cy", "da", "de",
			"el", "en", "eo",
			"es", "et", "eu",
			"fa", "fi", "fr",
			"ga", "gl", "hi",
			"hr", "hu", "hy",
			"id", "it", "ko",
			"lt", "lv", "mk",
			"mt", "nl", "pl",
			"pt", "ru", "ro",
			"sk", "sl", "sr",
			"sv", "tr", "uk",
			"ur", "vi", "war",
			"zh"
	};
	
	private String[] OrderValues = {"Ascending", "Descending"};
	private String[] TemplateValues;
	
	public TemplateResourceAnalyzerDialog(Shell parent, Object in, TransMeta tr, String sname) {
		super(parent, (BaseStepMeta) in, tr, sname);
		templateResourceAnalyzer = (TemplateResourceAnalyzerMeta) in;
		swthlp = new SwtHelper(tr, this.props);
		dialogTitle = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.Title");
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
		CTabFolder wTabFolder = swthlp.appendTabFolder(shell, lastControl, 90);
		CTabItem item = new CTabItem(wTabFolder, SWT.NONE);
		item.setText(BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.Tab.InputFields"));
		Composite cpt = swthlp.appendComposite(wTabFolder, lastControl);
		
		String chooseInputField = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.DBpediaField.Label");
		wChooseInput = appendComboVar(null, defModListener, cpt, chooseInputField);
		wChooseInput.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wChooseInput.setItems(new String[] {"Previous fields input", "DBpedia fields"});
				busy.dispose();
			}
		});
		wChooseInput.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}

			public void widgetSelected(SelectionEvent e) {
				chooseInputField(wChooseInput.getText());
				templateResourceAnalyzer.setChanged(true);
			}
		});
		
		String inputFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.FirstInputField.Label");
		wFirstInputGroup = swthlp.appendGroup(cpt, wChooseInput, inputFieldName);
		
		String templatePropertiesField = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.TemplatePropertiesField.Label");
		wTemplateProperties = appendComboVar(wFirstInputGroup, defModListener, wFirstInputGroup, templatePropertiesField);
		wTemplateProperties.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wTemplateProperties.setItems(getFields(ValueMetaInterface.TYPE_STRING));
				busy.dispose();
			}
		});
		
		String resourceField = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.ResourceField.Label");
		wResource = appendComboVar(wTemplateProperties, defModListener, wFirstInputGroup, resourceField);
		wResource.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wResource.setItems(getFields(ValueMetaInterface.TYPE_STRING));
				busy.dispose();
			}
		});
		
		String resourcePropertyField = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.ResourcePropertyField.Label");
		wResourceProperties = appendComboVar(wResource, defModListener, wFirstInputGroup, resourcePropertyField);
		wResourceProperties.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wResourceProperties.setItems(getFields(ValueMetaInterface.TYPE_STRING));
				busy.dispose();
			}
		});
		
		String secondInputFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.SecondInputFields.Label");
		wSecondInputGroup = swthlp.appendGroup(cpt, wFirstInputGroup, secondInputFieldName);
		
		String DBpediaFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.DBpediaField.Label");
		wDBpedia = appendComboVar(wSecondInputGroup, defModListener, wSecondInputGroup, DBpediaFieldName);
		wDBpedia.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wDBpedia.setItems(DBpediaValues);
				busy.dispose();
			}
		});
		String templateFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.TemplateField.Label");
		wTemplate = appendComboVar(wDBpedia, defModListener, wSecondInputGroup, templateFieldName);
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
		
		String getNotMappedFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.GetNotMappedFields.Label");
		wGetNotMappedResources = swthlp.appendCheckboxRow(wSecondInputGroup, wTemplate, getNotMappedFieldName,
				new SelectionListener() {
	            @Override
	            public void widgetSelected(SelectionEvent arg0)
	            {
	            	templateResourceAnalyzer.setChanged();
	            }
	
	            @Override
	            public void widgetDefaultSelected(SelectionEvent arg0)
	            {
	            	templateResourceAnalyzer.setChanged();
	            }
        	});
		
		item.setControl(cpt);
		item = new CTabItem(wTabFolder, SWT.NONE);
		item.setText(BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.Tab.OutputFields"));
		cpt = swthlp.appendComposite(wTabFolder, lastControl);

		String outputGroupFieldLabel = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.OutputFields.Label");
		wOutputGroup = swthlp.appendGroup(cpt, null, outputGroupFieldLabel);
		String orderFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.OrderField.Label");
		wOrder = appendComboVar(wOutputGroup, defModListener, wOutputGroup, orderFieldName);
		wOrder.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				wOrder.setItems(OrderValues);
				shell.setCursor(null);
				busy.dispose();
			}
		});
		String browseButtonLabel = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.Btn.Browse");
		String outputBrowseFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.OutputReport.Label");
		wOutputBrowse = textVarWithButton(wOutputGroup, wOrder, outputBrowseFieldName,
				defModListener, browseButtonLabel, new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.txt; *.TXT" },
								wOutputBrowse, new String[] { ".(txt) files" });
					}
				});
		String outputCSVFieldName = BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.OutputCSVBrowse.Label");
		wOutputCSVBrowse = textVarWithButton(wOutputGroup, wOutputBrowse, outputCSVFieldName,
				defModListener, browseButtonLabel, new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.csv; *.CSV" },
								wOutputCSVBrowse, new String[] { ".(csv) files" });
					}
				});
		
		item.setControl(cpt);
		
		wTabFolder.setSelection(0);

		return wTabFolder;
	}
	
	private void chooseInputField(String choice) {
		Boolean enabled = (choice.equals("Previous fields input")) ? true : false;
		wResource.setEnabled(enabled);
		wTemplateProperties.setEnabled(enabled);
		wResourceProperties.setEnabled(enabled);
		wDBpedia.setEnabled(!enabled);
		wTemplate.setEnabled(!enabled);
		wGetNotMappedResources.setEnabled(!enabled);
	}
	
	private String[] getFields(int type) {

		List<String> result = new ArrayList<String>();

		try {
			RowMetaInterface inRowMeta = this.transMeta.getPrevStepFields(stepname);

			List<ValueMetaInterface> fields = inRowMeta.getValueMetaList();

			for (ValueMetaInterface field : fields) {
				if (field.getType() == type || type == -1)
					result.add(field.getName());
			}

		} catch (KettleStepException e) {
			e.printStackTrace();
		}

		return result.toArray(new String[result.size()]);
	}
	
	@Override
	public String open() {
		Shell parent = getParent();
		Display display = parent.getDisplay();
		
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		props.setLook(shell);
		setShellImage(shell, templateResourceAnalyzer);
		
		ModifyListener lsMod = new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				templateResourceAnalyzer.setChanged();
			}
		};
	
		changed = templateResourceAnalyzer.hasChanged();
		
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
		wOK.setText(BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.Btn.OK")); //$NON-NLS-1$
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "TemplateResourceAnalyzerStep.Btn.Cancel")); //$NON-NLS-1$
		
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
		getData(templateResourceAnalyzer, true);
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
		setData(templateResourceAnalyzer);
		dispose();
	}
	
	private void cancel() {
		stepname = null;
		templateResourceAnalyzer.setChanged(changed);
		dispose();
	}
	
	/**
	* Copy information from the meta-data input to the dialog fields.
	*/
	/**
	* @param consumerMeta
	* @param copyStepname
	*/
	private void getData(TemplateResourceAnalyzerMeta templateResourceAnalyzer, boolean copyStepname) {
		if (copyStepname) {
			wStepname.setText(stepname);
			if (templateResourceAnalyzer.getResourceProperties() != null)
				wResourceProperties.setText(templateResourceAnalyzer.getResourceProperties());
			if (templateResourceAnalyzer.getTemplateProperties() != null)
				wTemplateProperties.setText(templateResourceAnalyzer.getTemplateProperties());
			if (templateResourceAnalyzer.getResource() != null)
				wResource.setText(templateResourceAnalyzer.getResource());
			if (templateResourceAnalyzer.getOutputFile() != null)
				wOutputBrowse.setText(templateResourceAnalyzer.getOutputFile());
			if (templateResourceAnalyzer.getOutputCSVFile() != null)
				wOutputCSVBrowse.setText(templateResourceAnalyzer.getOutputCSVFile());
			if (templateResourceAnalyzer.getOrder() != null)
				wOrder.setText(templateResourceAnalyzer.getOrder());
			if (templateResourceAnalyzer.getDBpedia() != null)
				wDBpedia.setText(templateResourceAnalyzer.getDBpedia());
			if (templateResourceAnalyzer.getTemplate() != null)
				wTemplate.setText(templateResourceAnalyzer.getTemplate());
			if (templateResourceAnalyzer.getChooseInput() != null)
				wChooseInput.setText(templateResourceAnalyzer.getChooseInput());
			wGetNotMappedResources.setSelection(templateResourceAnalyzer.getNotMappedResources());
			chooseInputField(wChooseInput.getText());
		}
	}
	
	/**
	* Copy information from the dialog fields to the meta-data input
	*/
	private void setData(TemplateResourceAnalyzerMeta templateResourceAnalyzer) {
		stepname = wStepname.getText();
		templateResourceAnalyzer.setResourceProperties(wResourceProperties.getText());
		templateResourceAnalyzer.setResource(wResource.getText());
		templateResourceAnalyzer.setTemplateProperties(wTemplateProperties.getText());
		templateResourceAnalyzer.setOutputFile(wOutputBrowse.getText());
		templateResourceAnalyzer.setOutputCSVFile(wOutputCSVBrowse.getText());
		templateResourceAnalyzer.setOrder(wOrder.getText());
		templateResourceAnalyzer.setDBpedia(wDBpedia.getText());
		templateResourceAnalyzer.setTemplate(wTemplate.getText());
		templateResourceAnalyzer.setNotMappedResources(wGetNotMappedResources.getSelection());
		templateResourceAnalyzer.setChooseInput(wChooseInput.getText());
		templateResourceAnalyzer.setChanged();
	}
}