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
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
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
    public ArrayList<Object> potentialGOList = new ArrayList();
    private JDialog dialog;
    
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
            HashMap<String, String> outputMap = new HashMap<String, String>();

            Set<String> allNodeSet = new HashSet();
            Set<String> allEdgeSet = new HashSet();
            int formatSign = 0;
            String oneSeq = "";
            ArrayList seq = new ArrayList();
            HashMap<String, ArrayList> tempDataArray = new HashMap<String, ArrayList>();
            long start=System.currentTimeMillis();
            //1st step - check file format and get the list of all nodes
            try {
                BufferedReader in = new BufferedReader(new FileReader(inputFilePath));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.indexOf(">")!=-1) {
                        if(oneSeq.equals("")) {
                            oneSeq = inputLine.trim();
                        } else {
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
                        if(temp.length<2) {
                            formatSign = -1;
                            break;
                        } else {
                            allNodeSet.add(temp[0].trim());
                            allNodeSet.add(temp[1].trim());
                            if(this.algType.equals(NOAStaticValues.Algorithm_EDGE)) {
                                if(!(allEdgeSet.contains(temp[0]+"\t"+temp[1])||allEdgeSet.contains(temp[1]+"\t"+temp[0])))
                                    allEdgeSet.add(temp[0]+"\t"+temp[1]);
                            }
                        }
                    }
                }
                if(!oneSeq.equals(""))
                    tempDataArray.put(oneSeq, seq);
                in.close();
            } catch (Exception e) {
                formatSign = -1;
                e.printStackTrace();
            }
            //2nd step - annotate all nodes
            if(formatSign == -1) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "The file format is invalid, please check user manual for the detail.", NOA.pluginName,
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
                //if node-base algorithm has been selected.
                if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {                    
                    taskMonitor.setStatus("Counting nodes for the whole network ......");
                    NOAUtil.retrieveNodeCountMapBatchMode(idGOMapArray, allNodeSet, goNodeRefMap, potentialGOList);
                    if(isWholeNet) {
                        valueD = allNodeSet.size();                        
                    } else {
                        valueD = NOAUtil.retrieveAllNodeCountMap(speciesGOFile, goNodeCountRefMap, potentialGOList);
                    }
                    for(String networkID : networkList) {
                        resultMap = new HashMap<String, String>();
                        ArrayList detail = tempDataArray.get(networkID);
                        Set<String> testNodeSet = new HashSet();
                        for(Object line : detail) {
                            String[] temp = line.toString().split("\t");
                            if(temp.length>=2){
                                testNodeSet.add(temp[0]);
                                testNodeSet.add(temp[1]);
                            }
                        }
                        HashMap<String, Set<String>> goNodeMap = new HashMap<String, Set<String>>();
                        NOAUtil.retrieveNodeCountMapBatchMode(idGOMapArray, testNodeSet, goNodeMap, potentialGOList);
                        valueB = testNodeSet.size();
                        Object topGOID = "";
                        double topPvalue = 100;
                        for(Object eachGO : potentialGOList) {                            
                            if(!eachGO.equals("unassigned")) {
                                if(goNodeMap.containsKey(eachGO)) {
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
                        if(corrMethod.equals("none")) {

                        } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                            resultMap = CorrectionMethod.calBenjamCorrection(resultMap, resultMap.size(), pvalue);
                        } else {
                            resultMap = CorrectionMethod.calBonferCorrection(resultMap, resultMap.size(), pvalue);
                        }
                        for(Object eachGO : potentialGOList) {
                            if(resultMap.containsKey(eachGO))
                                outputMap.put(eachGO.toString(), resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length()));
                        }
                    }
                //if edge-base algorithm has been selected.
                } else {
                    System.out.println("Counting edges for the whole clique......");
                    taskMonitor.setStatus("Counting edges for the whole clique......");
                    NOAUtil.retrieveEdgeCountMapBatchMode(idGOMapArray, allEdgeSet, goNodeRefMap, potentialGOList, this.edgeAnnotation);
                    for(String networkID : networkList) {
                        System.out.println(networkID);
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
                                    System.out.println(eachGO+": "+pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
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
                        if(corrMethod.equals("none")) {

                        } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                            resultMap = CorrectionMethod.calBenjamCorrection(resultMap, resultMap.size(), pvalue);
                        } else {
                            resultMap = CorrectionMethod.calBonferCorrection(resultMap, resultMap.size(), pvalue);
                        }
                        for(Object eachGO : potentialGOList) {
                            if(resultMap.containsKey(eachGO))
                                outputMap.put(eachGO.toString(), resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length()));
                        }
                    }
                }
                if(outputMap.size()>0){
                    dialog = new MultipleOutputDialog(Cytoscape.getDesktop(), false, goNodeRefMap, outputMap, this.algType);
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
