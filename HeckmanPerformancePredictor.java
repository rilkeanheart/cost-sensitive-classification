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

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.learner.functions.LinearRegression;
import com.rapidminer.operator.learner.functions.LinearRegressionModel;

/**
 * The Heckman Performance Predictor predicts the performance (e.g., cost)
 * of an arbitrary example.  It is specifically useful in compensating for 
 * sample selection bias when the performance of a number of training examples
 * is unknown (e.g., due to non response).  This implementations uses
 * a nonlinear variant of Nobel Prize winning economist, James J. Heckman.
 * @author Michael Green
 * @version $Id: SmoothedAndCurtailedTreeModel.java,v 1.8 2008/05/09 19:22:53 greenm9 Exp $
 */

public class HeckmanPerformancePredictor {

	private ExampleBasedPerformanceCalculator performanceCalculator;
	private Model classProbabilityPredictor;
	private ExampleSet exampleSet;
	private boolean useEstimatedPerformance;
	private LinearRegressionModel regressionModel;

	public HeckmanPerformancePredictor(ExampleSet trainingSet,
								       ExampleBasedPerformanceCalculator performanceCalculator,
								       Model classProbabilityPredictor)
	{
		this.exampleSet = trainingSet;
		this.performanceCalculator = performanceCalculator;
		this.classProbabilityPredictor = classProbabilityPredictor;
	}
	
	public Model predictPerformance()
	{
		// Determine appropriate attributes for multiple linear regression
		//List mlrAttributes = performanceCalculator.getMLRAttributeNames();
		
		// Get list of attributes from exampleset
		//Attributes exampleAttributes exampleSet.getAttributes();
		
		// Iterate through Attributes list set
		
		   // Check attribute to see if it is in the list of attributes to use
		   // if not, set attribute to "special"
		   // add attribute to a list of "modified" attributes
		
		// if using class probabilities
		   // create new attribute in example set
		   // iterate through example set
		       //for each example, predict class probability using class predictor
		       //set new attribute with result
		
		// Use linear regression on example set
		
		// Clean up
			// Switch modified attributes back to regular
		    // Remove attribute
		    // Get list of attributes from exampleSet

		return this.regressionModel;
	}
	
}
