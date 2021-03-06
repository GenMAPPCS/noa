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
package org.nrnb.noa.algorithm;

import java.util.HashMap;
import java.util.Set;
import org.nrnb.noa.utils.NOAUtil;

public class CorrectionMethod {

    public CorrectionMethod() {
    }
    
    public static HashMap<String, String> calBonferCorrection(HashMap<String, String> resultMap, int total, double pvalueCutoff) {
        HashMap<String, String> reducedMap = new HashMap<String, String>();
		Set<String> goList = resultMap.keySet();
        for(String go:goList) {
            String[] temp = resultMap.get(go).toString().split("\t");
            double pvalue = new Double(temp[0]).doubleValue();
            pvalue = pvalue*total>1?1:pvalue*total;
            if(pvalue<=pvalueCutoff)
                reducedMap.put(go, pvalue+"\t"+temp[1]+"\t"+temp[2]);
        }        
		return reducedMap;
	}

    public static double[] calBonferCorrection(double[] pvalueArray, int total) {
        double[] result = new double[pvalueArray.length];
        for(int i=0;i<result.length;i++) {
            if(pvalueArray[i]==0||pvalueArray[i]==-1000) {
                result[i]=pvalueArray[i];
            } else {
                result[i] = Math.log(Math.exp(pvalueArray[i])*total>1?1:Math.exp(pvalueArray[i])*total);
                result[i] = result[i]<-1000?-1000:result[i];
            }
        }
		return result;
	}

    public static HashMap<String, String> calBenjamCorrection(HashMap<String, String> resultMap, int total, double pvalueCutoff) {
        HashMap<String, String> reducedMap = new HashMap<String, String>();
        Object[][] goPvalueArray = new String[resultMap.size()][2];
		Set<String> goList = resultMap.keySet();
        int i=0;
        for(String go:goList) {
            goPvalueArray[i][0] = go;
            String[] temp = resultMap.get(go).toString().split("\t");
            goPvalueArray[i][1] = temp[0];
            i++;
        }
        goPvalueArray = NOAUtil.dataSort(goPvalueArray, 1);
        for(i=0;i<goPvalueArray.length;i++){
            String[] temp = resultMap.get(goPvalueArray[i][0]).toString().split("\t");
            double pvalue = new Double(goPvalueArray[i][1].toString()).doubleValue();
            pvalue = pvalue*total/(i+1);
            pvalue = pvalue>1?1:pvalue;
            if(pvalue<=pvalueCutoff)
                reducedMap.put(goPvalueArray[i][0].toString(), pvalue+"\t"+temp[1]+"\t"+temp[2]);
        }
        return resultMap;
	}

    public static double[] calBenjamCorrection(double[] pvalueArray, int total) {
        double[] result = new double[pvalueArray.length];
        double[][] goPvalueArray = new double[pvalueArray.length][2];
        for(int i=0;i<result.length;i++) {
            goPvalueArray[i][0] = i;
            goPvalueArray[i][1] = result[i];
        }
        goPvalueArray = NOAUtil.dataSort(goPvalueArray, 1);
        for(int i=0;i<result.length;i++){
            if(pvalueArray[i]==0||pvalueArray[i]==-1000) {
                result[(int)goPvalueArray[i][0]] = pvalueArray[i];
            } else {
                double pvalue = Math.exp(goPvalueArray[i][1]);
                pvalue = pvalue*total/(i+1);
                pvalue = Math.log(pvalue>1?1:pvalue);
                result[(int)goPvalueArray[i][0]] = pvalue<-1000?-1000:pvalue;
            }
        }
		return result;
	}
}
