package first;

import java.io.Serializable;
import java.util.HashMap;

record Message(String message, HashMap<Integer, VectorClock> buffer, VectorClock clock) implements Serializable {}
