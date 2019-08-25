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
    private static final float RIM_RADIUS_PERCENTAGE = 0.9f;
    private static final float RIM_TEXT_FONT_SIZE = 16f;
    private static final float RIM_TEXT_PADDING_ANGLE = 5f;
    private static final float HOUR_HAND_LENGTH_PERCENTAGE = 0.5f;
    private static final float MINUTE_HAND_LENGTH_PERCENTAGE = 0.7f;
    private static final float SECOND_HAND_LENGTH_PERCENTAGE = 0.8f;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final float STROKE_WIDTH = 2f;

        private Calendar calendar;
        private SimpleDateFormat dowFormatter;
        private SimpleDateFormat dateFormatter;

        private boolean hasRegisteredTimeZoneReceiver = false;
        private boolean isInAmbientMode;

        private float faceWidth;
        private float faceHeight;

        private float centerX;
        private float centerY;
        private float rimRadius;
        private float rimCircumference;

        private float secondHandLength;
        private float minuteHandLength;
        private float hourHandLength;

        private Paint backgroundPaint;
        private Paint foregroundPaint;
        private Paint dowPaint;
        private Paint datePaint;
        private Paint handsPaint;
        private Paint handsAccessoriesPaint;
        private Paint secondHandPaint;
        private Paint secondHandAccessoriesPaint;

        private Path textCircle;
        private Path hourHandPath;
        private Path minuteHandPath;
        private Path secondHandPath;

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

            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(RealiestSimplestFace.this)
                            .setHideStatusBar(true)
                            .setHideNotificationIndicator(true)
                            .build()
            );

            calendar = Calendar.getInstance();
            dowFormatter = new SimpleDateFormat("EEE");
            dateFormatter = new SimpleDateFormat("MMM d");

            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);

            foregroundPaint = new Paint();
            foregroundPaint.setColor(Color.WHITE);
            foregroundPaint.setStrokeWidth(STROKE_WIDTH);
            foregroundPaint.setAntiAlias(true);
            foregroundPaint.setStrokeCap(Paint.Cap.ROUND);

            dowPaint = new Paint();
            dowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            dowPaint.setColor(Color.WHITE);
            dowPaint.setTypeface(Typeface.MONOSPACE);
            dowPaint.setTextSize(RIM_TEXT_FONT_SIZE);
            dowPaint.setTextAlign(Paint.Align.RIGHT);
            dowPaint.setAntiAlias(true);

            datePaint = new Paint();
            datePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            datePaint.setColor(Color.WHITE);
            datePaint.setTypeface(Typeface.MONOSPACE);
            datePaint.setTextSize(RIM_TEXT_FONT_SIZE);
            datePaint.setTextAlign(Paint.Align.LEFT);
            datePaint.setAntiAlias(true);

            handsPaint = new Paint();
            handsPaint.setColor(Color.WHITE);
            handsPaint.setStyle(Paint.Style.FILL);
            handsPaint.setStrokeWidth(STROKE_WIDTH);
            handsPaint.setAntiAlias(true);
            handsPaint.setStrokeCap(Paint.Cap.ROUND);
            handsPaint.setStrokeJoin(Paint.Join.ROUND);
            handsPaint.setPathEffect(new CornerPathEffect(2));
            handsPaint.setShadowLayer(1, 0, 0, Color.BLACK);

            handsAccessoriesPaint = new Paint();
            handsAccessoriesPaint.setColor(Color.WHITE);
            handsAccessoriesPaint.setStyle(Paint.Style.FILL);
            handsAccessoriesPaint.setStrokeWidth(STROKE_WIDTH);
            handsAccessoriesPaint.setAntiAlias(true);
            handsAccessoriesPaint.setStrokeCap(Paint.Cap.ROUND);
            handsAccessoriesPaint.setStrokeJoin(Paint.Join.ROUND);
            handsAccessoriesPaint.setPathEffect(new CornerPathEffect(2));

            secondHandPaint = new Paint();
            secondHandPaint.setColor(Color.rgb(255, 87, 34));
            secondHandPaint.setStyle(Paint.Style.FILL);
            secondHandPaint.setStrokeWidth(STROKE_WIDTH);
            secondHandPaint.setAntiAlias(true);
            secondHandPaint.setStrokeCap(Paint.Cap.ROUND);
            secondHandPaint.setStrokeJoin(Paint.Join.ROUND);
            secondHandPaint.setPathEffect(new CornerPathEffect(2));
            secondHandPaint.setShadowLayer(1, 0, 0, Color.BLACK);

            secondHandAccessoriesPaint = new Paint();
            secondHandAccessoriesPaint.setColor(Color.rgb(255, 87, 34));
            secondHandAccessoriesPaint.setStyle(Paint.Style.FILL);
            secondHandAccessoriesPaint.setStrokeWidth(STROKE_WIDTH);
            secondHandAccessoriesPaint.setAntiAlias(true);
            secondHandAccessoriesPaint.setStrokeCap(Paint.Cap.ROUND);
            secondHandAccessoriesPaint.setStrokeJoin(Paint.Join.ROUND);
            secondHandAccessoriesPaint.setPathEffect(new CornerPathEffect(2));

            textCircle = new Path();
            secondHandPath = new Path();
            hourHandPath = new Path();
            minuteHandPath = new Path();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (isInAmbientMode != inAmbientMode) {
                isInAmbientMode = inAmbientMode;
                foregroundPaint.setColor(isInAmbientMode ? Color.GRAY : Color.WHITE);
                invalidate();
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
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

            // Calculate rim parameters.
            this.rimRadius = this.faceWidth / 2 * RealiestSimplestFace.RIM_RADIUS_PERCENTAGE;
            this.rimCircumference =  (float)(2 * Math.PI * this.rimRadius);

            // Calculate lengths of different hands based on watch screen size.
            this.hourHandLength = this.centerX * RealiestSimplestFace.HOUR_HAND_LENGTH_PERCENTAGE;
            this.minuteHandLength = this.centerX * RealiestSimplestFace.MINUTE_HAND_LENGTH_PERCENTAGE;
            this.secondHandLength = this.centerX * RealiestSimplestFace.SECOND_HAND_LENGTH_PERCENTAGE;

            // (Re)build text path.
            textCircle.reset();
            textCircle.addCircle(centerX, centerY, this.rimRadius, Path.Direction.CW);

            // (Re)build hour hand path.
            hourHandPath.reset();
            hourHandPath.moveTo(centerX,     centerY - hourHandLength);
            hourHandPath.lineTo(centerX + 1, centerY - hourHandLength);
            hourHandPath.lineTo(centerX + 3, centerY - hourHandLength + 3);
            hourHandPath.lineTo(centerX + 5, centerY);
            hourHandPath.lineTo(centerX - 5, centerY);
            hourHandPath.lineTo(centerX - 3, centerY - hourHandLength + 3);
            hourHandPath.lineTo(centerX - 1, centerY - hourHandLength);
            hourHandPath.lineTo(centerX,     centerY - hourHandLength);

            // (Re)build minute hand path.
            minuteHandPath.reset();
            minuteHandPath.moveTo(centerX,     centerY - minuteHandLength);
            minuteHandPath.lineTo(centerX + 1, centerY - minuteHandLength);
            minuteHandPath.lineTo(centerX + 2, centerY - minuteHandLength + 2);
            minuteHandPath.lineTo(centerX + 4, centerY);
            minuteHandPath.lineTo(centerX - 4, centerY);
            minuteHandPath.lineTo(centerX - 2, centerY - minuteHandLength + 2);
            minuteHandPath.lineTo(centerX - 1, centerY - minuteHandLength);
            minuteHandPath.lineTo(centerX,     centerY - minuteHandLength);

            // (Re)build second hand path.
            secondHandPath.reset();
            secondHandPath.moveTo(centerX,     centerY - secondHandLength);
            secondHandPath.lineTo(centerX + 1, centerY - secondHandLength);
            secondHandPath.lineTo(centerX + 1, centerY - secondHandLength + 1);
            secondHandPath.lineTo(centerX + 2, centerY);
            secondHandPath.lineTo(centerX + 1, centerY + 40);
            secondHandPath.lineTo(centerX - 1, centerY + 40);
            secondHandPath.lineTo(centerX - 2, centerY);
            secondHandPath.lineTo(centerX - 1, centerY - secondHandLength + 1);
            secondHandPath.lineTo(centerX - 1, centerY - secondHandLength);
            secondHandPath.lineTo(centerX,     centerY - secondHandLength);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar.setTimeInMillis(System.currentTimeMillis());

            drawBackground(canvas);
            drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        }

        private void drawWatchFace(Canvas canvas) {
            // Draw the 12 dots.
            canvas.save();
            for (int i = 0; i < 12; i++) {
                canvas.rotate(360 / 12, centerX, centerY);
                canvas.drawCircle(centerX, this.centerY - this.rimRadius, 3, foregroundPaint);
            }
            canvas.restore();

            // Only draw status texts when not in ambient mode.
            if (!isInAmbientMode) {
                Date now = calendar.getTime();
                String dow = dowFormatter.format(now).toUpperCase();
                String date = dateFormatter.format(now).toUpperCase();

                Rect textBounds = new Rect();
                dowPaint.getTextBounds(dow, 0, 1, textBounds);
                canvas.drawTextOnPath(dow, textCircle, -(this.rimCircumference * ((90f + RealiestSimplestFace.RIM_TEXT_PADDING_ANGLE) / 360f)), (textBounds.height() / 2f), dowPaint);
                datePaint.getTextBounds(date, 0, 1, textBounds);
                canvas.drawTextOnPath(date, textCircle, (this.rimCircumference * ((270f + RealiestSimplestFace.RIM_TEXT_PADDING_ANGLE) / 360f)), (textBounds.height() / 2f), datePaint);
            }

            // These calculations reflect the rotation in degrees per unit of time.
            // For example: 360 / 60 = 6 and 360 / 12 = 30
            final float seconds = (calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;
            final float minutesRotation = calendar.get(Calendar.MINUTE) * 6f;
            final float hourHandOffset = calendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (calendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            // Save the canvas state before we can begin to rotate it.
            canvas.save();

            // Draw hour hand.
            canvas.rotate(hoursRotation, centerX, centerY);
            canvas.drawPath(hourHandPath, handsPaint);

            // Draw minute hand.
            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY);
            canvas.drawPath(minuteHandPath, handsPaint);

            // Draw center circle.
            canvas.drawCircle(centerX, centerY, 12, handsAccessoriesPaint);
            canvas.drawCircle(centerX, centerY, 4, backgroundPaint);

            // Only draw second hand when not in ambient mode.
            if (!isInAmbientMode) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY);
                canvas.drawPath(secondHandPath, secondHandPaint);

                // Draw second hand's own center circle.
                canvas.drawCircle(centerX, centerY, 8, secondHandAccessoriesPaint);
                canvas.drawCircle(centerX, centerY, 4, backgroundPaint);
            }

            // Restore the canvas' original orientation.
            canvas.restore();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
        }

        private void registerReceiver() {
            if (hasRegisteredTimeZoneReceiver) {
                return;
            }

            hasRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            RealiestSimplestFace.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!hasRegisteredTimeZoneReceiver) {
                return;
            }

            hasRegisteredTimeZoneReceiver = false;
            RealiestSimplestFace.this.unregisterReceiver(timeZoneReceiver);
        }

        // Starts/stops the updateTimeHandler timer based on the state of the watch face.
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        // Returns whether the updateTimeHandler timer should be running.
        // The timer should only run in active mode.
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode;
        }

        // Handle updating the time periodically in interactive mode.
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<RealiestSimplestFace.Engine> engineRef;

        public EngineHandler(RealiestSimplestFace.Engine reference) {
            engineRef = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            RealiestSimplestFace.Engine engine = engineRef.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
