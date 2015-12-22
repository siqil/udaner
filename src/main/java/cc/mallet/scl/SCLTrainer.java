package cc.mallet.scl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.IndexedSparseVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import cc.mallet.types.SparseVector;

public class SCLTrainer {
	// size of Theta
	private int h = 25;
	private int threads = 1;
	// min num of occurrences for pivot features
	private int minOccurPivot = 50;
	// max num of pivot features
	private int maxNumPivots = 1000;
	private boolean useMI;

	public SCL train(File w, int h, Alphabet features) {
		return new SCL(w, buildGroups(features), h);
	}

	protected Map<String, Set<Integer>> buildGroups(Alphabet features) {
		Pattern pattern = Pattern.compile("([^=]+)=.+@(-?\\d+)");
		Map<String, Set<Integer>> groups = new HashMap<String, Set<Integer>>();
		for (int i = 0; i < features.size(); i++) {
			String key = (String) features.lookupObject(i);
			Matcher m = pattern.matcher(key);
			if (m.matches()) {
				String name = m.group(1);
				String pos = m.group(2);
				key = name + "@" + pos;
				if (!groups.containsKey(key)) {
					groups.put(key, new HashSet<Integer>());
				}
				groups.get(key).add(i);
			} else {
				throw new IllegalArgumentException("Illegal feature: " + key);
			}
		}
		return groups;
	}

	public SCL train(InstanceList sourceTrainInstances,
			InstanceList targetTrainInstances) {
		System.err.println("Pivot threshold: " + getMinOccurPivot());
		System.err.println("Pivot number: " + getMaxNumPivots());
		System.err.println("h: " + getH());
		// select frequent features as pivot candidates
		if (!sourceTrainInstances.getDataAlphabet().equals(
				targetTrainInstances.getDataAlphabet()))
			throw new IllegalArgumentException(
					"Instances from two domains should have the same data alphabet");
		Alphabet features = sourceTrainInstances.getDataAlphabet();
		Alphabet labels = sourceTrainInstances.getTargetAlphabet();
		InstanceList[] ilists = new InstanceList[2];
		ilists[0] = sourceTrainInstances;
		ilists[1] = targetTrainInstances;
		int nFeatures = features.size();
		int nLabels = labels.size();
		int[][] count = new int[ilists.length][nFeatures];
		for (int i = 0; i < ilists.length; i++) {
			for (Instance inst : ilists[i]) {
				FeatureVectorSequence data = (FeatureVectorSequence) inst
						.getData();
				for (int j = 0; j < data.size(); j++) {
					FeatureVector v = data.get(j);
					if (!features.equals(v.getAlphabet())) {
						throw new IllegalArgumentException(
								"Instances from two domains should have the same data alphabet");
					}
					for (int l = 0; l < v.numLocations(); l++) {
						int index = v.indexAtLocation(l);
						double value = v.valueAtLocation(l);
						if (value != 1.0 && value != 0.0) {
							throw new IllegalArgumentException(
									"Only binary features are supported");
						}
						if (value == 1.0) {
							count[i][index] += 1;
						}
					}
				}
			}
		}

		// calculate mutual information for each feature
		int[][][] joint = new int[nFeatures][nLabels][2];
		int[][] marginalX = new int[nFeatures][2];
		int[] marginalY = new int[nLabels];
		int N = 0;
		System.err.println("Counting feature and label occurences.");
		for (Instance inst : sourceTrainInstances) {
			FeatureVectorSequence data = (FeatureVectorSequence) inst.getData();
			Sequence<?> target = (Sequence<?>) inst.getTarget();
			for (int j = 0; j < data.size(); j++) {
				FeatureVector v = data.get(j);
				Object o = target.get(j);
				int label = labels.lookupIndex(o, false);
				for (int l = 0; l < v.numLocations(); l++) {
					int index = v.indexAtLocation(l);
					double value = v.valueAtLocation(l);
					if (value == 1.0) {
						joint[index][label][1]++;
						marginalX[index][1]++;
					}
				}
				marginalY[label]++;
				N++;
			}
		}

		final double[] mi = new double[nFeatures]; // mutual information
		if (useMI) {
			System.err.println("Calculating mutual information.");
			for (int index = 0; index < nFeatures; index++) {
				double info = 0;
				for (int label = 0; label < labels.size(); label++) {
					joint[index][label][0] = marginalY[label]
							- joint[index][label][1];
					marginalX[index][0] = N - marginalX[index][1];
					for (int value = 0; value < 2; value++) {
						info += joint[index][label][value]
								* (Math.log(N)
										+ Math.log(joint[index][label][value]) - Math
											.log(marginalX[index][value]
													- Math.log(marginalY[label])));
					}
				}
				// we don't need divide info by N because it doesn't matter
				mi[index] = info;
			}
		}

		ArrayList<Integer> fs = new ArrayList<Integer>();
		for (int i = 0; i < features.size(); i++) {
			boolean keep = true;
			for (int j = 0; j < count.length; j++) {
				// we may get different set of pivots for the same pair of
				// training sets because the mutual information of a feature may
				// be a NaN in one set but a valid double in another
				if (count[j][i] < getMinOccurPivot()
						|| (useMI && Double.isNaN(mi[i]))) {
					keep = false;
					break;
				}
			}
			if (keep)
				fs.add(i);
		}

		if (useMI) {
			Collections.sort(fs, new Comparator<Integer>() {
				public int compare(Integer o1, Integer o2) {
					return (int) Math.signum(mi[o2] - mi[o1]);
				}
			});
		}

		int nPivots = Math.min(fs.size(), getMaxNumPivots());
		int[] pivots = new int[nPivots];
		for (int i = 0; i < nPivots; i++) {
			int index = fs.get(i);
			pivots[i] = index;
			System.err.println("Pivot " + i + " "
					+ features.lookupObject(pivots[i]) + " "
					+ (useMI ? mi[pivots[i]] : ""));
			// System.err.println("marginalX: " + marginalX[index][0] + " "
			// + marginalX[index][1]);
			// for (int label = 0; label < labels.size(); label++) {
			// System.err.println("marginalY: " + marginalY[label]);
			// for (int value = 0; value < 2; value++) {
			// System.err.println("label=" + label + " value=" + value
			// + " count=" + joint[index][label][value]);
			// }
			// }
		}

		Pattern pattern = Pattern.compile(".+@(-?\\d+)");
		Map<Integer, SparseVector> maskMap = new HashMap<Integer, SparseVector>();
		SparseVector[] masks = new SparseVector[pivots.length];
		int[] featurePos = new int[nFeatures];
		Map<Integer, Integer> featurePosCount = new HashMap<Integer, Integer>();
		for (int i = 0; i < nFeatures; i++) {
			String key = (String) features.lookupObject(i);
			Matcher m = pattern.matcher(key);
			if (m.matches()) {
				int pos = Integer.parseInt(m.group(1));
				featurePos[i] = pos;
				if (featurePosCount.containsKey(pos)) {
					featurePosCount.put(pos, featurePosCount.get(pos) + 1);
				} else {
					featurePosCount.put(pos, 1);
				}
			}
		}
		for (int p = 0; p < pivots.length; p++) {
			String key = (String) features.lookupObject(pivots[p]);
			Matcher m = pattern.matcher(key);
			if (m.matches()) {
				int pos = Integer.parseInt(m.group(1));
				if (!maskMap.containsKey(pos)) {
					int n = nFeatures - featurePosCount.get(pos);
					int[] indices = new int[n];
					double[] values = new double[n];
					int k = 0;
					for (int i = 0; i < nFeatures; i++) {
						if (featurePos[i] != pos) {
							indices[k++] = i;
						}
					}
					if (k != n) {
						throw new IllegalArgumentException(
								"Something is wrong.");
					}
					// System.err.println("Pos: " + pos);
					// System.err.println("Masks: "
					// + Arrays.toString(features.lookupObjects(indices)));
					maskMap.put(pos, new IndexedSparseVector(indices, values));
				}
				masks[p] = maskMap.get(pos);
			} else {
				throw new IllegalArgumentException("Not a legal feature: "
						+ key);
			}
		}

		InstanceList instances = new InstanceList(features, null);
		for (InstanceList ilist : ilists) {
			for (Instance inst : ilist) {
				FeatureVectorSequence data = (FeatureVectorSequence) inst
						.getData();
				for (int i = 0; i < data.size(); i++) {
					instances.add(new Instance(data.get(i), null, null, null));
				}
			}
		}
		System.err.println("SCL training instances size: " + instances.size());

		SCL scl = new SCL(pivots, masks, buildGroups(features), getH());
		scl.setThreads(getThreads());
		scl.train(instances);
		return scl;
	}

	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public int getMaxNumPivots() {
		return maxNumPivots;
	}

	public void setMaxNumPivots(int k) {
		this.maxNumPivots = k;
	}

	public int getMinOccurPivot() {
		return minOccurPivot;
	}

	public void setMinOccurPivot(int threshold) {
		this.minOccurPivot = threshold;
	}

	public boolean isUseMI() {
		return useMI;
	}

	public void setUseMI(boolean useMI) {
		this.useMI = useMI;
	}
}
