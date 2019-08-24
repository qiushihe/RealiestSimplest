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

public class WatchFace extends CanvasWatchFaceService {
    // Updates rate in milliseconds for interactive mode.
    // We update once a second to advance the second hand.
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    // Handler message id for updating the time periodically in interactive mode.
    private static final int MSG_UPDATE_TIME = 0;

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

        private float centerX;
        private float centerY;
        private float textCircleRadius;
        private float textCircleCircumference;
        private float secondHandLength;
        private float minuteHandLength;
        private float hourHandLength;

        private Paint backgroundPaint;
        private Paint foregroundPaint;
        private Paint dowPaint;
        private Paint datePaint;
        private Paint handsPaint;
        private Paint secondHandPaint;

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

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this).build());

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
            dowPaint.setTextSize(10f);
            dowPaint.setTextAlign(Paint.Align.RIGHT);
            dowPaint.setAntiAlias(true);

            datePaint = new Paint();
            datePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            datePaint.setColor(Color.WHITE);
            datePaint.setTypeface(Typeface.MONOSPACE);
            datePaint.setTextSize(10f);
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

            secondHandPaint = new Paint();
            secondHandPaint.setColor(Color.rgb(255, 87, 34));
            secondHandPaint.setStyle(Paint.Style.FILL);
            secondHandPaint.setStrokeWidth(STROKE_WIDTH);
            secondHandPaint.setAntiAlias(true);
            secondHandPaint.setStrokeCap(Paint.Cap.ROUND);
            secondHandPaint.setStrokeJoin(Paint.Join.ROUND);
            secondHandPaint.setPathEffect(new CornerPathEffect(2));

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

            // Find the coordinates of the center point on the screen.
            centerX = width / 2f;
            centerY = height / 2f;

            // Calculate text circle parameters.
            textCircleRadius = centerX - 10;
            textCircleCircumference  = (float)(2 * Math.PI * textCircleRadius);

            // Calculate lengths of different hands based on watch screen size.
            hourHandLength = centerX * 0.5f;
            minuteHandLength = centerX * 0.7f;
            secondHandLength = centerX * 0.8f;

            // (Re)build text path.
            textCircle.reset();
            textCircle.addCircle(centerX, centerY, textCircleRadius, Path.Direction.CW);

            // (Re)build hour hand path.
            hourHandPath.reset();
            hourHandPath.moveTo(centerX, centerY - hourHandLength);
            hourHandPath.lineTo(centerX + 1, centerY - hourHandLength);
            hourHandPath.lineTo(centerX + 3, centerY);
            hourHandPath.lineTo(centerX - 3, centerY);
            hourHandPath.lineTo(centerX - 1, centerY - hourHandLength);
            hourHandPath.lineTo(centerX, centerY - hourHandLength);

            // (Re)build minute hand path.
            minuteHandPath.reset();
            minuteHandPath.moveTo(centerX, centerY - minuteHandLength);
            minuteHandPath.lineTo(centerX + 1, centerY - minuteHandLength);
            minuteHandPath.lineTo(centerX + 3, centerY);
            minuteHandPath.lineTo(centerX - 3, centerY);
            minuteHandPath.lineTo(centerX - 1, centerY - minuteHandLength);
            minuteHandPath.lineTo(centerX, centerY - minuteHandLength);

            // (Re)build second hand path.
            secondHandPath.reset();
            secondHandPath.moveTo(centerX, centerY - secondHandLength);
            secondHandPath.lineTo(centerX + 1, centerY - secondHandLength);
            secondHandPath.lineTo(centerX + 2, centerY + 40);
            secondHandPath.lineTo(centerX - 2, centerY + 40);
            secondHandPath.lineTo(centerX - 1, centerY - secondHandLength);
            secondHandPath.lineTo(centerX, centerY - secondHandLength);
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
                canvas.drawCircle(centerX, 10, 2, foregroundPaint);
            }
            canvas.restore();

            // Draw status texts.
            Date now = calendar.getTime();
            String dow = dowFormatter.format(now).toUpperCase();
            String date = dateFormatter.format(now).toUpperCase();

            Rect textBounds = new Rect();

            dowPaint.getTextBounds(dow, 0, 1, textBounds);
            canvas.drawTextOnPath(dow, textCircle, -(textCircleCircumference * ((90f + 3f) / 360f)), (textBounds.height() / 2f), dowPaint);

            datePaint.getTextBounds(date, 0, 1, textBounds);
            canvas.drawTextOnPath(date, textCircle, (textCircleCircumference * ((270f + 3f) / 360f)), (textBounds.height() / 2f), datePaint);

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
            canvas.drawCircle(centerX, centerY, 6, handsPaint);
            canvas.drawCircle(centerX, centerY, 3, backgroundPaint);

            // Only draw second hand when not in ambient mode.
            if (!isInAmbientMode) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY);
                canvas.drawPath(secondHandPath, secondHandPaint);

                // Draw second hand's own center circle.
                canvas.drawCircle(centerX, centerY, 5, secondHandPaint);
                canvas.drawCircle(centerX, centerY, 2, backgroundPaint);
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
            WatchFace.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!hasRegisteredTimeZoneReceiver) {
                return;
            }

            hasRegisteredTimeZoneReceiver = false;
            WatchFace.this.unregisterReceiver(timeZoneReceiver);
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
        private final WeakReference<WatchFace.Engine> engineRef;

        public EngineHandler(WatchFace.Engine reference) {
            engineRef = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFace.Engine engine = engineRef.get();
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
