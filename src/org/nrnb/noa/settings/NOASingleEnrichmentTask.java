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
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.EdgeAppearanceCalculator;
import cytoscape.visual.LineStyle;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyDependency;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.LinearNumberToColorInterpolator;
import cytoscape.visual.mappings.LinearNumberToNumberInterpolator;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.mappings.PassThroughMapping;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.nrnb.noa.NOA;
import org.nrnb.noa.algorithm.CorrectionMethod;
import org.nrnb.noa.algorithm.EdgeAnnotationMethod;
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
            HashMap<String, Set<String>> goNode4EdgeAlgMap = new HashMap<String, Set<String>>();
            HashMap<String, String> goNodeCountRefMap = new HashMap<String, String>();
            HashMap<String, String> resultMap = new HashMap<String, String>();
            long start=System.currentTimeMillis();
            //if node-base algorithm has been selected.
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
                            } else if(statMethod.equals(NOAStaticValues.STAT_ZScore)) {
                                pvalue = StatMethod.calZScorePValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
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
                    //System.out.println(goNodeCountRefMap);
                    for(Object eachGO : potentialGOList) {
                        if(!eachGO.equals("unassigned")) {
                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                            int valueA = goNodeMap.get(eachGO).size();
                            int valueB = selectedNodesSet.size();
                            int valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                            int valueD = totalNodesInGenome;
                            double pvalue = 0;
                            if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_ZScore)) {
                                pvalue = StatMethod.calZScorePValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
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
            //if edge-base algorithm has been selected.
            } else {
                Set<CyEdge> selectedEdgesSet = Cytoscape.getCurrentNetwork().getSelectedEdges();
                if(!isSubnet)
                    selectedEdgesSet = new HashSet<CyEdge>(Cytoscape.getCurrentNetwork().edgesList());
                //Step 1: retrieve potential GO list base on "Test network"
                taskMonitor.setStatus("Obtaining GO list from test network ......");
                potentialGOList.addAll(NOAUtil.retrieveEdgeAttribute(NOAStaticValues.BP_ATTNAME, selectedEdgesSet, goNodeMap, this.edgeAnnotation));
                potentialGOList.addAll(NOAUtil.retrieveEdgeAttribute(NOAStaticValues.CC_ATTNAME, selectedEdgesSet, goNodeMap, this.edgeAnnotation));
                potentialGOList.addAll(NOAUtil.retrieveEdgeAttribute(NOAStaticValues.MF_ATTNAME, selectedEdgesSet, goNodeMap, this.edgeAnnotation));

                Set<CyNode> selectedNodesSet = Cytoscape.getCurrentNetwork().getSelectedNodes();                
                if(!isSubnet) {
                    selectedNodesSet = new HashSet<CyNode>(Cytoscape.getCurrentNetwork().nodesList());
                } else {
                    for(CyEdge cedge:selectedEdgesSet){
                        selectedNodesSet.add((CyNode)cedge.getSource());
                        selectedNodesSet.add((CyNode)cedge.getTarget());
                    }
                }
                NOAUtil.retrieveNodeAttribute(NOAStaticValues.BP_ATTNAME, selectedNodesSet, goNode4EdgeAlgMap);
                NOAUtil.retrieveNodeAttribute(NOAStaticValues.CC_ATTNAME, selectedNodesSet, goNode4EdgeAlgMap);
                NOAUtil.retrieveNodeAttribute(NOAStaticValues.MF_ATTNAME, selectedNodesSet, goNode4EdgeAlgMap);

                if(isWholeNet) {
                    //Step 2: count no of nodes for "whole net", save annotation mapping file into memory
                    taskMonitor.setStatus("Counting edges for the whole network ......");
                    NOAUtil.retrieveEdgeCountMap(NOAStaticValues.BP_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    NOAUtil.retrieveEdgeCountMap(NOAStaticValues.CC_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    NOAUtil.retrieveEdgeCountMap(NOAStaticValues.MF_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    System.out.println(goNodeCountRefMap);
                    //Step 3: loop based on potential GO list, calculate p-value for each GO
                    for(Object eachGO : potentialGOList) {
                        if(!eachGO.equals("unassigned")) {
                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                            int valueA = goNodeMap.get(eachGO).size();
                            int valueB = selectedEdgesSet.size();
                            int valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                            int valueD = Cytoscape.getCurrentNetwork().getEdgeCount();
                            double pvalue = 0;
                            if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_ZScore)) {
                                pvalue = StatMethod.calZScorePValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
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
                    int numOfNode = Cytoscape.getCurrentNetwork().getNodeCount();
                    int totalEdgesInClique = numOfNode*(numOfNode-1)/2;
//                    NOAUtil.retrieveAllEdgeCountMap(NOAStaticValues.BP_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
//                    NOAUtil.retrieveAllEdgeCountMap(NOAStaticValues.CC_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
//                    NOAUtil.retrieveAllEdgeCountMap(NOAStaticValues.MF_ATTNAME, goNodeCountRefMap, potentialGOList, this.edgeAnnotation);
                    NOAUtil.retrieveAllEdgeCountMap(goNode4EdgeAlgMap, goNodeCountRefMap, potentialGOList, this.edgeAnnotation, numOfNode);
                    //System.out.println("potentialGOList size "+potentialGOList.size());
                    //System.out.println("nodeGO size "+goNode4EdgeAlgMap.size());
                    for(Object eachGO : potentialGOList) {
                        if(!eachGO.equals("unassigned")) {
                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                            int valueA = goNodeMap.get(eachGO).size();
                            int valueB = selectedEdgesSet.size();
                            //System.out.print("Calculating p-value for "+eachGO+" ...... ");
                            int valueC = new Integer(goNodeCountRefMap.get(eachGO).toString()).intValue();
                            int valueD = totalEdgesInClique;
                            double pvalue = 0;
                            if(statMethod.equals(NOAStaticValues.STAT_Hypergeo)) {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_Fisher)) {
                                pvalue = StatMethod.calFisherTestPValue(valueA, valueB, valueC, valueD);
                            } else if(statMethod.equals(NOAStaticValues.STAT_ZScore)) {
                                pvalue = StatMethod.calZScorePValue(valueA, valueB, valueC, valueD);
                            } else {
                                pvalue = StatMethod.calHyperGeoPValue(valueA, valueB, valueC, valueD);
                            }
                            //System.out.println(pvalue);
                            if(pvalue<=this.pvalue)
                                resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                        }
                    }
                    if(corrMethod.equals("none")) {

                    } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                        resultMap = CorrectionMethod.calBenjamCorrection(resultMap, potentialGOList.size(), pvalue);
                    } else {
                        resultMap = CorrectionMethod.calBonferCorrection(resultMap, potentialGOList.size(), pvalue);
                    }

                }
                //Step 1: annotate edges for the "whole network"
                
                //Step 2: retrieve potential GO list base on "Test network"
            }

            Object[][] goPvalueArray = new String[resultMap.size()][7];
            int i = 0;
            int BPcount = 0;
            int CCcount = 0;
            int MFcount = 0;
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass()
                        .getResource(NOAStaticValues.GO_DescFile).openStream()));
                String inputLine=in.readLine();
                while ((inputLine = in.readLine()) != null) {
                    String[] retail = inputLine.split("\t");
                    if(retail.length>=3) {
                        if(resultMap.containsKey(retail[0].trim())) {
                            goPvalueArray[i][0] = retail[0];
                            String[] temp = resultMap.get(retail[0]).toString().split("\t");
                            DecimalFormat df1 = new DecimalFormat("#.####");
                            DecimalFormat df2 = new DecimalFormat("#.####E0");
                            double pvalue = new Double(temp[0]).doubleValue();
                            if(pvalue>0.0001)
                                goPvalueArray[i][2] = df1.format(pvalue);
                            else
                                goPvalueArray[i][2] = df2.format(pvalue);
                            goPvalueArray[i][3] = temp[1];
                            goPvalueArray[i][4] = temp[2];
                            goPvalueArray[i][5] = retail[1];
                            String tempList = goNodeMap.get(retail[0]).toString();
                            goPvalueArray[i][6] = tempList.substring(1, tempList.length()-1).trim();
                            if(retail[2].equals("biological_process")) {
                                goPvalueArray[i][1] = "BP";
                                BPcount++;
                            } else if (retail[2].equals("cellular_component")) {
                                goPvalueArray[i][1] = "CC";
                                CCcount++;
                            } else {
                                goPvalueArray[i][1] = "MF";
                                MFcount++;
                            }
                            i++;
                        }
                    }
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            long pause=System.currentTimeMillis();
            System.out.println("Running time:"+(pause-start)/1000/60+"min "+(pause-start)/1000%60+"sec");
            if(resultMap.size()>0&&goPvalueArray.length>0){
                goPvalueArray = NOAUtil.dataSort(goPvalueArray, 2);
                Object[][] cells = new Object[resultMap.size()][7];
                int BPindex = 0;
                int CCindex = BPcount;
                int MFindex = BPcount+CCcount;
                for(i=0;i<goPvalueArray.length;i++){
                    if(goPvalueArray[i][1].equals("BP")) {
                        cells[BPindex] = goPvalueArray[i];
                        BPindex++;
                    } else if (goPvalueArray[i][1].equals("CC")) {
                        cells[CCindex] = goPvalueArray[i];
                        CCindex++;
                    } else {
                        cells[MFindex] = goPvalueArray[i];
                        MFindex++;
                    }
                }


                CyNetwork net = Cytoscape.getCurrentNetwork();
                buildSubnetworkOverview(net, goPvalueArray, goNodeMap);
                dialog = new SingleOutputDialog(Cytoscape.getDesktop(), false, cells, this.algType);
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
    
    public void findOverlap() {

    }
    public void buildSubnetworkOverview(CyNetwork net, Object[][] cells, HashMap<String, Set<String>> goNodeMap) {
        CyNetwork overview_network = Cytoscape.createNetwork(new int[0], new int[0], "Overview", net);
        CyNetworkView overview_view = Cytoscape.getNetworkView(overview_network.getIdentifier());
        CyAttributes nAttributes = Cytoscape.getNodeAttributes();
        CyAttributes eAttributes = Cytoscape.getEdgeAttributes();
        int cutoff = 100;
        
        for(int i=0;i<cutoff;i++) {
            CyNode cn1 = Cytoscape.getCyNode(cells[i][0].toString(), true);
            List<String> cnList1 = new ArrayList<String>(goNodeMap.get(cells[i][0].toString()));
            if(!overview_network.containsNode(cn1)){
                overview_network.addNode(cn1);
                nAttributes.setAttribute(cn1.getIdentifier(), "canonicalName", cells[i][5].toString());
                nAttributes.setAttribute(cn1.getIdentifier(), "GO type", cells[i][1].toString());
                nAttributes.setAttribute(cn1.getIdentifier(), "P-value", Double.parseDouble(cells[i][2].toString()));
                nAttributes.setAttribute(cn1.getIdentifier(), "LogP", Math.log(Double.parseDouble(cells[i][2].toString())));
                nAttributes.setListAttribute(cn1.getIdentifier(), "evidences", cnList1);
                nAttributes.setAttribute(cn1.getIdentifier(), "subnetworkSize", cnList1.size());
            }
            for(int j=i+1;j<cutoff;j++) {
                CyNode cn2 = Cytoscape.getCyNode(cells[j][0].toString(), true);
                List<String> cnList2 = new ArrayList<String>(goNodeMap.get(cells[j][0].toString()));
                if(!overview_network.containsNode(cn2)) {
                    overview_network.addNode(cn2);
                    nAttributes.setAttribute(cn2.getIdentifier(), "canonicalName", cells[j][5].toString());
                    nAttributes.setAttribute(cn2.getIdentifier(), "GO type", cells[j][1].toString());
                    nAttributes.setAttribute(cn2.getIdentifier(), "P-value", Double.parseDouble(cells[j][2].toString()));
                    nAttributes.setAttribute(cn2.getIdentifier(), "LogP", Math.log(Double.parseDouble(cells[j][2].toString())));
                    nAttributes.setListAttribute(cn2.getIdentifier(), "evidences", cnList2);
                    nAttributes.setAttribute(cn2.getIdentifier(), "subnetworkSize", cnList2.size());
                }
                List<String> degreeOfEdge = EdgeAnnotationMethod.edgeIntersection(cnList1, cnList2);
                if(degreeOfEdge.size()>10) {
                    CyEdge ce = Cytoscape.getCyEdge(cn1, cn2, Semantics.INTERACTION, "subnetworkInteraction", true);
                    if(!overview_network.containsEdge(ce)) {
                        overview_network.addEdge(ce);
                        eAttributes.setAttribute(ce.getIdentifier(), "overlapCount", degreeOfEdge.size());
                        eAttributes.setListAttribute(ce.getIdentifier(), "commonEvidences", degreeOfEdge);
                    }
                }
            }
        }

        VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
        CalculatorCatalog catalog = vmm.getCalculatorCatalog();
        VisualStyle mfStyle;
        try {
            mfStyle = (VisualStyle) vmm.getVisualStyle().clone();
        } catch (CloneNotSupportedException e) {
            mfStyle = new VisualStyle("overview");
        }
        NodeAppearanceCalculator nac = new MFNodeAppearanceCalculator();
        EdgeAppearanceCalculator eac = new MFEdgeAppearanceCalculator();
        mfStyle.getDependency().set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED, false);
        // NODE MAPPINGS
        PassThroughMapping passMappingLabel = new PassThroughMapping("", "canonicalName");
        Calculator labelCalculator = new BasicCalculator("subnetworkSize", passMappingLabel, VisualPropertyType.NODE_LABEL);
        nac.setCalculator(labelCalculator);

        ContinuousMapping contMappingNodeWidth = new ContinuousMapping(5, ObjectMapping.NODE_MAPPING);
        contMappingNodeWidth.setControllingAttributeName("subnetworkSize", overview_view.getNetwork(), false);
        contMappingNodeWidth.setInterpolator(new LinearNumberToNumberInterpolator());
        contMappingNodeWidth.addPoint(5, new BoundaryRangeValues(10, 20, 20));
        contMappingNodeWidth.addPoint(200, new BoundaryRangeValues(80, 80, 100));
        Calculator nodeWidthCalculator = new BasicCalculator("subnetworkSize", contMappingNodeWidth, VisualPropertyType.NODE_WIDTH);
        nac.setCalculator(nodeWidthCalculator);

        ContinuousMapping contMappingNodeHeight = new ContinuousMapping(5, ObjectMapping.NODE_MAPPING);
        contMappingNodeHeight.setControllingAttributeName("subnetworkSize", overview_view.getNetwork(), false);
        contMappingNodeHeight.setInterpolator(new LinearNumberToNumberInterpolator());
        contMappingNodeHeight.addPoint(5, new BoundaryRangeValues(10, 20, 20));
        contMappingNodeHeight.addPoint(200, new BoundaryRangeValues(80, 80, 100));
        Calculator nodeHeightCalculator = new BasicCalculator("subnetworkSize", contMappingNodeHeight, VisualPropertyType.NODE_HEIGHT);
        nac.setCalculator(nodeHeightCalculator);

        ContinuousMapping contMappingNodeColor = new ContinuousMapping(Color.WHITE, ObjectMapping.NODE_MAPPING);
        contMappingNodeColor.setControllingAttributeName("LogP", overview_view.getNetwork(), false);
        contMappingNodeColor.setInterpolator(new LinearNumberToColorInterpolator());
        contMappingNodeColor.addPoint(-20.0, new BoundaryRangeValues(Color.YELLOW, Color.YELLOW, Color.YELLOW));
        contMappingNodeColor.addPoint(-3.0, new BoundaryRangeValues(Color.BLUE, Color.BLUE, Color.BLACK));
        Calculator colorCalculator = new BasicCalculator("LogP", contMappingNodeColor,VisualPropertyType.NODE_FILL_COLOR);
        nac.setCalculator(colorCalculator);
        mfStyle.setNodeAppearanceCalculator(nac);

        // EDGE MAPPINGS
        DiscreteMapping disMappingEdgeColor = new DiscreteMapping(
                        LineStyle.SOLID, "_isEdgeToUnassigned",
                        ObjectMapping.EDGE_MAPPING);
        disMappingEdgeColor.putMapValue(Boolean.TRUE, LineStyle.LONG_DASH);
        disMappingEdgeColor.putMapValue(Boolean.FALSE, LineStyle.SOLID);
        Calculator edgeColorCalculator = new BasicCalculator(
                        "overview", disMappingEdgeColor,
                        VisualPropertyType.EDGE_LINE_STYLE);
        eac.setCalculator(edgeColorCalculator);

        DiscreteMapping disMappingEdgeLineStyle = new DiscreteMapping(
                        Color.blue, "_isEdgeToUnassigned",
                        ObjectMapping.EDGE_MAPPING);
        disMappingEdgeLineStyle.putMapValue(Boolean.TRUE, Color.darkGray);
        disMappingEdgeLineStyle.putMapValue(Boolean.FALSE, Color.blue);
        Calculator edgeLineStyleCalculator = new BasicCalculator(
                        "overview", disMappingEdgeLineStyle,
                        VisualPropertyType.EDGE_COLOR);
        eac.setCalculator(edgeLineStyleCalculator);

        ContinuousMapping contMappingLineWidth = new ContinuousMapping(1, ObjectMapping.EDGE_MAPPING);
        contMappingLineWidth.setControllingAttributeName("overlapCount", overview_view.getNetwork(), false);
        contMappingLineWidth.setInterpolator(new LinearNumberToNumberInterpolator());
        contMappingLineWidth.addPoint(10, new BoundaryRangeValues(0, 0, 1));
        contMappingLineWidth.addPoint(200, new BoundaryRangeValues(40, 40, 60));
        Calculator lineWidthCalculator = new BasicCalculator("overlapCount", contMappingLineWidth, VisualPropertyType.EDGE_LINE_WIDTH);
        eac.setCalculator(lineWidthCalculator);

        mfStyle.setEdgeAppearanceCalculator(eac);

        // set edge opacity
        VisualPropertyType type = VisualPropertyType.EDGE_OPACITY;
        type.setDefault(mfStyle, new Integer(150));
        // set node shape
        type = VisualPropertyType.NODE_SHAPE;
        type.setDefault(mfStyle, NodeShape.ELLIPSE);
        vmm.setNetworkView(overview_view);
        vmm.setVisualStyle(mfStyle);
        Cytoscape.getVisualMappingManager().applyAppearances();
        CyLayoutAlgorithm layout = CyLayouts.getLayout("force-directed");
        layout.doLayout(overview_view);
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
