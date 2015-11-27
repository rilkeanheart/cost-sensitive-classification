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

import java.awt.Component;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTabbedPane;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;

/**
 * The model for the internal Bagging implementation.
 * 
 * @author Michael Green
 * @version $Id: CostModel.java,v 1.6 2008/07/27 19:22:48 greenm9 Exp $
 */
public class CostingModel extends PredictionModel {

	private static final long serialVersionUID = -4691755811263523354L;
	
	// Holds the models
	private List<Model> models;

	public CostingModel(ExampleSet exampleSet, List<Model> models) {
		super(exampleSet);
		this.models = models;
	}

	public Component getVisualizationComponent(IOContainer container) {
		JTabbedPane tabPane = new ExtendedJTabbedPane();
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			Model model = this.getModel(i);
			tabPane.add("Model " + (i + 1), model.getVisualizationComponent(container));
		}
		return tabPane;
	}

	/** @return a <code>String</code> representation of this boosting model. */
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparator() + "Number of inner models: " + this.getNumberOfModels() + Tools.getLineSeparators(2));
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			Model model = this.getModel(i);
			result.append((i > 0 ? Tools.getLineSeparator() : "") 
			       + "Embedded model #" + i + ":" + Tools.getLineSeparator() + model.toResultString());
		}
		return result.toString();
	}

	/** @return the number of embedded models */
	public int getNumberOfModels() {
		return this.models.size();
	}

	/**
	 * Getter method for embedded models
	 * 
	 * @param index
	 *            the number of a model part of this boost model
	 * @return binary or nominal decision model
	 */
	public Model getModel(int index) {
		return this.models.get(index);
	}

	@Override
	/**
	 * Iterates over all models and averages confidences.
	 * 
	 * @param origExampleSet
	 *            the set of examples to be classified
	 */
	public ExampleSet performPrediction(ExampleSet origExampleSet, Attribute predictedLabel) throws OperatorException {
		final String attributePrefix = "CostingModelPrediction";
		final int numLabels = predictedLabel.getMapping().size();
		final Attribute[] specialAttributes = new Attribute[numLabels];
		for (int i = 0; i < numLabels; i++) {
			specialAttributes[i] =
				com.rapidminer.example.Tools.createSpecialAttribute(origExampleSet, attributePrefix + i, Ontology.NUMERICAL);
		}

		Iterator<Example> reader = origExampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			for (int i = 0; i < specialAttributes.length; i++) {
				example.setValue(specialAttributes[i], 0);
			}
		}

		reader = origExampleSet.iterator();
		for (int modelNr = 0; modelNr < this.getNumberOfModels(); modelNr++) {
			Model model = this.getModel(modelNr);
			ExampleSet exampleSet = (ExampleSet) origExampleSet.clone();
			exampleSet = model.apply(exampleSet);
			this.updateEstimates(exampleSet, modelNr, specialAttributes);
			PredictionModel.removePredictedLabel(exampleSet);
		}

		// Turn prediction weights into confidences and a crisp prediction:
		this.evaluateSpecialAttributes(origExampleSet, specialAttributes);

		// Clean up attributes:
		for (int i = 0; i < numLabels; i++) {
			origExampleSet.getAttributes().remove(specialAttributes[i]);
			origExampleSet.getExampleTable().removeAttribute(specialAttributes[i]);
		}
		
		return origExampleSet;
	}

	private void updateEstimates(ExampleSet exampleSet, int modelNr, Attribute[] specialAttributes) {
		final int numModels = this.getNumberOfModels();
		//final int numClasses = this.getLabel().getMapping().size();
		
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			
			int nPrediction = (int) example.getPredictedLabel();
			double value = example.getValue(specialAttributes[nPrediction]);
			value += (new Double(1)/numModels);
			example.setValue(specialAttributes[nPrediction], value);
			/*for (int i=0; i<numClasses; i++) {
				String consideredPrediction = this.getLabel().getMapping().mapIndex(i);
				double confidence = example.getConfidence(consideredPrediction);
				double value = example.getValue(specialAttributes[i]);
				value += confidence / numModels;
				example.setValue(specialAttributes[i], value);
			}*/
		}
	}

	private void evaluateSpecialAttributes(ExampleSet exampleSet, Attribute[] specialAttributes) {
		Attribute exSetLabel = exampleSet.getAttributes().getLabel();
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			int bestLabel = 0;
			double bestConf = -1;
			for (int n = 0; n < specialAttributes.length; n++) {
				double curConf = example.getValue(specialAttributes[n]);
				String curPredS = this.getLabel().getMapping().mapIndex(n);
				example.setConfidence(curPredS, curConf);

				if (curConf > bestConf) {
					bestConf = curConf;
					bestLabel = n;
					if(bestLabel == 0)
						n = bestLabel;
				}
			}

			example.setValue(example.getAttributes().getPredictedLabel(), exSetLabel.getMapping().mapString(this.getLabel().getMapping().mapIndex(bestLabel)));			
		}
	}

}
