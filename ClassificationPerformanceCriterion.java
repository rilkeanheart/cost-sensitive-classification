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
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.tools.math.Averagable;

/**
 * This performance Criterion works with a given ExampleBasedPerformanceCalculator. Every
 * classification result contributes to the performance metric. Performance is defined by the
 * provided ExampleBasedPerformanceCalculator, and can be cost or profit based.
 * 
 * @author Michael Green
 * @version $Id: ClassificationPerformanceCriterion.java,v 1.3 2008/05/09 19:23:24 ingomierswa Exp $
 */
public class ClassificationPerformanceCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = -7466139591781925005L;

	private ExampleBasedPerformanceCalculator performanceCalculator;
	private double exampleCount; 
	private double performance;


	public ClassificationPerformanceCriterion(ExampleBasedPerformanceCalculator performanceCalculator) {
		this.performanceCalculator = performanceCalculator;
		exampleCount = 0;
		performance = 0;
	}

	public ClassificationPerformanceCriterion(String strPerformanceCalculatorClass) {

		Class myClass;
		ExampleBasedPerformanceCalculator performanceCalculator = null;
		
		try {
		    myClass = Class.forName(strPerformanceCalculatorClass).asSubclass(ExampleBasedPerformanceCalculator.class);	   
	        performanceCalculator = (ExampleBasedPerformanceCalculator) myClass.newInstance();
			this.performanceCalculator = performanceCalculator;
			exampleCount = 0;
			performance = 0;
		} catch (ClassNotFoundException e) {
			System.out.println("Unknown class specified as performance calculator");
		} catch (InstantiationException e) {
			System.out.println("Unable to instantiate performance calculator");
		} catch (IllegalAccessException e) {
			System.out.println("Unable to instantiate performance calculator");
		}
		
	}

	
	@Override
	public void countExample(Example example) {
		exampleCount ++;
		performance += performanceCalculator.getPerformance(example);
	}

	@Override
	public String getDescription() {
		return "This Criterion delievers the classification performance (cost or profit)";
	}

	@Override
	public double getExampleCount() {
		return exampleCount;
	}

	@Override
	public double getFitness() {
		return performance;
	}

	@Override
	protected void buildSingleAverage(Averagable averagable) {
	}

	@Override
	public double getMikroAverage() {
		return performance;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public String getName() {
		return "Classification Performance";
	}

}
