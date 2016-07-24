package edu.dartmouth.cs.funf.probe;

import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;

public interface LocalPhoneNumberInfoKeys extends BaseProbeKeys {
	public static final String 
	LOCAL_PHONENUMBER = "**********",
	LOCAL_PHONENUMBER_WITH1 = "1**********",
	LOCAL_PHONENUMBER_WITH1_BRACKET = "(***) ***-****",
	LOCAL_PHONENUMBER_WITH_2DASH = "***-***-****",
	LOCAL_PHONENUMBER_WITH1_WITH_2DASH = "1***-***-****",
	LOCAL_PHONENUMBER_WITHPLUS1_WITH_2DASH = "+1***-***-****",
	LOCAL_PHONENUMBER_WITHPLUS1_WITH_BLANK_WITH_2DASH = "+1 ***-***-****";
}