package cc.simp.utils.client;

public class Timer {
    public long time = System.currentTimeMillis();

    public boolean hasTimeElapsed(double delay, boolean reset) {
        if ((double) (System.currentTimeMillis() - this.time) >= delay) {
            if (reset) {
                this.reset();
            }
            return true;
        }
        return false;
    }

    public boolean hasTimeElapsed(double delay) {
        return (double) (System.currentTimeMillis() - this.time) >= delay;
    }

    public long getTime() {
        return System.currentTimeMillis() - this.time;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public void setTime(long time) {
        this.time = time;
    }

    // Usage: timer.stopAfter(numberOfSeconds, restartWhenSecondsReached)
    public boolean stopAfter(long time, boolean reset) {
        if (this.time > System.currentTimeMillis()) {
            this.time = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - this.time > time) {
            if (reset) {
                reset();
            }
            return true;
        } else {
            return false;
        }
    }
}
