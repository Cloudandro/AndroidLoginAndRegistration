package info.androidhive.loginandregistration.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;
import info.androidhive.loginandregistration.R;
import info.androidhive.loginandregistration.helper.SQLiteHandler;
import info.androidhive.loginandregistration.helper.SessionManager;

public class MainActivity extends Activity {

	private TextView txtName;
	private TextView txtEmail;
	private TextView txtPassword;
	private Button btnLogout;


	private SQLiteHandler db;
	private SessionManager session;

	//edited Progress Dialog Object
	ProgressDialog prgDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtName = (TextView) findViewById(R.id.name);
		txtEmail = (TextView) findViewById(R.id.email);
		txtPassword = (TextView) findViewById(R.id.password);
		btnLogout = (Button) findViewById(R.id.btnLogout);

		// SqLite database handler
		db = new SQLiteHandler(getApplicationContext());

		// session manager
		session = new SessionManager(getApplicationContext());

		/*// Fetching user details from SQLite
		HashMap<String, String> user = db.getUserDetails();

		String name = user.get("name");
		String email = user.get("email");
		String password = user.get("password");

		// Displaying the user details on the screen
		txtName.setText(name);
		txtEmail.setText(email);
		txtPassword.setText(password);*/

		Toast.makeText(getApplicationContext(), "Please Sync your data online, if not then you are not able to login again, if already sync please ignor this mgsa", Toast.LENGTH_LONG).show();

		// Logout button click eventf
		btnLogout.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				logoutUser();
			}
		});
		//Get User records from SQLite DB
		HashMap<String, String> userList =  db.getUserDetails();
		//
		if(userList.size()!=0){
			//Set the User Array list in ListView
            //ListAdapter adapter = new SimpleAdapter(MainActivity.this, user, R.layout.activity_main, new String[] { "name","email"}, new int[] {R.id.name, R.id.email});
            ListAdapter adapter = new SimpleAdapter(MainActivity.this,
                    (List<? extends Map<String, ?>>) userList,
                    R.layout.view_user_entry,
                    new String[] { "name", "email" },
                    new int[] { R.id.name , R.id.email });  
            ListView myList=(ListView)findViewById(android.R.id.list);
			myList.setAdapter(adapter);
			//Display Sync status of SQLite DB
			Toast.makeText(getApplicationContext(), db.getSyncStatus(), Toast.LENGTH_LONG).show();
		}
		//Initialize Progress Dialog properties
		prgDialog = new ProgressDialog(this);
		prgDialog.setMessage("Synching SQLite Data with Remote MySQL DB. Please wait...");
		prgDialog.setCancelable(false);

		if (!session.isLoggedIn()) {
			logoutUser();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		//When Sync action button is clicked
		if (id == R.id.refresh) {
			//Sync SQLite DB data to remote MySQL DB
			syncSQLiteMySQLDB();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	/*//Add User method getting called on clicking (+) button
	public void addUser(View view) {
		Intent objIntent = new Intent(getApplicationContext(), NewUser.class);
		startActivity(objIntent);
	}*/
	/**
	 * Logging out the user. Will set isLoggedIn flag to false in shared
	 * preferences Clears the user data from sqlite users table
	 * */

	public void syncSQLiteMySQLDB(){
		//Create AsycHttpClient object
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		HashMap<String, String> userList =  db.getUserDetails();
		if(userList.size()!=0){
			if(db.dbSyncCount() != 0){
				prgDialog.show();
				params.put("usersJSON", db.composeJSONfromSQLite());
				client.post("http://android.elementsbpo.com/android_login_api/register.php",params ,new AsyncHttpResponseHandler() {
					@TargetApi(Build.VERSION_CODES.KITKAT)
					@Override
					public void onSuccess(int statusCode, Header[] headers, byte[] response) {
						if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
							getActionBar().setHomeButtonEnabled(false);
						}
						System.out.println(response);
						prgDialog.hide();
						try {
							JSONArray arr = new JSONArray(response);
							System.out.println(arr.length());
							for(int i=0; i<arr.length();i++){
								JSONObject obj = (JSONObject)arr.get(i);
								System.out.println(obj.get("name"));
								System.out.println(obj.get("email"));
								db.updateSyncStatus(obj.get("name").toString(),obj.get("email").toString());
							}
							Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
					}

					@Override
					public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable error) {
						// TODO Auto-generated method stub
						prgDialog.hide();
						if(statusCode == 404){
							Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
						}else if(statusCode == 500){
							Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
						}else{
							Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
						}
					}
				});
			}else{
				Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
			}
		}else{
			Toast.makeText(getApplicationContext(), "No data in SQLite DB, please do enter User name to perform Sync action", Toast.LENGTH_LONG).show();
		}
	}

	private void logoutUser() {
		session.setLogin(false);


		db.deleteUsers();

		// Launching the login activity
		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
}
