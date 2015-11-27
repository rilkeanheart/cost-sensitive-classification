/**
 * 
 */
package com.rapidminer.operator.learner.meta.thesis;

import com.rapidminer.example.Example;

/**
 * @author Michael Green
 *
 */
public interface ExampleBasedPerformanceCalculator {

	//  Assess the actual performance of a labeled example
	public double getPerformance(Example example);
	
	//  Assess the conditionalPerformance of an example
	public double getConditionalRisk(Example example, int dPredictedClass, int dActualClass);
	
	//  Assess the importance of the example (may be based on other example attributes)
	public double getImportance(Example example);
}
