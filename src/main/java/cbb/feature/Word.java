package cbb.feature;

import java.util.Set;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class Word extends Pipe {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6352250697946799272L;
	private Set<String> vocab;

	public Word(Set<String> vocab) {
		this.vocab = vocab;
	}

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence ts = (TokenSequence) carrier.getData();
		for (Token token : ts) {
			String text = token.getText();
			if (vocab == null || vocab.contains(text.toLowerCase())) {
				token.setFeatureValue("W=" + text.toLowerCase(), 1);
			}
		}
		carrier.setData(ts);
		return carrier;
	}
}
