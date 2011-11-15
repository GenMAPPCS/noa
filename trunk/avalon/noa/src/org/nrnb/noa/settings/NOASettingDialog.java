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

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import org.nrnb.noa.NOA;
import org.nrnb.noa.utils.IdMapping;
import org.nrnb.noa.utils.NOAStaticValues;
import org.nrnb.noa.utils.NOAUtil;

/**
 *
 * @author Chao
 */
public class NOASettingDialog extends javax.swing.JDialog implements ActionListener{
    public String annotationSpeciesCode = "";
    private String annotationButtonLabel = "Annotate";
    private List<String> speciesValues = new ArrayList<String>();
    private List<String> downloadDBList = new ArrayList<String>();
    private List<String> currentAttributeList = new ArrayList<String>();
    private List<String> sAnnIdeValues = new ArrayList<String>();
    private List<String> idMappingTypeValues = new ArrayList<String>();
    private int currentNetworksize = 0;
    
    /** Creates new form NOASettingDialog */
    public NOASettingDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.setTitle(NOA.pluginName+" Settings"+ " "+NOA.VERSION + "1108");
        currentNetworksize = Cytoscape.getNetworkSet().size();
        loadCurrentValues();
        initComponents();
        initValues();
        this.pack();
    }
    
    private void loadCurrentValues() {
    }

    private void initValues() {
        if(currentNetworksize>0) {
            NOAMainTabbedPane.setSelectedIndex(0);
            NOA.logger.debug("Initializing annotation IDs");
            speciesValues = Arrays.asList(NOAStaticValues.speciesList);
            currentAttributeList = Arrays.asList(cytoscape.Cytoscape
                    .getNodeAttributes().getAttributeNames());
            Collections.sort(currentAttributeList);
            sAnnIdeValues.add("ID");
            sAnnIdeValues.addAll(currentAttributeList);
            sAnnIdeComboBox.setModel(new DefaultComboBoxModel(sAnnIdeValues.toArray()));
            NOA.logger.debug("Current species: "+annotationSpeciesCode);
            //Guess species of current network for annotation
            String[] defaultSpecies = getSpeciesCommonName(CytoscapeInit
                    .getProperties().getProperty("defaultSpeciesName"));
            NOA.logger.debug("Guess species: "+defaultSpecies[0]);
            if(!defaultSpecies[0].equals("")) {
                annotationSpeciesCode = defaultSpecies[1];
                sAnnSpeComboBox.setModel(new DefaultComboBoxModel(speciesValues.toArray()));
                sAnnSpeComboBox.setSelectedIndex(speciesValues.indexOf(defaultSpecies[0]));
                downloadDBList = checkMappingResources(annotationSpeciesCode);
                NOA.logger.debug("Download db list: "+downloadDBList);
                checkDownloadStatus();

                if(downloadDBList.isEmpty()) {
                    idMappingTypeValues = IdMapping.getSourceTypes();
                    sAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
                    setDefaultAttType("ID");
                }
            }
            //updates ui based on current network attributes
            checkAnnotationStatus();
            Set selectedNodesSet = Cytoscape.getCurrentNetwork().getSelectedNodes();
            if(selectedNodesSet.size()>0) {
                sInpTesSelRadioButton.setSelected(true);
                sInpTesSelLabel.setText(Cytoscape.getCurrentNetwork().getSelectedEdges().size()
                        +"/"+Cytoscape.getCurrentNetwork().getEdgeCount()+" edges & "
                        +selectedNodesSet.size()+"/"+Cytoscape.getCurrentNetwork().getNodeCount()+" nodes");
            } else {
                sInpTesSelRadioButton.setEnabled(false);
                sInpTesSelLabel.setText("0/"+Cytoscape.getCurrentNetwork().getEdgeCount()+" edges & "
                        +"0/"+Cytoscape.getCurrentNetwork().getNodeCount()+" nodes");
            }
            checkGroupButtonSelection();
        } else {
            NOAMainTabbedPane.setSelectedIndex(1);
            sAnnMesLabel.setText("Please load a network first!");
            sAnnMesButton.setEnabled(false);
            sAnnSpeLabel.setEnabled(false);
            sAnnSpeComboBox.setEnabled(false);
            sAnnGOtLabel.setEnabled(false);
            sAnnGOtComboBox.setEnabled(false);
            sAnnIdeLabel.setEnabled(false);
            sAnnIdeComboBox.setEnabled(false);
            sAnnTypLabel.setEnabled(false);
            sAnnTypComboBox.setEnabled(false);
            sSubmitButton.setEnabled(false);
        }
    }

    private void checkGroupButtonSelection(){
        if(sInpAlgEdgRadioButton.isSelected()) {
            sInpRefCliRadioButton.setEnabled(true);
            sInpRefGenRadioButton.setEnabled(false);
            sParEdgLabel.setEnabled(true);
            sParEdgComboBox.setEnabled(true);
            if(sInpTesWhoRadioButton.isSelected()) {
                sInpRefCliRadioButton.setEnabled(true);
                sInpRefGenRadioButton.setEnabled(false);
                sInpRefWhoRadioButton.setEnabled(false);
                sInpRefCliRadioButton.setSelected(true);
            }
            if(sInpTesSelRadioButton.isSelected()) {
                sInpRefCliRadioButton.setEnabled(true);
                sInpRefGenRadioButton.setEnabled(false);
                sInpRefWhoRadioButton.setEnabled(true);
                sInpRefWhoRadioButton.setSelected(true);
            }
        }
        if(sInpAlgNodRadioButton.isSelected()) {
            sInpRefCliRadioButton.setEnabled(false);
            sInpRefGenRadioButton.setEnabled(true);
            sParEdgLabel.setEnabled(false);
            sParEdgComboBox.setEnabled(false);
            if(sInpTesWhoRadioButton.isSelected()) {
                sInpRefCliRadioButton.setEnabled(false);
                sInpRefGenRadioButton.setEnabled(true);
                sInpRefWhoRadioButton.setEnabled(false);
                sInpRefGenRadioButton.setSelected(true);
            }
            if(sInpTesSelRadioButton.isSelected()) {
                sInpRefCliRadioButton.setEnabled(false);
                sInpRefGenRadioButton.setEnabled(true);
                sInpRefWhoRadioButton.setEnabled(true);
                sInpRefWhoRadioButton.setSelected(true);
            }
        }
    }
    
    private String[] getSpeciesCommonName(String speName) {
        String[] result = {"", ""};
        for (String line : NOA.speciesMappinglist) {
            String tempMappingString = line.replace("\t", " ").toUpperCase();
            if(tempMappingString.indexOf(speName.toUpperCase())!=-1) {
                String[] s = line.split("\t");
                result[0] = s[2].trim();
                result[1] = s[3].trim();
                return result;
            }
        }
        return null;
    }
    
    private List<String> checkMappingResources(String species){
        List<String> downloadList = new ArrayList<String>();
        List<String> localFileList = new ArrayList<String>();

        String latestDerbyDB = identifyLatestVersion(NOA.derbyRemotelist,
                species+"_Derby", ".zip");
        String latestGOAnnotationDB = identifyLatestVersion(NOA.goRemotelist,
                species+"_GO", ".zip");

        localFileList = NOAUtil.retrieveLocalFiles(NOA.NOADatabaseDir);
        if(localFileList==null || localFileList.isEmpty()) {
            downloadList.add(NOAStaticValues.bridgedbDerbyDir+latestDerbyDB+".zip");
            downloadList.add(NOAStaticValues.genmappcsDatabaseDir+latestGOAnnotationDB+".zip");
            System.out.println("No any local db, need download all");
        }  else {
            String localDerbyDB = identifyLatestVersion(localFileList,
                    species+"_Derby", ".bridge");
            if(latestDerbyDB.equals("")&&!localDerbyDB.equals(""))
                latestDerbyDB = localDerbyDB;
            if(localDerbyDB.equals("")||!localDerbyDB.equals(latestDerbyDB))
                downloadList.add(NOAStaticValues.bridgedbDerbyDir+latestDerbyDB+".zip");
            String localGOslimDB = identifyLatestVersion(localFileList,
                    species+"_GO", ".zip");
            if(latestGOAnnotationDB.equals("")&&!localGOslimDB.equals(""))
                latestGOAnnotationDB = localGOslimDB;
            if(localGOslimDB.equals("")||!localGOslimDB.equals(latestGOAnnotationDB))
                downloadList.add(NOAStaticValues.genmappcsDatabaseDir+latestGOAnnotationDB+".zip");
        }
        return downloadList;
    }
    
    public String identifyLatestVersion(List<String> dbList, String prefix, String surfix) {
        String result = "";
        int latestdate = 0;
        for (String filename : dbList) {
            Pattern p = Pattern.compile(prefix+"_\\d{8}\\"+surfix);
            Matcher m = p.matcher(filename);
            if(m.find()) {
                filename = m.group();
                String datestr = filename.substring(filename.lastIndexOf("_")+1,
                        filename.indexOf("."));
                if (datestr.matches("^\\d{8}$")) {
                    int date = new Integer(datestr);
                    if (date > latestdate) {
                        latestdate = date;
                        result = filename.substring(0,filename.lastIndexOf("."));
                    }
                }
            }
        }
        return result;
    }
    
    private void checkDownloadStatus() {
        if(downloadDBList.isEmpty()) {
            sAnnIdeComboBox.setEnabled(true);
            sAnnTypComboBox.setEnabled(true);
            sAnnMesButton.setText(this.annotationButtonLabel);
            if(this.annotationButtonLabel == "Re-annotate") {
                sAnnMesButton.setForeground(Color.BLACK);
                sAnnMesLabel.setText("You can optionally re-annotate this network and old annotation will be replaced.");
                sAnnMesLabel.setForeground(Color.BLACK);
                sSubmitButton.setEnabled(true);
            } else {
                sAnnMesButton.setForeground(Color.RED);
                sAnnMesLabel.setText("You need to first annotate this network with the GO terms selected above.");
                sAnnMesLabel.setForeground(Color.RED);
                sSubmitButton.setEnabled(false);
            }
        } else {
            sAnnIdeComboBox.setEnabled(false);
            sAnnTypComboBox.setEnabled(false);
            if(!NOA.tagInternetConn) {
                sAnnMesButton.setText("Help!");
                sAnnMesButton.setForeground(Color.RED);
                sAnnMesLabel.setText("Please check internet connection.");
                sAnnMesLabel.setForeground(Color.RED);
            } else {
                sAnnMesButton.setText("Download");
                sAnnMesButton.setForeground(Color.RED);
                sAnnMesLabel.setText("You need to first download annotation databases for selected species.");
                sAnnMesLabel.setForeground(Color.RED);
            }
            sSubmitButton.setEnabled(false);
        }
    }
    
    private void checkAnnotationStatus() {
        List CurrentNetworkAtts = Arrays.asList(Cytoscape.getNodeAttributes()
                .getAttributeNames());
        if(CurrentNetworkAtts.contains(NOAStaticValues.BP_ATTNAME) &&
                CurrentNetworkAtts.contains(NOAStaticValues.CC_ATTNAME) &&
                CurrentNetworkAtts.contains(NOAStaticValues.MF_ATTNAME)) {
            this.annotationButtonLabel = "Re-annotate";
            sAnnMesButton.setText(this.annotationButtonLabel);
            sAnnMesButton.setForeground(Color.BLACK);
            sAnnMesLabel.setEnabled(true);
            sAnnMesLabel.setText("You can optionally re-annotate this network and old annotations wiil be replaced.");
            sAnnMesLabel.setForeground(Color.BLACK);
        } else {
            this.annotationButtonLabel = "Annotate";
            sAnnMesButton.setText(this.annotationButtonLabel);
            sAnnMesButton.setForeground(Color.RED);
            sAnnMesLabel.setEnabled(true);
            sAnnMesLabel.setText("You need to first annotate this network with the GO terms selected above.");
            sAnnMesLabel.setForeground(Color.RED);
        }
        checkDownloadStatus();
    }

    private void setDefaultAttType(String idName) {
        String sampleID = Cytoscape.getCurrentNetwork().nodesList().get(0)
                .toString();
        if(!idName.equals("ID")) {
            CyAttributes attribs = Cytoscape.getNodeAttributes();
            if (attribs.getType(idName) == CyAttributes.TYPE_SIMPLE_LIST) {
                List<Object> attList = attribs.getListAttribute(sampleID, idName);
                for(int i=0;i<attList.size();i++) {
                    if(attList.get(i) != null) {
                        sampleID = attList.get(i).toString();
                        break;
                    }
                }
            } else {
                sampleID = Cytoscape.getNodeAttributes().getAttribute(sampleID, idName).toString();
            }
        }
        Set<String> guessResult = IdMapping.guessIdType(sampleID);
        if(guessResult.isEmpty()) {
            sAnnTypComboBox.setSelectedIndex(findMatchType("Ensembl"));
        } else {
            sAnnTypComboBox.setSelectedIndex(findMatchType(guessResult.toArray()[0]
                    .toString()));
        }
    }
    
    private int findMatchType(String matchSeq) {
        if(matchSeq.equals("Ensembl") && annotationSpeciesCode.equals("At"))
            matchSeq = "Gramene Arabidopsis";
        int i = idMappingTypeValues.indexOf(matchSeq);
        if(i==-1) {
            int n=0;
            for(String type:idMappingTypeValues) {
                if(type.trim().toLowerCase().indexOf("ensembl")!=-1)
                    return n;
                n++;
            }
            return 0;
        } else {
            return i;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sAlgButtonGroup = new javax.swing.ButtonGroup();
        sTesButtonGroup = new javax.swing.ButtonGroup();
        sRefButtonGroup = new javax.swing.ButtonGroup();
        bAlgButtonGroup = new javax.swing.ButtonGroup();
        bRefButtonGroup = new javax.swing.ButtonGroup();
        NOAMainTabbedPane = new javax.swing.JTabbedPane();
        SinglePanel = new javax.swing.JPanel();
        javax.swing.JPanel sInpPanel = new javax.swing.JPanel();
        sInpAlgLabel = new javax.swing.JLabel();
        sInpTesLabel = new javax.swing.JLabel();
        sInpRefLabel = new javax.swing.JLabel();
        sInpTesSelLabel = new javax.swing.JLabel();
        sInpAlgNodRadioButton = new javax.swing.JRadioButton();
        sInpAlgEdgRadioButton = new javax.swing.JRadioButton();
        sInpTesWhoRadioButton = new javax.swing.JRadioButton();
        sInpTesSelRadioButton = new javax.swing.JRadioButton();
        sInpRefWhoRadioButton = new javax.swing.JRadioButton();
        sInpRefGenRadioButton = new javax.swing.JRadioButton();
        sInpRefCliRadioButton = new javax.swing.JRadioButton();
        sParPanel = new javax.swing.JPanel();
        sParEdgLabel = new javax.swing.JLabel();
        sParStaLabel = new javax.swing.JLabel();
        sParCorLabel = new javax.swing.JLabel();
        sParPvaLabel = new javax.swing.JLabel();
        sParPvaTextField = new javax.swing.JTextField();
        sParEdgComboBox = new javax.swing.JComboBox();
        sParStaComboBox = new javax.swing.JComboBox();
        sParCorComboBox = new javax.swing.JComboBox();
        sAnnPanel = new javax.swing.JPanel();
        sAnnMesLabel = new javax.swing.JLabel();
        sAnnMesButton = new javax.swing.JButton();
        sAnnSpeLabel = new javax.swing.JLabel();
        sAnnSpeComboBox = new javax.swing.JComboBox();
        sAnnIdeComboBox = new javax.swing.JComboBox();
        sAnnTypComboBox = new javax.swing.JComboBox();
        sAnnIdeLabel = new javax.swing.JLabel();
        sAnnTypLabel = new javax.swing.JLabel();
        sAnnGOtLabel = new javax.swing.JLabel();
        sAnnGOtComboBox = new javax.swing.JComboBox();
        sButtonPanel = new javax.swing.JPanel();
        sSubmitButton = new javax.swing.JButton();
        sCancelButton = new javax.swing.JButton();
        BatchPanel = new javax.swing.JPanel();
        javax.swing.JPanel bInpPanel = new javax.swing.JPanel();
        bInpAlgLabel = new javax.swing.JLabel();
        bInpTesLabel = new javax.swing.JLabel();
        bInpRefLabel = new javax.swing.JLabel();
        bInpAlgNodRadioButton = new javax.swing.JRadioButton();
        bInpAlgEdgRadioButton = new javax.swing.JRadioButton();
        bInpRefWhoRadioButton = new javax.swing.JRadioButton();
        bInpRefGenRadioButton = new javax.swing.JRadioButton();
        bInpRefCliRadioButton = new javax.swing.JRadioButton();
        bInpTesUplButton = new javax.swing.JButton();
        bInpTesPatTextField = new javax.swing.JTextField();
        bParPanel = new javax.swing.JPanel();
        bParEdgAnnLabel = new javax.swing.JLabel();
        bParStaLabel = new javax.swing.JLabel();
        bParCorLabel = new javax.swing.JLabel();
        bParPvaLabel = new javax.swing.JLabel();
        bParPvaTextField = new javax.swing.JTextField();
        bParEdgComboBox = new javax.swing.JComboBox();
        bParStaComboBox = new javax.swing.JComboBox();
        bParCorComboBox = new javax.swing.JComboBox();
        bAnnPanel = new javax.swing.JPanel();
        bAnnMesLabel = new javax.swing.JLabel();
        bAnnMesButton = new javax.swing.JButton();
        bAnnSpeLabel = new javax.swing.JLabel();
        bAnnSpeComboBox = new javax.swing.JComboBox();
        bAnnIdeComboBox = new javax.swing.JComboBox();
        bAnnTypComboBox = new javax.swing.JComboBox();
        bAnnIdeLabel = new javax.swing.JLabel();
        bAnnTypLabel = new javax.swing.JLabel();
        bAnnGOtLabel = new javax.swing.JLabel();
        bAnnGOtComboBox = new javax.swing.JComboBox();
        bButtonPanel = new javax.swing.JPanel();
        bSubmitButton = new javax.swing.JButton();
        bCancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        NOAMainTabbedPane.setPreferredSize(new java.awt.Dimension(701, 420));
        NOAMainTabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                NOAMainTabbedPaneMouseClicked(evt);
            }
        });

        sInpPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Input"));

        sInpAlgLabel.setText("Algorithm");
        sInpAlgLabel.setMaximumSize(new java.awt.Dimension(110, 14));
        sInpAlgLabel.setMinimumSize(new java.awt.Dimension(110, 14));
        sInpAlgLabel.setPreferredSize(new java.awt.Dimension(110, 14));

        sInpTesLabel.setText("Test network");
        sInpTesLabel.setMaximumSize(new java.awt.Dimension(110, 14));
        sInpTesLabel.setMinimumSize(new java.awt.Dimension(110, 14));
        sInpTesLabel.setPreferredSize(new java.awt.Dimension(110, 14));

        sInpRefLabel.setText("Reference network");
        sInpRefLabel.setMaximumSize(new java.awt.Dimension(110, 14));
        sInpRefLabel.setMinimumSize(new java.awt.Dimension(110, 14));
        sInpRefLabel.setPreferredSize(new java.awt.Dimension(110, 14));

        sInpTesSelLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        sInpTesSelLabel.setText("0/0 edges & 0/0 nodes");
        sInpTesSelLabel.setMaximumSize(new java.awt.Dimension(200, 14));
        sInpTesSelLabel.setMinimumSize(new java.awt.Dimension(200, 14));
        sInpTesSelLabel.setPreferredSize(new java.awt.Dimension(200, 14));

        bAlgButtonGroup.add(sInpAlgNodRadioButton);
        sInpAlgNodRadioButton.setText("Node-based");
        sInpAlgNodRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        sInpAlgNodRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        sInpAlgNodRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));
        sInpAlgNodRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sInpAlgNodRadioButtonActionPerformed(evt);
            }
        });

        bAlgButtonGroup.add(sInpAlgEdgRadioButton);
        sInpAlgEdgRadioButton.setSelected(true);
        sInpAlgEdgRadioButton.setText("Edge-based");
        sInpAlgEdgRadioButton.setActionCommand("Edge-based");
        sInpAlgEdgRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        sInpAlgEdgRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        sInpAlgEdgRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));
        sInpAlgEdgRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sInpAlgEdgRadioButtonActionPerformed(evt);
            }
        });

        sTesButtonGroup.add(sInpTesWhoRadioButton);
        sInpTesWhoRadioButton.setSelected(true);
        sInpTesWhoRadioButton.setText("Whole network");
        sInpTesWhoRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        sInpTesWhoRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        sInpTesWhoRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));
        sInpTesWhoRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sInpTesWhoRadioButtonActionPerformed(evt);
            }
        });

        sTesButtonGroup.add(sInpTesSelRadioButton);
        sInpTesSelRadioButton.setText("Selected sub network");
        sInpTesSelRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        sInpTesSelRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        sInpTesSelRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));
        sInpTesSelRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sInpTesSelRadioButtonActionPerformed(evt);
            }
        });

        sRefButtonGroup.add(sInpRefWhoRadioButton);
        sInpRefWhoRadioButton.setText("Whole network");
        sInpRefWhoRadioButton.setEnabled(false);
        sInpRefWhoRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        sInpRefWhoRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        sInpRefWhoRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));

        sRefButtonGroup.add(sInpRefGenRadioButton);
        sInpRefGenRadioButton.setText("Whole genome");
        sInpRefGenRadioButton.setEnabled(false);
        sInpRefGenRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        sInpRefGenRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        sInpRefGenRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));

        sRefButtonGroup.add(sInpRefCliRadioButton);
        sInpRefCliRadioButton.setSelected(true);
        sInpRefCliRadioButton.setText("Clique");
        sInpRefCliRadioButton.setMaximumSize(new java.awt.Dimension(200, 23));
        sInpRefCliRadioButton.setMinimumSize(new java.awt.Dimension(200, 23));
        sInpRefCliRadioButton.setPreferredSize(new java.awt.Dimension(200, 23));

        javax.swing.GroupLayout sInpPanelLayout = new javax.swing.GroupLayout(sInpPanel);
        sInpPanel.setLayout(sInpPanelLayout);
        sInpPanelLayout.setHorizontalGroup(
            sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sInpPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sInpAlgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpTesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpRefLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sInpTesWhoRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(sInpAlgEdgRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(sInpRefWhoRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE))
                .addGap(3, 3, 3)
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sInpTesSelRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(sInpRefGenRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(sInpAlgNodRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE))
                .addGap(6, 6, 6)
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sInpRefCliRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpTesSelLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                .addContainerGap(51, Short.MAX_VALUE))
        );
        sInpPanelLayout.setVerticalGroup(
            sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sInpPanelLayout.createSequentialGroup()
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sInpAlgEdgRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpAlgNodRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpAlgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sInpTesWhoRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpTesSelRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpTesSelLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpTesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sInpRefWhoRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpRefGenRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpRefCliRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sInpRefLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        sParPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Set Parameters"));

        sParEdgLabel.setText("Edge annotation");
        sParEdgLabel.setMaximumSize(new java.awt.Dimension(180, 14));
        sParEdgLabel.setMinimumSize(new java.awt.Dimension(180, 14));
        sParEdgLabel.setPreferredSize(new java.awt.Dimension(180, 14));

        sParStaLabel.setText("Statistical method");
        sParStaLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sParStaLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        sParStaLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        sParStaLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        sParCorLabel.setText("Correction method");
        sParCorLabel.setMaximumSize(new java.awt.Dimension(180, 14));
        sParCorLabel.setMinimumSize(new java.awt.Dimension(180, 14));
        sParCorLabel.setPreferredSize(new java.awt.Dimension(180, 14));

        sParPvaLabel.setText("P-value threshold");
        sParPvaLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sParPvaLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        sParPvaLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        sParPvaLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        sParPvaTextField.setColumns(5);
        sParPvaTextField.setText("0.05");
        sParPvaTextField.setMaximumSize(new java.awt.Dimension(32767, 32767));
        sParPvaTextField.setMinimumSize(new java.awt.Dimension(90, 20));
        sParPvaTextField.setPreferredSize(new java.awt.Dimension(108, 20));
        sParPvaTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sParPvaTextFieldActionPerformed(evt);
            }
        });

        sParEdgComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Intersection", "Union" }));
        sParEdgComboBox.setEnabled(false);
        sParEdgComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sParEdgComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        sParStaComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Hyper-geometry", "Fisher’s exact test" }));
        sParStaComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sParStaComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        sParCorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none", "Bonferroni", "Benjamini & Hochberg q value" }));
        sParCorComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sParCorComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        javax.swing.GroupLayout sParPanelLayout = new javax.swing.GroupLayout(sParPanel);
        sParPanel.setLayout(sParPanelLayout);
        sParPanelLayout.setHorizontalGroup(
            sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sParPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParCorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParEdgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParEdgComboBox, 0, 120, Short.MAX_VALUE)
                    .addComponent(sParCorComboBox, 0, 120, Short.MAX_VALUE))
                .addGap(44, 44, 44)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParStaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParPvaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParStaComboBox, 0, 120, Short.MAX_VALUE)
                    .addComponent(sParPvaTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE))
                .addContainerGap())
        );
        sParPanelLayout.setVerticalGroup(
            sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sParPanelLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sParEdgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParEdgComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParStaComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParStaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sParCorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParCorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParPvaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParPvaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        sAnnPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Retrieve GO Annotations"));
        sAnnPanel.setPreferredSize(new java.awt.Dimension(660, 140));

        sAnnMesLabel.setForeground(java.awt.Color.red);
        sAnnMesLabel.setText("You need to first annotate this network with the GO terms!");

        sAnnMesButton.setForeground(java.awt.Color.red);
        sAnnMesButton.setText("Annotate");
        sAnnMesButton.setMaximumSize(new java.awt.Dimension(32767, 32767));
        sAnnMesButton.setMinimumSize(new java.awt.Dimension(90, 18));
        sAnnMesButton.setPreferredSize(new java.awt.Dimension(108, 23));
        sAnnMesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sAnnMesButtonActionPerformed(evt);
            }
        });

        sAnnSpeLabel.setText("Species");
        sAnnSpeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sAnnSpeLabel.setMaximumSize(new java.awt.Dimension(180, 14));
        sAnnSpeLabel.setMinimumSize(new java.awt.Dimension(180, 14));
        sAnnSpeLabel.setPreferredSize(new java.awt.Dimension(180, 14));

        sAnnSpeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yeast" }));
        sAnnSpeComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sAnnSpeComboBox.setPreferredSize(new java.awt.Dimension(108, 18));
        sAnnSpeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sAnnSpeComboBoxActionPerformed(evt);
            }
        });

        sAnnIdeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ID" }));
        sAnnIdeComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sAnnIdeComboBox.setPreferredSize(new java.awt.Dimension(108, 18));
        sAnnIdeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sAnnIdeComboBoxActionPerformed(evt);
            }
        });

        sAnnTypComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Ensembl Yeast" }));
        sAnnTypComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sAnnTypComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        sAnnIdeLabel.setText("Identifier attribute");
        sAnnIdeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sAnnIdeLabel.setMaximumSize(new java.awt.Dimension(180, 14));
        sAnnIdeLabel.setMinimumSize(new java.awt.Dimension(180, 14));
        sAnnIdeLabel.setPreferredSize(new java.awt.Dimension(180, 14));

        sAnnTypLabel.setText("Type of identifier");
        sAnnTypLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sAnnTypLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        sAnnTypLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        sAnnTypLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        sAnnGOtLabel.setText("Type of GO");
        sAnnGOtLabel.setMaximumSize(new java.awt.Dimension(130, 14));
        sAnnGOtLabel.setMinimumSize(new java.awt.Dimension(130, 14));
        sAnnGOtLabel.setPreferredSize(new java.awt.Dimension(130, 14));

        sAnnGOtComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SlimMosaic", "SlimPIR", "SlimGeneric", "Full" }));
        sAnnGOtComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sAnnGOtComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        javax.swing.GroupLayout sAnnPanelLayout = new javax.swing.GroupLayout(sAnnPanel);
        sAnnPanel.setLayout(sAnnPanelLayout);
        sAnnPanelLayout.setHorizontalGroup(
            sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sAnnPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sAnnPanelLayout.createSequentialGroup()
                        .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sAnnIdeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sAnnSpeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sAnnIdeComboBox, 0, 120, Short.MAX_VALUE)
                            .addComponent(sAnnSpeComboBox, 0, 120, Short.MAX_VALUE))
                        .addGap(44, 44, 44)
                        .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(sAnnGOtLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sAnnTypLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(sAnnMesLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sAnnMesButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                    .addComponent(sAnnGOtComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 120, Short.MAX_VALUE)
                    .addComponent(sAnnTypComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 120, Short.MAX_VALUE))
                .addContainerGap())
        );
        sAnnPanelLayout.setVerticalGroup(
            sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sAnnPanelLayout.createSequentialGroup()
                .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sAnnMesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnMesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sAnnSpeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnSpeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnGOtLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnGOtComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sAnnIdeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnTypLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnTypComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sAnnIdeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        sSubmitButton.setText("Run");
        sSubmitButton.setMaximumSize(new java.awt.Dimension(70, 23));
        sSubmitButton.setMinimumSize(new java.awt.Dimension(70, 23));
        sSubmitButton.setPreferredSize(new java.awt.Dimension(70, 23));
        sSubmitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sSubmitButtonActionPerformed(evt);
            }
        });

        sCancelButton.setText("Cancel");
        sCancelButton.setMaximumSize(new java.awt.Dimension(70, 23));
        sCancelButton.setMinimumSize(new java.awt.Dimension(70, 23));
        sCancelButton.setPreferredSize(new java.awt.Dimension(70, 23));
        sCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sButtonPanelLayout = new javax.swing.GroupLayout(sButtonPanel);
        sButtonPanel.setLayout(sButtonPanelLayout);
        sButtonPanelLayout.setHorizontalGroup(
            sButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sButtonPanelLayout.createSequentialGroup()
                .addGap(490, 490, 490)
                .addComponent(sSubmitButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(sCancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(24, 24, 24))
        );
        sButtonPanelLayout.setVerticalGroup(
            sButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sButtonPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(sButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sSubmitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout SinglePanelLayout = new javax.swing.GroupLayout(SinglePanel);
        SinglePanel.setLayout(SinglePanelLayout);
        SinglePanelLayout.setHorizontalGroup(
            SinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SinglePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sButtonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sInpPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sParPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sAnnPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 672, Short.MAX_VALUE))
                .addGap(14, 14, 14))
        );
        SinglePanelLayout.setVerticalGroup(
            SinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SinglePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sInpPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sAnnPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sParPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        NOAMainTabbedPane.addTab("Single", SinglePanel);

        bInpPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Input"));

        bInpAlgLabel.setText("Algorithm");
        bInpAlgLabel.setMaximumSize(new java.awt.Dimension(110, 14));
        bInpAlgLabel.setMinimumSize(new java.awt.Dimension(110, 14));
        bInpAlgLabel.setPreferredSize(new java.awt.Dimension(110, 14));

        bInpTesLabel.setText("Test network");
        bInpTesLabel.setMaximumSize(new java.awt.Dimension(110, 14));
        bInpTesLabel.setMinimumSize(new java.awt.Dimension(110, 14));
        bInpTesLabel.setPreferredSize(new java.awt.Dimension(110, 14));

        bInpRefLabel.setText("Reference network");
        bInpRefLabel.setMaximumSize(new java.awt.Dimension(110, 14));
        bInpRefLabel.setMinimumSize(new java.awt.Dimension(110, 14));
        bInpRefLabel.setPreferredSize(new java.awt.Dimension(110, 14));

        sAlgButtonGroup.add(bInpAlgNodRadioButton);
        bInpAlgNodRadioButton.setText("Node-based");
        bInpAlgNodRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        bInpAlgNodRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        bInpAlgNodRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));
        bInpAlgNodRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInpAlgNodRadioButtonActionPerformed(evt);
            }
        });

        sAlgButtonGroup.add(bInpAlgEdgRadioButton);
        bInpAlgEdgRadioButton.setSelected(true);
        bInpAlgEdgRadioButton.setText("Edge-based");
        bInpAlgEdgRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        bInpAlgEdgRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        bInpAlgEdgRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));
        bInpAlgEdgRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInpAlgEdgRadioButtonActionPerformed(evt);
            }
        });

        bRefButtonGroup.add(bInpRefWhoRadioButton);
        bInpRefWhoRadioButton.setText("Whole network");
        bInpRefWhoRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        bInpRefWhoRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        bInpRefWhoRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));

        bRefButtonGroup.add(bInpRefGenRadioButton);
        bInpRefGenRadioButton.setText("Whole genome");
        bInpRefGenRadioButton.setEnabled(false);
        bInpRefGenRadioButton.setMaximumSize(new java.awt.Dimension(140, 23));
        bInpRefGenRadioButton.setMinimumSize(new java.awt.Dimension(140, 23));
        bInpRefGenRadioButton.setPreferredSize(new java.awt.Dimension(140, 23));

        bRefButtonGroup.add(bInpRefCliRadioButton);
        bInpRefCliRadioButton.setSelected(true);
        bInpRefCliRadioButton.setText("Clique");
        bInpRefCliRadioButton.setMaximumSize(new java.awt.Dimension(200, 23));
        bInpRefCliRadioButton.setMinimumSize(new java.awt.Dimension(200, 23));
        bInpRefCliRadioButton.setPreferredSize(new java.awt.Dimension(200, 23));

        bInpTesUplButton.setText("upload");
        bInpTesUplButton.setMaximumSize(new java.awt.Dimension(65, 20));
        bInpTesUplButton.setMinimumSize(new java.awt.Dimension(65, 19));
        bInpTesUplButton.setPreferredSize(new java.awt.Dimension(65, 19));

        bInpTesPatTextField.setText("c:\\");
            bInpTesPatTextField.setPreferredSize(new java.awt.Dimension(19, 19));

            javax.swing.GroupLayout bInpPanelLayout = new javax.swing.GroupLayout(bInpPanel);
            bInpPanel.setLayout(bInpPanelLayout);
            bInpPanelLayout.setHorizontalGroup(
                bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bInpPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bInpAlgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpTesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpRefLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(bInpPanelLayout.createSequentialGroup()
                            .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(bInpAlgEdgRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                                .addComponent(bInpRefWhoRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE))
                            .addGap(3, 3, 3)
                            .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(bInpRefGenRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                                .addComponent(bInpAlgNodRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE))
                            .addGap(6, 6, 6))
                        .addGroup(bInpPanelLayout.createSequentialGroup()
                            .addComponent(bInpTesPatTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                    .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bInpRefCliRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                        .addComponent(bInpTesUplButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(51, Short.MAX_VALUE))
            );
            bInpPanelLayout.setVerticalGroup(
                bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bInpPanelLayout.createSequentialGroup()
                    .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bInpAlgEdgRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpAlgNodRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpAlgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bInpTesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpTesUplButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpTesPatTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bInpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bInpRefWhoRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpRefGenRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpRefCliRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bInpRefLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
            );

            bParPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Set Parameters"));

            bParEdgAnnLabel.setText("Edge annotation");
            bParEdgAnnLabel.setMaximumSize(new java.awt.Dimension(180, 14));
            bParEdgAnnLabel.setMinimumSize(new java.awt.Dimension(180, 14));
            bParEdgAnnLabel.setPreferredSize(new java.awt.Dimension(180, 14));

            bParStaLabel.setText("Statistical method");
            bParStaLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            bParStaLabel.setMaximumSize(new java.awt.Dimension(148, 14));
            bParStaLabel.setMinimumSize(new java.awt.Dimension(148, 14));
            bParStaLabel.setPreferredSize(new java.awt.Dimension(148, 14));

            bParCorLabel.setText("Correction method");
            bParCorLabel.setMaximumSize(new java.awt.Dimension(180, 14));
            bParCorLabel.setMinimumSize(new java.awt.Dimension(180, 14));
            bParCorLabel.setPreferredSize(new java.awt.Dimension(180, 14));

            bParPvaLabel.setText("P-value threshold");
            bParPvaLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            bParPvaLabel.setMaximumSize(new java.awt.Dimension(148, 14));
            bParPvaLabel.setMinimumSize(new java.awt.Dimension(148, 14));
            bParPvaLabel.setPreferredSize(new java.awt.Dimension(148, 14));

            bParPvaTextField.setColumns(5);
            bParPvaTextField.setText("0.05");
            bParPvaTextField.setMinimumSize(new java.awt.Dimension(90, 20));
            bParPvaTextField.setPreferredSize(new java.awt.Dimension(108, 20));
            bParPvaTextField.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bParPvaTextFieldActionPerformed(evt);
                }
            });

            bParEdgComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Intersection", "Union", "Integer programming" }));
            bParEdgComboBox.setEnabled(false);
            bParEdgComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bParEdgComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

            bParStaComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Hyper-geometry" }));
            bParStaComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bParStaComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

            bParCorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Bonferroni" }));
            bParCorComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bParCorComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

            javax.swing.GroupLayout bParPanelLayout = new javax.swing.GroupLayout(bParPanel);
            bParPanel.setLayout(bParPanelLayout);
            bParPanelLayout.setHorizontalGroup(
                bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bParPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bParCorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParEdgAnnLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bParEdgComboBox, 0, 120, Short.MAX_VALUE)
                        .addComponent(bParCorComboBox, 0, 120, Short.MAX_VALUE))
                    .addGap(44, 44, 44)
                    .addGroup(bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bParStaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParPvaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bParStaComboBox, 0, 120, Short.MAX_VALUE)
                        .addComponent(bParPvaTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE))
                    .addContainerGap())
            );
            bParPanelLayout.setVerticalGroup(
                bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bParPanelLayout.createSequentialGroup()
                    .addGap(13, 13, 13)
                    .addGroup(bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bParEdgAnnLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParEdgComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParStaComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParStaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bParCorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParCorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParPvaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bParPvaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
            );

            bAnnPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Retrieve GO Annotations"));
            bAnnPanel.setPreferredSize(new java.awt.Dimension(660, 140));

            bAnnMesLabel.setForeground(java.awt.Color.red);
            bAnnMesLabel.setText("You need to first download necessary databases for selected species!");

            bAnnMesButton.setForeground(java.awt.Color.red);
            bAnnMesButton.setText("Download");
            bAnnMesButton.setMaximumSize(new java.awt.Dimension(32767, 32767));
            bAnnMesButton.setMinimumSize(new java.awt.Dimension(90, 18));
            bAnnMesButton.setPreferredSize(new java.awt.Dimension(108, 23));
            bAnnMesButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bAnnMesButtonActionPerformed(evt);
                }
            });

            bAnnSpeLabel.setText("Species");
            bAnnSpeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            bAnnSpeLabel.setMaximumSize(new java.awt.Dimension(180, 14));
            bAnnSpeLabel.setMinimumSize(new java.awt.Dimension(180, 14));
            bAnnSpeLabel.setPreferredSize(new java.awt.Dimension(180, 14));

            bAnnSpeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yeast" }));
            bAnnSpeComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bAnnSpeComboBox.setPreferredSize(new java.awt.Dimension(108, 18));
            bAnnSpeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bAnnSpeComboBoxActionPerformed(evt);
                }
            });

            bAnnIdeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ID" }));
            bAnnIdeComboBox.setEnabled(false);
            bAnnIdeComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bAnnIdeComboBox.setPreferredSize(new java.awt.Dimension(108, 18));
            bAnnIdeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bAnnIdeComboBoxActionPerformed(evt);
                }
            });

            bAnnTypComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Ensembl Yeast" }));
            bAnnTypComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bAnnTypComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

            bAnnIdeLabel.setText("Identifier attribute");
            bAnnIdeLabel.setEnabled(false);
            bAnnIdeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            bAnnIdeLabel.setMaximumSize(new java.awt.Dimension(180, 14));
            bAnnIdeLabel.setMinimumSize(new java.awt.Dimension(180, 14));
            bAnnIdeLabel.setPreferredSize(new java.awt.Dimension(180, 14));

            bAnnTypLabel.setText("Type of identifier");
            bAnnTypLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            bAnnTypLabel.setMaximumSize(new java.awt.Dimension(148, 14));
            bAnnTypLabel.setMinimumSize(new java.awt.Dimension(148, 14));
            bAnnTypLabel.setPreferredSize(new java.awt.Dimension(148, 14));

            bAnnGOtLabel.setText("Type of GO");
            bAnnGOtLabel.setMaximumSize(new java.awt.Dimension(130, 14));
            bAnnGOtLabel.setMinimumSize(new java.awt.Dimension(130, 14));
            bAnnGOtLabel.setPreferredSize(new java.awt.Dimension(130, 14));

            bAnnGOtComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SlimMosaic", "SlimPIR", "SlimGeneric", "Full" }));
            bAnnGOtComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
            bAnnGOtComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

            javax.swing.GroupLayout bAnnPanelLayout = new javax.swing.GroupLayout(bAnnPanel);
            bAnnPanel.setLayout(bAnnPanelLayout);
            bAnnPanelLayout.setHorizontalGroup(
                bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bAnnPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bAnnPanelLayout.createSequentialGroup()
                            .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(bAnnIdeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(bAnnSpeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(bAnnIdeComboBox, 0, 120, Short.MAX_VALUE)
                                .addComponent(bAnnSpeComboBox, 0, 120, Short.MAX_VALUE))
                            .addGap(44, 44, 44)
                            .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(bAnnGOtLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(bAnnTypLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(bAnnMesLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bAnnMesButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                        .addComponent(bAnnGOtComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 120, Short.MAX_VALUE)
                        .addComponent(bAnnTypComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 120, Short.MAX_VALUE))
                    .addContainerGap())
            );
            bAnnPanelLayout.setVerticalGroup(
                bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bAnnPanelLayout.createSequentialGroup()
                    .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bAnnMesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnMesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bAnnSpeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnSpeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnGOtLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnGOtComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(bAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bAnnIdeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnTypLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnTypComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bAnnIdeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );

            bSubmitButton.setText("Run");
            bSubmitButton.setMaximumSize(new java.awt.Dimension(70, 23));
            bSubmitButton.setMinimumSize(new java.awt.Dimension(70, 23));
            bSubmitButton.setPreferredSize(new java.awt.Dimension(70, 23));
            bSubmitButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bSubmitButtonActionPerformed(evt);
                }
            });

            bCancelButton.setText("Cancel");
            bCancelButton.setMaximumSize(new java.awt.Dimension(70, 23));
            bCancelButton.setMinimumSize(new java.awt.Dimension(70, 23));
            bCancelButton.setPreferredSize(new java.awt.Dimension(70, 23));
            bCancelButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bCancelButtonActionPerformed(evt);
                }
            });

            javax.swing.GroupLayout bButtonPanelLayout = new javax.swing.GroupLayout(bButtonPanel);
            bButtonPanel.setLayout(bButtonPanelLayout);
            bButtonPanelLayout.setHorizontalGroup(
                bButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bButtonPanelLayout.createSequentialGroup()
                    .addGap(490, 490, 490)
                    .addComponent(bSubmitButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(18, 18, 18)
                    .addComponent(bCancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(24, 24, 24))
            );
            bButtonPanelLayout.setVerticalGroup(
                bButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bButtonPanelLayout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(bButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(bSubmitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
            );

            javax.swing.GroupLayout BatchPanelLayout = new javax.swing.GroupLayout(BatchPanel);
            BatchPanel.setLayout(BatchPanelLayout);
            BatchPanelLayout.setHorizontalGroup(
                BatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(BatchPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(BatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bInpPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bParPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bAnnPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 672, Short.MAX_VALUE)
                        .addComponent(bButtonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(14, 14, 14))
            );
            BatchPanelLayout.setVerticalGroup(
                BatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(BatchPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(bInpPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(bAnnPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(bParPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(bButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(12, Short.MAX_VALUE))
            );

            NOAMainTabbedPane.addTab("Batch", BatchPanel);

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(NOAMainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 701, Short.MAX_VALUE)
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(NOAMainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
            );

            NOAMainTabbedPane.getAccessibleContext().setAccessibleName("Single");

            pack();
        }// </editor-fold>//GEN-END:initComponents

    private void bInpAlgNodRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bInpAlgNodRadioButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bInpAlgNodRadioButtonActionPerformed

    private void bInpAlgEdgRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bInpAlgEdgRadioButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bInpAlgEdgRadioButtonActionPerformed

    private void bParPvaTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bParPvaTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bParPvaTextFieldActionPerformed

    private void bAnnMesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAnnMesButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bAnnMesButtonActionPerformed

    private void bAnnSpeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAnnSpeComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bAnnSpeComboBoxActionPerformed

    private void bAnnIdeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAnnIdeComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bAnnIdeComboBoxActionPerformed

    private void bSubmitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSubmitButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bSubmitButtonActionPerformed

    private void bCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCancelButtonActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_bCancelButtonActionPerformed

    private void sAnnIdeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sAnnIdeComboBoxActionPerformed
        // TODO add your handling code here:
        //setDefaultAttType(rAnnIdeComboBox.getSelectedItem().toString());
}//GEN-LAST:event_sAnnIdeComboBoxActionPerformed

    private void sAnnSpeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sAnnSpeComboBoxActionPerformed
        // TODO add your handling code here:
        //Regenerate list of ID types when user select another species.
        NOA.logger.debug("change species");
        IdMapping.disConnectDerbyFileSource(NOA.NOADatabaseDir
                +identifyLatestVersion(NOAUtil.retrieveLocalFiles(
                NOA.NOADatabaseDir), annotationSpeciesCode+
                "_Derby", ".bridge")+".bridge");
        String[] speciesCode = getSpeciesCommonName(sAnnSpeComboBox.getSelectedItem().toString());
        annotationSpeciesCode = speciesCode[1];
        downloadDBList = checkMappingResources(annotationSpeciesCode);
        checkDownloadStatus();
        if(downloadDBList.isEmpty()) {
            IdMapping.connectDerbyFileSource(NOA.NOADatabaseDir
                    +identifyLatestVersion(NOAUtil.retrieveLocalFiles(
                    NOA.NOADatabaseDir), annotationSpeciesCode+
                    "_Derby", ".bridge")+".bridge");
            idMappingTypeValues = IdMapping.getSourceTypes();
            //System.out.println("No. of types "+ idMappingTypeValues.size());
            sAnnTypComboBox.removeAllItems();
            sAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
        }
        sAnnIdeComboBox.setSelectedItem("ID");
        setDefaultAttType("ID");
        CytoscapeInit.getProperties().setProperty("defaultSpeciesName", speciesCode[0]);
}//GEN-LAST:event_sAnnSpeComboBoxActionPerformed

    private void sAnnMesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sAnnMesButtonActionPerformed
        // TODO add your handling code here:
        if(((JButton)evt.getSource()).getText().equals("Download")) {
            System.out.println("download button on click");
            final JTaskConfig jTaskConfig = new JTaskConfig();
            jTaskConfig.setOwner(cytoscape.Cytoscape.getDesktop());
            jTaskConfig.displayCloseButton(true);
            jTaskConfig.displayCancelButton(false);
            jTaskConfig.displayStatus(true);
            jTaskConfig.setAutoDispose(true);
            jTaskConfig.setMillisToPopup(100);
            FileDownloadDialog task
                    = new FileDownloadDialog(downloadDBList);
            TaskManager.executeTask(task, jTaskConfig);
            downloadDBList = checkMappingResources(annotationSpeciesCode);
            checkDownloadStatus();
            if(downloadDBList.isEmpty()) {
                IdMapping.connectDerbyFileSource(NOA.NOADatabaseDir
                        +identifyLatestVersion(NOAUtil.retrieveLocalFiles(
                        NOA.NOADatabaseDir), annotationSpeciesCode+
                        "_Derby", ".bridge")+".bridge");
                idMappingTypeValues = IdMapping.getSourceTypes();
                //System.out.println(idMappingTypeValues.toString());
                sAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
            }
            sAnnIdeComboBox.setSelectedItem("ID");
            setDefaultAttType("ID");
        } else if (((JButton)evt.getSource()).getText().equals(this.annotationButtonLabel)) {
            String[] selectSpecies = getSpeciesCommonName(sAnnSpeComboBox.getSelectedItem().toString());
            //annotationSpeciesCode = speciesCode[1];
            if(!selectSpecies[0].equals("")) {
                List<String> localFileList = NOAUtil.retrieveLocalFiles(
                        NOA.NOADatabaseDir);
                String localDerbyDB = NOA.NOADatabaseDir +
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_Derby", ".bridge") + ".bridge";
                String localGOslimDB = NOA.NOADatabaseDir+
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_GO"+sAnnGOtComboBox.getSelectedItem().toString().toLowerCase(), ".txt") + ".txt";
                final JTaskConfig jTaskConfig = new JTaskConfig();
                jTaskConfig.setOwner(cytoscape.Cytoscape.getDesktop());
                jTaskConfig.displayCloseButton(true);
                jTaskConfig.displayCancelButton(false);
                jTaskConfig.displayStatus(true);
                jTaskConfig.setAutoDispose(true);
                jTaskConfig.setMillisToPopup(100);
                AnnotationDialog task = new AnnotationDialog(localDerbyDB,
                        localGOslimDB, sAnnIdeComboBox.getSelectedItem().toString(),
                        sAnnTypComboBox.getSelectedItem().toString(),
                        idMappingTypeValues.get(findMatchType("Ensembl")));
                TaskManager.executeTask(task, jTaskConfig);
                this.annotationButtonLabel = "Re-annotate";
                sAnnMesButton.setText(this.annotationButtonLabel);
                sAnnMesButton.setForeground(Color.BLACK);
                sAnnMesLabel.setText("You can optionally re-annotate this network and old annotation will be replaced.");
                sAnnMesLabel.setForeground(Color.BLACK);
                checkAnnotationStatus();
            } else {
                System.out.println("Retrive species error!");
            }
        } else if (((JButton)evt.getSource()).getText().equals("Help!")) {
            JOptionPane.showConfirmDialog(Cytoscape.getDesktop(),
                    "You need internet connection for downloading databases.",
                    "Warning", JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
        }
}//GEN-LAST:event_sAnnMesButtonActionPerformed

    private void sParPvaTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sParPvaTextFieldActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_sParPvaTextFieldActionPerformed

    private void sInpAlgEdgRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sInpAlgEdgRadioButtonActionPerformed
        // TODO add your handling code here:
        checkGroupButtonSelection();
}//GEN-LAST:event_sInpAlgEdgRadioButtonActionPerformed

    private void sInpAlgNodRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sInpAlgNodRadioButtonActionPerformed
        // TODO add your handling code here:
        checkGroupButtonSelection();
}//GEN-LAST:event_sInpAlgNodRadioButtonActionPerformed

    private void sSubmitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sSubmitButtonActionPerformed
        // TODO add your handling code here:
        // execute task
        String[] selectSpecies = getSpeciesCommonName(sAnnSpeComboBox.getSelectedItem().toString());
        List<String> localFileList = NOAUtil.retrieveLocalFiles(NOA.NOADatabaseDir);
        String localGOslimDB = NOA.NOADatabaseDir+
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_GO"+sAnnGOtComboBox.getSelectedItem().toString().toLowerCase(), ".txt") + ".txt";
        NOASingleEnrichmentTask task = new NOASingleEnrichmentTask(sInpAlgEdgRadioButton.isSelected(),
                sInpTesSelRadioButton.isSelected(), sInpRefWhoRadioButton.isSelected(),
                sParEdgComboBox.getSelectedItem(), sParStaComboBox.getSelectedItem(),
                sParCorComboBox.getSelectedItem(), sParPvaTextField.getText(), localGOslimDB);
        // Configure JTask Dialog Pop-Up Box
        final JTaskConfig jTaskConfig = new JTaskConfig();
        jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayCloseButton(true);
        jTaskConfig.displayCancelButton(false);
        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(true);
        jTaskConfig.setMillisToPopup(0); // always pop the task

        // Execute Task in New Thread; pop open JTask Dialog Box.
        TaskManager.executeTask(task, jTaskConfig);
        boolean succ = task.success();
        if (succ) {
            this.setVisible(false);
            this.dispose();
        } else {
            //Delete the new attributes
//            CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
//            for (String attrName : mapTgtAttrNameAttrType.keySet()) {
//                nodeAttributes.deleteAttribute(attrName);
//            }
        }
        final JDialog dialog = task.dialog();
        dialog.setVisible(true);
    }//GEN-LAST:event_sSubmitButtonActionPerformed

    private void sCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sCancelButtonActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_sCancelButtonActionPerformed

    private void sInpTesWhoRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sInpTesWhoRadioButtonActionPerformed
        // TODO add your handling code here:
        checkGroupButtonSelection();
    }//GEN-LAST:event_sInpTesWhoRadioButtonActionPerformed

    private void sInpTesSelRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sInpTesSelRadioButtonActionPerformed
        // TODO add your handling code here:
        checkGroupButtonSelection();    
    }//GEN-LAST:event_sInpTesSelRadioButtonActionPerformed

    private void NOAMainTabbedPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_NOAMainTabbedPaneMouseClicked
        // TODO add your handling code here:
        int i = NOAMainTabbedPane.getSelectedIndex();
        if(this.currentNetworksize<=0 && i==0)
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                            "Please load a network first!", NOA.pluginName,
                            JOptionPane.WARNING_MESSAGE);
    }//GEN-LAST:event_NOAMainTabbedPaneMouseClicked

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                NOASettingDialog dialog = new NOASettingDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel BatchPanel;
    private javax.swing.JTabbedPane NOAMainTabbedPane;
    private javax.swing.JPanel SinglePanel;
    private javax.swing.ButtonGroup bAlgButtonGroup;
    private javax.swing.JComboBox bAnnGOtComboBox;
    private javax.swing.JLabel bAnnGOtLabel;
    private javax.swing.JComboBox bAnnIdeComboBox;
    private javax.swing.JLabel bAnnIdeLabel;
    private javax.swing.JButton bAnnMesButton;
    private javax.swing.JLabel bAnnMesLabel;
    private javax.swing.JPanel bAnnPanel;
    private javax.swing.JComboBox bAnnSpeComboBox;
    private javax.swing.JLabel bAnnSpeLabel;
    private javax.swing.JComboBox bAnnTypComboBox;
    private javax.swing.JLabel bAnnTypLabel;
    private javax.swing.JPanel bButtonPanel;
    private javax.swing.JButton bCancelButton;
    private javax.swing.JRadioButton bInpAlgEdgRadioButton;
    private javax.swing.JLabel bInpAlgLabel;
    private javax.swing.JRadioButton bInpAlgNodRadioButton;
    private javax.swing.JRadioButton bInpRefCliRadioButton;
    private javax.swing.JRadioButton bInpRefGenRadioButton;
    private javax.swing.JLabel bInpRefLabel;
    private javax.swing.JRadioButton bInpRefWhoRadioButton;
    private javax.swing.JLabel bInpTesLabel;
    private javax.swing.JTextField bInpTesPatTextField;
    private javax.swing.JButton bInpTesUplButton;
    private javax.swing.JComboBox bParCorComboBox;
    private javax.swing.JLabel bParCorLabel;
    private javax.swing.JLabel bParEdgAnnLabel;
    private javax.swing.JComboBox bParEdgComboBox;
    private javax.swing.JPanel bParPanel;
    private javax.swing.JLabel bParPvaLabel;
    private javax.swing.JTextField bParPvaTextField;
    private javax.swing.JComboBox bParStaComboBox;
    private javax.swing.JLabel bParStaLabel;
    private javax.swing.ButtonGroup bRefButtonGroup;
    private javax.swing.JButton bSubmitButton;
    private javax.swing.ButtonGroup sAlgButtonGroup;
    private javax.swing.JComboBox sAnnGOtComboBox;
    private javax.swing.JLabel sAnnGOtLabel;
    private javax.swing.JComboBox sAnnIdeComboBox;
    private javax.swing.JLabel sAnnIdeLabel;
    private javax.swing.JButton sAnnMesButton;
    private javax.swing.JLabel sAnnMesLabel;
    private javax.swing.JPanel sAnnPanel;
    private javax.swing.JComboBox sAnnSpeComboBox;
    private javax.swing.JLabel sAnnSpeLabel;
    private javax.swing.JComboBox sAnnTypComboBox;
    private javax.swing.JLabel sAnnTypLabel;
    private javax.swing.JPanel sButtonPanel;
    private javax.swing.JButton sCancelButton;
    private javax.swing.JRadioButton sInpAlgEdgRadioButton;
    private javax.swing.JLabel sInpAlgLabel;
    private javax.swing.JRadioButton sInpAlgNodRadioButton;
    private javax.swing.JRadioButton sInpRefCliRadioButton;
    private javax.swing.JRadioButton sInpRefGenRadioButton;
    private javax.swing.JLabel sInpRefLabel;
    private javax.swing.JRadioButton sInpRefWhoRadioButton;
    private javax.swing.JLabel sInpTesLabel;
    private javax.swing.JLabel sInpTesSelLabel;
    private javax.swing.JRadioButton sInpTesSelRadioButton;
    private javax.swing.JRadioButton sInpTesWhoRadioButton;
    private javax.swing.JComboBox sParCorComboBox;
    private javax.swing.JLabel sParCorLabel;
    private javax.swing.JComboBox sParEdgComboBox;
    private javax.swing.JLabel sParEdgLabel;
    private javax.swing.JPanel sParPanel;
    private javax.swing.JLabel sParPvaLabel;
    private javax.swing.JTextField sParPvaTextField;
    private javax.swing.JComboBox sParStaComboBox;
    private javax.swing.JLabel sParStaLabel;
    private javax.swing.ButtonGroup sRefButtonGroup;
    private javax.swing.JButton sSubmitButton;
    private javax.swing.ButtonGroup sTesButtonGroup;
    // End of variables declaration//GEN-END:variables

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
