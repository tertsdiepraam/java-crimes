package GallagerHumbletSpira;

import java.io.Serializable;

import GallagerHumbletSpira.Client.Edge;
import GallagerHumbletSpira.Client.State;
import GallagerHumbletSpira.Client.Fragment;

class Message implements Serializable {
    enum Type {
        Test, Initiate, Accept, Reject, ChangeRoot, Report, Connect
    }

    Type type;
    Fragment fragment;
    State S;
    Edge j;

    public Message(Type type, Fragment fragment, State S, Edge j) {
        this.type = type;
        this.fragment = fragment;
        this.S = S;
        this.j = j;
    }
}