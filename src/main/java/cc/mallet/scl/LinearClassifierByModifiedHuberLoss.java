package cc.mallet.scl;

import java.util.logging.Logger;

import cc.mallet.optimize.GradientAscent;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SparseVector;
import cc.mallet.util.MalletLogger;

public class LinearClassifierByModifiedHuberLoss {
	private static Logger logger = MalletLogger
			.getLogger(LinearClassifierByModifiedHuberLoss.class.getName());
	private SparseVector w;
	private InstanceList instances;
	private int pivot;

	public LinearClassifierByModifiedHuberLoss(SparseVector w, int pivot,
			InstanceList instances) {
		this.w = w;
		this.pivot = pivot;
		this.instances = instances;
	}

	public void train() {
		LinearClassifierByModifiedHuberLossOptimizable optimizable = new LinearClassifierByModifiedHuberLossOptimizable(
				this);
//		StochasticGradientDescent optimizer = new StochasticGradientDescent(
//				optimizable);
		GradientAscent optimizer = new GradientAscent(optimizable);
		optimizer.optimize();
	}

	public int getNumInstances() {
		return instances.size();
	}

	public Instance getInstance(int i) {
		return instances.get(i);
	}

	public SparseVector getX(int i) {
		return (SparseVector) getInstance(i).getData();
	}

	public double getY(int i) {
		return 2 * getX(i).value(pivot) - 1;
	}

	public SparseVector getW() {
		return w;
	}

	public void evaluate() {
		int correct = 0;
		for (int i = 0; i < instances.size(); i++) {
			if (predict(getX(i)) == getY(i))
				correct++;
		}
		logger.info("Accuracy (pivot "+pivot+"): " + (double) correct / instances.size());
	}

	public double predict(SparseVector x) {
		double y = w.dotProduct(x);
		return y >= 0 ? 1 : -1;
	}
}
