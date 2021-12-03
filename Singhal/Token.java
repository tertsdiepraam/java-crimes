package Singhal;

import java.io.Serializable;

record Token (int[] N, State[] S) implements Serializable {
    void print() {
        System.out.print("[");
        for (int n : N) {
            System.out.print(n);
            System.out.print(", ");
        }
        System.out.println("]");
        System.out.print("[");
        for (State s : S) {
            System.out.print(s);
            System.out.print(", ");
        }
        System.out.println("]");
    }

    static Token clone(Token tok) {
        return new Token(tok.N().clone(), tok.S().clone());
    }
}
