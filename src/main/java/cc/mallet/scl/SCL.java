package cc.mallet.scl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

import cc.mallet.types.FeatureVector;
import cc.mallet.types.IndexedSparseVector;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SparseVector;
import cc.mallet.util.MalletLogger;

public class SCL implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2467855691908875754L;
	private static Logger logger = MalletLogger.getLogger(SCL.class.getName());
	private int threads = 1;

	int[] pivots; // indices of pivots
	SparseVector[] masks; // masks for pivots
	Map<String, Set<Integer>> groups;
	int h; // # of rows of theta
	List<SparseVector> theta; // rows of theta
	InstanceList instances;

	public SCL(File mat, Map<String, Set<Integer>> groups, int h) {
		this.h = h;
		this.groups = groups;
		svd(mat);
	}

	public SCL(int[] pivots, SparseVector[] masks,
			Map<String, Set<Integer>> groups, int h) {
		this.pivots = pivots;
		this.masks = masks;
		this.groups = groups;
		this.h = h;
	}

	private class TrainThread implements Runnable {
		private int numThread;
		private CountDownLatch latch;
		private SparseVector[] ws;
		private int t;

		public TrainThread(SparseVector[] ws, int t, int numThread,
				CountDownLatch latch) {
			this.numThread = numThread;
			this.latch = latch;
			this.ws = ws;
			this.t = t;
		}

		public void run() {
			try {
				int pivot;
				for (int i = t; i < pivots.length; i += numThread) {
					pivot = pivots[i];
					SparseVector mask = masks[i];
					SparseVector w = (SparseVector) mask.cloneMatrixZeroed();
					// train a linear classifier for pivot feature
					LinearClassifierByModifiedHuberLoss clf = new LinearClassifierByModifiedHuberLoss(
							w, pivot, instances);
					clf.train();
					clf.evaluate();
					ws[i] = w;
					logger.info("Thread " + t + " finish training pivot(" + i
							+ "): " + pivot);
				}
			} finally {
				latch.countDown();
			}
		}
	}

	public SparseVector[] trainByMultiThread(final int numThread) {
		final SparseVector[] ws = new SparseVector[pivots.length];
		Thread[] threads = new Thread[numThread];
		CountDownLatch latch = new CountDownLatch(numThread);
		for (int t = 0; t < threads.length; t++) {
			threads[t] = new Thread(new TrainThread(ws, t, numThread, latch));
			threads[t].start();
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return ws;
	}

	public SparseVector[] trainBySingleThread() {
		SparseVector[] ws = new SparseVector[pivots.length];
		int pivot;
		for (int i = 0; i < pivots.length; i++) {
			pivot = pivots[i];
			SparseVector mask = masks[i];
			SparseVector w = (SparseVector) mask.cloneMatrixZeroed();
			// train a linear classifier for pivot feature
			LinearClassifierByModifiedHuberLoss clf = new LinearClassifierByModifiedHuberLoss(
					w, pivot, instances);
			clf.train();
			clf.evaluate();
			ws[i] = w;
			logger.info("Finish training pivot " + pivot);
		}
		return ws;
	}

	public void svd(File file) {
		try {
			SVDLIBC svd = new SVDLIBC();
			List<SparseVector> mat = svd.readSparse(file);
			svd(mat);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int[] ListToIntArray(List<Integer> list) {
		int[] arr = new int[list.size()];
		int i = 0;
		for (Integer e : list) {
			arr[i++] = e;
		}
		return arr;
	}

	private double[] ListToDoubleArray(List<Double> list) {
		double[] arr = new double[list.size()];
		int i = 0;
		for (Double e : list) {
			arr[i++] = e;
		}
		return arr;
	}

	public void svd(List<SparseVector> mat) {
		theta = new ArrayList<SparseVector>();
		for (String key : groups.keySet()) {
			Set<Integer> select = groups.get(key);
			List<SparseVector> sub = new ArrayList<SparseVector>();
			// maps original index to index in the sub matrix
			Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
			Map<Integer, Integer> reverseIndexMap = new HashMap<Integer, Integer>();
			int newIndex = 0;
			for (Integer oldIndex : select) {
				indexMap.put(oldIndex, newIndex);
				reverseIndexMap.put(newIndex, oldIndex);
				newIndex++;
			}
			for (SparseVector col : mat) {
				List<Integer> indices = new ArrayList<Integer>();
				List<Double> values = new ArrayList<Double>();
				for (int l = 0; l < col.numLocations(); l++) {
					int i = col.indexAtLocation(l);
					if (select.contains(i)) {
						double val = col.valueAtLocation(l);
						if (val != 0) {
							indices.add(indexMap.get(i));
							values.add(val);
						}
					}
				}
				sub.add(new IndexedSparseVector(ListToIntArray(indices),
						ListToDoubleArray(values)));
			}
			try {
				SVDLIBC svd = new SVDLIBC();
				svd.svd(sub, select.size(), sub.size(), h);
				List<SparseVector> Ut = svd.getUt();
				for (SparseVector v : Ut) {
					int[] indices = new int[v.numLocations()];
					double[] values = new double[v.numLocations()];
					for (int l = 0; l < v.numLocations(); l++) {
						int i = v.indexAtLocation(l);
						double value = v.valueAtLocation(l);
						indices[l] = reverseIndexMap.get(i);
						values[l] = value;
					}
					theta.add(new IndexedSparseVector(indices, values));
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("SVD failed.");
			}
		}
		System.out.println("Finish SVD. Theta size: " + theta.size());
	}

	public void train(InstanceList instances) {
		this.instances = instances;
		SparseVector[] ws;
		if (getThreads() > 1) {
			ws = trainByMultiThread(getThreads());
		} else {
			ws = trainBySingleThread();
		}

		int dim = instances.getDataAlphabet().size();

		for (int i = 0; i < ws.length; i++) {
			SparseVector w = ws[i];
			int count = 0;
			for (int l = 0; l < w.numLocations(); l++) {
				double val = w.valueAtLocation(l);
				if (val < 0) {
					w.setValueAtLocation(l, 0);
					count++;
				}
			}
			logger.info("Turn " + count + " negative values to 0 for w_" + i);
		}

		// svd the weight matrix [w_1 w_2 ... w_m]

		// for each group SVD
		// each group has a map from its name to its indices[]
		// after SVD, read values[] from Ut, and build SparseVector with
		// indices[]
		try {
			SVDLIBC svd = new SVDLIBC();
			File matF = svd.newFile(svd.input);
			// write the matrix
			// it's not necessary
			// but it saves time for trying different h
			List<SparseVector> mat = Arrays.asList(ws);
			svd.writeSparse(matF, mat, dim, ws.length);
			// do the SVD
			svd(mat);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double[] transform(FeatureVector v) {
		double[] values = new double[theta.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = theta.get(i).dotProduct(v);
		}
		return values;
	}

	public int getNumInstances() {
		return instances.size();
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}
}
