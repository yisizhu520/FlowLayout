/*
 * Copyright 2013 Blaz Solar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wang.mogujun.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wang.mogujun.flowlayout.R;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class FlowLayout extends ViewGroup {

    public static final String TAG = "MOGUJUN";

    public static final int DEFAULT_SPACING = 8;

    private int mGravity = (isIcs() ? Gravity.START : Gravity.LEFT) | Gravity.TOP;

    private int mVerticalSpacing; //vertical spacing
    private int mHorizontalSpacing; //horizontal spacing

    private final List<List<View>> mLines = new ArrayList<>();
    private final List<Integer> mLineHeights = new ArrayList<>();
    private final List<Integer> mLineMargins = new ArrayList<>();
    private final List<Integer> mLineWidths = new ArrayList<>();

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout, defStyleAttr, defStyleRes);

        try {
            mHorizontalSpacing = (int) ta.getDimension(R.styleable.FlowLayout_horizonSpacing, DEFAULT_SPACING);
            mVerticalSpacing = (int) ta.getDimension(R.styleable.FlowLayout_verticalSpacing, DEFAULT_SPACING);
            int index = ta.getInt(R.styleable.FlowLayout_android_gravity, -1);
            if (index > 0) {
                setGravity(index);
            }
        } finally {
            ta.recycle();
        }

    }

    public void setHorizontalSpacing(int pixelSize) {
        mHorizontalSpacing = pixelSize;
        requestLayout();
    }

    public void setVerticalSpacing(int pixelSize) {
        mVerticalSpacing = pixelSize;
        requestLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        mLines.clear();
        mLineHeights.clear();
        mLineWidths.clear();

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int widthUsed = getPaddingLeft() + getPaddingRight() + mHorizontalSpacing;

        int lineWidth = widthUsed;
        int lineHeight = 0;


        int childCount = getChildCount();

        List<View> lineViews = new ArrayList<>();

        for (int i = 0; i < childCount; i++) {

            View child = getChildAt(i);

            if (child.getVisibility() == View.GONE) {
                continue;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            measureChildWithMargins(child, widthMeasureSpec, mHorizontalSpacing * 2, heightMeasureSpec, mVerticalSpacing * 2);

            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if (lineWidth + childWidth + mHorizontalSpacing > sizeWidth) {
                mLineWidths.add(lineWidth);
                lineWidth = widthUsed + childWidth + mHorizontalSpacing;

                mLineHeights.add(lineHeight);
                lineHeight = childHeight;

                mLines.add(lineViews);
                lineViews = new ArrayList<>();
            } else {
                lineWidth += childWidth + mHorizontalSpacing;
                lineHeight = Math.max(lineHeight, childHeight);
            }

            lineViews.add(child);

        }
        //最后一个child的处理
        mLineHeights.add(lineHeight);
        mLineWidths.add(lineWidth);
        mLines.add(lineViews);

        int maxWidth = Collections.max(mLineWidths);
        int totalHeight = getChildHeights();

        //printLineHeights();
        //TODO 处理getMinimumWidth/height的情况

        setMeasuredDimension(
                (modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : Math.min(maxWidth, sizeWidth),
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : Math.min(totalHeight, sizeHeight));

        remeasureChild(widthMeasureSpec);
    }

    private int getChildHeights() {
        int totalHeight = getPaddingTop() + getPaddingBottom() + mVerticalSpacing;
        for (Integer height : mLineHeights) {
            totalHeight += height + mVerticalSpacing;
        }
        return totalHeight;
    }

    private void remeasureChild(int parentWidthSpec) {
        int numLines = mLines.size();
        for (int i = 0; i < numLines; i++) {
            int lineHeight = mLineHeights.get(i);
            List<View> lineViews = mLines.get(i);
            int children = lineViews.size();
            for (int j = 0; j < children; j++) {
                View child = lineViews.get(j);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    if (child.getVisibility() == View.GONE) {
                        continue;
                    }

                    int widthUsed = lp.leftMargin + lp.rightMargin +
                            getPaddingLeft() + getPaddingRight() + 2 * mHorizontalSpacing;
                    child.measure(
                            getChildMeasureSpec(parentWidthSpec, widthUsed, lp.width),
                            MeasureSpec.makeMeasureSpec(lineHeight - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY)
                    );
                }
            }
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        mLineMargins.clear();

        int width = getWidth();
        int height = getHeight();

        float horizontalGravityFactor;
        switch ((mGravity & Gravity.HORIZONTAL_GRAVITY_MASK)) {
            case Gravity.LEFT:
            default:
                horizontalGravityFactor = 0;
                break;
            case Gravity.CENTER_HORIZONTAL:
                horizontalGravityFactor = .5f;
                break;
            case Gravity.RIGHT:
                horizontalGravityFactor = 1;
                break;
        }

        int linesNum = mLineWidths.size();
        for (int i = 0; i < linesNum; i++) {
            int lineWidth = mLineWidths.get(i);
            mLineMargins.add((int) ((width - lineWidth) * horizontalGravityFactor) + getPaddingLeft() + mHorizontalSpacing);
        }

        int verticalGravityMargin;
        int childHeights = getChildHeights();
        switch ((mGravity & Gravity.VERTICAL_GRAVITY_MASK)) {
            case Gravity.TOP:
            default:
                verticalGravityMargin = 0;
                break;
            case Gravity.CENTER_VERTICAL:
                verticalGravityMargin = Math.max((height - childHeights) / 2, 0);
                break;
            case Gravity.BOTTOM:
                verticalGravityMargin = Math.max(height - childHeights, 0);
                break;
        }

        int numLines = mLines.size();

        int left;
        int top = getPaddingTop() + mVerticalSpacing + verticalGravityMargin;

        for (int i = 0; i < numLines; i++) {

            int lineHeight = mLineHeights.get(i);
            List<View> lineViews = mLines.get(i);
            left = mLineMargins.get(i);

            int children = lineViews.size();

            for (int j = 0; j < children; j++) {

                View child = lineViews.get(j);

                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();

                int gravityMargin = 0;

                if (Gravity.isVertical(lp.gravity)) {
                    switch (lp.gravity) {
                        case Gravity.TOP:
                        default:
                            gravityMargin = 0;
                            break;
                        case Gravity.CENTER_VERTICAL:
                        case Gravity.CENTER:
                            gravityMargin = (lineHeight - childHeight - lp.topMargin - lp.bottomMargin) / 2;
                            break;
                        case Gravity.BOTTOM:
                            gravityMargin = lineHeight - childHeight - lp.topMargin - lp.bottomMargin;
                            break;
                    }
                }

                child.layout(left + lp.leftMargin,
                        top + lp.topMargin + gravityMargin,
                        left + lp.leftMargin + childWidth,
                        top + lp.topMargin + gravityMargin + childHeight);

                Log.i(TAG, String.format("child (%d,%d) position: (%d,%d,%d,%d)",
                        i, j, child.getLeft(), child.getTop(), child.getRight(), child.getBottom()));

                left += childWidth + lp.leftMargin + lp.rightMargin + mHorizontalSpacing;

            }

            top += lineHeight + mVerticalSpacing;
        }

    }

    Paint paint = new Paint();

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(3);
        int verticalGravityMargin = 0;
        int top = getPaddingTop() + mVerticalSpacing + verticalGravityMargin;

        int numLines = mLines.size();

        for (int i = 0; i < numLines; i++) {
            int lineHeight = mLineHeights.get(i);
            top += lineHeight + mVerticalSpacing;
            canvas.drawLine(getPaddingLeft(), top - mVerticalSpacing / 2, getWidth() - getPaddingRight(), top - mVerticalSpacing / 2, paint);
        }

    }

    @Override
    public void setWillNotDraw(boolean willNotDraw) {
        super.setWillNotDraw(false);
    }

    private void printLineHeights() {
        for (int i = 0; i < mLineHeights.size(); i++) {
            Log.i(TAG, String.format("line %d height : %d",
                    i, mLineHeights.get(i)));
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p) && p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= isIcs() ? Gravity.START : Gravity.LEFT;
            }

            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.TOP;
            }

            mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return mGravity;
    }

    /**
     * @return <code>true</code> if device is running ICS or grater version of Android.
     */
    private static boolean isIcs() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FlowLayout_Layout);

            try {
                gravity = a.getInt(R.styleable.FlowLayout_Layout_android_layout_gravity, -1);
            } finally {
                a.recycle();
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

    }

}
