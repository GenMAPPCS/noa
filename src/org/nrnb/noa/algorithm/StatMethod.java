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

public class StatMethod {

    public StatMethod() {
    }
    
    public static double calHyperGeoPValue(int vA, int vB, int vC, int vD) {
		double sum = 0;
        int upperBound = vB>vC?vC:vB;
		for(int i=vA;i<=upperBound;i++) {
            sum += Math.exp(getCombinationValue(i, vB, vC, vD));
		}
		return sum;
	}

    public static double calFisherTestPValue(int vA, int vB, int vC, int vD) {
		double sum = 0;
        double exactValue = Math.exp(getCombinationValue(vA, vB, vC, vD));
        int upperBound = vB>vC?vC:vB;
		for(int i=0;i<=upperBound;i++) {
            double temp = Math.exp(getCombinationValue(i, vB, vC, vD));
            if(temp<=exactValue){
                sum += temp;
            }
		}
		return sum;
	}

	public static double getLogFact(int num) {
		if(num<=0) {
			return 0;
		} else if(num>10) {
			double sum = 0;
			sum = (double)num*Math.log((double)num)+Math.log((double)(num+4*Math.pow(num, 2)+8*Math.pow(num, 3)))/6d-(double)num+Math.log(Math.PI)/2d;
			return sum;//Math.log(Math.PI)/2d-(double)num+(double)num*Math.log((double)num)+Math.log((double)(num+4*Math.pow(num, 2)+8*Math.pow(num, 3)))/2d;
		} else {
			double sum = 0;
			for(int i=1;i<=num;i++) {
				sum += Math.log((double)i);
			}
			return sum;
		}
	}

    public static double getCombinationValue(int vA, int vB, int vC, int vD) {
		double result = getLogFact(vB) + getLogFact(vD-vB) + getLogFact(vC) + getLogFact(vD-vC)
		- getLogFact(vA) - getLogFact(vB-vA) - getLogFact(vD) - getLogFact(vC-vA) - getLogFact(vD-vB-vC+vA);
		return result;
	}
}
