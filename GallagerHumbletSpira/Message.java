package GallagerHumbletSpira;

import GallagerHumbletSpira.Client.Edge;
import GallagerHumbletSpira.Client.State;
import GallagerHumbletSpira.Client.Fragment;

class Message {
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