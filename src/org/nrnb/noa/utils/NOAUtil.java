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
package org.nrnb.noa.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils;
import org.nrnb.mosaic.Mosaic;
import org.nrnb.noa.NOA;
import csplugins.id.mapping.CyThesaurusPlugin;
import cytoscape.CyEdge;
import cytoscape.CyNode;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import org.nrnb.noa.algorithm.EdgeAnnotationMethod;

public class NOAUtil {

    public static boolean checkMosaic(){
        try {
            double mosaicVersion = Mosaic.VERSION;
            NOA.logger.debug("MosaicPlugin VERSION: "+ mosaicVersion);
            if(mosaicVersion>=1.0)
                return true;
            else
                return false;
        } catch(NoClassDefFoundError e){
            return false;
        }
    }

    public static boolean checkCyThesaurus(){
        try {
            double cyThesVersion = CyThesaurusPlugin.VERSION;
            NOA.logger.debug("CyThesaurusPlugin VERSION: "+ cyThesVersion);
            if(cyThesVersion>=1.31)
                return true;
            else
                return false;
        } catch(NoClassDefFoundError e){
            return false;
        }
    }

    public static boolean isValidGOTerm(List values) {
        for(Object o:values) {
            if(o.toString().indexOf("GO")!=-1||o.toString().equals("unassigned"))
                continue;
            else
                return false;
        }
        return true;
    }
    
    public static boolean checkConnection() {
        try {
            URL url = new URL("http://www.google.com/");
            URLConnection urlConnection = url.openConnection();

            InputStream inputStream = urlConnection.getInputStream();
            Reader reader = new InputStreamReader(inputStream);

            StringBuilder contents = new StringBuilder();
            CharBuffer buf = CharBuffer.allocate(1024);

            while (true) {
                    reader.read(buf);
                    if (!buf.hasRemaining())
                            break;

                    contents = contents.append(buf);
            }
            inputStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param filePath
     * @return
     */
    public static void checkFolder(String filePath) {
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * @param strUrl
     * @return
     */
    public static List<String> readUrl(final String strUrl) {
        final List<String> ret = new ArrayList<String>();
        try {
            URL url = new URL(strUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            InputStream in = c.getInputStream();
            if (in != null) {
                StringBuilder sb = new StringBuilder();
                String line;
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in, "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        ret.add(line);
                    }
                } finally {
                    in.close();
                }
            } else {
                System.out.println("No databases found at " + strUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param filename
     * @return
     */
    public static List<String> readFile(final String filename) {
        final List<String> ret = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                ret.add(inputLine);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param txtList
     * @param MyFilePath
     * @return
     */
    public static boolean writeFile(List<String> txtList, String MyFilePath) {
        boolean tag = true;
        try {
            FileWriter writer = new FileWriter(MyFilePath);
            BufferedWriter bufWriter = new BufferedWriter(writer);
            for(String txtData:txtList){
                bufWriter.write(txtData);
                bufWriter.newLine();
            }
            bufWriter.close();
            writer.close();
        } catch (Exception e) {
            tag = false;
            e.printStackTrace();
        }
        return tag;
    }

    public static boolean writeDoubleArray(double[][] value, String MyFilePath) {
		boolean tag = true;
		try {
			FileWriter writer = new FileWriter(MyFilePath);
			BufferedWriter bufWriter = new BufferedWriter(writer);
			for(int i=0;i<value.length;i++) {
				for(int j=0;j<value[0].length;j++) {
					bufWriter.write(value[i][j]+"\t");
				}
				bufWriter.newLine();
			}
			bufWriter.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tag;
	}

    public static void copyfile(String sourFile, String destFile) {
        try {
            InputStream in = new FileInputStream(sourFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
			e.printStackTrace();
		}
    }

    /**
     * @param filename
     * @return
     */
    public static List<String> readResource(final URL filename) {
        final List<String> ret = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                ret.add(inputLine);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param filename
     * @return
     */
    public static Map<String, String> readMappingFile(final URL filename) {
        final Map<String, String> ret = new HashMap<String, String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] retail = inputLine.split("\t");
                if(retail.length>=2) {
                    ret.put(retail[0].trim(), retail[1].trim());
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param filename
     * @return
     */
    public static Map<String, String> readMappingFile(URL filename, List<Object> firstAttributeList, int index) {
        Map<String, String> ret = new HashMap<String, String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] retail = inputLine.split("\t");
                if(retail.length>=2) {
                    if(firstAttributeList.indexOf(retail[index].trim())!=-1)
                        ret.put(retail[0].trim(), retail[1].trim());
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param filename
     * @return
     */
    public static Map<String, String> readGOMappingFile(URL filename, Set<Object> secondAttributeList) {
        Map<String, String> ret = new HashMap<String, String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename.openStream()));
            String inputLine=in.readLine();
            while ((inputLine = in.readLine()) != null) {
                String[] retail = inputLine.split("\t");
                if(retail.length>=3) {
                    if(secondAttributeList.contains(retail[1].trim()))
                        ret.put(retail[0].trim(), retail[1].trim()+"\t"+retail[2].trim());
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param filePath
     * @return
     */
    public static List<String> retrieveLocalFiles(String filePath) {
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String[] children = dir.list();
        return Arrays.asList(children);
    }
    
	/**
	 * sorting an array by one of the columns, and then return a decreasing array.
	 * @param a - unsorted double array
	 * @param b - the column number of array
	 * @param orderTag - decreasing tag, the value is not important
	 * @return sorted array
	 */
	public static double[][] dataSort(double[][] a, int b, int orderTag){
		double[][] dataArray = a;
		int array_size = a.length;
		if (a[0].length <= b) {
			return null;
		}
		//Build-Min-Heap
		for(int i = new Double(Math.floor((array_size-1)/2)).intValue(); i>=0; i--){
			//Min-Heap
			double[] key = dataArray[i];
			int smallest = 0;
			do {
				smallest = (i+1)*2-1;
				if (((i+1)*2<array_size)&&(dataArray[(i+1)*2][b]<dataArray[(i+1)*2-1][b])) {
					smallest = (i+1)*2;
				}
				if (((i+1)*2-1<array_size)&&(key[b]>dataArray[smallest][b])){
					dataArray[i] = dataArray[smallest];
					i = smallest;
				} else {
					dataArray[i] = key;
				}
			} while(key != dataArray[i]);
		}

		for(int i = array_size-1; i>=1; i--){
			double[] key = dataArray[i];
			dataArray[i] = dataArray[0];
			dataArray[0] = key;
			array_size = array_size - 1;
			int j = 0;
			key = dataArray[j];
			int smallest = 0;
			do {
				smallest = (j+1)*2-1;
				if (((j+1)*2<array_size)&&(dataArray[(j+1)*2][b]<dataArray[(j+1)*2-1][b])) {
					smallest = (j+1)*2;
				}
				if (((j+1)*2-1<array_size)&&(key[b]>dataArray[smallest][b])){
					dataArray[j] = dataArray[smallest];
					j = smallest;
				} else {
					dataArray[j] = key;
				}
			} while(key != dataArray[j]);
		}
		return dataArray;
	}

    /**
	 * sorting an array by one of the columns, and then return a increasing array.
	 * @param a - unsorted double array
	 * @param b - the column number of array
	 * @return sorted array
	 */
	public static double[][] dataSort(double[][] a, int b){
		double[][] dataArray = a;
		int array_size = a.length;
		if (a[0].length <= b) {
			return null;
		}
		//Build-Max-Heap
		for(int i = new Double(Math.floor((array_size-1)/2)).intValue(); i>=0; i--){
			//Max-Heap
			double[] key = dataArray[i];
			int largest = 0;
			do {
				largest = (i+1)*2-1;
				if (((i+1)*2<array_size)&&(dataArray[(i+1)*2][b]>dataArray[(i+1)*2-1][b])) {
					largest = (i+1)*2;
				}
				if (((i+1)*2-1<array_size)&&(key[b]<dataArray[largest][b])){
					dataArray[i] = dataArray[largest];
					i = largest;
				} else {
					dataArray[i] = key;
				}
			} while(key != dataArray[i]);
		}
		for(int i = array_size-1; i>=1; i--){
			double[] key = dataArray[i];
			dataArray[i] = dataArray[0];
			dataArray[0] = key;
			array_size = array_size - 1;
			int j = 0;
			key = dataArray[j];
			int largest = 0;
			do {
				largest = (j+1) * 2 - 1;
				if (((j+1) * 2<array_size)&&(dataArray[(j+1) * 2][b]>dataArray[(j+1) * 2 - 1][b])) {
					largest = (j+1) * 2;
				}
				if (((j+1) * 2-1<array_size)&&(key[b]<dataArray[largest][b])){
					dataArray[j] = dataArray[largest];
					j = largest;
				} else {
					dataArray[j] = key;
				}
			} while(key != dataArray[j]);
		}
		return dataArray;
    }

	/**
	 * sorting an array by one of the columns, and then return a increasing array.
	 * @param a - unsorted Object array
	 * @param b - the column number of array
	 * @return sorted array
	 */
	public static Object[][] dataSort(Object[][] a, int b){
		Object[][] dataArray = a;
		int array_size = a.length;
		if (a[0].length <= b) {
			return null;
		}
		//Build-Max-Heap
		for(int i = new Double(Math.floor((array_size-1)/2)).intValue(); i>=0; i--){
			//Max-Heap
			Object[] key = dataArray[i];
			int largest = 0;
			do {
				largest = (i+1)*2-1;
				if (((i+1)*2<array_size)&&(new Double(dataArray[(i+1)*2][b].toString()).doubleValue()>new Double(dataArray[(i+1)*2-1][b].toString()).doubleValue())) {
					largest = (i+1)*2;
				}
				if (((i+1)*2-1<array_size)&&(new Double(key[b].toString()).doubleValue()<new Double(dataArray[largest][b].toString()).doubleValue())){
					dataArray[i] = dataArray[largest];
					i = largest;
				} else {
					dataArray[i] = key;
				}
			} while(key != dataArray[i]);
		}
		for(int i = array_size-1; i>=1; i--){
			Object[] key = dataArray[i];
			dataArray[i] = dataArray[0];
			dataArray[0] = key;
			array_size = array_size - 1;
			int j = 0;
			key = dataArray[j];
			int largest = 0;
			do {
				largest = (j+1) * 2 - 1;
				if (((j+1) * 2<array_size)&&(new Double(dataArray[(j+1) * 2][b].toString()).doubleValue()>new Double(dataArray[(j+1) * 2 - 1][b].toString()).doubleValue())) {
					largest = (j+1) * 2;
				}
				if (((j+1) * 2-1<array_size)&&(new Double(key[b].toString()).doubleValue()<new Double(dataArray[largest][b].toString()).doubleValue())){
					dataArray[j] = dataArray[largest];
					j = largest;
				} else {
					dataArray[j] = key;
				}
			} while(key != dataArray[j]);
		}
		return dataArray;
	}

    /**
	 * Generate the unique value list of the selected attribute
	 */
	public static ArrayList<Object> setupNodeAttributeValues(String attributeName) {
		CyAttributes attribs = Cytoscape.getNodeAttributes();
		Map attrMap = CyAttributesUtils.getAttribute(attributeName, attribs);
        Collection values = attrMap.values();
		ArrayList<Object> uniqueValueList = new ArrayList<Object>();

		// key will be a List attribute value, so we need to pull out individual
		// list items
        for (Object o : values) {
            List oList = (List) o;
            if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
                for (int j = 0; j < oList.size(); j++) {
                    Object jObj = oList.get(j);
                    if (jObj != null) {
                        if (!uniqueValueList.contains(jObj)) {
                            uniqueValueList.add(jObj);
                        }
                    }
                }
            } else {
                if (o != null) {
                    if (!uniqueValueList.contains(o)) {
                        uniqueValueList.add(o);
                    }
                }
            }
        }
		return uniqueValueList;
	}

    /**
	 *
	 */
	public static void retrieveNodeCountMapBatchMode(Map<String, Set<String>>[] idGOMapArray, 
            Set<String> nodeList, Map<String, Set<String>> geneGOCountMap, List potentialGOList) {
        for(String node : nodeList) {
            for(int i=0;i<3;i++){
                Set<String> GOList = idGOMapArray[i].get(node);
                for(String GOID : GOList) {
                    if(potentialGOList.indexOf(GOID)!=-1){
                        if(geneGOCountMap.containsKey(GOID)) {
                            Set<String> tempSet = geneGOCountMap.get(GOID);
                            tempSet.add(node);
                            geneGOCountMap.put(GOID, tempSet);
                        } else {
                            Set<String> tempSet = new HashSet<String>();
                            tempSet.add(node);
                            geneGOCountMap.put(GOID, tempSet);
                        }
                    }
                }
            }
        }
    }

    /**
	 * 
	 */
	public static void retrieveNodeCountMap(String attributeName, Map<String, String> geneGOCountMap, ArrayList<Object> potentialGOList) {
        List<CyNode> wholeNetNodes = Cytoscape.getCurrentNetwork().nodesList();
        CyAttributes attribs = Cytoscape.getNodeAttributes();
		Map attrMap = CyAttributesUtils.getAttribute(attributeName, attribs);
		if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
             for(CyNode o : wholeNetNodes){
                if(attrMap.containsKey(o.getIdentifier())){
                    List oList = (List) attrMap.get(o.getIdentifier());
                    for (int j = 0; j < oList.size(); j++) {
                        Object jObj = oList.get(j);
                        if(potentialGOList.indexOf(jObj)!=-1){
                            if(geneGOCountMap.containsKey(jObj)) {
                                geneGOCountMap.put(jObj.toString(), new Integer(geneGOCountMap.get(jObj)).intValue()+1+"");
                            } else {
                                geneGOCountMap.put(jObj.toString(), "1");
                            }
                        }
                    }
                }
            }
        } else {
            for(CyNode o : wholeNetNodes){
                if(attrMap.containsKey(o.getIdentifier())){
                    Object oValue = attrMap.get(o.getIdentifier());
                    if (oValue != null) {
                        if(potentialGOList.indexOf(oValue)!=-1){
                            if(geneGOCountMap.containsKey(oValue)) {
                                geneGOCountMap.put(oValue.toString(), new Integer(geneGOCountMap.get(oValue)).intValue()+1+"");
                            } else {
                                geneGOCountMap.put(oValue.toString(), "1");
                            }
                        }
                    }
                }
            }
        }
	}

    /**
	 *
	 */
	public static void retrieveEdgeCountMap(String attributeName, Map<String, String> geneGOCountMap, ArrayList<Object> potentialGOList, String edgeAlg) {
        List<CyEdge> wholeNetEdges = Cytoscape.getCurrentNetwork().edgesList();
        CyAttributes attribs = Cytoscape.getNodeAttributes();
		Map attrMap = CyAttributesUtils.getAttribute(attributeName, attribs);
        for(CyEdge edge:wholeNetEdges){
            int nodeInt1 = Cytoscape.getRootGraph().getEdgeSourceIndex(edge.getRootGraphIndex());
            int nodeInt2 = Cytoscape.getRootGraph().getEdgeTargetIndex(edge.getRootGraphIndex());
            String node1 = Cytoscape.getCurrentNetwork().getNode(nodeInt1).getIdentifier();
            String node2 = Cytoscape.getCurrentNetwork().getNode(nodeInt2).getIdentifier();
            List edgeAnnotation;
            if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
                if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection((List)attrMap.get(node1), (List)attrMap.get(node2));
                else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                    edgeAnnotation = EdgeAnnotationMethod.edgeUnion((List)attrMap.get(node1), (List)attrMap.get(node2));
                else
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection((List)attrMap.get(node1), (List)attrMap.get(node2));
            } else {
                if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection(attrMap.get(node1), attrMap.get(node2));
                else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                    edgeAnnotation = EdgeAnnotationMethod.edgeUnion(attrMap.get(node1), attrMap.get(node2));
                else
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection(attrMap.get(node1), attrMap.get(node2));
            }
            for (Object obj:edgeAnnotation) {
                if (obj != null) {
                    if(potentialGOList.indexOf(obj)!=-1){
                        if(geneGOCountMap.containsKey(obj)) {
                            geneGOCountMap.put(obj.toString(), new Integer(geneGOCountMap.get(obj)).intValue()+1+"");
                        } else {
                            geneGOCountMap.put(obj.toString(), "1");
                        }
                    }
                }
            }
        }
	}
    
    /**
	 *
	 */
	public static void retrieveEdgeCountMapBatchMode(Map<String, Set<String>>[] idGOMapArray, 
            Set<String> allEdgeSet, Map<String, Set<String>> geneGOCountMap, List potentialGOList, String edgeAlg) {
        for(String edge:allEdgeSet){
            String[] nodesArray = edge.split("\t");
            Set<String> edgeAnnotation = new HashSet();
            if(nodesArray.length>1){
                for(int i=0;i<3;i++){
                    List<String> nodeGOList1 = new ArrayList((Set<String>)idGOMapArray[i].get(nodesArray[0]));
                    List<String> nodeGOList2 = new ArrayList((Set<String>)idGOMapArray[i].get(nodesArray[1]));
                    if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                        edgeAnnotation.addAll(EdgeAnnotationMethod.edgeIntersection(nodeGOList1, nodeGOList2));
                    else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                        edgeAnnotation.addAll(EdgeAnnotationMethod.edgeUnion(nodeGOList1, nodeGOList2));
                    else
                        edgeAnnotation.addAll(EdgeAnnotationMethod.edgeIntersection(nodeGOList1, nodeGOList2));
                }
            }
            for(String GOID : edgeAnnotation) {
                if(potentialGOList.indexOf(GOID)!=-1){
                    if(geneGOCountMap.containsKey(GOID)) {
                        Set<String> tempSet = geneGOCountMap.get(GOID);
                        tempSet.add(edge.replace("\t", "-"));
                        geneGOCountMap.put(GOID, tempSet);
                    } else {
                        Set<String> tempSet = new HashSet<String>();
                        tempSet.add(edge.replace("\t", "-"));
                        geneGOCountMap.put(GOID, tempSet);
                    }
                }
            }
        }
	}

    /**
	 *
	 */
	public static int retrieveAllNodeCountMap(String goFilePath, Map<String, String> goNodeCountRefMap, List potentialGOList) {
        int ret = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(goFilePath));
            String inputLine=in.readLine();
            while ((inputLine = in.readLine()) != null) {
                String[] retail = inputLine.split("\t");
                for(int i=1;i<retail.length;i++) {
                    String[] temp = retail[i].split(",");
                    for(String str:temp){
                        if(potentialGOList.contains(str)) {
                            if(goNodeCountRefMap.containsKey(str)) {
                                goNodeCountRefMap.put(str, new Integer(goNodeCountRefMap.get(str)).intValue()+1+"");
                            } else {
                                goNodeCountRefMap.put(str, "1");
                            }
                        }
                    }
                }
                ret++;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
	 *  old function, running time O(n^2)
	 */
	public static void retrieveAllEdgeCountMap(String attributeName, Map<String, String> goNodeCountRefMap, ArrayList<Object> potentialGOList, String edgeAlg) {
        Object[] wholeNetNodes = Cytoscape.getCurrentNetwork().nodesList().toArray();
        CyAttributes attribs = Cytoscape.getNodeAttributes();
		Map attrMap = CyAttributesUtils.getAttribute(attributeName, attribs);
       
        for(int i=0;i<wholeNetNodes.length;i++){
            CyNode node1 = (CyNode)wholeNetNodes[i];
            for(int j=i+1;j<wholeNetNodes.length;j++){
                CyNode node2 = (CyNode)wholeNetNodes[j];
                if(!node1.equals(node2)){
                    List edgeAnnotation;
                    if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
                        if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                            edgeAnnotation = EdgeAnnotationMethod.edgeIntersection((List)attrMap.get(node1.getIdentifier()), (List)attrMap.get(node2.getIdentifier()));
                        else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                            edgeAnnotation = EdgeAnnotationMethod.edgeUnion((List)attrMap.get(node1.getIdentifier()), (List)attrMap.get(node2.getIdentifier()));
                        else
                            edgeAnnotation = EdgeAnnotationMethod.edgeIntersection((List)attrMap.get(node1.getIdentifier()), (List)attrMap.get(node2.getIdentifier()));
                    } else {
                        if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                            edgeAnnotation = EdgeAnnotationMethod.edgeIntersection(attrMap.get(node1.getIdentifier()), attrMap.get(node2.getIdentifier()));
                        else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                            edgeAnnotation = EdgeAnnotationMethod.edgeUnion(attrMap.get(node1.getIdentifier()), attrMap.get(node2.getIdentifier()));
                        else
                            edgeAnnotation = EdgeAnnotationMethod.edgeIntersection(attrMap.get(node1.getIdentifier()), attrMap.get(node2.getIdentifier()));
                    }
                    for (Object obj:edgeAnnotation) {
                        if (obj != null) {
                            if(potentialGOList.indexOf(obj)!=-1){
                                if(goNodeCountRefMap.containsKey(obj)) {
                                    goNodeCountRefMap.put(obj.toString(), new Integer(goNodeCountRefMap.get(obj)).intValue()+1+"");
                                } else {
                                    goNodeCountRefMap.put(obj.toString(), "1");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
	 *  new function, running time O(n)
	 */
	public static void retrieveAllEdgeCountMap(HashMap<String, Set<String>> goNodeMap, Map<String, String> goNodeCountRefMap, ArrayList<Object> potentialGOList, String edgeAlg, int numOfNode) {
        for (Object obj:potentialGOList) {
            if(goNodeMap.containsKey(obj)){
                int numOfAnn = goNodeMap.get(obj).size();
                int totalEdges = 0;
                if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection)) {
                    totalEdges = numOfAnn*(numOfAnn-1)/2;
                } else if (edgeAlg.equals(NOAStaticValues.EDGE_Union)) {
                    totalEdges = numOfAnn*(numOfNode-1)-numOfAnn*(numOfAnn-1)/2;
                } else {
                    totalEdges = numOfAnn*(numOfAnn-1)/2;
                }
                goNodeCountRefMap.put(obj.toString(), totalEdges+"");
            } else {
                //goNodeCountRefMap.put(obj.toString(), totalEdges+"");
                System.out.print(obj+" doesn't exist...... ");
            }
        }
    }

    /**
	 *
	 */
	public static void retrieveAllEdgeCountMapBatchMode(Map<String, Set<String>>[] idGOMapArray,
            Set<String> allNodeSet, Map<String, String> geneGOCountMap, List potentialGOList, String edgeAlg) {
        Object[] nodesArray = allNodeSet.toArray();
        for(int i=0;i<nodesArray.length;i++) {
            for(int j=i+1;j<nodesArray.length;j++){
                Set<String> edgeAnnotation = new HashSet();
                for(int n=0;n<3;n++){
                    List<String> nodeGOList1 = new ArrayList((Set<String>)idGOMapArray[n].get(nodesArray[i]));
                    List<String> nodeGOList2 = new ArrayList((Set<String>)idGOMapArray[n].get(nodesArray[j]));
                    if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                        edgeAnnotation.addAll(EdgeAnnotationMethod.edgeIntersection(nodeGOList1, nodeGOList2));
                    else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                        edgeAnnotation.addAll(EdgeAnnotationMethod.edgeUnion(nodeGOList1, nodeGOList2));
                    else
                        edgeAnnotation.addAll(EdgeAnnotationMethod.edgeIntersection(nodeGOList1, nodeGOList2));
                }
                for(String GOID : edgeAnnotation) {
                    if(potentialGOList.indexOf(GOID)!=-1){
                        if(geneGOCountMap.containsKey(GOID)) {
                            geneGOCountMap.put(GOID, new Integer(geneGOCountMap.get(GOID)).intValue()+1+"");
                        } else {
                            geneGOCountMap.put(GOID, "1");
                        }
                    }
                }
            }
        }
    }

    /**
	 * Generate the unique value list of the selected attribute with partial network
	 */
	public static ArrayList<Object> retrieveNodeAttribute(String attributeName, Set<CyNode> selectedNetwork, Map<String, Set<String>> goGeneMap) {
        CyAttributes attribs = Cytoscape.getNodeAttributes();
		Map attrMap = CyAttributesUtils.getAttribute(attributeName, attribs);
		ArrayList<Object> uniqueValueList = new ArrayList<Object>();
        // key will be a List attribute value, so we need to pull out individual
		// list items
        if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
             for(CyNode o : selectedNetwork){
                if(attrMap.containsKey(o.getIdentifier())){
                    List oList = (List) attrMap.get(o.getIdentifier());
                    for (int j = 0; j < oList.size(); j++) {
                        Object jObj = oList.get(j);
                        if (jObj != null) {
                            if (!uniqueValueList.contains(jObj)) {
                                uniqueValueList.add(jObj);
                            }
                            if(goGeneMap.containsKey(jObj)) {
                                Set<String> tempSet = goGeneMap.get(jObj);
                                tempSet.add(o.getIdentifier());
                                goGeneMap.put(jObj.toString(), tempSet);
                            } else {
                                Set<String> tempSet = new HashSet<String>();
                                tempSet.add(o.getIdentifier());
                                goGeneMap.put(jObj.toString(), tempSet);
                            }
                        }
                    }
                }
            }
        } else {
            for(CyNode o : selectedNetwork){
                if(attrMap.containsKey(o.getIdentifier())){
                    Object oValue = attrMap.get(o.getIdentifier());
                    if (oValue != null) {
                        if (!uniqueValueList.contains(oValue)) {
                            uniqueValueList.add(oValue);
                        }
                        if(goGeneMap.containsKey(oValue)) {
                            Set<String> tempSet = goGeneMap.get(oValue);
                            tempSet.add(o.getIdentifier());
                            goGeneMap.put(oValue.toString(), tempSet);
                        } else {
                            Set<String> tempSet = new HashSet<String>();
                            tempSet.add(o.getIdentifier());
                            goGeneMap.put(oValue.toString(), tempSet);
                        }
                    }
                }
            }
        }
        return uniqueValueList;
	}

    /**
	 * Generate the unique value list of the selected attribute with partial network
	 */
	public static ArrayList<Object> retrieveEdgeAttribute(String attributeName, Set<CyEdge> selectedNetwork, Map<String, Set<String>> goGeneMap, String edgeAlg) {
        CyAttributes attribs = Cytoscape.getNodeAttributes();
		Map attrMap = CyAttributesUtils.getAttribute(attributeName, attribs);
		ArrayList<Object> uniqueValueList = new ArrayList<Object>();
        // key will be a List attribute value, so we need to pull out individual
		// list items
        for(CyEdge edge:selectedNetwork){
            int nodeInt1 = Cytoscape.getRootGraph().getEdgeSourceIndex(edge.getRootGraphIndex());
            int nodeInt2 = Cytoscape.getRootGraph().getEdgeTargetIndex(edge.getRootGraphIndex());
            String node1 = Cytoscape.getCurrentNetwork().getNode(nodeInt1).getIdentifier();
            String node2 = Cytoscape.getCurrentNetwork().getNode(nodeInt2).getIdentifier();
            List edgeAnnotation;
            if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
                if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection((List)attrMap.get(node1), (List)attrMap.get(node2));
                else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                    edgeAnnotation = EdgeAnnotationMethod.edgeUnion((List)attrMap.get(node1), (List)attrMap.get(node2));
                else
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection((List)attrMap.get(node1), (List)attrMap.get(node2));
            } else {
                if(edgeAlg.equals(NOAStaticValues.EDGE_Intersection))
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection(attrMap.get(node1), attrMap.get(node2));
                else if (edgeAlg.equals(NOAStaticValues.EDGE_Union))
                    edgeAnnotation = EdgeAnnotationMethod.edgeUnion(attrMap.get(node1), attrMap.get(node2));
                else
                    edgeAnnotation = EdgeAnnotationMethod.edgeIntersection(attrMap.get(node1), attrMap.get(node2));
            }
            for (Object obj:edgeAnnotation) {
                if (obj != null) {
                    if (!uniqueValueList.contains(obj)) {
                        uniqueValueList.add(obj);
                    }
                    if(goGeneMap.containsKey(obj)) {
                        Set<String> tempSet = goGeneMap.get(obj);
                        tempSet.add(edge.getIdentifier());
                        goGeneMap.put(obj.toString(), tempSet);
                    } else {
                        Set<String> tempSet = new HashSet<String>();
                        tempSet.add(edge.getIdentifier());
                        goGeneMap.put(obj.toString(), tempSet);
                    }
                }
            }
        }
        return uniqueValueList;
	}
    
    public static String[] parseSpeciesList(List<String> speciesList) {
        String[] result = new String[speciesList.size()];
        for(int i=0; i<result.length; i++){
            String[] temp = speciesList.get(i).split("\t");
            result[i] = temp[0];
        }
        return result;
    }
}
