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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
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
public class MultipleOutputDialog extends javax.swing.JDialog implements MouseListener{
    HashMap<String, ArrayList<String>> resultMap;
    String algType;
    OutputTableModel outputModelForResult;
    String[] tableTitleForResult;
    Object[][] cellsForResult;
    OutputTableModel outputModelForOverlap;
    String[] tableTitleForOverlap;
    Object[][] cellsForOverlap;
    int selectedRow;
    int formatSign = 0;
    int recordCount;
    String heatmapFileName = "";

    /** Creates new form SingleOutputDialog */
    public MultipleOutputDialog(java.awt.Frame parent, boolean modal,
            HashMap<String, ArrayList<String>> resultMap,
            String algType, int inputFormat, int recordCount,
            String tempHeatmapFileName) {
        super(parent, modal);
        this.resultMap = resultMap;
        this.algType = algType;
        this.formatSign = inputFormat;
        this.recordCount = recordCount;
        this.heatmapFileName = tempHeatmapFileName;
        initComponents();
        initValues();
    }
    private void initValues() {
        this.setTitle(NOA.pluginName+" output for Batch Mode");
        if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {
            if(this.formatSign == NOAStaticValues.NETWORK_FORMAT) {
                tableTitleForResult = new String [] {"Network ID", "GO ID", "Type", "P-value", "Sample", "Population", "Desciption", "Associated genes"};
                tableTitleForOverlap = new String [] {"GO ID", "Type", "Desciption", "Associated networks"};
            } else {
                tableTitleForResult = new String [] {"Set ID", "GO ID", "Type", "P-value", "Sample", "Population", "Desciption", "Associated genes"};
                tableTitleForOverlap = new String [] {"GO ID", "Type", "Desciption", "Associated sets"};
            }
        } else {
            tableTitleForResult = new String [] {"Network ID", "GO ID", "Type", "P-value", "Sample", "Population", "Desciption", "Associated edges"};
            tableTitleForOverlap = new String [] {"GO ID", "Type", "Desciption", "Associated networks"};
        }
        Object[][] goPvalueArray = new String[recordCount][8];
        cellsForOverlap = new Object[resultMap.size()][4];
        int i = 0;
        int j = 0;
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
                    if(this.resultMap.containsKey(retail[0].trim())) {
                        cellsForOverlap[j][0] = retail[0].trim();
                        cellsForOverlap[j][3] = "";
                        ArrayList<String> resultWithNetworkID = this.resultMap.get(retail[0].trim());
                        for(String eachNet:resultWithNetworkID) {
                            goPvalueArray[i][1] = retail[0];
                            String[] temp = eachNet.trim().split("\t");
                            goPvalueArray[i][0] = temp[3].trim();
                            DecimalFormat df1 = new DecimalFormat("#.####");
                            DecimalFormat df2 = new DecimalFormat("#.####E0");
                            double pvalue = new Double(temp[0]).doubleValue();
                            if(pvalue>0.0001)
                                goPvalueArray[i][3] = df1.format(pvalue);
                            else
                                goPvalueArray[i][3] = df2.format(pvalue);
                            goPvalueArray[i][4] = temp[1];
                            goPvalueArray[i][5] = temp[2];
                            goPvalueArray[i][6] = retail[1];
                            cellsForOverlap[j][2] = retail[1];
                            //String tempList = this.goNodeMap.get(retail[0]).toString();
                            goPvalueArray[i][7] = temp[4].substring(1, temp[4].length()-1).trim();
                            if(cellsForOverlap[j][3].equals("")){
                                cellsForOverlap[j][3] = temp[3].trim();
                            } else {
                                cellsForOverlap[j][3] = cellsForOverlap[j][3]+"; "+temp[3].trim();
                            }
                            if(retail[2].equals("biological_process")) {
                                goPvalueArray[i][2] = "BP";
                                cellsForOverlap[j][1] = "BP";
                                BPcount++;
                            } else if (retail[2].equals("cellular_component")) {
                                goPvalueArray[i][2] = "CC";
                                cellsForOverlap[j][1] = "CC";
                                CCcount++;
                            } else {
                                goPvalueArray[i][2] = "MF";
                                cellsForOverlap[j][1] = "MF";
                                MFcount++;
                            }
                            i++;
                        }
//                        if(cellsForOverlap[j][3].toString().substring(0,1).equals(";")){
//                            cellsForOverlap[j][3] = cellsForOverlap[j][3].toString().substring(1, cellsForOverlap[j][3].toString().length()).trim();
//                        }
//                        if(cellsForOverlap[j][3].toString().substring(cellsForOverlap[j][3].toString().length()-1,cellsForOverlap[j][3].toString().length()).equals(";")){
//                            cellsForOverlap[j][3] = cellsForOverlap[j][3].toString().substring(0, cellsForOverlap[j][3].toString().length()-1).trim();
//                        }
                        j++;
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        goPvalueArray = NOAUtil.dataSort(goPvalueArray, 3);
        cellsForResult = new Object[recordCount][8];
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
        resultTable.setAutoCreateRowSorter(true);
        setColumnWidths(resultTable);

        outputModelForOverlap = new OutputTableModel(cellsForOverlap, tableTitleForOverlap);
        overlapTable.setModel(outputModelForOverlap);
        overlapTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        overlapTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overlapTable.addMouseListener(this);
        overlapTable.setAutoCreateRowSorter(true);
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
        save2FileButton6 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        cancelButton6 = new javax.swing.JButton();
        resultSwitchComboBox = new javax.swing.JComboBox();
        heatmapButton = new javax.swing.JButton();
        resultTabbedPane = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        overlapTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        save2FileButton6.setText("Save to ...");
        save2FileButton6.setMaximumSize(new java.awt.Dimension(95, 23));
        save2FileButton6.setMinimumSize(new java.awt.Dimension(95, 23));
        save2FileButton6.setPreferredSize(new java.awt.Dimension(95, 23));
        save2FileButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save2FileButtonActionPerformed(evt);
            }
        });

        jButton8.setText("Go to Mosaic");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        cancelButton6.setText("Cancel");
        cancelButton6.setMaximumSize(new java.awt.Dimension(95, 23));
        cancelButton6.setMinimumSize(new java.awt.Dimension(95, 23));
        cancelButton6.setPreferredSize(new java.awt.Dimension(95, 23));
        cancelButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        resultSwitchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Top results", "All results" }));
        resultSwitchComboBox.setMaximumSize(new java.awt.Dimension(95, 23));
        resultSwitchComboBox.setMinimumSize(new java.awt.Dimension(95, 23));
        resultSwitchComboBox.setPreferredSize(new java.awt.Dimension(95, 23));

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
                .addComponent(resultSwitchComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(heatmapButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 219, Short.MAX_VALUE)
                .addComponent(save2FileButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addComponent(jButton8)
                .addGap(31, 31, 31)
                .addComponent(cancelButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton8)
                    .addComponent(save2FileButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resultSwitchComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(heatmapButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        resultTabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultTabbedPaneMouseClicked(evt);
            }
        });

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
                "Network ID", "Top GO ID", "Type", "P-value", "Desciption", "Associated genes"
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

        resultTabbedPane.addTab("Normal results", jScrollPane1);

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
                "GO ID", "Type", "Desciption", "Associated networks"
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

        resultTabbedPane.addTab("Overlap", jScrollPane2);

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
            String heatmapFilePath = fc.getSelectedFile().getPath() + "_heatmap.png";
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
                NOAUtil.copyfile(NOA.NOATempDir+this.heatmapFileName, heatmapFilePath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
}//GEN-LAST:event_save2FileButtonActionPerformed

    private void resultTabbedPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTabbedPaneMouseClicked
        // TODO add your handling code here:
        int i = resultTabbedPane.getSelectedIndex();
        if(i==0) {
            heatmapButton.setEnabled(false);
            resultSwitchComboBox.setEnabled(true);
        } else if (i==1) {
            if(new File(NOA.NOATempDir+heatmapFileName).exists())
                heatmapButton.setEnabled(true);
            resultSwitchComboBox.setEnabled(false);
        }
    }//GEN-LAST:event_resultTabbedPaneMouseClicked

    private void heatmapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_heatmapButtonActionPerformed
        // TODO add your handling code here:
        if(new File(NOA.NOATempDir+heatmapFileName).exists()){
//            ImageIcon imh = new ImageIcon(NOA.NOATempDir+heatmapFileName);
//            int widSize = imh.getIconWidth();
//            int heiSize = imh.getIconHeight();
//            int realWide = 0;
//            int realHei = 0;
//            if(heiSize>widSize){
//                double portion = (double)heiSize/600.0;
//                realHei = 600;
//                realWide = (int)((double)widSize/portion);
//            } else {
//                double portion = (double)widSize/800.0;
//                realHei = (int)((double)heiSize/portion);
//                realWide = 800;
//            }
//            Image img = imh.getImage();
//            Image resizedImage =img.getScaledInstance(realWide, realHei, Image.SCALE_DEFAULT);
//            JLabel pnlBackground = new JLabel(new ImageIcon(resizedImage));
//            pnlBackground.setBounds(0, 0, realWide, realHei);
//            JDialog dialog = new JDialog(this, "Pvalue heatmap between first 200 networks/sets and 200 GO IDs");
//            dialog.setContentPane(pnlBackground);
//            dialog.setSize(realWide, realHei);
//            dialog.setVisible(true);

            ImageIcon imh = new ImageIcon(NOA.NOATempDir+heatmapFileName);
            int widSize = imh.getIconWidth();
            int heiSize = imh.getIconHeight();
            int realWide = 0;
            int realHei = 0;
            if(heiSize>widSize){
                double portion = (double)heiSize/600.0;
                realHei = 600;
                realWide = (int)((double)widSize/portion);
            } else {
                double portion = (double)widSize/800.0;
                realHei = (int)((double)heiSize/portion);
                realWide = 800;
            }
            if(realHei<200)
                realHei = 200;
            if(realWide<200)
                realWide = 200;
            JLabel imageLabel = new JLabel(imh);
            JScrollPane pnlBackground = new JScrollPane(imageLabel);
            JDialog dialog = new JDialog(this, "Pvalue heatmap between first 200 networks/sets and 200 GO IDs");
            dialog.setContentPane(pnlBackground);
            dialog.setLocationRelativeTo(this);
            dialog.setSize(realWide, realHei);
            dialog.setVisible(true);
            
        }
    }//GEN-LAST:event_heatmapButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
        if(new File(NOA.NOATempDir+this.heatmapFileName).exists())
            new File(NOA.NOATempDir+this.heatmapFileName).delete();
        this.dispose();
    }//GEN-LAST:event_formWindowClosed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton6;
    private javax.swing.JButton heatmapButton;
    private javax.swing.JButton jButton8;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable overlapTable;
    private javax.swing.JComboBox resultSwitchComboBox;
    private javax.swing.JTabbedPane resultTabbedPane;
    private javax.swing.JTable resultTable;
    private javax.swing.JButton save2FileButton6;
    // End of variables declaration//GEN-END:variables

    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2){
			try{
				if(e.getSource().getClass() == Class.forName("javax.swing.JTable")) {
					if(resultTable.getSelectedColumn()==0) {
						//just for search by seq, click the detail button to see the whole result of blast.
						Object geneName = outputModelForResult.getValueAt(resultTable.getSelectedRow(),0);
						URI uri = new java.net.URI("http://amigo.geneontology.org/cgi-bin/amigo/term_details?term="+geneName);
                        Desktop.getDesktop().browse(uri);
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
}