/*
    Copyright (C) 2014 Jerzy Chalupski

    Modified by: Garrett Franks 2015

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package com.github.gfranks.fab.menu;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

public class FloatingActionsMenu extends ViewGroup implements View.OnLongClickListener {

    public static final int EXPAND_UP = 0;
    public static final int EXPAND_DOWN = 1;
    public static final int EXPAND_LEFT = 2;
    public static final int EXPAND_RIGHT = 3;

    private static final int DEFAULT_LABEL_HORIZONTAL_PADDING = 25;
    private static final int DEFAULT_LABEL_VERTICAL_PADDING = 15;
    private static final int DEFAULT_LABEL_MIN_WIDTH = 115;

    private static final int DISPLAY_HELP_LABELS_IN_HORIZONTAL_MODE_OFFSET = 60;
    private static final int ANIMATION_DURATION = 300;
    private AnimatorSet mExpandAnimation = new AnimatorSet().setDuration(ANIMATION_DURATION);
    private AnimatorSet mCollapseAnimation = new AnimatorSet().setDuration(ANIMATION_DURATION);
    private static final float COLLAPSED_PLUS_ROTATION = 0f;
    private static final float EXPANDED_PLUS_ROTATION = 90f + 45f;
    private static Interpolator sExpandInterpolator = new DecelerateInterpolator(3f);
    private static Interpolator sCollapseInterpolator = new DecelerateInterpolator(3f);
    private static Interpolator sAlphaExpandInterpolator = new DecelerateInterpolator();
    private int mAddButtonPlusColor;
    private int mAddButtonColorNormal;
    private int mAddButtonColorPressed;
    private int mAddButtonSize;
    private boolean mAddButtonStrokeVisible;
    private int mExpandDirection;
    private int mButtonSpacing;
    private int mLabelsMargin;
    private int mLabelsVerticalOffset;
    private boolean mExpanded;
    private AddFloatingActionButton mAddButton;
    private RotatingDrawable mRotatingDrawable;
    private int mMaxButtonWidth;
    private int mMaxButtonHeight;
    private int mLabelsStyle;
    private int mLabelsTextColor;
    private int mCardBackgroundColor;
    private int mButtonsCount;
    private boolean mCloseOnItemClick;
    private boolean mIsSlidOut;
    private boolean mBounceWhenExpanding;
    private boolean mDisplayHelpLabelsInHorizontalMode;
    private OnFloatingActionsMenuUpdateListener mListener;

    public FloatingActionsMenu(Context context) {
        this(context, null);
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        mButtonSpacing = (int) (getResources().getDimension(R.dimen.fab_actions_spacing) - getResources().getDimension(R.dimen.fab_shadow_radius) - getResources().getDimension(R.dimen.fab_shadow_offset));
        mLabelsMargin = getResources().getDimensionPixelSize(R.dimen.fab_labels_margin);
        mLabelsVerticalOffset = getResources().getDimensionPixelSize(R.dimen.fab_shadow_offset);

        TypedArray attr = context.obtainStyledAttributes(attributeSet, R.styleable.FloatingActionsMenu, 0, 0);
        mAddButtonPlusColor = attr.getColor(R.styleable.FloatingActionsMenu_fab_addButtonPlusIconColor, getColor(android.R.color.white));
        mAddButtonColorNormal = attr.getColor(R.styleable.FloatingActionsMenu_fab_addButtonColorNormal, getColor(android.R.color.holo_blue_dark));
        mAddButtonColorPressed = attr.getColor(R.styleable.FloatingActionsMenu_fab_addButtonColorPressed, getColor(android.R.color.holo_blue_light));
        mAddButtonSize = attr.getInt(R.styleable.FloatingActionsMenu_fab_addButtonSize, FloatingActionButton.SIZE_NORMAL);
        mAddButtonStrokeVisible = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_addButtonStrokeVisible, true);
        mExpandDirection = attr.getInt(R.styleable.FloatingActionsMenu_fab_expandDirection, EXPAND_UP);
        mLabelsStyle = attr.getResourceId(R.styleable.FloatingActionsMenu_fab_labelStyle, 0);
        mLabelsTextColor = attr.getColor(R.styleable.FloatingActionsMenu_fab_labelTextColor, 0);
        mCardBackgroundColor = attr.getColor(R.styleable.FloatingActionsMenu_fab_cardBackgroundColor, getColor(android.R.color.white));
        mCloseOnItemClick = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_closeOnItemClick, false);
        mBounceWhenExpanding = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_bounceWhenExpanding, false);
        mDisplayHelpLabelsInHorizontalMode = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_displayHelpLabelsInHorizontalMode, false);
        attr.recycle();

        if (mBounceWhenExpanding) {
            sExpandInterpolator = new OvershootInterpolator();
        } else {
            sExpandInterpolator = new DecelerateInterpolator(3f);
        }
        createAddButton(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int width = 0;
        int height = 0;

        mMaxButtonWidth = 0;
        mMaxButtonHeight = 0;
        int maxLabelWidth = 0;

        for (int i = 0; i < mButtonsCount; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            switch (mExpandDirection) {
                case EXPAND_UP:
                case EXPAND_DOWN:
                    mMaxButtonWidth = Math.max(mMaxButtonWidth, child.getMeasuredWidth());
                    height += child.getMeasuredHeight();
                    break;
                case EXPAND_LEFT:
                case EXPAND_RIGHT:
                    width += child.getMeasuredWidth();
                    mMaxButtonHeight = Math.max(mMaxButtonHeight, child.getMeasuredHeight());
                    break;
            }

            if (!expandsHorizontally()) {
                CardView cardView = (CardView) child.getTag(R.id.fab_card_label);
                if (cardView != null) {
                    maxLabelWidth = Math.max(maxLabelWidth, cardView.getMeasuredWidth());
                }
            } else {
                if (mDisplayHelpLabelsInHorizontalMode) {
                    CardView cardView = (CardView) child.getTag(R.id.fab_card_label);
                    if (cardView != null) {
                        mMaxButtonHeight = Math.max(mMaxButtonHeight, child.getMeasuredHeight() + cardView.getMeasuredHeight() + DISPLAY_HELP_LABELS_IN_HORIZONTAL_MODE_OFFSET);
                    }
                }
            }
        }

        if (!expandsHorizontally()) {
            width = mMaxButtonWidth + (maxLabelWidth > 0 ? maxLabelWidth + mLabelsMargin : 0);
        } else {
            height = mMaxButtonHeight;
        }

        switch (mExpandDirection) {
            case EXPAND_UP:
            case EXPAND_DOWN:
                height += mButtonSpacing * (getChildCount() - 1);
                height = adjustForOvershoot(height);
                break;
            case EXPAND_LEFT:
            case EXPAND_RIGHT:
                width += mButtonSpacing * (getChildCount() - 1);
                width = adjustForOvershoot(width);
                break;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        switch (mExpandDirection) {
            case EXPAND_UP:
            case EXPAND_DOWN:
                boolean expandUp = mExpandDirection == EXPAND_UP;

                int addButtonY = expandUp ? b - t - mAddButton.getMeasuredHeight() : 0;
                // Ensure mAddButton is centered on the line where the buttons should be
                int addButtonLeft = r - l - mMaxButtonWidth + (mMaxButtonWidth - mAddButton.getMeasuredWidth()) / 2;
                mAddButton.layout(addButtonLeft, addButtonY, addButtonLeft + mAddButton.getMeasuredWidth(), addButtonY + mAddButton.getMeasuredHeight());

                int labelsRight = r - l - mMaxButtonWidth - mLabelsMargin;

                int nextY = expandUp ?
                        addButtonY - mButtonSpacing :
                        addButtonY + mAddButton.getMeasuredHeight() + mButtonSpacing;

                for (int i = mButtonsCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if (child == mAddButton || child.getVisibility() == GONE) continue;

                    int childX = addButtonLeft + (mAddButton.getMeasuredWidth() - child.getMeasuredWidth()) / 2;
                    int childY = expandUp ? nextY - child.getMeasuredHeight() : nextY;
                    child.layout(childX, childY, childX + child.getMeasuredWidth(), childY + child.getMeasuredHeight());

                    float collapsedTranslation = addButtonY - childY;
                    float expandedTranslation = 0f;

                    child.setTranslationY(mExpanded ? expandedTranslation : collapsedTranslation);
                    child.setAlpha(mExpanded ? 1f : 0f);

                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                    params.setAnimationsTarget(child);

                    View label = (View) child.getTag(R.id.fab_card_label);
                    if (label != null) {
                        int labelLeft = labelsRight - label.getMeasuredWidth();
                        int labelTop = childY - mLabelsVerticalOffset + (child.getMeasuredHeight() - label.getMeasuredHeight()) / 2;

                        label.layout(labelLeft, labelTop, labelsRight, labelTop + label.getMeasuredHeight());

                        label.setTranslationY(mExpanded ? expandedTranslation : collapsedTranslation);
                        label.setAlpha(mExpanded ? 1f : 0f);

                        LayoutParams labelParams = (LayoutParams) label.getLayoutParams();
                        labelParams.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                        labelParams.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                        labelParams.setAnimationsTarget(label);
                    }

                    nextY = expandUp ?
                            childY - mButtonSpacing :
                            childY + child.getMeasuredHeight() + mButtonSpacing;
                }
                break;

            case EXPAND_LEFT:
            case EXPAND_RIGHT:
                boolean expandLeft = mExpandDirection == EXPAND_LEFT;

                int addButtonX = expandLeft ? r - l - mAddButton.getMeasuredWidth() : 0;
                // Ensure mAddButton is centered on the line where the buttons should be
                int addButtonTop = b - t - mMaxButtonHeight + (mMaxButtonHeight - mAddButton.getMeasuredHeight()) / 2;
                if (mDisplayHelpLabelsInHorizontalMode) {
                    addButtonTop += DISPLAY_HELP_LABELS_IN_HORIZONTAL_MODE_OFFSET;
                }
                mAddButton.layout(addButtonX, addButtonTop, addButtonX + mAddButton.getMeasuredWidth(), addButtonTop + mAddButton.getMeasuredHeight());

                int nextX = expandLeft ?
                        addButtonX - mButtonSpacing :
                        addButtonX + mAddButton.getMeasuredWidth() + mButtonSpacing;

                for (int i = mButtonsCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if (child == mAddButton || child.getVisibility() == GONE) continue;

                    int childX = expandLeft ? nextX - child.getMeasuredWidth() : nextX;
                    int childY = addButtonTop + (mAddButton.getMeasuredHeight() - child.getMeasuredHeight()) / 2;
                    child.layout(childX, childY, childX + child.getMeasuredWidth(), childY + child.getMeasuredHeight());

                    float collapsedTranslation = addButtonX - childX;
                    float expandedTranslation = 0f;

                    child.setTranslationX(mExpanded ? expandedTranslation : collapsedTranslation);
                    child.setAlpha(mExpanded ? 1f : 0f);

                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                    params.setAnimationsTarget(child);

                    if (mDisplayHelpLabelsInHorizontalMode) {
                        View label = (View) child.getTag(R.id.fab_card_label);
                        if (label != null) {
                            int labelLeft = childX - (label.getMeasuredWidth() / 4);
                            int labelTop = addButtonTop - (label.getMeasuredWidth() / 4);
                            label.layout(labelLeft, labelTop, labelLeft + label.getMeasuredWidth(), labelTop + label.getMeasuredHeight());
                            label.setVisibility(View.GONE);
                            LayoutParams labelParams = (LayoutParams) label.getLayoutParams();
                            labelParams.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                            labelParams.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                            labelParams.setAnimationsTarget(label);
                        }
                    }

                    nextX = expandLeft ?
                            childX - mButtonSpacing :
                            childX + child.getMeasuredWidth() + mButtonSpacing;
                }

                break;
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(super.generateDefaultLayoutParams());
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(super.generateLayoutParams(attrs));
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(super.generateLayoutParams(p));
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        bringChildToFront(mAddButton);
        mButtonsCount = getChildCount();

        if (mLabelsStyle != 0 || mLabelsTextColor != 0) {
            createLabels();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mExpanded = mExpanded;
        savedState.mIsSlidOut = mIsSlidOut;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mExpanded = savedState.mExpanded;
            mIsSlidOut = savedState.mIsSlidOut;

            if (mRotatingDrawable != null) {
                mRotatingDrawable.setRotation(mExpanded ? EXPANDED_PLUS_ROTATION : COLLAPSED_PLUS_ROTATION);
            }

            if (mIsSlidOut) {
                setVisibility(View.GONE);
                slideOutMenu();
            } else {
                setVisibility(View.VISIBLE);
                slideInMenu();
            }

            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    public void setOnFloatingActionsMenuUpdateListener(OnFloatingActionsMenuUpdateListener listener) {
        mListener = listener;
    }

    public void addButton(FloatingActionButton button) {
        addView(button, mButtonsCount - 1);
        mButtonsCount++;

        if (mLabelsStyle != 0 || mLabelsTextColor != 0) {
            createLabels();
        }
    }

    public void removeButton(FloatingActionButton button) {
        removeView(button.getCardView());
        removeView(button);
        mButtonsCount--;
    }

    public void collapse() {
        if (mExpanded) {
            mExpanded = false;
            mCollapseAnimation.start();
            mExpandAnimation.cancel();

            if (mListener != null) {
                mListener.onMenuCollapsed();
            }
        }
    }

    public void toggle() {
        if (mExpanded) {
            collapse();
        } else {
            expand();
        }
    }

    public void expand() {
        if (!mExpanded) {
            mExpanded = true;
            mCollapseAnimation.cancel();
            mExpandAnimation.start();

            if (mListener != null) {
                mListener.onMenuExpanded();
            }
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void slideInMenu() {
        if (!mIsSlidOut) {
            return;
        }
        Animation slide_in = AnimationUtils.loadAnimation(getContext(), R.anim.fab_slide_in);
        slide_in.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsSlidOut = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        setAnimation(slide_in);
    }

    public void slideOutMenu() {
        if (mIsSlidOut) {
            return;
        }
        if (mExpanded) {
            collapse();
        }

        Animation slide_out = AnimationUtils.loadAnimation(getContext(), R.anim.fab_slide_out);
        slide_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsSlidOut = true;
                setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        setAnimation(slide_out);
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof FloatingActionButton) {
            displayFabLabel((FloatingActionButton) v);
            return true;
        }
        return false;
    }

    private int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    private int adjustForOvershoot(int dimension) {
        return dimension * 12 / 10;
    }

    private boolean expandsHorizontally() {
        return mExpandDirection == EXPAND_LEFT || mExpandDirection == EXPAND_RIGHT;
    }

    private void createAddButton(Context context) {
        mAddButton = new AddFloatingActionButton(context) {
            @Override
            void updateBackground() {
                mPlusColor = mAddButtonPlusColor;
                mColorNormal = mAddButtonColorNormal;
                mColorPressed = mAddButtonColorPressed;
                mStrokeVisible = mAddButtonStrokeVisible;
                super.updateBackground();
            }

            @Override
            Drawable getIconDrawable() {
                final RotatingDrawable rotatingDrawable = new RotatingDrawable(super.getIconDrawable());
                mRotatingDrawable = rotatingDrawable;

                Interpolator interpolator;
                if (mBounceWhenExpanding) {
                    interpolator = new OvershootInterpolator();
                } else {
                    interpolator = new DecelerateInterpolator(3f);
                }

                final ObjectAnimator collapseAnimator = ObjectAnimator.ofFloat(rotatingDrawable, "rotation", EXPANDED_PLUS_ROTATION, COLLAPSED_PLUS_ROTATION);
                final ObjectAnimator expandAnimator = ObjectAnimator.ofFloat(rotatingDrawable, "rotation", COLLAPSED_PLUS_ROTATION, EXPANDED_PLUS_ROTATION);

                collapseAnimator.setInterpolator(interpolator);
                expandAnimator.setInterpolator(interpolator);

                mExpandAnimation.play(expandAnimator);
                mCollapseAnimation.play(collapseAnimator);

                return rotatingDrawable;
            }
        };

        mAddButton.setId(R.id.fab_expand_menu_button);
        mAddButton.setSize(mAddButtonSize);
        mAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        addView(mAddButton, super.generateDefaultLayoutParams());
    }

    private void createLabels() {
        Context labelContext = null;
        if (mLabelsStyle != 0) {
            labelContext = new ContextThemeWrapper(getContext(), mLabelsStyle);
        }

        for (int i = 0; i < mButtonsCount; i++) {
            FloatingActionButton button = (FloatingActionButton) getChildAt(i);
            String title = button.getTitle();

            if (button == mAddButton || button.getTag(R.id.fab_card_label) != null)
                continue;

            button.setCloseMenuOnClick(mCloseOnItemClick);
            button.setFloatingActionsMenu(this);
            if (title == null)
                continue;

            CardView cardView = new CardView(getContext());
            cardView.setCardBackgroundColor(mCardBackgroundColor);
            TextView label;
            if (labelContext != null) {
                label = new TextView(labelContext);
            } else {
                label = new TextView(getContext());
                label.setPadding(DEFAULT_LABEL_HORIZONTAL_PADDING,
                        DEFAULT_LABEL_VERTICAL_PADDING,
                        DEFAULT_LABEL_HORIZONTAL_PADDING,
                        DEFAULT_LABEL_VERTICAL_PADDING);
                label.setMinimumWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LABEL_MIN_WIDTH,
                        getResources().getDisplayMetrics()));
                label.setGravity(Gravity.CENTER);
            }
            if (mLabelsTextColor != 0) {
                label.setTextColor(mLabelsTextColor);
            }
            label.setText(button.getTitle());
            cardView.addView(label);
            addView(cardView);

            button.setTag(R.id.fab_card_label, cardView);

            if (expandsHorizontally() && mDisplayHelpLabelsInHorizontalMode) {
                button.setOnLongClickListener(this);
            }
        }
    }

    private void displayFabLabel(FloatingActionButton button) {
        if (!expandsHorizontally() || !mDisplayHelpLabelsInHorizontalMode)
            return;

        final View view = (View) button.getTag(R.id.fab_card_label);
        AlphaAnimation fadeAnimation = new AlphaAnimation(0f, 1f);
        fadeAnimation.setDuration(500);
        fadeAnimation.setInterpolator(new LinearInterpolator());
        fadeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AlphaAnimation fadeAnimation = new AlphaAnimation(1f, 0f);
                fadeAnimation.setStartOffset(3000);
                fadeAnimation.setDuration(500);
                fadeAnimation.setInterpolator(new LinearInterpolator());
                fadeAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                view.startAnimation(fadeAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeAnimation);
    }

    public interface OnFloatingActionsMenuUpdateListener {
        void onMenuExpanded();

        void onMenuCollapsed();
    }

    private static class RotatingDrawable extends LayerDrawable {
        private float mRotation;

        public RotatingDrawable(Drawable drawable) {
            super(new Drawable[]{drawable});
        }

        @SuppressWarnings("UnusedDeclaration")
        public float getRotation() {
            return mRotation;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setRotation(float rotation) {
            mRotation = rotation;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.rotate(mRotation, getBounds().centerX(), getBounds().centerY());
            super.draw(canvas);
            canvas.restore();
        }
    }

    public static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public boolean mExpanded;
        public boolean mIsSlidOut;

        public SavedState(Parcelable parcel) {
            super(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            mExpanded = in.readInt() == 1;
            mIsSlidOut = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mExpanded ? 1 : 0);
            out.writeInt(mIsSlidOut ? 1 : 0);
        }
    }

    private class LayoutParams extends ViewGroup.LayoutParams {

        private ObjectAnimator mExpandDir = new ObjectAnimator();
        private ObjectAnimator mExpandAlpha = new ObjectAnimator();
        private ObjectAnimator mCollapseDir = new ObjectAnimator();
        private ObjectAnimator mCollapseAlpha = new ObjectAnimator();
        private boolean animationsSetToPlay;

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);

            mExpandDir.setInterpolator(sExpandInterpolator);
            mExpandAlpha.setInterpolator(sAlphaExpandInterpolator);
            mCollapseDir.setInterpolator(sCollapseInterpolator);
            mCollapseAlpha.setInterpolator(sCollapseInterpolator);

            mCollapseAlpha.setProperty(View.ALPHA);
            mCollapseAlpha.setFloatValues(1f, 0f);

            mExpandAlpha.setProperty(View.ALPHA);
            mExpandAlpha.setFloatValues(0f, 1f);

            switch (mExpandDirection) {
                case EXPAND_UP:
                case EXPAND_DOWN:
                    mCollapseDir.setProperty(View.TRANSLATION_Y);
                    mExpandDir.setProperty(View.TRANSLATION_Y);
                    break;
                case EXPAND_LEFT:
                case EXPAND_RIGHT:
                    mCollapseDir.setProperty(View.TRANSLATION_X);
                    mExpandDir.setProperty(View.TRANSLATION_X);
                    break;
            }
        }

        public void setAnimationsTarget(View view) {
            mCollapseAlpha.setTarget(view);
            mCollapseDir.setTarget(view);
            mExpandAlpha.setTarget(view);
            mExpandDir.setTarget(view);

            // Now that the animations have targets, set them to be played
            if (!animationsSetToPlay) {
                mCollapseAnimation.play(mCollapseAlpha);
                mCollapseAnimation.play(mCollapseDir);
                mExpandAnimation.play(mExpandAlpha);
                mExpandAnimation.play(mExpandDir);
                animationsSetToPlay = true;
            }
        }
    }
}