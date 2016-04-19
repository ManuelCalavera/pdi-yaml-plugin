package com.seibelsdata.di.plugins.seibelsyamlinput;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.yaml.snakeyaml.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

// TODO: Comment and Document

public class SeibelsYAMLInput extends BaseStep implements StepInterface {
	String fileName;
	Map<String, String> keys;
	Map<String, Integer> keyIndecies;
	Map<String, Object> yamlData;
	ArrayList< ArrayList<String[]> > parsedMap;

	SeibelsYAMLInputMeta meta;
	SeibelsYAMLInputData data;
	
	public SeibelsYAMLInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
	    Object[] outputRowData = getRow();
	    
		// log progress if it is time to to so
		if (checkFeedback(getLinesRead())) {
			logBasic("Linenr " + getLinesRead());
		}

	    if (first) { // step just started
	        first = false;
	        
	        data.outputRowMeta = (RowMetaInterface) new RowMeta();

	        parseMapToArray(yamlData, "");

	        // Get the unique keys
	        for (ArrayList<String[]> aL : parsedMap) {
	            for (String[] s : aL) {
	            	keys.put(s[0], "");
	            }
	        }
	        
	        logDetailed("keys: " + keys.keySet().toString());

	        // Create a field for every unique key combination in the YAML file
	        meta.generateFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore, keys.keySet());
	        
	        // Set number of unique keys
	        // TODO: set other data variables in the Data class to comply with Pentaho best practices
	        data.numYAMLKeys = keys.keySet().size();
	        
	        // Set a field index for every key
	        int i = 0;
	        for (String key : keys.keySet()) {
	        	keyIndecies.put(key, i++);
	        }
	        
	        // Every ArrayList in the parsedMap is a new row
	        for (ArrayList<String[]> aL : parsedMap) {
			    Object[] row = RowDataUtil.allocateRowData(data.numYAMLKeys);

		        // Every String in that ArrayList is a new field
	            for (String[] s : aL) {
				    RowDataUtil.addValueData(row, keyIndecies.get(s[0]), s[1]);
	            }
	            
	            putRow( data.outputRowMeta, row );
	        }
	    }
	    
        // if no more rows are expected, indicate step is finished
	    if ( outputRowData == null ) {
	      setOutputDone();
	      return false;
	    }
	    
	    // pass original rows through
	    // TODO: Disable incoming rows, this step should not receive rows
	    // putRow( data.outputRowMeta, outputRowData );

	    // true means this step is not done
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (SeibelsYAMLInputMeta) smi;
		data = (SeibelsYAMLInputData) sdi;
		
		fileName = environmentSubstitute(meta.getFilePath());
		keys = new HashMap<String, String>();
		keyIndecies = new HashMap<String, Integer>();
		parsedMap = new ArrayList< ArrayList<String[]> >();
		parsedMap.add(new ArrayList<String[]>());

		Yaml yaml = new Yaml();
		try {
			yamlData = (Map<String, Object>) yaml.load(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			logError("Unable to open file '" + fileName + "'");
			e.printStackTrace();
		}

		return super.init(meta, data);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (SeibelsYAMLInputMeta) smi;
		data = (SeibelsYAMLInputData) sdi;

		yamlData.clear();

		super.dispose(meta, data);
	}
	

	/*************************************************
	 * 
	 * 
	 * @param map: the map to parse
	 * @param prefix: the prefix to add to the key
	 *************************************************/
	@SuppressWarnings({ "unchecked" })
	public void parseMapToArray(Map<String, Object> map, String prefix) {
		String thisKey = "";
		Object thisValue = null;
		
        for (Entry<String, Object> entry : map.entrySet()) {
        	thisKey = entry.getKey();
        	thisValue = entry.getValue();
        	
		    if (thisValue instanceof ArrayList) {
		        ArrayList<Map<String, Object>> thisEntry = (ArrayList<Map<String, Object>>) thisValue;
            	for (Map<String, Object> newMap : thisEntry) {
            		parseMapToArray(newMap, prefix + thisKey + "-");
            		logDetailed(" ");
            		parsedMap.add(new ArrayList<String[]>());
            	}
		    } else if (thisValue instanceof Map) {
		    	parseMapToArray((Map<String, Object>) thisValue, prefix + thisKey + "-");

            } else {
            	logDetailed(prefix + thisKey + "=" + thisValue);
            	parsedMap.get(parsedMap.size() - 1).add(new String[] {prefix + thisKey, (thisValue == null) ? "null" : thisValue.toString()});
            }
        }
	}
}