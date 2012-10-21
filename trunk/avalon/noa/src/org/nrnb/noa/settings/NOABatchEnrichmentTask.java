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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.nrnb.noa.algorithm.ChiSquareDist;
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
    public List allPotentialGOList = new ArrayList();
    private JDialog dialog;
    private int formatSign = 0;
    private boolean isSortedNetwork = true;
    private List<String> networkNameArray = new ArrayList<String>();
    private String tempHeatmapFileName = "";
    private int networkSize = 100;
    private int goSize = 100;
    private final int TOTAL_GO = 1000;
    private int numGOEachNet = 1000;
    
    public NOABatchEnrichmentTask(boolean isEdge, String inputFilePath,
            boolean isWholeNet, Object edgeAnnotation, Object statMethod,
            Object corrMethod, Object pvalue, String speciesDerbyFile,
            String speciesGOFile, Object idType, String ensemblType, int formatSign,
            boolean isSortedNetwork) {
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
        this.formatSign = formatSign;
        this.isSortedNetwork = isSortedNetwork;
    }

    public void run() {
        try {            
            taskMonitor.setPercentCompleted(-1);

            Set<String> allNodeSet = new HashSet();
            Set<String> allEdgeSet = new HashSet();
            HashMap<String, ArrayList> networkDataMap = new HashMap<String, ArrayList>();
            HashMap<String, Set<String>> goNodeRefMap4AllNet = new HashMap<String, Set<String>>();
            HashMap<String, String> goNodeCountMap4WholeGenome = new HashMap<String, String>();
            
            HashMap<String, String> resultMap = new HashMap<String, String>();
            HashMap<String, ArrayList<String>> outputMap = new HashMap<String, ArrayList<String>>();
            HashMap<String, ArrayList<String>> allOutputMap = new HashMap<String, ArrayList<String>>();
            HashMap<String, String> outputTopMap = new HashMap<String, String>();
            
            formatSign = 0;
            String networkTitle = "";
            ArrayList tmpNetworkData = new ArrayList();
            tempHeatmapFileName = System.currentTimeMillis()+"";            

            long start=System.currentTimeMillis();

            //1st step - check file format and get the list of all nodes
            //If file contains more than 3 columns for each network/set, it will
            //be detected as "Wrong input format"
            try {
                BufferedReader in = new BufferedReader(new FileReader(inputFilePath));
                String inputLine = in.readLine();
                inputLine = in.readLine();
                while((inputLine.indexOf(">")!=-1)||(inputLine.trim().equals("")
                        ||inputLine.equals(null))) {
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
            //Generate non-redundant node and edge list
            if(formatSign != NOAStaticValues.WRONG_FORMAT) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(inputFilePath));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if(inputLine.indexOf(">")!=-1) {
                            if(networkTitle.equals("")) {
                                networkTitle = inputLine.trim();
                            } else {
                                networkNameArray.add(networkTitle);
                                networkDataMap.put(networkTitle, tmpNetworkData);
                                tmpNetworkData = new ArrayList();
                                networkTitle = inputLine.trim();
                            }
                        } else if (inputLine.trim().equals("")||inputLine.equals(null)) {
                            networkDataMap.put(networkTitle, tmpNetworkData);
                            tmpNetworkData = new ArrayList();
                            networkTitle = "";
                        } else {
                            tmpNetworkData.add(inputLine.trim());
                            String[] temp = inputLine.split("\t");
                            if(formatSign == NOAStaticValues.NETWORK_FORMAT) {
                                if(temp.length<2) {
                                    formatSign = NOAStaticValues.WRONG_FORMAT;
                                    break;
                                } else {
                                    //ignore self-connected edges
                                    if(!temp[0].trim().equals(temp[1].trim())) {
                                        allNodeSet.add(temp[0].trim());
                                        allNodeSet.add(temp[1].trim());
                                        if(this.algType.equals(NOAStaticValues.Algorithm_EDGE)) {
                                            //ignore duplicated edges
                                            if(!(allEdgeSet.contains(temp[0]+"\t"+temp[1])
                                                    ||allEdgeSet.contains(temp[1]+"\t"+temp[0])))
                                                allEdgeSet.add(temp[0]+"\t"+temp[1]);
                                        }
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
                    if(!networkTitle.equals("")){
                        networkDataMap.put(networkTitle, tmpNetworkData);
                        networkNameArray.add(networkTitle);
                    }
                    in.close();
                } catch (Exception e) {
                    formatSign = NOAStaticValues.WRONG_FORMAT;
                    e.printStackTrace();
                }
            }
            if(networkNameArray.size()==0)
                formatSign = NOAStaticValues.WRONG_FORMAT;
            //It will stop if format is not right.
            if(formatSign == NOAStaticValues.WRONG_FORMAT) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "The file format is invalid, please check user manual for the detail.", NOA.pluginName,
                    JOptionPane.WARNING_MESSAGE);
            } else if((formatSign == NOAStaticValues.SET_FORMAT)&&(this.algType.equals(NOAStaticValues.Algorithm_EDGE))){
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "Edge-based algorithm cannot be applied to gene sets, please choose Node-based algorithm.",
                    NOA.pluginName, JOptionPane.WARNING_MESSAGE);
            } else {
                //2nd step
                //annotate all nodes
                List<String> nodeList = new Vector(allNodeSet);
                IdMapping idMapper = new IdMapping();
                taskMonitor.setStatus("Obtaining GO list from test networks ......");
                Map<String, Set<String>>[] idGOMapArray = idMapper.mapID2Array(
                    this.speciesDerbyFile, this.speciesGOFile, nodeList,
                    this.idType.toString(), this.ensemblIDType);                
//                System.out.println(idGOMapArray[0]);
//                System.out.println(idGOMapArray[1]);
//                System.out.println(idGOMapArray[2]);
                List<String>[] GOList = new ArrayList[3];
                GOList[0] = new ArrayList();
//                GOList[0].add("GO:0050789");
                GOList[0].addAll(idMapper.convertSetMapValueToSet(idGOMapArray[0]));
                GOList[0].remove("unassigned");
                allPotentialGOList.addAll(GOList[0]);
                GOList[1] = new ArrayList();
//                GOList[1].add("GO:0005634");
                GOList[1].addAll(idMapper.convertSetMapValueToSet(idGOMapArray[1]));
                GOList[1].remove("unassigned");
                allPotentialGOList.addAll(GOList[1]);
                GOList[2] = new ArrayList();
//                GOList[2].add("GO:0003676");
                GOList[2].addAll(idMapper.convertSetMapValueToSet(idGOMapArray[2]));
                GOList[2].remove("unassigned");
                allPotentialGOList.addAll(GOList[2]);
                //Decide the number of GO for each network for display
                numGOEachNet = TOTAL_GO/networkNameArray.size();
                //initialize all pvalue array
                int maxGOCount = Math.max(GOList[0].size(), Math.max(GOList[1].size(), GOList[2].size()));
                double[][][] sumPvaluePerGO = new double[3][maxGOCount][2];
                double[][][] sumPvaluePerNetwork = new double[3][networkNameArray.size()][2];
                double[][][] pvalueMatrix = new double[3][networkNameArray.size()+1][maxGOCount];
                for(int i=0;i<pvalueMatrix.length;i++) {
                    for(int j=0;j<pvalueMatrix[0].length;j++) {
                        for(int k=0;k<pvalueMatrix[0][0].length;k++) {
                            pvalueMatrix[i][j][k] = 0;
                        }
                    }
                }

                int valueA = 0;
                int valueB = 0;
                int valueC = 0;
                int valueD = 0;
                int recordCount = 0;
                goNodeRefMap4AllNet = new HashMap<String, Set<String>>();

                //If all nodes don't have any annotations, stop!
                if(allPotentialGOList.size()<=1) {
                     JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "Failed to retrieve GO annotations. Please verify the type of identifier and try again.", 
                        NOA.pluginName, JOptionPane.WARNING_MESSAGE);
                } else {
                    //Node-base algorithm
                    if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {
                        taskMonitor.setStatus("Counting nodes for the whole network ......");
                        NOAUtil.retrieveNodeCountMapBatchMode(idGOMapArray, allNodeSet,
                                goNodeRefMap4AllNet, allPotentialGOList);
                        if(isWholeNet) {
                            valueD = allNodeSet.size();
                        } else {
                            valueD = NOAUtil.retrieveAllNodeCountMap(speciesGOFile,
                                    goNodeCountMap4WholeGenome, allPotentialGOList);
                        }
                        start=System.currentTimeMillis();
                        //Calculate p-value for each network
                        for(String networkID : networkNameArray) {
                            System.out.println("***************"+networkID+"***********");                            
                            resultMap = new HashMap<String, String>();
                            ArrayList detail = networkDataMap.get(networkID);
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
                            NOAUtil.retrieveNodeCountMapBatchMode(idGOMapArray, testNodeSet,
                                    goNodeMap, allPotentialGOList);
                            int m = networkNameArray.indexOf(networkID);
                            sumPvaluePerNetwork[0][m][0] = m;
                            sumPvaluePerNetwork[1][m][0] = m;
                            sumPvaluePerNetwork[2][m][0] = m;
                            if(goNodeMap.size()>0) {
                                System.out.println("goNodeRefMap4AllNet: "+goNodeRefMap4AllNet.size());
                                System.out.println("allNodeSet: "+allNodeSet.size());
                                System.out.println("goNodeMap: "+goNodeMap.size());
                                System.out.println("testNodeSet: "+testNodeSet.size());
                                System.out.println("potentialGOList: "+allPotentialGOList.size());
                                valueB = testNodeSet.size();
                                Object[][] goPvalue4SingleNetwork = new String[allPotentialGOList.size()][2];
                                for(int x=0;x<goPvalue4SingleNetwork.length;x++) {
                                    goPvalue4SingleNetwork[x][0] = "50000";
                                    goPvalue4SingleNetwork[x][1] = "NA";
                                }
                                Object topGOID = "";
                                double topPvalue = 100;


                                for(int i=0;i<GOList.length;i++) {
                                    int countGO = 0;
                                    for(Object eachGO : GOList[i]) {
                                        int n = GOList[i].indexOf(eachGO);
                                        sumPvaluePerGO[i][n][0] = n;
                                        if(!eachGO.equals("unassigned")&&(goNodeMap.containsKey(eachGO))) {
                                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                                            valueA = goNodeMap.get(eachGO).size();
                                            if(isWholeNet) {
                                                valueC = goNodeRefMap4AllNet.get(eachGO).size();
                                            } else {
                                                valueC = new Integer(goNodeCountMap4WholeGenome.get(eachGO)
                                                        .toString()).intValue();
                                            }
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
                                            if(m!=-1&&n!=-1) {
                                                if(pvalue!=0)
                                                    pvalueMatrix[i][m][n] = Math.log(pvalue);
                                                else
                                                    pvalueMatrix[i][m][n] = -1000;
                                            }
                                            if(pvalue<=this.pvalue) {
                                                resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                                                goPvalue4SingleNetwork[countGO][0] = pvalue+"";
                                                goPvalue4SingleNetwork[countGO][1] = eachGO.toString();
                                            } else {
                                                goPvalue4SingleNetwork[countGO][0] = "10000";
                                                goPvalue4SingleNetwork[countGO][1] = eachGO.toString();
                                            }
                                        } else {
                                            goPvalue4SingleNetwork[countGO][0] = "10000";
                                            goPvalue4SingleNetwork[countGO][1] = eachGO.toString();
                                        }
                                        countGO++;
                                    }
                                    if(corrMethod.equals("none")) {

                                    } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                                        pvalueMatrix[i][m] = CorrectionMethod.calBenjamCorrection(pvalueMatrix[i][m], goNodeMap.size());
                                    } else {
                                        pvalueMatrix[i][m] = CorrectionMethod.calBonferCorrection(pvalueMatrix[i][m], goNodeMap.size());
                                    }
                                    for(int j=0;j<pvalueMatrix[i][m].length;j++){
                                        pvalueMatrix[i][networkNameArray.size()][j] += -2.0*pvalueMatrix[i][m][j];
                                        sumPvaluePerGO[i][j][1] += -2.0*pvalueMatrix[i][m][j];
                                        sumPvaluePerNetwork[i][m][1] += -2.0*pvalueMatrix[i][m][j];
                                    }
                                }
                                taskMonitor.setStatus("Calculating corrected p-value ......");
                                if(corrMethod.equals("none")) {

                                } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                                    resultMap = CorrectionMethod.calBenjamCorrection(resultMap,
                                            resultMap.size(), pvalue);
                                } else {
                                    resultMap = CorrectionMethod.calBonferCorrection(resultMap,
                                            resultMap.size(), pvalue);
                                }
                                goPvalue4SingleNetwork = NOAUtil.dataSort(goPvalue4SingleNetwork, 0);
                                topGOID = goPvalue4SingleNetwork[0][1];
                                if(resultMap.containsKey(topGOID))
                                    outputTopMap.put(topGOID.toString()+"\t"+networkID.substring(1,networkID.length()),
                                            resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length())
                                            +"\t"+goNodeMap.get(topGOID));
                                int numGOLimit = numGOEachNet>goPvalue4SingleNetwork.length?goPvalue4SingleNetwork.length:numGOEachNet;
                                for(int i=0; i<numGOLimit; i++) {
                                    if(goPvalue4SingleNetwork[i][0]!="10000") {
                                        Object eachGO = goPvalue4SingleNetwork[i][1];
                                        if(resultMap.containsKey(eachGO)) {
                                            if(outputMap.containsKey(eachGO)) {
                                                ArrayList<String> resultWithNetworkID = outputMap.get(eachGO);
                                                resultWithNetworkID.add(resultMap.get(eachGO).toString()
                                                        +"\t"+networkID.substring(1,networkID.length())
                                                        +"\t"+goNodeMap.get(eachGO));
                                                outputMap.put(eachGO.toString(), resultWithNetworkID);
                                            } else {
                                                ArrayList<String> resultWithNetworkID = new ArrayList<String>();
                                                resultWithNetworkID.add(resultMap.get(eachGO).toString()
                                                        +"\t"+networkID.substring(1,networkID.length())
                                                        +"\t"+goNodeMap.get(eachGO));
                                                outputMap.put(eachGO.toString(), resultWithNetworkID);
                                            }
                                            recordCount++;
                                        }
                                    }
                                }
                                for(Object eachGO : allPotentialGOList) {
                                    if(resultMap.containsKey(eachGO)) {
                                        if(allOutputMap.containsKey(eachGO)) {
                                            ArrayList<String> resultWithNetworkID = allOutputMap.get(eachGO);
                                            resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"
                                                    +networkID.substring(1,networkID.length())
                                                    +"\t"+goNodeMap.get(eachGO));
                                            allOutputMap.put(eachGO.toString(), resultWithNetworkID);
                                        } else {
                                            ArrayList<String> resultWithNetworkID = new ArrayList<String>();
                                            resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"
                                                    +networkID.substring(1,networkID.length())
                                                    +"\t"+goNodeMap.get(eachGO));
                                            allOutputMap.put(eachGO.toString(), resultWithNetworkID);
                                        }
                                    }
                                }
                            }
                        }
                        long end=System.currentTimeMillis();
                        System.out.println("Running time:"+(end-start)/1000/60+"min "+(end-start)/1000%60+"sec");
                    //Edge-base algorithm.
                    } else {
                        System.out.println("Counting edges for the whole clique......");
                        taskMonitor.setStatus("Counting edges for the whole clique......");
                        NOAUtil.retrieveEdgeCountMapBatchMode(idGOMapArray, allEdgeSet,
                                goNodeRefMap4AllNet, allPotentialGOList, this.edgeAnnotation);

                        //Calculate p-value for each network
                        for(String networkID : networkNameArray) {
                            System.out.println("***************"+networkID+"***********");
                            resultMap = new HashMap<String, String>();
                            //Object[][] goPvalue4SingleNetwork = new String[go200List.size()][2];
                            ArrayList detail = networkDataMap.get(networkID);
                            Set<String> testEdgeSet = new HashSet();
                            Set<String> testNodeSet = new HashSet();
                            for(Object line : detail) {
                                String[] temp = line.toString().split("\t");
                                if(temp.length>=2){
                                    if(!temp[0].trim().equals(temp[1].trim())) {
                                        testNodeSet.add(temp[0]);
                                        testNodeSet.add(temp[1]);
                                        if(!(testEdgeSet.contains(temp[0]+"\t"+temp[1])||
                                                testEdgeSet.contains(temp[1]+"\t"+temp[0])))
                                            testEdgeSet.add(temp[0]+"\t"+temp[1]);
                                    }
                                }
                            }
                            HashMap<String, Set<String>> goNodeMap = new HashMap<String, Set<String>>();
                            goNodeCountMap4WholeGenome = new HashMap<String, String>();
                            NOAUtil.retrieveEdgeCountMapBatchMode(idGOMapArray, testEdgeSet,
                                    goNodeMap, allPotentialGOList, this.edgeAnnotation);
                            int m = networkNameArray.indexOf(networkID);
                            sumPvaluePerNetwork[0][m][0] = m;
                            sumPvaluePerNetwork[1][m][0] = m;
                            sumPvaluePerNetwork[2][m][0] = m;
                            if(goNodeMap.size()>0) {
                                System.out.println("goNodeRefMap4AllNet: "+goNodeRefMap4AllNet.size());
                                System.out.println("allEdgeSet: "+allEdgeSet.size());
                                System.out.println("goNodeMap: "+goNodeMap.size());
                                System.out.println("testEdgeSet: "+testEdgeSet.size());
                                System.out.println("potentialGOList: "+allPotentialGOList.size());
                                valueB = testEdgeSet.size();
                                if(isWholeNet) {
                                    valueD = allEdgeSet.size();
                                } else {
                                    valueD = testNodeSet.size()*(testNodeSet.size()-1)/2;
                                    NOAUtil.retrieveAllEdgeCountMapBatchMode(idGOMapArray, testNodeSet,
                                            goNodeCountMap4WholeGenome, allPotentialGOList, this.edgeAnnotation);
                                    //System.out.println(goNodeCountRefMap.size());
                                }

                                Object[][] goPvalue4SingleNetwork = new String[allPotentialGOList.size()][2];
                                for(int x=0;x<goPvalue4SingleNetwork.length;x++) {
                                    goPvalue4SingleNetwork[x][0] = "50000";
                                    goPvalue4SingleNetwork[x][1] = "NA";
                                }
                                Object topGOID = "";
                                double topPvalue = 100;

                                for(int i=0;i<GOList.length;i++) {
                                    int countGO = 0;
                                    for(Object eachGO : GOList[i]) {
                                        int n = GOList[i].indexOf(eachGO);
                                        sumPvaluePerGO[i][n][0] = n;
                                        if(!eachGO.equals("unassigned")&&(goNodeMap.containsKey(eachGO))) {
                                            taskMonitor.setStatus("Calculating p-value for "+eachGO+" ......");
                                            valueA = goNodeMap.get(eachGO).size();
                                            if(isWholeNet) {
                                                valueC = goNodeRefMap4AllNet.get(eachGO).size();
                                            } else {
                                                valueC = new Integer(goNodeCountMap4WholeGenome.get(eachGO)
                                                        .toString()).intValue();
                                            }
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
                                            if(m!=-1&&n!=-1) {
                                                if(pvalue!=0)
                                                    pvalueMatrix[i][m][n] = Math.log(pvalue);
                                                else
                                                    pvalueMatrix[i][m][n] = -1000;
                                            }
                                            //System.out.println(eachGO+"\t"+pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                                            if(pvalue<=this.pvalue) {
                                                resultMap.put(eachGO.toString(), pvalue+"\t"+valueA+"/"+valueB+"\t"+valueC+"/"+valueD);
                                                goPvalue4SingleNetwork[countGO][0] = pvalue+"";
                                                goPvalue4SingleNetwork[countGO][1] = eachGO.toString();
                                            } else {
                                                goPvalue4SingleNetwork[countGO][0] = "10000";
                                                goPvalue4SingleNetwork[countGO][1] = eachGO.toString();
                                            }
                                        } else {
                                            goPvalue4SingleNetwork[countGO][0] = "10000";
                                            goPvalue4SingleNetwork[countGO][1] = eachGO.toString();
                                        }
                                        countGO++;
                                    }
                                    if(corrMethod.equals("none")) {

                                    } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                                        pvalueMatrix[i][m] = CorrectionMethod.calBenjamCorrection(pvalueMatrix[i][m], goNodeMap.size());
                                    } else {
                                        pvalueMatrix[i][m] = CorrectionMethod.calBonferCorrection(pvalueMatrix[i][m], goNodeMap.size());
                                    }
                                    for(int j=0;j<pvalueMatrix[i][m].length;j++){
                                        //System.out.println(GOList[i].get(j)+"\t"+pvalueMatrix[i][m][j]);
                                        //pvalue*total>1?1:pvalue*total;
                                        pvalueMatrix[i][networkNameArray.size()][j] += -2.0*pvalueMatrix[i][m][j];
                                        sumPvaluePerGO[i][j][1] += -2.0*pvalueMatrix[i][m][j];
                                        sumPvaluePerNetwork[i][m][1] += -2.0*pvalueMatrix[i][m][j];
                                    }
                                }
                                taskMonitor.setStatus("Calculating corrected p-value ......");
                                if(corrMethod.equals("none")) {

                                } else if(corrMethod.equals(NOAStaticValues.CORRECTION_Benjam)) {
                                    resultMap = CorrectionMethod.calBenjamCorrection(resultMap, goNodeMap.size(), pvalue);
                                    //pvalueMatrix[i][m] = CorrectionMethod.calBenjamCorrection(pvalueMatrix[i][m], goNodeMap.size(), pvalue);
                                } else {
                                    resultMap = CorrectionMethod.calBonferCorrection(resultMap, resultMap.size(), pvalue);
                                }
                                goPvalue4SingleNetwork = NOAUtil.dataSort(goPvalue4SingleNetwork, 0);
                                topGOID = goPvalue4SingleNetwork[0][1];
                                if(resultMap.containsKey(topGOID))
                                    outputTopMap.put(topGOID.toString()+"\t"+networkID.substring(1,networkID.length()),
                                            resultMap.get(topGOID).toString()+"\t"+networkID.substring(1,networkID.length())+
                                            "\t"+goNodeMap.get(topGOID));
                                int numGOLimit = numGOEachNet>goPvalue4SingleNetwork.length?goPvalue4SingleNetwork.length:numGOEachNet;
                                for(int i=0; i<numGOLimit; i++) {
                                    if(goPvalue4SingleNetwork[i][0]!="10000") {
                                        Object eachGO = goPvalue4SingleNetwork[i][1];
                                        if(resultMap.containsKey(eachGO)) {
                                            if(outputMap.containsKey(eachGO)) {
                                                ArrayList<String> resultWithNetworkID = outputMap.get(eachGO);
                                                resultWithNetworkID.add(resultMap.get(eachGO).toString()
                                                        +"\t"+networkID.substring(1,networkID.length())
                                                        +"\t"+goNodeMap.get(eachGO));
                                                outputMap.put(eachGO.toString(), resultWithNetworkID);
                                            } else {
                                                ArrayList<String> resultWithNetworkID = new ArrayList<String>();
                                                resultWithNetworkID.add(resultMap.get(eachGO).toString()
                                                        +"\t"+networkID.substring(1,networkID.length())
                                                        +"\t"+goNodeMap.get(eachGO));
                                                outputMap.put(eachGO.toString(), resultWithNetworkID);
                                            }
                                            recordCount++;
                                        }
                                    }
                                }
                                for(Object eachGO : allPotentialGOList) {
                                    if(resultMap.containsKey(eachGO)) {
                                        if(allOutputMap.containsKey(eachGO)) {
                                            ArrayList<String> resultWithNetworkID = allOutputMap.get(eachGO);
                                            resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"+networkID.substring(1,networkID.length())
                                                    +"\t"+goNodeMap.get(eachGO));
                                            allOutputMap.put(eachGO.toString(), resultWithNetworkID);
                                        } else {
                                            ArrayList<String> resultWithNetworkID = new ArrayList<String>();
                                            resultWithNetworkID.add(resultMap.get(eachGO).toString()+"\t"+networkID.substring(1,networkID.length())
                                                    +"\t"+goNodeMap.get(eachGO));
                                            allOutputMap.put(eachGO.toString(), resultWithNetworkID);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //End of algorithm
                 
                    goNodeRefMap4AllNet.clear();
                    if(!isWholeNet)
                        goNodeCountMap4WholeGenome.clear();
                    //System.gc();
                    Map<String, String> goDescMap = NOAUtil.readMappingFile(this.getClass().getResource(NOAStaticValues.GO_DescFile), allPotentialGOList, 0);
                    //Common code for both algorithms
                    taskMonitor.setStatus("Generating heatmap ......");
                    for(int k=0;k<GOList.length;k++) {
                        start=System.currentTimeMillis();
                        String goBranch = "";
                        if(k==0)
                            goBranch = "BP";
                        else if(k==1)
                            goBranch = "CC";
                        else
                            goBranch = "MF";
                        //Convert Chi^2 values of each GO from Fisher's exact test to P-value
                        int countPvalue = 0;
                        goSize = 100;
                        networkSize = 100;
                        for(int i=0;i<maxGOCount;i++) {
                            pvalueMatrix[k][networkNameArray.size()][i] = 1.0-ChiSquareDist.chiSquareCDF(
                                    pvalueMatrix[k][networkNameArray.size()][i], networkNameArray.size()*2);
                            if(pvalueMatrix[k][networkNameArray.size()][i]<=0.05){
                                countPvalue++;
                            }
                        }
                        //Check number of networks and number of GO terms
                        if(networkNameArray.size()<networkSize)
                            networkSize = networkNameArray.size();
                        if(countPvalue<goSize)
                            goSize = countPvalue;
                        if(goSize>0) {
                            double[][] heatmapPvalueMatrix = new double[networkSize][goSize];
                            sumPvaluePerGO[k] = NOAUtil.dataSort(sumPvaluePerGO[k], 1 ,1);

                            if(this.isSortedNetwork)
                                sumPvaluePerNetwork[k] = NOAUtil.dataSort(sumPvaluePerNetwork[k], 1, 1);


                            List heatmapGOList = new ArrayList();
                            List heatmapNetworkList = new ArrayList();
                            countPvalue = 0;
                            for(int i=0;i<GOList[k].size();i++) {
                                int idxOfGO = (int)sumPvaluePerGO[k][i][0];
                                if(pvalueMatrix[k][networkNameArray.size()][idxOfGO]<=0.05){
                                    heatmapGOList.add(GOList[k].get(idxOfGO));
                                    int m=0;
                                    int j=0;
                                    while(j<networkSize&&m<networkNameArray.size()) {
                                        int idxOfNetwrok = (int)sumPvaluePerNetwork[k][m][0];
                                        if(sumPvaluePerNetwork[k][m][1]>0) {
                                            String netID = networkNameArray.get(idxOfNetwrok).toString();
                                            netID = netID.substring(1, netID.length());
                                            if(heatmapNetworkList.indexOf(netID)==-1)
                                                heatmapNetworkList.add(netID);
                                            if(pvalueMatrix[k][idxOfNetwrok][idxOfGO]<NOAStaticValues.LOG_PVALUE_CUTOFF)
                                                heatmapPvalueMatrix[j][countPvalue] = NOAStaticValues.LOG_PVALUE_CUTOFF;
                                            else
                                                heatmapPvalueMatrix[j][countPvalue] = pvalueMatrix[k][idxOfNetwrok][idxOfGO];
                                            j++;
                                        }
                                        m++;
                                    }
                                    countPvalue++;
                                    if(countPvalue>=goSize)
                                        break;
                                }
                            }

                            String writeString = "\t,\"";
                            String titleString = "\t,\"";
                            for(int i=0;i<GOList[k].size();i++) {
                                int idxOfGO = (int)sumPvaluePerGO[k][i][0];
                                writeString += GOList[k].get(idxOfGO)+"\",\"";
                                titleString += goDescMap.get(GOList[k].get(idxOfGO))+"\",\"";
                            }
                            writeString += "\"";
                            titleString += "\"";
                            String heatmapCSVFilePath = NOA.NOATempDir+tempHeatmapFileName+"_"+goBranch+".csv";
                            try {
                                FileWriter writer = new FileWriter(heatmapCSVFilePath);
                                BufferedWriter bufWriter = new BufferedWriter(writer);
                                bufWriter.write(titleString);
                                bufWriter.newLine();
                                bufWriter.write(writeString);
                                bufWriter.newLine();
                                for(int j=0;j<networkNameArray.size();j++) {
                                    int idxOfNetwrok = (int)sumPvaluePerNetwork[k][j][0];
                                    writeString = "\""+networkNameArray.get(idxOfNetwrok).toString()+"\",";
                                    for(int i=0;i<GOList[k].size();i++) {
                                        int idxOfGO = (int)sumPvaluePerGO[k][i][0];
                                        writeString += pvalueMatrix[k][idxOfNetwrok][idxOfGO]+",";
                                    }
                                    bufWriter.write(writeString);
                                    bufWriter.newLine();
                                }
                                bufWriter.close();
                                writer.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(heatmapNetworkList.size()<networkSize)
                                networkSize = heatmapNetworkList.size();
                            double[][] sortedHeatmapPvalueMatrix = new double[goSize][networkSize];
                            double[][] sumPvaluePerGO1 = new double[goSize][2];
                            double[][] sumPvaluePerNetwork1 = new double[networkSize][2];
                            for(int i=0;i<goSize;i++) {
                                sumPvaluePerGO1[i][0] = i;
                                for(int j=0;j<networkSize;j++) {
                                    sumPvaluePerNetwork1[j][0] = j;
                                    sumPvaluePerGO1[i][1] += -2*heatmapPvalueMatrix[j][i];
                                    sumPvaluePerNetwork1[j][1] += -2*heatmapPvalueMatrix[j][i];
                                }
                            }
                            sumPvaluePerGO1 = NOAUtil.dataSort(sumPvaluePerGO1, 1, 1);
                            if(this.isSortedNetwork)
                                sumPvaluePerNetwork1 = NOAUtil.dataSort(sumPvaluePerNetwork1, 1, 1);
                            List heatmapGOList1 = new ArrayList();
                            List heatmapNetworkList1 = new ArrayList();
                            for(int i=0;i<goSize;i++) {
                                heatmapGOList1.add(heatmapGOList.get((int)sumPvaluePerGO1[i][0]));
                                for(int j=0;j<networkSize;j++) {
                                    int idxOfNetwrok = (int)sumPvaluePerNetwork1[j][0];
                                    String netID = heatmapNetworkList.get(idxOfNetwrok).toString();
                                    if(heatmapNetworkList1.indexOf(netID)==-1)
                                        heatmapNetworkList1.add(netID);
                                    sortedHeatmapPvalueMatrix[i][j] = heatmapPvalueMatrix[idxOfNetwrok][(int)sumPvaluePerGO1[i][0]];
                                }
                            }
                            pvalueMatrix[k] = null;
                            heatmapPvalueMatrix = null;
                            //System.gc();
                            long end=System.currentTimeMillis();
                            System.out.println("Running time:"+(end-start)/1000/60+"min "+(end-start)/1000%60+"sec");

                            //Mapping GO term to description
                            Object[] go4Display = new Object[goSize];

                            for(int i=0;i<goSize;i++){
                                if(goDescMap.containsKey(heatmapGOList1.get(i).toString())) {
                                    go4Display[i] = goDescMap.get(heatmapGOList1.get(i).toString());
                                    if(go4Display[i].toString().length()>45)
                                        go4Display[i] = go4Display[i].toString().substring(0, 45)+"...";
                                } else {
                                    go4Display[i] = heatmapGOList1.get(i);
                                }
                            }
                            System.out.println("sortedHeatmapPvalueMatrix: "+ sortedHeatmapPvalueMatrix.length
                                    +" "+sortedHeatmapPvalueMatrix[0].length);
    //                        List aaa = Arrays.asList(go4Display);
    //                        NOAUtil.writeFile(heatmapNetworkList1, NOA.NOATempDir+tempHeatmapFileName+"_temp1.txt");
    //                        NOAUtil.writeFile(aaa, NOA.NOATempDir+tempHeatmapFileName+"_temp2.txt");
    //                        NOAUtil.writeDoubleArray(sortedHeatmapPvalueMatrix, NOA.NOATempDir+tempHeatmapFileName+"_temp3.txt");

                            HeatChart chart = new HeatChart(sortedHeatmapPvalueMatrix);
                            chart.setHighValueColour(Color.BLACK);
                            chart.setLowValueColour(Color.YELLOW);
                            chart.setYValues(go4Display);
                            chart.setXValues(heatmapNetworkList1.toArray());
                            try {
                                chart.saveToFile(new File(NOA.NOATempDir+tempHeatmapFileName+"_"+goBranch+".png"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            long end1=System.currentTimeMillis();
                            System.out.println("Running time:"+(end1-end)/1000/60+"min "+(end1-end)/1000%60+"sec");
                        }
                    }

                    taskMonitor.setStatus("Generating results ......");
                    start=System.currentTimeMillis();
                    ArrayList<Object[]> goPvalueArrayList = new ArrayList();
                    Object[][] cellsForTopResult = new Object[outputTopMap.size()][8];
                    Object[][] cellsForOverlap = new Object[outputMap.size()][4];
                    HashMap<String, String> GODescMap = new HashMap<String, String>();
                    int i = 0;
                    int j = 0;
                    int n = 0;
                    int BPcount = 0;
                    int CCcount = 0;
                    int MFcount = 0;
                    BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass()
                            .getResource(NOAStaticValues.GO_DescFile).openStream()));
                    String inputLine=in.readLine();
                    while ((inputLine = in.readLine()) != null) {
                        String[] retail = inputLine.split("\t");
                        if(retail.length>=3) {
                            //generate 'result table'
                            if(outputMap.containsKey(retail[0].trim())) {
                                GODescMap.put(retail[0].trim(), inputLine);
                                cellsForOverlap[j][0] = retail[0].trim();
                                cellsForOverlap[j][3] = "";
                                ArrayList<String> resultWithNetworkID = outputMap.get(retail[0].trim());
                                for(String eachNet:resultWithNetworkID) {
                                    Object[] tempGOPvalue = new Object[8];
                                    tempGOPvalue[1] = retail[0];
                                    String[] temp = eachNet.trim().split("\t");
                                    tempGOPvalue[0] = temp[3].trim();
                                    DecimalFormat df1 = new DecimalFormat("#.####");
                                    DecimalFormat df2 = new DecimalFormat("#.####E0");
                                    double pvalue = new Double(temp[0]).doubleValue();
                                    if(pvalue>0.0001)
                                        tempGOPvalue[3] = df1.format(pvalue);
                                    else
                                        tempGOPvalue[3] = df2.format(pvalue);
                                    tempGOPvalue[4] = temp[1];
                                    tempGOPvalue[5] = temp[2];
                                    tempGOPvalue[6] = retail[1];
                                    cellsForOverlap[j][2] = retail[1];
                                    //String tempList = this.goNodeMap.get(retail[0]).toString();
                                    tempGOPvalue[7] = temp[4].substring(1, temp[4].length()-1).trim();
                                    if(cellsForOverlap[j][3].equals("")){
                                        cellsForOverlap[j][3] = temp[3].trim();
                                    } else {
                                        cellsForOverlap[j][3] = cellsForOverlap[j][3]+"; "+temp[3].trim();
                                    }
                                    if(retail[2].equals("biological_process")) {
                                        tempGOPvalue[2] = "BP";
                                        cellsForOverlap[j][1] = "BP";
                                        BPcount++;
                                    } else if (retail[2].equals("cellular_component")) {
                                        tempGOPvalue[2] = "CC";
                                        cellsForOverlap[j][1] = "CC";
                                        CCcount++;
                                    } else {
                                        tempGOPvalue[2] = "MF";
                                        cellsForOverlap[j][1] = "MF";
                                        MFcount++;
                                    }
                                    i++;
                                    goPvalueArrayList.add(tempGOPvalue);
                                }
                                j++;
                            }
                            //Save all results to csv file.
                            if(allOutputMap.containsKey(retail[0].trim())) {
                                ArrayList<String> resultWithNetworkID = allOutputMap.get(retail[0].trim());
                                for(String eachNet:resultWithNetworkID) {
                                    String[] temp = eachNet.trim().split("\t");
                                    String tempLine = temp[3].trim()+",";
                                    tempLine += retail[0]+",";
                                    if(retail[2].equals("biological_process")) {
                                        tempLine += "BP,";
                                    } else if (retail[2].equals("cellular_component")) {
                                        tempLine += "CC,";
                                    } else {
                                        tempLine += "MF,";
                                    }
                                    DecimalFormat df1 = new DecimalFormat("#.####");
                                    DecimalFormat df2 = new DecimalFormat("#.####E0");
                                    double pvalue = new Double(temp[0]).doubleValue();
                                    if(pvalue>0.0001)
                                        tempLine += "\""+df1.format(pvalue)+"\",\"";
                                    else
                                        tempLine += "\""+df2.format(pvalue)+"\",\"";
                                    tempLine += temp[1]+"\",\"";
                                    tempLine += temp[2]+"\",\"";
                                    tempLine += retail[1]+"\",\"";
                                    tempLine += temp[4].substring(1, temp[4].length()-1).trim()+"\"";
                                    NOAUtil.writeString(tempLine, NOA.NOATempDir+tempHeatmapFileName+".csv");
                                }
                            }
                        }
                    }
                    in.close();

                    Set<String> topResultKey = outputTopMap.keySet();
                    for(String key:topResultKey){
                        cellsForTopResult[n][1] = key.substring(0, key.indexOf("\t"));
                        String[] retail = GODescMap.get(cellsForTopResult[n][1]).trim().split("\t");
                        String eachNet = outputTopMap.get(key);
                        String[] temp = eachNet.trim().split("\t");
                        cellsForTopResult[n][0] = temp[3].trim();
                        DecimalFormat df1 = new DecimalFormat("#.####");
                        DecimalFormat df2 = new DecimalFormat("#.####E0");
                        double pvalue = new Double(temp[0]).doubleValue();
                        if(pvalue>0.0001)
                            cellsForTopResult[n][3] = df1.format(pvalue);
                        else
                            cellsForTopResult[n][3] = df2.format(pvalue);
                        cellsForTopResult[n][4] = temp[1];
                        cellsForTopResult[n][5] = temp[2];
                        cellsForTopResult[n][6] = retail[1];

                        cellsForTopResult[n][7] = temp[4].substring(1, temp[4].length()-1).trim();
                        if(retail[2].equals("biological_process")) {
                            cellsForTopResult[n][2] = "BP";
                        } else if (retail[2].equals("cellular_component")) {
                            cellsForTopResult[n][2] = "CC";
                        } else {
                            cellsForTopResult[n][2] = "MF";
                        }
                        n++;
                    }
                    Object[][] goPvalueArray = new Object[goPvalueArrayList.size()][8];
                    
                    taskMonitor.setStatus("Done!");
                    Long end=System.currentTimeMillis();
                    System.out.println("Running time:"+(end-start)/1000/60+"min "+(end-start)/1000%60+"sec");
                    if(outputMap.size()>0&&goPvalueArray.length>0){
                        for(i=0;i<goPvalueArrayList.size();i++){
                            goPvalueArray[i] = goPvalueArrayList.get(i);
                        }
                        goPvalueArray = NOAUtil.dataSort(goPvalueArray, 3);
                        Object[][] cellsForResult = new Object[recordCount][8];
                        int BPindex = 0;
                        int CCindex = BPcount;
                        int MFindex = BPcount+CCcount;
                        for(i=0;i<goPvalueArray.length;i++){
                            if(goPvalueArray[i][2].equals("BP")) {
                                cellsForResult[BPindex] = goPvalueArray[i];
                                BPindex++;
                            } else if (goPvalueArray[i][2].equals("CC")) {
                                cellsForResult[CCindex] = goPvalueArray[i];
                                CCindex++;
                            } else {
                                cellsForResult[MFindex] = goPvalueArray[i];
                                MFindex++;
                            }
                        }
                        dialog = new MultipleOutputDialog(Cytoscape.getDesktop(), false, 
                                cellsForResult, cellsForTopResult, cellsForOverlap,
                                this.algType, this.formatSign, tempHeatmapFileName);
                        dialog.setLocationRelativeTo(Cytoscape.getDesktop());
                        dialog.setResizable(true);
                    } else {
                        JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                            "No result for selected criteria!", NOA.pluginName,
                            JOptionPane.WARNING_MESSAGE);
                    }
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
