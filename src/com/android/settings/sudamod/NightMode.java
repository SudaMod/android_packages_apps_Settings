/*
 * Copyright (C) 2015 The SudaMod project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.sudamod;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class NightMode extends Service {

	public static boolean beStart;
	private static final LayoutParams mParams = new WindowManager.LayoutParams();
	private View view;	
	
	int bri = 0;
    int red = 0;
	int green = 0;
	int yellow = 0 ;
	
	@SuppressWarnings("deprecation")
	public void onStart(Intent paramIntent, int paramInt)
	  {
		
	    bri = getApplicationContext().getSharedPreferences("eye", 0).getInt("bri", 0);
	    red = getApplicationContext().getSharedPreferences("eye", 0).getInt("red", 0);
		green = getApplicationContext().getSharedPreferences("eye", 0).getInt("green", 0);
		yellow = getApplicationContext().getSharedPreferences("eye", 0).getInt("yellow", 00);
	    super.onStart(paramIntent, paramInt);
	    view.setBackgroundColor(Color.argb(bri ,red,green, yellow));
	
	  }
	
	  public void onDestroy()
	  {
	    super.onDestroy();
	    beStart = false;
	    ((WindowManager)getApplication().getSystemService("window")).removeView(view);
	  }
	   

	  public void onCreate()
	  {
	    super.onCreate();
	    WindowManager localWindowManager = (WindowManager)getApplication().getSystemService("window");
	    view = new View(getApplicationContext());
	    view.setFocusable(false);
	    view.setFocusableInTouchMode(false);
	    mParams.type = 2006;
	    mParams.flags = 280;
	    mParams.format = 1;
	    mParams.gravity = 51;
	    mParams.x = 0;
	    mParams.y = 0;
	    mParams.width = -1;
	    mParams.height = -1;
	    localWindowManager.addView(view, mParams);
	    beStart = true;
	  }

	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
