package cbb;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

public class CRFLabeler {
	private CRF crf;

	public CRFLabeler(CRF crf) {
		this.crf = crf;
	}

	public Instance predict(Instance tokenSequence) {
		return crf.transduce(tokenSequence);
	}

	public String[] predict(String[][] tokens) {
		Instance instance = new Instance(tokens, null, null, null);
		instance = predict(instance);
		Sequence<?> labelseq = ((Sequence<?>) instance.getData());
		String[] labels = new String[labelseq.size()];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = (String) labelseq.get(i);
		}
		return labels;
	}
	
}
