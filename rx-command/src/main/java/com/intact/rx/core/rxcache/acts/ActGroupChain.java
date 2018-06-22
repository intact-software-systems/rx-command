package com.intact.rx.core.rxcache.acts;

import java.util.Vector;

public class ActGroupChain extends Vector<ActGroup> {

    private static final long serialVersionUID = 4448642194597957639L;

    private ActGroupChain() {
    }

    public static ActGroupChain create() {
        return new ActGroupChain();
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ActGroupChain@").append(hashCode()).append(", groups=\n");
        for (ActGroup actGroup : this) {
            builder.append("\t").append(actGroup).append("\n");
        }
        return builder.toString();
    }
}
