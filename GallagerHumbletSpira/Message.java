package GallagerHumbletSpira;

import java.io.Serializable;

import GallagerHumbletSpira.Client.Edge;
import GallagerHumbletSpira.Client.State;
import GallagerHumbletSpira.Client.Fragment;

record Message(Type type, Fragment fragment, State S, Edge j, Integer w) implements Serializable {
    enum Type {
        Test, Initiate, Accept, Reject, ChangeRoot, Report, Connect
    }
}