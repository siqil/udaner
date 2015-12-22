package cbb;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.MaxLatticeDefault;
import cc.mallet.fst.SumLattice;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.fst.Transducer;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.fst.semi_supervised.EntropyLattice;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Sequence;

public class CRFTrainerByBootstrapping extends TransducerTrainer {
	int t; // the current iteration
	InstanceList Ds; // labeled source data & target set
	InstanceList Rt; // remaining unlabeled target data set
	CRFTrainerByThreadedLabelLikelihood trainer;
	CRF crf;
	Map<Instance, Double> entropyS = new HashMap<Instance, Double>();
	Map<Instance, Double> entropyT = new HashMap<Instance, Double>();
	Map<Instance, Double> probS = new HashMap<Instance, Double>();
	Map<Instance, Double> probT = new HashMap<Instance, Double>();
	Map<Instance, Double> score = new HashMap<Instance, Double>();
	private int k = 10;
	boolean stop = false;
	private int threads = 1;

	public CRFTrainerByBootstrapping(CRF crf, InstanceList Ds, InstanceList Dt) {
		this.crf = crf;
		this.Ds = Ds;
		this.Rt = Dt;
	}

	public CRF train() {
		t = 0;
		do {
			crf = new CRF(crf.getInputPipe(), crf.getOutputPipe());
			crf.addStatesForBiLabelsConnectedAsIn(Ds);
			trainer = new CRFTrainerByThreadedLabelLikelihood(crf, getThreads());
			trainer.train(Ds);
			trainer.shutdown();
			runEvaluators();
			t++;
			System.err.println("Finished iteration " + t);
			if (Rt.isEmpty())
				break;
			label();
			select();
		} while (!stop);
		return crf;
	}

	private void score() {
		score.clear();
		for (Instance inst : Rt) {
			double s = probS.get(inst)
					* (entropyS.get(inst) - entropyT.get(inst));
			score.put(inst, s);
		}
	}

	private void select() {
		score();
		Collections.sort(Rt, new Comparator<Instance>() {
			public int compare(Instance o1, Instance o2) {
				double diff = score.get(o2) - score.get(o1);
				if (diff < 0) {
					return -1;
				} else if (diff > 0) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		System.err.println("Adding instances: ");
		int i = 0;
		Iterator<Instance> it = Rt.iterator();
		while (it.hasNext()) {
			Instance inst = it.next();
			Ds.add(inst);
			it.remove();
			System.err.println("ProbS: " + probS.get(inst));
			System.err.println("ProbT: " + probT.get(inst));
			System.err.println("Score: " + score.get(inst));
			if (entropyS.get(inst) < entropyT.get(inst)
					|| probS.get(inst) > probT.get(inst)) {
				stop = true;
			}
			if (++i >= getK())
				break;
		}
	}

	private void label() {
		entropyS.clear();
		entropyT.clear();
		probS.clear();
		probT.clear();
		for (Instance inst : Rt) {
			getStatistics(crf, inst, entropyS, probS, true);
		}
		Collections.shuffle(Rt);
		int n = Rt.size();
		InstanceList[] Rts = new InstanceList[2];
		Rts[0] = Rt.cloneEmpty();
		for (int i = 0; i < n / 2; i++) {
			try {
				Rts[0].add(Rt.get(i));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(i);
				System.err.println(Rts[0].getDataAlphabet() == (Rt.get(i)
						.getDataAlphabet()));
				System.err.println(Rts[0].getTargetAlphabet() == (Rt.get(i)
						.getTargetAlphabet()));
				System.exit(1);
			}
		}
		Rts[1] = Rt.cloneEmpty();
		for (int i = n / 2; i < n; i++) {
			Rts[1].add(Rt.get(i));
		}
		CRF[] crfs = new CRF[2];
		CRFTrainerByThreadedLabelLikelihood[] trainers = new CRFTrainerByThreadedLabelLikelihood[2];
		for (int i = 0; i < 2; i++) {
			// crfs[i] = new CRF(this.crf);
			crfs[i] = new CRF(this.crf.getInputPipe(), this.crf.getOutputPipe());
			crfs[i].addStatesForBiLabelsConnectedAsIn(Rts[i]);
			trainers[i] = new CRFTrainerByThreadedLabelLikelihood(crfs[i],
					getThreads());
			trainers[i].train(Rts[i]);
			trainers[i].shutdown();
		}
		for (int i = 0; i < 2; i++) {
			CRF crf = crfs[i];
			for (Instance inst : Rts[1 - i]) {
				getStatistics(crf, inst, entropyT, probT, false);
			}
		}

	}

	private void getStatistics(CRF crf, Instance inst,
			Map<Instance, Double> entropyMap, Map<Instance, Double> probMap,
			boolean setTarget) {
		FeatureVectorSequence input = (FeatureVectorSequence) inst.getData();
		MaxLatticeDefault maxLattice = new MaxLatticeDefault(crf, input);
		Sequence output = maxLattice.bestOutputSequence();
		double labeled = new SumLatticeDefault(crf, input, output)
				.getTotalWeight();
		SumLattice lattice = new SumLatticeDefault(crf, input, true);
		double unlabeled = lattice.getTotalWeight();
		EntropyLattice entropyLattice = new EntropyLattice(input,
				lattice.getGammas(), lattice.getXis(), crf, null, 1);
		double entropy = -entropyLattice.getEntropy();
		double prob = Math.exp(labeled - unlabeled);
		entropyMap.put(inst, entropy);
		probMap.put(inst, prob);
		if (setTarget) {
			inst.unLock();
			int n = output.size();
			LabelSequence seq = new LabelSequence(
					(LabelAlphabet) crf.getOutputAlphabet(), n);
			for (int i = 0; i < output.size(); i++) {
				seq.add(output.get(i));
			}
			inst.setTarget(seq);
			inst.lock();
		}
	}

	private InstanceList compose(InstanceList ds, InstanceList tt) {
		InstanceList ilist = ds.shallowClone();
		for (Instance i : tt) {
			ilist.add(i);
		}
		return ilist;
	}

	@Override
	public Transducer getTransducer() {
		return crf;
	}

	@Override
	public int getIteration() {
		return t;
	}

	@Override
	public boolean isFinishedTraining() {
		return stop;
	}

	@Override
	public boolean train(InstanceList trainingSet, int numIterations) {
		return false;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}
}
