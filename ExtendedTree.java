/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2008 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.learner.meta.thesis;

import java.util.Iterator;

import com.rapidminer.operator.learner.tree.Edge;
import com.rapidminer.operator.learner.tree.Tree;

public class ExtendedTree extends Tree {

    private static final long serialVersionUID = -5930873149086170840L;

    private long totalExamples = 0;
    
    public ExtendedTree(Tree originalTree) {
    	super(originalTree.getTrainingSet());
    	
    	// Copy label
    	super.setLeaf(originalTree.getLabel());
    	
    	// Copy CounterMap
        Iterator<String> s = originalTree.getCounterMap().keySet().iterator();
        while (s.hasNext()) {
            String className = s.next();
            int count = originalTree.getCount(className);
            totalExamples += count;
            super.addCount(className, count);
        }

        // Copy Children
        Iterator<Edge> childIterator = originalTree.childIterator();
        while (childIterator.hasNext()) {
            Edge edge = childIterator.next();
            ExtendedTree newChild = new ExtendedTree(edge.getChild());
            totalExamples += newChild.totalExamples;
            super.addChild(newChild, edge.getCondition());
        }	
    }
    
    public long getTotalExamples() { 
        return this.totalExamples;
    }
    

}
