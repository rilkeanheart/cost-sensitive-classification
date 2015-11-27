package com.rapidminer.operator.learner.meta.thesis;

import com.rapidminer.example.Example;

public class HeartFailurePerformanceCalculator implements
		ExampleBasedPerformanceCalculator {

	public double getPerformance(Example example) {

		// Determine predictedLabel and actual label
		double nPredictedClass = example.getPredictedLabel();
		double nActualClass = example.getLabel();
		
		double dPerformance = 0;
		
		if(nPredictedClass != nActualClass) {
			if (nActualClass == 0)
				dPerformance = 1000;
			else
				dPerformance = 10000;
		}
		
		return dPerformance;
	}

	public double getConditionalRisk(Example example, int nPredictedClass, int nActualClass) {

		double dPerformance = 0;
		
		if(nPredictedClass != nActualClass) {
			if (nActualClass == 0)
				dPerformance = 1000;
			else
				dPerformance = 10000;
		}
		
		return dPerformance;
	}

	public double getImportance(Example example) {
		return 1;
	}

}
