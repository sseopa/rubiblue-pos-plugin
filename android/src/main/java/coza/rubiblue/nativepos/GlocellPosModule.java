package coza.rubiblue.nativepos;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import android.os.Build;
import android.util.Log;

import cn.weipass.pos.sdk.LatticePrinter;
import cn.weipass.pos.sdk.LatticePrinter.*;
import cn.weipass.pos.sdk.Weipos.*;
import cn.weipass.pos.sdk.impl.WeiposImpl;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;


import com.wisedevice.sdk.WiseDeviceSdk;
import com.wisepos.smartpos.InitPosSdkListener;
import com.wisepos.smartpos.WisePosException;
import com.wisepos.smartpos.WisePosSdk;
import com.wisepos.smartpos.device.Device;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;



import com.wisepos.smartpos.printer.Printer;
import com.wisepos.smartpos.printer.PrinterListener;

import android.os.Bundle;
import com.wisedevice.sdk.IInitDeviceSdkListener;
import com.wisedevice.sdk.WiseDeviceSdk;
import com.wisepos.smartpos.printer.TextInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@NativePlugin(
        requestCodes={GlocellPosModule.REQUEST_CODE,GlocellPosModule.PRINT_REQUEST_CODE}
)
public class GlocellPosModule extends Plugin {
    protected static final int REQUEST_CODE = 1; // Unique request code
    protected static final int PRINT_REQUEST_CODE = 2;
    public static int tsn=1;
    public static String lastSentTsn="";
    private PluginCall mReturnResults;

    public WisePosSdk wisePosSdk;
    public WiseDeviceSdk wiseDeviceSdk;

    public Device device;
    public com.wisedevice.sdk.version.Device dDevice = null;
    public Printer printer;
    public static final int PRINT_STYLE_CENTER = 0x02;  //print style to the center

    public void sdkInit(){
        wiseDeviceSdk = WiseDeviceSdk.getInstance();
        wisePosSdk = WisePosSdk.getInstance();

        wisePosSdk.initPosSdk(getContext(), new InitPosSdkListener() {  //Initialize the SDK and bind the service
            @Override
            public void onInitPosSuccess() {
                Log.d("sdkdemo", "initPosSdk: success!");
            }

            @Override
            public void onInitPosFail(int i) {
                Log.d("sdkdemo", "initPosSdk: fail!");
            }
        });
        wiseDeviceSdk.initDeviceSdk(getContext(), new IInitDeviceSdkListener() {
            @Override
            public void onInitPosSuccess() {
                Log.d("devicesdkdemo", "initPosSdk: success!");
            }

            @Override
            public void onInitPosFail(int i) {
                Log.d("devicesdkdemo", "initPosSdk: fail!");
            }
        });
    }

    void printText(String printText,String Logo) {
    sdkInit();
        try {
            printer = WisePosSdk.getInstance().getPrinter();    //Get the Printer object.
            InputStream inputStream = null;

            Map<String, Object> map = new HashMap<>();
            int ret;
            printer.initPrinter();  //Initializing the printer.


            map = printer.getPrinterStatus();   //Gets the current status of the printer.
            if (map == null) {
                return;
            }
            //Gets whether the printer is out of paper from the map file.
            if ((byte) map.get("paper") == 1) {
                return;
            } else {
            }

            //When printing text information, the program needs to set the printing font. The current setting is the default font.
            Bundle bundle1 = new Bundle();
            Bitmap bitmap = null;

            byte[] bytes =  android.util.Base64.decode(Logo,  android.util.Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            bitmap = toGrayscale(decodedByte);
            bitmap = convertGreyImgByFloyd(bitmap);
            printer.addPicture(PRINT_STYLE_CENTER, bitmap);

            bundle1.putString("font", "DEFAULT");
            printer.setPrintFont(bundle1);

            TextInfo textInfo = new TextInfo();

            textInfo.setFontSize(24);
            printer.setLineSpacing(1);

            textInfo.setText(printText);
            printer.addSingleText(textInfo);

            Bundle printerOption = new Bundle();
            //Start printing
            printer.startPrinting(printerOption, new PrinterListener() {
                @Override
                public void onError(int i) {

                }

                @Override
                public void onFinish() {
  //                  logMsg("print success\n");
                    try {
                        //After printing, Feed the paper.
                        printer.feedPaper(24);
                    } catch (WisePosException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onReport(int i) {
                    //The callback method is reserved and does not need to be implemented
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
//            logMsg("print failed" + e.toString() + "\n");
        }
    }
    public static Bitmap convertGreyImgByFloyd(Bitmap img) {
        int width = img.getWidth();         //Obtain the width of the bitmap
        int height = img.getHeight();       //Obtain the height of the bitmap

        int[] pixels = new int[width * height]; //Create a pixel array based on the size of the bitmap
        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int[] gray=new int[height*width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                int red = ((grey  & 0x00FF0000 ) >> 16);
                gray[width*i+j]=red;
            }
        }

        int e=0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int g=gray[width*i+j];
                if (g>=128) {
                    pixels[width*i+j]=0xffffffff;
                    e=g-255;
                }else {
                    pixels[width*i+j]=0xff000000;
                    e=g-0;
                }
                if (j<width-1&&i<height-1) {
                    gray[width*i+j+1]+=3*e/8;
                    gray[width*(i+1)+j]+=3*e/8;
                    gray[width*(i+1)+j+1]+=e/4;
                }else if (j==width-1&&i<height-1) {
                    gray[width*(i+1)+j]+=3*e/8;
                }else if (j<width-1&&i==height-1) {
                    gray[width*(i)+j+1]+=e/4;
                }
            }
        }
        Bitmap mBitmap=Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return mBitmap;
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();


        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
    @PluginMethod
    public void getSerial(PluginCall call) {
        device = WisePosSdk.getInstance().getDevice();  //Get the Device object.
        if (device != null ){
            JSObject ret = new JSObject();

        }else{
            try {
                JSObject ret = new JSObject();
                String SerialNumber = Build.SERIAL;
                if (SerialNumber == "unknown") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        SerialNumber = Build.getSerial();
                    }
                }
                ret.put("value", SerialNumber);
                call.success(ret);
            } catch (Exception ex) {
                JSObject ret = new JSObject();
                ret.put("value", "unknown");
                call.success(ret);
            }
        }

    }

    @PluginMethod()
    public void print(PluginCall call) {

        device = WisePosSdk.getInstance().getDevice();
        final String receiptContent = call.getString("ReceiptText");
        final String receiptLogo = call.getString("ReceiptLogo");
        if(device != null){
            printText(receiptContent,receiptLogo);
        }else {
            try {
                final FontSize size = FontSize.MEDIUM;
                final FontStyle style = FontStyle.NORMAL;
                mReturnResults = call;

                WeiposImpl.as().init(getContext(), new OnInitListener() {
                    @Override
                    public void onInitOk() {
                        String deviceInfo = WeiposImpl.as().getDeviceInfo();
                        LatticePrinter latticePrinter = WeiposImpl.as().openLatticePrinter();
                        try {

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                byte[] encodeByte = Base64.getDecoder().decode(receiptLogo);

                                latticePrinter.printImage(encodeByte, cn.weipass.pos.sdk.IPrint.Gravity.CENTER);
                            }
                        } catch (Exception d) {
                            latticePrinter.printText("Exception", LatticePrinter.FontFamily.SONG, size, style);
                        }


                        String[] receiptContents = receiptContent.split("\\|");
                        if (receiptContents.length > 0) {
                            for (String line : receiptContents) {
                                latticePrinter.printText(line, LatticePrinter.FontFamily.SONG, size, style);
                                latticePrinter.printText("\n", LatticePrinter.FontFamily.SONG, size, style);
                            }
                        } else {
                            latticePrinter.printText(receiptContent, LatticePrinter.FontFamily.SONG, size, style);
                        }
                        latticePrinter.submitPrint();
                        JSObject ret = new JSObject();
                        ret.put("value", "latticePrinter.submitPrint()");
                        mReturnResults.success(ret);
                        WeiposImpl.as().destroy();
                    }

                    @Override
                    public void onError(String s) {
                        JSObject ret = new JSObject();
                        ret.put("value", s);
                        mReturnResults.success(ret);
                        WeiposImpl.as().destroy();
                    }

                    @Override
                    public void onDestroy() {
                        JSObject ret = new JSObject();
                        ret.put("value", "onDestroy");
                        mReturnResults.success(ret);
                        // listener.onPrinterClosed("");
                    }
                });
            } catch (Exception ex) {
                JSObject ret = new JSObject();
                ret.put("value", "printing failed " + ex.getMessage());
                mReturnResults.success(ret);
            }
        }

    }

}
