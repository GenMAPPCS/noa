/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SingleOutputDialog.java
 *
 * Created on Sep 19, 2011, 10:00:30 AM
 */

package org.nrnb.noa.result;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.nrnb.noa.utils.NOAStaticValues;
import org.nrnb.noa.utils.NOAUtil;

/**
 *
 * @author Chao
 */
public class SingleOutputDialog extends javax.swing.JDialog {
    Map<String, Set<String>> goNodeMap;
    Map<String, String> resultMap;
    String algType;
    DefaultTableModel outputModel;
    String[] tableTitle;
    Object[][] cells;
    
    /** Creates new form SingleOutputDialog */
    public SingleOutputDialog(java.awt.Frame parent, boolean modal, 
            Map<String, Set<String>> goNodeMap,
            Map<String, String> resultMap, String algType) {
        super(parent, modal);
        this.goNodeMap = goNodeMap;
        this.resultMap = resultMap;
        this.algType = algType;
        initComponents();
        initValues();
    }

    private void initValues() {
        if(this.algType.equals(NOAStaticValues.Algorithm_NODE)) {
            tableTitle = new String [] {"GO ID", "Type", "P-value", "Desciption", "Associated genes"};
        } else {
            tableTitle = new String [] {"GO ID", "Type", "P-value", "Desciption", "Associated edges"};
        }
        Object[][] goPvalueArray = new String[resultMap.size()][5];
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
                    if(this.resultMap.containsKey(retail[0].trim())) {
                        goPvalueArray[i][0] = retail[0];                        
                        DecimalFormat df = new DecimalFormat("#.##E0");
                        goPvalueArray[i][2] = df.format(new Double(this.resultMap.get(retail[0])).doubleValue());
                        goPvalueArray[i][3] = retail[1];
                        goPvalueArray[i][4] = this.goNodeMap.get(retail[0]).toString();
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
        goPvalueArray = NOAUtil.dataSort(goPvalueArray, 2);
        cells = new Object[resultMap.size()][5];
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
        outputModel = new DefaultTableModel(cells, tableTitle);
        resultTable.setModel(outputModel);
        resultTable.getColumnModel().getColumn(0).setMinWidth(70);
        resultTable.getColumnModel().getColumn(1).setMinWidth(40);
        resultTable.getColumnModel().getColumn(2).setMinWidth(50);
        resultTable.getColumnModel().getColumn(3).setMinWidth(100);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setColumnWidths(resultTable);
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

        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jButton1.setText("Save results");
        jButton1.setMaximumSize(new java.awt.Dimension(95, 23));
        jButton1.setMinimumSize(new java.awt.Dimension(95, 23));
        jButton1.setPreferredSize(new java.awt.Dimension(95, 23));

        jButton2.setText("GO to Mosaic");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Cancel");
        jButton3.setMaximumSize(new java.awt.Dimension(95, 23));
        jButton3.setMinimumSize(new java.awt.Dimension(95, 23));
        jButton3.setPreferredSize(new java.awt.Dimension(95, 23));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(449, Short.MAX_VALUE)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addComponent(jButton2)
                .addGap(31, 31, 31)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"GO:0022402 ", "BP", "1.2E-9", "cell cycle process ", "S000001592; S000006308; S000001595; S000000444; S000006010; S000006269; S000000735"},
                {"GO:0006357 ", "BP", "2.7E-9", "regulation of transcription from RNA polymerase II promoter ", "S000001595; S000000444; S000006010; S000006269; S000000735"},
                {"GO:0045944 ", "BP", "4.3E-9", "positive regulation of transcription from RNA polymerase II promoter ", "S000001592; S000006308; S000001595; S000000444; S000006010; S000006269"},
                {"GO:0000785 ", "CC", "8.9E-45 ", "chromatin ", "S000006308; S000001595; S000000444; S000006010; S000000735"},
                {"GO:0000790", "CC", "4.8E-43", "nuclear chromatin ", "S000001592; S000006308; S000001595; S000000444; S000006010"},
                {"GO:0044454 ", "CC", "3.4E-38", "nuclear chromosome part", "S000006308; S000001595; S000006269; S000000735; S000004674; S000005160"},
                {"GO:0044427", "MF", "8.9E-45 ", "CCAAT-binding factor complex", "S000001742; S000000842; S000001545; S000006169; S000004640; S000006151"},
                {"GO:0044428", "MF", "3.6E-41", "transcription factor complex ", "S000005449; S000006098; S000005428; S000000199; S000005258; S000003134"},
                {"GO:0044422", "MF", "2.4E-4", "nucleoplasm part ", "S000000571; S000000636"}
            },
            new String [] {
                "GO ID", "Type", "P-value", "Desciption", "Associated genes"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(resultTable);
        resultTable.getColumnModel().getColumn(0).setMinWidth(70);
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(0).setMaxWidth(70);
        resultTable.getColumnModel().getColumn(1).setMinWidth(40);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        resultTable.getColumnModel().getColumn(1).setMaxWidth(40);
        resultTable.getColumnModel().getColumn(2).setMinWidth(50);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        resultTable.getColumnModel().getColumn(2).setMaxWidth(50);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable resultTable;
    // End of variables declaration//GEN-END:variables

}
