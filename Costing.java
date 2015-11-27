package com.rapidminer.operator.learner.meta.thesis;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.LearnerCapability;
import com.rapidminer.operator.learner.meta.AbstractMetaLearner;
import com.rapidminer.operator.learner.meta.AdaBoostModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;

/**
 * This Costing implementation can be used with all learners available in RapidMiner, not only
 * the ones which originally are part of the Weka package.
 * 
 * @author Michael Green
 * @version $Id: Costing.java,v 1.7 2008/06/06 09:37:14 ingomierswa Exp $
 */

public class Costing extends AbstractMetaLearner {

	final String strImportance = "CostingImportance";
	
	/**
	 * Name of the variable specifying the maximal number of iterations of the
	 * learner.
	 */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/** The parameter name for &quot;The ExampleBasedPerformance Calculator;&quot */
	public static final String PARAMETER_PERFORMANCE_CALCULATOR = "example_performance_calculator";

	/** The parameter name for &quot;Use the given random seed instead of global random numbers (-1: use global)&quot; */
	public static final String PARAMETER_LOCAL_RANDOM_SEED = "local_random_seed";

	/** Name of the flag indicating internal bootstrapping. */
	public static final String PARAMETER_AVERAGE_CONFIDENCES = "average_confidences";

	/** Class used to determine example based weights & model performance */
	private ExampleBasedPerformanceCalculator m_myPerformanceCalculator;
	
	// field for visualizing performance
	protected int currentIteration;

	// The total weight as a performance measure to be visualized.
	private double performance = 0;

	// A backup of the original importances of the training set to restore them
	// after learning.
	private double[] oldImportances;

	/** Constructor. */
	public Costing(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("performance", "The performance.") {
			public double getDoubleValue() {
				return performance;
			}
		});
		addValue(new ValueDouble("iteration", "The current iteration.") {
			public double getDoubleValue() {
				return currentIteration;
			}
		});
	}

	/**
	 * Overrides the method of the super class. Returns true for polynominal
	 * class.
	 */
	public boolean supportsCapability(LearnerCapability lc) {
		if (lc == LearnerCapability.NUMERICAL_CLASS)
			return false;
        if (lc == LearnerCapability.WEIGHTED_EXAMPLES)
            return true;
        
        return super.supportsCapability(lc);
	}

	/**
	 * Constructs a <code>Model</code> based upon a weak learner,
	 * re-weighting the training example set accordingly by rejection
	 * sampling the original training set.  The is combines the
	 * hypothesis using the available weighted performance values.
	 */
	public Model learn(ExampleSet exampleSet) throws OperatorException {
        if (!exampleSet.getAttributes().getLabel().isNominal())
            throw new UserError(this, 119, exampleSet.getAttributes().getLabel().getName(), getName());
        			
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

		this.performance = this.prepareImportances(exampleSet);
		Model model = this.trainCostingModel(exampleSet);

		Attribute importanceAttribute = exampleSet.getAttributes().getSpecial(strImportance);
		if (this.oldImportances != null) { // need to reset importances
			Iterator<Example> reader = exampleSet.iterator();
			int i = 0;
			while (reader.hasNext() && i < this.oldImportances.length) {
				reader.next().setValue(importanceAttribute, this.oldImportances[i++]);
			}
		} else { // need to delete the importance attribute
			exampleSet.getAttributes().remove(importanceAttribute);
			exampleSet.getExampleTable().removeAttribute(importanceAttribute);
		}

		return model;
	}

	/**
	 * Creates an importance attribute if not yet done. 
	 * 
	 * @param exampleSet
	 *            the example set to be prepared
	 * @return the total importance
	 */
	protected double prepareImportances(ExampleSet exampleSet) {
		//Attribute weightAttr = exampleSet.getAttributes().getWeight();
		Attribute importanceAttribute = exampleSet.getAttributes().getSpecial(strImportance);
		double totalImportance = 0;			

		if (importanceAttribute == null) {
			this.oldImportances = null;
			importanceAttribute = Tools.createSpecialAttribute(exampleSet, strImportance, Ontology.NUMERICAL);
			Iterator<Example> exRead = exampleSet.iterator();
			while (exRead.hasNext()) {
				Example nextExample = exRead.next();
				double dImportance = m_myPerformanceCalculator.getImportance(nextExample);
				nextExample.setValue(importanceAttribute, dImportance);
				totalImportance+= dImportance;
			}
		} else { // Back up old weights:
			this.oldImportances = new double[exampleSet.size()];
			Iterator<Example> reader = exampleSet.iterator();

			for (int i = 0; (reader.hasNext() && i < oldImportances.length); i++) {
				Example nextExample = reader.next();
				this.oldImportances[i] = nextExample.getValue(importanceAttribute);
				double dImportance = m_myPerformanceCalculator.getImportance(nextExample);
				nextExample.setValue(importanceAttribute, dImportance);
				totalImportance+= dImportance;
			}
		}
		return totalImportance;
	}

	/** Main method for training the ensemble classifier */
	private Model trainCostingModel(ExampleSet trainingSet) throws OperatorException {
		log("Total weight of example set at the beginning: " + this.performance);

		// Containers for models and weights:
		Vector<Model> ensembleModels = new Vector<Model>();
		Vector<Double> ensembleWeights = new Vector<Double>();
		
		// maximum number of iterations
		final int iterations = this.getParameterAsInt(PARAMETER_ITERATIONS);
		for (int i = 0; (i < iterations && this.performance > 0); i++) {
			this.currentIteration = i;
			
			//Create Rejection Sample
		    ExampleSet rejectionSample = rejectionSampleWithImportance(trainingSet);
			
			// train one model per iteration and add it to the list
		    ensembleModels.add(applyInnerLearner(rejectionSample));
			inApplyLoop();
		}
		
		// Build a Model object. Last parameter is "crispPredictions", nowadays
		// always true.
		if (this.getParameterAsBoolean(PARAMETER_AVERAGE_CONFIDENCES)) {
			return new CostingModel(trainingSet, ensembleModels);
		} else {
			for (int i=0; i<ensembleModels.size(); i++) {
				ensembleWeights.add(1.0d);
			}
			return new AdaBoostModel(trainingSet, ensembleModels, ensembleWeights);
		}
	}

	/**
	 * Adds the parameters &quot;number of iterations&quot; and &quot;model
	 * file&quot;.
	 */
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_ITERATIONS, "The maximum number of iterations.", 1, Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeInt(PARAMETER_LOCAL_RANDOM_SEED, "Use the given random seed instead of global random numbers (-1: use global)", -1, Integer.MAX_VALUE, -1));
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_CONFIDENCES, "Specifies whether to average available prediction confidences or not.", true)); 
		types.add(new ParameterTypeString(PARAMETER_PERFORMANCE_CALCULATOR, "The class name of the performance calculator (com.rapidminer.operator.learner.meta.thesis.etc)", false));
		return types;
	}

	/**
	 * Creates a new (smaller) data set by drawing samples from this
	 * data set with probability proportionate to the weighted average
	 * of each instance.
	 * The length of the weight vector has to be the same as the
	 * number of instances in the data set, and all weights have to
	 * be positive.
	 *
	 * @param random a random number generator
	 * @param weights the weight vector
	 * @return the new data set
	 * @exception IllegalArgumentException if the weights array is of the wrong
	 * length or contains negative weights.
	 */
	 public final ExampleSet rejectionSampleWithImportance(ExampleSet inputData) throws OperatorException {

		Attribute importanceAttribute = inputData.getAttributes().getSpecial(strImportance);
		int[]  mappingVector = new int[inputData.size()];
	    int includedSampleCount = 0;
	    
		int randomSeed = this.getParameterAsInt(PARAMETER_LOCAL_RANDOM_SEED);
		Random randomGenerator = RandomGenerator.getRandomGenerator(randomSeed);	

		// Determine largest weight placed on instance
	    double maxImportance = 0;
		Iterator<Example> reader = inputData.iterator();

		for (int i = 0; reader.hasNext(); i++) {
			double thisImportance = reader.next().getValue(importanceAttribute);
			if(thisImportance > maxImportance)
				maxImportance = thisImportance;
		}
	    
	    // Iterate through instances, rejecting an instance
	    // with probability (1 - weight/maxweight)
		reader = inputData.iterator();

		for (int i = 0; reader.hasNext(); i++) {
			double currentImportance = Math.abs(reader.next().getValue(importanceAttribute));
			if(randomGenerator.nextDouble() < (currentImportance/maxImportance))
			{
				mappingVector[includedSampleCount++] = i;
			}
		}

		//Build final mapping vector
        int[] finalMapping = new int[includedSampleCount];
        for (int i = 0; i < includedSampleCount; i++) {
        	finalMapping[i] = mappingVector[i];
        }
        
		MappedExampleSet rejectionSampledSet = new MappedExampleSet(inputData, finalMapping);
		
		return rejectionSampledSet;
	 }

}
