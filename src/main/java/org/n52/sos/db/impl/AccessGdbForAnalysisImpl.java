/**
 * Copyright (C) 2012 52°North Initiative for Geospatial Open Source Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.sos.db.impl;

import java.io.IOException;

import org.n52.sos.DBInspector;
import org.n52.sos.db.AccessGdbForAnalysis;
import org.n52.util.logging.Logger;

import com.esri.arcgis.carto.IMapServer3;
import com.esri.arcgis.carto.IMapServerDataAccess;
import com.esri.arcgis.datasourcesGDB.SqlWorkspace;
import com.esri.arcgis.geodatabase.FeatureClass;
import com.esri.arcgis.geodatabase.ICursor;
import com.esri.arcgis.geodatabase.IDataset;
import com.esri.arcgis.geodatabase.IEnumDataset;
import com.esri.arcgis.geodatabase.IRow;
import com.esri.arcgis.geodatabase.Workspace;
import com.esri.arcgis.geodatabase.esriDatasetType;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.server.json.JSONObject;

/**
 * @author <a href="mailto:broering@52north.org">Arne Broering</a>
 */
public class AccessGdbForAnalysisImpl implements AccessGdbForAnalysis {

    static Logger LOGGER = Logger.getLogger(AccessGdbForAnalysisImpl.class.getName());


    private DBInspector soe;


	private WorkspaceWrapper workspace;

    public AccessGdbForAnalysisImpl(DBInspector soe) throws AutomationException, IOException {
        LOGGER.debug("Creating AccessGdbForAnalysisImpl.");
        
        this.soe = soe;
        
        // Workspace creation
        IMapServer3 ms = (IMapServer3) soe.getMapServerDataAccess();
        String mapName = ms.getDefaultMapName();
        IMapServerDataAccess mapServerDataAccess = soe.getMapServerDataAccess();
        Object dataSource= mapServerDataAccess.getDataSource(mapName, 0);
        FeatureClass fc = new FeatureClass(dataSource);
        Workspace workspace = new Workspace(fc.getWorkspace());
        
        this.workspace = new WorkspaceWrapper();
        
        if (fc.getWorkspace() instanceof SqlWorkspace) {
        	this.workspace.setSqlWorkspace((SqlWorkspace) fc.getWorkspace());
        	this.workspace.setWorkspace(workspace);
        }
        else {
        	this.workspace.setWorkspace(workspace);
        }
        
    }
    
    public JSONObject analyzeTable (JSONObject inputObject) throws AutomationException, IOException {

        String tableName = null;
        if (inputObject.has("tableName")) {
            tableName = inputObject.getString("tableName");
        }
        
        String primaryKeyColumn = null;
        if (inputObject.has("primaryKeyColumn")) {
            tableName = inputObject.getString("primaryKeyColumn");
        }
        
        return analyzeTable(tableName, primaryKeyColumn);
    }
    
    
    /**
     * @throws IOException 
     * @throws AutomationException 
     * 
     */
    public JSONObject analyzeTable (String tableName, String primaryKeyColumn) throws AutomationException, IOException {
        
        
        JSONObject json = new JSONObject();

        try {
            ICursor cursor = DatabaseUtils.evaluateQuery(tableName, "", "COUNT(" + primaryKeyColumn + ")",
            		workspace);
            
            json.append("Reading count of table:", tableName);
            IRow row;
            if ((row = cursor.nextRow()) != null) {
                Object count = row.getValue(0);
                String countAsString = count.toString();
                
                json.append("Table count:", countAsString);
            }
        } catch (Exception e) {
            LOGGER.severe(e.getLocalizedMessage(), e);
            throw new IOException(e);
        }
        
        return json;
    }
    
    /**
     * @throws IOException 
     * @throws AutomationException 
     * 
     */
    public JSONObject analyzeProcedureTable () throws AutomationException, IOException {
        
        JSONObject json = new JSONObject();
        json.append("This function: ", "...checks the availability of a table as specified in the properties of this SOE (configure in ArcGIS Server Manager).");
        json.append("This function: ", "...and presents the count of rows contained in that table.");
        
        try {
            ICursor cursor = DatabaseUtils.evaluateQuery(soe.getTable(), "", "COUNT(" + soe.getTablePkField() + ")", workspace);
            
            json.append("Reading count of table:", soe.getTable());
            IRow row;
            if ((row = cursor.nextRow()) != null) {
                Object count = row.getValue(0);
                String countAsString = count.toString();
                
                json.append("Table count:", countAsString);
            }
        } catch (Exception e) {
            LOGGER.severe(e.getLocalizedMessage(), e);
            
            json.append("ERROR:", "while trying to read table '" + soe.getTable() + "' with specified primary key '" + soe.getTablePkField() + "'");
        }
        
        return json;
    }

    public JSONObject readTableNamesFromDB() throws AutomationException, IOException
    {


            JSONObject json = new JSONObject();
            json.append("This function: ", "...reads directly the table names from the DB through ArcGIS Server. This gives you a picture of how the SOE sees your DB.");
            
            IEnumDataset datasets = this.workspace.getWorkspace().getDatasets(esriDatasetType.esriDTAny);
            IDataset dataset = datasets.next();
            
            while (dataset != null) {
                
//                int typeID = dataset.getType();
                
//                if (typeID == esriDatasetType.esriDTTable) {
//                    ITable table = this.workspace.openTable(dataset.getName());
//                    
//                    IFields fields = table.getFields();
//                    
//                }
//                else if (typeID == esriDatasetType.esriDTFeatureClass) {
//                    IFeatureClass featureClass = this.workspace.openFeatureClass(dataset.getName());
//                    IFields fields = featureClass.getFields();
//                     
//                }
//                else {
//                    throw new UnsupportedDataTypeException("Type not supported");
//                }
                
                json.append("Reading dataset names from DB:", dataset.getName());
                
                dataset = datasets.next();
            }
        return json;
    }
}
