package org.mixare;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

/**
 * This class is for debugging purpose, it initializes ACRA (Application Crash
 * Report for Android) which sends the stack trace to this google Form:
 *  {@link https://docs.google.com/spreadsheet/ccc?key=0AkBr1EPcS_mfdFdFNXpjUExsNi1rRTJNc095LTh6RGc#gid=0}
 * 
 * @author KlemensE
 */
@ReportsCrashes(formKey = "dHVhRHlFU3JxbHhjUG80RTNNS19KVnc6MQ")
public class Mixare extends Application {
	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
	}
}
