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

import java.util.ArrayList;
import java.util.List;

public class EdgeAnnotationMethod {

    public EdgeAnnotationMethod() {
    }

    public static List<String> edgeIntersection(List<String> node1GOList, List<String> node2GOList) {
        List<String> ret = new ArrayList();
        for(String str:node1GOList) {
            if(node2GOList.contains(str))
                ret.add(str);
        }
        return ret;
    }

    public static List<String> edgeIntersection(Object node1GOList, Object node2GOList) {
        List<String> ret = new ArrayList();
        if(node1GOList.equals(node2GOList))
            ret.add(node1GOList.toString());
        return ret;
    }

    public static List<String> edgeUnion(List<String> node1GOList, List<String> node2GOList) {
        List<String> ret = node1GOList;
        for(String str:node2GOList) {
            if(!node1GOList.contains(str))
                ret.add(str);
        }
        return ret;
    }

    public static List<String> edgeUnion(Object node1GOList, Object node2GOList) {
        List<String> ret = new ArrayList();
        ret.add(node1GOList.toString());
        if(!node1GOList.equals(node2GOList))
            ret.add(node2GOList.toString());
        return ret;
    }
}
