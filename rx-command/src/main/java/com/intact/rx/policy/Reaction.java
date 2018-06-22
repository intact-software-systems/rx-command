package com.intact.rx.policy;

/**
 * A policy to decide "what to do" upon an event.
 * <p>
 * For example: If error, then either continue or halt.
 */
public enum Reaction {
    CONTINUE, HALT, RETRY
}
