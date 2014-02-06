package edu.purdue.isodebug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

//import org.isoblue.isoblue.ISOBlueCommand;
//import org.isoblue.isoblue.ISOBlueDevice;
//import org.isoblue.isobus.ISOBUSSocket;
//import org.isoblue.isobus.PGN;
//import org.isoblue.isobus.PGN.InvalidPGNException;

public class BluetoothAgent {
	private final BluetoothAdapter mBluetoothAdapter;
	public int ConnectorStatus;
	private final Handler mHandler;
	private final Context mContext;
	private volatile BluetoothSocket mSocket; //volatile?

	public BluetoothDevice mydevice;
	private Thread mReadThread, mWriteThread;

	public static int STATUS_NO_BT = 2;
	public static int STATUS_BT_OFF = 1;
	public static int STATUS_BT_ON = 0;

	private static final UUID MY_UUID = UUID
			.fromString("00000000-0000-0000-0000-00000000abcd");
	private static final byte[] MY_PIN = { '0', '0', '0', '0' };
	
	public BluetoothAgent(Context context, Handler handler) {
		mContext = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;
		if (mBluetoothAdapter == null) {
			ConnectorStatus = STATUS_NO_BT; // Unsupported
		}else{
			if (!mBluetoothAdapter.isEnabled()) {
		 	    ConnectorStatus = STATUS_BT_OFF;
		    }else{ 
		    	ConnectorStatus = STATUS_BT_ON;
		    }		
		}

	}
	
	public void populateWithPairedDevices(ArrayList<BluetoothDevice> j){
		if(ConnectorStatus == STATUS_BT_OFF){
			Toast toast = Toast.makeText(mContext, 
					"Bluetooth is off. Please turn it on.",
					Toast.LENGTH_SHORT);
	 	    toast.show();
			return;
		}
		if(ConnectorStatus == STATUS_NO_BT){
			Toast toast = Toast.makeText(mContext, 
					"Bluetooth is not supported by your device. Please consult your manufacturer or the IRS.",
					Toast.LENGTH_SHORT);
	 	    toast.show();
			return;
		}
		
		Log.i("ISOCOMM","Listing paired devices..");
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		
		//If there are paired devices
		if (pairedDevices.size() > 0) {
			Log.i("ISOCOMM","Turns out there are!");
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		    	Log.i("ISOCOMM",device.getName());
		    	j.add(device);
		    }
		}
	}
	
	public void sendSampleBit(final BluetoothDevice device) throws IOException{
		mydevice = device;
		
		try {
			device.getClass().getMethod("setPin", byte[].class)
					.invoke(device, MY_PIN);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			mSocket = mydevice.createRfcommSocketToServiceRecord(MY_UUID);
			mSocket.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		mReadThread = new ReadThread();
		mWriteThread = new WriteThread();

		mReadThread.start();
		mWriteThread.start();

		
	}

	private synchronized BluetoothSocket reconnectSocket() {
		try {
			mSocket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		mSocket = null;
		while (mSocket == null) {
			try {
				mSocket = mydevice.createRfcommSocketToServiceRecord(MY_UUID);
				mSocket.connect();
			} catch (IOException e) {
				mSocket = null;

				// TODO Auto-generated catch block
				e.printStackTrace();
				android.os.SystemClock.sleep(100);
			}
		}

		return mSocket;
	}


	private class ReadThread extends Thread {
		private BufferedReader mReader;
		private ReadThread() throws IOException {
			mReader = new BufferedReader(new InputStreamReader(
					mSocket.getInputStream()));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			
			while(true){
				while(true){
					String line;
					try {
						line = mReader.readLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
					Log.d("Reeived", line);
				}
				
				synchronized (mSocket) {
					try {
						reconnectSocket();
						mReader = new BufferedReader(new InputStreamReader(
								mSocket.getInputStream()));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			
		}
	}
	
	private class WriteThread extends Thread {
		private OutputStream mOut;

		private WriteThread() throws IOException {
			mOut = mSocket.getOutputStream();
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {

			while (true) {
				
				while (true) {
					try {
						byte[] mymsg = "Hello Blue World".getBytes(); 
						mOut.write(mymsg);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					} catch (NullPointerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
				}

				synchronized (mSocket) {
					try {
						mOut = mSocket.getOutputStream();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
}
