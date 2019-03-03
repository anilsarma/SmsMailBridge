package com.smartdeviceny.smsmailbridge;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import com.smartdeviceny.smsmailbridge.R;
import com.smartdeviceny.smsmailbridge.SmsReceiver;
import com.smartdeviceny.smsmailbridge.misc.Constants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SmsMessage.class)
public class SmsReceiverTest {

    @Test
    public void epochSmsReceiverTest() {
        SmsReceiver receiver = new SmsReceiver();
        String time = receiver.getPrintableTime(1551628461000l);
        System.out.println(time);
        Assert.assertEquals(time, "03/03/2019 10:54:21 EST");
    }

    private static byte reverseByte(byte b) {
        return (byte) ((b & 0xF0) >> 4 | (b & 0x0F) << 4);
    }

    private static byte[] makeSmsPDU(String sender, String body) {
        byte[] pdu = null;
        byte[] scBytes = "0000000000".getBytes(); // PhoneNumberUtils.networkPortionToCalledPartyBCD("0000000000");
        byte[] senderBytes = sender.getBytes(); // PhoneNumberUtils.networkPortionToCalledPartyBCD(sender);
        int lsmcs = scBytes.length;
        byte[] dateBytes = new byte[7];
        Calendar calendar = new GregorianCalendar();
        dateBytes[0] = reverseByte((byte) (calendar.get(Calendar.YEAR)));
        dateBytes[1] = reverseByte((byte) (calendar.get(Calendar.MONTH) + 1));
        dateBytes[2] = reverseByte((byte) (calendar.get(Calendar.DAY_OF_MONTH)));
        dateBytes[3] = reverseByte((byte) (calendar.get(Calendar.HOUR_OF_DAY)));
        dateBytes[4] = reverseByte((byte) (calendar.get(Calendar.MINUTE)));
        dateBytes[5] = reverseByte((byte) (calendar.get(Calendar.SECOND)));
        dateBytes[6] = reverseByte((byte) ((calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000 * 15)));
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            bo.write(lsmcs);
            bo.write(scBytes);
            bo.write(0x04);
            bo.write((byte) sender.length());
            bo.write(senderBytes);
            bo.write(0x00);
            bo.write(0x00); // encoding: 0 for default 7bit
            bo.write(dateBytes);
            try {
                String sReflectedClassName = "com.android.internal.telephony.GsmAlphabet";
                Class cReflectedNFCExtras = Class.forName(sReflectedClassName);
                Method stringToGsm7BitPacked = cReflectedNFCExtras.getMethod("stringToGsm7BitPacked", new Class[]{String.class});
                stringToGsm7BitPacked.setAccessible(true);
                byte[] bodybytes = (byte[]) stringToGsm7BitPacked.invoke(null, body);
                bo.write(bodybytes);
            } catch (Exception e) {
            }

            pdu = bo.toByteArray();
        } catch (IOException e) {
        }
        return pdu;

    }

    @Test
    public void onRecvTest() {
        SmsReceiver receiver = new SmsReceiver();
        Context context = Mockito.mock(Context.class);
        Intent intent = Mockito.mock(Intent.class);
        final Bundle myBundle = Mockito.mock(Bundle.class);
        ;
        final SharedPreferences pref = Mockito.mock(SharedPreferences.class);
        final Object pdus[] = new Object[1];
        final SmsMessage message = Mockito.mock(SmsMessage.class);

        //pdus[0] = SmsMessage.getSubmitPdu("510123456", "610123456", "This is a test message", false).encodedMessage;

        pdus[0] = makeSmsPDU("510123456", "This is a test message");

        PowerMockito.mockStatic(SmsMessage.class);

        Mockito.when(SmsMessage.createFromPdu(Mockito.any(byte[].class), Mockito.anyString())).thenReturn(message);
        Mockito.when(SmsMessage.createFromPdu(Mockito.any(byte[].class))).thenReturn(message);

        Mockito.when(message.getOriginatingAddress()).thenReturn("610123456");
        Mockito.when(message.getServiceCenterAddress()).thenReturn("210123456");
        Mockito.when(message.getTimestampMillis()).thenReturn(1551628461000l);
        Mockito.when(myBundle.get("pdus")).thenReturn(pdus);
        Mockito.when(pref.getString(Constants.ACCOUNT_NAME, "")).thenReturn("test_account@google.com");
        Mockito.when(intent.getExtras()).thenReturn(myBundle);
        Mockito.when(context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE)).thenReturn(pref);
        Mockito.when(context.getApplicationContext()).thenReturn(context);
        Mockito.when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(Mockito.mock(TelephonyManager.class));

        Mockito.when(context.startService(Mockito.any(Intent.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.println("Start Called");
                Intent intent = invocation.getArgument(0);
//                Assert.assertNotNull(intent.getStringExtra(Constants.ACCOUNT_NAME));
//                Assert.assertNotNull(intent.getStringExtra(Constants.EXTRA_SUBJECT));
//                Assert.assertNotNull(intent.getStringExtra(Constants.EXTRA_BODY));

                return null;
            }
        });
        receiver.onReceive(context, intent);

    }
}
