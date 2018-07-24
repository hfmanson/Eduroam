package nl.mansoft.eduroam;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.simalliance.openmobileapi.SEService;
import nl.mansoft.openmobileapi.util.ResponseApdu;

import java.io.IOException;
import java.util.List;

public class MainActivity extends ActionBarActivity  implements SEService.CallBack{
    private final static String TAG = MainActivity.class.getSimpleName();
    public final static byte[] AID_3GPP = { (byte) 0xA0, 0x00, 0x00, 0x00, (byte) 0x87 };
    public final static byte[] AID_ISOAPPLET = { (byte) 0xF2, (byte) 0x76, (byte) 0xA2, (byte) 0x88, (byte) 0xBC, (byte) 0xFB, (byte) 0xA6, (byte) 0x9D, (byte) 0x34, (byte) 0xF3, (byte) 0x10, (byte) 0x01 };
    private Button mBtnEduroam;
    private Button mBtnTelecom;
    private TextView mTextView;
    private SmartcardIO mSmartcardIO;
    private EditText mEditText;

    @Override
    public void serviceConnected(SEService seService) {
        try {
            mSmartcardIO.setSession();
            mBtnEduroam.setEnabled(true);
            mBtnTelecom.setEnabled(true);
        } catch (IOException e) {
            mTextView.setText("Error: " + e.getMessage());
        }
    }

    private class EduroamOnClickListener implements View.OnClickListener {
        final String TAG = EduroamOnClickListener.class.getSimpleName();

        public void doEduroam() throws Exception {
            mSmartcardIO.openChannel(AID_ISOAPPLET);
            Eduroam eduroam = new Eduroam(mSmartcardIO);
            String pin = mEditText.getText().toString();
            ResponseApdu responseApdu = null;
            if (!pin.isEmpty()) {
                responseApdu = eduroam.login(pin.getBytes());
            }
            if (responseApdu == null || responseApdu.isSuccess()) {
                responseApdu = eduroam.readEduroam();
            }
            int sw = responseApdu.getSwValue();
            if (responseApdu.isSuccess()) {
                byte data[] = responseApdu.getData();
                String user = Eduroam.readStringFromByteArray(data, Eduroam.OFFSET_USER);
                String password = Eduroam.readStringFromByteArray(data, Eduroam.OFFSET_PASSWORD);
                Log.d(TAG, "user: " + user);
                //Log.d(TAG, "password: " + password);
                mTextView.setText(pin.isEmpty() ?  "" : "PIN accepted");
                connectEduroam(user, password);
            } else if (sw == 0x6982) { // Security status not satisfied
                //loge("No credentials found on SIM card");
                mTextView.setText("PIN required!");
            } else if ((sw & 0xFFF0) == 0x63C0) {
                mTextView.setText("Wrong PIN, " + (sw & 0x000F) + " attempts left!");
            }
            mSmartcardIO.closeChannel();
        }

        @Override
        public void onClick(View view) {
            try {
                doEduroam();
            } catch (Exception e) {
                mTextView.setText("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private class TelecomOnClickListener implements View.OnClickListener {
        final String TAG = TelecomOnClickListener.class.getSimpleName();

        public void doTelecom() throws Exception {
            mSmartcardIO.openChannel(AID_3GPP);
            // select EXT1
            Telecom telecom = new Telecom(mSmartcardIO);
            String user = telecom.readData(Telecom.EF_EXT1, Telecom.RECORD_USER);
            String password = telecom.readData(Telecom.EF_EXT1, Telecom.RECORD_PASSWORD);
            Log.d(TAG, "user: " + user);
            //Log.d(TAG, "password: " + password);
            connectEduroam(user, password);
        }

        @Override
        public void onClick(View view) {
            try {
                doTelecom();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            mSmartcardIO = new SmartcardIO();
            mSmartcardIO.setup(this, this);
            mEditText = (EditText) findViewById(R.id.edtPin);
            mBtnEduroam = (Button) findViewById(R.id.btnEduroam);
            mBtnEduroam.setOnClickListener(new EduroamOnClickListener());
            mBtnTelecom = (Button) findViewById(R.id.btnTelecom);
            mBtnTelecom.setOnClickListener(new TelecomOnClickListener());
            mTextView = (TextView) findViewById(R.id.textView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        mSmartcardIO.teardown();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connectEduroam(String user, String password) {
        WifiManager wm =  (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> wifiConfigurationList = wm.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : wifiConfigurationList) {
            Log.e(TAG, wifiConfiguration.SSID);
            if (wifiConfiguration.SSID.equals("\"eduroam\"")) {
                wm.removeNetwork(wifiConfiguration.networkId);
            }
        }
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"eduroam\"";
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        WifiEnterpriseConfig wifiEnterpriseConfig = new WifiEnterpriseConfig();
        wifiEnterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
        wifiEnterpriseConfig.setIdentity(user);
        wifiEnterpriseConfig.setPassword(password);
        wifiConfiguration.enterpriseConfig = wifiEnterpriseConfig;
        int netId = wm.addNetwork(wifiConfiguration);
        // Connect to eduroam
        wm.enableNetwork(netId, true);
    }
}
