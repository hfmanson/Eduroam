package nl.mansoft.eduroam;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.simalliance.openmobileapi.SEService;

import java.io.IOException;
import java.util.List;

public class MainActivity extends ActionBarActivity  implements SEService.CallBack{
    private final static String TAG = MainActivity.class.getSimpleName();
    public final static byte[] AID_3GPP = { (byte) 0xA0, 0x00, 0x00, 0x00, (byte) 0x87 };
    private Button mBtnEduroam;
    private SmartcardIO mSmartcardIO;

    @Override
    public void serviceConnected(SEService seService) {
        mBtnEduroam.setEnabled(true);
    }

    private class MyOnClickListener implements View.OnClickListener {
        final String TAG = MyOnClickListener.class.getSimpleName();

        @Override
        public void onClick(View view) {
            try {
                mSmartcardIO.openChannel(AID_3GPP);
                // select EXT1
                Telecom telecom = new Telecom(mSmartcardIO);
                String user = telecom.readData(Telecom.EF_EXT1, Telecom.RECORD_USER);
                String password = telecom.readData(Telecom.EF_EXT1, Telecom.RECORD_PASSWORD);
                Log.d(TAG, "user: " + user);
                //Log.d(TAG, "password: " + password);
                connectEduroam(user, password);
            } catch (IOException e) {
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
            mBtnEduroam = (Button) findViewById(R.id.btnEduroam);
            mBtnEduroam.setOnClickListener(new MyOnClickListener());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        wm.enableNetwork(netId, true);
    }
}
