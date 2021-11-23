package SchiperEggliSandoz;

import java.io.Serializable;

class VectorClock implements Serializable, Cloneable {
    private int[] values;

    public VectorClock(int num_processes) {
        values = new int[num_processes];
    }

    VectorClock(int[] values) {
        this.values = values;
    }

    @Override
    public VectorClock clone() {
        return new VectorClock(this.values.clone());
    }

    public boolean equals(VectorClock other) {
        return values.equals(other.values);
    }

    public boolean lessThanEq(VectorClock other) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] > other.values[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean lessThan(VectorClock other) {
        return lessThanEq(other) && !equals(other);
    }

    public void update(VectorClock other) {
        for (int i = 0; i < values.length; i++) {
            if (other.values[i] > values[i]) {
                values[i] = other.values[i];
            }
        }
    }

    public void tick(int process_id) {
        values[process_id]++;
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

    public String printValues() {
        String res = "";
        for(int i : values) {
            res += i;
        }
        return res;
    }
}