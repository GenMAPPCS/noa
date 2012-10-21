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
package org.nrnb.noa.result;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.nrnb.noa.NOA;
import org.nrnb.noa.utils.FileChooseFilter;
import org.nrnb.noa.utils.NOAStaticValues;
import org.nrnb.noa.utils.NOAUtil;

/**
 *
 * @author Chao
 */
public class MultipleOutputDialog extends javax.swing.JDialog implements MouseListener,ChangeListener{
    HashMap<String, ArrayList<String>> resultMap;
    HashMap<String, String> topResultMap;
    String algType;
    OutputTableModel outputModelForResult;
    String[] tableTitleForResult;
    Object[][] cellsForResult;
    Object[][] cellsForTopResult;
    OutputTableModel outputModelForOverlap;
    String[] tableTitleForOverlap;
    Object[][] cellsForOverlap;
    int selectedRow;
    int formatSign = 0;
    int recordCount;
    String heatmapFileName = "";

    /** Creates new form SingleOutputDialog */
    public MultipleOutputDialog(java.awt.Frame parent, boolean modal,
            Object[][] cellsForResult, Object[][] cellsForTopResult,
            Object[][] cellsForOverlap, String algType,
            int inputFormat, String tempHeatmapFileName) {
        super(parent, modal);
        this.cellsForResult = cellsForResult;
        this.cellsForTopResult = cellsForTopResult;
        this.cellsForOverlap = cellsForOverlap;
        this.algType = algType;
        this.formatSign = inputFormat;
        this.heatmapFileName = tempHeatmapFileName;
        initComponents();
        initValues();
    }
    private void initValues() {
        this.setTitle(NOA.pluginName+" output for Batch Mode");
        if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {
            if(this.formatSign == NOAStaticValues.NETWORK_FORMAT) {
                tableTitleForResult = new String [] {"Network ID", "GO ID", "Type", "P-value", "Test", "Reference", "Description", "Associated genes"};
                tableTitleForOverlap = new String [] {"GO ID", "Type", "Description", "Associated networks"};
            } else {
                tableTitleForResult = new String [] {"Set ID", "GO ID", "Type", "P-value", "Test", "Reference", "Description", "Associated genes"};
                tableTitleForOverlap = new String [] {"GO ID", "Type", "Description", "Associated sets"};
            }
        } else {
            tableTitleForResult = new String [] {"Network ID", "GO ID", "Type", "P-value", "Test", "Reference", "Description", "Associated edges"};
            tableTitleForOverlap = new String [] {"GO ID", "Type", "Description", "Associated networks"};
        }
        outputModelForResult = new OutputTableModel(cellsForResult, tableTitleForResult);
        resultTable.setModel(outputModelForResult);
        resultTable.getColumnModel().getColumn(0).setMinWidth(70);
        resultTable.getColumnModel().getColumn(1).setMinWidth(70);
        resultTable.getColumnModel().getColumn(2).setMinWidth(40);
        resultTable.getColumnModel().getColumn(3).setMinWidth(60);
        resultTable.getColumnModel().getColumn(4).setMinWidth(60);
        resultTable.getColumnModel().getColumn(5).setMinWidth(70);
        resultTable.getColumnModel().getColumn(6).setMinWidth(100);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.addMouseListener(this);
        //resultTable.setAutoCreateRowSorter(true);
        setColumnWidths(resultTable);

        outputModelForOverlap = new OutputTableModel(cellsForOverlap, tableTitleForOverlap);
        overlapTable.setModel(outputModelForOverlap);
        overlapTable.getColumnModel().getColumn(0).setMinWidth(70);
        overlapTable.getColumnModel().getColumn(1).setMinWidth(40);
        overlapTable.getColumnModel().getColumn(2).setMinWidth(100);
        overlapTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        overlapTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overlapTable.addMouseListener(this);
        setColumnWidths(overlapTable);

        resultTabbedPane.addChangeListener(this);
        heatmapButton.setVisible(false);
        goMosaicButton.setVisible(false);
    }
    
    public void setColumnWidths(JTable table) {
        int headerwidth = 0;
        int datawidth = 0;

        int columnCount = table.getColumnCount();
        TableColumnModel tcm = table.getColumnModel();
        for (int i = 0; i < columnCount; i++) {
            try{
                TableColumn column = tcm.getColumn(i);
                TableCellRenderer renderer = table.getCellRenderer(0, i);
                Component comp = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, i);
                headerwidth = comp.getPreferredSize().width;
                datawidth = calculateColumnWidth(table, i);
                if(headerwidth > datawidth){
                    column.setPreferredWidth(headerwidth + 10);
                } else {
                    column.setPreferredWidth(datawidth + 10);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public int calculateColumnWidth(JTable table,int columnIndex) {
        int width = 0;
        int rowCount = table.getRowCount();
        for (int j = 0; j < rowCount; j++) {
            TableCellRenderer renderer = table.getCellRenderer(j, columnIndex);
            Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(j, columnIndex), false, false, j, columnIndex);
            int thisWidth = comp.getPreferredSize().width;
            if (thisWidth > width) {
                width = thisWidth;
            }
        }
        return width;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel8 = new javax.swing.JPanel();
        save2FileButton = new javax.swing.JButton();
        goMosaicButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        resultSwitchComboBox = new javax.swing.JComboBox();
        heatmapButton = new javax.swing.JButton();
        resultTabbedPane = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new NOASortTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        overlapTable = new NOASortTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        save2FileButton.setText("Save");
        save2FileButton.setMaximumSize(new java.awt.Dimension(95, 23));
        save2FileButton.setMinimumSize(new java.awt.Dimension(95, 23));
        save2FileButton.setPreferredSize(new java.awt.Dimension(95, 23));
        save2FileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save2FileButtonActionPerformed(evt);
            }
        });

        goMosaicButton.setText("Go to Mosaic");
        goMosaicButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new java.awt.Dimension(95, 23));
        cancelButton.setMinimumSize(new java.awt.Dimension(95, 23));
        cancelButton.setPreferredSize(new java.awt.Dimension(95, 23));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        resultSwitchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Top results", "All results" }));
        resultSwitchComboBox.setSelectedIndex(1);
        resultSwitchComboBox.setMaximumSize(new java.awt.Dimension(95, 23));
        resultSwitchComboBox.setMinimumSize(new java.awt.Dimension(95, 23));
        resultSwitchComboBox.setPreferredSize(new java.awt.Dimension(95, 23));
        resultSwitchComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resultSwitchComboBoxActionPerformed(evt);
            }
        });

        heatmapButton.setText("Heatmap");
        heatmapButton.setEnabled(false);
        heatmapButton.setMaximumSize(new java.awt.Dimension(95, 23));
        heatmapButton.setMinimumSize(new java.awt.Dimension(95, 23));
        heatmapButton.setPreferredSize(new java.awt.Dimension(95, 23));
        heatmapButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                heatmapButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(heatmapButton, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resultSwitchComboBox, 0, 95, Short.MAX_VALUE)
                .addGap(245, 245, 245)
                .addComponent(save2FileButton, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                .addGap(31, 31, 31)
                .addComponent(goMosaicButton, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addGap(31, 31, 31)
                .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(goMosaicButton)
                    .addComponent(save2FileButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resultSwitchComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(heatmapButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Network1", "GO:0022402 ", "BP", "1.2E-9", "cell cycle process ", "S000001592; S000006308; S000001595; S000000444; S000006010; S000006269; S000000735"},
                {"Network1", "GO:0000785 ", "CC", "8.9E-45 ", "chromatin ", "S000006308; S000001595; S000000444; S000006010; S000000735"},
                {"Network1", "GO:0044427", "MF", "8.9E-45 ", "CCAAT-binding factor complex", "S000001742; S000000842; S000001545; S000006169; S000004640; S000006151"},
                {"Network2", "GO:0006357 ", "BP", "2.7E-9", "regulation of transcription from RNA polymerase II promoter ", "S000001595; S000000444; S000006010; S000006269; S000000735"},
                {"Network2", "GO:0000790", "CC", "4.8E-43", "nuclear chromatin ", "S000001592; S000006308; S000001595; S000000444; S000006010"},
                {"Network2", "GO:0044428", "MF", "3.6E-41", "transcription factor complex ", "S000005449; S000006098; S000005428; S000000199; S000005258; S000003134"},
                {"Network3", "GO:0045944 ", "BP", "4.3E-9", "positive regulation of transcription from RNA polymerase II promoter ", "S000001592; S000006308; S000001595; S000000444; S000006010; S000006269"},
                {"Network3", "GO:0044454 ", "CC", "3.4E-38", "nuclear chromosome part", "S000006308; S000001595; S000006269; S000000735; S000004674; S000005160"},
                {"Network3", "GO:0044422", "MF", "2.4E-4", "nucleoplasm part ", "S000000571; S000000636"}
            },
            new String [] {
                "Network ID", "Top GO ID", "Type", "P-value", "Description", "Associated genes"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(resultTable);
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(0).setMaxWidth(100);
        resultTable.getColumnModel().getColumn(1).setMinWidth(70);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(1).setMaxWidth(70);
        resultTable.getColumnModel().getColumn(2).setMinWidth(40);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(40);
        resultTable.getColumnModel().getColumn(2).setMaxWidth(40);
        resultTable.getColumnModel().getColumn(3).setMinWidth(50);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        resultTable.getColumnModel().getColumn(3).setMaxWidth(50);

        resultTabbedPane.addTab("Results", jScrollPane1);

        overlapTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"GO:0022402 ", "BP", "cell cycle process ", "S000001592; S000006308; S000001595; S000000444; S000006010; S000006269; S000000735"},
                {"GO:0006357 ", "BP", "regulation of transcription from RNA polymerase II promoter ", "S000001595; S000000444; S000006010; S000006269; S000000735"},
                {"GO:0045944 ", "BP", "positive regulation of transcription from RNA polymerase II promoter ", "S000001592; S000006308; S000001595; S000000444; S000006010; S000006269"},
                {"GO:0000785 ", "CC", "chromatin ", "S000006308; S000001595; S000000444; S000006010; S000000735"},
                {"GO:0000790", "CC", "nuclear chromatin ", "S000001592; S000006308; S000001595; S000000444; S000006010"},
                {"GO:0044454 ", "CC", "nuclear chromosome part", "S000006308; S000001595; S000006269; S000000735; S000004674; S000005160"},
                {"GO:0044427", "MF", "CCAAT-binding factor complex", "S000001742; S000000842; S000001545; S000006169; S000004640; S000006151"},
                {"GO:0044428", "MF", "transcription factor complex ", "S000005449; S000006098; S000005428; S000000199; S000005258; S000003134"},
                {"GO:0044422", "MF", "nucleoplasm part ", "S000000571; S000000636"}
            },
            new String [] {
                "GO ID", "Type", "Description", "Associated networks"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane2.setViewportView(overlapTable);

        resultTabbedPane.addTab("Overlap results", jScrollPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(resultTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(resultTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        // TODO add your handling code here:
        if(new File(NOA.NOATempDir+this.heatmapFileName).exists())
            new File(NOA.NOATempDir+this.heatmapFileName).delete();
        this.dispose();
}//GEN-LAST:event_cancelButtonActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_jButton2ActionPerformed

    private void save2FileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save2FileButtonActionPerformed
        // TODO add your handling code here:
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileChooseFilter("csv","CSV (Comma delimited)(*.csv)"));
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String resultFilePath = fc.getSelectedFile().getPath() + "_result.csv";
            String overlapFilePath = fc.getSelectedFile().getPath() + "_overlap.csv";
            String heatmapFilePath = fc.getSelectedFile().getPath() + "_heatmap";
            String allResultsFilePath = fc.getSelectedFile().getPath() + "_all.csv";
            try {
                int rowNumber = outputModelForResult.getRowCount();
                List<String> output = new ArrayList<String>();
                for(int i=0;i<rowNumber;i++) {
                    String tempLine = outputModelForResult.getValueAt(i, 0)+","+outputModelForResult.getValueAt(i, 1)
                            +","+outputModelForResult.getValueAt(i, 2)+",\""+outputModelForResult.getValueAt(i, 3)
                            +"\",\""+outputModelForResult.getValueAt(i, 4)+"\",\""+outputModelForResult.getValueAt(i, 5)
                            +"\",\""+outputModelForResult.getValueAt(i, 6)+"\",\""+outputModelForResult.getValueAt(i, 7)+"\"";
                    output.add(tempLine);
                }
                NOAUtil.writeFile(output, resultFilePath);
                rowNumber = outputModelForOverlap.getRowCount();
                output = new ArrayList<String>();
                for(int i=0;i<rowNumber;i++) {
                    String tempLine = outputModelForOverlap.getValueAt(i, 0)+","+outputModelForOverlap.getValueAt(i, 1)
                            +","+outputModelForOverlap.getValueAt(i, 2)+",\""+outputModelForOverlap.getValueAt(i, 3)+"\"";
                    output.add(tempLine);
                }
                NOAUtil.writeFile(output, overlapFilePath);
                if(new File(NOA.NOATempDir+this.heatmapFileName+"_MF.png").exists()){
                    NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+"_MF.png", heatmapFilePath+"_MF.png");
                    NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+"_MF.csv", heatmapFilePath+"_MF.csv");
                }
                if(new File(NOA.NOATempDir+this.heatmapFileName+"_CC.png").exists()){
                    NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+"_CC.png", heatmapFilePath+"_CC.png");
                    NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+"_CC.csv", heatmapFilePath+"_CC.csv");
                }
                if(new File(NOA.NOATempDir+this.heatmapFileName+"_BP.png").exists()){
                    NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+"_BP.png", heatmapFilePath+"_BP.png");
                    NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+"_BP.csv", heatmapFilePath+"_BP.csv");
                }
                NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName+".csv", allResultsFilePath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
}//GEN-LAST:event_save2FileButtonActionPerformed

    private void heatmapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_heatmapButtonActionPerformed
        // TODO add your handling code here:
        if(new File(NOA.NOATempDir+this.heatmapFileName+"_BP.png").exists()||
                new File(NOA.NOATempDir+this.heatmapFileName+"_CC.png").exists()||
                new File(NOA.NOATempDir+this.heatmapFileName+"_MF.png").exists()){
            int realWide = 0;
            int realHei = 0;            
            
            JDialog dialog = new JDialog(this, "Pvalue heatmap between networks/sets and GO IDs");
            JTabbedPane heatmapTabbedPane = new JTabbedPane();
            if(new File(NOA.NOATempDir+this.heatmapFileName+"_MF.png").exists()){
                ImageIcon imh = new ImageIcon(NOA.NOATempDir+this.heatmapFileName+"_MF.png");
                int widSize = imh.getIconWidth();
                int heiSize = imh.getIconHeight();
                if(heiSize>widSize){
                    double portion = (double)heiSize/600.0;
                    realHei = 600;
                    realWide = (int)((double)widSize/portion);
                } else {
                    double portion = (double)widSize/800.0;
                    realHei = (int)((double)heiSize/portion);
                    realWide = 800;
                }
                JLabel imageLabel = new JLabel(imh);
                JScrollPane pnlBackground = new JScrollPane(imageLabel);
                heatmapTabbedPane.add(pnlBackground, 0);
                heatmapTabbedPane.setTitleAt(0, "Molecular Function");
            }
            if(new File(NOA.NOATempDir+this.heatmapFileName+"_CC.png").exists()){
                ImageIcon imh = new ImageIcon(NOA.NOATempDir+this.heatmapFileName+"_CC.png");
                int widSize = imh.getIconWidth();
                int heiSize = imh.getIconHeight();
                if(heiSize>widSize){
                    double portion = (double)heiSize/600.0;
                    realHei = 600;
                    realWide = (int)((double)widSize/portion);
                } else {
                    double portion = (double)widSize/800.0;
                    realHei = (int)((double)heiSize/portion);
                    realWide = 800;
                }
                JLabel imageLabel = new JLabel(imh);
                JScrollPane pnlBackground = new JScrollPane(imageLabel);
                heatmapTabbedPane.add(pnlBackground, 0);
                heatmapTabbedPane.setTitleAt(0, "Cellular Component");
            }
            if(new File(NOA.NOATempDir+this.heatmapFileName+"_BP.png").exists()){
                ImageIcon imh = new ImageIcon(NOA.NOATempDir+this.heatmapFileName+"_BP.png");
                int widSize = imh.getIconWidth();
                int heiSize = imh.getIconHeight();
                if(heiSize>widSize){
                    double portion = (double)heiSize/600.0;
                    realHei = 600;
                    realWide = (int)((double)widSize/portion);
                } else {
                    double portion = (double)widSize/800.0;
                    realHei = (int)((double)heiSize/portion);
                    realWide = 800;
                }
                JLabel imageLabel = new JLabel(imh);
                JScrollPane pnlBackground = new JScrollPane(imageLabel);                
                heatmapTabbedPane.add(pnlBackground, 0);
                heatmapTabbedPane.setTitleAt(0, "Biological Process");
            }
            if(realHei<200)
                realHei = 200;
            if(realWide<200)
                realWide = 200;
            dialog.setContentPane(heatmapTabbedPane);
            dialog.setLocationRelativeTo(this);
            dialog.setSize(realWide, realHei);
            dialog.setVisible(true);
            
        }
    }//GEN-LAST:event_heatmapButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
        if(new File(NOA.NOATempDir+this.heatmapFileName+".png").exists())
            new File(NOA.NOATempDir+this.heatmapFileName+".png").delete();
        if(new File(NOA.NOATempDir+this.heatmapFileName+".csv").exists())
            new File(NOA.NOATempDir+this.heatmapFileName+".csv").delete();
        if(new File(NOA.NOATempDir+this.heatmapFileName+"_BP.png").exists()){
            new File(NOA.NOATempDir+this.heatmapFileName+"_BP.png").delete();
            new File(NOA.NOATempDir+this.heatmapFileName+"_BP.csv").delete();
        }
        if(new File(NOA.NOATempDir+this.heatmapFileName+"_CC.png").exists()){
            new File(NOA.NOATempDir+this.heatmapFileName+"_CC.png").delete();
            new File(NOA.NOATempDir+this.heatmapFileName+"_CC.csv").delete();
        }
        if(new File(NOA.NOATempDir+this.heatmapFileName+"_MF.png").exists()){
            new File(NOA.NOATempDir+this.heatmapFileName+"_MF.png").delete();
            new File(NOA.NOATempDir+this.heatmapFileName+"_MF.csv").delete();
        }
        this.dispose();
    }//GEN-LAST:event_formWindowClosed

    private void resultSwitchComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resultSwitchComboBoxActionPerformed
        // TODO add your handling code here:
        if(((JComboBox)evt.getSource()).getSelectedItem().equals("Top results")) {
            outputModelForResult = new OutputTableModel(cellsForTopResult, tableTitleForResult);
        } else {
            outputModelForResult = new OutputTableModel(cellsForResult, tableTitleForResult);
        }
        resultTable.setModel(outputModelForResult);
        resultTable.getColumnModel().getColumn(0).setMinWidth(70);
        resultTable.getColumnModel().getColumn(1).setMinWidth(70);
        resultTable.getColumnModel().getColumn(2).setMinWidth(40);
        resultTable.getColumnModel().getColumn(3).setMinWidth(60);
        resultTable.getColumnModel().getColumn(4).setMinWidth(60);
        resultTable.getColumnModel().getColumn(5).setMinWidth(70);
        resultTable.getColumnModel().getColumn(6).setMinWidth(100);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.addMouseListener(this);
        resultTable.setAutoCreateRowSorter(true);
        setColumnWidths(resultTable);
    }//GEN-LAST:event_resultSwitchComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton goMosaicButton;
    private javax.swing.JButton heatmapButton;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable overlapTable;
    private javax.swing.JComboBox resultSwitchComboBox;
    private javax.swing.JTabbedPane resultTabbedPane;
    private javax.swing.JTable resultTable;
    private javax.swing.JButton save2FileButton;
    // End of variables declaration//GEN-END:variables

    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2){
            int i = resultTabbedPane.getSelectedIndex();
			try{
				if(e.getSource().getClass() == Class.forName("javax.swing.JTable")) {
                    if(i==0) {
                        if(resultTable.getSelectedColumn()==1) {
                            int rowNum = resultTable.convertRowIndexToModel(resultTable.getSelectedRow());
                            //just for search by seq, click the detail button to see the whole result of blast.
                            Object geneName = outputModelForResult.getValueAt(rowNum,1);
                            URI uri = new java.net.URI("http://amigo.geneontology.org/cgi-bin/amigo/term_details?term="+geneName);
                            Desktop.getDesktop().browse(uri);
                        }
                    } else {
                        if(overlapTable.getSelectedColumn()==0) {
                            int rowNum = overlapTable.convertRowIndexToModel(overlapTable.getSelectedRow());
                            //just for search by seq, click the detail button to see the whole result of blast.
                            Object geneName = outputModelForOverlap.getValueAt(rowNum,0);
                            URI uri = new java.net.URI("http://amigo.geneontology.org/cgi-bin/amigo/term_details?term="+geneName);
                            Desktop.getDesktop().browse(uri);
                        }
                    }
				}
			} catch(Exception ex){
				ex.printStackTrace();
			}
		}
    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    public void stateChanged(ChangeEvent e) {
        int i = resultTabbedPane.getSelectedIndex();
        if(i==0) {
            heatmapButton.setEnabled(false);
            heatmapButton.setVisible(false);
            resultSwitchComboBox.setEnabled(true);
            resultSwitchComboBox.setVisible(true);
        } else {
            if(new File(NOA.NOATempDir+this.heatmapFileName+"_BP.png").exists()||
                new File(NOA.NOATempDir+this.heatmapFileName+"_CC.png").exists()||
                new File(NOA.NOATempDir+this.heatmapFileName+"_MF.png").exists()){
                heatmapButton.setEnabled(true);
                heatmapButton.setVisible(true);
            }
            resultSwitchComboBox.setEnabled(false);
            resultSwitchComboBox.setVisible(false);
        }
    }
}