/**
 * 
 */
package edu.dartmouth.cs.funf.probe;

import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;

/**
 * @author Rui Wang
 * 
 */
public interface WiFiProbeKeys extends BaseProbeKeys {
    public static final String BSSID = "BSSID", SSID = "SSID",
            CAPABILITY = "CAPABILITY", FREQUENCY = "FREQUENCY",
            LEVEL = "LEVEL";
}
