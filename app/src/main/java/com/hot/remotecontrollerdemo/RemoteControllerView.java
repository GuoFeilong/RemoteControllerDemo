package com.hot.remotecontrollerdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by 有人@我 on 2017/9/6.
 */

public class RemoteControllerView extends View {
    private static final String TAG = "RemoteControllerView";
    private static final float SCALE_OF_PADDING = 40.F / 320;
    private static final float SCALE_OF_BIG_CIRCLE = 288.F / 320;
    private static final float SCALE_OF_SMALL_CIRCLE = 100.F / 320;
    private static final float DEF_VIEW_SIZE = 300;

    private OnRemoteControllerClickListener remoteControllerClickListener;

    private int rcvViewHeight;
    private int rcvViewWidth;
    private int rcvPadding;
    private int viewContentHeight;
    private int viewContentWidht;
    private Point centerPoint;
    private int rcvTextColor;
    private int rcvShadowColor;
    private int rcvStrokeColor;
    private int rcvStrokeWidth;
    private int rcvTextSize;
    private int rcvDegree;
    private int rcvOtherDegree;
    private Paint rcvTextPaint;
    private Paint rcvShadowPaint;
    private Paint rcvStrokePaint;
    private Paint rcvWhitePaint;


    private RectF ovalRectF;
    private List<Path> ovalPaths;
    private List<Region> ovalRegions;
    private List<Paint> ovalPaints;
    private int seleced;
    private Point textPointInView;

    public RemoteControllerView(Context context) {
        this(context, null);
    }

    public RemoteControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemoteControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttribute(context, attrs, defStyleAttr);
        initPaints();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerPoint = new Point(w / 2, h / 2);

        ovalPaths = new ArrayList<>();
        ovalRegions = new ArrayList<>();
        ovalPaints = new ArrayList<>();

        rcvViewWidth = w;
        rcvViewHeight = h;
        rcvPadding = (int) (Math.min(w, h) * SCALE_OF_PADDING);
        viewContentWidht = rcvViewWidth - rcvPadding;
        viewContentHeight = rcvViewHeight - rcvPadding;
        textPointInView = getTextPointInView(rcvTextPaint, "OK", 0, 0);
        // 注意外环的线宽占用的尺寸
        ovalRectF = new RectF(-rcvViewWidth / 2 + rcvStrokeWidth, -rcvViewWidth / 2 + rcvStrokeWidth, rcvViewHeight / 2 - rcvStrokeWidth, rcvViewHeight / 2 - rcvStrokeWidth);

        for (int i = 0; i < 4; i++) {
            Region tempRegin = new Region();
            Path tempPath = new Path();

            float tempStarAngle = 0;
            float tempSweepAngle;
            if (i % 2 == 0) {
                tempSweepAngle = rcvDegree;
            } else {
                tempSweepAngle = rcvOtherDegree;
            }
            // 计算扇形的开始角度,这里不能用canvas旋转的方法
            // 因为设计到扇形点击,如果画布旋转,会因为角度问题,导致感官上看上去点击错乱的问题,
            // 其实点击的区域是正确的,就是因为旋转角度导致的,注意,

            // 这块需要一个n的公式,本人没学历不会总结通用公式.....
            switch (i) {
                case 0:
                    tempStarAngle = -rcvDegree / 2;
                    break;
                case 1:
                    tempStarAngle = rcvDegree / 2;
                    break;
                case 2:
                    tempStarAngle = rcvDegree / 2 + rcvOtherDegree;
                    break;
                case 3:
                    tempStarAngle = rcvDegree / 2 + rcvOtherDegree + rcvDegree;
                    break;

            }

            tempPath.moveTo(0, 0);
            tempPath.lineTo(viewContentWidht / 2, 0);
            tempPath.addArc(ovalRectF, tempStarAngle, tempSweepAngle);
            tempPath.lineTo(0, 0);
            tempPath.close();
            RectF tempRectF = new RectF();
            tempPath.computeBounds(tempRectF, true);
            tempRegin.setPath(tempPath, new Region((int) tempRectF.left, (int) tempRectF.top, (int) tempRectF.right, (int) tempRectF.bottom));


            ovalPaths.add(tempPath);
            ovalRegions.add(tempRegin);
            ovalPaints.add(creatPaint(Color.WHITE, 0, Paint.Style.FILL, 0));
        }

        Region smallCircleRegion = new Region();
        Path smallCirclePath = new Path();
        smallCirclePath.moveTo(0, 0);
        smallCirclePath.lineTo(Math.min(rcvViewWidth, rcvViewHeight) * SCALE_OF_SMALL_CIRCLE / 2, 0);
        smallCirclePath.addCircle(0, 0, Math.min(rcvViewWidth, rcvViewHeight) * SCALE_OF_SMALL_CIRCLE / 2, Path.Direction.CW);
        smallCirclePath.lineTo(0, 0);
        smallCirclePath.close();
        RectF tempRectF = new RectF();
        smallCirclePath.computeBounds(tempRectF, true);
        smallCircleRegion.setPath(smallCirclePath, new Region((int) tempRectF.left, (int) tempRectF.top, (int) tempRectF.right, (int) tempRectF.bottom));

        ovalPaths.add(smallCirclePath);
        ovalRegions.add(smallCircleRegion);
        ovalPaints.add(creatPaint(Color.WHITE, 0, Paint.Style.FILL, 0));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize;
        int heightSize;

        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEF_VIEW_SIZE, getResources().getDisplayMetrics());
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }

        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEF_VIEW_SIZE, getResources().getDisplayMetrics());
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(centerPoint.x, centerPoint.y);
        canvas.drawCircle(0, 0, Math.min(rcvViewWidth, rcvViewHeight) / 2, rcvStrokePaint);
        for (int i = 0; i < ovalRegions.size(); i++) {
            canvas.drawPath(ovalPaths.get(i), ovalPaints.get(i));
        }
        canvas.drawCircle(0, 0, Math.min(rcvViewWidth, rcvViewHeight) * SCALE_OF_SMALL_CIRCLE / 2, rcvStrokePaint);
        canvas.drawText("OK", textPointInView.x, textPointInView.y, rcvTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x;
        float y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX() - centerPoint.x;
                y = event.getY() - centerPoint.y;

                for (int i = 0; i < ovalRegions.size(); i++) {
                    Region tempRegin = ovalRegions.get(i);
                    boolean contains = tempRegin.contains((int) x, (int) y);
                    if (contains) {
                        seleced = i;
                    }
                }
                resetPaints();
                ovalPaints.get(seleced).setColor(rcvShadowColor);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                resetPaints();
                invalidate();
                remoteClickAction();
                break;

        }
        return true;
    }

    private void remoteClickAction() {
        if (remoteControllerClickListener != null) {
            switch (seleced) {
                case 0:
                    remoteControllerClickListener.rightClick();
                    break;
                case 1:
                    remoteControllerClickListener.bottomClick();
                    break;
                case 2:
                    remoteControllerClickListener.leftClick();
                    break;
                case 3:
                    remoteControllerClickListener.topClick();
                    break;
                case 4:
                    remoteControllerClickListener.centerOkClick();
                    break;
            }
        }
    }


    private void initAttribute(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RemoteControllerView, defStyleAttr, R.style.def_remote_controller);
        int indexCount = typedArray.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int attr = typedArray.getIndex(i);
            switch (attr) {
                case R.styleable.RemoteControllerView_rcv_text_color:
                    rcvTextColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.RemoteControllerView_rcv_text_size:
                    rcvTextSize = typedArray.getDimensionPixelSize(attr, 0);
                    break;
                case R.styleable.RemoteControllerView_rcv_shadow_color:
                    rcvShadowColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.RemoteControllerView_rcv_stroke_color:
                    rcvStrokeColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.RemoteControllerView_rcv_stroke_width:
                    rcvStrokeWidth = typedArray.getDimensionPixelOffset(attr, 0);
                    break;
                case R.styleable.RemoteControllerView_rcv_oval_degree:
                    rcvDegree = typedArray.getInt(attr, 0);
                    rcvOtherDegree = (int) ((360 - rcvDegree * 2) / 2.F);
                    break;

            }
        }
        typedArray.recycle();
    }


    private void initPaints() {
        rcvTextPaint = creatPaint(rcvTextColor, rcvTextSize, Paint.Style.FILL, 0);
        rcvShadowPaint = creatPaint(rcvShadowColor, 0, Paint.Style.FILL, 0);
        rcvStrokePaint = creatPaint(rcvStrokeColor, 0, Paint.Style.STROKE, 0);
        rcvWhitePaint = creatPaint(Color.WHITE, 0, Paint.Style.FILL, 0);
    }


    private Paint creatPaint(int paintColor, int textSize, Paint.Style style, int lineWidth) {
        Paint paint = new Paint();
        paint.setColor(paintColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(lineWidth);
        paint.setDither(true);
        paint.setTextSize(textSize);
        paint.setStyle(style);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        return paint;
    }

    private void resetPaints() {
        for (Paint p : ovalPaints) {
            p.setColor(Color.WHITE);
        }
    }

    private Point getTextPointInView(Paint textPaint, String textDesc, int w, int h) {
        if (null == textDesc) return null;
        Point point = new Point();
        int textW = (w - (int) textPaint.measureText(textDesc)) / 2;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        int textH = (int) Math.ceil(fm.descent - fm.top);
        point.set(textW, h / 2 + textH / 2 - textH / 4);
        return point;
    }


    public interface OnRemoteControllerClickListener {
        void topClick();

        void leftClick();

        void rightClick();

        void bottomClick();

        void centerOkClick();
    }

    public void setRemoteControllerClickListener(OnRemoteControllerClickListener remoteControllerClickListener) {
        this.remoteControllerClickListener = remoteControllerClickListener;
    }
}
