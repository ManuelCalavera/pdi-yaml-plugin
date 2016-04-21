package com.seibelsdata.di.plugins.seibelsyamlinput;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
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
	Map<String, Integer> keyIndicies;
	ArrayList< ArrayList<String[]> > parsedMap;

	SeibelsYAMLInputMeta meta;
	SeibelsYAMLInputData data;
	
	public SeibelsYAMLInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	// This is called every time the step receives a row from kettle
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    	// The step just started
	    if (first) {
	        first = false;

	        // Every ArrayList in the parsedMap is a new row
	        for (ArrayList<String[]> aL : parsedMap) {
	        	if (aL.isEmpty()) continue;
	        	
			    Object[] row = RowDataUtil.allocateRowData(data.numYAMLKeys);

		        // Every String in that ArrayList is a new field
	            for (String[] s : aL) {
				    RowDataUtil.addValueData(row, keyIndicies.get(s[0]), s[1]);
	            }
	            
	            // Send each ArrayList out as a row
	            putRow( data.outputRowMeta, row );
	        }
	    }
	    
        // if no more rows are expected tell kettle the step is finished
	    //Object[] outputRowData = getRow(); // We are throwing input rows out
	    //if ( outputRowData == null ) {
		if ( getRow() == null ) {
	      setOutputDone();
	      return false;
	    }
		
		// log progress if it is time to to so
		if (checkFeedback(getLinesRead())) {
			logBasic("Linenr " + getLinesRead());
		}

	    // TODO: Disable incoming rows, this step should not receive rows
	    // pass original rows through
	    // putRow( data.outputRowMeta, outputRowData );

	    // true means this step is not done
		return true;
	}

	/*****************************************
	 * init is called when the step is initialized by kettle
	 * During this method we are going to:
	 *    read the YAML file using SnakeYAML
	 *    parse the resulting map recursively
	 *    get the unique keys from the YAML
	 *    create a field for each key
	 *    log the indices of each field
	 */
	@SuppressWarnings("unchecked")
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (SeibelsYAMLInputMeta) smi;
		data = (SeibelsYAMLInputData) sdi;
		
		// keys: map of unique keys
		// yamlData: the results from SnakeYAML
		
		Map<String, String> keys = new HashMap<String, String>();
		Map<String, Object> yamlData = new HashMap<String, Object>();;
		String fileName = environmentSubstitute(meta.getFilePath());
		
		keyIndicies = new HashMap<String, Integer>();
		parsedMap = new ArrayList< ArrayList<String[]> >();
		
		// add empty arraylist
		parsedMap.add(new ArrayList<String[]>());

		// Use SnakeYAML to parse the file
		Yaml yaml = new Yaml();
		try {
			yamlData = (Map<String, Object>) yaml.load(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			logError("Unable to open file '" + fileName + "'");
			e.printStackTrace();
		}
		
		// recursively parse the YAML map 
        parseMapToArray(yamlData, "");

        // Get the unique keys
        for (ArrayList<String[]> aL : parsedMap) {
            for (String[] s : aL) {
            	keys.put(s[0], "");
            }
        }
        
        logDetailed("keys: " + keys.keySet().toString());
        
        // Set number of unique keys
        // TODO: set other data variables in the Data class to comply with Pentaho best practices
        data.numYAMLKeys = keys.keySet().size();
        meta.allocate(data.numYAMLKeys);
        
        // Set a field index for every key
        int i = 0;
        for (String key : keys.keySet()) {
        	meta.addOutputField(key, i);
        	keyIndicies.put(key, i++);
        }

		// Create new row metadata
        // Create a field for every unique key combination in the YAML file
        data.outputRowMeta = (RowMetaInterface) new RowMeta();
        try {
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
		} catch (KettleStepException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // clean up
        yamlData.clear();
        keys.clear();

		return super.init(meta, data);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (SeibelsYAMLInputMeta) smi;
		data = (SeibelsYAMLInputData) sdi;

		// clean up
		keyIndicies.clear();
		parsedMap.clear();
		
		super.dispose(meta, data);
	}
	

	/*************************************************
	 * parseMapToArray: A recursive function for parsing Maps
	 * 
	 * If the Map has a key-value pair where the value is another Map
	 * this function will flatten it to a List while retaining the hierarchy
	 * 
	 * Using Detailed logging will produce a log
	 * with the same contents of the resulting arraylist
	 * 
	 * @param map: the map to parse
	 * @param prefix: the prefix to add to the keys of the map
	 *************************************************/
	@SuppressWarnings({ "unchecked" })
	public void parseMapToArray(Map<String, Object> map, String prefix) {
		String thisKey = "";
		Object thisValue = null;
		
        for (Entry<String, Object> entry : map.entrySet()) {
        	thisKey = entry.getKey();
        	thisValue = entry.getValue();
        	
        	// If the Object is an arraylist of maps we'll have to read each one
		    if (thisValue instanceof ArrayList) {
		        ArrayList<Map<String, Object>> thisEntry = (ArrayList<Map<String, Object>>) thisValue;
            	for (Map<String, Object> newMap : thisEntry) {
            		parseMapToArray(newMap, prefix + thisKey + "-");
            		logDetailed(" "); // New line to separate arraylist entries
            		parsedMap.add(new ArrayList<String[]>());
            	}
            	
            // If the Object is a map we can recursively read it
		    } else if (thisValue instanceof Map) {
		    	parseMapToArray((Map<String, Object>) thisValue, prefix + thisKey + "-");

		    // Otherwise we will consider the key-value pair to not have sub-objects
            } else {
            	logDetailed(prefix + thisKey + "=" + thisValue);
            	parsedMap.get(parsedMap.size() - 1).add(new String[] {prefix + thisKey,
            			                                             (thisValue == null) ? "null"
            			                                            		             : thisValue.toString()});
            }
        }
	}
}