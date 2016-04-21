package com.seibelsdata.di.plugins.seibelsyamlinput;

import java.util.ArrayList;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

// TODO: Set constants and data things in here

public class SeibelsYAMLInputData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	public RowMetaInterface convertRowMeta;
	
	public int numYAMLKeys;
	public ArrayList<String> keyFields;
	public int[] keyFieldIndex;
	
    public SeibelsYAMLInputData() {
		super();
		keyFields = new ArrayList<String>();
	}
}
	
