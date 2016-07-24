package org.bewellapp.ServiceControllers.AccelerometerLib;

/**
 * This class represents the features of a batch of SensorEvents
 * @author Giuseppe Cardone
 *
 */
public class AccelFeatures {
	
	public static String arrfHeader() {
		StringBuilder sb = new StringBuilder();
		String newLine = System.getProperty("line.separator");
		sb.append("@attribute energyx numeric").append(newLine).append("@attribute energyy numeric").append(newLine).append("@attribute energyz numeric").append(newLine);
		sb.append("@attribute maxx numeric").append(newLine).append("@attribute maxy numeric").append(newLine).append("@attribute maxz numeric").append(newLine);
		sb.append("@attribute meanx numeric").append(newLine).append("@attribute meany numeric").append(newLine).append("@attribute meanz numeric").append(newLine);
		sb.append("@attribute meanxyabsdiff numeric").append(newLine);
		sb.append("@attribute minx numeric").append(newLine).append("@attribute miny numeric").append(newLine).append("@attribute minz numeric").append(newLine);
		sb.append("@attribute stdevx numeric").append(newLine).append("@attribute stdevy numeric").append(newLine).append("@attribute stdevz numeric").append(newLine);
		//sb.append("@attribute numstepsx numeric").append(newLine).append("@attribute numstepsy numeric").append(newLine).append("@attribute numstepsz numeric");
		return sb.toString();
	}
	public double energyx;
	public double energyy;
	public double energyz;
	public double maxx;
	public double maxy;
	public double maxz;
	public double meanx;
	public double meany;
	public double meanz;
	public double meanxyabsdiff;
	public double minx;
	public double miny;
	public double minz;
	public double stdevx;
	public double stdevy;
	public double stdevz;
	public int numstepsx;
	public int numstepsy;
	
	public int numstepsz;
	
	/**
	 * Text representation of the features (suitable for an ARFF file).
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%f, %f, %f", energyx, energyy, energyz));
		sb.append(String.format(", %f, %f, %f", maxx, maxy, maxz));
		sb.append(String.format(", %f, %f, %f", meanx, meany, meanz));
		sb.append(String.format(", %f", meanxyabsdiff));
		sb.append(String.format(", %f, %f, %f", minx, miny, minz));
		sb.append(String.format(", %f, %f, %f", stdevx, stdevy, stdevz));
		//sb.append(String.format(", %d, %d, %d", numstepsx, numstepsy, numstepsz));
		return sb.toString();
	}
	
	
	/**
	 * Represents the features as a Object array (suitable for classification by Weka) 
	 * @return
	 */
	public Object[] toWekaObject() {
		Double[] res = new Double[15];
		res[0] = energyx;
		res[1] = energyy;
		res[2] = energyz;
		res[3] = maxx;
		res[4] = maxy;
		res[5] = maxz;
		res[6] = meanx;
		res[7] = meany;
		res[8] = meanz;
		/*res[9] = meanxyabsdiff;
		res[10] = minx;
		res[11] = miny;
		res[12] = minz;
		res[13] = stdevx;
		res[14] = stdevy;
		res[15] = stdevz;*/
		res[9] = minx;
		res[10] = miny;
		res[11] = minz;
		res[12] = stdevx;
		res[13] = stdevy;
		res[14] = stdevz;
		return res;
	}
}