package com.seibelsdata.di.plugins.seibelsyamlinput;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.*;
import org.pentaho.di.trans.step.*;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

// TODO: ValueMeta is deprecated, move over to whatever the new implementation is

public class SeibelsYAMLInputMeta extends BaseStepMeta implements StepMetaInterface {

	private static Class<?> PKG = SeibelsYAMLInputMeta.class; // for i18n purposes
	private String filePath;

	public SeibelsYAMLInputMeta() {
		super(); 
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
		return new SeibelsYAMLInputDialog(shell, meta, transMeta, name);
	}
	
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp) {
		return new SeibelsYAMLInput(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}
	
	public StepDataInterface getStepData() {
		return new SeibelsYAMLInputData();
	}
	
	public void setDefault() {
		filePath = "";
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/*****************************
	 * Save step settings to ktr
	 *****************************/
	public String getXML() throws KettleValueException {
		String xml = XMLHandler.addTagValue("filePath", filePath);
		return xml;
	}
	
	/*****************************
	 * Load step settings from ktr
	 *****************************/
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
		try {
			setFilePath(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "filePath")));
		} catch (Exception e) {
			throw new KettleXMLException("YAML plugin unable to read step info from XML node", e);
		}
	}
	
	public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException {
		try {
			rep.saveStepAttribute(id_transformation, id_step, "filePath", filePath); //$NON-NLS-1$
		} catch(Exception e) {
			throw new KettleException("Unable to save step into repository: "+id_step, e); 
		}
	}
	
	public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException  {
		try {
			setFilePath(rep.getStepAttributeString(id_step, "filePath")); //$NON-NLS-1$
		} catch(Exception e) {
			throw new KettleException("Unable to load step from repository", e);
		}
	}
	
	public void generateFields(RowMetaInterface inputRowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore, Set<String> keySet) throws KettleStepException{
        for (String key : keySet) { 
			// a value meta object contains the meta data for a field
			ValueMetaInterface v = new ValueMeta(key, ValueMeta.TYPE_STRING);
			v.setTrimType(ValueMeta.TRIM_TYPE_BOTH);
			v.setOrigin(origin);
			
			// modify the row structure and add the field this step generates  
			inputRowMeta.addValueMeta(v);
        }
	}
	
	public void getFields(RowMetaInterface inputRowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException{

		// a value meta object contains the meta data for a field
		ValueMetaInterface v = new ValueMeta("test");
		v.setTrimType(ValueMeta.TRIM_TYPE_BOTH);
		v.setOrigin(origin);
		
		// modify the row structure and add the field this step generates  
		inputRowMeta.addValueMeta(v);
		
	}
	
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository, IMetaStore metaStore)  {
		// See if there are input streams leading to this step!
		remarks.add((input.length > 0)
				  ? new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "YAML.CheckResult.ReceivingRows.OK"), stepMeta)
		          : new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "YAML.CheckResult.ReceivingRows.ERROR"), stepMeta));
	}
}
