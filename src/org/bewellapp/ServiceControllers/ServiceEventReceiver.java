/**
 * 
 */
package org.bewellapp.ServiceControllers;

import org.bewellapp.ServiceControllers.MessageNofitier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Rui Wang
 * 
 */
public class ServiceEventReceiver extends BroadcastReceiver {

    private MessageNofitier notifier = null;

    public ServiceEventReceiver(MessageNofitier n)
    {
	notifier = n;
    }
    
    @Override
    public void onReceive(Context ctx, Intent intent) {
	if (notifier != null) {
	    notifier.onNotifyNewMessage(intent);
	}
    }

}
