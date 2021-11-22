package first;

import java.io.Serializable;

class VectorClock implements Serializable, Cloneable {
    private int[] values;

    protected VectorClock(int num_processes) {
        this.values = new int[num_processes];
    }

    @Override
    public VectorClock clone() {
        VectorClock clone = new VectorClock(this.values.length);
        clone.values = this.values.clone();
        return clone;
    }

    public boolean equals(VectorClock other) {
        return this.values.equals(other.values);
    }

    public boolean lessThanEq(VectorClock other) {
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i] > other.values[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean lessThan(VectorClock other) {
        return lessThanEq(other) && !equals(other);
    }

    public void update(VectorClock other) {
        for (int i = 0; i < this.values.length; i++) {
            if (other.values[i] > this.values[i]) {
                this.values[i] = other.values[i];
            }
        }
    }

    public void tick(int process_id) {
        this.values[process_id]++;
    }

    public VectorClock ticked(int process_id) {
        VectorClock ticked = null;
        try {
            ticked = (VectorClock) this.clone();
        } catch (Exception e) {
        }
        ticked.tick(process_id);
        return ticked;
    }
}