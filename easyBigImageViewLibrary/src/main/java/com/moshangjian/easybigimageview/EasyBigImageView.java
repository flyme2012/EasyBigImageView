package com.moshangjian.easybigimageview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.InputSource;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

/**
 * auto:陌上尖
 */
public class EasyBigImageView extends View implements Cache.CacheListener {

	public EasyBigImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	public EasyBigImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public EasyBigImageView(Context context) {
		super(context);
		init(context);
	}
	
	private void init(Context context){
		mRect = new Rect() ;
		slop = ViewConfiguration.get(context).getScaledEdgeSlop();
	}
	
	private int screanWidth ; 
	private int screnHeight ;
	private int bitmapWidth ; 
	private int bitmapHeight ; 
	private Rect mRect ;
	private int slop ;
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		screanWidth = getWidth() ;
		screnHeight = getHeight() ;
		checkSize();
		mCache.setImageSize(screanWidth, screnHeight);
	}
	
	private void checkSize(){
//		if (mBitmapRegionDecoder == null){
//			canMove = false ;
//			return;
//		}
		showWidth = Math.min(screanWidth, bitmapWidth);
		showHeight = Math.min(screnHeight, bitmapHeight);

		//居中
//		mRect.left = (bitmapWidth - showWidth ) / 2 ;
//		mRect.right = mRect.left + showWidth ;
//		mRect.top = (bitmapHeight - showHeight ) / 2 ;
//		mRect.bottom = mRect.top + showHeight ;

		mRect.left = 0 ;
		mRect.right = mRect.left + showWidth ;
		mRect.top = 0 ;
		mRect.bottom = mRect.top + showHeight ;

		canMove = screanWidth < bitmapWidth || screnHeight < bitmapHeight ? true : false ;

	}

	private Cache mCache ;

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save();
		if (!canMove)
			return;
		if (mCache != null){
			mCache.draw(canvas,mRect);
		}
		canvas.restore();
	}
	
	private int startX ;
	private int startY ; 
	
	private boolean canMove ;
	private boolean isTouch = false ;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!canMove) {
			return super.onTouchEvent(event);
		}
		int action = event.getAction() & MotionEvent.ACTION_MASK ;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			isTouch = true ;
			startX = (int) event.getX();
			startY = (int) event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			int delayX = (int) (event.getX() - startX) ;
			int delayY = (int) (event.getY() - startY) ;
			if (delayX !=0 || delayY != 0) {
				move(-delayX, -delayY);
				startX = (int) event.getX() ;
				startY = (int) event.getY();
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			isTouch = false ;
			mCache.loadClearBitmap(mRect);
			break;
		}
		return true;
	}
	
	private void move(int offsetX , int offsetY){
		if ((mRect.left + offsetX) <= 0){
			mRect.left = 0 ;
			mRect.right = showWidth ;
		}else if((mRect.right + offsetX) >= bitmapWidth ) {
			mRect.right = bitmapWidth ;
			mRect.left = mRect.right - showWidth ;
		}else {
			mRect.offset(offsetX,0);
		}

		if ((mRect.top + offsetY) <= 0 ){
			mRect.top = 0 ;
			mRect.bottom = showHeight ;
		}else if((mRect.bottom + offsetY) >= bitmapHeight ) {
			mRect.bottom = bitmapHeight ;
			mRect.top = mRect.bottom - showHeight ;
		}else {
			mRect.offset(0,offsetY);
		}
		invalidate();
	}

	private Options options;
	private int showWidth;
	private int showHeight; 
	public void setBitmapByInputStream(InputStream mInputStream){
		try {
			
			options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565 ;
			options.inJustDecodeBounds = true ;
			
			BitmapFactory.decodeStream(mInputStream, null, options);
			bitmapWidth = options.outWidth ;
			bitmapHeight = options.outHeight ;

			mCache = new Cache(mInputStream);
			mCache.setCacheListener(this);

			requestLayout();
			
		} catch (Exception e) {
		}
	}
	
	public void setBitmapByFile(File file){
		
	}
	
	public void setBitmapByFile(String path){
		File file = new File(path);
		setBitmapByFile(file);
	}

	@Override
	public void cacheOver() {
		if (!isTouch)
			invalidate();
	}
}
