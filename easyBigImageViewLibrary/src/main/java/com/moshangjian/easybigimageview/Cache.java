package com.moshangjian.easybigimageview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by 陌上尖 on 15/11/16.
 */
public class Cache implements Handler.Callback {
    private final static String TAG = "Cache";

    public Bitmap cacheBitmap ;
    private BitmapRegionDecoder regionDecoder ;
    private final  BitmapFactory.Options options = new BitmapFactory.Options();
    private CacheThread mhandlerThread ;
    private Handler mHandler ;
    private   CacheState state  ;
    public Bitmap sampleBitmap ;
    public final  static int SAMPLE_RADIO = 2 ;

    private Rect dst ;
    private Rect src ;

    private Rect cacheRect ;

    @Override
    public boolean handleMessage(Message msg) {
        if (mCacheListener != null)
            mCacheListener.cacheOver();
        return false;
    }

    enum CacheState {LOAD_CACHE,READ,UNCACHE};

    public Cache(){
        this(null);
    }

    public Cache(InputStream bitmapInput){
        setState(CacheState.UNCACHE);
        options.inPreferredConfig = Bitmap.Config.RGB_565 ;
        mhandlerThread = new CacheThread("cache");
        mhandlerThread.start();
        initDecoder(bitmapInput);
        dst = new Rect();
        src = new Rect();
    }

    public void draw(Canvas canvas , Rect rect){
        if (cacheRect != null && rect.left == cacheRect.left && rect.top == cacheRect.top && cacheBitmap != null && state == CacheState.READ){
            canvas.drawBitmap(cacheBitmap, 0, 0, null);
            cacheRect = null ;
            return;
        }
        countSize(rect);
        if (sampleBitmap != null)
            canvas.drawBitmap(sampleBitmap, src, dst, null);
        if (cacheBitmap == null )
            loadClearBitmap(new Rect(0,0,screenWidth,screenHeight));
    }

    private void countSize(Rect rect){
        src.left = rect.left >> Cache.SAMPLE_RADIO ;
        src.top = rect.top >> Cache.SAMPLE_RADIO ;
        src.right = rect.right >> Cache.SAMPLE_RADIO ;
        src.bottom = rect.bottom >> Cache.SAMPLE_RADIO ;
    }

    public void setImageSize(int screenWidth,int screenHeight){
        this.screenWidth = screenWidth ;
        this.screenHeight = screenHeight ;
        dst.set(0,0,screenWidth,screenHeight);
    }

    public CacheState getState() {
        return state;
    }

    public void setState(CacheState state) {
        this.state = state;
    }

    public void loadClearBitmap(Rect clearRect){
        sendMessageToLoadCache(clearRect);
    }

    public void initSampleBitmap(InputStream in){
        options.inSampleSize = 1 << SAMPLE_RADIO ;
        sampleBitmap = BitmapFactory.decodeStream(in,null,options);
        options.inSampleSize = 1;
    }


    public void setBitmapSource(String filePath){
        setBitmapSource(new File(filePath));
    }

    public void setBitmapSource(File filePath){
        try {
            InputStream inputStream = new FileInputStream(filePath);
            setBitmapSource(inputStream);
        } catch (FileNotFoundException e) {
        }
    }

    public void setBitmapSource(InputStream filePath){
        initDecoder(filePath);
    }


    private void sendMessageToLoadCache(Rect currentRect){
        try{
            if (getState() == CacheState.LOAD_CACHE) {
                return;
            }
            setState(CacheState.LOAD_CACHE);
            if (mHandler == null){
                mHandler = new Handler(mhandlerThread.getLooper(),mhandlerThread);
            }
            Message message = mHandler.obtainMessage();
            if (currentRect != null)
                message.obj = currentRect ;
            message.what = 1 ;
            mHandler.sendMessage(message);
        }catch (Exception e){
        }
    }

    private void initDecoder(InputStream bitmapSource){
        if (regionDecoder == null && bitmapSource != null) {
            try {
                regionDecoder = BitmapRegionDecoder.newInstance(bitmapSource, false);
                initSampleBitmap(bitmapSource);
            } catch (IOException e) {}
        }
    }

    private void loadCache(Rect currentRect){
        if (currentRect == null){
            cacheRect = new Rect(0,0,screenWidth,screenHeight);
        }else{
            cacheRect = currentRect ;
        }
        cacheBitmap = regionDecoder.decodeRegion(cacheRect, options) ;
        Log.e(TAG,"loadedCache");
        setState(CacheState.READ);
        Handler handler = new Handler(Looper.getMainLooper(),this);
        handler.sendEmptyMessage(2);
    }

    private int screenWidth ;
    private int screenHeight ;

    class  CacheThread extends HandlerThread   implements Handler.Callback{

        public CacheThread(String name) {
            super(name);
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1){
                if (msg.obj != null)
                    loadCache((Rect)msg.obj);
                else
                    loadCache(null);
            }
            return true;
        }
    }


    private CacheListener mCacheListener ;
    public void setCacheListener(CacheListener mcacheListener){
        this.mCacheListener = mcacheListener ;
    }


    public interface  CacheListener{
        public void cacheOver();
    }



}
