package cbb;

import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;

public class CRFBuilderByBootstrapping extends CRFBuilder {

	private int k;

	public CRFBuilderByBootstrapping(Pipe pipe) {
		super(pipe);
	}

	@Override
	protected TransducerTrainer train(InstanceList source, InstanceList target) {
		CRFTrainerByBootstrapping trainer = new CRFTrainerByBootstrapping(crf, source, target);
		trainer.setThreads(threads);
		trainer.setK(getK());
		crf = trainer.train();
		return trainer;
	}

	public int getK() {
		return k;
	}

	public CRFBuilderByBootstrapping setK(int k) {
		this.k = k;
		return this;
	}

}
