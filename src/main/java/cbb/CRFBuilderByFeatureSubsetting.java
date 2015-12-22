package cbb;

import cc.mallet.fst.CRFTrainerByFeatureSubsetting;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;

public class CRFBuilderByFeatureSubsetting extends CRFBuilder {

	private int gamma;
	private double lambda;

	public CRFBuilderByFeatureSubsetting(Pipe pipe) {
		super(pipe);
	}

	@Override
	protected TransducerTrainer train(InstanceList source, InstanceList target) {
		CRFTrainerByFeatureSubsetting trainer = new CRFTrainerByFeatureSubsetting(
				crf);
		trainer.setNorm(gamma);
		trainer.setRegWeight(lambda);
		trainer.train(source, target, Integer.MAX_VALUE);
		return trainer;
	}

	public CRFBuilderByFeatureSubsetting setGamma(int norm) {
		this.gamma = norm;
		return this;
	}

	public CRFBuilderByFeatureSubsetting setLambda(double lambda) {
		this.lambda = lambda;
		return this;
	}

}
