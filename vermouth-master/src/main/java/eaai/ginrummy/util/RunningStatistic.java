package eaai.ginrummy.util;

/**
 *
 **/
public class RunningStatistic {

	/* */
	private int n;

	/* */
	private double mean;

	/**
	 *
	 **/
	public RunningStatistic(double initial) {
		this.n = 1;
		this.mean = initial;
	}

	/**
	 *
	 **/
	public void add(double value) {
		this.n += 1;
		this.mean = (value + (this.n - 1) * this.mean) / this.n;
	}

	/**
	 *
	 **/
	public double mean() {
		return mean;
	}
}
