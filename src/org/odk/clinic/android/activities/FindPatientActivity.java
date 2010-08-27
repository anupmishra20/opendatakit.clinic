package org.odk.clinic.android.activities;

import java.util.ArrayList;

import org.odk.clinic.android.R;
import org.odk.clinic.android.adapters.PatientAdapter;
import org.odk.clinic.android.database.PatientDbAdapter;
import org.odk.clinic.android.logic.GlobalConstants;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.preferences.ServerPreferences;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

//TODO: live search
//TODO: fix search by patient id
//TODO: download cohort list
//TODO: multiple cohorts
//TODO: error correction if connection fails
//TODO: display ages instead of dates
//TODO: stronger typing of data in db
//TODO: differential and faster download
//TODO: more underlying data
//TODO: logging stuff
//TODO: hitting enter on keyboard should do search

public class FindPatientActivity extends ListActivity {

	private static final String KEY_SEARCH = "search";

	private static final int MENU_DOWNLOAD = Menu.FIRST;
	private static final int MENU_PREFERENCES = MENU_DOWNLOAD + 1;

	private ImageButton mSearchButton;
	private ImageButton mBarcodeButton;
	private EditText mSearchText;

	private InputMethodManager mInputMethodManager;

	private ArrayAdapter<Patient> mPatientAdapter;
	private ArrayList<Patient> mPatients = new ArrayList<Patient>();

	/*
	 * (non-Javadoc)
	 * 
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.find_patient);
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.find_patient));

		// used to hide keyboard after search
		mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		mSearchText = (EditText) findViewById(R.id.search_text);
		mSearchText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				//mPatientAdapter.getFilter().filter(s);
			}
			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}


		});

		mSearchButton = (ImageButton) findViewById(R.id.search_button);
		mSearchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String searchStr = mSearchText.getText().toString();
				if (searchStr.length() > 0) {
					mInputMethodManager.hideSoftInputFromWindow(
							mSearchText.getWindowToken(), 0);
					getFoundPatients(searchStr);
				} else {
					getAllPatients();
				}
			}
		});

		mBarcodeButton = (ImageButton) findViewById(R.id.barcode_button);
		mBarcodeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent("com.google.zxing.client.android.SCAN");
				try {
					startActivityForResult(i, GlobalConstants.BARCODE_CAPTURE);
				} catch (ActivityNotFoundException e) {
					Toast t = Toast.makeText(getApplicationContext(),
							getString(R.string.error, R.string.barcode_error),
							Toast.LENGTH_LONG);
					t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					t.show();
				}
			}
		});
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(KEY_SEARCH)) {
				getFoundPatients(savedInstanceState.getString(KEY_SEARCH));
			}
		} else {
			getAllPatients();
		}
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position,
			long id) {
		// Get selected patient
		Patient p = (Patient) getListAdapter().getItem(position);
		String patientIdStr = p.getPatientId().toString();

		// create intent for return and store path
		Intent i = new Intent();
		i.putExtra("SCAN_RESULT", patientIdStr);
		setResult(RESULT_OK, i);

		Toast t = Toast.makeText(getApplicationContext(), "Selected patient "
				+ patientIdStr, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		t.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_DOWNLOAD, 0, getString(R.string.download_patients))
				.setIcon(R.drawable.ic_menu_invite);
		menu.add(0, MENU_PREFERENCES, 0, getString(R.string.server_preferences))
				.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DOWNLOAD:
			Intent id = new Intent(getApplicationContext(),
					DownloadPatientActivity.class);
			startActivity(id);
			return true;
		case MENU_PREFERENCES:
			Intent ip = new Intent(getApplicationContext(),
					ServerPreferences.class);
			startActivity(ip);

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			return;
		}

		if (requestCode == GlobalConstants.BARCODE_CAPTURE && intent != null) {
			String sb = intent.getStringExtra("SCAN_RESULT");
			if (sb != null && sb.length() > 0) {
				mSearchText.setText(sb);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);

	}

	private void getAllPatients() {
		getFoundPatients(null);
	}

	private void getFoundPatients(String searchStr) {

		PatientDbAdapter pda = new PatientDbAdapter(this);
		pda.open();
		Cursor c = null;
		if (searchStr != null) {
			c = pda.fetchPatients(searchStr, searchStr);
		} else {
			c = pda.fetchAllPatients();
		}

		if (c != null && c.getCount() >= 0) {

			mPatients.clear();

			int patientIdIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_PATIENT_ID);
			int identifierIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_IDENTIFIER);
			int givenNameIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_GIVEN_NAME);
			int familyNameIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_FAMILY_NAME);
			int middleNameIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_MIDDLE_NAME);
			int birthdateIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_BIRTHDATE);
			int genderIndex = c.getColumnIndex(PatientDbAdapter.KEY_GENDER);

			Patient p;
			if (c.getCount() > 0) {
				do {
					p = new Patient();
					p.setPatientId(c.getInt(patientIdIndex));
					p.setIdentifier(c.getString(identifierIndex));
					p.setGivenName(c.getString(givenNameIndex));
					p.setFamilyName(c.getString(familyNameIndex));
					p.setMiddleName(c.getString(middleNameIndex));
					p.setBirthdate(c.getString(birthdateIndex));
					p.setGender(c.getString(genderIndex));
					mPatients.add(p);
				} while (c.moveToNext());
			}
		}

		refreshView();

		c.close();
		pda.close();

	}

	private void refreshView() {

		mPatientAdapter = new PatientAdapter(this, R.layout.patient_list_item,
				mPatients);
		setListAdapter(mPatientAdapter);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		String s = mSearchText.getText().toString();
		if (s != null && s.length() > 0) {
			getFoundPatients(s);
		} else {
			getAllPatients();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SEARCH, mSearchText.getText().toString());
	}

}
