package nl.mansoft.eduroam;

import android.content.Context;
import android.util.Log;

import org.simalliance.openmobileapi.Channel;
import org.simalliance.openmobileapi.Reader;
import org.simalliance.openmobileapi.SEService;
import org.simalliance.openmobileapi.Session;
import org.simalliance.openmobileapi.util.CommandApdu;
import org.simalliance.openmobileapi.util.ResponseApdu;

import java.io.IOException;

public class SmartcardIO {
    private final String TAG = SmartcardIO.class.getSimpleName();
    private Session session;
    private Channel cardChannel;
    private SEService mSeService;

    public ResponseApdu runAPDU(CommandApdu commandApdu) throws IOException {
        byte cmdApdu[] = commandApdu.toByteArray();
        ResponseApdu responseApdu = new ResponseApdu(cardChannel.transmit(cmdApdu));

        if (!responseApdu.isSuccess()) {
            Log.e(TAG,"ERROR: status: " + String.format("%04X", responseApdu.getSwValue()));
        }
        return responseApdu;
    }

    public void setup(Context context, SEService.CallBack callBack) throws IOException {
        mSeService = new SEService(context, callBack);
    }

    public void teardown() {
        Reader[] readers = mSeService.getReaders();
        closeChannel();
        if (readers.length < 1) {
            Log.e(TAG, "No readers found");
        } else {
            readers[0].closeSessions();
        }
        if (mSeService != null && mSeService.isConnected()) {
            mSeService.shutdown();
        }
    }

    public void closeChannel() {
        if (cardChannel != null && !cardChannel.isClosed()) {
            cardChannel.close();
        }
    }
    public void openChannel(byte aid[]) throws Exception {
        closeChannel();
        cardChannel = session.openLogicalChannel(aid);
    }

    public ResponseApdu login(byte[] password) throws Exception {
        return runAPDU(new CommandApdu((byte)0x00, (byte)0x20, (byte)0x00, (byte)0x01, password));
    }

    public static String hex2(int hex) {
        return String.format("%02X", hex & 0xff);
    }

    public static String hex(byte[] barr) {
        String result;
        if (barr == null) {
            result = "null";
        } else {
            result = "";
            for (byte b : barr) {
                result += " " + hex2(b);
            }
        }
        return result;
    }

    public void setSession() throws IOException {
        Log.d(TAG, "serviceConnected()");
        Log.d(TAG, "Retrieve available readers...");
        Reader[] readers = mSeService.getReaders();
        if (readers.length < 1) {
            Log.d(TAG, "No readers found");
        } else {
            Log.d(TAG, "Create Session from the first reader...");
            session = readers[0].openSession();
        }
    }
}
