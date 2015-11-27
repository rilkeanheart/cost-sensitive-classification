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
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This class is associated to the MetaCost operator and supports the 
 * evaluation procedures of the MetaCost method.
 *  
 * @author Helge Homburg
 * @version $Id: MetaCostModel.java,v 1.5 2008/05/09 19:22:47 ingomierswa Exp $
 */
public class MetaCostModel extends PredictionModel {
		
	private static final long serialVersionUID = -7378871544357578954L;

	private Model[] models;
	
	//private double[][] costMatrix;
	
	private ExampleBasedPerformanceCalculator myPerformanceCalculator;
			
	public MetaCostModel(ExampleSet exampleSet, Model[] models, ExampleBasedPerformanceCalculator performanceCalculator) {
		super(exampleSet);
		this.models = models;
		this.myPerformanceCalculator = performanceCalculator;
	}	
	
	public int getNumberOfModels() {
		return models.length;
	}

	/** Returns a binary decision model for the given classification index. */
	public Model getModel(int index) {
		return models[index];
	}
	
	
	/** Returns a single value from the cost matrix. */
	//public double getCostValue(int i, int j) {
	//	return costMatrix[i][j];
	//}
	
	public ExampleSet performPrediction(ExampleSet originalExampleSet, Attribute predictedLabel) throws OperatorException {
		
		ExampleSet exampleSet = (ExampleSet)originalExampleSet.clone();
		int numberOfClasses = getLabel().getMapping().getValues().size();
		
		double[][] confidences = new double[exampleSet.size()][numberOfClasses];
		
		// Hash maps are used for addressing particular class values using indices without relying 
		// upon a consistent index distribution of the corresponding substructure.
		int currentNumber = 0;		
		HashMap<Integer, String>  classIndexMap = new HashMap<Integer, String> (numberOfClasses);		
		for (String currentClass : getLabel().getMapping().getValues()) {
			
			classIndexMap.put(currentNumber, currentClass);							
			currentNumber++;
		}			
		
		// 1. Iterate over all models and all examples for every model to receive all confidence values.
		for (int k = 0; k < getNumberOfModels(); k++) {
			
			Model model = getModel(k);			
			exampleSet = model.apply(exampleSet);
			
			
			Iterator<Example> reader = exampleSet.iterator();
			int counter = 0;				
			
			while (reader.hasNext()) {				
				Example example = reader.next();
				
				int currentClassNumber = 0;
				
				for (String currentClass : getLabel().getMapping().getValues()) {					
					confidences[counter][currentClassNumber] += example.getConfidence(currentClass);
					currentClassNumber++;
				}
			
			counter++;
			}
			
			PredictionModel.removePredictedLabel(exampleSet);		
		}
		
		
		// 2. Iterate again over all examples to compute a prediction and a confidence distribution for 
		//    all examples depending on the results of step 1 and the cost matrix. 
		Attribute classificationCost = AttributeFactory.createAttribute(Attributes.CLASSIFICATION_COST, Ontology.REAL);
		originalExampleSet.getExampleTable().addAttribute(classificationCost);
		originalExampleSet.getAttributes().setCost(classificationCost);		
		
		Attribute classLabel = originalExampleSet.getAttributes().getLabel();
		boolean hasLabel = (classLabel != null);		
		
		Iterator<Example> reader = originalExampleSet.iterator();
		int counter = 0;				
		
		while (reader.hasNext()) {	
			
			Example example = reader.next();
			
			for (int i = 0; i < numberOfClasses; i++) { 				
				confidences[counter][i] = confidences[counter][i] / getNumberOfModels(); 				
			}			
			
			double[] conditionalRisk = new double[numberOfClasses];
			int bestIndex = - 1;
			double bestValue = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < numberOfClasses; i++) {
				
				for (int j = 0; j < numberOfClasses; j++) {
					
					conditionalRisk[i] += confidences[counter][j] * myPerformanceCalculator.getConditionalRisk(example,i,j);					
				}
				if (conditionalRisk[i] < bestValue) {
					bestValue = conditionalRisk[i];
					bestIndex = i;						
				} 
			}
			
			example.setPredictedLabel(getLabel().getMapping().mapString(classIndexMap.get(bestIndex)));
			
			if (hasLabel) {
				int labelIndex = getLabel().getMapping().mapString(classIndexMap.get((int)example.getLabel()));
				example.setValue(originalExampleSet.getAttributes().getCost(), myPerformanceCalculator.getConditionalRisk(example,bestIndex,labelIndex));
			} else {
				example.setValue(originalExampleSet.getAttributes().getCost(), conditionalRisk[bestIndex]);
			}
			
			for (int i = 0; i < numberOfClasses; i++) {								
				example.setConfidence(classIndexMap.get(i), confidences[counter][i]);
			}
			
			counter++;
		}		
		
		return originalExampleSet;
	}
	
	public Component getVisualizationComponent(IOContainer container) {
		JTabbedPane tabPane = new ExtendedJTabbedPane();
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			Model model = this.getModel(i);
			tabPane.add("Model " + (i + 1), model.getVisualizationComponent(container));
		}
		return tabPane;
	}
	
	
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparator());
		for (int i = 0; i < models.length; i++)
			result.append((i > 0 ? Tools.getLineSeparator() : "") + models[i].toString());
		return result.toString();
	}

}
