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

import android.os.Bundle;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.suda.location.PhoneLocation;
import com.android.settings.R;

public class LocationLookup extends Activity {

	private Button btlookup;
	private EditText etphone;
	private TextView tvlocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.phonelocation);
		
		btlookup = (Button) findViewById(R.id.lookup);
        etphone  = (EditText) findViewById( R.id.phone);
        tvlocation = (TextView) findViewById(R.id.location);
		
        btlookup.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String pl = PhoneLocation.getCityFromPhone(etphone.getText().toString());
				String phonelocation_tip = (String) LocationLookup.this.getResources().getText(R.string.phonelocation_tip);
				String location_unknow = (String) LocationLookup.this.getResources().getText(R.string.location_unknow);
								
				if(pl.equals("")){
					tvlocation.setText(phonelocation_tip + location_unknow);		
				} else{
					tvlocation.setText(phonelocation_tip + pl );					
				}	
				
			}
		});		

	}

}
