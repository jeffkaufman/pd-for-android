/**
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 * 
 */

package org.puredata.android.melodybox;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.Toast;


public class MelodyBox extends Activity implements OnClickListener {

	private static final String TAG = "Pd Melody Box";
	private static final String TOP = "top";
	private static final int SAMPLE_RATE = 44100;

	private Toast toast = null;
	
	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
				}
				toast.setText(TAG + ": " + msg);
				toast.show();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initGui();
		try {
			initPd();
		} catch (IOException e) {
			toast(e.toString());
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		PdAudio.startAudio(this);
	}
	
	@Override
	protected void onStop() {
		PdAudio.stopAudio();
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		cleanup();
		super.onDestroy();
	}
	
	private void initGui() {
		setContentView(R.layout.main);
		MelodyView melody = (MelodyView) findViewById(R.id.melodyview);
		melody.setOwner(this);
		int top = getPreferences(MODE_PRIVATE).getInt(TOP, 0);
		melody.setTopSegment(top);
	}

	private void initPd() throws IOException {
		if (AudioParameters.suggestSampleRate() < SAMPLE_RATE) {
			throw new IOException("required sample rate not available");
		}
		int nOut = Math.min(AudioParameters.suggestOutputChannels(), 2);
		if (nOut == 0) {
			throw new IOException("audio output not available");
		}
		PdAudio.initAudio(SAMPLE_RATE, 0, nOut, 1, true);
		
		File dir = getFilesDir();
		File patchFile = new File(dir, "test1.pd");
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.patch), dir, true);
		PdBase.openPatch(patchFile.getAbsolutePath());
	}

	private void cleanup() {
		// make sure to release all resources
		PdAudio.release();
		PdBase.release();
	}

	public void playNote(int n) {
		float f = (float)n / (float)24.0;
		f = (float)0.5 + f/2;
		
		PdBase.sendFloat("pitch", f);
		PdBase.sendFloat("volume", (float)1);
	}
	
	public void endNote() {
		PdBase.sendFloat("volume", (float)0);
	}

	public void setTop(int top) {
		PdBase.sendFloat("shift", top);
		getPreferences(MODE_PRIVATE).edit().putInt(TOP, top).commit();
	}

	@Override
	public void onClick(View v) {
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.circle_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		switch (item.getItemId()) {
		case R.id.about_item:
			ad.setTitle(R.string.about_title);
			ad.setMessage(R.string.about_msg);
			break;
		case R.id.help_item:
			ad.setTitle(R.string.help_title);
			ad.setMessage(R.string.help_msg);
			break;
		default:
			break;
		}
		ad.setNeutralButton(android.R.string.ok, null);
		ad.setCancelable(true);
		ad.show();
		return true;
	}
}
