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
            double pvalue = new Double(resultMap.get(go)).doubleValue();
            System.out.print(go+" "+pvalue+" ");
            pvalue = pvalue*total>1?1:pvalue*total;
            if(pvalue<=pvalueCutoff)
                reducedMap.put(go, pvalue+"");
            System.out.println(pvalue);
        }
        
		return reducedMap;
	}

    public static HashMap<String, String> calBenjamCorrection(HashMap<String, String> resultMap, int total, double pvalueCutoff) {
        HashMap<String, String> reducedMap = new HashMap<String, String>();
        Object[][] goPvalueArray = new String[resultMap.size()][2];
		Set<String> goList = resultMap.keySet();
        int i=0;
        for(String go:goList) {
            goPvalueArray[i][0] = go;
            goPvalueArray[i][1] = resultMap.get(go);
            i++;
        }
        goPvalueArray = NOAUtil.dataSort(goPvalueArray, 1);
        for(i=0;i<goPvalueArray.length;i++){
            double pvalue = new Double(goPvalueArray[i][1].toString()).doubleValue();
            System.out.print(goPvalueArray[i][0].toString()+" "+pvalue+" ");
            pvalue = pvalue*total/(i+1);
            pvalue = pvalue>1?1:pvalue;
            if(pvalue<=pvalueCutoff)
                reducedMap.put(goPvalueArray[i][0].toString(), pvalue+"");
            System.out.println(pvalue);
        }
        return resultMap;
	}
}
