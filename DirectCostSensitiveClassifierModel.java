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

import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JTabbedPane;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;

public class DirectCostSensitiveClassifierModel extends PredictionModel {

	private static final long serialVersionUID = -8036434608645810089L;
	
	private SmoothedAndCurtailedTreeModel classPredictor;
	
	private Model heckmanPerformancePredictor;
	private ExampleBasedPerformanceCalculator exampleBasedPerformancePredictor;
	private boolean isCostBasedPerformance = false;

	public DirectCostSensitiveClassifierModel(ExampleSet exampleSet, SmoothedAndCurtailedTreeModel classPredictor, Model performancePredictor) {
		super(exampleSet);
		this.classPredictor = classPredictor;
		this.heckmanPerformancePredictor = performancePredictor;
		this.exampleBasedPerformancePredictor = null;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet,
			Attribute predictedLabel) throws OperatorException {
		

		// iterate through example set
		Iterator<Example> reader = exampleSet.iterator();
		Attributes exampleSetAttributes = exampleSet.getAttributes();
		int numberOfClasses = exampleSetAttributes.getLabel().getMapping().getValues().size();
		
		// Hash maps are used for addressing particular class values using indices without relying 
		// upon a consistent index distribution of the corresponding substructure.
		int currentNumber = 0;		
		HashMap<Integer, String>  classIndexMap = new HashMap<Integer, String> (numberOfClasses);		
		for (String currentClass : exampleSetAttributes.getLabel().getMapping().getValues()) {
			
			classIndexMap.put(currentNumber, currentClass);							
			currentNumber++;
		}			

		while (reader.hasNext()) {	
			Example example = reader.next();
			
			// pass example to class predictor
			classPredictor.predict(example);
			
			//double[] conditionalPerformance = new double[numberOfClasses];
			int bestIndex = - 1;
			double bestValue = 0;
			if(isCostBasedPerformance)
				bestValue = Double.POSITIVE_INFINITY;
			else
				bestValue = Double.NEGATIVE_INFINITY;
			
		    // iterate through each label type
			for (int i = 0; i < numberOfClasses; i++) {
				
	            // determine the expected performance of that label using performance predictor (Heckman or ExampleBased)
	            double performance = 1;
	            /*if(heckmanPerformancePredictor!= null)
	            	performance = heckmanPerformancePredictor.predictPerformance(example);
	            else
	            	performance = exampleBasedPerformancePredictor.getImportance(example);*/
				
				// determine the expected performance (benefit) of that label by multiplying confidence by 
	            // if expected performance is greater than "currentBest", change predicted label
	            double confidence = example.getConfidence(getLabel().getMapping().mapIndex(i));
                double expectedValue = confidence * performance;
                
				if(isCostBasedPerformance == true) { //We want the smallest cost (i.e., cost)
					if (expectedValue < bestValue) {
						bestValue = expectedValue;
						bestIndex = i;
					}
				} else { //We want the largest benefit 
					if (expectedValue > bestValue) {
						bestValue = expectedValue;
						bestIndex = i;
					}
			    }
			}
			
			// Relabel the example
			double dBestMappedIndex = exampleSetAttributes.getLabel().getMapping().mapString(classIndexMap.get(bestIndex));
			example.setLabel(dBestMappedIndex);
		}		
		
		return exampleSet;
	}
}
