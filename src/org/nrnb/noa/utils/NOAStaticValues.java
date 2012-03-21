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

public class NOAStaticValues {
    public static final String BP_ATTNAME = "annotation.GO BIOLOGICAL_PROCESS";
    public static final String CC_ATTNAME = "annotation.GO CELLULAR_COMPONENT";
    public static final String MF_ATTNAME = "annotation.GO MOLECULAR_FUNCTION";
    public static String[] speciesList = {"Human", "Mouse", "Yeast", "Arabidopsis"};
    public static final String supportedSpecieslist = "/supported_species.tab";
    public static final String bridgedbSpecieslist = "/resources/organisms.txt";
    public static final String genmappcsDatabaseDir = "http://genmapp.org/genmappcs/databases/";
    public static final String bridgedbDerbyDir = "http://bridgedb.org/data/gene_database/";
    public static final String GO_DescFile = "/resources/go_annotations.txt";
    public static final String Algorithm_EDGE = "Edge-based";
    public static final String Algorithm_NODE = "Node-based";
    public static final String EDGE_Intersection = "Intersection";
    public static final String EDGE_Union = "Union";
    public static final String STAT_Hypergeo = "Hyper-geometry";
    public static final String STAT_Fisher = "Fisher exact test";
    public static final String CORRECTION_Bonfer = "Bonferroni";
    public static final String CORRECTION_Benjam = "Benjamini & Hochberg q value";
    public static final int NETWORK_FORMAT  = 1;
    public static final int SET_FORMAT  = 2;
    public static final int WRONG_FORMAT  = -1;
    public static final double LOG_PVALUE_CUTOFF = -20.0;
}
