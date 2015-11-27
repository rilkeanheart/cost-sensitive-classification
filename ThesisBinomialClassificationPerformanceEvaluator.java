package com.rapidminer.operator.learner.meta.thesis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.performance.AbstractPerformanceEvaluator;
import com.rapidminer.operator.performance.BinaryClassificationPerformance;
//import com.rapidminer.operator.performance.PerformanceComparator;
import com.rapidminer.operator.performance.PerformanceCriterion;
//import com.rapidminer.operator.performance.PolynominalClassificationPerformanceEvaluator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;

public class ThesisBinomialClassificationPerformanceEvaluator extends AbstractPerformanceEvaluator {

	/** The parameter name for &quot;List of classes that implement com.rapidminer..operator.performance.PerformanceCriterion.&quot; */
	public static final String PARAMETER_ADDITIONAL_PERFORMANCE_CRITERIA = "additional_performance_criteria";
	
	/**
	 * The names of allowed user criteria. These are necessary for plotting
	 * purposes and the definition of the main criterion.
	 */
	public static final String[] USER_CRITERIA_NAMES = {
		"user1", "user2", "user3"
	};
	
	/** Used for logging. */
	private List<PerformanceCriterion> userCriteria = new ArrayList<PerformanceCriterion>();

	/** The proper criteria to the names. */
	private static final Class[] SIMPLE_CRITERIA_CLASSES = {  
		com.rapidminer.operator.performance.AreaUnderCurve.class
	};
	
	public ThesisBinomialClassificationPerformanceEvaluator(OperatorDescription description) {
		super(description);
		for (int i = 0; i < USER_CRITERIA_NAMES.length; i++) {
			addUserPerformanceValue(USER_CRITERIA_NAMES[i], "The user defined performance criterion " + i);
		}
	}

	private void addUserPerformanceValue(final String name, String description) {
		addValue(new ValueDouble(name, description) {
			public double getDoubleValue() {
				int index = Integer.parseInt(name.substring(4)) - 1;
				PerformanceCriterion c = userCriteria.get(index);
				return c.getAverage();
			}
		});
	}
	
	@Override
	protected void checkCompatibility(ExampleSet exampleSet)
			throws OperatorException {
		Tools.isLabelled(exampleSet);
		Tools.isNonEmpty(exampleSet);
		
		Attribute label = exampleSet.getAttributes().getLabel();
		if (!label.isNominal()) {
			throw new UserError(this, 101, "the calculation of performance criteria for binominal classification tasks", label.getName());
		}
		
		if (label.getMapping().size() != 2) {
			throw new UserError(this, 114, "the calculation of performance criteria for binominal classification tasks", label.getName());
		}

	}

	@Override
	protected double[] getClassWeights(Attribute label)
			throws UndefinedParameterError {
		return null;
	}
	
	/** Returns false. */
	protected boolean showCriteriaParameter() {
		return false;
	}
	
	@Override
	public List<PerformanceCriterion> getCriteria() {
		List<PerformanceCriterion> performanceCriteria = new LinkedList<PerformanceCriterion>();

		for (int i = 0; i < SIMPLE_CRITERIA_CLASSES.length; i++) {
			try {
				performanceCriteria.add((PerformanceCriterion)SIMPLE_CRITERIA_CLASSES[i].newInstance());
			} catch (InstantiationException e) {
				LogService.getGlobal().logError("Cannot instantiate " + SIMPLE_CRITERIA_CLASSES[i] + ". Skipping...");
			} catch (IllegalAccessException e) {
				LogService.getGlobal().logError("Cannot instantiate " + SIMPLE_CRITERIA_CLASSES[i] + ". Skipping...");
			}
		}
		
		// binary classification criteria
		for (int i = 0; i < BinaryClassificationPerformance.NAMES.length; i++) {
			performanceCriteria.add(new BinaryClassificationPerformance(i));
		}

		if (this.userCriteria != null)
			this.userCriteria.clear();
		Iterator i = null;
		try {
			i = getParameterList(PARAMETER_ADDITIONAL_PERFORMANCE_CRITERIA).iterator();
		} catch (UndefinedParameterError e1) {
			logError("No additional performance criteria defined. No criteria will be calculated...");
		}
		if (i != null) {
			while (i.hasNext()) {
				Object[] keyValue = (Object[]) i.next();
				String className = (String) keyValue[0];
				//String parameter = (String) keyValue[1];

				ClassificationPerformanceCriterion criterion = new ClassificationPerformanceCriterion(className);
				performanceCriteria.add(criterion);
				if (userCriteria != null)
					userCriteria.add(criterion);
			}
		}

		return performanceCriteria;
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeStringCategory(PARAMETER_MAIN_CRITERION, "The criterion used for comparing performance vectors.", USER_CRITERIA_NAMES, USER_CRITERIA_NAMES[0]);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeList(PARAMETER_ADDITIONAL_PERFORMANCE_CRITERIA, "List of classes that implement com.rapidminer..operator.performance.PerformanceCriterion.", new ParameterTypeString("optional_parameters",
				"The key must be a fully qualified classname and the value may be a string that is passed to the constructor of this class.", "")));
		return types;
	}
	
}
