package first;

import java.io.Serializable;

class VectorClock implements Serializable {
    int[] values;

    protected VectorClock(int num_processes) {
        this.values = new int[num_processes];
    }

    public boolean equals(VectorClock other) {
        return this.values.equals(other.values);
    }

    public boolean lessThanEq(VectorClock other) {
        for (int i=0; i<this.values.length; i++) {
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
        for (int i=0; i<this.values.length; i++) {
            if (other.values[i] > this.values[i]) {
                this.values[i] = other.values[i];
            }
        }
    }
    
    public void tick(int process_id) {
        this.values[process_id]++;
    }
}