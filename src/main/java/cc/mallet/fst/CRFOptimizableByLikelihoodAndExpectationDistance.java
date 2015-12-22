package cc.mallet.fst;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Logger;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.MatrixOps;
import cc.mallet.types.SparseVector;
import cc.mallet.fst.CRF;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.fst.Transducer;
import cc.mallet.fst.CRF.Factors;
import cc.mallet.fst.CRF.Factors.Incrementor;
import cc.mallet.fst.CRF.Factors.WeightedIncrementor;
import cc.mallet.optimize.Optimizable;
import cc.mallet.util.MalletLogger;

public class CRFOptimizableByLikelihoodAndExpectationDistance implements
		Optimizable.ByGradientValue, Serializable {

	private static Logger logger = MalletLogger
			.getLogger(CRFOptimizableByLikelihoodAndExpectationDistance.class
					.getName());

	// gsc: changing field access to make this class extensible
	protected InstanceList sourceInstances, targetInstances;
	protected double cachedValue = -123456789;
	protected double[] cachedGradient;
	protected BitSet infiniteValues = null;
	protected CRF crf;
	protected CRF.Factors constraints, expectations;
	protected CRF.Factors sourceExpectations, targetExpectations, distances;
	// Various values from CRF acting as indicators of when we need to ...
	private int cachedValueWeightsStamp = -1; // ... re-calculate expectations
												// and values to getValue()
												// because weights' values
												// changed
	private int cachedGradientWeightsStamp = -1; // ... re-calculate to
													// getValueGradient()
													// because weights' values
													// changed

	private double lambda = 1;

	private int gamma = 1;

	public CRFOptimizableByLikelihoodAndExpectationDistance(CRF crf,
			InstanceList sourceInstances, InstanceList targetInstances) {
		// Set up
		this.crf = crf;
		this.sourceInstances = sourceInstances;
		this.targetInstances = targetInstances;
		// cachedGradient = new DenseVector (numParameters);
		cachedGradient = new double[crf.parameters.getNumFactors()];

		constraints = new CRF.Factors(crf.parameters);
		expectations = new CRF.Factors(crf.parameters);
		sourceExpectations = new CRF.Factors(crf.parameters);
		targetExpectations = new CRF.Factors(crf.parameters);
		distances = new CRF.Factors(crf.parameters);
		// This resets and values that may have been in expectations and
		// constraints
		// reallocateSufficientStatistics();

		// This is unfortunately necessary, b/c cachedValue & cachedValueStale
		// not in same place!
		cachedValueWeightsStamp = -1;
		cachedGradientWeightsStamp = -1;

		gatherConstraints();
		getSourceExpectations();
	}

	protected void gatherConstraints() {
		// Set the constraints by running forward-backward with the *output
		// label sequence provided*, thus restricting it to only those
		// paths that agree with the label sequence.
		// Zero the constraints[]
		// Reset constraints[] to zero before we fill them again
		assert (constraints.structureMatches(crf.parameters));
		constraints.zero();

		for (Instance instance : sourceInstances) {
			FeatureVectorSequence input = (FeatureVectorSequence) instance
					.getData();
			FeatureSequence output = (FeatureSequence) instance.getTarget();
			double instanceWeight = sourceInstances.getInstanceWeight(instance);
			// System.out.println
			// ("Constraint-gathering on instance "+i+" of "+ilist.size());
			Transducer.Incrementor incrementor = instanceWeight == 1.0 ? constraints.new Incrementor()
					: constraints.new WeightedIncrementor(instanceWeight);
			new SumLatticeDefault(this.crf, input, output, incrementor);
		}
		// System.out.println ("testing Value and Gradient");
		// TestOptimizable.testValueAndGradientCurrentParameters (this);
	}

	protected void getSourceExpectations() {
		assert (sourceExpectations.structureMatches(crf.parameters));
		sourceExpectations.zero();

		for (Instance instance : sourceInstances) {
			FeatureVectorSequence input = (FeatureVectorSequence) instance
					.getData();
			FeatureSequence output = (FeatureSequence) instance.getTarget();
			double instanceWeight = sourceInstances.getInstanceWeight(instance);
			Transducer.Incrementor incrementor = instanceWeight == 1.0 ? sourceExpectations.new Incrementor()
					: sourceExpectations.new WeightedIncrementor(instanceWeight);
			new SumLatticeDefault(this.crf, input, output, incrementor);
		}
		double factor = 1.0 / sourceInstances.size();
		for (int i = 0; i < sourceExpectations.weights.length; i++) {
			log(sourceExpectations.weights[i], factor);
		}
	}

	protected void getTargetExpectations() {
		// Reset expectations to zero before we fill them again
		assert (targetExpectations.structureMatches(crf.parameters));
		targetExpectations.zero();

		// Calculate the value of each instance, and also fill in expectations
		for (Instance instance : targetInstances) {
			FeatureVectorSequence input = (FeatureVectorSequence) instance
					.getData();
			double instanceWeight = targetInstances.getInstanceWeight(instance);
			Transducer.Incrementor incrementor = instanceWeight == 1.0 ? targetExpectations.new Incrementor()
					: targetExpectations.new WeightedIncrementor(instanceWeight);
			new SumLatticeDefault(this.crf, input, null, incrementor);
		}
		double factor = 1.0 / targetInstances.size();
		for (int i = 0; i < targetExpectations.weights.length; i++) {
			log(targetExpectations.weights[i], factor);
		}
	}

	protected double getDistances() {
		getTargetExpectations();
		double cost = 0;
		// System.out.println("Weights.length: " + distances.weights.length);
		for (int i = 0; i < distances.weights.length; i++) {
			SparseVector dv = distances.weights[i];
			SparseVector sev = sourceExpectations.weights[i];
			SparseVector tev = targetExpectations.weights[i];
			SparseVector wv = crf.parameters.weights[i];
			for (int j = 0; j < dv.numLocations(); j++) {
				// int i1 = dv.indexAtLocation(j);
				// int i2 = sev.indexAtLocation(j);
				// int i3 = tev.indexAtLocation(j);
				// int i4 = wv.indexAtLocation(j);
				// if (!(i1 == i2 && i2 == i3 && i3 == i4 && i4 == i1)) {
				// throw new RuntimeException("Impossible!");
				// }
				double diff = sev.valueAtLocation(j) - tev.valueAtLocation(j);
				// System.out.println(sev.valueAtLocation(j) + " - "
				// + tev.valueAtLocation(j));
				// System.out.println("Diff: " + diff);
				if (Double.isInfinite(diff) || Double.isNaN(diff)) {
					throw new RuntimeException("Impossible value: " + diff);
				} else {
					if (gamma == 2) {
						double w = wv.valueAtLocation(j);
						diff = diff * diff;
						cost += w * w * diff;
						double deriv = 2 * w * diff;
						dv.setValueAtLocation(j, deriv);
					} else if (gamma == 1) {
						double w = wv.valueAtLocation(j);
						diff = diff * diff;
						double abs = Math.sqrt(w * w + Double.MIN_VALUE);
						cost += abs * diff;
						double deriv = w * diff / abs;
						dv.setValueAtLocation(j, deriv);
					} else {
						throw new IllegalArgumentException(
								"Gamma must be 1 or 2");
					}
				}
			}
		}
		return cost;
	}

	static void log(SparseVector v, double factor) {
		for (int i = 0; i < v.numLocations(); i++) {
			double value = factor * v.valueAtLocation(i);
			v.setValueAtLocation(i, Math.log(value));
		}
	}

	static void square(SparseVector v) {
		for (int i = 0; i < v.numLocations(); i++) {
			v.setValueAtLocation(i, v.valueAtLocation(i) * v.valueAtLocation(i));
		}
	}

	public int getNumParameters() {
		return crf.parameters.getNumFactors();
	}

	public void getParameters(double[] buffer) {
		crf.parameters.getParameters(buffer);
	}

	public double getParameter(int index) {
		return crf.parameters.getParameter(index);
	}

	public void setParameters(double[] buff) {
		crf.parameters.setParameters(buff);
		crf.weightsValueChanged();
	}

	public void setParameter(int index, double value) {
		crf.parameters.setParameter(index, value);
		crf.weightsValueChanged();
	}

	// log probability of the training sequence labels, and fill in
	// expectations[]
	protected double getExpectationValue() {
		// Instance values must either always or never be included in
		// the total values; we can't just sometimes skip a value
		// because it is infinite, this throws off the total values.
		boolean initializingInfiniteValues = false;
		double value = 0;
		if (infiniteValues == null) {
			infiniteValues = new BitSet();
			initializingInfiniteValues = true;
		}

		// Reset expectations to zero before we fill them again
		assert (expectations.structureMatches(crf.parameters));
		expectations.zero();

		// count the number of instances that have infinite weight
		int numInfLabeledWeight = 0;
		int numInfUnlabeledWeight = 0;
		int numInfWeight = 0;

		// Calculate the value of each instance, and also fill in expectations
		double unlabeledWeight, labeledWeight, weight;
		for (int ii = 0; ii < sourceInstances.size(); ii++) {
			Instance instance = sourceInstances.get(ii);
			double instanceWeight = sourceInstances.getInstanceWeight(instance);
			FeatureVectorSequence input = (FeatureVectorSequence) instance
					.getData();
			FeatureSequence output = (FeatureSequence) instance.getTarget();
			labeledWeight = new SumLatticeDefault(this.crf, input, output,
					(Transducer.Incrementor) null).getTotalWeight();
			String instanceName = instance.getName() == null ? "instance#" + ii
					: instance.getName().toString();
			// System.out.println ("labeledWeight = "+labeledWeight);
			if (Double.isInfinite(labeledWeight)) {
				++numInfLabeledWeight;
				logger.warning(instanceName
						+ " has -infinite labeled weight.\n"
						+ (instance.getSource() != null ? instance.getSource()
								: ""));
			}

			Transducer.Incrementor incrementor = instanceWeight == 1.0 ? expectations.new Incrementor()
					: expectations.new WeightedIncrementor(instanceWeight);
			unlabeledWeight = new SumLatticeDefault(this.crf, input, null,
					incrementor).getTotalWeight();
			// System.out.println ("unlabeledWeight = "+unlabeledWeight);
			if (Double.isInfinite(unlabeledWeight)) {
				++numInfUnlabeledWeight;
				logger.warning(instance.getName().toString()
						+ " has -infinite unlabeled weight.\n"
						+ (instance.getSource() != null ? instance.getSource()
								: ""));
			}

			// Here weight is log(conditional probability correct label
			// sequence)
			weight = labeledWeight - unlabeledWeight;
			// System.out.println
			// ("Instance "+ii+" CRF.MaximizableCRF.getWeight = "+weight);
			if (Double.isInfinite(weight)) {
				++numInfWeight;
				logger.warning(instanceName
						+ " has -infinite weight; skipping.");
				if (initializingInfiniteValues)
					infiniteValues.set(ii);
				else if (!infiniteValues.get(ii))
					throw new IllegalStateException(
							"Instance i used to have non-infinite value, but now it has infinite value.");
				continue;
			}
			// Weights are log probabilities, and we want to return a log
			// probability
			value += weight * instanceWeight;
		}

		if (numInfLabeledWeight > 0 || numInfUnlabeledWeight > 0
				|| numInfWeight > 0) {
			logger.warning("Number of instances with:\n"
					+ "\t -infinite labeled weight: " + numInfLabeledWeight
					+ "\n" + "\t -infinite unlabeled weight: "
					+ numInfUnlabeledWeight + "\n" + "\t -infinite weight: "
					+ numInfWeight);
		}

		return value;
	}

	/**
	 * Returns the log probability of the training sequence labels and the prior
	 * over parameters.
	 */
	public double getValue() {
		if (crf.weightsValueChangeStamp != cachedValueWeightsStamp) {
			// The cached value is not up to date; it was calculated for a
			// different set of CRF weights.
			cachedValueWeightsStamp = crf.weightsValueChangeStamp; // cachedValue
																	// will soon
																	// no longer
																	// be stale
			long startingTime = System.currentTimeMillis();
			// crf.print();

			// Get the value of all the all the true labels, also filling in
			// expectations at the same time.

			cachedValue = -lambda * getDistances(); // this set weights for
													// invalid features to 0
			cachedValue += getExpectationValue();

			assert (!(Double.isNaN(cachedValue) || Double
					.isInfinite(cachedValue))) : "Label likelihood is NaN/Infinite";

			logger.info("getValue() (loglikelihood, optimizable by label likelihood) = "
					+ cachedValue);
			long endingTime = System.currentTimeMillis();
			logger.fine("Inference milliseconds = "
					+ (endingTime - startingTime));
		}
		return cachedValue;
	}

	// gsc: changing method from assertNotNaN to assertNotNaNOrInfinite
	private void assertNotNaNOrInfinite() {
		// crf.parameters are allowed to have infinite values
		crf.parameters.assertNotNaN();
		expectations.assertNotNaNOrInfinite();
		constraints.assertNotNaNOrInfinite();
	}

	public void zero(CRF.Factors f) {
		Arrays.fill(f.defaultWeights, 0);
		Arrays.fill(f.initialWeights, 0);
		Arrays.fill(f.finalWeights, 0);
	}

	public void getValueGradient(double[] buffer) {
		// PriorGradient is -parameter/gaussianPriorVariance
		// Gradient is (constraint - expectation + PriorGradient)
		// == -(expectation - constraint - PriorGradient).
		// Gradient points "up-hill", i.e. in the direction of higher value
		if (cachedGradientWeightsStamp != crf.weightsValueChangeStamp) {
			cachedGradientWeightsStamp = crf.weightsValueChangeStamp;

			getValue();
			assertNotNaNOrInfinite();

			// Gradient is constraints - expectations - distance. We do this by
			// -(expectations - constraints + distance).

			expectations.plusEquals(constraints, -1.0);
			expectations.plusEquals(distances, lambda);
			// we keep weights for invalid features being zeros by setting their
			// gradients to zeros
			// setInvalidToZero(expectations.weights);

			expectations.assertNotNaNOrInfinite();
			expectations.getParameters(cachedGradient);
			MatrixOps.timesEquals(cachedGradient, -1.0);
		}
		// What the heck was this!?: if (buffer.length != this.numParameters)
		// buffer = new double[this.numParameters];
		System.arraycopy(cachedGradient, 0, buffer, 0, cachedGradient.length);
		// Arrays.fill (buffer, 0.0);
		// System.arraycopy(cachedGradie, 0, buffer, 0,
		// 2*crf.parameters.initialWeights.length); // TODO For now, just copy
		// the state inital/final weights
	}

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(sourceInstances);
		out.writeDouble(cachedValue);
		out.writeObject(cachedGradient);
		out.writeObject(infiniteValues);
		out.writeObject(crf);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		int version = in.readInt();
		sourceInstances = (InstanceList) in.readObject();
		cachedValue = in.readDouble();
		cachedGradient = (double[]) in.readObject();
		infiniteValues = (BitSet) in.readObject();
		crf = (CRF) in.readObject();
	}

	public void setLambda(double regWeight) {
		this.lambda = regWeight;
	}

	public int getGamma() {
		return gamma;
	}

	public void setGamma(int gamma) {
		this.gamma = gamma;
	}

}
