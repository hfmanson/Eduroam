package nl.mansoft.eduroam;

import android.util.Log;

import org.simalliance.openmobileapi.util.CommandApdu;
import org.simalliance.openmobileapi.util.ResponseApdu;

import java.io.IOException;

public class Eduroam {
    private final String TAG = Eduroam.class.getSimpleName();
    public final static int OFFSET_USER = 0x00;
    public final static int OFFSET_PASSWORD = 0x20;
    private SmartcardIO mSmartcardIO;

    public Eduroam(SmartcardIO smartcardIO) {
        mSmartcardIO = smartcardIO;
    }

    public ResponseApdu login(byte password[]) throws Exception {
        return mSmartcardIO.login(password);
    }

    public ResponseApdu selectEduroam() throws IOException {
        CommandApdu c = new CommandApdu((byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x00, new byte[] { 0x10, 0x00 });
        return mSmartcardIO.runAPDU(c);
    }

    public ResponseApdu readEduroam() throws IOException {
        ResponseApdu result = selectEduroam();
        if (result.isSuccess()) {
            Log.d(TAG, "reading eduroam");
            CommandApdu c = new CommandApdu((byte)0x00, (byte)0xB0, (byte)0x00, (byte)0x00);
            result = mSmartcardIO.runAPDU(c);
        }
        return result;
    }

    public ResponseApdu updateEduroam(byte[] data) throws IOException {
        ResponseApdu result = selectEduroam();
        if (result.isSuccess()) {
            Log.d(TAG, "updating eduroam");
            CommandApdu c = new CommandApdu((byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, data);
            result = mSmartcardIO.runAPDU(c);
        }
        return result;
    }

    public static String readStringFromByteArray(byte[] barr, int offset) {
        String result = "";
        byte b;
        int i = offset;
        while ((b = barr[i++]) != (byte) 0xFF) {
            result += (char) b;
        }
        return result;
    }

}
