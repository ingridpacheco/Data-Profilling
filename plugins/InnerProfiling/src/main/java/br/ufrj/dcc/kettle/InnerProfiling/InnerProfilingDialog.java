/**
*
*/
package br.ufrj.dcc.kettle.InnerProfiling;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
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
public class InnerProfilingDialog extends BaseStepDialog implements StepDialogInterface {
	
	private static Class<?> PKG = InnerProfilingMeta.class;
	
	private InnerProfilingMeta InnerProfiling;
	private SwtHelper swthlp;
	private String dialogTitle;
	
	private Group wInputGroup;
	private ComboVar wInputChoice;
	private ComboVar wNTripleFieldName;
	private ComboVar wSubject;
	private ComboVar wPredicate;
	
	private Group wOutputGroup;
	private TextVar wOutputBrowse;
	private TextVar wOutputCSVBrowse;
	
	private String[] InputValues = {
			"N-Triple",
			"RDF Fields"
	};
	
	public InnerProfilingDialog(Shell parent, Object in, TransMeta tr, String sname) {
		super(parent, (BaseStepMeta) in, tr, sname);
		InnerProfiling = (InnerProfilingMeta) in;
		swthlp = new SwtHelper(tr, this.props);
		
		dialogTitle = BaseMessages.getString(PKG, "InnerProfilingStep.Title");
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
		String inputGroupLabel = BaseMessages.getString(PKG, "InnerProfilingStep.InputFields.Label");
		wInputGroup = swthlp.appendGroup(shell, lastControl, inputGroupLabel);
		String InputChoiceLabel = BaseMessages.getString(PKG, "InnerProfilingStep.InputChoice.Label");
		wInputChoice = appendComboVar(wInputGroup, defModListener, wInputGroup, InputChoiceLabel);
		wInputChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}

			public void widgetSelected(SelectionEvent e) {
				chooseNTripleOrFields(wInputChoice.getText());
				InnerProfiling.setChanged(true);
			}
		});
		wInputChoice.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				wInputChoice.setItems(InputValues);
				shell.setCursor(null);
				busy.dispose();
			}
		});
		
		String NTripleLabel = BaseMessages.getString(PKG, "InnerProfilingStep.NTripleField.Label");
		wNTripleFieldName = appendComboVar(wInputChoice, defModListener, wInputGroup, NTripleLabel);
		wNTripleFieldName.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wNTripleFieldName.setItems(getFields(ValueMetaInterface.TYPE_STRING));
				busy.dispose();
			}
		});
		
		String subjectLabel = BaseMessages.getString(PKG, "InnerProfilingStep.SubjectField.Label");	
		wSubject = appendComboVar(wNTripleFieldName, defModListener, wInputGroup, subjectLabel);
		wSubject.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wSubject.setItems(getFields(ValueMetaInterface.TYPE_STRING));
				busy.dispose();
			}
		});
		
		String predicateLabel = BaseMessages.getString(PKG, "InnerProfilingStep.PredicateField.Label");	
		wPredicate = appendComboVar(wSubject, defModListener, wInputGroup, predicateLabel);
		wPredicate.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
				shell.setCursor(busy);
				shell.setCursor(null);
				wPredicate.setItems(getFields(ValueMetaInterface.TYPE_STRING));
				busy.dispose();
			}
		});
		
		String outputLabel = BaseMessages.getString(PKG, "InnerProfilingStep.OutputFields.Label");
		wOutputGroup = swthlp.appendGroup(shell, wInputGroup, outputLabel);
		
		String outputReportLabel = BaseMessages.getString(PKG, "InnerProfilingStep.OutputReport.Label");
		wOutputBrowse = textVarWithButton(wOutputGroup, wOutputGroup, outputReportLabel,
				defModListener, BaseMessages.getString(PKG, "InnerProfilingStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.txt; *.TXT" },
								wOutputBrowse, new String[] { ".(txt) files" });
					}
				});
		
		String outputCSVLabel = BaseMessages.getString(PKG, "InnerProfilingStep.OutputCSVBrowse.Label");
		wOutputCSVBrowse = textVarWithButton(wOutputGroup, wOutputBrowse, outputCSVLabel,
				defModListener, BaseMessages.getString(PKG, "InnerProfilingStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.csv; *.CSV" },
								wOutputCSVBrowse, new String[] { ".(csv) files" });
					}
				});

		return wOutputGroup;
	}
	
	private void chooseNTripleOrFields(String choice) {
		Boolean enabled = (choice.equals("N-Triple")) ? true : false;
		wNTripleFieldName.setEnabled(enabled);
		wSubject.setEnabled(!enabled);
		wPredicate.setEnabled(!enabled);
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
	
	/**
	   * This method is called by Spoon when the user opens the settings dialog of the step.
	   * It should open the dialog and return only once the dialog has been closed by the user.
	   *  
	   * If the user confirms the dialog, the meta object (passed in the constructor) must
	   * be updated to reflect the new step settings. The changed flag of the meta object must  
	   * reflect whether the step configuration was changed by the dialog.
	   *  
	   * If the user cancels the dialog, the meta object must not be updated, and its changed flag
	   * must remain unaltered.
	   *  
	   * The open() method must return the name of the step after the user has confirmed the dialog,
	   * or null if the user cancelled the dialog.
	   */
	@Override
	public String open() {
		Shell parent = getParent();
		Display display = parent.getDisplay();
		
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		props.setLook(shell);
		setShellImage(shell, InnerProfiling);
		
		ModifyListener lsMod = new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				InnerProfiling.setChanged();
			}
		};
	
		changed = InnerProfiling.hasChanged();
		
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
		wOK.setText(BaseMessages.getString(PKG, "InnerProfilingStep.Btn.OK")); //$NON-NLS-1$
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "InnerProfilingStep.Btn.Cancel")); //$NON-NLS-1$
		
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
		
		// populate the dialog with the values from the meta object
		getData();
		
		// restore the changed flag to original value, as the modify listeners fire during dialog population  
		InnerProfiling.setChanged( changed );
		
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		return stepname;
	}
	
	/**
	* Called when the user confirms the dialog
	*/
	private void ok() {
		if (StringUtils.isEmpty(wStepname.getText())) {
			return;
		}
		setData();
		dispose();
	}
	
	/**
	* Called when the user cancels the dialog.  
	*/
	private void cancel() {
		// The "stepname" variable will be the return value for the open() method.  
	    // Setting to null to indicate that dialog was cancelled.
		stepname = null;
		// Restoring original "changed" flag on the met aobject
		InnerProfiling.setChanged(changed);
		// close the SWT dialog window
		dispose();
	}
	
	/**
	* Copy information from the meta-data input to the dialog fields.
	*/
	private void getData() {
		wStepname.setText(stepname);
		if (InnerProfiling.getNTripleFieldName() != null)
			wNTripleFieldName.setText(InnerProfiling.getNTripleFieldName());
		if (InnerProfiling.getSubject() != null)
			wSubject.setText(InnerProfiling.getSubject());
		if (InnerProfiling.getPredicate() != null)
			wPredicate.setText(InnerProfiling.getPredicate());
		if (InnerProfiling.getOutputFile() != null)
			wOutputBrowse.setText(InnerProfiling.getOutputFile());
		if (InnerProfiling.getOutputCSVFile() != null)
			wOutputCSVBrowse.setText(InnerProfiling.getOutputCSVFile());
		if (InnerProfiling.getInputChoice() != null)
			wInputChoice.setText(InnerProfiling.getInputChoice());
		chooseNTripleOrFields(wInputChoice.getText());
	}
	
	/**
	* Copy information from the dialog fields to the meta-data input
	*/
	private void setData() {
		// The "stepname" variable will be the return value for the open() method.  
	    // Setting to step name from the dialog control
		stepname = wStepname.getText();
		
		// Setting the  settings to the meta object
		InnerProfiling.setInputChoice(wInputChoice.getText());
		InnerProfiling.setNTripleFieldName(wNTripleFieldName.getText());
		InnerProfiling.setSubject(wSubject.getText());
		InnerProfiling.setPredicate(wPredicate.getText());
		InnerProfiling.setOutputFile(wOutputBrowse.getText());
		InnerProfiling.setOutputCSVFile(wOutputCSVBrowse.getText());
		
		// close the SWT dialog window
		InnerProfiling.setChanged();
	}
}