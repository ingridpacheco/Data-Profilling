/**
*
*/
package br.ufrj.dcc.kettle.MergeProfiling;

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
public class MergeProfilingDialog extends BaseStepDialog implements StepDialogInterface {
	
	private static Class<?> PKG = MergeProfilingMeta.class;
	
	private MergeProfilingMeta MergeProfiling;
	private SwtHelper swthlp;
	private String dialogTitle;
	
	private Group wInputGroup;
	private Button wIsInputCSV;
	private TextVar wInputFirstCSVBrowse;
	private ComboVar wInputChoice;
	private ComboVar wNTripleFieldName;
	private ComboVar wSubject;
	private ComboVar wPredicate;
	
	private Group wInputSecondGroup;
	private Button wIsTriplified;
	private TextVar wInputCSVBrowse;
	
	private Group wOutputGroup;
	private TextVar wOutputBrowse;
	private TextVar wOutputCSVBrowse;
	
	private String[] InputValues = {
			"N-Triple",
			"RDF Fields"
	};
	
	public MergeProfilingDialog(Shell parent, Object in, TransMeta tr, String sname) {
		super(parent, (BaseStepMeta) in, tr, sname);
		MergeProfiling = (MergeProfilingMeta) in;
		swthlp = new SwtHelper(tr, this.props);
		
		dialogTitle = BaseMessages.getString(PKG, "MergeProfilingStep.Title");
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
	
	private Control firstInputFields(Control lastControl, ModifyListener defModListener) {
		String inputGroupLabel = BaseMessages.getString(PKG, "MergeProfilingStep.InputFields.Label");
		wInputGroup = swthlp.appendGroup(shell, lastControl, inputGroupLabel);
		String isInputCSVLabel = BaseMessages.getString(PKG, "MergeProfilingStep.isInputCSV.Label");
		wIsInputCSV = swthlp.appendCheckboxRow(wInputGroup, wInputGroup, isInputCSVLabel,
				new SelectionListener() {
	            @Override
	            public void widgetSelected(SelectionEvent arg0)
	            {
	            	shouldInputCSV(wIsInputCSV.getSelection());
	            	MergeProfiling.setChanged();
	            }
	
	            @Override
	            public void widgetDefaultSelected(SelectionEvent arg0)
	            {
	            	shouldInputCSV(wIsInputCSV.getSelection());
	            	MergeProfiling.setChanged();
	            }
		});
		String inputCSVLabel = BaseMessages.getString(PKG, "MergeProfilingStep.FirstInputCSVBrowse.Label");
		wInputFirstCSVBrowse = textVarWithButton(wInputGroup, wIsInputCSV, inputCSVLabel,
				defModListener, BaseMessages.getString(PKG, "MergeProfilingStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.csv; *.CSV" },
								wInputFirstCSVBrowse, new String[] { ".(csv) files" });
					}
				});
		String InputChoiceLabel = BaseMessages.getString(PKG, "MergeProfilingStep.InputChoice.Label");
		wInputChoice = appendComboVar(wInputFirstCSVBrowse, defModListener, wInputGroup, InputChoiceLabel);
		wInputChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}

			public void widgetSelected(SelectionEvent e) {
				chooseNTripleOrFields(wInputChoice.getText());
				MergeProfiling.setChanged(true);
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
		
		String NTripleLabel = BaseMessages.getString(PKG, "MergeProfilingStep.NTripleField.Label");
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
		
		String subjectLabel = BaseMessages.getString(PKG, "MergeProfilingStep.SubjectField.Label");	
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
		
		String predicateLabel = BaseMessages.getString(PKG, "MergeProfilingStep.PredicateField.Label");	
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
		return wInputGroup;
	}
	
	private Control secondInputFields(Control lastControl, ModifyListener defModListener) {
		String inputSecondGroupLabel = BaseMessages.getString(PKG, "MergeProfilingStep.InputSecondFields.Label");
		wInputSecondGroup = swthlp.appendGroup(shell, lastControl, inputSecondGroupLabel);
		String isTriplifiedLabel = BaseMessages.getString(PKG, "MergeProfilingStep.IsTriplified.Label");
		wIsTriplified = swthlp.appendCheckboxRow(wInputSecondGroup, wInputSecondGroup, isTriplifiedLabel,
			new SelectionListener() {
	
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
	
				public void widgetSelected(SelectionEvent e) {
					MergeProfiling.setChanged(true);
				}
		});
		String inputCSVLabel = BaseMessages.getString(PKG, "MergeProfilingStep.InputCSVBrowse.Label");
		wInputCSVBrowse = textVarWithButton(wInputSecondGroup, wIsTriplified, inputCSVLabel,
				defModListener, BaseMessages.getString(PKG, "MergeProfilingStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.csv; *.CSV" },
								wInputCSVBrowse, new String[] { ".(csv) files" });
					}
				});
		return wInputSecondGroup;
	}
	
	private Control buildContents(Control lastControl, ModifyListener defModListener) {
		lastControl = firstInputFields(lastControl, defModListener);
		
		lastControl = secondInputFields(lastControl, defModListener);
		
		String outputLabel = BaseMessages.getString(PKG, "MergeProfilingStep.OutputFields.Label");
		wOutputGroup = swthlp.appendGroup(shell, lastControl, outputLabel);
		
		String outputReportLabel = BaseMessages.getString(PKG, "MergeProfilingStep.OutputReport.Label");
		wOutputBrowse = textVarWithButton(wOutputGroup, wOutputGroup, outputReportLabel,
				defModListener, BaseMessages.getString(PKG, "MergeProfilingStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.txt; *.TXT" },
								wOutputBrowse, new String[] { ".(txt) files" });
					}
				});
		
		String outputCSVLabel = BaseMessages.getString(PKG, "MergeProfilingStep.OutputCSVBrowse.Label");
		wOutputCSVBrowse = textVarWithButton(wOutputGroup, wOutputBrowse, outputCSVLabel,
				defModListener, BaseMessages.getString(PKG, "MergeProfilingStep.Btn.Browse"), new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fileDialogFunction(SWT.OPEN, new String[] { "*.csv; *.CSV" },
								wOutputCSVBrowse, new String[] { ".(csv) files" });
					}
				});

		return wOutputGroup;
	}
	
	private void shouldInputCSV(boolean choice) {
		wInputChoice.setEnabled(!choice);
		wNTripleFieldName.setEnabled(!choice);
		wSubject.setEnabled(!choice);
		wPredicate.setEnabled(!choice);
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
		setShellImage(shell, MergeProfiling);
		
		ModifyListener lsMod = new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				MergeProfiling.setChanged();
			}
		};
	
		changed = MergeProfiling.hasChanged();
		
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
		wOK.setText(BaseMessages.getString(PKG, "MergeProfilingStep.Btn.OK")); //$NON-NLS-1$
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "MergeProfilingStep.Btn.Cancel")); //$NON-NLS-1$
		
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
		MergeProfiling.setChanged( changed );
		
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
		MergeProfiling.setChanged(changed);
		// close the SWT dialog window
		dispose();
	}
	
	/**
	* Copy information from the meta-data input to the dialog fields.
	*/
	private void getData() {
		wStepname.setText(stepname);
		if (MergeProfiling.getNTripleFieldName() != null)
			wNTripleFieldName.setText(MergeProfiling.getNTripleFieldName());
		if (MergeProfiling.getSubject() != null)
			wSubject.setText(MergeProfiling.getSubject());
		if (MergeProfiling.getPredicate() != null)
			wPredicate.setText(MergeProfiling.getPredicate());
		if (MergeProfiling.getIsTriplified() != null)
			wIsTriplified.setSelection(MergeProfiling.getIsTriplified());
		if (MergeProfiling.getInputCSVFile() != null)
			wInputCSVBrowse.setText(MergeProfiling.getInputCSVFile());
		if (MergeProfiling.getOutputFile() != null)
			wOutputBrowse.setText(MergeProfiling.getOutputFile());
		if (MergeProfiling.getOutputCSVFile() != null)
			wOutputCSVBrowse.setText(MergeProfiling.getOutputCSVFile());
		if (MergeProfiling.getInputChoice() != null)
			wInputChoice.setText(MergeProfiling.getInputChoice());
		if (MergeProfiling.getInputCSVBrowse() != null)
			wInputFirstCSVBrowse.setText(MergeProfiling.getInputCSVBrowse());
		wIsInputCSV.setSelection(MergeProfiling.getIsInputCSV());
		chooseNTripleOrFields(wInputChoice.getText());
		shouldInputCSV(true);
	}
	
	/**
	* Copy information from the dialog fields to the meta-data input
	*/
	private void setData() {
		// The "stepname" variable will be the return value for the open() method.  
	    // Setting to step name from the dialog control
		stepname = wStepname.getText();
		
		// Setting the  settings to the meta object
		MergeProfiling.setInputChoice(wInputChoice.getText());
		MergeProfiling.setNTripleFieldName(wNTripleFieldName.getText());
		MergeProfiling.setSubject(wSubject.getText());
		MergeProfiling.setPredicate(wPredicate.getText());
		MergeProfiling.setIsTriplified(wIsTriplified.getSelection());
		MergeProfiling.setInputCSVFile(wInputCSVBrowse.getText());
		MergeProfiling.setOutputFile(wOutputBrowse.getText());
		MergeProfiling.setOutputCSVFile(wOutputCSVBrowse.getText());
		MergeProfiling.setInputCSVBrowse(wInputFirstCSVBrowse.getText());
		MergeProfiling.setIsInputCSV(wIsInputCSV.getSelection());
		
		// close the SWT dialog window
		MergeProfiling.setChanged();
	}
}