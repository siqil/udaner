package cc.mallet.scl;

import cc.mallet.optimize.Optimizable;
import cc.mallet.types.SparseVector;

public class LinearClassifierByModifiedHuberLossOptimizable implements
		Optimizable.ByGradientValue {

	private SparseVector w;
	private double lambda = 1e-5;
	private LinearClassifierByModifiedHuberLoss clf;
	private SparseVector g;
	private double value;
	private boolean isValueCacheValid;
	private boolean isGradientCacheValid;

	public LinearClassifierByModifiedHuberLossOptimizable(
			LinearClassifierByModifiedHuberLoss clf) {
		this.clf = clf;
		this.w = clf.getW();
		this.g = (SparseVector) w.cloneMatrixZeroed();
	}

	public SparseVector getParameters() {
		return w;
	}

	public int getNumInstances() {
		return clf.getNumInstances();
	}

	public int getNumParameters() {
		return w.numLocations();
	}

	public void getParameters(double[] buffer) {
		int nl = w.numLocations();
		for (int j = 0; j < nl; j++)
			buffer[j] = w.valueAtLocation(j);
	}

	public double getParameter(int index) {
		return w.valueAtLocation(index);
	}

	public void setParameters(double[] params) {
		int nl = w.numLocations();
		for (int j = 0; j < nl; j++)
			w.setValueAtLocation(j, params[j]);
		isValueCacheValid = false;
		isGradientCacheValid = false;
	}

	public void setParameter(int index, double value) {
		w.setValueAtLocation(index, value);
		isValueCacheValid = false;
		isGradientCacheValid = false;
	}

	public SparseVector getValueGradient(int i) {
		SparseVector x = clf.getX(i);
		double y = clf.getY(i);
		double p = w.dotProduct(x);
		double py = p * y;
		double phi_d;
		if (py >= 1) {
			phi_d = 0;
		} else if (py <= -1) {
			phi_d = -4 * y;
		} else {
			phi_d = -2 * (1 - py) * y;
		}
		g.setAll(0);
		g.plusEqualsSparse(w, lambda);
		g.plusEqualsSparse(x, phi_d);
		return g;
	}

	public double getValue(int i) {
		SparseVector x = clf.getX(i);
		double y = clf.getY(i);
		double py = w.dotProduct(x) * y;
		double loss;
		if (py >= -1) {
			py = Math.max(0, 1 - py);
			loss = py * py;
		} else {
			loss = -4 * py;
		}
		double norm = w.twoNorm();
		return loss + lambda * norm * norm / 2;
	}

	public void getValueGradient(double[] buffer) {
		if (!isGradientCacheValid) {
			int n = getNumInstances();
			g.setAll(0);
			g.plusEqualsSparse(w, -lambda);
			for (int i = 0; i < n; i++) {
				SparseVector x = clf.getX(i);
				double y = clf.getY(i);
				double p = w.dotProduct(x);
				double py = p * y;
				double phi_d;
				if (py >= 1) {
					phi_d = 0;
				} else if (py <= -1) {
					phi_d = -4 * y;
				} else {
					phi_d = -2 * (1 - py) * y;
				}
				g.plusEqualsSparse(x, -phi_d / n);
			}
			isGradientCacheValid = true;
		}
		int nl = g.numLocations();
		for (int j = 0; j < nl; j++)
			buffer[j] = g.valueAtLocation(j);
	}

	public double getValue() {
		if (!isValueCacheValid) {
			int n = getNumInstances();
			double norm = w.twoNorm();
			value = 0;
			for (int i = 0; i < n; i++) {
				SparseVector x = clf.getX(i);
				double y = clf.getY(i);
				double py = w.dotProduct(x) * y;
				double loss;
				if (py >= -1) {
					py = Math.max(0, 1 - py);
					loss = py * py;
				} else {
					loss = -4 * py;
				}
				value += loss;
			}
			value = -(value / n + lambda * norm * norm / 2);
			isValueCacheValid = true;
		}
		return value;
	}
}
