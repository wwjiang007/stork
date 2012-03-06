package org.tigase.mobile.filetransfer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class IncomingFileActivity extends Activity {

	private static final String TAG = "IncomingFileActivity";

	private JID account;
	private String filename;
	private Long filesize;
	private String id;
	private String mimetype;
	private JID sender;
	private String senderName;
	private String sid;
	private String store = "Download";
	
	private void accept() {
		Log.v(TAG, "incoming file accepted");
		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account.getBareJid());
		final JID sender = this.sender;
		final String id = this.id;
		final String sid = this.sid;
		final String filename = this.filename;
		final String mimetype = this.mimetype;
		final File destination = getPathToSave(this.filename);
		final long filesize = this.filesize;
		new Thread() {
			@Override
			public void run() {
				FileTransferModule ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
				try {
					RosterItem ri = jaxmpp.getRoster().get(sender.getBareJid());
					final FileTransfer ft = new FileTransfer(jaxmpp, sender, ri != null ? ri.getName() : null, filename,
							filesize, destination);
					ft.mimetype = mimetype;
					ft.setSid(sid);
					AndroidFileTransferUtility.registerFileTransferForStreamhost(sid, ft);
					ftModule.acceptStreamInitiation(sender, id, Features.BYTESTREAMS);
				} catch (JaxmppException e) {
					Log.e(TAG, "Could not send stream initiation accept", e);
				}
			}
		}.start();
	}

	private Jaxmpp getJaxmpp(BareJID account) {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.incoming_file);

		Intent intent = getIntent();
		account = JID.jidInstance(intent.getStringExtra("account"));
		sender = JID.jidInstance(intent.getStringExtra("sender"));
		id = intent.getStringExtra("id");
		sid = intent.getStringExtra("sid");
		filename = intent.getStringExtra("filename");
		filesize = intent.getLongExtra("filesize", 0);
		String filesizeStr = null;
		if (filesize == 0) {
			filesize = null;
		} else {
			if (filesize > 1024 * 1024) {
				filesizeStr = String.valueOf(filesize / (1024 * 1024)) + "MB";
			} else if (filesize > 1024) {
				filesizeStr = String.valueOf(filesize / 1024) + "KB";
			} else {
				filesizeStr = String.valueOf(filesize) + "B";
			}
		}
		mimetype = intent.getStringExtra("mimetype");
		if (mimetype == null) {		
			mimetype = guessMimeType();
		}
		Log.v(TAG, "got mimetype = " + mimetype);

		Jaxmpp jaxmpp = getJaxmpp(account.getBareJid());
		RosterItem ri = jaxmpp.getRoster().get(sender.getBareJid());

		senderName = ri != null && ri.getName() != null ? ri.getName() : sender.toString();

		((TextView) findViewById(R.id.incoming_file_from)).setText(senderName);
		((TextView) findViewById(R.id.incoming_file_filename)).setText(filename);
		((TextView) findViewById(R.id.incoming_file_mimetype)).setText(mimetype);
		((TextView) findViewById(R.id.incoming_file_filesize)).setText(filesize == null ? "unknown" : filesizeStr);
		ArrayAdapter storeAdapter = ArrayAdapter.createFromResource(this, R.array.incoming_file_stores, android.R.layout.simple_spinner_item);
		storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner storeSpinner =(Spinner) findViewById(R.id.incoming_file_store); 
		storeSpinner.setAdapter(storeAdapter);
		storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		        store = (String) parent.getItemAtPosition(pos);
		    }
		    public void onNothingSelected(AdapterView<?> parent) {
		    	parent.setSelection(0);
		    }
		});
		
		int pos = 0;
		if (mimetype != null) {
			if (mimetype.startsWith("video/")) {
				pos = storeAdapter.getPosition("Movies");
			}
			else if (mimetype.startsWith("audio/")) {
				pos = storeAdapter.getPosition("Music");
			}
			else if (mimetype.startsWith("image/")) {
				pos = storeAdapter.getPosition("Pictures");
			}
		}
		storeSpinner.setSelection(pos);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.incoming_file_accept: {
			accept();
			break;
		}
		case R.id.incoming_file_reject: {
			reject();
			break;
		}
		default:
			break;
		}

		finish();
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account.getBareJid());
		if (jaxmpp.isConnected()) {
			inflater.inflate(R.menu.incoming_file_menu, menu);
		}
		return true;
	}

	private void reject() {
		Log.v(TAG, "incoming file rejected");
		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account.getBareJid());
		new Thread() {
			@Override
			public void run() {
				FileTransferModule ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
				try {
					ftModule.rejectStreamInitiation(sender, id);
				} catch (JaxmppException e) {
					Log.e(TAG, "Could not send stream initiation reject", e);
				}
			}
		}.start();
	}
	
	private File getPathToSave(String filename) {
		File path = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			String type = Environment.DIRECTORY_DOWNLOADS;
			if (store != null) {
				if ("Pictures".equals(store)) {
					type = Environment.DIRECTORY_PICTURES;
				}
				else if ("Music".equals(store)) {
					type = Environment.DIRECTORY_MUSIC;
				}
				else if ("Movies".equals(store)) {
					type = Environment.DIRECTORY_MOVIES;
				}
			}
			path = Environment.getExternalStoragePublicDirectory(type); 
		}
		if (path == null) {
			path = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "Download" + File.separator);
		}
		return new File(path, filename);		
	}
	
	private String guessMimeType() {
		int idx = filename.lastIndexOf(".");
		if (idx == -1) {
			 return null;
		}
		String suffix = this.filename.substring(idx+1).toLowerCase();		
		if (suffix.equals("png")) {
			return "image/png";
		}
		else if (suffix.equals("jpg") || suffix.equals("jpeg")) {
			return "image/jpeg";
		}
		else if (suffix.equals("avi")) {
			return "video/avi";
		}
		else if (suffix.equals(".mkv")) {
			return "video/x-matroska";
		}
		else if (suffix.equals(".mpg") || suffix.equals(".mp4")) {
			return "video/mpeg";
		}
		else if (suffix.equals(".mp3")) {
			return "audio/mpeg3";
		}
		else if (suffix.equals(".ogg")) {
			return "audio/ogg";
		}
		else if (suffix.equals(".pdf")) {
			return "application/pdf";
		}
		else {
			return null;
		}
	}
}
