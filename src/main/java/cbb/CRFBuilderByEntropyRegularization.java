package cbb;

import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;

public class CRFBuilderByEntropyRegularization extends CRFBuilder {

	private double lambda;

	public CRFBuilderByEntropyRegularization(Pipe pipe) {
		super(pipe);
	}

	@Override
	protected TransducerTrainer train(InstanceList source, InstanceList target) {
		CRFTrainerByEntropyRegularization trainer = new CRFTrainerByEntropyRegularization(
				crf);
		trainer.setEntropyWeight(getLambda());
		trainer.train(source, target, Integer.MAX_VALUE);
		return trainer;
	}

	public double getLambda() {
		return lambda;
	}

	public CRFBuilderByEntropyRegularization setLambda(double lambda) {
		this.lambda = lambda;
		return this;
	}

}
