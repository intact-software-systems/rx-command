package com.intact.rx.policy;

public enum Reservation {
    NONE(Integer.MAX_VALUE),
    READ_WRITE(1000),
    READ_ONLY(100);

    private final int priveliges;

    Reservation(int i) {
        this.priveliges = i;
    }

    public int getPriveliges() {
        return priveliges;
    }
}
