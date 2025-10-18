package cc.simp.utils.render.animations.impl;

import cc.simp.utils.render.animations.Animation;
import cc.simp.utils.render.animations.Direction;

public class EaseOutSine extends Animation {


    public EaseOutSine(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public EaseOutSine(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    @Override
    protected boolean correctOutput() {
        return true;
    }

    @Override
    protected double getEquation(double x) {
        return Math.sin(x * (Math.PI / 2));
    }
}
