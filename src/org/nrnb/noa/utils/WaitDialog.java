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

package org.nrnb.noa.utils;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.nrnb.noa.settings.NOASettingDialog;

public class WaitDialog extends JDialog{

    private JPanel contextPanel;

    public WaitDialog(NOASettingDialog mPanel, String str) {
            super(mPanel,str);
            contextPanel = new JPanel();
            contextPanel.setLayout(new BorderLayout());
            JLabel b = new JLabel("",new ImageIcon(this.getClass()
            .getResource(NOAStaticValues.hourGlassGIF)),JLabel.CENTER);
            contextPanel.add(b, BorderLayout.CENTER);
            this.add(contextPanel);
            this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
}
