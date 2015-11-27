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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;

/**
 * This operator provides the ability to evaluate classification performance
 * (potentially example based).  Therefore an implementation of an
 * ExampleBasedPerformanceCalculator must be specified.  The implementation
 * will calculate the actual performance (cost or profit) based on individual examples.
 * 
 * @author Michael Green
 * @version $Id: PerformanceEvaluator.java,v 1.3 2008/05/09 19:23:24 ingomierswa Exp $
 */
public class PerformanceEvaluator extends Operator {

	private static final String PARAMETER_PERFORMANCE_EVALUATOR = "performance_evaluator";
	private static final String PARAMETER_KEEP_EXAMPLE_SET = "keep_exampleSet";
	
	public PerformanceEvaluator(OperatorDescription description) {
		super(description);
	}

	public IOObject[] apply() throws OperatorException {
		ExampleSet exampleSet = getInput(ExampleSet.class);
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label != null) {
			if (label.isNominal()) {
				String strPerformanceCalculator = getParameterAsString(PARAMETER_PERFORMANCE_EVALUATOR);
				Class myClass;
				ExampleBasedPerformanceCalculator performanceCalculator;
				try {
					    myClass = Class.forName(strPerformanceCalculator).asSubclass(ExampleBasedPerformanceCalculator.class);	   
				        performanceCalculator = (ExampleBasedPerformanceCalculator) myClass.newInstance();
						MeasuredPerformance criterion = new ClassificationPerformanceCriterion(performanceCalculator);
						PerformanceVector performance = new PerformanceVector();
						performance.addCriterion(criterion);
						// now measuring costs
						criterion.startCounting(exampleSet, false);
						for (Example example: exampleSet) {
							criterion.countExample(example);
						}
						if (getParameterAsBoolean(PARAMETER_KEEP_EXAMPLE_SET)) {
							return new IOObject[] {exampleSet, performance};
						} else {
							return new IOObject[] {performance};
						}
				} catch (ClassNotFoundException e) {
					System.out.println("Unknown class specified as performance calculator");
				} catch (InstantiationException e) {
					System.out.println("Unable to instantiate performance calculator");
				} catch (IllegalAccessException e) {
					System.out.println("Unable to instantiate performance calculator");
				}
			}
		}
		return new IOObject[] {exampleSet};
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_EXAMPLE_SET, "Indicates if the example set should be kept.", false));
		types.add(new ParameterTypeString(PARAMETER_PERFORMANCE_EVALUATOR, "The implemenation of the performance calculator (com.rapidminer.operator.learner.meta.thesis.etc)", false));
		return types;
	}

	@Override
	public Class[] getInputClasses() {
		return new Class[] { ExampleSet.class };
	}

	@Override
	public Class[] getOutputClasses() {
		return new Class[] { PerformanceVector.class };
	}

}
