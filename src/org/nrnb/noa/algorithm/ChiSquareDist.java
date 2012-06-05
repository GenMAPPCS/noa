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
package org.nrnb.noa.algorithm;

public class ChiSquareDist {
    static private double
        third = 1.0 / 3.0,
        zero = 0.0,
        one = 1.0,
        two = 2.0,
        oflo = 1.0e+37,
        three = 3.0,
        nine = 9.0,
        xbig = 1.0e+8,
        plimit = 1000.0e0,
        elimit = -88.0e0;
    static private double  SIXTEN = 1.6;
        /* 30 Decimal-place constants computed with bc -l (scale=32; proper round) */

    /* sqrt(32) */
    public static final double  M_SQRT_32 = 5.656854249492380195206754896838;
    /*1/sqrt(2pi)*/
    public static final double  M_1_SQRT_2PI = 0.398942280401432677939946059934;
    /* Difference between 1.0 and the minimum float/double greater than 1.0 */
    public static final double DBL_EPSILON = 2.2204460492503131e-16;
    
    public static double chiSquareCDF(double x, double df) {
        return gammaCDF(x, df / 2.0, 2.0);
    }

    public static double gammaCDF(double x, double p, double scale) {
        double pn1, pn2, pn3, pn4, pn5, pn6, arg, c, rn, a, b, an;
        double sum;
    
        /* check that we have valid values for x and p */
        if (Double.isNaN(x) || Double.isNaN(p) || Double.isNaN(scale))
            return x + p + scale;
        if(p <= zero || scale <= zero) {
            throw new java.lang.ArithmeticException("Math Error: DOMAIN");
        }
        x = x / scale;
        if (x <= zero)
            return 0.0;
    
        /* use a normal approximation if p > plimit */    
        if (p > plimit) {
            pn1 = Math.sqrt(p) * three * (Math.pow(x/p, third) + one / (p * nine) - one);
            return normalCDF(pn1, 0.0, 1.0);
        }
    
        /* if x is extremely large compared to p then return 1 */    
        if (x > xbig)
            return one;
        if (x <= one || x < p) {
            /* use pearson's series expansion. */
            arg = p * Math.log(x) - x - lnGamma(p + one);
            c = one;
            sum = one;
            a = p;
            do {
                a = a + one;
                c = c * x / a;
                sum = sum + c;
            } while (c > DBL_EPSILON);
            arg = arg + Math.log(sum);
            sum = zero;
            if (arg >= elimit)
                sum = Math.exp(arg);
        } else {
            /* use a continued fraction expansion */
            arg = p * Math.log(x) - x - lnGamma(p);
            a = one - p;
            b = a + x + one;
            c = zero;
            pn1 = one;
            pn2 = x;
            pn3 = x + one;
            pn4 = x * b;
            sum = pn3 / pn4;
            for (;;) {
                a = a + one;
                b = b + two;
                c = c + one;
                an = a * c;
                pn5 = b * pn3 - an * pn1;
                pn6 = b * pn4 - an * pn2;
                if (Math.abs(pn6) > zero) {
                    rn = pn5 / pn6;
                    if (Math.abs(sum - rn) <= Math.min(DBL_EPSILON, DBL_EPSILON * rn))
                        break;
                    sum = rn;
                }
                pn1 = pn3;
                pn2 = pn4;
                pn3 = pn5;
                pn4 = pn6;
                if (Math.abs(pn5) >= oflo) {
                    /* re-scale the terms in continued fraction */
                    /* if they are large */
                    pn1 = pn1 / oflo;
                    pn2 = pn2 / oflo;
                    pn3 = pn3 / oflo;
                    pn4 = pn4 / oflo;
                }
            }
            arg = arg + Math.log(sum);
            sum = one;
            if (arg >= elimit)
                sum = one - Math.exp(arg);
        }
        return sum;
    }

    /** The Normal Density Function */
    public static double normalDensity(double x, double mu, double sigma)
    {
        if (Double.isNaN(x) || Double.isNaN(mu) || Double.isNaN(sigma))
            return x + mu + sigma;
        if (sigma <= 0) {
            throw new java.lang.ArithmeticException("Math Error: DOMAIN");
        }

        x = (x - mu) / sigma;
        return M_1_SQRT_2PI *Math.exp(-0.5 * x * x) / sigma;
    }
    
    public static double normalCDF(double x, double mu, double sigma)
    {
        final double c[] = {
    	0.39894151208813466764, 8.8831497943883759412, 93.506656132177855979,
    	597.27027639480026226, 2494.5375852903726711, 6848.1904505362823326,
    	11602.651437647350124, 9842.7148383839780218, 1.0765576773720192317e-8};

        final double d[] = {
    	22.266688044328115691, 235.38790178262499861, 1519.377599407554805,
    	6485.558298266760755, 18615.571640885098091, 34900.952721145977266,
    	38912.003286093271411, 19685.429676859990727};

        final double p[] = {
    	0.21589853405795699, 0.1274011611602473639, 0.022235277870649807,
    	0.001421619193227893466, 2.9112874951168792e-5, 0.02307344176494017303 };

        final double q[] = {
    	1.28426009614491121, 0.468238212480865118, 0.0659881378689285515,
    	0.00378239633202758244, 7.29751555083966205e-5};

        final double a[] = {
    	2.2352520354606839287, 161.02823106855587881, 1067.6894854603709582,
    	18154.981253343561249, 0.065682337918207449113};

        final double b[] = {
    	47.20258190468824187, 976.09855173777669322, 10260.932208618978205,
    	45507.789335026729956};

        double xden, temp, xnum, result, ccum;
        double del, min, eps, xsq;
        double y;
        int i;

        /* Note: The structure of these checks has been */
        /* carefully thought through.  For example, if x == mu */
        /* and sigma == 0, we still get the correct answer. */

        if(Double.isNaN(x) || Double.isNaN(mu) || Double.isNaN(sigma))
            return x + mu + sigma;
        if (sigma < 0) {
            throw new java.lang.ArithmeticException("Math Error: DOMAIN");
        }
        x = (x - mu) / sigma;
        if(Double.isInfinite(x)) {
            if(x < 0)
                return 0;
            else
                return 1;
        }
        eps = DBL_EPSILON * 0.5;
        min = Double.MIN_VALUE;
        y = java.lang.Math.abs(x);
        if (y <= 0.66291) {
            xsq = 0.0;
            if (y > eps) {
                xsq = x * x;
            }
            xnum = a[4] * xsq;
            xden = xsq;
            for (i = 1; i <= 3; ++i) {
                xnum = (xnum + a[i - 1]) * xsq;
                xden = (xden + b[i - 1]) * xsq;
            }
            result = x * (xnum + a[3]) / (xden + b[3]);
            temp = result;
            result = 0.5 + temp;
            ccum = 0.5 - temp;
        } else if (y <= M_SQRT_32) {
            /* Evaluate pnorm for 0.66291 <= |z| <= sqrt(32) */
            xnum = c[8] * y;
            xden = y;
            for (i = 1; i <= 7; ++i) {
                xnum = (xnum + c[i - 1]) * y;
                xden = (xden + d[i - 1]) * y;
            }
            result = (xnum + c[7]) / (xden + d[7]);
            xsq = java.lang.Math.floor(y * SIXTEN) / SIXTEN;
            del = (y - xsq) * (y + xsq);
            result = java.lang.Math.exp(-xsq * xsq * 0.5) * java.lang.Math.exp(-del * 0.5) * result;
            ccum = 1.0 - result;
            if (x > 0.0) {
                temp = result;
                result = ccum;
                ccum = temp;
            }
        } else if(y < 50) {
    	/* Evaluate pnorm for sqrt(32) < |z| < 50 */
            result = 0.0;
            xsq = 1.0 / (x * x);
            xnum = p[5] * xsq;
            xden = xsq;
            for (i = 1; i <= 4; ++i) {
                xnum = (xnum + p[i - 1]) * xsq;
                xden = (xden + q[i - 1]) * xsq;
            }
            result = xsq * (xnum + p[4]) / (xden + q[4]);
            result = (M_1_SQRT_2PI - result) / y;
            xsq = java.lang.Math.floor(x * SIXTEN) / SIXTEN;
            del = (x - xsq) * (x + xsq);
            result = java.lang.Math.exp(-xsq * xsq * 0.5) * java.lang.Math.exp(-del * 0.5) * result;
            ccum = 1.0 - result;
            if (x > 0.0) {
                temp = result;
                result = ccum;
                ccum = temp;
            }
        } else {
            if(x > 0) {
                result = 1.0;
                ccum = 0.0;
            }
            else {
                result = 0.0;
                ccum = 1.0;
            }
        }
        if (result < min)
            result = 0.0;
        if (ccum < min)
            ccum = 0.0;
        return result;
    }
    /**
     * log Gamma function: ln(gamma(alpha)) for alpha>0, accurate to 10 decimal places
     *
     * @param alpha argument
     * @return the log of the gamma function of the given alpha
     */
    public static double lnGamma(double alpha) {
        // Pike MC & Hill ID (1966) Algorithm 291: Logarithm of the gamma function.
        // Communications of the Association for Computing Machinery, 9:684

        double x = alpha, f = 0.0, z;

        if (x < 7) {
            f = 1;
            z = x - 1;
            while (++z < 7) {
                f *= z;
            }
            x = z;
            f = -Math.log(f);
        }
        z = 1 / (x * x);

        return
                f + (x - 0.5) * Math.log(x) - x + 0.918938533204673 +
                        (((-0.000595238095238 * z + 0.000793650793651) *
                                z - 0.002777777777778) * z + 0.083333333333333) / x;
    }
}
