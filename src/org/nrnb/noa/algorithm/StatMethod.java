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

    public static double calZScorePValue(int vA, int vB, int vC, int vD){
        double z = calZScore(vA, vB, vC, vD);
        double x = 0.0;
        double y = 0.0;
        double w = 0.0;
        double Z_MAX = 6.0;
        if (z == 0.0) {
            x = 0.0;
        } else {
            y = 0.5 * Math.abs(z);
            if (y > (Z_MAX * 0.5)) {
                x = 1.0;
            } else if (y < 1.0) {
                w = y * y;
                x = ((((((((0.000124818987 * w
                         - 0.001075204047) * w + 0.005198775019) * w
                         - 0.019198292004) * w + 0.059054035642) * w
                         - 0.151968751364) * w + 0.319152932694) * w
                         - 0.531923007300) * w + 0.797884560593) * y * 2.0;
            } else {
                y -= 2.0;
                x = (((((((((((((-0.000045255659 * y
                               + 0.000152529290) * y - 0.000019538132) * y
                               - 0.000676904986) * y + 0.001390604284) * y
                               - 0.000794620820) * y - 0.002034254874) * y
                               + 0.006549791214) * y - 0.010557625006) * y
                               + 0.011630447319) * y - 0.009279453341) * y
                               + 0.005353579108) * y - 0.002141268741) * y
                               + 0.000535310849) * y + 0.999936657524;
            }
        }
        return z > 0.0 ? ((x + 1.0) * 0.5) : ((1.0 - x) * 0.5);
    }

    public static double calZScore(int vA, int vB, int vC, int vD) {
        double ratio = (double)vB/(double)vD;
        double sum = ((double)vA-(double)vC*ratio)/Math.sqrt(vC*ratio*(1.0-ratio)*(1.0-((double)vC-1.0)/((double)vD-1.0)));
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
