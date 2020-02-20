/*! ******************************************************************************
*
* Pentaho Data Integration
*
* Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
*
*******************************************************************************
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
******************************************************************************/

package br.ufrj.ingrid.kettle;

import java.io.IOException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
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
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;

/**
 * This class is part of the demo step plug-in implementation.
 * It demonstrates the basics of developing a plug-in step for PDI.  
 *  
 * The demo step adds a new string field to the row stream and sets its
 * value to "Hello World!". The user may select the name of the new field.
 *  
 * This class is the implementation of StepDialogInterface.
 * Classes implementing this interface need to:
 *  
 * - build and open a SWT dialog displaying the step's settings (stored in the step's meta object)
 * - write back any changes the user makes to the step's meta object
 * - report whether the user changed any settings when confirming the dialog  
 *  
 */
public class DataCompletudeStepDialog extends BaseStepDialog implements StepDialogInterface {

  /**
   *  The PKG member is used when looking up internationalized strings.
   *  The properties file with localized keys is expected to reside in  
   *  {the package of the class specified}/messages/messages_{locale}.properties  
   */
  private static Class<?> PKG = DataCompletudeStepMeta.class; // for i18n purposes  

  // this is the object the stores the step's settings
  // the dialog reads the settings from it when opening
  // the dialog writes the settings to it when confirmed  
  private DataCompletudeStepMeta meta;
  private String dialogTitle;
  
  /* tab DataSources */
  private ComboVar wcDBpediaField;
  private Label wlDBpediaField;
  
  private String[] DBpediaValues = {
		  "ar", "az", "be", "bg", "bn",
		  "ca", "ceb", "commons", "cs", "cy",
		  "da", "de", "el", "en", "eo",
		  "es", "et", "eu", "fa", "fi",
		  "fr", "ga", "gl", "hi", "hr",
		  "hu", "hy", "id", "it", "ja",
		  "ko", "lt", "lv", "mk", "mt",
		  "nl", "pl", "pt", "ru", "ro",
		  "sk", "sl", "sr", "sv", "tr",
		  "uk", "ur", "vi", "war", "zh"};
  
  private ComboVar wcTemplateField;
  private Label wlTemplateField;
  
  private String[] TemplateValues;

  private FormData fdlDBpediaField;
  private FormData fdcDBpediaField;
  
  private FormData fdlTemplateField;
  private FormData fdcTemplateField;
  

  /**
   * The constructor should simply invoke super() and save the incoming meta
   * object to a local variable, so it can conveniently read and write settings
   * from/to it.
   *  
   * @param parent   the SWT shell to open the dialog in
   * @param in    the meta object holding the step's settings
   * @param transMeta  transformation description
   * @param sname    the step name
   */
  public DataCompletudeStepDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );

    meta = (DataCompletudeStepMeta) in;

    // Additional initialization here
	  dialogTitle = BaseMessages.getString( PKG, "DataCompletudeStep.Title" );
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
 * @throws IOException 
   */
  
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
  
  public String open() {
    // store some convenient SWT variables  
    Shell parent = getParent();
    Display display = parent.getDisplay();

    // SWT code for preparing the dialog
    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX );
    props.setLook( shell );
    setShellImage( shell, meta );

    // Save the value of the changed flag on the meta object. If the user cancels
    // the dialog, it will be restored to this saved value.
    // The "changed" variable is inherited from BaseStepDialog
    boolean changed = meta.hasChanged();

    // The ModifyListener used on all controls. It will update the meta object to  
    // indicate that changes are being made.
    ModifyListener lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        meta.setChanged();
      }
    };

    // ------------------------------------------------------- //
    // SWT code for building the actual settings dialog        //
    // ------------------------------------------------------- //
    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);

    shell.setText(dialogTitle);

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    wlStepname = new Label( shell, SWT.RIGHT );
    wlStepname.setText( BaseMessages.getString( PKG, "DataCompletudeStep.StepNameField.Label" ) );
    props.setLook( wlStepname );
    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment( 0, 0 );
    fdlStepname.right = new FormAttachment( middle, -margin );
    fdlStepname.top = new FormAttachment( 0, margin );
    wlStepname.setLayoutData( fdlStepname );

    wStepname = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wStepname.setText( stepname );
    props.setLook( wStepname );
    wStepname.addModifyListener( lsMod );
    fdStepname = new FormData();
    fdStepname.left = new FormAttachment( middle, 0 );
    fdStepname.top = new FormAttachment( 0, margin );
    fdStepname.right = new FormAttachment( 100, 0 );
    wStepname.setLayoutData( fdStepname );

    wlDBpediaField = new Label(shell, SWT.RIGHT);
	wlDBpediaField.setText(BaseMessages.getString(PKG, "DataCompletudeStep.DBpediaField.Label"));
	props.setLook(wlDBpediaField);
	fdlDBpediaField = new FormData();
	fdlDBpediaField.left = new FormAttachment(0, 0);
	fdlDBpediaField.top = new FormAttachment(wStepname, margin);
	fdlDBpediaField.right = new FormAttachment(middle, -margin);
	wlDBpediaField.setLayoutData(fdlDBpediaField);

	wcDBpediaField = new ComboVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
	props.setLook(wcDBpediaField);
	wcDBpediaField.addModifyListener(lsMod);
	fdcDBpediaField = new FormData();
	fdcDBpediaField.left = new FormAttachment(middle, 0);
	fdcDBpediaField.right = new FormAttachment(100, 0);
	fdcDBpediaField.top = new FormAttachment(wStepname, margin);
	wcDBpediaField.setLayoutData(fdcDBpediaField);
	wcDBpediaField.addFocusListener(new FocusListener() {
		public void focusLost(org.eclipse.swt.events.FocusEvent e) {
		}

		public void focusGained(org.eclipse.swt.events.FocusEvent e) {
			Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
			shell.setCursor(busy);
			wcDBpediaField.setItems(DBpediaValues);
			shell.setCursor(null);
			busy.dispose();
		}
	});
		
	wlTemplateField = new Label(shell, SWT.RIGHT);
	wlTemplateField.setText(BaseMessages.getString(PKG, "DataCompletudeStep.TemplateField.Label"));
	props.setLook(wlTemplateField);
	fdlTemplateField = new FormData();
	fdlTemplateField.left = new FormAttachment(0, 0);
	fdlTemplateField.top = new FormAttachment(wcDBpediaField, margin);
	fdlTemplateField.right = new FormAttachment(middle, -margin);
	wlTemplateField.setLayoutData(fdlTemplateField);

	wcTemplateField = new ComboVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
	props.setLook(wcTemplateField);
	wcTemplateField.addModifyListener(lsMod);
	fdcTemplateField = new FormData();
	fdcTemplateField.left = new FormAttachment(middle, 0);
	fdcTemplateField.right = new FormAttachment(100, 0);
	fdcTemplateField.top = new FormAttachment(wcDBpediaField, margin);
	wcTemplateField.setLayoutData(fdcTemplateField);
	wcTemplateField.addFocusListener(new FocusListener() {
		public void focusLost(org.eclipse.swt.events.FocusEvent e) {
		}

		public void focusGained(org.eclipse.swt.events.FocusEvent e) {
			Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
			shell.setCursor(busy);
			shell.setCursor(null);
			wcTemplateField.setItems(getTemplateValues(wcDBpediaField.getText()));
			busy.dispose();
		}
	});
    
    // OK and cancel buttons
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    setButtonPositions( new Button[] { wOK, wCancel }, margin, wcTemplateField );

    // Add listeners for cancel and OK
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };
    wCancel.addListener( SWT.Selection, lsCancel );
    wOK.addListener( SWT.Selection, lsOK );

    // default listener (for hitting "enter")
    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };
    wStepname.addSelectionListener( lsDef );
    // wHelloFieldName.addSelectionListener( lsDef );

    // Detect X or ALT-F4 or something that kills this window and cancel the dialog properly
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    // Set/Restore the dialog size based on last position on screen
    // The setSize() method is inherited from BaseStepDialog
    setSize();

    // populate the dialog with the values from the meta object
    populateDialog();

    // Alarga um pouco mais a janela
	Rectangle shellBounds = shell.getBounds();
	shellBounds.width += 120;
	shellBounds.height += 5;
	shell.setBounds(shellBounds);

    // restore the changed flag to original value, as the modify listeners fire during dialog population  
    meta.setChanged( changed );

    // open dialog and enter event loop  
    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }

    // at this point the dialog has closed, so either ok() or cancel() have been executed
    // The "stepname" variable is inherited from BaseStepDialog
    return stepname;
  }

  /**
   * This helper method puts the step configuration stored in the meta object
   * and puts it into the dialog controls.
   */
  private void populateDialog() {
    wStepname.selectAll();
    // wHelloFieldName.setText( meta.getOutputField() );
    wcDBpediaField.setText(Const.NVL(meta.getInputDBpedia(), "pt"));
    wcTemplateField.setText(Const.NVL(meta.getInputTemplate(), ""));
  }

  /**
   * Called when the user cancels the dialog.  
   */
  private void cancel() {
    // The "stepname" variable will be the return value for the open() method.  
    // Setting to null to indicate that dialog was cancelled.
    stepname = null;
    // Restoring original "changed" flag on the met aobject
    meta.setChanged( changed );
    // close the SWT dialog window
    dispose();
  }

  /**
   * Called when the user confirms the dialog
   */
  private void ok() {
    // The "stepname" variable will be the return value for the open() method.  
    // Setting to step name from the dialog control
    stepname = wStepname.getText();
    // Setting the  settings to the meta object
    meta.setInputDBpedia( wcDBpediaField.getText() );
    meta.setInputTemplate( wcTemplateField.getText() );
    // close the SWT dialog window
    dispose();
  }
}