/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.dartmouth.cs.funf.probe;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import edu.mit.media.funf.HashUtil;
import edu.mit.media.funf.probe.SynchronousProbe;

public class LocalPhoneNumberProbe extends SynchronousProbe implements LocalPhoneNumberInfoKeys {
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.ACCESS_WIFI_STATE,
				android.Manifest.permission.BLUETOOTH
		};
	}
	
	@Override
	protected long getDefaultPeriod() {
		return 604800L;
	}


	protected Bundle getData() {
		Bundle data = new Bundle();
		String local_phonenumber = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
		String local_phonenumber_with1;
		String local_phonenumber_with_bracket;
		String local_phonenumber_with_2dash;
		String local_phonenumber_with1_with_2dash;
		String local_phonenumber_withplus1_with_2dash;
		String local_phonenumber_withplus1_with_blank_with_2dash;
		
		if (!local_phonenumber.equals("")){
			String country_idex = local_phonenumber.substring(0, 1);
			if (country_idex.equals("1")){
				local_phonenumber_with1 = local_phonenumber;
				local_phonenumber = local_phonenumber.substring(1);
				local_phonenumber_with_bracket = "(" + local_phonenumber.substring(0, 3) + ") " + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_with_2dash = local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_with1_with_2dash = "1" + local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_withplus1_with_2dash = "+1" + local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_withplus1_with_blank_with_2dash = "+1 " + local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
			}
			else{
				local_phonenumber_with1 = "1" + local_phonenumber;
				local_phonenumber_with_bracket = "(" + local_phonenumber.substring(0, 3) + ") " + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_with_2dash = local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_with1_with_2dash = "1" + local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_withplus1_with_2dash = "+1" + local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
				local_phonenumber_withplus1_with_blank_with_2dash = "+1 " + local_phonenumber.substring(0, 3) + "-" + local_phonenumber.substring(3,6) + "-" + local_phonenumber.substring(6);
			}
			String hash_localphonenumber = HashUtil.hashString(this, local_phonenumber);
			String hash_localphonenumber_with1 = HashUtil.hashString(this, local_phonenumber_with1);
			String hash_local_phonenumber_with_bracket = HashUtil.hashString(this, local_phonenumber_with_bracket);
			String hash_local_phonenumber_with_2dash = HashUtil.hashString(this, local_phonenumber_with_2dash);
			String hash_local_phonenumber_with1_with_2dash = HashUtil.hashString(this, local_phonenumber_with1_with_2dash);
			String hash_local_phonenumber_withplus1_with_2dash = HashUtil.hashString(this, local_phonenumber_withplus1_with_2dash);
			String hash_local_phonenumber_withplus1_with_blank_with_2dash = HashUtil.hashString(this, local_phonenumber_withplus1_with_blank_with_2dash);
			data.putString(LOCAL_PHONENUMBER, hash_localphonenumber);
			data.putString(LOCAL_PHONENUMBER_WITH1, hash_localphonenumber_with1);
			data.putString(LOCAL_PHONENUMBER_WITH1_BRACKET, hash_local_phonenumber_with_bracket);
			data.putString(LOCAL_PHONENUMBER_WITH_2DASH, hash_local_phonenumber_with_2dash);
			data.putString(LOCAL_PHONENUMBER_WITH1_WITH_2DASH, hash_local_phonenumber_with1_with_2dash);
			data.putString(LOCAL_PHONENUMBER_WITHPLUS1_WITH_2DASH, hash_local_phonenumber_withplus1_with_2dash);
			data.putString(LOCAL_PHONENUMBER_WITHPLUS1_WITH_BLANK_WITH_2DASH, hash_local_phonenumber_withplus1_with_blank_with_2dash);
		}

		return data;
	}
}
