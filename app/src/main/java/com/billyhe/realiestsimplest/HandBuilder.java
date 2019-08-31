package com.billyhe.realiestsimplest;

import android.graphics.Path;

import lombok.Getter;
import lombok.Setter;

public class HandBuilder {
    @Setter private float originX = 0;
    @Setter private float originY = 0;
    @Setter private float handLength = 0;
    @Setter private float tailLength = 0;
    @Setter private float tipWidth = 0;
    @Setter private float tipHeight = 0;
    @Setter private float baseWidth = 0;
    @Setter private float circleRadius = 0;

    @Getter private Path path;

    public HandBuilder() {
        this.path = new Path();
    }

    public void rebuild() {
        this.path.reset();

        if (this.handLength <= 0) {
            return;
        }

        this.path.moveTo(this.originX, this.originY - this.handLength);

        if (this.tipWidth > 0 || this.tipHeight > 0) {
            this.path.lineTo(this.originX + this.tipWidth, this.originY - this.handLength + this.tipHeight);
        }

        this.path.lineTo(this.originX + this.baseWidth, this.originY);

        if (this.tailLength > 0) {
            if (this.tipWidth > 0 || this.tipHeight > 0) {
                this.path.lineTo(this.originX + this.tipWidth, this.originY + this.tailLength - this.tipHeight);
            }

            this.path.lineTo(this.originX, this.originY + this.tailLength);
            this.path.lineTo(this.originX, this.originY + this.tailLength);

            if (this.tipWidth > 0 || this.tipHeight > 0) {
                this.path.lineTo(this.originX - this.tipWidth, this.originY + this.tailLength - this.tipHeight);
            }
        }

        this.path.lineTo(this.originX - this.baseWidth, this.originY);

        if (this.tipWidth > 0 || this.tipHeight > 0) {
            this.path.lineTo(this.originX - this.tipWidth, this.originY - this.handLength + this.tipHeight);
        }

        this.path.lineTo(this.originX, this.originY - this.handLength);

        if (this.circleRadius > 0) {
            this.path.addCircle(this.originX, this.originY, this.circleRadius, Path.Direction.CW);
        }
    }
}
