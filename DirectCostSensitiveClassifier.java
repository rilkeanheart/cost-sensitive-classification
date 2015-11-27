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


import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.tree.DecisionTreeLearner;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.operator.learner.tree.TreeBuilder;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;


public class DirectCostSensitiveClassifier extends DecisionTreeLearner {

	/** The parameter name for &quot;The ExampleBasedPerformance Calculator;&quot */
	public static final String PARAMETER_PERFORMANCE_CALCULATOR = "example_performance_calculator";

	/** The parameter name for &quot;Use the given random seed instead of global random numbers (-1: use global)&quot; */
	public static final String PARAMETER_LOCAL_RANDOM_SEED = "local_random_seed";

	/** Name of the flag indicating internal bootstrapping. */
	public static final String PARAMETER_AVERAGE_CONFIDENCES = "average_confidences";
	
	/** Name of the flag indicating use of Heckman Cost Prediction Method. */
	public static final String PARAMETER_USE_HECKMAN = "use_heckman";

	/** Class used to determine example based weights & model performance */
	private ExampleBasedPerformanceCalculator m_myPerformanceCalculator;

    public DirectCostSensitiveClassifier(OperatorDescription description) {
        super(description);
    }

    public Model learn(ExampleSet eSet) throws OperatorException {
    
		boolean useClassProbabilitiesForPerformanceEstimate = getParameterAsBoolean(PARAMETER_USE_HECKMAN);
		String strPerformanceCalculator = getParameterAsString(PARAMETER_PERFORMANCE_CALCULATOR);
		Class myClass;
		try {
			    myClass = Class.forName(strPerformanceCalculator).asSubclass(ExampleBasedPerformanceCalculator.class);	   
			    m_myPerformanceCalculator = (ExampleBasedPerformanceCalculator) myClass.newInstance();
		} catch (ClassNotFoundException e) {
			System.out.println("Unknown class specified as performance calculator");
		} catch (InstantiationException e) {
			System.out.println("Unable to instantiate performance calculator");
		} catch (IllegalAccessException e) {
			System.out.println("Unable to instantiate performance calculator");
		}

		ExampleSet exampleSet = (ExampleSet)eSet.clone();
    	
    	// create tree builder
    	TreeBuilder builder = new TreeBuilder(createCriterion(),
    			              getTerminationCriteria(exampleSet),
    			              getPruner(),
    			              getSplitPreprocessing(),
    			              getParameterAsInt(PARAMETER_MINIMAL_LEAF_SIZE),
    			              getParameterAsDouble(PARAMETER_MINIMAL_GAIN));
    	    	
    	// learn tree
    	Tree root = builder.learnTree(exampleSet);
        
        // create decision tree model
    	SmoothedAndCurtailedTreeModel classProbabilityModel = new SmoothedAndCurtailedTreeModel(exampleSet, root);
    	
    	// create directCost model
    	DirectCostSensitiveClassifierModel model = null;

    	// create Heckman cost predictor
    	HeckmanPerformancePredictor heckmanPredictor;
    	if(useClassProbabilitiesForPerformanceEstimate == true) {
    		heckmanPredictor = new HeckmanPerformancePredictor(exampleSet,
			                                                   m_myPerformanceCalculator,
			                                                   classProbabilityModel);
    	}
    	else {
    		heckmanPredictor = new HeckmanPerformancePredictor(exampleSet,
                                                               m_myPerformanceCalculator,
                                                               null);
    	}
    	
    	Model performanceEstimationModel = heckmanPredictor.predictPerformance();
    	model = new DirectCostSensitiveClassifierModel(exampleSet, classProbabilityModel, performanceEstimationModel);
     		
    	return model;
	}

	/**
	 * Adds the parameters &quot;number of iterations&quot; and &quot;model
	 * file&quot;.
	 */
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_LOCAL_RANDOM_SEED, "Use the given random seed instead of global random numbers (-1: use global)", -1, Integer.MAX_VALUE, -1));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_HECKMAN, "Specifies whether to average available prediction confidences or not.", true)); 
		types.add(new ParameterTypeString(PARAMETER_PERFORMANCE_CALCULATOR, "The class name of the performance calculator (com.rapidminer.operator.learner.meta.thesis.etc)", false));
		return types;
	}

}
