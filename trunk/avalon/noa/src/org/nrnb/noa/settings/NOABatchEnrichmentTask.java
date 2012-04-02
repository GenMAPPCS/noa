/*******************************************************************************
 * Copyright 2012 Chao Zhang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.nrnb.noa.settings;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.nrnb.noa.NOA;
import org.nrnb.noa.algorithm.CorrectionMethod;
import org.nrnb.noa.algorithm.StatMethod;
import org.nrnb.noa.result.MultipleOutputDialog;
import org.nrnb.noa.utils.HeatChart;
import org.nrnb.noa.utils.IdMapping;
import org.nrnb.noa.utils.NOAStaticValues;
import org.nrnb.noa.utils.NOAUtil;

class NOABatchEnrichmentTask implements Task {
    private TaskMonitor taskMonitor;
    private boolean success;
    private String algType;
    private String inputFilePath;
    private boolean isWholeNet;
    private String edgeAnnotation;
    private String statMethod;
    private String corrMethod;
    private double pvalue;
    private String speciesGOFile;
    private String speciesDerbyFile;
    private Object idType;
    private String ensemblIDType;
    public List potentialGOList = new ArrayList();
    private JDialog dialog;
    private int formatSign = 0;
    private ArrayList<String> networkNameArray = new ArrayList<String>();
    private String tempHeatmapFileName = "";
    
    public NOABatchEnrichmentTask(boolean isEdge, String inputFilePath,
            boolean isWholeNet, Object edgeAnnotation, Object statMethod,
            Object corrMethod, Object pvalue, String speciesDerbyFile,
            String speciesGOFile, Object idType, String ensemblType) {
        if(isEdge)
            this.algType = NOAStaticValues.Algorithm_EDGE;
        else
            this.algType = NOAStaticValues.Algorithm_NODE;
        this.inputFilePath = inputFilePath;
        this.isWholeNet = isWholeNet;
        this.edgeAnnotation = edgeAnnotation.toString();
        this.statMethod = statMethod.toString();
        this.corrMethod = corrMethod.toString();
        this.pvalue = new Double(pvalue.toString()).doubleValue();
        this.speciesDerbyFile = speciesDerbyFile;
        this.speciesGOFile = speciesGOFile;
        this.idType = idType;
        this.ensemblIDType = ensemblType;
    }

    public void run() {
        try {
            taskMonitor.setPercentCompleted(-1);
            HashMap<String, Set<String>> goNodeRefMap = new HashMap<String, Set<String>>();
            HashMap<String, String> goNodeCountRefMap = new HashMap<String, String>();
            HashMap<String, String> resultMap = new HashMap<String, String>();
            HashMap<String, String> topHitMap = new HashMap<String, String>();
            HashMap<String, ArrayList<String>> outputMap = new HashMap<String, ArrayList<String>>();
            HashMap<String, String> outputTopMap = new HashMap<String, String>();

            Set<String> allNodeSet = new HashSet();
            Set<String> allEdgeSet = new HashSet();
            formatSign = 0;
            String oneSeq = "";
            ArrayList seq = new ArrayList();
            HashMap<String, ArrayList> tempDataArray = new HashMap<String, ArrayList>();
            long start=System.currentTimeMillis();
            //1st step - check file format and get the list of all nodes
            try {
                BufferedReader in = new BufferedReader(new FileReader(inputFilePath));
                String inputLine = in.readLine();
                inputLine = in.readLine();
                while((inputLine.indexOf(">")!=-1)||(inputLine.trim().equals("")||inputLine.equals(null))) {
                    inputLine = in.readLine();
                }
                String[] temp = inputLine.trim().split("\t");
                if(temp.length == 1) {
                    formatSign = NOAStaticValues.SET_FORMAT;
                } else if(temp.length == 2) {
                    formatSign = NOAStaticValues.NETWORK_FORMAT;
                } else {
                    formatSign = NOAStaticValues.WRONG_FORMAT;
                }
            } catch (Exception e) {
                formatSign = NOAStaticValues.WRONG_FORMAT;
                e.printStackTrace();
            }
            if(formatSign != NOAStaticValues.WRONG_FORMAT) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(inputFilePath));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if(inputLine.indexOf(">")!=-1) {
                            if(oneSeq.equals("")) {
                                oneSeq = inputLine.trim();
                            } else {
                                networkNameArray.add(oneSeq);
                                tempDataArray.put(oneSeq, seq);
                                seq = new ArrayList();
                                oneSeq = inputLine.trim();
                            }
                        } else if (inputLine.trim().equals("")||inputLine.equals(null)) {
                            tempDataArray.put(oneSeq, seq);
                            seq = new ArrayList();
                            oneSeq = "";
                        } else {
                            seq.add(inputLine.trim());
                            String[] temp = inputLine.split("\t");
                            if(formatSign == NOAStaticValues.NETWORK_FORMAT) {
                                if(temp.length<2) {
                                    formatSign = NOAStaticValues.WRONG_FORMAT;
                                    break;
                                } else {
                                    allNodeSet.add(temp[0].trim());
                                    allNodeSet.add(temp[1].trim());
                                    if(this.algType.equals(NOAStaticValues.Algorithm_EDGE)) {
                                        if(!(allEdgeSet.contains(temp[0]+"\t"+temp[1])||allEdgeSet.contains(temp[1]+"\t"+temp[0])))
                                            allEdgeSet.add(temp[0]+"\t"+temp[1]);
                                    }
                                }
                            } else if(formatSign == NOAStaticValues.SET_FORMAT) {
                                if(temp.length>1) {
                                    formatSign = NOAStaticValues.WRONG_FORMAT;
                                    break;
                                } else {
                                    allNodeSet.add(inputLine.trim());
                                }
                            }
                        }
                    }
                    if(!oneSeq.equals("")){
                        tempDataArray.put(oneSeq, seq);
                        networkNameArray.add(oneSeq);
                    }
                    in.close();
                } catch (Exception e) {
                    formatSign = NOAStaticValues.WRONG_FORMAT;
                    e.printStackTrace();
                }
            }
            //2nd step - annotate all nodes
            if(formatSign == NOAStaticValues.WRONG_FORMAT) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "The file format is invalid, please check user manual for the detail.", NOA.pluginName,
                    JOptionPane.WARNING_MESSAGE);
            } else if((formatSign == NOAStaticValues.SET_FORMAT)&&(this.algType.equals(NOAStaticValues.Algorithm_EDGE))){
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "Edge-based algorithm cannot be applied to gene sets, please choose Node-based algorithm.", NOA.pluginName,
                    JOptionPane.WARNING_MESSAGE);
            } else {
                List<String> nodeList = new ArrayList<String>();
                goNodeRefMap = new HashMap<String, Set<String>>();
                nodeList = new Vector(allNodeSet);
                IdMapping idMapper = new IdMapping();
                Map<String, Set<String>>[] idGOMapArray = idMapper.mapID2Array(
                    this.speciesDerbyFile, this.speciesGOFile, nodeList,
                    this.idType.toString(), this.ensemblIDType);
                Set<String> networkList = tempDataArray.keySet();
                taskMonitor.setStatus("Obtaining GO list from test networks ......");
                Set<String> GOList = idMapper.convertSetMapValueToSet(idGOMapArray[0]);
                GOList.addAll(idMapper.convertSetMapValueToSet(idGOMapArray[1]));
                GOList.addAll(idMapper.convertSetMapValueToSet(idGOMapArray[2]));
                potentialGOList.addAll(GOList);
                int valueA = 0;
                int valueB = 0;
                int valueC = 0;
                int valueD = 0;
                int recordCount = 0;

                //Pick first 200 networks
                List network200List = new ArrayList();
                if(networkNameArray.size()>200){
                    for(int i=0;i<200;i++)
                        network200List.add(networkNameArray.get(i));
                } else {
                    network200List = networkNameArray;
                }
                
                //Node-base algorithm
                if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {                    
                    taskMonitor.setStatus("Counting nodes for the whole network ......");
                    NOAUtil.retrieveNodeCountMapBatchMode(idGOMapArray, allNodeSet, goNodeRefMap, potentialGOList);
                    if(isWholeNet) {
                        valueD = allNodeSet.size();                        
                    } else {
                        valueD = NOAUtil.retrieveAllNodeCountMap(speciesGOFile, goNodeCountRefMap, potentialGOList);
                    }

                    //Pick the most specific 200 GO IDs
                    List go200List = new ArrayList();
                    if(potentialGOList.size()>200) {
                        Collections.sort(potentialGOList);
                        if(potentialGOList.get(potentialGOList.size()-1).equals("unassigned"))
                            potentialGOList.remove("unassigned");
                        for(int i=potentialGOList.size()-1;i>=potentialGOList.size()-200;i--)
                            go200List.add(potentialGOList.get(i));
                    } else {
                        go200List = potentialGOList;
                    }
                    double[][] pvalueMatrix = new double[network200List.size()][go200List.size()];
                    for(int i=0;i<pvalueMatrix.length;i++) {
                        for(int j=0;j<pvalueMatrix[0].length;j++) {
                            pvalueMatrix[i][j] = 0;
                        }
                    }
                    
                    //Calculate p-value for each network
                    for(String networkID : networkList) {
                        resultMap = new HashMap<String, String>();
                        ArrayList detail = tempDataArray.get(networkID);
                        Set<String> testNodeSet = new HashSet();
                        for(Object line : detail) {
                            String[] temp = line.toString().split("\t");
                            if(formatSign == NOAStaticValues.NETWORK_FORMAT) {
                                if(temp.length>=2){
                                    testNodeSet.add(temp[0]);
                                    testNodeSet.add(temp[1]);
                                }
                            } else if(formatSign == NOAStaticValues.SET_FORMAT) {
                                testNodeSet.add(line.toString().trim());
                            }
                        }
                        HashMap<String, Set<String>> goNodeMap = new HashMap<String, Set<String>>();
                        NOAUtil.retrieveNodeCountMapBatchMode(idGOMapArray, testNodeSet, goNodeMap, potentialGOList);
                        valueB = testNodeSet.size();
                        Object topGOID = "";
                        double topPvalue = 100;
                        for(Object eachGO : potentialGOList) {
                            //System.out.println(goNodeMap.size());
                            if(!eachGO.equals("unassigned")) {
                                if(goNodeMap.containsKey(eachGO)) {
                                    taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                                    valueA = goNodeMap.get(eachGO).size();
                                    if(isWholeNet) {
                                        valueC = goNodeRefMap.get(eachGO).size();
                                    } else {
                                        valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                                    }
                                    double pvalue = 0;
                                    if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                        pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                                    } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                        pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                                    } else {
                                        pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                                    }
                                    int n = go200List.indexOf(eachGO);
                                    int m = network200List.indexOf(networkID);
                                    if(m!=-1&&n!=-1) {
                                        if(Math.log(pvalue)<NOAStaticValues.LOG_PVALUE_CUTOFF)
                                            pvalueMatrix[m][n] = NOAStaticValues.LOG_PVALUE_CUTOFF;
                                        else
                                            pvalueMatrix[m][n] = Math.log(pvalue);
                                    }
                                    if(pvalue<=this.pvalue) {
                                        resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                                        if(pvalue<topPvalue) {
                                            topGOID = eachGO;
                                            topPvalue = pvalue;
                                        }
                                    }
                                }
                            }
                        }
                        taskMonitor.setStatus("Calculating corrected p-value ......");
                        if(corrMethod.equals("none")) {

                        } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                            resultMap = CorrectionMethod.calBenjamCorrection(resultMap, resultMap.size(), pvalue);
                        } else {
                            resultMap = CorrectionMethod.calBonferCorrection(resultMap, resultMap.size(), pvalue);
                        }
//                        for(Object eachGO : potentialGOList) {
//                            if(resultMap.containsKey(eachGO))
//                                outputMap.put(eachGO.toString(), resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length()));
//                        }
                        if(resultMap.containsKey(topGOID))
                            outputTopMap.put(topGOID.toString(), resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length())+"\t"+goNodeMap.get(topGOID));
                        for(Object eachGO : potentialGOList) {
                            if(resultMap.containsKey(eachGO)) {
                                if(outputMap.containsKey(eachGO)) {
                                    ArrayList<String> resultWithNetworkID = outputMap.get(eachGO);
                                    resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"+networkID.substring(1,networkID.length())+"\t"+goNodeMap.get(eachGO));
                                    outputMap.put(eachGO.toString(), resultWithNetworkID);
                                } else {
                                    ArrayList<String> resultWithNetworkID = new ArrayList<String>();
                                    resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"+networkID.substring(1,networkID.length())+"\t"+goNodeMap.get(eachGO));
                                    outputMap.put(eachGO.toString(), resultWithNetworkID);
                                }
                                recordCount++;
                            }
                        }
                    }
                    taskMonitor.setStatus("Generating heatmap ......");
                    HeatChart chart = new HeatChart(pvalueMatrix);
                    chart.setHighValueColour(Color.GREEN);
                    chart.setLowValueColour(Color.RED);
                    chart.setXValues(go200List.toArray());
                    chart.setYValues(network200List.toArray());
                    tempHeatmapFileName = System.currentTimeMillis()+".png";
                    System.out.println();
                    try {
                        chart.saveToFile(new File(NOA.NOATempDir+tempHeatmapFileName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    taskMonitor.setStatus("Done!");
                //Edge-base algorithm.
                } else {
                    System.out.println("Counting edges for the whole clique......");
                    taskMonitor.setStatus("Counting edges for the whole clique......");
                    NOAUtil.retrieveEdgeCountMapBatchMode(idGOMapArray, allEdgeSet, goNodeRefMap, potentialGOList, this.edgeAnnotation);
                    
                    //Pick the most specific 200 GO IDs
                    List go200List = new ArrayList();
                    if(potentialGOList.size()>200) {
                        Collections.sort(potentialGOList);
                        if(potentialGOList.get(potentialGOList.size()-1).equals("unassigned"))
                            potentialGOList.remove("unassigned");
                        for(int i=potentialGOList.size()-1;i>=potentialGOList.size()-200;i--)
                            go200List.add(potentialGOList.get(i));
                    } else {
                        go200List = potentialGOList;
                    }
                    double[][] pvalueMatrix = new double[network200List.size()][go200List.size()];
                    for(int i=0;i<pvalueMatrix.length;i++) {
                        for(int j=0;j<pvalueMatrix[0].length;j++) {
                            pvalueMatrix[i][j] = 0;
                        }
                    }
                    
                    //Calculate p-value for each network
                    for(String networkID : networkList) {
                        //System.out.println(networkID);
                        resultMap = new HashMap<String, String>();
                        ArrayList detail = tempDataArray.get(networkID);
                        Set<String> testEdgeSet = new HashSet();
                        Set<String> testNodeSet = new HashSet();
                        for(Object line : detail) {
                            String[] temp = line.toString().split("\t");
                            if(temp.length>=2){
                                testNodeSet.add(temp[0]);
                                testNodeSet.add(temp[1]);
                                if(!(testEdgeSet.contains(temp[0]+"\t"+temp[1])||testEdgeSet.contains(temp[1]+"\t"+temp[0])))
                                    testEdgeSet.add(temp[0]+"\t"+temp[1]);
                            }
                        }
                        HashMap<String, Set<String>> goNodeMap = new HashMap<String, Set<String>>();
                        goNodeCountRefMap = new HashMap<String, String>();
                        NOAUtil.retrieveEdgeCountMapBatchMode(idGOMapArray, testEdgeSet, goNodeMap, potentialGOList, this.edgeAnnotation);
                        valueB = testEdgeSet.size();
                        if(isWholeNet) {
                            valueD = allEdgeSet.size();
                        } else {
                            valueD = testNodeSet.size()*(testNodeSet.size()-1)/2;
                            NOAUtil.retrieveAllEdgeCountMapBatchMode(idGOMapArray, testNodeSet, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                        }
                        Object topGOID = "";
                        double topPvalue = 100;
                        for(Object eachGO : potentialGOList) {
                            if(!eachGO.equals("unassigned")) {
                                if(goNodeMap.containsKey(eachGO)) {
                                    taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                                    valueA = goNodeMap.get(eachGO).size();
                                    if(isWholeNet) {
                                        valueC = goNodeRefMap.get(eachGO).size();
                                    } else {
                                        valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                                    }
                                    double pvalue = 0;
                                    if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                        pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                                    } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                        pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                                    } else {
                                        pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                                    }
                                    int n = go200List.indexOf(eachGO);
                                    int m = network200List.indexOf(networkID);
                                    if(m!=-1&&n!=-1) {
                                        if(Math.log(pvalue)<NOAStaticValues.LOG_PVALUE_CUTOFF)
                                            pvalueMatrix[m][n] = NOAStaticValues.LOG_PVALUE_CUTOFF;
                                        else
                                            pvalueMatrix[m][n] = Math.log(pvalue);
                                    }
                                    //System.out.println(eachGO+": "+pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                                    if(pvalue<=this.pvalue) {
                                        resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                                        if(pvalue<topPvalue) {
                                            topGOID = eachGO;
                                            topPvalue = pvalue;
                                        }
                                    }
                                }
                            }
                        }
                        //taskMonitor.setStatus("Calculating corrected p-value ......");
                        if(corrMethod.equals("none")) {

                        } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                            resultMap = CorrectionMethod.calBenjamCorrection(resultMap, resultMap.size(), pvalue);
                        } else {
                            resultMap = CorrectionMethod.calBonferCorrection(resultMap, resultMap.size(), pvalue);
                        }
                        if(resultMap.containsKey(topGOID))
                            outputTopMap.put(topGOID.toString(), resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length())+"\t"+goNodeMap.get(topGOID));
                        for(Object eachGO : potentialGOList) {
                            if(resultMap.containsKey(eachGO)) {
                                if(outputMap.containsKey(eachGO)) {
                                    ArrayList<String> resultWithNetworkID = outputMap.get(eachGO);
                                    resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"+networkID.substring(1,networkID.length())+"\t"+goNodeMap.get(eachGO));
                                    outputMap.put(eachGO.toString(), resultWithNetworkID);
                                } else {
                                    ArrayList<String> resultWithNetworkID = new ArrayList<String>();
                                    resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"+networkID.substring(1,networkID.length())+"\t"+goNodeMap.get(eachGO));
                                    outputMap.put(eachGO.toString(), resultWithNetworkID);
                                }
                                recordCount++;
                            }
                        }
                    }
                    taskMonitor.setStatus("Generating heatmap ......");
                    HeatChart chart = new HeatChart(pvalueMatrix);
                    chart.setHighValueColour(Color.GREEN);
                    chart.setLowValueColour(Color.RED);
                    chart.setXValues(go200List.toArray());
                    chart.setYValues(network200List.toArray());
                    tempHeatmapFileName = System.currentTimeMillis()+".png";
                    try {
                        chart.saveToFile(new File(NOA.NOATempDir+tempHeatmapFileName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    taskMonitor.setStatus("Done!");
                }
                if(outputMap.size()>0){
                    dialog = new MultipleOutputDialog(Cytoscape.getDesktop(), false, outputMap, outputTopMap, this.algType, this.formatSign, recordCount, tempHeatmapFileName);
                    dialog.setLocationRelativeTo(Cytoscape.getDesktop());
                    dialog.setResizable(true);
                } else {
                    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "No result for selected criteria!", NOA.pluginName,
                        JOptionPane.WARNING_MESSAGE);
                }
            }
            long pause=System.currentTimeMillis();
            System.out.println("Running time:"+(pause-start)/1000/60+"min "+(pause-start)/1000%60+"sec");
            
            taskMonitor.setPercentCompleted(100);
            success = true;
        } catch (Exception e) {
            taskMonitor.setPercentCompleted(100);
            taskMonitor.setStatus("NOA failed.\n");
            e.printStackTrace();
        }
        success = true;
    }

    public boolean success() {
        return success;
    }

    public void halt() {
    }

    public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
        this.taskMonitor = tm;
    }

    public String getTitle() {
        return new String("Running NOA...");
    }

    public JDialog dialog() {
        return dialog;
    }
}
