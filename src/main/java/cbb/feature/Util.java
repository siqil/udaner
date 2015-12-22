package cbb.feature;

public class Util {

	/**
	 * Returns <tt>Math.log(Math.exp(a) + Math.exp(b))</tt>.
	 * <p>
	 * <tt>a, b</tt> represent weights.
	 */
	public static double sumLogProb(double a, double b) {
		if (a == Double.NEGATIVE_INFINITY) {
			if (b == Double.NEGATIVE_INFINITY)
				return Double.NEGATIVE_INFINITY;
			return b;
		} else if (b == Double.NEGATIVE_INFINITY)
			return a;
		else if (a > b)
			return a + Math.log(1 + Math.exp(b - a));
		else
			return b + Math.log(1 + Math.exp(a - b));
	}

}
