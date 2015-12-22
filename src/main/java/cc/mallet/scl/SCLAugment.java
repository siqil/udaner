package cc.mallet.scl;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;

public class SCLAugment extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7950688971273182873L;
	SCL scl;

	public SCLAugment(SCL scl) {
		this.scl = scl;
	}

	@Override
	public Instance pipe(Instance inst) {
		FeatureVectorSequence vs = (FeatureVectorSequence) inst.getData();
		for (int i = 0; i < vs.size(); i++) {
			AugmentableFeatureVector v = (AugmentableFeatureVector) vs.get(i);
			// System.out.println("Before augment: " + v.numLocations());
			double[] values = scl.transform(v);
			for (int j = 0; j < values.length; j++) {
				String key = "SCL_AUG" + j;
				// if (values[j] != 0)
				// v.add("SCL_AUG" + j, values[j]);
				// if (values[j] < -1) {
				// v.add(key + "(<-1)", 1);
				// } else if (values[j] < -0.5) {
				// v.add(key + "(<-0.5)", 1);
				// } else if (values[j] < 0) {
				// v.add(key + "(<0)", 1);
				// } else if (values[j] > 1) {
				// v.add(key + "(>1)", 1);
				// } else if (values[j] > 0.5) {
				// v.add(key + "(>0.5)", 1);
				// } else if (values[j] > 0) {
				// v.add(key + "(>0)", 1);
				// } else {
				// v.add(key + "(=0)", 1);
				// }
				// if (values[j] > 0) {
				// v.add(key + "(>0)", 1);
				// } else if (values[j] < 0) {
				// v.add(key + "(<0)", 1);
				// }
				if (values[j] != 0)
					v.add(key, values[j]);
			}
			// System.out.println(v);
			// System.out.println("After augment: " + v.numLocations());
		}

		return inst;
	}
}
