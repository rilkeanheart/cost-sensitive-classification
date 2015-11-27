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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.LearnerCapability;
import com.rapidminer.operator.learner.meta.AbstractMetaLearner;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operator uses costs to compute label predictions
 * according to misclassification costs. The method used by this operator
 * is similar to MetaCost as described by Pedro Domingos.  
 * 
 * This class has been extended to recognize example-based misclassification costs
 *  
 * @author Michael Green
 * @version $Id: MetaCost.java,v 1.8 2008/05/09 19:22:48 mtgreen Exp $
 */
public class MetaCost extends AbstractMetaLearner {

	/** The parameter name for &quot;The ExampleBasedPerformance Calculator;&quot */
	public static final String PARAMETER_PERFORMANCE_CALCULATOR = "example_performance_calculator";
	
	/** The parameter name for &quot;View performance as a cost minimization (true) or view performance as profit maximization (false);&quot */
	public static final String 	PARAMETER_PERFORMANCE_AS_COSTS = "example_performance_as_costs";

	/** The parameter name for &quot;File&quot; */
	//public static final String PARAMETER_COST_MATRIX_FILE_LOCATION = "cost_matrix_file_location";

	/** The parameter name for &quot;Fraction of examples used for training. Must be greater than 0 and should be lower than 1.&quot; */
	public static final String PARAMETER_USE_SUBSET_FOR_TRAINING = "use_subset_for_training";

	/** The parameter name for &quot;The number of iterations (base models).&quot; */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/** The parameter name for &quot;Use sampling with replacement (true) or without (false)&quot; */
	public static final String PARAMETER_SAMPLING_WITH_REPLACEMENT = "sampling_with_replacement";

	/** The parameter name for &quot;Use the given random seed instead of global random numbers (-1: use global)&quot; */
	public static final String PARAMETER_LOCAL_RANDOM_SEED = "local_random_seed";
	
	public MetaCost(OperatorDescription description) {
		super(description);
	}		
	
	public Model learn(ExampleSet inputSet) throws OperatorException {
		int iterations = getParameterAsInt(PARAMETER_ITERATIONS);
		double subsetRatio = getParameterAsDouble(PARAMETER_USE_SUBSET_FOR_TRAINING);
		boolean isCostBasedPerformance = getParameterAsBoolean(PARAMETER_PERFORMANCE_AS_COSTS);
		Model[] models = new Model[iterations]; 
		
		//get cost calculator class get class for name
		//double[][] costMatrix = getParameterAsMatrix(PARAMETER_COST_MATRIX);		
		String strPerformanceCalculator = getParameterAsString(PARAMETER_PERFORMANCE_CALCULATOR);
		Class myClass;
		ExampleBasedPerformanceCalculator performanceCalculator;
		try {
			    myClass = Class.forName(strPerformanceCalculator).asSubclass(ExampleBasedPerformanceCalculator.class);	   
		        performanceCalculator = (ExampleBasedPerformanceCalculator) myClass.newInstance();

			//perform bagging operation			
			if (getParameterAsBoolean(PARAMETER_SAMPLING_WITH_REPLACEMENT)) {	
				//sampling with replacement
				int randomSeed = getParameterAsInt(PARAMETER_LOCAL_RANDOM_SEED);
				Random randomGenerator = RandomGenerator.getRandomGenerator(randomSeed);	
				int size = (int)(inputSet.size()*subsetRatio);		
				for (int i = 0; i < iterations; i++) {		
					ExampleSet exampleSet = (ExampleSet)inputSet.clone();
					int[] mapping = MappedExampleSet.createBootstrappingMapping(exampleSet, size, randomGenerator);
					MappedExampleSet currentSampleSet = new MappedExampleSet(exampleSet, mapping);
					models[i] = applyInnerLearner(currentSampleSet);				
					inApplyLoop();
				}
			} else {
				//sampling without replacement
				for (int i = 0; i < iterations; i++) {			
					SplittedExampleSet splitted = new SplittedExampleSet((ExampleSet)inputSet.clone(), subsetRatio, SplittedExampleSet.SHUFFLED_SAMPLING, -1);
					splitted.selectSingleSubset(0);
					models[i] = applyInnerLearner(splitted);
					inApplyLoop();
				}
			}
			
			// Label each example to minimize conditional risk

			// Begin new code
			
			
			// End new code
			ExampleSet exampleSet = (ExampleSet)inputSet.clone();
			Attributes exampleSetAttributes = exampleSet.getAttributes();
			int numberOfClasses = exampleSetAttributes.getLabel().getMapping().getValues().size();
			
			double[][] confidences = new double[exampleSet.size()][numberOfClasses];
			
			// Hash maps are used for addressing particular class values using indices without relying 
			// upon a consistent index distribution of the corresponding substructure.
			int currentNumber = 0;		
			HashMap<Integer, String>  classIndexMap = new HashMap<Integer, String> (numberOfClasses);		
			for (String currentClass : exampleSetAttributes.getLabel().getMapping().getValues()) {
				
				classIndexMap.put(currentNumber, currentClass);							
				currentNumber++;
			}			
			
			// 1. Iterate over all models and all examples for every model to receive all confidence values.
			for (int k = 0; k < iterations; k++) {
				
				Model model = models[k];			
				exampleSet = model.apply(exampleSet);
				
				Iterator<Example> reader = exampleSet.iterator();
				int counter = 0;				
				
				while (reader.hasNext()) {				
					Example example = reader.next();
					
					//int currentClassNumber = 0;
					int dPredictedLabel = (int) example.getPredictedLabel();
					confidences[counter][dPredictedLabel]+= (new Double(1)/iterations);
					
					/*for (String currentClass : exampleSetAttributes.getLabel().getMapping().getValues()) {					
						confidences[counter][currentClassNumber] += example.getConfidence(currentClass);
						currentClassNumber++;
					}*/
				
				counter++;
				}
				
				PredictionModel.removePredictedLabel(exampleSet);		
			}
			
			
			// 2. Iterate again over all examples to compute a prediction and a confidence distribution for 
			//    all examples depending on the results of step 1 and the cost matrix. 
			//Attribute classificationCost = AttributeFactory.createAttribute(Attributes.CLASSIFICATION_COST, Ontology.REAL);
			//originalExampleSet.getExampleTable().addAttribute(classificationCost);
			//.getAttributes().setCost(classificationCost);		
				
			Iterator<Example> reader = inputSet.iterator();
			int counter = 0;				
			
			while (reader.hasNext()) {	
				
				Example example = reader.next();
				
				for (int i = 0; i < numberOfClasses; i++) { 				
					confidences[counter][i] = confidences[counter][i] / iterations; 				
				}			
				
				double[] conditionalPerformance = new double[numberOfClasses];
				int bestIndex = - 1;
				double bestValue = 0;
				if(isCostBasedPerformance)
					bestValue = Double.POSITIVE_INFINITY;
				else
					bestValue = Double.NEGATIVE_INFINITY;
				
				for (int i = 0; i < numberOfClasses; i++) {
					
					for (int j = 0; j < numberOfClasses; j++) {
						
						conditionalPerformance[i] += confidences[counter][j] * performanceCalculator.getConditionalRisk(example, i, j);					
					}
					if(isCostBasedPerformance == true) { //We want the smallest cost (i.e., cost)
						if (conditionalPerformance[i] < bestValue) {
							bestValue = conditionalPerformance[i];
							bestIndex = i;
						}
					} 
				}
				
				// Relabel the example
				double dBestMappedIndex = exampleSetAttributes.getLabel().getMapping().mapString(classIndexMap.get(bestIndex));
				example.setLabel(dBestMappedIndex);
							
				counter++;
			}		
		} catch (ClassNotFoundException e) {
			System.out.println("Unknown class specified as performance calculator");
		} catch (InstantiationException e) {
			System.out.println("Unable to instantiate performance calculator");
		} catch (IllegalAccessException e) {
			System.out.println("Unable to instantiate performance calculator");
		}
		
		
		//re-apply the classifier to the relabeled training set
		return applyInnerLearner(inputSet);
	}

	/**
	 * Support polynominal labels. For all other capabilities, it checks for the underlying 
	 * operator to see which capabilities are supported by them.
	 */
	public boolean supportsCapability(LearnerCapability capability) {
		if (getNumberOfOperators() == 0)
			return false;
		if (capability == LearnerCapability.POLYNOMINAL_CLASS)
			return true;
		if (capability == LearnerCapability.BINOMINAL_CLASS)
			return true;
		return super.supportsCapability(capability);
	}
	
	
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();		
		//types.add(new ParameterTypeMatrix(PARAMETER_COST_MATRIX, "The cost matrix in Matlab single line format", true, false));
		//types.add(new ParameterTypeFile(PARAMETER_COST_MATRIX_FILE_LOCATION,"File",null,true));
		types.add(new ParameterTypeDouble(PARAMETER_USE_SUBSET_FOR_TRAINING, "Fraction of examples used for training. Must be greater than 0 and should be lower than 1.", 0, 1, 1.0));
		types.add(new ParameterTypeInt(PARAMETER_ITERATIONS, "The number of iterations (base models).", 1, Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeBoolean(PARAMETER_SAMPLING_WITH_REPLACEMENT, "Use sampling with replacement (true) or without (false)", true));
		types.add(new ParameterTypeBoolean(PARAMETER_PERFORMANCE_AS_COSTS, "View performance as a cost minimization (true) or view performance as profit maximization (false)", true));
		types.add(new ParameterTypeInt(PARAMETER_LOCAL_RANDOM_SEED, "Use the given random seed instead of global random numbers (-1: use global)", -1, Integer.MAX_VALUE, -1));
		types.add(new ParameterTypeString(PARAMETER_PERFORMANCE_CALCULATOR, "The class name of the performance calculator (com.rapidminer.operator.learner.meta.thesis.etc)", false));
		return types;
	}	
}
