/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * Copyright (C) 2026 AlphaDroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.cutoutprogress.ring;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.OvershootInterpolator;

import com.android.systemui.cutoutprogress.CutoutProgressSettings;

import java.util.Objects;

public final class CutoutRingView extends View {

    private static final long BURN_IN_HIDE_MS = 10_000L;
    private static final long CHARGING_PULSE_MS = 900L;
    private static final long MIN_VIS_MS = 500L;
    private static final float RING_GAP = 1.155f;
    private static final int BG_RING_OPACITY = 30;

    private static final int[] RAINBOW = {
            0xFFFF0000, // Red
            0xFFFF7F00, // Orange
            0xFFFFFF00, // Yellow
            //0xFF00FF00, // Green
            //0xFFFFFF00, // Yellow
            0xFFFF7F00, // Orange
            //0xFF00FFFF, // Cyan
            //0xFF0000FF, // Blue
            //0xFF8B00FF, // Purple/Violet
            0xFFFF0000  // Red (loops back for the SweepGradient)
    };

    private final float mDp;

    private final Path mCutoutPath = new Path();
    private final Path mScaledPath = new Path();

    private final Matrix mScaleMatrix = new Matrix();
    private final Matrix mTempMatrix = new Matrix();

    private final RectF mPathBounds = new RectF();
    private final RectF mArcBounds = new RectF();
    private final RectF mCurrentTouchBounds = new RectF();

    private boolean mHasCutout = false;

    private boolean mIsIslandExpanded = false;
    private float mIslandExpandFraction = 0f;
    private ValueAnimator mIslandAnimator = null;

    private String mChargeMode = "";
    private String mChargeTime = "";
    private String mChargeStatsLine1 = "";
    private String mChargeStatsLine2 = "";

    private final OverlayAnimationHelper mAnim;
    private RingViewRenderer mRenderer;

    private final Paint mRingPaint = makePaint();
    private final Paint mShinePaint = makePaint();
    private final Paint mErrorPaint = makePaint();
    private final Paint mAnimPaint = makePaint();
    private final Paint mBgPaint = makePaint();
    private final Paint mChargingPaint = makePaint();
    private final Paint mIslandBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private SweepGradient mRainbowShader = null;
    private float mRainbowCx = Float.NaN;
    private float mRainbowCy = Float.NaN;

    private int mProgress = 0;
    private String mFilenameHint = null;

    private long mDownloadStartMs = 0L;
    private long mLastProgressMs = 0L;
    private Runnable mPendingFinish = null;
    private final Runnable mAutoCollapse = this::collapseIsland;
    private final Runnable mBurnInHide = this::invalidate;

    private boolean mIsCharging = false;
    private boolean mChargingPulseEnabled = true;
    private int mBatteryPct = 0;
    private float mChargingPulsePhase = 0f;
    private float mChargingDisplayPct = 0f;

    private ValueAnimator mChargingPulseAnim = null;
    private ValueAnimator mChargingLevelAnim = null;

    private boolean sCfgDownloadEnabled;
    private boolean sCfgBgRing;
    private boolean sCfgChargingRing;
    private boolean sCfgChargingPulse;
    private boolean sCfgIslandCompact;

    private int sCfgRingColorMode;
    private int sCfgRingColor;
    private int sCfgIslandPosition;
    private int sCfgIslandTimeoutMs;
    private int sCfgRingOpacityPct;
    private int sCfgDialogBgOpacityPct;
    private float sCfgStrokeDp;

    private final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsListener = info -> {
        info.setTouchableInsets(ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
        info.touchableRegion.setEmpty();

        boolean isActive = mProgress > 0 || mIsCharging || mIslandExpandFraction > 0;

        if (mHasCutout && isActive) {
            Rect touchRect = new Rect();
            Rect ringRect = new Rect();

            mCurrentTouchBounds.roundOut(touchRect);
            mArcBounds.roundOut(ringRect);

            touchRect.union(ringRect);
            touchRect.inset(-20, -20);
            info.touchableRegion.set(touchRect);
        }
    };

    public CutoutRingView(Context ctx) {
        super(ctx);
        mDp = ctx.getResources().getDisplayMetrics().density;
        mAnim = new OverlayAnimationHelper(this);
        mRenderer = new CircleRingRenderer();

        mIslandBgPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(Color.WHITE);

        setClickable(true);
        setFocusable(true);
        setOnLongClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.android.settings",
                        "com.alpha.settings.trampoline.CutoutProgressSettingsActivity"
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intent);
                collapseIsland();
            } catch (Exception e) {
                Log.e("CutoutRingView", "Failed to launch Settings", e);
            }
            return true;
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnComputeInternalInsetsListener(mInsetsListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnComputeInternalInsetsListener(mInsetsListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && (mProgress > 0 || mIsCharging)) {
            boolean hitIsland = mCurrentTouchBounds.contains(event.getX(), event.getY());
            boolean hitRing = mArcBounds.contains(event.getX(), event.getY());

            if (hitIsland || hitRing) {
                if (mIsIslandExpanded) {
                    collapseIsland();
                } else {
                    expandIsland();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private void animateIsland(float target) {
        if (mIslandAnimator != null) {
            mIslandAnimator.cancel();
        }

        mIslandAnimator = ValueAnimator.ofFloat(mIslandExpandFraction, target);
        mIslandAnimator.setDuration(400);
        mIslandAnimator.setInterpolator(new OvershootInterpolator(1.05f));
        mIslandAnimator.addUpdateListener(a -> {
            mIslandExpandFraction = (float) a.getAnimatedValue();
            invalidate();
        });
        mIslandAnimator.start();
    }

    private void expandIsland() {
        mIsIslandExpanded = true;
        animateIsland(1f);

        removeCallbacks(mAutoCollapse);
        postDelayed(mAutoCollapse, sCfgIslandTimeoutMs);
    }

    private void collapseIsland() {
        mIsIslandExpanded = false;
        animateIsland(0f);

        removeCallbacks(mAutoCollapse);
    }

    public void applySettings(CutoutProgressSettings s) {
        boolean oldDownloadEnabled = sCfgDownloadEnabled;
        boolean oldChargingRing = sCfgChargingRing;

        sCfgDownloadEnabled = s.isDownloadRingEnabled();
        sCfgRingColorMode = s.getRingColorMode();
        sCfgRingColor = s.getRingColor();
        sCfgStrokeDp = s.getStrokeWidthDp();
        sCfgBgRing = s.isBgRingEnabled();
        sCfgRingOpacityPct = s.getRingOpacityPercent();
        sCfgDialogBgOpacityPct = s.getDialogBgOpacityPercent();

        sCfgChargingRing = s.isChargingRingEnabled();
        sCfgChargingPulse = s.isChargingPulseEnabled();

        sCfgIslandCompact = s.isIslandCompactMode();
        sCfgIslandPosition = s.getIslandPosition();
        sCfgIslandTimeoutMs = s.getIslandTimeoutMs();

        if (sCfgChargingRing && !oldChargingRing && mIsCharging) {
            mChargingDisplayPct = 0f;
            expandIsland();
            animateChargingLevelTo(mBatteryPct);
        } else if (!sCfgChargingRing && oldChargingRing && mIsCharging) {
            stopChargingAnimations();
            if (mProgress == 0 || !sCfgDownloadEnabled) {
                collapseIsland();
            }
        }

        mChargingPulseEnabled = sCfgChargingPulse;
        if (!mChargingPulseEnabled && mChargingPulseAnim != null) {
            stopChargingPulse();
        } else if (mChargingPulseEnabled && sCfgChargingRing && mIsCharging
                && mBatteryPct < 100 && mChargingPulseAnim == null) {
            startChargingPulse();
        }

        if (sCfgDownloadEnabled && !oldDownloadEnabled && mProgress > 0 && mProgress < 100) {
            expandIsland();
        } else if (!sCfgDownloadEnabled && oldDownloadEnabled && mProgress > 0) {
            mAnim.cancelFinish();
            if (!mIsCharging || !sCfgChargingRing) {
                collapseIsland();
            }
        }

        if (sCfgRingColorMode != CutoutProgressSettings.RING_COLOR_MODE_RAINBOW) {
            mRainbowShader = null;
            mRainbowCx = Float.NaN;
        }

        refreshPaints();
        recalcScaledPath();
        invalidate();
    }

    private void refreshPaints() {
        float strokeWidth = sCfgStrokeDp * mDp;
        int baseColor = sCfgRingColorMode == CutoutProgressSettings.RING_COLOR_MODE_CUSTOM
                ? sCfgRingColor : resolveAccentColor();

        int ringAlpha = sCfgRingOpacityPct * 255 / 100;

        applyStroke(mRingPaint, baseColor, strokeWidth, ringAlpha);
        applyStroke(mShinePaint, Color.WHITE, strokeWidth * 1.2f, 255);
        applyStroke(mErrorPaint, 0xFFF44336, strokeWidth * 1.5f, 255);
        applyStroke(mBgPaint, 0xFF808080, strokeWidth, BG_RING_OPACITY * 255 / 100);
        applyStroke(mChargingPaint, baseColor, strokeWidth, ringAlpha);
    }

    private int resolveAccentColor() {
        TypedValue tv = new TypedValue();
        boolean resolved = getContext().getTheme()
                .resolveAttribute(android.R.attr.colorAccent, tv, true);
        int baseColor = resolved ? tv.data : 0xFF2196F3;

        boolean isNightMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isNightMode) {
            return brighten(baseColor, 0.50f);
        }
        return baseColor;
    }

    public void setChargingState(boolean charging, int batteryPct, String mode,
                                 String time, String stats1, String stats2) {
        mChargeMode = mode != null ? mode : "";
        mChargeTime = time != null ? time : "";
        mChargeStatsLine1 = stats1 != null ? stats1 : "";
        mChargeStatsLine2 = stats2 != null ? stats2 : "";

        boolean wasCharging = mIsCharging;
        mIsCharging = charging;
        mBatteryPct = batteryPct;

        if (!charging) {
            stopChargingAnimations();
            if (wasCharging && (mProgress == 0 || !sCfgDownloadEnabled)) {
                collapseIsland();
            }
            invalidate();
            return;
        }

        if (!sCfgChargingRing) {
            return;
        }

        if (!wasCharging) {
            mChargingDisplayPct = 0f;
            expandIsland();
        }

        animateChargingLevelTo(batteryPct);

        if (batteryPct >= 100) {
            stopChargingPulse();
        } else if (mChargingPulseEnabled && sCfgChargingPulse && mChargingPulseAnim == null) {
            startChargingPulse();
        }

        invalidate();
    }

    public void setChargingPulseEnabled(boolean enabled) {
        mChargingPulseEnabled = enabled;

        if (!enabled) {
            stopChargingPulse();
        } else if (mIsCharging && mBatteryPct < 100 && mChargingPulseAnim == null) {
            startChargingPulse();
        }
    }

    private void animateChargingLevelTo(int targetPct) {
        if (mChargingLevelAnim != null) {
            mChargingLevelAnim.cancel();
        }

        float target = Math.max(0f, Math.min(100f, targetPct));
        mChargingLevelAnim = ValueAnimator.ofFloat(mChargingDisplayPct, target);

        long duration = (long) (Math.abs(target - mChargingDisplayPct) * 12f);
        mChargingLevelAnim.setDuration(Math.max(200L, Math.min(duration, 1200L)));
        mChargingLevelAnim.addUpdateListener(a -> {
            mChargingDisplayPct = (float) a.getAnimatedValue();
            invalidate();
        });

        mChargingLevelAnim.start();
    }

    private void startChargingPulse() {
        mChargingPulseAnim = ValueAnimator.ofFloat(0f, 1f);
        mChargingPulseAnim.setDuration(CHARGING_PULSE_MS);
        mChargingPulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        mChargingPulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        mChargingPulseAnim.addUpdateListener(a -> {
            mChargingPulsePhase = (float) a.getAnimatedValue();
            invalidate();
        });
        mChargingPulseAnim.start();
    }

    private void stopChargingPulse() {
        if (mChargingPulseAnim != null) {
            mChargingPulseAnim.cancel();
        }
        mChargingPulseAnim = null;
        mChargingPulsePhase = 0f;
    }

    private void stopChargingAnimations() {
        stopChargingPulse();

        if (mChargingLevelAnim != null) {
            mChargingLevelAnim.cancel();
        }
        mChargingLevelAnim = null;
        mChargingDisplayPct = 0f;
    }

    public void setProgress(int value) {
        int pct = Math.max(0, Math.min(100, value));
        if (mProgress == pct) {
            return;
        }

        int prev = mProgress;
        mProgress = pct;
        mLastProgressMs = System.currentTimeMillis();

        if (!sCfgDownloadEnabled) {
            return;
        }

        removeCallbacks(mBurnInHide);
        if (pct > 0 && pct < 100) {
            postDelayed(mBurnInHide, BURN_IN_HIDE_MS);
        } else if (!mIsCharging || !sCfgChargingRing) {
            collapseIsland();
        }

        if (prev == 0 && pct > 0) {
            mDownloadStartMs = System.currentTimeMillis();
            cancelPendingFinish();
            expandIsland();
        }

        if (pct == 100 && !mAnim.isFinishAnimating) {
            long timeActive = System.currentTimeMillis() - mDownloadStartMs;
            long remaining = MIN_VIS_MS - timeActive;

            if (remaining > 0 && mDownloadStartMs > 0) {
                mPendingFinish = () -> {
                    mPendingFinish = null;
                    mAnim.startFinish("pop", 500, 500, true, () -> setProgress(0));
                };
                postDelayed(mPendingFinish, remaining);
            } else {
                mAnim.startFinish("pop", 500, 500, true, () -> setProgress(0));
            }
        } else if (pct > 0 && pct < 100 && mAnim.isFinishAnimating) {
            mAnim.cancelFinish();
        } else if (pct == 0) {
            mDownloadStartMs = 0L;
            cancelPendingFinish();
        }

        invalidate();
    }

    public void setFilenameHint(String hint) {
        if (!Objects.equals(mFilenameHint, hint)) {
            mFilenameHint = hint;
            invalidate();
        }
    }

    public void showError() {
        if (sCfgDownloadEnabled) {
            mAnim.startError(() -> setProgress(0));
        }
    }

    private void cancelPendingFinish() {
        if (mPendingFinish != null) {
            removeCallbacks(mPendingFinish);
            mPendingFinish = null;
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mCutoutPath.reset();
        mHasCutout = false;

        DisplayCutout cutout = insets.getDisplayCutout();
        if (cutout != null) {
            Path nativePath = null;
            try {
                nativePath = cutout.getCutoutPath();
            } catch (NoSuchMethodError ignored) {}

            if (nativePath != null && !nativePath.isEmpty()) {
                mCutoutPath.set(nativePath);
                mHasCutout = true;
            } else if (!cutout.getBoundingRects().isEmpty()) {
                Rect r = cutout.getBoundingRects().get(0);
                mCutoutPath.addCircle(r.exactCenterX(), r.exactCenterY(),
                        Math.min(r.width(), r.height()) / 2f, Path.Direction.CW);
                mHasCutout = true;
            }
        }

        if (!mHasCutout) {
            float cx = getResources().getDisplayMetrics().widthPixels / 2f;
            mCutoutPath.addCircle(cx, 30f * mDp, 15f * mDp, Path.Direction.CW);
            mHasCutout = true;
        }

        mRainbowShader = null;
        mRainbowCx = Float.NaN;

        recalcScaledPath();
        invalidate();
        return super.onApplyWindowInsets(insets);
    }

    private void recalcScaledPath() {
        if (!mHasCutout) {
            return;
        }

        mCutoutPath.computeBounds(mPathBounds, true);

        boolean isCapsule = mPathBounds.width() > mPathBounds.height() * 2.5f;
        if (isCapsule) {
            mRenderer = new CapsuleRingRenderer();
        } else {
            mRenderer = new CircleRingRenderer();
            float screenWidth = getResources().getDisplayMetrics().widthPixels;
            float centerX = mPathBounds.centerX();

            float startAngle;
            if (centerX < screenWidth / 3f) {
                startAngle = 180f;
            } else if (centerX > screenWidth * 2f / 3f) {
                startAngle = 0f;
            } else {
                startAngle = -90f;
            }
            ((CircleRingRenderer) mRenderer).setStartAngle(startAngle);
        }

        mScaleMatrix.setScale(RING_GAP, RING_GAP, mPathBounds.centerX(), mPathBounds.centerY());
        mScaledPath.reset();
        mCutoutPath.transform(mScaleMatrix, mScaledPath);
    }

    private float measureTextWidth(String text, float sp, boolean bold) {
        if (TextUtils.isEmpty(text)) return 0f;
        mTextPaint.setTextSize(sp * mDp);
        mTextPaint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return mTextPaint.measureText(text);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mHasCutout) {
            return;
        }

        mScaledPath.computeBounds(mArcBounds, true);
        float centerX = mArcBounds.centerX();
        float centerY = mArcBounds.centerY();

        int activeColor = sCfgRingColorMode == CutoutProgressSettings.RING_COLOR_MODE_RAINBOW
                ? RAINBOW[0] : mRingPaint.getColor();

        setupIslandBounds();

        if (mAnim.isErrorAnimating) {
            mRenderer.updateBounds(mArcBounds);
            mErrorPaint.setAlpha((int) (mAnim.errorAlpha * 255));
            mRenderer.drawFullRing(canvas, mErrorPaint);
            return;
        }

        int effectivePct = mAnim.isGeometryPreviewActive() ? 100
                : mAnim.isDynamicPreviewActive() ? mAnim.previewProgress : mProgress;

        boolean isBurnedOut = effectivePct > 0 && effectivePct < 100 && mLastProgressMs > 0
                && System.currentTimeMillis() - mLastProgressMs >= BURN_IN_HIDE_MS;

        boolean showCharging = mIsCharging && sCfgChargingRing
                && !mAnim.isFinishAnimating && !mAnim.isErrorAnimating && effectivePct == 0;

        boolean isAnimating = mAnim.isFinishAnimating || mAnim.isGeometryPreviewActive()
                || mAnim.isDynamicPreviewActive();
        boolean isActive = effectivePct > 0 && effectivePct < 100 && !isBurnedOut;

        if (!(isAnimating || isActive || mPendingFinish != null
                || mIslandExpandFraction > 0 || showCharging)) {
            return;
        }

        if (mIslandExpandFraction > 0.01f) {
            drawIsland(canvas, activeColor, effectivePct);
        }

        float ringFade = 1f - (mIslandExpandFraction * 2f);
        if (ringFade > 0.01f) {
            mRenderer.updateBounds(mArcBounds);

            if (showCharging) {
                drawChargingRing(canvas, ringFade);
                return;
            }

            if (mAnim.displayScale != 1f) {
                canvas.save();
                canvas.scale(mAnim.displayScale, mAnim.displayScale, centerX, centerY);
            }

            mAnimPaint.set(mRingPaint);
            mAnimPaint.setColor(activeColor);

            int targetAlpha = (int) ((sCfgRingOpacityPct * 2.55f) * mAnim.displayAlpha
                    * mAnim.completionPulseAlpha * ringFade);
            mAnimPaint.setAlpha(targetAlpha);

            if (sCfgRingColorMode == CutoutProgressSettings.RING_COLOR_MODE_RAINBOW) {
                if (mRainbowShader == null) {
                    mRainbowShader = new SweepGradient(centerX, centerY, RAINBOW, null);
                }
                mTempMatrix.setRotate(-90f, centerX, centerY);
                mRainbowShader.setLocalMatrix(mTempMatrix);
                mAnimPaint.setShader(mRainbowShader);
            } else {
                mAnimPaint.setShader(null);
            }

            if (mAnim.successColorBlend > 0f) {
                int blended = blendColors(activeColor, Color.WHITE, mAnim.successColorBlend);
                mAnimPaint.setColor(blended);
                mAnimPaint.setShader(null);
            }

            if (sCfgBgRing && !mAnim.isFinishAnimating) {
                int bgAlpha = (int) (BG_RING_OPACITY * 2.55f * mAnim.displayAlpha * ringFade);
                mBgPaint.setAlpha(bgAlpha);
                mRenderer.drawFullRing(canvas, mBgPaint);
            }

            if (mAnim.isFinishAnimating) {
                mRenderer.drawFullRing(canvas, mAnimPaint);
            } else {
                mRenderer.drawProgress(canvas, effectivePct / 100f, true, mAnimPaint);
            }

            if (mAnim.displayScale != 1f) {
                canvas.restore();
            }
        }
    }

    private void drawChargingRing(Canvas canvas, float ringFade) {
        if (sCfgBgRing) {
            mBgPaint.setAlpha((int) (BG_RING_OPACITY * 2.55f * ringFade));
            mRenderer.drawFullRing(canvas, mBgPaint);
        }

        mChargingPaint.setAlpha((int) (sCfgRingOpacityPct * 2.55f * ringFade));
        mChargingPaint.setColor(chargingColor(mChargingDisplayPct));

        float fraction = mChargingDisplayPct / 100f;
        float pulseFactor = mChargingPulseEnabled && mChargingPulseAnim != null
                ? mChargingPulsePhase * fraction : fraction;
        float sweep = Math.min(pulseFactor, 1f) * 180f;

        canvas.drawArc(mArcBounds, 90f - sweep, sweep, false, mChargingPaint);
        canvas.drawArc(mArcBounds, 90f, sweep, false, mChargingPaint);
    }

    private void setupIslandBounds() {
        float height, expandWidth, currentLeft, currentRight, currentTop;

        if (sCfgIslandCompact) {
            float padTop = 2f * mDp;
            float padBottom = 1f * mDp;
            float padAnchor = 1f * mDp;

            float desiredWidth = computeCompactDesiredWidth();
            float maxLeftWidth = sCfgIslandPosition == 1 ? desiredWidth : 220f * mDp * 0.8f;
            float maxRightWidth = sCfgIslandPosition == 2 ? desiredWidth : 220f * mDp * 0.8f;
            float maxCenterWidth = sCfgIslandPosition == 0 ? desiredWidth : 340f * mDp * 0.8f;

            float expandedHeight =
                    Math.max(mArcBounds.height() + padTop + padBottom, 32f * mDp * 0.8f);
            height = mArcBounds.height()
                    + (expandedHeight - mArcBounds.height()) * mIslandExpandFraction;

            float verticalOffset = (padTop - padBottom) / 2f * mIslandExpandFraction;
            currentTop = mArcBounds.centerY() - (height / 2f) - verticalOffset;

            if (sCfgIslandPosition == 1) {
                currentRight = mArcBounds.right + (padAnchor * mIslandExpandFraction);
                currentLeft = currentRight - (mArcBounds.width()
                        + (maxLeftWidth - mArcBounds.width()) * mIslandExpandFraction);
            } else if (sCfgIslandPosition == 2) {
                currentLeft = mArcBounds.left - (padAnchor * mIslandExpandFraction);
                currentRight = currentLeft + (mArcBounds.width()
                        + (maxRightWidth - mArcBounds.width()) * mIslandExpandFraction);
            } else {
                float currentWidth = mArcBounds.width()
                        + (maxCenterWidth - mArcBounds.width()) * mIslandExpandFraction;
                currentLeft = mArcBounds.centerX() - currentWidth / 2f;
                currentRight = mArcBounds.centerX() + currentWidth / 2f;
            }
        } else if (mIsCharging) {
            float targetHeight = 84f * mDp;
            float targetTop = mArcBounds.bottom + 2f * mDp;

            height = mArcBounds.height()
                    + (targetHeight - mArcBounds.height()) * mIslandExpandFraction;
            expandWidth = mArcBounds.width()
                    + (320f * mDp - mArcBounds.width()) * mIslandExpandFraction;

            float startTop = mArcBounds.centerY() - (mArcBounds.height() / 2f);
            currentTop = startTop + (targetTop - startTop) * mIslandExpandFraction;

            currentLeft = mArcBounds.centerX() - expandWidth / 2f;
            currentRight = mArcBounds.centerX() + expandWidth / 2f;
        } else {
            String pctStr = mProgress + "%";
            mTextPaint.setTextSize(13f * mDp);
            mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            String label = mFilenameHint != null ? mFilenameHint : "Downloading...";
            float maxTextWidth = mTextPaint.measureText(pctStr + " • " + label) + (24f * mDp);

            float targetHeight = Math.max(mArcBounds.height() + 3f * mDp, 32f * mDp * 1.3f);
            height = mArcBounds.height()
                    + (targetHeight - mArcBounds.height()) * mIslandExpandFraction;
            expandWidth = mArcBounds.width()
                    + (Math.max(maxTextWidth, mArcBounds.width() + 24f * mDp)
                    - mArcBounds.width()) * mIslandExpandFraction;

            float centerOffset =
                    mArcBounds.top + (mArcBounds.height() / 2f) - (height / 2f);
            currentTop = centerOffset
                    + ((mArcBounds.bottom + 2f * mDp - centerOffset) * mIslandExpandFraction);

            currentLeft = mArcBounds.centerX() - expandWidth / 2f;
            currentRight = mArcBounds.centerX() + expandWidth / 2f;
        }

        mCurrentTouchBounds.set(currentLeft, currentTop, currentRight, currentTop + height);
    }

    private float computeCompactDesiredWidth() {
        float margin = 12f * mDp;
        String pctText = mIsCharging ? (int) mChargingDisplayPct + "%" : mProgress + "%";
        float extra = measureTextWidth("WWWW", 11f, true);
        float desired = 0f;

        if (sCfgIslandPosition == 0) {
            if (mIsCharging) {
                float leftWidth = Math.max(
                        measureTextWidth(mChargeMode, 10f, true),
                        measureTextWidth(
                                pctText + (mChargeStatsLine2.isEmpty()
                                        ? "" : " • " + mChargeStatsLine2),
                                9f,
                                false));
                float rightWidth = Math.max(
                        measureTextWidth(mChargeStatsLine1, 10f, true),
                        measureTextWidth(
                                mChargeTime.replace(" • ", "").trim(),
                                9f,
                                false));
                desired = mArcBounds.width() + leftWidth + rightWidth + (margin * 2f) + extra;
            } else {
                desired = mArcBounds.width()
                        + measureTextWidth(pctText, 12f, true)
                        + measureTextWidth(
                                mFilenameHint != null ? mFilenameHint : "Downloading...",
                                11f,
                                false)
                        + (margin * 2f) + extra;
            }
        } else {
            String line1 = pctText + " • " + (mIsCharging
                    ? mChargeMode
                    : (mFilenameHint != null ? mFilenameHint : "Downloading..."));
            String line2 = mIsCharging
                    ? mChargeStatsLine1 + (mChargeStatsLine2.isEmpty()
                    ? "" : " • " + mChargeStatsLine2)
                    : "";
            desired = Math.max(
                    measureTextWidth(line1, 10f, true),
                    measureTextWidth(line2, 9f, false)) + (margin * 2f) + extra;
        }

        return Math.max(desired, mArcBounds.width() + (margin * 2f));
    }

    private void drawIsland(Canvas c, int col, int pct) {
        int alpha = (int) (255 * (sCfgDialogBgOpacityPct / 100f) * mIslandExpandFraction);
        float cornerRadius = mCurrentTouchBounds.height() / 2f;

        mIslandBgPaint.setColor(Color.BLACK);
        mIslandBgPaint.setAlpha(alpha);
        mIslandBgPaint.setStyle(Paint.Style.FILL);
        c.drawRoundRect(mCurrentTouchBounds, cornerRadius, cornerRadius, mIslandBgPaint);

        if (!sCfgIslandCompact && mIsCharging) {
            drawDashboardUI(c, col, alpha);
            return;
        }

        float progressFraction = mIsCharging ? mChargingDisplayPct / 100f : pct / 100f;

        if (progressFraction > 0) {
            float pulseMultiplier = 1f;

            if (mIsCharging && mChargingPulseEnabled && mChargingPulseAnim != null) {
                pulseMultiplier = 0.6f + (0.4f * mChargingPulsePhase);
            }

            if (!mIsCharging
                    && sCfgRingColorMode == CutoutProgressSettings.RING_COLOR_MODE_RAINBOW) {
                android.graphics.LinearGradient rainbowShader =
                        new android.graphics.LinearGradient(
                                mCurrentTouchBounds.left, 0,
                                mCurrentTouchBounds.right, 0,
                                RAINBOW, null,
                                android.graphics.Shader.TileMode.CLAMP
                        );
                mIslandBgPaint.setShader(rainbowShader);
            } else {
                int fillColor = mIsCharging ? chargingColor(mChargingDisplayPct) : col;
                mIslandBgPaint.setShader(null);
                mIslandBgPaint.setColor(fillColor);
            }

            mIslandBgPaint.setAlpha((int) (alpha * 0.8f * pulseMultiplier));

            c.save();
            int effectivePosition = sCfgIslandCompact ? sCfgIslandPosition : 0;
            if (effectivePosition == 1) {
                float clipLeft = mCurrentTouchBounds.right
                        - (mCurrentTouchBounds.width() * progressFraction);
                c.clipRect(
                        clipLeft,
                        mCurrentTouchBounds.top,
                        mCurrentTouchBounds.right,
                        mCurrentTouchBounds.bottom
                );
            } else {
                float clipRight = mCurrentTouchBounds.left
                        + (mCurrentTouchBounds.width() * progressFraction);
                c.clipRect(
                        mCurrentTouchBounds.left,
                        mCurrentTouchBounds.top,
                        clipRight,
                        mCurrentTouchBounds.bottom
                );
            }
            c.drawRoundRect(mCurrentTouchBounds, cornerRadius, cornerRadius, mIslandBgPaint);
            c.restore();

            mIslandBgPaint.setShader(null);
        }

        if (mIslandExpandFraction <= 0.5f) return;

        float textAlphaMulti = (mIslandExpandFraction - 0.5f) * 2f;
        mTextPaint.setAlpha((int) (255 * textAlphaMulti));

        String pctStr = (mIsCharging ? (int) mChargingDisplayPct : pct) + "%";
        float centerY = mCurrentTouchBounds.centerY();

        if (!sCfgIslandCompact) {
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setTextSize(13f * mDp);
            mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            String fullText =
                    pctStr + " • " + (mFilenameHint != null ? mFilenameHint : "Downloading...");
            float textY = centerY - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f);
            c.drawText(fullText, mCurrentTouchBounds.centerX(), textY, mTextPaint);
            return;
        }

        if (sCfgIslandPosition == 0) {
            if (mIsCharging) {
                float leftCenterX = (mCurrentTouchBounds.left + mArcBounds.left) / 2f;
                float rightCenterX = (mArcBounds.right + mCurrentTouchBounds.right) / 2f;

                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setTextAlign(Paint.Align.CENTER);
                mTextPaint.setTextSize(10f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(mChargeMode, leftCenterX, centerY - (2f * mDp), mTextPaint);

                mTextPaint.setTextSize(9f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT);
                c.drawText(
                        pctStr + (mChargeStatsLine2.isEmpty() ? "" : " • " + mChargeStatsLine2),
                        leftCenterX,
                        centerY + (9f * mDp),
                        mTextPaint
                );

                float maxTextWidth = Math.max(
                        0f,
                        (mCurrentTouchBounds.right - mArcBounds.right) - (16f * mDp));
                mTextPaint.setTextSize(10f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(
                        TextUtils.ellipsize(
                                mChargeStatsLine1,
                                mTextPaint,
                                maxTextWidth,
                                TextUtils.TruncateAt.END
                        ).toString(),
                        rightCenterX,
                        centerY - (2f * mDp),
                        mTextPaint
                );

                mTextPaint.setTextSize(9f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT);
                c.drawText(
                        TextUtils.ellipsize(
                                mChargeTime.replace(" • ", "").trim(),
                                mTextPaint,
                                maxTextWidth,
                                TextUtils.TruncateAt.END
                        ).toString(),
                        rightCenterX,
                        centerY + (9f * mDp),
                        mTextPaint
                );
            } else {
                mTextPaint.setColor(col);
                mTextPaint.setTextSize(12f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
                mTextPaint.setTextAlign(Paint.Align.RIGHT);

                float yPos = centerY - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f);
                c.drawText(pctStr, mArcBounds.left - 12f * mDp, yPos, mTextPaint);

                float infoX = mArcBounds.right + (12f * mDp);
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setTextSize(11f * mDp);
                mTextPaint.setTextAlign(Paint.Align.LEFT);
                mTextPaint.setTypeface(Typeface.DEFAULT);

                String subStr = mFilenameHint != null ? mFilenameHint : "Downloading...";
                String ellip = TextUtils.ellipsize(
                        subStr,
                        mTextPaint,
                        Math.max(0f, mCurrentTouchBounds.right - infoX - (12f * mDp)),
                        TextUtils.TruncateAt.MIDDLE
                ).toString();
                c.drawText(ellip, infoX, yPos, mTextPaint);
            }
        } else {
            float safeLeft = sCfgIslandPosition == 1
                    ? mCurrentTouchBounds.left + (12f * mDp)
                    : mArcBounds.right + (12f * mDp);
            float safeRight = sCfgIslandPosition == 1
                    ? mArcBounds.left - (12f * mDp)
                    : mCurrentTouchBounds.right - (12f * mDp);

            String line1 = mIsCharging
                    ? mChargeMode
                    : (mFilenameHint != null ? mFilenameHint : "Downloading...");
            String line2 = mIsCharging
                    ? mChargeStatsLine1 + (mChargeStatsLine2.isEmpty()
                    ? "" : " • " + mChargeStatsLine2)
                    : "";

            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextAlign(
                    sCfgIslandPosition == 1 ? Paint.Align.RIGHT : Paint.Align.LEFT);
            float x = sCfgIslandPosition == 1 ? safeRight : safeLeft;
            float maxTextWidth = Math.max(0f, safeRight - safeLeft);

            if (!TextUtils.isEmpty(line2)) {
                mTextPaint.setTextSize(10f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(
                        TextUtils.ellipsize(
                                pctStr + " • " + line1,
                                mTextPaint,
                                maxTextWidth,
                                TextUtils.TruncateAt.END
                        ).toString(),
                        x,
                        centerY - (1f * mDp),
                        mTextPaint
                );

                mTextPaint.setTextSize(9f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT);
                c.drawText(
                        TextUtils.ellipsize(
                                line2,
                                mTextPaint,
                                maxTextWidth,
                                TextUtils.TruncateAt.END
                        ).toString(),
                        x,
                        centerY + (9f * mDp),
                        mTextPaint
                );
            } else {
                mTextPaint.setTextSize(11f * mDp);
                mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(
                        TextUtils.ellipsize(
                                pctStr + " • " + line1,
                                mTextPaint,
                                maxTextWidth,
                                TextUtils.TruncateAt.MIDDLE
                        ).toString(),
                        x,
                        centerY - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f),
                        mTextPaint
                );
            }
        }
    }

    private void drawDashboardUI(Canvas c, int col, int baseAlpha) {
        float alphaMult = (mIslandExpandFraction - 0.5f) * 2f;
        if (alphaMult <= 0f) return;

        int textAlpha = (int) (255 * alphaMult);

        float progressFraction = mChargingDisplayPct / 100f;
        int fillColor = chargingColor(mChargingDisplayPct);

        float pulseMultiplier = 1f;
        if (mChargingPulseEnabled && mChargingPulseAnim != null) {
            pulseMultiplier = 0.6f + (0.4f * mChargingPulsePhase);
        }

        float finalWidth = 320f * mDp;
        float targetLeft = mArcBounds.centerX() - finalWidth / 2f;
        float targetRight = mArcBounds.centerX() + finalWidth / 2f;

        float barW = 18f * mDp;
        float barH = 36f * mDp;
        float padX = 24f * mDp;

        float centerY = mCurrentTouchBounds.centerY();
        float barLeft = targetLeft + padX;
        float barTop = centerY - (barH / 2f);
        float barRight = barLeft + barW;
        float barBottom = barTop + barH;

        RectF batteryBody = new RectF(barLeft, barTop, barRight, barBottom);

        mIslandBgPaint.setStyle(Paint.Style.STROKE);
        mIslandBgPaint.setStrokeWidth(1.5f * mDp);
        mIslandBgPaint.setColor(Color.WHITE);
        mIslandBgPaint.setAlpha((int) (80 * alphaMult));
        c.drawRoundRect(batteryBody, 3f * mDp, 3f * mDp, mIslandBgPaint);

        float capW = 8f * mDp;
        float capH = 2.5f * mDp;
        float capLeft = barLeft + (barW - capW) / 2f;
        RectF batteryCap = new RectF(capLeft, barTop - capH, capLeft + capW, barTop);
        c.drawRoundRect(batteryCap, 1f * mDp, 1f * mDp, mIslandBgPaint);

        mIslandBgPaint.setStyle(Paint.Style.FILL);
        mIslandBgPaint.setAlpha((int) (20 * alphaMult));
        c.drawRoundRect(batteryBody, 3f * mDp, 3f * mDp, mIslandBgPaint);

        if (progressFraction > 0) {
            mIslandBgPaint.setColor(fillColor);
            mIslandBgPaint.setAlpha((int) (255 * pulseMultiplier * alphaMult));
            c.save();
            c.clipRect(barLeft, barBottom - (barH * progressFraction), barRight, barBottom);
            c.drawRoundRect(batteryBody, 3f * mDp, 3f * mDp, mIslandBgPaint);
            c.restore();
        }

        String raw1 = mChargeStatsLine1 != null ? mChargeStatsLine1 : "";
        String raw2 = mChargeStatsLine2 != null ? mChargeStatsLine2 : "";
        String combinedStats = (raw1 + " " + raw2).replace("·", " ").replace("•", " ");

        String currentVal = extractValue(combinedStats, "mA", "A");
        String powerVal = extractValue(combinedStats, "W");
        String voltageVal = extractValue(combinedStats, "V");
        String tempVal = extractValue(combinedStats, "°C", "C");

        String timeStr = mChargeTime != null ? mChargeTime.replace(" • ", "").trim() : "";

        float textStartX = barRight + 16f * mDp;
        float leftLineSpacing = 16f * mDp;

        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAlpha(textAlpha);
        mTextPaint.setTextSize(14f * mDp);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(mChargeMode != null ? mChargeMode : "", textStartX,
                centerY - leftLineSpacing, mTextPaint);

        mTextPaint.setTextSize(22f * mDp);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText((int) mChargingDisplayPct + "%", textStartX, centerY + 6f * mDp, mTextPaint);

        mTextPaint.setColor(Color.LTGRAY);
        mTextPaint.setAlpha(textAlpha);
        mTextPaint.setTextSize(11f * mDp);
        mTextPaint.setTypeface(Typeface.DEFAULT);
        c.drawText(timeStr, textStartX, centerY + leftLineSpacing + 6f * mDp, mTextPaint);

        float statsRightX = targetRight - padX;
        float rightLineSpacing = 14f * mDp;
        float rightStartY = centerY - (rightLineSpacing * 1.5f);

        mTextPaint.setTypeface(Typeface.DEFAULT);
        mTextPaint.setTextSize(11f * mDp);

        drawStatLine(c, "Power:", powerVal, statsRightX, rightStartY, textAlpha);
        rightStartY += rightLineSpacing;

        drawStatLine(c, "Current:", currentVal, statsRightX, rightStartY, textAlpha);
        rightStartY += rightLineSpacing;

        drawStatLine(c, "Voltage:", voltageVal, statsRightX, rightStartY, textAlpha);
        rightStartY += rightLineSpacing;

        drawStatLine(c, "Temp:", tempVal, statsRightX, rightStartY, textAlpha);
    }

    private void drawStatLine(Canvas c, String label, String value, float rightX,
                              float y, int alpha) {
        if (TextUtils.isEmpty(value)) value = "--";

        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAlpha(alpha);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(value, rightX, y, mTextPaint);

        float valueWidth = mTextPaint.measureText(value);
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setColor(Color.LTGRAY);
        mTextPaint.setAlpha(alpha);
        mTextPaint.setTypeface(Typeface.DEFAULT);
        c.drawText(label + " ", rightX - valueWidth, y, mTextPaint);
    }

    private String extractValue(String source, String... suffixes) {
        if (TextUtils.isEmpty(source)) return "";
        String[] tokens = source.split("\\s+");
        for (String token : tokens) {
            for (String suffix : suffixes) {
                if (token.endsWith(suffix)) {
                    return token;
                }
            }
        }
        return "";
    }

    private static void applyStroke(Paint paint, int color, float width, int alpha) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setAlpha(alpha);
    }

    private static Paint makePaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    private static int chargingColor(float pct) {
        if (pct < 30f) {
            return 0xCCF44336;
        } else if (pct < 60f) {
            return 0xCCFF9800;
        } else {
            return 0xCC4CAF50;
        }
    }

    private static int brighten(int color, float fraction) {
        int alpha = Color.alpha(color);
        int red = Math.min(255,
                (int) (Color.red(color) + (255 - Color.red(color)) * fraction));
        int green = Math.min(255,
                (int) (Color.green(color) + (255 - Color.green(color)) * fraction));
        int blue = Math.min(255,
                (int) (Color.blue(color) + (255 - Color.blue(color)) * fraction));

        return Color.argb(alpha, red, green, blue);
    }

    private static int blendColors(int color1, int color2, float ratio) {
        float inverse = 1f - ratio;
        int alpha = Color.alpha(color1);
        int red = (int) (Color.red(color1) * inverse + Color.red(color2) * ratio);
        int green = (int) (Color.green(color1) * inverse + Color.green(color2) * ratio);
        int blue = (int) (Color.blue(color1) * inverse + Color.blue(color2) * ratio);

        return Color.argb(alpha, red, green, blue);
    }
}
