package cc.mallet.fst;

import java.util.logging.Logger;

import cc.mallet.fst.CRF;
import cc.mallet.fst.Transducer;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.Optimizer;
import cc.mallet.types.InstanceList;
import cc.mallet.util.MalletLogger;

public class CRFTrainerByFeatureSubsetting extends TransducerTrainer implements
		TransducerTrainer.ByOptimization {

	private static Logger logger = MalletLogger
			.getLogger(CRFTrainerByFeatureSubsetting.class.getName());

	private static final double DEFAULT_REG_WEIGHT = 1;

	private static final int DEFAULT_NUM_RESETS = 1;

	private int iteration;
	private double regWeight = DEFAULT_REG_WEIGHT;
	private LimitedMemoryBFGS bfgs;

	CRF crf;
	int iterationCount = 0;
	boolean converged;

	boolean usingHyperbolicPrior = false;
	boolean useSparseWeights = true;
	boolean useNoWeights = false; // TODO remove this; it is just for debugging
	private transient boolean useSomeUnsupportedTrick = true;
	// Use mcrf.trainingSet to see when we need to re-allocate crf.weights,
	// expectations & constraints because we are using a different TrainingList
	// than last time

	// xxx temporary hack. This is quite useful to have, though!! -cas
	public boolean printGradient = false;

	private int norm;

	public CRFTrainerByFeatureSubsetting(CRF crf) {
		this.crf = crf;
		this.iteration = 0;
	}

	public void setRegWeight(double regWeight) {
		this.regWeight = regWeight;
	}

	@Override
	public int getIteration() {
		return this.iteration;
	}

	@Override
	public Transducer getTransducer() {
		return this.crf;
	}

	@Override
	public boolean isFinishedTraining() {
		return this.converged;
	}

	/*
	 * This is not used because we require both labeled and unlabeled data.
	 */
	public boolean train(InstanceList trainingSet, int numIterations) {
		throw new RuntimeException(
				"Use train(InstanceList labeled, InstanceList unlabeled, int numIterations) instead.");
	}

	/**
	 * Performs CRF training with label likelihood and entropy regularization.
	 * The CRF is first trained with label likelihood only. This parameter
	 * setting is used as a starting point for the combined optimization.
	 * 
	 * @param labeled
	 *            Labeled data, only used for label likelihood term.
	 * @param unlabeled
	 *            Unlabeled data, only used for entropy regularization term.
	 * @param numIterations
	 *            Number of iterations.
	 * @return True if training has converged.
	 */
	public boolean train(InstanceList labeled, InstanceList unlabeled,
			int numIterations) {
		// if (useSparseWeights)
		// crf.setWeightsDimensionAsIn(labeled, useSomeUnsupportedTrick);//
		// else
		// crf.setWeightsDimensionDensely();
		((CRFIm) crf).setWeightsDimensionIntersect(new InstanceList[] {
				labeled, unlabeled });

		CRFOptimizableByLikelihoodAndExpectationDistance regLikelihood = new CRFOptimizableByLikelihoodAndExpectationDistance(
				crf, labeled, unlabeled);
		regLikelihood.setLambda(regWeight);
		regLikelihood.setGamma(norm);

		this.bfgs = new LimitedMemoryBFGS(regLikelihood);
		converged = false;
		logger.info("CRF about to train with " + numIterations + " iterations");
		// sometimes resetting the optimizer helps to find
		// a better parameter setting
		for (int reset = 0; reset < DEFAULT_NUM_RESETS + 1; reset++) {
			for (int i = 0; i < numIterations; i++) {
				try {
					converged = bfgs.optimize(1);
					iteration++;
					logger.info("CRF finished one iteration of maximizer, i="
							+ i);
					runEvaluators();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					logger.info("Catching exception; saying converged.");
					converged = true;
				} catch (Exception e) {
					e.printStackTrace();
					logger.info("Catching exception; saying converged.");
					converged = true;
				}
				if (converged) {
					logger.info("CRF training has converged, i=" + i);
					break;
				}
			}
			this.bfgs.reset();
		}
		return converged;
	}

	public Optimizer getOptimizer() {
		return bfgs;
	}

	public int getNorm() {
		return norm;
	}

	public void setNorm(int norm) {
		this.norm = norm;
	}

}
