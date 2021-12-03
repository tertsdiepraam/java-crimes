package Singhal;

import java.io.Serializable;

record Token (int[] N, State[] S) implements Serializable {
    void print() {
        System.out.print("[");
        for (State s : S) {
            System.out.print(s);
            System.out.print(", ");
        }
        System.out.println("]");
    }
}
