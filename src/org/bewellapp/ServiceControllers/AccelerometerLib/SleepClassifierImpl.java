package org.bewellapp.ServiceControllers.AccelerometerLib;

public class SleepClassifierImpl implements SleepClassifier {

	static class WekaClassifier {

		public static double classify(Object[] i) throws Exception {

			double p = Double.NaN;
			p = WekaClassifier.N118f87f50(i);
			return p;
		}

		static double N118f87f50(Object[] i) {
			double p = Double.NaN;
			if (i[5] == null) {
				p = 1;
			} else if (((Double) i[5]).doubleValue() <= -8.349273) {
				p = WekaClassifier.N6626eed41(i);
			} else if (((Double) i[5]).doubleValue() > -8.349273) {
				p = 0;
			}
			return p;
		}

		static double N6626eed41(Object[] i) {
			double p = Double.NaN;
			if (i[1] == null) {
				p = 0;
			} else if (((Double) i[1]).doubleValue() <= 109.503699) {
				p = WekaClassifier.N609d4b12(i);
			} else if (((Double) i[1]).doubleValue() > 109.503699) {
				p = WekaClassifier.N6f6ab38f3(i);
			}
			return p;
		}

		static double N609d4b12(Object[] i) {
			double p = Double.NaN;
			if (i[2] == null) {
				p = 0;
			} else if (((Double) i[2]).doubleValue() <= 544970.638384) {
				p = 0;
			} else if (((Double) i[2]).doubleValue() > 544970.638384) {
				p = 1;
			}
			return p;
		}

		static double N6f6ab38f3(Object[] i) {
			double p = Double.NaN;
			if (i[6] == null) {
				p = 0;
			} else if (((Double) i[6]).doubleValue() <= -1.247151) {
				p = 0;
			} else if (((Double) i[6]).doubleValue() > -1.247151) {
				p = WekaClassifier.N4632aa6e4(i);
			}
			return p;
		}

		static double N4632aa6e4(Object[] i) {
			double p = Double.NaN;
			if (i[14] == null) {
				p = 1;
			} else if (((Double) i[14]).doubleValue() <= 0.063349) {
				p = WekaClassifier.N28533fc35(i);
			} else if (((Double) i[14]).doubleValue() > 0.063349) {
				p = WekaClassifier.N55211a4b7(i);
			}
			return p;
		}

		static double N28533fc35(Object[] i) {
			double p = Double.NaN;
			if (i[10] == null) {
				p = 1;
			} else if (((Double) i[10]).doubleValue() <= -0.38137) {
				p = 1;
			} else if (((Double) i[10]).doubleValue() > -0.38137) {
				p = WekaClassifier.N717f2fe36(i);
			}
			return p;
		}

		static double N717f2fe36(Object[] i) {
			double p = Double.NaN;
			if (i[8] == null) {
				p = 1;
			} else if (((Double) i[8]).doubleValue() <= -10.123766) {
				p = 1;
			} else if (((Double) i[8]).doubleValue() > -10.123766) {
				p = 0;
			}
			return p;
		}

		static double N55211a4b7(Object[] i) {
			double p = Double.NaN;
			if (i[2] == null) {
				p = 0;
			} else if (((Double) i[2]).doubleValue() <= 539347.69676) {
				p = WekaClassifier.N70455d968(i);
			} else if (((Double) i[2]).doubleValue() > 539347.69676) {
				p = 1;
			}
			return p;
		}

		static double N70455d968(Object[] i) {
			double p = Double.NaN;
			if (i[2] == null) {
				p = 1;
			} else if (((Double) i[2]).doubleValue() <= 466485.018169) {
				p = 1;
			} else if (((Double) i[2]).doubleValue() > 466485.018169) {
				p = 0;
			}
			return p;
		}
	}

	@Override
	public SleepState classify(final AccelFeatures f) {
		Double res;
		try {
			res = WekaClassifier.classify(f.toWekaObject());
		} catch (Exception e) {
			return SleepState.UNKNOWN;
		}
		if (res == 1.0) {
			return SleepState.SLEEPING;
		}
		return SleepState.AWAKE;
	}
}
