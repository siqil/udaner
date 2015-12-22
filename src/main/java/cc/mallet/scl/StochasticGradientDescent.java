package cc.mallet.scl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import cc.mallet.types.IndexedSparseVector;
import cc.mallet.types.MatrixOps;
import cc.mallet.types.SparseVector;
import cc.mallet.util.MalletLogger;

public class StochasticGradientDescent {

	private static Logger logger = MalletLogger
			.getLogger(StochasticGradientDescent.class.getName());

	private LinearClassifierByModifiedHuberLossOptimizable optimizable;
	final double tolerance = .0001;

	public StochasticGradientDescent(
			LinearClassifierByModifiedHuberLossOptimizable optimizable) {
		this.optimizable = optimizable;
	}

	public boolean optimize() {
		// return test(1000000);
		return optimize(1000000);
	}

	public boolean optimize(int maxIteration) {
		int n = optimizable.getNumInstances();
		ArrayList<Integer> index = new ArrayList<Integer>(n);
		for (int i = 0; i < n; i++) {
			index.add(i);
		}
		Collections.shuffle(index);
		int iter = 0;
		SparseVector w = optimizable.getParameters();
		logger.info("Number of parameters (binary: " + w.isBinary()
				+ " indexed: " + (w instanceof IndexedSparseVector) + "): "
				+ optimizable.getNumParameters());
		SparseVector w_old = (SparseVector) w.cloneMatrix();
		SparseVector g = null;
		// set the check interval to 1% of maxIteration
		int K = (int) (maxIteration * 0.01);
		double step = 1;
		while (iter < maxIteration) {
			int i = index.get(iter % n);
			step = 1.0 / (1 + iter);
			g = optimizable.getValueGradient(i);
			w.plusEqualsSparse(g, -step);
			iter++;
			if (iter % K == 0) {
				double norm = w.twoNorm() + w_old.twoNorm() + 1e-10;
				w_old.plusEqualsSparse(w, -1);
				double diff = w_old.twoNorm();
				diff = 2 * diff / norm;
				logger.info("Iteration: " + iter);
				logger.info("2*||w-w_old||/(||w||+||w_old||) = " + diff);
				if (diff < tolerance) {
					logger.info("Exiting SGD: diff = " + diff + " < " + tolerance);
					logger.info("Converged");
					return true;
				}
				w_old.arrayCopyFrom(w.getValues());
			}
		}
		logger.info("Not converged but exceeding maxIteration");
		return false;
	}

	public boolean test(int maxIteration) {
		int n = optimizable.getNumInstances();
		ArrayList<Integer> index = new ArrayList<Integer>(n);
		for (int i = 0; i < n; i++) {
			index.add(i);
		}
		Collections.shuffle(index);
		int iter = 0;
		SparseVector w = optimizable.getParameters();
		SparseVector g = null;
		double step = 1e-6;
		double loss, loss1;
		while (iter < maxIteration) {
			int pos = (int) (Math.random() * w.numLocations());
			double value = Math.random() * 10000;
			System.out.println("Change value of W at position " + pos + " to "
					+ value);
			w.setValueAtLocation(pos, value);
			int i = index.get(iter % n);
			g = optimizable.getValueGradient(i);
			loss = optimizable.getValue(i);
			// end test
			// System.arraycopy(parameter, 0, old, 0, old.length);
			w.plusEqualsSparse(g, -step);
			loss1 = optimizable.getValue(i);
			g.timesEqualsSparse(g, -step);
			double delta = g.oneNorm();
			double diff = Math.abs(loss1 - (loss + delta));
			System.out.println(diff);
			if (diff > step) {
				System.out.println("Failed");
				return false;
			}
			iter++;
		}
		System.out.println("Pass");
		return true;
	}
}
