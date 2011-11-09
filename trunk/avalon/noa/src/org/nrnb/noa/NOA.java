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
package org.nrnb.noa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.nrnb.noa.utils.IdMapping;
import org.nrnb.noa.utils.NOAStaticValues;
import org.nrnb.noa.utils.NOAUtil;

import cytoscape.Cytoscape;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import javax.swing.JOptionPane;
import org.nrnb.noa.settings.NOASettingDialog;



// Handles the top-level menu selection event from Cytoscape
public final class NOA extends CytoscapePlugin{
    public static String pluginName = "NOA";
    public static double VERSION = 1.9;
    public static CyLogger logger;
    public static final String parentPluginName = "Mosaic";
    public static String NOABaseDir;
    public static String NOADatabaseDir;
    public static boolean tagInternetConn;
    public static boolean tagMosaicPlugin;
    public static List<String> derbyRemotelist = new ArrayList<String>();
    public static List<String> goRemotelist = new ArrayList<String>();
    public static List<String> speciesMappinglist = new ArrayList<String>();
    private static final String HELP = pluginName + " Help";
	
    /**
     * The constructor registers our layout algorithm. The CyLayouts mechanism
     * will worry about how to get it in the right menu, etc.
     */
    public NOA(){
        logger = CyLogger.getLogger(NOA.class);
		logger.setDebug(true);
        //If Mosaic has been installed, use Mosaic's database rather than download again.
        try {
            NOABaseDir = PluginManager.getPluginManager().getPluginManageDirectory()
                    .getCanonicalPath() + File.separator+ NOA.parentPluginName + File.separator;
        } catch (IOException e) {
            NOABaseDir = File.separator+ NOA.parentPluginName + File.separator;
            e.printStackTrace();
        }
        NOA.logger.debug(NOABaseDir);
        NOAUtil.checkFolder(NOABaseDir);
        NOADatabaseDir=NOABaseDir+"/DB/";
        NOAUtil.checkFolder(NOADatabaseDir);
        speciesMappinglist = NOAUtil.readResource(this.getClass()
                .getResource(NOAStaticValues.bridgedbSpecieslist));
        //Check internet connection
        NOA.tagInternetConn = NOAUtil.checkConnection();
        if(NOA.tagInternetConn) {
            //Get the lastest db lists
            derbyRemotelist = NOAUtil.readUrl(NOAStaticValues.bridgedbDerbyDir);
            goRemotelist = NOAUtil.readUrl(NOAStaticValues.genmappcsDatabaseDir);
            List<String> supportedSpeList = NOAUtil.readUrl(NOAStaticValues.genmappcsDatabaseDir+NOAStaticValues.supportedSpecieslist);
            if(supportedSpeList.size()>0) {
                NOAUtil.writeFile(supportedSpeList, NOABaseDir+NOAStaticValues.supportedSpecieslist);
                NOAStaticValues.speciesList = NOAUtil.parseSpeciesList(supportedSpeList);
            }
        } else {
            if(new File(NOABaseDir+NOAStaticValues.supportedSpecieslist).exists()) {
                List<String> supportedSpeList = NOAUtil.readFile(NOABaseDir+NOAStaticValues.supportedSpecieslist);
                if(supportedSpeList.size()>0) {
                    NOAStaticValues.speciesList = NOAUtil.parseSpeciesList(supportedSpeList);
                }
            }            
        }

        JMenuItem item = new JMenuItem(pluginName);
        JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
                .getMenu("Plugins");
        item.addActionListener(new NOAPluginActionListener(this));
        layoutMenu.add(item);

        // Add help menu item
        JMenuItem getHelp = new JMenuItem(HELP);
        getHelp.setToolTipText("Open online help for " + pluginName);
        GetHelpListener getHelpListener = new GetHelpListener();
        getHelp.addActionListener(getHelpListener);
        Cytoscape.getDesktop().getCyMenus().getHelpMenu().add(getHelp);

//        // set properties
//        // set view thresholds to handle "overview" xGMMLs
//        CytoscapeInit.getProperties().setProperty("viewThreshold", "100000");
//        CytoscapeInit.getProperties().setProperty("secondaryViewThreshold",
//                        "120000");
//        // set default node width/height lock to avoid dependency issues
//        Cytoscape.getVisualMappingManager().getVisualStyle().getDependency()
//                        .set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED,
//                                        false);
//        // cycommands
//        //new WorkspacesCommandHandler();
    }
}
class NOAPluginActionListener implements ActionListener {
    NOA plugin = null;

    public NOAPluginActionListener(NOA plugin_) {
        plugin = plugin_;
    }

    public void actionPerformed(ActionEvent evt_) {
        try {
            NOA.tagMosaicPlugin = NOAUtil.checkMosaic();
            if(!NOAUtil.checkCyThesaurus()) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "CyThesaurus 1.31 or later verion is necessary for runing NOA!", NOA.pluginName,
                        JOptionPane.WARNING_MESSAGE);
            } else {
                NewDialogTask task = new NewDialogTask();
                final JTaskConfig jTaskConfig = new JTaskConfig();
                jTaskConfig.setOwner(Cytoscape.getDesktop());
                jTaskConfig.displayCloseButton(false);
                jTaskConfig.displayCancelButton(false);
                jTaskConfig.displayStatus(true);
                jTaskConfig.setAutoDispose(true);
                jTaskConfig.setMillisToPopup(100); // always pop the task

                // Execute Task in New Thread; pop open JTask Dialog Box.
                TaskManager.executeTask(task, jTaskConfig);

                final NOASettingDialog dialog = task.dialog();
                dialog.addWindowListener(new WindowAdapter(){
                    public void windowClosed(WindowEvent e){
                        IdMapping.disConnectDerbyFileSource(NOA.NOADatabaseDir
                    +dialog.identifyLatestVersion(NOAUtil.retrieveLocalFiles(
                    NOA.NOADatabaseDir), dialog.annotationSpeciesCode+
                    "_Derby", ".bridge")+".bridge");
                    }
                });
                dialog.setVisible(true);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}

class NewDialogTask implements Task {
    private TaskMonitor taskMonitor;
    private NOASettingDialog dialog;

    public NewDialogTask() {
    }

    /**
     * Executes Task.
     */
    //@Override
    public void run() {
        try {
            taskMonitor.setStatus("Initializing...");
            dialog = new NOASettingDialog(Cytoscape.getDesktop(), true);
            dialog.setLocationRelativeTo(Cytoscape.getDesktop());
            dialog.setResizable(true);
            taskMonitor.setPercentCompleted(100);
        } catch (Exception e) {
            taskMonitor.setPercentCompleted(100);
            taskMonitor.setStatus("Failed.\n");
            e.printStackTrace();
        }
    }

    public NOASettingDialog dialog() {
        return dialog;
    }


    /**
     * Halts the Task: Not Currently Implemented.
     */
    //@Override
    public void halt() {

    }

    /**
     * Sets the Task Monitor.
     *
     * @param taskMonitor
     *            TaskMonitor Object.
     */
    //@Override
    public void setTaskMonitor(TaskMonitor taskMonitor) throws IllegalThreadStateException {
        this.taskMonitor = taskMonitor;
    }

    /**
     * Gets the Task Title.
     *
     * @return Task Title.
     */
    //@Override
    public String getTitle() {
        return "Initializing...";
    }
}

/**
 * This class direct a browser to the help manual web page.
 */
class GetHelpListener implements ActionListener {
    private String helpURL = "http://genmapp.org/beta/mosaic/index.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}
}