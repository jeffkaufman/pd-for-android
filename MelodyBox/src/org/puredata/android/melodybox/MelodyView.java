/**
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 * 
 */

package org.puredata.android.melodybox;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Path.FillType;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class MelodyView extends View {

	private static enum State { UP, MAJOR, MINOR, SHIFT };
	private static final String[] notesSharp = { "C", "C\u266f", "D", "D\u266f", "E", "F", "F\u266f", "G", "G\u266f", "A", "A\u266f", "B" };
	private static final String[] notesFlat  = { "C", "D\u266d", "D", "E\u266d", "E", "F", "G\u266d", "G", "A\u266d", "A", "B\u266d", "B" };
	private static final int[] shifts =        {  0,   -5,   2,   -3,   4,   -1,  6,    1,   -4,   3,   -2,   5  };
	private static final float R0 = 0.25f;
	private static final float R2 = 0.92f;
	private static final float R1 = (float) Math.sqrt((R0 * R0 + R2 * R2) / 2);  // equal area for major and minor fields
	
	private MelodyBox owner;
	private int top = 0;
	private float xCenter, yCenter, xNorm, yNorm;
	private int selectedSegment;
	private State currentState = State.UP;
	private Bitmap keySigs[];
	private Bitmap wheel = null;
	private Bitmap grid = null;
	private Bitmap shadow = null;
	private final Path minorField = new Path();
	private final Path majorField = new Path();
	private final Path rimField = new Path();
	private final Paint labelPaint = new Paint();
	private final Paint selectedPaint = new Paint();

	public MelodyView(Context context) {
		super(context);
		init();
	}

	public MelodyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MelodyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void setOwner(MelodyBox owner) {
		this.owner = owner;
	}

	public void setTopSegment(int top) {
		this.top = top;
		invalidate();
	}

	private void init() {
		RectF r0 = new RectF(-R0, -R0, R0, R0);
		RectF r1 = new RectF(-R1, -R1, R1, R1);
		RectF r2 = new RectF(-R2, -R2, R2, R2);
		RectF r = new RectF(-1, -1, 1, 1);
		float phi = 255, dphi = 30;
		
		minorField.arcTo(r1, phi, dphi, true);
		minorField.arcTo(r0, phi+dphi, -dphi);
		minorField.close();
		minorField.setFillType(FillType.WINDING);
		
		majorField.arcTo(r2, phi, dphi, true);
		majorField.arcTo(r1, phi+dphi, -dphi);
		majorField.close();
		majorField.setFillType(FillType.WINDING);
		
		rimField.arcTo(r, phi, dphi, true);
		rimField.arcTo(r2, phi+dphi, -dphi);
		rimField.close();
		rimField.setFillType(FillType.WINDING);
		
		selectedPaint.setAntiAlias(true);
		selectedPaint.setColor(Color.RED);
		selectedPaint.setStyle(Paint.Style.FILL);
		
		labelPaint.setAntiAlias(true);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setTextAlign(Paint.Align.CENTER);
		labelPaint.setTypeface(Typeface.MONOSPACE);
		labelPaint.setTextSize(0.16f);

		Resources res = getResources();
		keySigs = new Bitmap[] {
				BitmapFactory.decodeResource(res, R.drawable.ks00), BitmapFactory.decodeResource(res, R.drawable.ks01), 
				BitmapFactory.decodeResource(res, R.drawable.ks02), BitmapFactory.decodeResource(res, R.drawable.ks03), 
				BitmapFactory.decodeResource(res, R.drawable.ks04), BitmapFactory.decodeResource(res, R.drawable.ks05), 
				BitmapFactory.decodeResource(res, R.drawable.ks06), BitmapFactory.decodeResource(res, R.drawable.ks07), 
				BitmapFactory.decodeResource(res, R.drawable.ks08), BitmapFactory.decodeResource(res, R.drawable.ks09), 
				BitmapFactory.decodeResource(res, R.drawable.ks10), BitmapFactory.decodeResource(res, R.drawable.ks11)
		};
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int xDim = getDim(widthMeasureSpec);
		int yDim = getDim(heightMeasureSpec);
		int dim = Math.min(xDim, yDim);
		setMeasuredDimension(dim, dim);
	}

	private int getDim(int widthMeasureSpec) {
		int mode = MeasureSpec.getMode(widthMeasureSpec);
		int size = MeasureSpec.getSize(widthMeasureSpec);
		return (mode == MeasureSpec.UNSPECIFIED) ? 320 : size;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(xCenter, yCenter);
		canvas.scale(xCenter, yCenter);
		canvas.drawBitmap(grid, null, new RectF(-1, -1, 1, 1), null);
	}

	private void drawLabel(Canvas canvas, String label, float r) {
		float d = labelPaint.getTextSize() / 3f - r;
		if (label.length() > 1) {
			// ugly hack to work around unicode spacing problem
			canvas.drawText(label.charAt(0) + " ", 0, d, labelPaint);
			canvas.drawText(" " + label.charAt(1), 0, d, labelPaint);
		} else {
			canvas.drawText(label, 0, d, labelPaint);				
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		xCenter = w / 2;
		xNorm = 1 / xCenter;
		yCenter = h / 2;
		yNorm = 1 / yCenter;
		drawBitmaps(w, h);
	}

	private void drawBitmaps(int w, int h) {
		if (grid != null) {
			grid.recycle();
		}
		grid  = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas();
		canvas.translate(xCenter, yCenter);
		canvas.scale(xCenter, yCenter);
		Paint gridPaint = new Paint();
		gridPaint.setAntiAlias(true);
		gridPaint.setStrokeWidth(0.016f);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setColor(Color.DKGRAY);
		
		canvas.setBitmap(grid);
		for (int i = 0; i < 7; i++) {
			canvas.drawLine((float)i/7*2-1, -1, (float)i/7*2-1, 1, gridPaint);
		}
		for (int i = 0; i < 3; i++) {
			canvas.drawLine(-1, (float)i/3*2-1, 1, (float)i/3*2-1, gridPaint);
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX()/(2*xCenter);
		float y = event.getY()/(2*yCenter);
		
		int n = 0;
		switch ((int)(x*7)) {
		case 0: n = 0; break;
		case 1: n = 2; break;
		case 2: n = 4; break;
		case 3: n = 5; break;
		case 4: n = 7; break;
		case 5: n = 9; break;
		case 6: n = 11; break;
		}
		
		n += (int)(y*3)*12;
		
		switch (event.getAction()) {	
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_DOWN:
			owner.playNote(n);
			break;
		default:
			owner.endNote();
		}
		return true;
	}
}
