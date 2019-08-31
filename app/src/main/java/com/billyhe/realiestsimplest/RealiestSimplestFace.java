package com.billyhe.realiestsimplest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RealiestSimplestFace extends CanvasWatchFaceService {
    // Updates rate in milliseconds for interactive mode.
    // We update once a second to advance the second hand.
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    // Handler message id for updating the time periodically in interactive mode.
    private static final int MSG_UPDATE_TIME = 0;

    // Style constants.
    private static final float STROKE_WIDTH = 2f;
    private static final float RIM_RADIUS_PERCENTAGE = 0.95f;
    private static final float RIM_TEXT_FONT_SIZE = 16f;
    private static final float RIM_TEXT_PADDING_ANGLE = 5f;
    private static final float HOUR_HAND_LENGTH_PERCENTAGE = 0.60f;
    private static final float MINUTE_HAND_LENGTH_PERCENTAGE = 0.80f;
    private static final float SECOND_HAND_LENGTH_PERCENTAGE = 0.90f;
    private static final float SECOND_HAND_TAIL_PERCENTAGE = 0.3f;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private Calendar calendar;
        private SimpleDateFormat dowFormatter;
        private SimpleDateFormat dateFormatter;

        private HandBuilder hourHandBuilder;
        private HandBuilder minuteHandBuilder;
        private HandBuilder secondHandBuilder;

        private int notificationCount;
        private int unreadCount;

        private boolean hasRegisteredTimeZoneReceiver = false;
        private boolean isInAmbientMode;

        private float faceWidth;
        private float faceHeight;

        private float centerX;
        private float centerY;
        private float rimRadius;
        private float rimCircumference;

        private Path textCircle;

        private Paint backgroundPaint;
        private Paint foregroundPaint;
        private Paint dowPaint;
        private Paint datePaint;
        private Paint handsPaint;
        private Paint secondHandPaint;

        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        // Handler to update the time once a second in interactive mode.
        private final Handler updateTimeHandler = new EngineHandler(this);

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            this.setWatchFaceStyle(
                    new WatchFaceStyle.Builder(RealiestSimplestFace.this)
                            .setHideStatusBar(true)
                            .setHideNotificationIndicator(true)
                            .setShowUnreadCountIndicator(false)
                            .build()
            );

            this.calendar = Calendar.getInstance();
            this.dowFormatter = new SimpleDateFormat("EEE");
            this.dateFormatter = new SimpleDateFormat("MMM d");

            this.hourHandBuilder = new HandBuilder();
            this.hourHandBuilder.setTipWidth(4f);
            this.hourHandBuilder.setTipHeight(4f);
            this.hourHandBuilder.setBaseWidth(6f);
            this.hourHandBuilder.setCircleRadius(14f);

            this.minuteHandBuilder = new HandBuilder();
            this.minuteHandBuilder.setTipWidth(4f);
            this.minuteHandBuilder.setTipHeight(4f);
            this.minuteHandBuilder.setBaseWidth(6f);
            this.minuteHandBuilder.setCircleRadius(14f);

            this.secondHandBuilder = new HandBuilder();
            this.secondHandBuilder.setTipWidth(2f);
            this.secondHandBuilder.setTipHeight(2f);
            this.secondHandBuilder.setBaseWidth(4f);
            this.secondHandBuilder.setCircleRadius(10f);

            this.backgroundPaint = new Paint();
            this.backgroundPaint.setColor(Color.BLACK);
            this.backgroundPaint.setAntiAlias(true);

            this.foregroundPaint = new Paint();
            this.foregroundPaint.setColor(Color.WHITE);
            this.foregroundPaint.setAntiAlias(true);

            this.dowPaint = new Paint();
            this.dowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            this.dowPaint.setColor(Color.WHITE);
            this.dowPaint.setTypeface(Typeface.MONOSPACE);
            this.dowPaint.setTextSize(RealiestSimplestFace.RIM_TEXT_FONT_SIZE);
            this.dowPaint.setTextAlign(Paint.Align.RIGHT);
            this.dowPaint.setAntiAlias(true);

            this.datePaint = new Paint();
            this.datePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            this.datePaint.setColor(Color.WHITE);
            this.datePaint.setTypeface(Typeface.MONOSPACE);
            this.datePaint.setTextSize(RealiestSimplestFace.RIM_TEXT_FONT_SIZE);
            this.datePaint.setTextAlign(Paint.Align.LEFT);
            this.datePaint.setAntiAlias(true);

            this.handsPaint = new Paint();
            this.handsPaint.setColor(Color.WHITE);
            this.handsPaint.setStyle(Paint.Style.FILL);
            this.handsPaint.setStrokeWidth(RealiestSimplestFace.STROKE_WIDTH);
            this.handsPaint.setAntiAlias(true);
            this.handsPaint.setStrokeCap(Paint.Cap.ROUND);
            this.handsPaint.setStrokeJoin(Paint.Join.ROUND);
            this.handsPaint.setPathEffect(new CornerPathEffect(2));
            this.handsPaint.setShadowLayer(1, 0, 0, Color.BLACK);

            this.secondHandPaint = new Paint();
            this.secondHandPaint.setColor(Color.rgb(255, 87, 34));
            this.secondHandPaint.setStyle(Paint.Style.FILL);
            this.secondHandPaint.setStrokeWidth(RealiestSimplestFace.STROKE_WIDTH);
            this.secondHandPaint.setAntiAlias(true);
            this.secondHandPaint.setShadowLayer(1, 0, 0, Color.BLACK);

            this.textCircle = new Path();

            this.notificationCount = getNotificationCount();
            this.unreadCount = getUnreadCount();
        }

        @Override
        public void onDestroy() {
            this.updateTimeHandler.removeMessages(RealiestSimplestFace.MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            this.invalidate();
        }

        @Override
        public void onNotificationCountChanged(int count) {
            this.notificationCount = count;
        }

        @Override
        public void onUnreadCountChanged(int count) {
            this.unreadCount = count;
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (this.isInAmbientMode != inAmbientMode) {
                this.isInAmbientMode = inAmbientMode;
                this.foregroundPaint.setColor(this.isInAmbientMode ? Color.GRAY : Color.WHITE);
                invalidate();
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            this.updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Memorize face width/height.
            this.faceWidth = width;
            this.faceHeight = height;

            // Calculate surface element parameters.
            this.calculateSurfaceParameters();
        }

        private void calculateSurfaceParameters() {
            // Find the coordinates of the center point on the screen.
            this.centerX = this.faceWidth / 2f;
            this.centerY = this.faceHeight / 2f;

            // Update origin on hand builders.
            this.hourHandBuilder.setOriginX(this.centerX);
            this.hourHandBuilder.setOriginY(this.centerY);
            this.minuteHandBuilder.setOriginX(this.centerX);
            this.minuteHandBuilder.setOriginY(this.centerY);
            this.secondHandBuilder.setOriginX(this.centerX);
            this.secondHandBuilder.setOriginY(this.centerY);

            // Calculate rim parameters.
            this.rimRadius = this.faceWidth / 2 * RealiestSimplestFace.RIM_RADIUS_PERCENTAGE;
            this.rimCircumference =  (float)(2 * Math.PI * this.rimRadius);

            // Calculate lengths of different hands based on watch screen size.
            this.hourHandBuilder.setHandLength(this.rimRadius * RealiestSimplestFace.HOUR_HAND_LENGTH_PERCENTAGE);
            this.minuteHandBuilder.setHandLength(this.rimRadius * RealiestSimplestFace.MINUTE_HAND_LENGTH_PERCENTAGE);
            this.secondHandBuilder.setHandLength(this.rimRadius * RealiestSimplestFace.SECOND_HAND_LENGTH_PERCENTAGE);
            this.secondHandBuilder.setTailLength(this.rimRadius * RealiestSimplestFace.SECOND_HAND_TAIL_PERCENTAGE);

            // (Re)build hour hand path.
            this.hourHandBuilder.rebuild();

            // (Re)build minute hand path.
            this.minuteHandBuilder.rebuild();

            // (Re)build second hand path.
            this.secondHandBuilder.rebuild();

            // (Re)build text path.
            textCircle.reset();
            textCircle.addCircle(this.centerX, this.centerY, this.rimRadius, Path.Direction.CW);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            this.calendar.setTimeInMillis(System.currentTimeMillis());

            this.drawBackground(canvas);
            this.drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), this.backgroundPaint);
        }

        private void drawWatchFace(Canvas canvas) {
            this.drawMarkers(canvas);

            if (!isInAmbientMode) {
                this.drawStatusTexts(canvas);
            }

            this.drawHands(canvas);
            this.drawNotificationIndicator(canvas);
        }

        private void drawMarkers(Canvas canvas) {
            canvas.save();
            for (int i = 0; i < 12; i++) {
                canvas.rotate(360 / 12, this.centerX, this.centerY);
                canvas.drawCircle(this.centerX, this.centerY - this.rimRadius, 3, this.foregroundPaint);
            }
            canvas.restore();
        }

        private void drawStatusTexts(Canvas canvas) {
            Date now = calendar.getTime();
            String dow = dowFormatter.format(now).toUpperCase();
            String date = dateFormatter.format(now).toUpperCase();

            Rect textBounds = new Rect();
            dowPaint.getTextBounds(dow, 0, 1, textBounds);
            canvas.drawTextOnPath(dow, this.textCircle, -(this.rimCircumference * ((90f + RealiestSimplestFace.RIM_TEXT_PADDING_ANGLE) / 360f)), (textBounds.height() / 2f), this.dowPaint);
            datePaint.getTextBounds(date, 0, 1, textBounds);
            canvas.drawTextOnPath(date, this.textCircle, (this.rimCircumference * ((270f + RealiestSimplestFace.RIM_TEXT_PADDING_ANGLE) / 360f)), (textBounds.height() / 2f), this.datePaint);
        }

        private void drawHands(Canvas canvas) {
            // These calculations reflect the rotation in degrees per unit of time.
            // For example: 360 / 60 = 6 and 360 / 12 = 30
            final float seconds = (this.calendar.get(Calendar.SECOND) + this.calendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;
            final float minutesRotation = this.calendar.get(Calendar.MINUTE) * 6f;
            final float hourHandOffset = this.calendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (this.calendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            // Save the canvas state before we can begin to rotate it.
            canvas.save();

            // Draw hour hand.
            canvas.rotate(hoursRotation, this.centerX, this.centerY);
            canvas.drawPath(this.hourHandBuilder.getPath(), this.handsPaint);

            // Draw minute hand.
            canvas.rotate(minutesRotation - hoursRotation, this.centerX, this.centerY);
            canvas.drawPath(this.minuteHandBuilder.getPath(), this.handsPaint);

            // Only draw second hand when not in ambient mode.
            if (!this.isInAmbientMode) {
                canvas.rotate(secondsRotation - minutesRotation, this.centerX, this.centerY);
                canvas.drawPath(this.secondHandBuilder.getPath(), this.secondHandPaint);
            }

            // Restore the canvas' original orientation.
            canvas.restore();
        }

        private void drawNotificationIndicator(Canvas canvas) {
            if (this.unreadCount > 0) {
                if (this.isInAmbientMode) {
                    canvas.drawCircle(this.centerX, this.centerY, 8, this.backgroundPaint);
                    canvas.drawCircle(this.centerX, this.centerY, 4, this.handsPaint);
                } else {
                    canvas.drawCircle(this.centerX, this.centerY, 6, this.handsPaint);
                    canvas.drawCircle(this.centerX, this.centerY, 3, this.secondHandPaint);
                }
            } else if (this.notificationCount > 0) {
                if (this.isInAmbientMode) {
                    canvas.drawCircle(this.centerX, this.centerY, 8, this.backgroundPaint);
                } else {
                    canvas.drawCircle(this.centerX, this.centerY, 6, this.handsPaint);
                }
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                this.registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                this.calendar.setTimeZone(TimeZone.getDefault());
                this.invalidate();
            } else {
                this.unregisterReceiver();
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            this.updateTimer();
        }

        private void registerReceiver() {
            if (this.hasRegisteredTimeZoneReceiver) {
                return;
            }

            this.hasRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            RealiestSimplestFace.this.registerReceiver(this.timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!this.hasRegisteredTimeZoneReceiver) {
                return;
            }

            this.hasRegisteredTimeZoneReceiver = false;
            RealiestSimplestFace.this.unregisterReceiver(this.timeZoneReceiver);
        }

        // Starts/stops the updateTimeHandler timer based on the state of the watch face.
        private void updateTimer() {
            this.updateTimeHandler.removeMessages(RealiestSimplestFace.MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                this.updateTimeHandler.sendEmptyMessage(RealiestSimplestFace.MSG_UPDATE_TIME);
            }
        }

        // Returns whether the updateTimeHandler timer should be running.
        // The timer should only run in active mode.
        private boolean shouldTimerBeRunning() {
            return this.isVisible() && !this.isInAmbientMode;
        }

        // Handle updating the time periodically in interactive mode.
        private void handleUpdateTimeMessage() {
            this.invalidate();
            if (this.shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = RealiestSimplestFace.INTERACTIVE_UPDATE_RATE_MS - (timeMs % RealiestSimplestFace.INTERACTIVE_UPDATE_RATE_MS);
                this.updateTimeHandler.sendEmptyMessageDelayed(RealiestSimplestFace.MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<RealiestSimplestFace.Engine> engineRef;

        public EngineHandler(RealiestSimplestFace.Engine reference) {
            this.engineRef = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            RealiestSimplestFace.Engine engine = this.engineRef.get();
            if (engine != null) {
                switch (msg.what) {
                    case RealiestSimplestFace.MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
