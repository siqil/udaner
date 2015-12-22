package cbb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFIm;
import cc.mallet.fst.CRFTrainerByOverlapFeatures;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.NoopTransducerTrainer;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class CRFBuilder {

	protected Pipe pipe;
	protected CRF crf;
	protected TransducerTrainer trainer;
	protected int order = 2;
	protected int threads = 1;
	private boolean overlap;

	public CRFBuilder(Pipe pipe) {
		this.pipe = pipe;
	}

	public CRFBuilder byFile(File model) throws IOException,
			ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(model));
		crf = (CRF) in.readObject();
		in.close();
		setTrainer(new NoopTransducerTrainer(crf));
		pipe = crf.getInputPipe();
		return this;
	}

	private InstanceList instanceList(Iterator<Instance> it) {
		if (it == null)
			return null;
		InstanceList ilist = new InstanceList(pipe);
		ilist.addThruPipe(it);
		return ilist;
	}

	public CRFBuilder byInstanceIterator(Iterator<Instance> sourceIt,
			Iterator<Instance> targetIt) {
		return byInstanceList(instanceList(sourceIt), instanceList(targetIt));
	}

	public CRFBuilder byInstanceList(InstanceList source, InstanceList target) {
		crf = new CRFIm(pipe, null);
		if (order == 1) {
			crf.addStatesForLabelsConnectedAsIn(source);
		} else if (order == 2) {
			crf.addStatesForBiLabelsConnectedAsIn(source);
		}
		trainer = train(source, target);
		return this;
	}

	protected TransducerTrainer train(InstanceList source, InstanceList target) {
		if (overlap) {
			CRFTrainerByOverlapFeatures trainer = new CRFTrainerByOverlapFeatures(
					crf);
			trainer.train(source, target, Integer.MAX_VALUE);
			return trainer;
		} else {
			CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(
					crf, threads);
			trainer.train(source);
			trainer.shutdown();
			return trainer;
		}
	};

	public CRF build() {
		if (crf == null) {
			throw new RuntimeException(
					"Should call either byFile or byInstanceList before build.");
		}
		return crf;
	}

	public TransducerTrainer getTrainer() {
		return trainer;
	}

	public void setTrainer(TransducerTrainer trainer) {
		this.trainer = trainer;
	}

	public int getThreads() {
		return threads;
	}

	public CRFBuilder setThreads(int threads) {
		this.threads = threads;
		return this;
	}

	public CRFBuilder setOverlap(boolean overlap) {
		this.overlap = overlap;
		return this;
	}

	public CRFBuilder setOrder(int order) {
		this.order = order;
		return this;
	}

}
