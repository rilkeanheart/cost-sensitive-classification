package com.rapidminer.operator.learner.meta.thesis;

import com.rapidminer.example.Example;

public class DMEF2ExamplePerformanceCalculator implements
		ExampleBasedPerformanceCalculator {

	public double getPerformance(Example example) {
		// Determine predictedLabel and actual label
		int nPredictedClass = 0;
		int nActualClass = 0;
		
		double dPerformance = 0;
		
		if(nPredictedClass == 1) {
			if(nActualClass == 0)
				dPerformance = 0; // Performance is minus the amount of postage
			else
				dPerformance = 0; // Performance is donation amount minus postage
		}
		
		return dPerformance;
	}

	public double getConditionalRisk(Example example, int nPredictedClass, int nActualClass) {
		double dRisk = 0;
		
		if(nPredictedClass == 0) {
			if(nActualClass == 0)
				dRisk = 0;
			else
				dRisk = 0; // Risk is the donation amount minus postage
		} else {
			if(nActualClass == 0)
				dRisk = 0;  // Risk is the postage amount
			else
				dRisk = 0;
		}
		
		return dRisk;
	}

	public double getImportance(Example example) {
		return 0;
	}
	
}
