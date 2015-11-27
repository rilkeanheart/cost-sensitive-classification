package com.rapidminer.operator.learner.meta.thesis;

import com.rapidminer.example.Example;

public class KDD98ExamplePerformanceCalculator implements
		ExampleBasedPerformanceCalculator {

	/** The attribute string the for the last gift value*/
	public static final String KDD98_COST_ATTRIBUTE = "cost";

	public double getPerformance(Example example) {

		// Determine predictedLabel and actual label
		double nPredictedClass = example.getPredictedLabel();
		
		double dPerformance = 0;
		
		if(nPredictedClass != 0) {
			dPerformance = example.getValue((example.getAttributes().get(KDD98_COST_ATTRIBUTE)));
		}
		
		return dPerformance;
	}

	public double getConditionalRisk(Example example, int nPredictedClass, int nActualClass) {

		double dRisk = 0;
		
		if(nPredictedClass == 0)  {
			if(nActualClass == 1)
			{
				//Should check example to see the risk of classifying this as a non-responder
				// if actual example is a non-responder, then the risk is zero
				// if actual example is a responder, then the risk is the postage-adjusted donation
				dRisk = Math.abs(example.getValue((example.getAttributes().get(KDD98_COST_ATTRIBUTE))));
			}
		} else {
			if(nActualClass == 0)
				dRisk = 0.68;
		}
		
		return dRisk;
	}

	public double getImportance(Example example) {
		return Math.abs(example.getValue((example.getAttributes().get(KDD98_COST_ATTRIBUTE))));
	}

}
