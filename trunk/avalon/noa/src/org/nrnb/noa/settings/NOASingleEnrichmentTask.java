/*******************************************************************************
 * Copyright 2011 Chao Zhang
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

import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.nrnb.noa.NOA;
import org.nrnb.noa.algorithm.CorrectionMethod;
import org.nrnb.noa.algorithm.StatMethod;
import org.nrnb.noa.result.SingleOutputDialog;
import org.nrnb.noa.utils.NOAStaticValues;
import org.nrnb.noa.utils.NOAUtil;

class NOASingleEnrichmentTask implements Task {
    private TaskMonitor taskMonitor;
    private boolean success;
    private String algType;
    private boolean isSubnet;
    private boolean isWholeNet;
    private String edgeAnnotation;
    private String statMethod;
    private String corrMethod;
    private double pvalue;
    private String speciesGOFile;
    public ArrayList<Object> potentialGOList = new ArrayList();
    private JDialog dialog;
    
    public NOASingleEnrichmentTask(boolean isEdge, boolean isSubnet, 
            boolean isWholeNet, Object edgeAnnotation, Object statMethod,
            Object corrMethod, Object pvalue, String speciesGOFile) {
        if(isEdge)
            this.algType = NOAStaticValues.Algorithm_EDGE;
        else
            this.algType = NOAStaticValues.Algorithm_NODE;
        this.isSubnet = isSubnet;
        this.isWholeNet = isWholeNet;
        this.edgeAnnotation = edgeAnnotation.toString();
        this.statMethod = statMethod.toString();
        this.corrMethod = corrMethod.toString();
        this.pvalue = new Double(pvalue.toString()).doubleValue();
        this.speciesGOFile = speciesGOFile;
    }

    public void run() {
        try {
            taskMonitor.setPercentCompleted(-1);
            HashMap<String, Set<String>> goNodeMap = new HashMap<String, Set<String>>();
            HashMap<String, String> goNodeCountRefMap = new HashMap<String, String>();
            HashMap<String, String> resultMap = new HashMap<String, String>();
            long start=System.currentTimeMillis();
            if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {
                Set<CyNode> selectedNodesSet = Cytoscape.getCurrentNetwork().getSelectedNodes();                
                if(!isSubnet)
                    selectedNodesSet = new HashSet<CyNode>(Cytoscape.getCurrentNetwork().nodesList());
                //Step 1: retrieve potential GO list base on "Test network"
                taskMonitor.setStatus("Obtaining GO list from test network ......");
                potentialGOList.addAll(NOAUtil.retrieveNodeAttribute(NOAStaticValues.BP_ATTNAME, selectedNodesSet, goNodeMap));
                potentialGOList.addAll(NOAUtil.retrieveNodeAttribute(NOAStaticValues.CC_ATTNAME, selectedNodesSet, goNodeMap));
                potentialGOList.addAll(NOAUtil.retrieveNodeAttribute(NOAStaticValues.MF_ATTNAME, selectedNodesSet, goNodeMap));
                
                if(isWholeNet) {
                    //Step 2: count no of nodes for "whole net", save annotation mapping file into memory
                    taskMonitor.setStatus("Counting nodes for the whole network ......");
                    NOAUtil.retrieveNodeCountMap(NOAStaticValues.BP_ATTNAME, goNodeCountRefMap, potentialGOList);
                    NOAUtil.retrieveNodeCountMap(NOAStaticValues.CC_ATTNAME, goNodeCountRefMap, potentialGOList);
                    NOAUtil.retrieveNodeCountMap(NOAStaticValues.MF_ATTNAME, goNodeCountRefMap, potentialGOList);
                    //Step 3: loop based on potential GO list, calculate p-value for each GO                    
                    for(Object eachGO : potentialGOList) {
                        if(!eachGO.equals("unassigned")) {
                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                            int valueA = goNodeMap.get(eachGO).size();
                            int valueB = selectedNodesSet.size();
                            int valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                            int valueD = Cytoscape.getCurrentNetwork().getNodeCount();
                            double pvalue = 0;
                            if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"");
                        }
                    }
                    if(corrMethod.equals("none")) {
                        
                    } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                        resultMap = CorrectionMethod.calBenjamCorrection(resultMap, potentialGOList.size(), pvalue);
                    } else {
                        resultMap = CorrectionMethod.calBonferCorrection(resultMap, potentialGOList.size(), pvalue);
                    }
                    //Print results to table.
                } else {
                    //Step 2: count sub or whole genome
                    taskMonitor.setStatus("Counting nodes for the whole genome......");
                    int totalNodesInGenome = NOAUtil.retrieveAllNodeCountMap(speciesGOFile, goNodeCountRefMap, potentialGOList);
                    System.out.println(goNodeCountRefMap);
                    for(Object eachGO : potentialGOList) {
                        if(!eachGO.equals("unassigned")) {
                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                            int valueA = goNodeMap.get(eachGO).size();
                            int valueB = Cytoscape.getCurrentNetwork().nodesList().size();
                            int valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                            int valueD = totalNodesInGenome;
                            double pvalue = 0;
                            if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"");
                        }
                    }
                    if(corrMethod.equals("none")) {

                    } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                        resultMap = CorrectionMethod.calBenjamCorrection(resultMap, potentialGOList.size(), pvalue);
                    } else {
                        resultMap = CorrectionMethod.calBonferCorrection(resultMap, potentialGOList.size(), pvalue);
                    }
                }                
                //Step 3: loop based on potential GO list, calculate p-value for each GO
                
            } else {
                Set<CyEdge> selectedEdgesSet = Cytoscape.getCurrentNetwork().getSelectedEdges();
                if(!isSubnet)
                    selectedEdgesSet = new HashSet<CyEdge>(Cytoscape.getCurrentNetwork().edgesList());
                //Step 1: retrieve potential GO list base on "Test network"
                taskMonitor.setStatus("Obtaining GO list from test network ......");
                potentialGOList.addAll(NOAUtil.retrieveEdgeAttribute(NOAStaticValues.BP_ATTNAME, selectedEdgesSet, goNodeMap, this.edgeAnnotation));
                System.out.println(potentialGOList.size());
                //System.out.println(goNodeMap);
                potentialGOList.addAll(NOAUtil.retrieveEdgeAttribute(NOAStaticValues.CC_ATTNAME, selectedEdgesSet, goNodeMap, this.edgeAnnotation));
                System.out.println(potentialGOList.size());
                //System.out.println(goNodeMap);
                potentialGOList.addAll(NOAUtil.retrieveEdgeAttribute(NOAStaticValues.MF_ATTNAME, selectedEdgesSet, goNodeMap, this.edgeAnnotation));
                System.out.println(potentialGOList.size());
                //System.out.println(goNodeMap);
                if(isWholeNet) {
                    //Step 2: count no of nodes for "whole net", save annotation mapping file into memory
                    taskMonitor.setStatus("Counting edges for the whole network ......");
                    NOAUtil.retrieveEdgeCountMap(NOAStaticValues.BP_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    NOAUtil.retrieveEdgeCountMap(NOAStaticValues.CC_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    NOAUtil.retrieveEdgeCountMap(NOAStaticValues.MF_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    //Step 3: loop based on potential GO list, calculate p-value for each GO
                    for(Object eachGO : potentialGOList) {
                        if(!eachGO.equals("unassigned")) {
                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                            int valueA = goNodeMap.get(eachGO).size();
                            int valueB = selectedEdgesSet.size();
                            int valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                            int valueD = Cytoscape.getCurrentNetwork().edgesList().size();
                            double pvalue = 0;
                            if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"");
                        }
                    }
                    if(corrMethod.equals("none")) {

                    } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                        resultMap = CorrectionMethod.calBenjamCorrection(resultMap, potentialGOList.size(), pvalue);
                    } else {
                        resultMap = CorrectionMethod.calBonferCorrection(resultMap, potentialGOList.size(), pvalue);
                    }
                    //Print results to table.
                } else {
                    //Step 2: number of edge for the whole clique
                    taskMonitor.setStatus("Counting edges for the whole clique......");
                    int numOfEdge = Cytoscape.getCurrentNetwork().getEdgeCount();
                    int totalEdgesInClique = numOfEdge*(numOfEdge-1)/2;
                    System.out.println(goNodeCountRefMap);
                }
                //Step 1: annotate edges for the "whole network"
                
                //Step 2: retrieve potential GO list base on "Test network"
            }
            
            long pause=System.currentTimeMillis();
            System.out.println("Running time:"+(pause-start)/1000/60+"min "+(pause-start)/1000%60+"sec");
            if(resultMap.size()>0){
                dialog = new SingleOutputDialog(Cytoscape.getDesktop(), false, goNodeMap, resultMap, this.algType);
                dialog.setLocationRelativeTo(Cytoscape.getDesktop());
                dialog.setResizable(true);
            } else {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "No result for selected criteria!", NOA.pluginName,
                        JOptionPane.WARNING_MESSAGE);
            }
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
