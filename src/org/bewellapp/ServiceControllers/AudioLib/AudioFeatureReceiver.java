/**
 * 
 */
package org.bewellapp.ServiceControllers.AudioLib;

import org.bewellapp.ServiceControllers.MessageNofitier;
import org.bewellapp.ServiceControllers.ServiceEventReceiver;

/**
 * @author Rui Wang
 *
 */
public class AudioFeatureReceiver extends ServiceEventReceiver {

    public AudioFeatureReceiver(MessageNofitier n) {
	super(n);
    }
}
