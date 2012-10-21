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

import java.util.Comparator;
import java.math.BigDecimal;
import java.util.Vector;

/**
 *
 */
public class NOAColumnComparator implements Comparator {
	private static final int EMPTY_STR_LENGTH = 2;
	protected int index;
	protected String internalColumnName;
	protected boolean ascending;

	/**
	 * Creates a new NOAColumnComparator object.
	 *
	 * @param index  DOCUMENT ME!
	 * @param ascending  DOCUMENT ME!
	 */
	public NOAColumnComparator(final int index, final String columnName, final boolean ascending) {
		this.index = index;
		this.internalColumnName = columnName;
		this.ascending = ascending;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param obj1 DOCUMENT ME!
	 * @param obj2 DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public int compare(final Object obj1, final Object obj2) {
		if (obj1 instanceof Vector && obj2 instanceof Vector) {
			final Object firstObj = ((Vector) obj1).elementAt(index);
			final Object secondObj = ((Vector) obj2).elementAt(index);
                        
			if ((firstObj == null) && (secondObj == null)) {
				return 0;
			} else if (firstObj == null) {
				return 1;
			} else if (secondObj == null) {
				return -1;
                        }else {
				if (internalColumnName == "P-value") {
                                    String newVal1 = firstObj.toString();
                                    if(firstObj.toString().contains("E")) {
                                        double d = Double.parseDouble(firstObj.toString());
                                        newVal1 = (new BigDecimal(Double.toString(d))).toPlainString();
                                    }
                                    String newVal2 = secondObj.toString();
                                    if(secondObj.toString().contains("E")) {
                                        double d = Double.parseDouble(secondObj.toString());
                                        newVal2 = (new BigDecimal(Double.toString(d))).toPlainString();
                                    }
                                    return ascending ? stringCompare(newVal1.toString(), newVal2.toString()):
				                   stringCompare(newVal2.toString(), newVal1.toString());
                                } else if ((internalColumnName == "Test")||(internalColumnName == "Reference")) {
                                    String newVal1 = firstObj.toString();
                                    newVal1 = newVal1.substring(0,newVal1.indexOf("/")).trim();
                                    String newVal2 = secondObj.toString();
                                    newVal2 = newVal2.substring(0,newVal2.indexOf("/")).trim();
					return ascending ? integerCompare(new Integer(newVal1).intValue(), new Integer(newVal2).intValue()) :
					                   integerCompare(new Integer(newVal2).intValue(), new Integer(newVal1).intValue());
                                } else {
				return ascending ? stringCompare(firstObj.toString(), secondObj.toString()):
				                   stringCompare(secondObj.toString(), firstObj.toString());
                                }
			}
		}

		return 1;
	}

	private static int doubleCompare(final double d1, final double d2) {
		if (d1 < d2)
			return -1;
		return d1 > d2 ? +1 : 0;
	}

	private static int longCompare(final long l1, final long l2) {
		if (l1 < l2)
			return -1;
		return l1 > l2 ? +1 : 0;
	}

	private static int integerCompare(final int i1, int i2) {
		if (i1 < i2)
			return -1;
		return i1 > i2 ? +1 : 0;
	}

	private static int booleanCompare(final boolean b1, final boolean b2) {
		if ((b1 && b2) || (!b1 && !b2))
			return 0;
		return b1 ? -1 : +1;
	}

	private static int stringCompare(final String s1, final String s2) {
		return s1.compareToIgnoreCase(s2);
	}

	/**
	 * Comparing numbers.
	 *
	 * @param number1
	 * @param number2
	 * @return
	 */
	public int compare(final Number number1, final Number number2) {
		final double firstNumber = number1.doubleValue();
		final double secondNumber = number2.doubleValue();

		if (firstNumber < secondNumber) {
			return -1;
		} else if (firstNumber > secondNumber) {
			return 1;
		} else {
			return 0;
		}
	}
}
