package cc.mallet.fst;

import java.util.BitSet;
import java.util.logging.Logger;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.IndexedSparseVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SparseVector;
import cc.mallet.util.MalletLogger;

public class CRFIm extends CRF {
	private static Logger logger = MalletLogger.getLogger(CRF.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = -5720157375366185643L;

	public CRFIm(CRF other) {
		super(other);
	}

	public CRFIm(Pipe inputPipe, Pipe outputPipe) {
		super(inputPipe, outputPipe);
	}

	public CRFIm(Alphabet inputAlphabet, Alphabet outputAlphabet) {
		super(inputAlphabet, outputAlphabet);
	}

	public void setWeightsDimensionIntersect(InstanceList... allDatasets) {
		final BitSet[][] allWeightsPresent = new BitSet[allDatasets.length][];
		int numWeights = 0;
		// The value doesn't actually change, because the "new" parameters will
		// have zero value
		// but the gradient changes because the parameters now have different
		// layout.
		weightsStructureChanged();

		// Put in the weights in the training set
		for (int d = 0; d < allDatasets.length; d++) {
			final BitSet[] weightsPresent = allWeightsPresent[d] = new BitSet[parameters.weights.length];
			for (int i = 0; i < parameters.weights.length; i++)
				weightsPresent[i] = new BitSet();
			// Put in the weights that are already there
			for (int i = 0; i < parameters.weights.length; i++)
				for (int j = parameters.weights[i].numLocations() - 1; j >= 0; j--)
					weightsPresent[i].set(parameters.weights[i]
							.indexAtLocation(j));
			InstanceList dataset = allDatasets[d];
			for (int i = 0; i < dataset.size(); i++) {
				Instance instance = dataset.get(i);
				FeatureVectorSequence input = (FeatureVectorSequence) instance
						.getData();
				FeatureSequence output = (FeatureSequence) instance.getTarget();
				// gsc: trainingData can have unlabeled instances as well
				if (output != null && output.size() > 0) {
					// Do it for the paths consistent with the labels...
					sumLatticeFactory.newSumLattice(this, input, output,
							new Transducer.Incrementor() {
								public void incrementTransition(
										Transducer.TransitionIterator ti,
										double count) {
									State source = (CRF.State) ti
											.getSourceState();
									FeatureVector input = (FeatureVector) ti
											.getInput();
									int index = ti.getIndex();
									int nwi = source.weightsIndices[index].length;
									for (int wi = 0; wi < nwi; wi++) {
										int weightsIndex = source.weightsIndices[index][wi];
										for (int i = 0; i < input
												.numLocations(); i++) {
											int featureIndex = input
													.indexAtLocation(i);
											if ((globalFeatureSelection == null || globalFeatureSelection
													.contains(featureIndex))
													&& (featureSelections == null
															|| featureSelections[weightsIndex] == null || featureSelections[weightsIndex]
																.contains(featureIndex)))
												weightsPresent[weightsIndex]
														.set(featureIndex);
										}
									}
								}

								public void incrementInitialState(
										Transducer.State s, double count) {
								}

								public void incrementFinalState(
										Transducer.State s, double count) {
								}
							});
				}
			}
		}
		BitSet[] intersect = new BitSet[parameters.weights.length];
		for (int i = 0; i < parameters.weights.length; i++) {
			intersect[i] = allWeightsPresent[0][i];
			for (int j = 1; j < allWeightsPresent.length; j++) {
				intersect[i].and(allWeightsPresent[j][i]);
			}
		}
		SparseVector[] newWeights = new SparseVector[parameters.weights.length];
		for (int i = 0; i < parameters.weights.length; i++) {
			int numLocations = intersect[i].cardinality();
			logger.info("CRF weights["
					+ parameters.weightAlphabet.lookupObject(i)
					+ "] num features = " + numLocations);
			int[] indices = new int[numLocations];
			for (int j = 0; j < numLocations; j++) {
				indices[j] = intersect[i].nextSetBit(j == 0 ? 0
						: indices[j - 1] + 1);
			}
			newWeights[i] = new IndexedSparseVector(indices,
					new double[numLocations], numLocations, numLocations,
					false, false, false);
			newWeights[i].plusEqualsSparse(parameters.weights[i]); // Put in the
																	// previous
																	// weights
			numWeights += (numLocations + 1);
		}
		logger.info("Number of weights = " + numWeights);
		parameters.weights = newWeights;
	}

}
