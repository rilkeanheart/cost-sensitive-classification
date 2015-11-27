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

import java.awt.Graphics;
import java.util.Iterator;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.operator.learner.tree.Edge;
import com.rapidminer.operator.learner.tree.SplitCondition;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.tools.Renderable;

/**
 * The tree model is the model created for decision trees. That can produce
 * well calibrated probabilities using "Smoothing" and "Curtailment" as defined
 * in the paper "Learning And Decision Making When Costs and Probabilities Are Unkown"
 * by Charles Elkan.
 * 
 * @author Michael Green
 * @version $Id: SmoothedAndCurtailedTreeModel.java,v 1.8 2008/05/09 19:22:53 greenm9 Exp $
 */
public class SmoothedAndCurtailedTreeModel extends SimplePredictionModel
		implements Renderable {

    private static final long serialVersionUID = 4368631725370998592L;
    
    private ExtendedTree root;

    private final double dBaseRate = 0.05;
    private final double dShiftFactor = 200;
    private final double dCurtailmentThreshold = 200;

	public SmoothedAndCurtailedTreeModel(ExampleSet exampleSet, Tree root) {
		super(exampleSet);
		this.root = new ExtendedTree(root);
	}

	public Tree getRoot() {
		return this.root;
	}
		
	@Override
	public double predict(Example example) throws OperatorException {
		double dPrediction = predictUsingCurtailment(example, root);
		return dPrediction;
	}

	private double predictUsingCurtailment(Example example, ExtendedTree node) {
		double dPrediction = 0;
		
        if (node.isLeaf()) {
        	dPrediction = predictAsLeaf(example, node);
        } else {
            boolean bEdgeConditionFound = false;
            Iterator<Edge> childIterator = node.childIterator();
            while ( (childIterator.hasNext()     ) &&
            	    (bEdgeConditionFound == false)    ) {
                Edge edge = childIterator.next();
                SplitCondition condition = edge.getCondition();
                if (condition.test(example)) {
                	bEdgeConditionFound = true;
                	ExtendedTree childTree = (ExtendedTree) edge.getChild();
                	if(childTree.getTotalExamples() >= dCurtailmentThreshold)
                		dPrediction = (predictUsingCurtailment(example, childTree));
                	else
                		dPrediction = predictAsLeaf(example, node);
                	}
                }
            
            if(bEdgeConditionFound == false) {
            	dPrediction = predictAsLeaf(example, node);
            }
        }
        
        return dPrediction;
	}
	
    private double smoothedConfidence(double dPositiveExamples, double dTotalExamples) {
    	double dSmoothedConfidence = ((dPositiveExamples + (dBaseRate * dShiftFactor))/ (dTotalExamples + dShiftFactor));
    	return dSmoothedConfidence;
    }
    
    private double predictAsLeaf(Example example, ExtendedTree node)  {
		double dPrediction = 0;
        Iterator<String> s = node.getCounterMap().keySet().iterator();
        int[] counts = new int[getLabel().getMapping().size()];
        int sum = 0;
        while (s.hasNext()) {
            String className = s.next();
            int count = node.getCount(className);
            int index = getLabel().getMapping().getIndex(className);
            counts[index] = count;
            sum += count;
        }
        for (int i = 0; i < counts.length; i++) {
            example.setConfidence(getLabel().getMapping().mapIndex(i), smoothedConfidence(counts[i], sum));
        }
       
        dPrediction = getLabel().getMapping().getIndex(node.getLabel());

        return dPrediction;
    }
    
	public int getRenderHeight(int preferredHeight) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getRenderWidth(int preferredWidth) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void render(Graphics graphics, int width, int height) {
		// TODO Auto-generated method stub

	}

}
