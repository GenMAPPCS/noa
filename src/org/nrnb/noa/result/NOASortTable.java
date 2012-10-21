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

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.visual.GlobalAppearanceCalculator;
import cytoscape.visual.VisualMappingManager;


/**
 *  Table for browser based on JSortTable
 */
public class NOASortTable extends JTable implements MouseListener {
	
	public static final int SELECTED_NODE = 1;
	public static final int REV_SELECTED_NODE = 2;
	public static final int SELECTED_EDGE = 3;
	public static final int REV_SELECTED_EDGE = 4;

	// Target network to watch selection
	CyNetwork currentNetwork;

	// Global calcs used for coloring
	private VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
	private GlobalAppearanceCalculator gac;
	protected int sortedColumnIndex = -1;
	protected boolean sortedColumnAscending = true;

	public NOASortTable() {
		initSortHeader();
	}

	protected void initSortHeader() {
		JTableHeader header = getTableHeader();
		header.setDefaultRenderer(new SortHeaderRenderer());
		header.addMouseListener(this);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

    @Override
    public void mouseReleased(MouseEvent event) {}

    @Override
    public void mousePressed(MouseEvent event) {}

    @Override
    public void mouseClicked(MouseEvent event) {
            final int cursorType = getTableHeader().getCursor().getType();
            if ((event.getButton() == MouseEvent.BUTTON1) && (cursorType != Cursor.E_RESIZE_CURSOR)
                && (cursorType != Cursor.W_RESIZE_CURSOR)) {
                    final int index = getColumnModel().getColumnIndexAtX(event.getX());

                    if (index >= 0) {
                            final int modelIndex = getColumnModel().getColumn(index).getModelIndex();

                            final OutputTableModel model = (OutputTableModel) getModel();

                            if (model.isSortable(modelIndex)) {
                                    // toggle ascension, if already sorted
                                    if (sortedColumnIndex == index) {
                                            sortedColumnAscending = !sortedColumnAscending;
                                    }

                                    sortedColumnIndex = index;

                                    model.sortColumn(modelIndex, sortedColumnAscending);
                            }
                    }
            }
    }

    @Override
    public void mouseEntered(MouseEvent event) {}

    @Override
    public void mouseExited(MouseEvent event) {}
    
    public int getSortedColumnIndex() {
            return sortedColumnIndex;
    }

    public boolean isSortedColumnAscending() {
            return sortedColumnAscending;
    }
}
